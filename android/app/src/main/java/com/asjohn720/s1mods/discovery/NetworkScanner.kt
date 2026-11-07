package com.asjohn720.s1mods.discovery

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

data class DiscoveredScooter(
    val ip: String,
    val scope: Int? = null,
    val isPortOpen: Boolean = false,
    val interfaceName: String = "",
    val source: String = "",
    val displayName: String = "Ola Scooter"
)

object NetworkScanner {

    private var multicastLock: WifiManager.MulticastLock? = null

    /**
     * Strips address decorations shared by the ADB and gRPC clients:
     * an `ipv6:`/`//` prefix, outer `[ ]` brackets, a trailing `:port` on IPv4,
     * and a `%25`-escaped scope. Does not touch the scope suffix itself.
     */
    fun normalizeHost(host: String): String {
        var h = host.trim()
        if (h.startsWith("ipv6:", ignoreCase = true)) h = h.substring(5).trim()
        if (h.startsWith("//")) h = h.substring(2).trim()
        if (h.startsWith("[") && h.contains("]")) {
            h = h.substringAfter("[").substringBefore("]")
        } else if (h.contains(":") && !h.contains("::")) {
            h = h.substringBefore(":")
        }
        return h.replace("%25", "%")
    }

    /**
     * Converts a device MAC address (e.g. aa:bb:cc:dd:ee:ff) into its deterministic
     * SLAAC EUI-64 link-local IPv6 address (e.g. fe80::a8bb:ccff:fedd:eeff).
     */
    fun macToIpv6(mac: String): String? {
        val clean = mac.replace("[:-]".toRegex(), "").trim()
        if (clean.length != 12) return null
        return try {
            val bytes = clean.chunked(2).map { it.toInt(16) }
            val b0 = (bytes[0] xor 0x02).toString(16).padStart(2, '0')
            val b1 = bytes[1].toString(16).padStart(2, '0')
            val b2 = bytes[2].toString(16).padStart(2, '0')
            val b3 = bytes[3].toString(16).padStart(2, '0')
            val b4 = bytes[4].toString(16).padStart(2, '0')
            val b5 = bytes[5].toString(16).padStart(2, '0')
            "fe80::$b0$b1:${b2}ff:fe$b3:$b4$b5"
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Tests if TCP port [port] (default 50005) is open on [ip].
     * Supports scoped IPv6 (e.g. fe80::...%ap0), raw IPv6, and IPv4.
     */
    fun testPort(ip: String, port: Int = 50005, scope: Int? = null, timeoutMs: Int = 700): Boolean {
        return try {
            val trimmed = ip.trim()
            val cleanHost = if (trimmed.contains("%")) {
                trimmed
            } else if (trimmed.contains(":") && scope != null) {
                "$trimmed%$scope"
            } else {
                trimmed
            }

            val address = InetAddress.getByName(cleanHost)
            Socket().use { socket ->
                socket.connect(InetSocketAddress(address, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            // Secondary fallback using raw IP string without brackets
            try {
                val rawIp = ip.split("%")[0].trim('[', ']')
                val address = InetAddress.getByName(rawIp)
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(address, port), timeoutMs)
                    true
                }
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Resolves active Wi-Fi and Hotspot network interface names.
     */
    private fun getActiveWifiInterfaces(context: Context): Set<String> {
        val ifaces = mutableSetOf<String>()

        // 1. Query ConnectivityManager for active Wi-Fi & Hotspot networks
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            cm?.allNetworks?.forEach { network ->
                val caps = cm.getNetworkCapabilities(network) ?: return@forEach
                val link = cm.getLinkProperties(network) ?: return@forEach
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    link.interfaceName?.let { ifaces.add(it) }
                }
            }
        } catch (_: Exception) {}

        // 2. Query NetworkInterface enum for active Wi-Fi / AP interfaces
        try {
            val netIfaces = NetworkInterface.getNetworkInterfaces()
            while (netIfaces != null && netIfaces.hasMoreElements()) {
                val iface = netIfaces.nextElement()
                if (iface.isUp && !iface.isLoopback) {
                    val name = iface.name
                    if (name.startsWith("ap") || name.startsWith("wlan") ||
                        name.startsWith("swlan") || name.startsWith("p2p") ||
                        name.startsWith("softap")
                    ) {
                        ifaces.add(name)
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Common fallback interface names on Android OS
        ifaces.addAll(listOf("ap0", "wlan0", "swlan0", "wlan1", "p2p0"))
        return ifaces
    }

    /**
     * Pings the IPv6 all-nodes multicast address (ff02::1%iface).
     * All active devices on the Hotspot/Wi-Fi link respond with their link-local IPv6 address.
     */
    private fun runMulticastPing(iface: String): String {
        return try {
            val process = ProcessBuilder("ping6", "-c", "2", "-w", "2", "ff02::1%$iface")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            process.waitFor()
            output
        } catch (_: Exception) {
            // Fallback for devices using ping -6
            try {
                val process = ProcessBuilder("ping", "-6", "-c", "2", "-W", "2", "ff02::1%$iface")
                    .redirectErrorStream(true)
                    .start()
                val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
                process.waitFor()
                output
            } catch (_: Exception) {
                ""
            }
        }
    }

    /**
     * Parses ping output to extract responding neighbor link-local IPv6 addresses.
     */
    private fun parsePingOutput(output: String, fallbackIface: String): List<DiscoveredScooter> {
        val neighbors = mutableListOf<DiscoveredScooter>()
        if (output.isBlank()) return neighbors

        for (line in output.lines()) {
            val trimmed = line.trim()
            if (trimmed.contains("bytes from", ignoreCase = true)) {
                try {
                    val afterFrom = trimmed.substringAfter("from ", "").trim()
                    val ipToken = afterFrom.split("\\s+".toRegex()).firstOrNull() ?: ""
                    val cleanToken = ipToken.removeSuffix(":").trim()

                    if (cleanToken.isNotBlank() && cleanToken.contains(":")) {
                        val rawIp = cleanToken.substringBefore("%").trim()
                        val iface = if (cleanToken.contains("%")) {
                            cleanToken.substringAfter("%").trim()
                        } else {
                            fallbackIface
                        }

                        if (rawIp.isNotBlank() && rawIp.startsWith("fe80", ignoreCase = true)) {
                            val target = if (iface.isNotBlank()) "$rawIp%$iface" else rawIp
                            val scopeIndex = NetworkInterface.getByName(iface)?.index
                            neighbors.add(
                                DiscoveredScooter(
                                    ip = target,
                                    scope = scopeIndex,
                                    interfaceName = iface,
                                    source = "multicast_ping"
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        return neighbors
    }

    /**
     * Reads kernel neighbor table via `ip neigh` shell command as a fallback.
     */
    private fun scanIpNeigh(): List<DiscoveredScooter> {
        val result = mutableListOf<DiscoveredScooter>()
        try {
            val process = Runtime.getRuntime().exec("ip neigh")
            BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
                lines.forEach { line ->
                    val parts = line.split("\\s+".toRegex())
                    if (parts.size >= 4) {
                        val ip = parts[0]
                        val devIdx = parts.indexOf("dev")
                        val ifaceName = if (devIdx != -1 && devIdx + 1 < parts.size) parts[devIdx + 1] else ""
                        val scope = NetworkInterface.getByName(ifaceName)?.index

                        result.add(
                            DiscoveredScooter(
                                ip = ip,
                                scope = scope,
                                interfaceName = ifaceName,
                                source = "ip_neigh"
                            )
                        )

                        // If MAC address is present, also derive link-local IPv6
                        val lladdrIdx = parts.indexOf("lladdr")
                        if (lladdrIdx != -1 && lladdrIdx + 1 < parts.size) {
                            val mac = parts[lladdrIdx + 1]
                            val derivedIpv6 = macToIpv6(mac)
                            if (derivedIpv6 != null) {
                                val target = if (ifaceName.isNotBlank()) "$derivedIpv6%$ifaceName" else derivedIpv6
                                result.add(
                                    DiscoveredScooter(
                                        ip = target,
                                        scope = scope,
                                        interfaceName = ifaceName,
                                        source = "mac_derivation"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return result
    }

    /**
     * Determines the active Wi-Fi or Hotspot subnet prefix (e.g. "192.168.43." on tethering).
     */
    fun getLocalSubnet(): Pair<String, Int?>? {
        try {
            val ifaces = NetworkInterface.getNetworkInterfaces()
            while (ifaces != null && ifaces.hasMoreElements()) {
                val iface = ifaces.nextElement()
                if (iface.isUp && !iface.isLoopback) {
                    val addrs = iface.inetAddresses
                    while (addrs.hasMoreElements()) {
                        val addr = addrs.nextElement()
                        val host = addr.hostAddress ?: ""
                        if (host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+".toRegex()) && host != "127.0.0.1") {
                            val parts = host.split(".")
                            val prefix = "${parts[0]}.${parts[1]}.${parts[2]}."
                            return Pair(prefix, iface.index)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    /**
     * Sweeps local /24 subnet testing port 50005.
     */
    suspend fun sweepSubnetIpv4(subnetPrefix: String): DiscoveredScooter? = withContext(Dispatchers.IO) {
        val candidates = (2..254).map { "$subnetPrefix$it" }
        val jobs = candidates.map { candidateIp ->
            async {
                if (testPort(candidateIp, 50005, null, 350)) {
                    DiscoveredScooter(ip = candidateIp, scope = null, isPortOpen = true, source = "ipv4_sweep")
                } else null
            }
        }
        jobs.awaitAll().filterNotNull().firstOrNull()
    }

    /**
     * Master auto-discovery function matching Axiom's discovery mechanism:
     * 1. Multi-interface IPv6 multicast ping (ping6 ff02::1%iface).
     * 2. Direct parsing of neighbor ICMP echo responses.
     * 3. Concurrent verification of TCP Port 50005.
     * 4. Secondary fallback: ip neigh table & IPv4 subnet sweep.
     */
    suspend fun scanForScooter(context: Context): DiscoveredScooter? = withContext(Dispatchers.IO) {
        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wm?.createMulticastLock("S1ModsScanner")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (_: Exception) {}

        try {
            val candidateList = mutableListOf<DiscoveredScooter>()

            // 1. Primary: IPv6 Multicast Ping across all Wi-Fi / Hotspot interfaces
            val ifaces = getActiveWifiInterfaces(context)
            for (iface in ifaces) {
                val pingOutput = runMulticastPing(iface)
                val neighbors = parsePingOutput(pingOutput, iface)
                candidateList.addAll(neighbors)
            }

            // Test candidates found from multicast ping immediately
            if (candidateList.isNotEmpty()) {
                val pingJobs = candidateList.distinctBy { it.ip }.map { candidate ->
                    async {
                        val open = testPort(candidate.ip, 50005, candidate.scope, 800)
                        if (open) candidate.copy(isPortOpen = true) else null
                    }
                }
                val pingFound = pingJobs.awaitAll().filterNotNull().firstOrNull()
                if (pingFound != null) {
                    return@withContext pingFound
                }
            }

            // 2. Secondary fallback: ip neigh table
            val neighCandidates = scanIpNeigh()
            if (neighCandidates.isNotEmpty()) {
                val neighJobs = neighCandidates.distinctBy { it.ip }.map { candidate ->
                    async {
                        val open = testPort(candidate.ip, 50005, candidate.scope, 800)
                        if (open) candidate.copy(isPortOpen = true) else null
                    }
                }
                val neighFound = neighJobs.awaitAll().filterNotNull().firstOrNull()
                if (neighFound != null) {
                    return@withContext neighFound
                }
            }

            // 3. Tertiary fallback: IPv4 subnet sweep (Hotspot / local subnet)
            val subnetInfo = getLocalSubnet()
            if (subnetInfo != null) {
                val sweepFound = sweepSubnetIpv4(subnetInfo.first)
                if (sweepFound != null) {
                    return@withContext sweepFound
                }
            }

            null
        } finally {
            try { multicastLock?.release() } catch (_: Exception) {}
        }
    }
}
