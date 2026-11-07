package com.asjohn720.s1mods.model

import android.content.Context
import android.graphics.Bitmap
import com.asjohn720.s1mods.adb.AdbManager
import com.asjohn720.s1mods.discovery.DiscoveredScooter
import com.asjohn720.s1mods.discovery.NetworkScanner
import com.asjohn720.s1mods.grpc.S1GrpcClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class SetupStep(val stepNum: Int, val title: String) {
    VERIFY_CONNECTION(1, "Detect Scooter & Connect"),
    PRE_CHECK_AND_WAKE(2, "Display Wake & Pre-Check"),
    UNLOCK_ADB(3, "In-Memory ADB Unlock"),
    INSTALL_APKS(4, "Install Selected Mod APKs"),
    GRANT_PERMISSIONS(5, "Grant System Permissions"),
    FINALIZE_HOME(6, "Initialize & Return to Home")
}

enum class ModApp(
    val id: String,
    val displayName: String,
    val assetFileName: String,
    val packageName: String,
    val isMandatory: Boolean,
    val description: String
) {
    BALLS(
        id = "balls",
        displayName = "Balls Navigation",
        assetFileName = "balls-1.1.0-lite-release.apk",
        packageName = "com.asjohn720.ball",
        isMandatory = true,
        description = "Floating Back, Home & Settings touch buttons (Required)"
    ),
    ANDROID_AUTO(
        id = "android_auto",
        displayName = "Android Auto",
        assetFileName = "com.asjohn720.androidauto.apk",
        packageName = "com.asjohn720.androidauto",
        isMandatory = false,
        description = "Wireless Android Auto projection for MoveOS"
    ),
    MUSIC(
        id = "music",
        displayName = "Music Mod",
        assetFileName = "musicmod-release.apk",
        packageName = "com.asjohn720.musicmod",
        isMandatory = false,
        description = "Ad-free streaming and offline music playback"
    )
}

data class ScooterInfo(
    val uid: String = "",
    val androidVersion: String = "",
    val buildId: String = "",
    val model: String = "",
    val scooterMode: String = ""
)

class ScooterManager(private val context: Context) {

    private val adbManager = AdbManager(context)
    private var grpcClient: S1GrpcClient? = null

    val scooterIp = MutableStateFlow("")
    val scooterScope = MutableStateFlow<Int?>(null)
    val isConnected = MutableStateFlow(false)

