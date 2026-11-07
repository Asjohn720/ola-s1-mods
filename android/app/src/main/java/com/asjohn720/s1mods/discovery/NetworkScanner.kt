package com.asjohn720.s1mods.discovery

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
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

data class PortTestResult(
    val isOpen: Boolean,
    val errorDetails: String? = null,
    val timeTakenMs: Long = 0
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
     * Detailed TCP port probe returning elapsed time and exact exception if failed.
     */
    fun testPortDetailed(ip: String, port: Int = 50005, scope: Int? = null, timeoutMs: Int = 1200): PortTestResult {
        val start = System.currentTimeMillis()
        var socket: Socket? = null
        return try {
            socket = Socket()
            val cleanIp = ip.split("%")[0].trim('[', ']')
            val address = if (cleanIp.contains(":") && scope != null) {
                Inet6Address.getByAddress(null, InetAddress.getByName(cleanIp).address, scope)
            } else {
                InetAddress.getByName(cleanIp)
            }
            socket.connect(InetSocketAddress(address, port), timeoutMs)
            val elapsed = System.currentTimeMillis() - start
            PortTestResult(isOpen = true, timeTakenMs = elapsed)
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - start
            val msg = "${e.javaClass.simpleName}: ${e.message ?: "Failed"}"
            PortTestResult(isOpen = false, errorDetails = msg, timeTakenMs = elapsed)
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Tests if TCP port [port] (default 50005) is open on [ip].
     */
    fun testPort(ip: String, port: Int = 50005, scope: Int? = null, timeoutMs: Int = 800): Boolean {
        return testPortDetailed(ip, port, scope, timeoutMs).isOpen
    }

    /**
     * Diagnostic helper: returns a list of all active non-loopback network interfaces and their IPs.
     */
    fun getNetworkInterfacesInfo(): List<String> {
        val list = mutableListOf<String>()
        try {
            val ifaces = NetworkInterface.getNetworkInterfaces()
            while (ifaces.hasMoreElements()) {
                val iface = ifaces.nextElement()
                if (iface.isUp && !iface.isLoopback) {
                    val ips = iface.inetAddresses.toList().map { it.hostAddress ?: "" }
                    list.add("${iface.name} [idx:${iface.index}] -> ${ips.joinToString(", ")}")
                }
            }
        } catch (e: Exception) {
            list.add("Error listing interfaces: ${e.message}")
        }
        return list
    }

    /**
     * Diagnostic helper: reads raw output of `ip neigh` kernel neighbor table.
     */
    fun getRawIpNeighLines(): List<String> {
        val list = mutableListOf<String>()
        try {
            val process = Runtime.getRuntime().exec("ip neigh")
            BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) list.add(line.trim())
                }
            }
        } catch (e: Exception) {
            list.add("Error reading ip neigh: ${e.message}")
        }
        return list
    }

    /**
     * Wakes up the Linux kernel ARP cache by blasting fast UDP probes across the subnet.
     * When the kernel transmits an IP packet, it automatically issues an ARP WHO-HAS request,
     * forcing connected devices (including the scooter) to answer with their MAC address.
     */
    fun warmupArpCache(subnetPrefix: String) {
        try {
            val socket = DatagramSocket()
            socket.broadcast = true
            val dummy = byteArrayOf(0x00)

            // Send subnet broadcast
            try {
                val bcast = InetAddress.getByName("${subnetPrefix}255")
                socket.send(DatagramPacket(dummy, dummy.size, bcast, 9))
            } catch (_: Exception) {}

            // Rapid lightweight UDP packet burst across candidate range
            for (i in 2..254) {
                try {
                    val addr = InetAddress.getByName("$subnetPrefix$i")
                    socket.send(DatagramPacket(dummy, dummy.size, addr, 9))
                } catch (_: Exception) {}
            }
            socket.close()
        } catch (_: Exception) {}
    }

    /**
     * Wakes up IPv6 neighbor discovery by sending an all-nodes link-local multicast probe (ff02::1).
     */
    fun probeIpv6Multicast() {
        try {
            val ifaces = NetworkInterface.getNetworkInterfaces()
            val socket = DatagramSocket()
            val dummy = byteArrayOf(0x00)
            while (ifaces.hasMoreElements()) {
                val iface = ifaces.nextElement()
                if (iface.isUp && !iface.isLoopback) {
                    try {
                        val addr = InetAddress.getByName("ff02::1%${iface.name}")
                        socket.send(DatagramPacket(dummy, dummy.size, addr, 9))
                    } catch (_: Exception) {}
                }
            }
            socket.close()
        } catch (_: Exception) {}
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
     * Resolves an IPv4 address manually entered by the user (as read from scooter Wi-Fi screen).
     * Warms up ARP, extracts the MAC address, derives IPv6 SLAAC, and tests port 50005.
     * Prioritizes IPv6 (customer-mode bypass) and falls back to IPv4.
     */
    suspend fun resolveIpv4Address(ipv4: String): DiscoveredScooter = withContext(Dispatchers.IO) {
        val clean = ipv4.trim()

        // 1. Warm up ARP for this target
        try {
            val s = DatagramSocket()
            val dummy = byteArrayOf(0x00)
            s.send(DatagramPacket(dummy, dummy.size, InetAddress.getByName(clean), 50005))
            s.close()
        } catch (_: Exception) {}

        delay(120)

        // 2. Query neighbor table for MAC address
        var foundMac: String? = null
        var ifaceScope: Int? = null
        var ifaceName = ""

        val rawNeigh = getRawIpNeighLines()
        for (line in rawNeigh) {
            val parts = line.split("\\s+".toRegex())
            if (parts.isNotEmpty() && parts[0] == clean) {
                val devIdx = parts.indexOf("dev")
                if (devIdx != -1 && devIdx + 1 < parts.size) {
                    ifaceName = parts[devIdx + 1]
                    ifaceScope = NetworkInterface.getByName(ifaceName)?.index
                }
                val lladdrIdx = parts.indexOf("lladdr")
                if (lladdrIdx != -1 && lladdrIdx + 1 < parts.size) {
                    foundMac = parts[lladdrIdx + 1]
                    break
                }
            }
        }

        if (foundMac != null) {
            val derivedIpv6 = macToIpv6(foundMac)
            if (derivedIpv6 != null) {
                val probe6 = testPortDetailed(derivedIpv6, 50005, ifaceScope, 1200)
                if (probe6.isOpen) {
                    return@withContext DiscoveredScooter(
                        ip = derivedIpv6,
                        scope = ifaceScope,
                        isPortOpen = true,
                        interfaceName = ifaceName,
                        source = "manual_ipv4_eui64"
                    )
                }
            }
        }

        // Test direct IPv4 reachability
        val probe4 = testPortDetailed(clean, 50005, null, 1200)
        DiscoveredScooter(
            ip = clean,
            scope = null,
            isPortOpen = probe4.isOpen,
            interfaceName = ifaceName,
            source = "manual_ipv4"
        )
    }

    /**
     * Resolves any manual user input (IPv4, IPv6, or MAC address) into a working vehicle address.
     */
    suspend fun resolveVehicleAddress(rawInput: String): DiscoveredScooter = withContext(Dispatchers.IO) {
        val input = rawInput.trim()

        // 1. MAC Address input (e.g. 3a:2b:7c:06:5d:a8 or 3a-2b-7c-06-5d-a8)
        val macIpv6 = macToIpv6(input)
        if (macIpv6 != null) {
            val subnetInfo = getLocalSubnet()
            val probe = testPortDetailed(macIpv6, 50005, subnetInfo?.second, 1200)
            return@withContext DiscoveredScooter(
                ip = macIpv6,
                scope = subnetInfo?.second,
                isPortOpen = probe.isOpen,
                source = "manual_mac"
            )
        }

        // 2. Direct IPv6 input (e.g. fe80::... or fe80::...%wlan0)
        if (input.contains(":")) {
            var addr = input
            var parsedScope: Int? = null
            if (addr.contains("%")) {
                val parts = addr.split("%")
                addr = parts[0]
                parsedScope = parts[1].toIntOrNull() ?: NetworkInterface.getByName(parts[1])?.index
            }
            if (parsedScope == null) {
                parsedScope = getLocalSubnet()?.second
            }
            val probe = testPortDetailed(addr, 50005, parsedScope, 1200)
            return@withContext DiscoveredScooter(
                ip = addr,
                scope = parsedScope,
                isPortOpen = probe.isOpen,
                source = "manual_ipv6"
            )
        }

        // 3. IPv4 address input (e.g. 192.168.43.150)
        return@withContext resolveIpv4Address(input)
    }

    /**
     * Master auto-discovery function trying multiple discovery vectors:
     * 1. Active ARP broadcast & IPv6 multicast cache warmup
     * 2. ip neigh output (direct IPv6 & MAC derivation)
     * 3. /proc/net/arp (if accessible)
     * 4. Local IPv4 Hotspot Subnet sweep (testing port 50005)
     */
    suspend fun scanForScooter(context: Context, logCallback: ((String) -> Unit)? = null): DiscoveredScooter? = withContext(Dispatchers.IO) {
        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wm?.createMulticastLock("S1ModsScanner")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (_: Exception) {}

        val subnetInfo = getLocalSubnet()
        if (subnetInfo != null) {
            logCallback?.invoke("[*] Warming up ARP cache on subnet ${subnetInfo.first}x...")
            warmupArpCache(subnetInfo.first)
        }

        logCallback?.invoke("[*] Sending IPv6 multicast discovery probe (ff02::1)...")
        probeIpv6Multicast()

        // Give the kernel 150ms to receive incoming ARP replies from connected devices
        delay(150)

        logCallback?.invoke("[*] Discovery Vector 1: Querying refreshed kernel neighbor table...")
        val candidateList = mutableListOf<DiscoveredScooter>()

        // 1. Check ip neigh
        val neighList = scanIpNeigh()
        logCallback?.invoke("    Found ${neighList.size} neighbor entries.")
        candidateList.addAll(neighList)

        // 2. Check /proc/net/arp
        val arpList = scanProcArp()
        if (arpList.isNotEmpty()) {
            logCallback?.invoke("    Found ${arpList.size} ARP entries.")
            candidateList.addAll(arpList)
        }

        // Prioritize IPv6 candidates (derived from MAC) first to bypass Customer mode iptables
        val uniqueCandidates = candidateList
            .distinctBy { it.ip }
            .sortedByDescending { it.ip.contains(":") } // IPv6 first

        logCallback?.invoke("[*] Probing ${uniqueCandidates.size} candidate addresses on TCP Port 50005...")
        for (candidate in uniqueCandidates) {
            val probe = testPortDetailed(candidate.ip, 50005, candidate.scope, 1000)
            if (probe.isOpen) {
                logCallback?.invoke("    [+] Port 50005 OPEN on ${candidate.ip} (${probe.timeTakenMs}ms) via ${candidate.source}!")
                try { multicastLock?.release() } catch (_: Exception) {}
                return@withContext candidate.copy(isPortOpen = true)
            } else {
                logCallback?.invoke("    [-] ${candidate.ip}: ${probe.errorDetails ?: "Closed"}")
            }
        }

        // 3. Subnet sweep on local IPv4 (Hotspot / Wi-Fi subnet)
        if (subnetInfo != null) {
            logCallback?.invoke("[*] Discovery Vector 2: Fast-probing local subnet ${subnetInfo.first}x...")
            val sweepFound = sweepSubnetIpv4(subnetInfo.first)
            if (sweepFound != null) {
                logCallback?.invoke("    [+] Discovered responsive host via subnet sweep: ${sweepFound.ip}")
                try { multicastLock?.release() } catch (_: Exception) {}
                return@withContext sweepFound
            }
        }

        try { multicastLock?.release() } catch (_: Exception) {}
        null
    }
}
