package com.asjohn720.s1mods.adb

import android.content.Context
import dadb.AdbKeyPair
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
     */
    suspend fun connect(host: String, port: Int = 5555): Dadb = withContext(Dispatchers.IO) {
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

        val dadbInstance = Dadb.create(cleanHost, port, keyPair)
        activeDadb = dadbInstance
        dadbInstance
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