    val currentStep = MutableStateFlow<SetupStep?>(null)
    val isSetupRunning = MutableStateFlow(false)
    val setupSuccess = MutableStateFlow(false)
    val errorMessage = MutableStateFlow<String?>(null)

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    fun log(msg: String) {
        _logs.value = _logs.value + msg
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    /**
     * Initializes or updates the active gRPC connection.
     */
    suspend fun connect(ip: String = scooterIp.value, scope: Int? = scooterScope.value) = withContext(Dispatchers.IO) {
        log("[*] Connecting to vehicle at $ip (Scope: ${scope ?: "auto"}) ...")
        grpcClient?.close()
        val client = S1GrpcClient(host = ip, port = 50005, scope = scope)
        client.connect(timeoutSeconds = 6L)
        grpcClient = client
        scooterIp.value = ip
        scooterScope.value = scope
        isConnected.value = true
        log("[+] Successfully connected to MoveOS Diagnostics daemon (Port 50005)!")
    }

    /**
     * Auto-discovers the scooter on the local Wi-Fi / Hotspot network.
     */
    suspend fun autoDiscover(): DiscoveredScooter? = withContext(Dispatchers.IO) {
        log("[*] Scanning network for vehicle...")
        val discovered = NetworkScanner.scanForScooter(context)
        if (discovered != null) {
            scooterIp.value = discovered.ip
            scooterScope.value = discovered.scope
            isConnected.value = true
            log("[+] Discovered Vehicle: ${discovered.ip} (Scope: ${discovered.scope ?: "none"}, Source: ${discovered.source})")
        } else {
            log("[!] Could not auto-detect vehicle on port 50005.")
            log("    Tip: Check 'Connected Devices' in your Hotspot settings and enter the MAC or IP.")
        }
        discovered
    }

    /**
     * Converts a MAC address from Hotspot settings to IPv6 link-local address.
     */
    fun setFromMac(mac: String): Boolean {
        val ipv6 = NetworkScanner.macToIpv6(mac)
        if (ipv6 != null) {
            scooterIp.value = ipv6
            log("[+] Calculated IPv6 from MAC ($mac) -> $ipv6")
            return true
        }
        return false
    }

    /**
     * Quick test to verify if port 50005 is reachable.
     */
    suspend fun testConnection(ip: String = scooterIp.value, scope: Int? = scooterScope.value): Boolean = withContext(Dispatchers.IO) {
        log("[*] Testing port 50005 on $ip...")
        val open = NetworkScanner.testPort(ip, 50005, scope, 1000)
        isConnected.value = open
        if (open) {
            log("[+] Port 50005 is OPEN and responsive!")
        } else {
            log("[!] Port 50005 is unreachable on $ip. Ensure scooter screen is awake!")
        }
        open
    }

    /**
     * Executes the complete 1-Click Automated Setup Wizard.
     * @param selectedApps The set of applications chosen to be installed.
     */
    suspend fun runFullSetup(selectedApps: Set<ModApp> = ModApp.values().toSet()) = withContext(Dispatchers.IO) {
        isSetupRunning.value = true
        setupSuccess.value = false
        errorMessage.value = null
        clearLogs()

        log("==================================================")
        log("       S1MODS AUTOMATED INSTALLATION WIZARD       ")
        log("==================================================")
        log("[*] Selected apps to install: ${selectedApps.joinToString { it.displayName }}")

        try {
            // Step 1: Verify Connection
            currentStep.value = SetupStep.VERIFY_CONNECTION
            log("\n[1/6] Verifying connection to vehicle...")
            if (scooterIp.value.isBlank()) {
                log("[*] No address entered. Attempting auto-discovery...")
                val found = autoDiscover()
                if (found == null || scooterIp.value.isBlank()) {
                    throw IllegalStateException("No vehicle detected. Please tap 'Scan' or edit to enter your scooter's IP/MAC address.")
                }
            }
            if (grpcClient == null || !isConnected.value) {
                connect()
            }
            log("[+] gRPC link confirmed alive.")

            // Step 2: Wake & Pre-Check
            currentStep.value = SetupStep.PRE_CHECK_AND_WAKE
            log("\n[2/6] Waking display...")
            grpcClient?.wakeScreen()
            log("[+] Screen awake. Ensure side stand is UP and screen stays active.")

            // Step 3: Unlock ADB (In-Memory RAM Bind Mount)
            currentStep.value = SetupStep.UNLOCK_ADB
            log("\n[3/6] Authorizing ADB key via in-memory RAM mount...")
            val (_, pubKey) = adbManager.getOrCreateAdbKeyPair()
            val unlocked = grpcClient?.unlockAdb(pubKey) ?: false
            if (!unlocked) {
                log("[*] Warning: bind mount script returned non-zero, trying ADB connect...")
            }
            log("[*] Connecting Kotlin ADB client over TCP Port 5555...")
            adbManager.connect(scooterIp.value, 5555)
            log("[+] ADB connected successfully as 'device'!")

            // Step 4: Extract and Install Chosen APKs
            currentStep.value = SetupStep.INSTALL_APKS
            log("\n[4/6] Installing selected Mod APKs...")

            for (app in selectedApps) {
                val isInstalled = adbManager.isPackageInstalled(app.packageName)
                if (isInstalled) {
                    log("[*] ${app.displayName} already installed on vehicle. Clean uninstalling (${app.packageName})...")
                    val uninstRes = adbManager.uninstallPackage(app.packageName)
                    log("    [+] Clean uninstall: $uninstRes")
                } else {
                    log("[*] ${app.displayName} not found on vehicle. Direct fresh install...")
                }
                installAssetApk(app.assetFileName, app.displayName)
            }

            // Step 5: Grant Permissions
            currentStep.value = SetupStep.GRANT_PERMISSIONS
            log("\n[5/6] Granting required permissions via ADB...")
            if (selectedApps.contains(ModApp.BALLS)) {
                log("[*] Configuring Balls Navigation (Overlay, Settings & Accessibility)...")
                adbManager.grantBallPermissions()
                log("[+] Balls Navigation permissions granted & initialized.")
            }

            if (selectedApps.contains(ModApp.ANDROID_AUTO)) {
                log("[*] Configuring Android Auto permissions (Microphone, Location, Overlay, Storage)...")
                adbManager.grantAndroidAutoPermissions()
                log("[+] Android Auto permissions granted.")
            }

            // Step 6: Finalize & Return to Home
            currentStep.value = SetupStep.FINALIZE_HOME
            log("\n[6/6] Returning vehicle to Dashboard Home...")
            openDashboardHome()

            log("\n==================================================")
            log("     CONGRATULATIONS! ALL MODS INSTALLED!         ")
            log("==================================================")
            log("Installation complete. Your selected apps are ready on screen.")
            setupSuccess.value = true

        } catch (e: Exception) {
            val err = e.localizedMessage ?: e.toString()
            log("\n[!] Setup failed: $err")
            errorMessage.value = err
            setupSuccess.value = false
        } finally {
            isSetupRunning.value = false
        }
    }

    /**
     * Extracts an APK bundled in assets to temporary storage and installs it via ADB.
     */
    private suspend fun installAssetApk(assetFileName: String, friendlyName: String) = withContext(Dispatchers.IO) {
        log("[*] Preparing $friendlyName...")
        val tempFile = File(context.cacheDir, assetFileName)
        context.assets.open(assetFileName).use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        val sizeMb = String.format("%.1f", tempFile.length() / (1024.0 * 1024.0))
        log("    Streaming $assetFileName ($sizeMb MB) via ADB...")
        val result = adbManager.installApk(tempFile)
        tempFile.delete()
        log("    [+] $friendlyName result: $result")
    }

    /**
     * Installs any user-selected APK file from phone storage.
     */
    suspend fun installCustomApk(apkFile: File): String = withContext(Dispatchers.IO) {
        val sizeMb = String.format("%.1f", apkFile.length() / (1024.0 * 1024.0))
        log("[*] Installing custom APK: ${apkFile.name} ($sizeMb MB)...")
        if (grpcClient == null || !isConnected.value) {
            connect()
        }
        val (_, pubKey) = adbManager.getOrCreateAdbKeyPair()
        grpcClient?.unlockAdb(pubKey)
        adbManager.connect(scooterIp.value, 5555)
        val res = adbManager.installApk(apkFile)
        log("[+] Custom APK installed: $res")
        res
    }

    /**
     * Launches specific Android Settings screen on the scooter and returns execution confirmation.
     */
    suspend fun openSettings(action: String): Boolean = withContext(Dispatchers.IO) {
        log("[*] Launching $action...")
        val client = grpcClient ?: throw IllegalStateException("Not connected to vehicle")
        val (exitCode, _) = client.shell("am start -a $action", 15L)
        exitCode in 0..1
    }

    /**
     * Returns scooter to Speedometer Home dashboard and returns confirmation.
     */
    suspend fun openDashboardHome(): Boolean = withContext(Dispatchers.IO) {
        log("[*] Bringing Dashboard Home to front...")
        val client = grpcClient ?: throw IllegalStateException("Not connected to vehicle")
        val (exitCode, _) = client.shell("am start -n com.olaelectric.hmiclient/.MainActivity", 15L)
        client.shell("input keyevent 3", 10L)
        exitCode in 0..1
    }

    suspend fun openOlaHome() = openDashboardHome()

    /**
     * Wakes the screen safely without modifying persistent power stay-on.
     */
    suspend fun wakeScreen() = withContext(Dispatchers.IO) {
        log("[*] Waking vehicle display...")
        grpcClient?.wakeScreen()
    }

    /**
     * Captures a screenshot from the scooter.
     */
    suspend fun captureScreenshot(): Bitmap = withContext(Dispatchers.IO) {
        log("[*] Capturing vehicle display screenshot...")
        val client = grpcClient ?: throw IllegalStateException("Not connected to vehicle")
        client.takeScreenshot()
    }

    /**
     * Executes arbitrary root shell command and returns output.
     */
    suspend fun runRootShell(command: String): String = withContext(Dispatchers.IO) {
        val client = grpcClient ?: throw IllegalStateException("Not connected to vehicle")
        val (_, out) = client.shell(command, 30L)
        out
    }

    /**
     * Fetches technical specifications and system properties of the vehicle.
     */
    suspend fun fetchScooterInfo(): ScooterInfo = withContext(Dispatchers.IO) {
        val client = grpcClient ?: throw IllegalStateException("Not connected to vehicle")
        val uid = client.shell("id -u").second.trim()
        val android = client.shell("getprop ro.build.version.release").second.trim()
        val build = client.shell("getprop ro.build.display.id").second.trim()
        val model = client.shell("getprop ro.product.model").second.trim()
        val mode = client.shell("getprop persist.ola.security.scootermode").second.trim()
        ScooterInfo(uid = uid, androidVersion = android, buildId = build, model = model, scooterMode = mode)
    }

    fun disconnect() {
        grpcClient?.close()
        adbManager.disconnect()
        isConnected.value = false
    }
}
