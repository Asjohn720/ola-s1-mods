package com.asjohn720.olamods.discovery

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
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
    val source: String = ""
)

object NetworkScanner {

    private var multicastLock: WifiManager.MulticastLock? = null

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
     */
    fun testPort(ip: String, port: Int = 50005, scope: Int? = null, timeoutMs: Int = 700): Boolean {
        return try {
            val socket = Socket()
            val cleanIp = ip.split("%")[0].trim('[', ']')
            val address = if (cleanIp.contains(":") && scope != null) {
                Inet6Address.getByAddress(null, InetAddress.getByName(cleanIp).address, scope)
            } else {
                InetAddress.getByName(cleanIp)
            }
            socket.connect(InetSocketAddress(address, port), timeoutMs)
            socket.close()
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Reads the Android kernel neighbor table via `ip neigh` shell command.
     */
    private fun scanIpNeigh(): List<DiscoveredScooter> {
        val result = mutableListOf<DiscoveredScooter>()
        try {
            val process = Runtime.getRuntime().exec("ip neigh")
            BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
                lines.forEach { line ->
                    // Example: "192.168.43.45 dev ap0 lladdr aa:bb:cc:dd:ee:ff REACHABLE"
                    // Example: "fe80::1234:5678:9abc:def0 dev ap0 lladdr aa:bb:cc:dd:ee:ff REACHABLE"
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

                        // Check if MAC is available to derive IPv6 if this was an IPv4 entry
                        val lladdrIdx = parts.indexOf("lladdr")
                        if (lladdrIdx != -1 && lladdrIdx + 1 < parts.size) {
                            val mac = parts[lladdrIdx + 1]
                            val derivedIpv6 = macToIpv6(mac)
                            if (derivedIpv6 != null) {
                                result.add(
                                    DiscoveredScooter(
                                        ip = derivedIpv6,
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
     * Reads /proc/net/arp fallback (if accessible on older or unrestricted ROMs).
     */
    private fun scanProcArp(): List<DiscoveredScooter> {
        val result = mutableListOf<DiscoveredScooter>()
        try {
            val arp = File("/proc/net/arp")
            if (arp.exists() && arp.canRead()) {
                BufferedReader(FileReader(arp)).useLines { lines ->
                    lines.drop(1).forEach { line ->
                        val parts = line.split("\\s+".toRegex())
                        if (parts.size >= 6 && parts[0].isNotEmpty() && parts[0] != "0.0.0.0") {
                            val ip = parts[0]
                            val mac = parts[3]
                            val iface = parts[5]
                            val scope = NetworkInterface.getByName(iface)?.index

                            result.add(DiscoveredScooter(ip = ip, scope = scope, interfaceName = iface, source = "proc_arp"))

                            val derivedIpv6 = macToIpv6(mac)
                            if (derivedIpv6 != null) {
                                result.add(DiscoveredScooter(ip = derivedIpv6, scope = scope, interfaceName = iface, source = "arp_mac"))
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
            while (ifaces.hasMoreElements()) {
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
     * Fast-sweeps the local /24 subnet (e.g. 192.168.43.2 to 254) testing port 50005.
     */
    suspend fun sweepSubnetIpv4(subnetPrefix: String): DiscoveredScooter? = withContext(Dispatchers.IO) {
        val candidates = (2..254).map { "$subnetPrefix$it" }
        val jobs = candidates.map { candidateIp ->
            async {
                if (testPort(candidateIp, 50005, null, 400)) {
                    DiscoveredScooter(ip = candidateIp, scope = null, isPortOpen = true, source = "ipv4_sweep")
                } else null
            }
        }
        jobs.awaitAll().filterNotNull().firstOrNull()
    }

    /**
     * Master auto-discovery function trying multiple discovery vectors:
     * 1. ip neigh output (direct IPv6 & MAC derivation)
     * 2. /proc/net/arp (if accessible)
     * 3. Local IPv4 Hotspot Subnet sweep (testing port 50005)
     */
    suspend fun scanForScooter(context: Context): DiscoveredScooter? = withContext(Dispatchers.IO) {
        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wm?.createMulticastLock("OlaModsScanner")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (_: Exception) {}

        val candidateList = mutableListOf<DiscoveredScooter>()

        // 1. Check ip neigh
        candidateList.addAll(scanIpNeigh())

        // 2. Check /proc/net/arp
        candidateList.addAll(scanProcArp())

        // Test all discovered candidates from neighbor tables on port 50005
        val neighborJobs = candidateList.distinctBy { it.ip }.map { candidate ->
            async {
                val open = testPort(candidate.ip, 50005, candidate.scope, 800)
                if (open) candidate.copy(isPortOpen = true) else null
            }
        }
        val neighborFound = neighborJobs.awaitAll().filterNotNull().firstOrNull()
        if (neighborFound != null) {
            try { multicastLock?.release() } catch (_: Exception) {}
            return@withContext neighborFound
        }

        // 3. Subnet sweep on local IPv4 (Hotspot / Wi-Fi subnet)
        val subnetInfo = getLocalSubnet()
        if (subnetInfo != null) {
            val sweepFound = sweepSubnetIpv4(subnetInfo.first)
            if (sweepFound != null) {
                try { multicastLock?.release() } catch (_: Exception) {}
                return@withContext sweepFound
            }
        }

        try { multicastLock?.release() } catch (_: Exception) {}
        null
    }
}
