package com.asjohn720.s1mods.adb

import android.content.Context
import dadb.AdbKeyPair
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * Manages ADB keypair generation and Dadb connection to the vehicle over TCP Port 5555.
 */
class AdbManager(private val context: Context) {

    private var activeDadb: Dadb? = null

    /**
     * Retrieves or generates an RSA 2048 keypair in standard ADB format.
     */
    fun getOrCreateAdbKeyPair(): Pair<AdbKeyPair, String> {
        val adbDir = File(context.filesDir, "adb").apply { mkdirs() }
        val privFile = File(adbDir, "adbkey")
        val pubFile = File(adbDir, "adbkey.pub")

        if (!privFile.exists() || !pubFile.exists()) {
            AdbKeyPair.generate(privFile, pubFile)
        }

        val keyPair = AdbKeyPair.read(privFile, pubFile)
        val pubKeyContent = pubFile.readText().trim()
        return Pair(keyPair, pubKeyContent)
    }

    /**
     * Connects to the vehicle's ADB daemon at [host]:[port].
     * Supports link-local IPv6 with scope (%scope) and automatic local IPv4 fallback.
     */
    suspend fun connect(
        host: String,
        port: Int = 5555,
        scope: Int? = null,
        fallbackIpv4: String? = null
    ): Dadb = withContext(Dispatchers.IO) {
        activeDadb?.close()
        val (keyPair, _) = getOrCreateAdbKeyPair()

        // Clean IPv6 brackets or formatting if needed
        var cleanHost = host.trim()
        if (cleanHost.lowercase().startsWith("ipv6:")) {
            cleanHost = cleanHost.substring(5).trim()
        }
        cleanHost = cleanHost.trim('[', ']')
        if (cleanHost.contains("]")) {
            cleanHost = cleanHost.split("]")[0].trimStart('[')
        }

        val scopeIndex = if (cleanHost.contains("%")) {
            cleanHost.split("%")[1].toIntOrNull() ?: scope
        } else {
            scope
        }
        val bareHost = cleanHost.split("%")[0]

        val candidates = mutableListOf<String>()

        if (bareHost.lowercase().startsWith("fe80")) {
            // Link-local IPv6 MUST have a scope identifier to avoid EINVAL on Linux/Android
            if (scopeIndex != null) {
                candidates.add("$bareHost%$scopeIndex")
            }
            // Also try with network interface name (e.g. ap0, wlan0)
            try {
                val ifaces = java.net.NetworkInterface.getNetworkInterfaces()
                while (ifaces.hasMoreElements()) {
                    val iface = ifaces.nextElement()
                    if (iface.index == scopeIndex || iface.name.startsWith("ap") || iface.name.startsWith("wlan")) {
                        candidates.add("$bareHost%${iface.name}")
                    }
                }
            } catch (_: Exception) {}

            if (fallbackIpv4 != null && fallbackIpv4.isNotBlank() && !fallbackIpv4.contains(":")) {
                candidates.add(fallbackIpv4.trim())
            }

            // Probe local subnet for scooter's IPv4 lease (e.g. 10.80.106.251)
            val detectedIpv4 = findScooterIpv4(port)
            if (detectedIpv4 != null && !candidates.contains(detectedIpv4)) {
                candidates.add(detectedIpv4)
            }
            candidates.add(bareHost)
        } else {
            candidates.add(bareHost)
        }

        var lastException: Exception? = null
        for (candidate in candidates.distinct()) {
            try {
                // Set explicit 2000ms connect and socket read timeout so it NEVER hangs
                val dadbInstance = Dadb.create(candidate, port, keyPair, 2000, 2000, false)
                // Test the connection immediately with an active shell ping bounded to 2 seconds
                val ping = withTimeoutOrNull(2000L) {
                    dadbInstance.shell("id -u")
                }
                if (ping != null && (ping.allOutput.isNotEmpty() || ping.exitCode == 0)) {
                    activeDadb = dadbInstance
                    return@withContext dadbInstance
                }
            } catch (e: Exception) {
                lastException = e
                if (e.message?.contains("unauthorized", ignoreCase = true) == true) {
                    // Socket connected successfully, waiting for authorization dialog
                    val dadbInstance = Dadb.create(candidate, port, keyPair, 2000, 2000, false)
                    activeDadb = dadbInstance
                    return@withContext dadbInstance
                }
            }
        }

        // If active test did not return immediately, use the best candidate (prefer IPv4 to avoid EINVAL)
        val best = candidates.firstOrNull { !it.contains(":") }
            ?: candidates.firstOrNull { it.contains("%") }
            ?: candidates.firstOrNull()
            ?: bareHost

        try {
            val dadbInstance = Dadb.create(best, port, keyPair, 3000, 3000, false)
            activeDadb = dadbInstance
            dadbInstance
        } catch (e: Exception) {
            throw lastException ?: e
        }
    }

    /**
     * Probes local Wi-Fi / Hotspot interface subnet for responsive ADB on port 5555.
     */
    private fun findScooterIpv4(port: Int): String? {
        try {
            val ifaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (ifaces.hasMoreElements()) {
                val iface = ifaces.nextElement()
                if (iface.name.startsWith("ap") || iface.name.startsWith("wlan")) {
                    for (addr in iface.inetAddresses) {
                        if (addr is java.net.Inet4Address && !addr.isLoopbackAddress) {
                            val hostAddr = addr.hostAddress ?: continue
                            val parts = hostAddr.split(".")
                            if (parts.size == 4) {
                                val prefix = "${parts[0]}.${parts[1]}.${parts[2]}."
                                val checkList = listOf("${prefix}251", "${prefix}192", "${prefix}2", "${prefix}3")
                                for (target in checkList) {
                                    val r = com.asjohn720.s1mods.discovery.NetworkScanner.testPortDetailed(target, port, null, 150)
                                    if (r.isOpen) return target
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    /**
     * Checks if a package is already installed on the vehicle.
     */
    suspend fun isPackageInstalled(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val client = activeDadb ?: return@withContext false
        val out = client.shell("pm list packages $packageName").allOutput
        out.contains("package:$packageName")
    }

    /**
     * Installs an APK file onto the vehicle via ADB streaming install (-r reinstall with data kept).
     */
    suspend fun installApk(apkFile: File): String = withContext(Dispatchers.IO) {
        val client = activeDadb ?: throw IllegalStateException("ADB not connected")
        client.install(apkFile)
        "Success"
    }

    /**
     * Runs shell command via ADB and returns stdout + stderr.
     */
    suspend fun shell(command: String): String = withContext(Dispatchers.IO) {
        val client = activeDadb ?: throw IllegalStateException("ADB not connected")
        val response = client.shell(command)
        (response.allOutput).trim()
    }

    /**
     * Grants essential permissions for Balls floating overlay.
     */
    suspend fun grantBallPermissions() = withContext(Dispatchers.IO) {
        val client = activeDadb ?: throw IllegalStateException("ADB not connected")
        val pkg = "com.asjohn720.ball"
        val svc = "$pkg/com.asjohn720.ball.core.QuickBallService"

        // 1. Overlay (SYSTEM_ALERT_WINDOW)
        client.shell("appops set $pkg SYSTEM_ALERT_WINDOW allow")

        // 2. Write Settings
        client.shell("appops set $pkg WRITE_SETTINGS allow")

        // 3. Accessibility Service
        client.shell("settings put secure enabled_accessibility_services $svc")
        client.shell("settings put secure accessibility_enabled 1")

        // 4. Launch Balls Main Activity
        client.shell("am start -n $pkg/.ui.MainActivity")

        // 5. Return to Dashboard Home
        client.shell("am start -n com.olaelectric.hmiclient/.MainActivity")
        client.shell("input keyevent 3")
    }

    /**
     * Grants permissions for Android Auto (OpenHeadUnit) via ADB:
     * - Microphone (RECORD_AUDIO)
     * - Location (ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
     * - Display over other apps (SYSTEM_ALERT_WINDOW)
     * - Storage (READ/WRITE_EXTERNAL_STORAGE, MANAGE_EXTERNAL_STORAGE)
     * - System settings & Wi-Fi/Bluetooth
     */
    suspend fun grantAndroidAutoPermissions() = withContext(Dispatchers.IO) {
        val client = activeDadb ?: throw IllegalStateException("ADB not connected")
        val pkg = "com.asjohn720.androidauto"

        // Overlay & Settings
        client.shell("appops set $pkg SYSTEM_ALERT_WINDOW allow")
        client.shell("appops set $pkg WRITE_SETTINGS allow")

        // Microphone
        client.shell("pm grant $pkg android.permission.RECORD_AUDIO")

        // Location
        client.shell("pm grant $pkg android.permission.ACCESS_FINE_LOCATION")
        client.shell("pm grant $pkg android.permission.ACCESS_COARSE_LOCATION")

        // Storage
        client.shell("pm grant $pkg android.permission.READ_EXTERNAL_STORAGE")
        client.shell("pm grant $pkg android.permission.WRITE_EXTERNAL_STORAGE")
        client.shell("appops set $pkg MANAGE_EXTERNAL_STORAGE allow")

        // Connectivity
        client.shell("pm grant $pkg android.permission.NEARBY_WIFI_DEVICES")
        client.shell("pm grant $pkg android.permission.BLUETOOTH_CONNECT")
        client.shell("pm grant $pkg android.permission.BLUETOOTH_SCAN")
    }

    fun disconnect() {
        try {
            activeDadb?.close()
        } catch (_: Exception) {}
        activeDadb = null
    }
}
