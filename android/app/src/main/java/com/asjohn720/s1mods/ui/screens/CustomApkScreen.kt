package com.asjohn720.s1mods.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asjohn720.s1mods.model.ModApp
import com.asjohn720.s1mods.model.OnlineApp
import com.asjohn720.s1mods.model.OnlineAppCatalog
import com.asjohn720.s1mods.model.RamInfo
import com.asjohn720.s1mods.model.ScooterManager
import com.asjohn720.s1mods.ui.theme.*
import com.asjohn720.s1mods.util.ApkDownloader
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class AppConfirmation(
    val title: String,
    val message: String,
    val confirmButtonText: String,
    val isDestructive: Boolean = false,
    val onConfirm: () -> Unit
)

@Composable
fun CustomApkScreen(manager: ScooterManager) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val isConnected by manager.isConnected.collectAsState()

    // Scooter Installed Packages State
    var installedPackages by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isCheckingPackages by remember { mutableStateOf(false) }

    // Live RAM Telemetry State
    var ramInfo by remember { mutableStateOf<RamInfo?>(null) }
    var isLoadingRam by remember { mutableStateOf(false) }

    // Active operation indicator per app: e.g. "balls:installing", "balls:opening", "org.mozilla.focus:uninstalling"
    var activeAppAction by remember { mutableStateOf<String?>(null) }

    // Action Confirmation Dialog State
    var pendingConfirmation by remember { mutableStateOf<AppConfirmation?>(null) }

    // Download state for online catalog
    var downloadingAppId by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadStatusText by remember { mutableStateOf("") }

    // Custom Sideload APK State
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf<String?>(null) }
    var selectedSizeMb by remember { mutableStateOf<Double?>(null) }
    var isSideloadInstalling by remember { mutableStateOf(false) }
    var sideloadResult by remember { mutableStateOf<String?>(null) }

    // Local download directory
    val downloadDir = remember {
        File(context.filesDir, "downloaded_apks").apply { mkdirs() }
    }

    // Comprehensive refresh function: loads live installed packages & RAM telemetry
    fun refreshAllData() {
        coroutineScope.launch {
            if (isConnected) {
                isCheckingPackages = true
                isLoadingRam = true
                try {
                    installedPackages = manager.fetchInstalledPackages()
                } catch (_: Exception) {}
                try {
                    ramInfo = manager.fetchRamInfo()
                } catch (_: Exception) {}
                isCheckingPackages = false
                isLoadingRam = false
            }
        }
    }

    // Auto-fetch latest information immediately when entering App Manager tab
    LaunchedEffect(isConnected) {
        if (isConnected) {
            refreshAllData()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    selectedName = cursor.getString(nameIndex)
                    val sizeBytes = cursor.getLong(sizeIndex)
                    val sizeMb = sizeBytes.toDouble() / (1024.0 * 1024.0)
                    selectedSizeMb = sizeMb
                    manager.log("[*] Selected local APK file: $selectedName (${String.format("%.2f", sizeMb)} MB)")
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "App Manager",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextMuted
        )

        // 1. DYNAMIC LIVE RAM USAGE CARD (Green >= 200MB, Orange 150-200MB, Red < 150MB)
        val freeMb = ramInfo?.freeMb ?: 0
        val ramColor = when {
            ramInfo == null -> CardBorder
            freeMb >= 200 -> AccentMint          // Green: >= 200 MB
            freeMb >= 150 -> StatusWarning       // Orange: 150 - 200 MB
            else -> StatusDanger                 // Red: < 150 MB
        }
        val ramBgColor = when {
            ramInfo == null -> SurfaceDark
            freeMb >= 200 -> AccentMint.copy(alpha = 0.08f)
            freeMb >= 150 -> StatusWarning.copy(alpha = 0.08f)
            else -> StatusDanger.copy(alpha = 0.08f)
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, if (ramInfo != null) ramColor.copy(alpha = 0.6f) else CardBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = if (ramInfo != null) ramBgColor else SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header Row: Memory Icon + Title + Reload Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (ramInfo != null) ramColor.copy(alpha = 0.15f) else SurfaceVariantDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Memory,
                                contentDescription = null,
                                tint = if (ramInfo != null) ramColor else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Head Unit Live RAM",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (ramInfo != null) {
                                    when {
                                        freeMb >= 200 -> "● Optimal Free Memory (>= 200 MB)"
                                        freeMb >= 150 -> "▲ Moderate Memory (150-200 MB)"
                                        else -> "⚠️ Critical Low Memory (< 150 MB)"
                                    }
                                } else if (isConnected) {
                                    "Connected. Tap reload to read /proc/meminfo"
                                } else {
                                    "Connect scooter to inspect live RAM"
                                },
                                fontSize = 11.sp,
                                color = if (ramInfo != null) ramColor else TextMuted
                            )
                        }
                    }

                    // Live Reload Button
                    IconButton(
                        onClick = {
                            manager.log("[*] Manually refreshing vehicle RAM & package status...")
                            refreshAllData()
                        },
                        enabled = isConnected && !isLoadingRam
                    ) {
                        if (isLoadingRam) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = if (ramInfo != null) ramColor else AccentSky,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Reload RAM",
                                tint = if (isConnected) (if (ramInfo != null) ramColor else AccentSky) else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (ramInfo != null) {
                    val ram = ramInfo!!
                    // 3 Column Metrics: Total RAM, Used RAM, Free RAM
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Total RAM
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Total RAM", fontSize = 10.sp, color = TextMuted)
                            Text("${ram.totalMb} MB", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("~${String.format("%.1f", ram.totalMb / 1024.0)} GB", fontSize = 10.sp, color = TextSecondary)
                        }

                        // Used RAM
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Used RAM", fontSize = 10.sp, color = TextMuted)
                            Text("${ram.usedMb} MB", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            val usedPct = if (ram.totalMb > 0) ((ram.usedMb.toFloat() / ram.totalMb.toFloat()) * 100).toInt() else 0
                            Text("$usedPct% Used", fontSize = 10.sp, color = TextSecondary)
                        }

                        // Free RAM (Highlighted in dynamic color!)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Free RAM", fontSize = 10.sp, color = ramColor, fontWeight = FontWeight.SemiBold)
                            Text("${ram.freeMb} MB", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ramColor)
                            Text(
                                if (freeMb >= 200) "Healthy" else if (freeMb >= 150) "Moderate" else "Low Free",
                                fontSize = 10.sp,
                                color = ramColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Memory Progress Bar
                    val usedFraction = if (ram.totalMb > 0) (ram.usedMb.toFloat() / ram.totalMb.toFloat()).coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { usedFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = ramColor,
                        trackColor = SurfaceHighlight
                    )
                } else {
                    Text(
                        text = if (isConnected) "Tap refresh to read" else "Not connected",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }

        // 2. SHORT & CONCISE HARDWARE MEMORY NOTICE
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = StatusWarning,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Heavy apps use a lot of RAM and can hang the scooter.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 14.sp
                )
            }
        }

        // Offline warning if vehicle is not connected
        if (!isConnected) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(StatusDanger))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Not connected. Connect from Setup.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }

        // 3. S1 OPTIMIZED APPS SECTION
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mods",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    // Refresh Button
                    IconButton(
                        onClick = {
                            manager.log("[*] Refreshing installed apps list...")
                            refreshAllData()
                        },
                        enabled = isConnected && !isCheckingPackages
                    ) {
                        if (isCheckingPackages) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentSky, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AccentSky, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Balls Navigation
                ModAppRow(
                    app = ModApp.BALLS,
                    icon = Icons.Default.Navigation,
                    size = "1.4 MB",
                    desc = "Floating nav buttons",
                    isInstalled = installedPackages.contains(ModApp.BALLS.packageName),
                    actionInProgress = activeAppAction,
                    onOpen = {
                        coroutineScope.launch {
                            activeAppAction = "${ModApp.BALLS.id}:opening"
                            try {
                                val ok = manager.openApp(ModApp.BALLS.packageName, ModApp.BALLS.displayName)
                                if (ok) {
                                    Toast.makeText(context, "Balls Navigation opened", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                activeAppAction = null
                            }
                        }
                    },
                    onInstall = {
                        pendingConfirmation = AppConfirmation(
                            title = "Install Balls Navigation?",
                            message = "Permissions are set up automatically.",
                            confirmButtonText = "Install",
                            isDestructive = false
                        ) {
                            coroutineScope.launch {
                                activeAppAction = "${ModApp.BALLS.id}:installing"
                                try {
                                    manager.installSingleModApp(ModApp.BALLS)
                                    Toast.makeText(context, "Balls Navigation installed & configured!", Toast.LENGTH_SHORT).show()
                                    refreshAllData()
                                } catch (e: Exception) {
                                    manager.log("[!] Install failed: ${e.message}")
                                    Toast.makeText(context, "Install failed: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    activeAppAction = null
                                }
                            }
                        }
                    },
                    onUninstall = {
                        pendingConfirmation = AppConfirmation(
                            title = "Uninstall Balls Navigation?",
                            message = "It will be removed from the scooter.",
                            confirmButtonText = "Uninstall",
                            isDestructive = true
                        ) {
                            coroutineScope.launch {
                                activeAppAction = "${ModApp.BALLS.id}:uninstalling"
                                try {
                                    manager.uninstallPackage(ModApp.BALLS.packageName)
                                    Toast.makeText(context, "Balls Navigation uninstalled", Toast.LENGTH_SHORT).show()
                                    refreshAllData()
                                } catch (e: Exception) {
                                    manager.log("[!] Uninstall failed: ${e.message}")
                                    Toast.makeText(context, "Uninstall failed: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    activeAppAction = null
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Music Mod
                ModAppRow(
                    app = ModApp.MUSIC,
                    icon = Icons.Default.MusicNote,
                    size = "18.3 MB",
                    desc = "Ad-free music",
                    isInstalled = installedPackages.contains(ModApp.MUSIC.packageName),
                    actionInProgress = activeAppAction,
                    onOpen = {
                        coroutineScope.launch {
                            activeAppAction = "${ModApp.MUSIC.id}:opening"
                            try {
                                val ok = manager.openApp(ModApp.MUSIC.packageName, ModApp.MUSIC.displayName)
                                if (ok) {
                                    Toast.makeText(context, "Music Mod opened", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                activeAppAction = null
                            }
                        }
                    },
                    onInstall = {
                        pendingConfirmation = AppConfirmation(
                            title = "Install Music Mod?",
                            message = "It will be installed on the scooter.",
                            confirmButtonText = "Install",
                            isDestructive = false
                        ) {
                            coroutineScope.launch {
                                activeAppAction = "${ModApp.MUSIC.id}:installing"
                                try {
                                    manager.installSingleModApp(ModApp.MUSIC)
                                    Toast.makeText(context, "Music Mod installed!", Toast.LENGTH_SHORT).show()
                                    refreshAllData()
                                } catch (e: Exception) {
                                    manager.log("[!] Install failed: ${e.message}")
                                    Toast.makeText(context, "Install failed: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    activeAppAction = null
                                }
                            }
                        }
                    },
                    onUninstall = {
                        pendingConfirmation = AppConfirmation(
                            title = "Uninstall Music Mod?",
                            message = "It will be removed from the scooter.",
                            confirmButtonText = "Uninstall",
                            isDestructive = true
                        ) {
                            coroutineScope.launch {
                                activeAppAction = "${ModApp.MUSIC.id}:uninstalling"
                                try {
                                    manager.uninstallPackage(ModApp.MUSIC.packageName)
                                    Toast.makeText(context, "Music Mod uninstalled", Toast.LENGTH_SHORT).show()
                                    refreshAllData()
                                } catch (e: Exception) {
                                    manager.log("[!] Uninstall failed: ${e.message}")
                                    Toast.makeText(context, "Uninstall failed: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    activeAppAction = null
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Android Auto
                ModAppRow(
                    app = ModApp.ANDROID_AUTO,
                    icon = Icons.Default.DirectionsCar,
                    size = "20.9 MB",
                    desc = "Maps, music and calls",
                    isInstalled = installedPackages.contains(ModApp.ANDROID_AUTO.packageName),
                    actionInProgress = activeAppAction,
                    onOpen = {
                        coroutineScope.launch {
                            activeAppAction = "${ModApp.ANDROID_AUTO.id}:opening"
                            try {
                                val ok = manager.openApp(ModApp.ANDROID_AUTO.packageName, ModApp.ANDROID_AUTO.displayName)
                                if (ok) {
                                    Toast.makeText(context, "Android Auto opened", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                activeAppAction = null
                            }
                        }
                    },
                    onInstall = {
                        pendingConfirmation = AppConfirmation(
                            title = "Install Android Auto?",
                            message = "Permissions are set up automatically.",
                            confirmButtonText = "Install",
                            isDestructive = false
                        ) {
                            coroutineScope.launch {
                                activeAppAction = "${ModApp.ANDROID_AUTO.id}:installing"
                                try {
                                    manager.installSingleModApp(ModApp.ANDROID_AUTO)
                                    Toast.makeText(context, "Android Auto installed & permissions granted!", Toast.LENGTH_SHORT).show()
                                    refreshAllData()
                                } catch (e: Exception) {
                                    manager.log("[!] Install failed: ${e.message}")
                                    Toast.makeText(context, "Install failed: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    activeAppAction = null
                                }
                            }
                        }
                    },
                    onUninstall = {
                        pendingConfirmation = AppConfirmation(
                            title = "Uninstall Android Auto?",
                            message = "It will be removed from the scooter.",
                            confirmButtonText = "Uninstall",
                            isDestructive = true
                        ) {
                            coroutineScope.launch {
                                activeAppAction = "${ModApp.ANDROID_AUTO.id}:uninstalling"
                                try {
                                    manager.uninstallPackage(ModApp.ANDROID_AUTO.packageName)
                                    Toast.makeText(context, "Android Auto uninstalled", Toast.LENGTH_SHORT).show()
                                    refreshAllData()
                                } catch (e: Exception) {
                                    manager.log("[!] Uninstall failed: ${e.message}")
                                    Toast.makeText(context, "Uninstall failed: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    activeAppAction = null
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // OpenPlay (CarPlay)
                val carPlayApp = OnlineAppCatalog.carPlayApp
                val carPlayFile = remember(carPlayApp.fileName) {
                    File(downloadDir, carPlayApp.fileName)
                }
                var carPlayExists by remember(carPlayApp.fileName) {
                    mutableStateOf(carPlayFile.exists() && carPlayFile.length() > 500_000)
                }
                val isCarPlayInstalled = installedPackages.contains(carPlayApp.packageName)
                val isCarPlayDownloading = downloadingAppId == carPlayApp.id

                OnlineAppRow(
                    app = carPlayApp,
                    icon = Icons.Default.DirectionsCar,
                    isInstalled = isCarPlayInstalled,
                    isDownloaded = carPlayExists,
                    isDownloading = isCarPlayDownloading,
                    downloadProgress = downloadProgress,
                    downloadStatusText = downloadStatusText,
                    actionInProgress = activeAppAction,
                    onOpen = {
                        coroutineScope.launch {
                            activeAppAction = "${carPlayApp.id}:opening"
                            try {
                                val ok = manager.openApp(carPlayApp.packageName, carPlayApp.displayName)
                                if (ok) {
                                    Toast.makeText(context, "${carPlayApp.displayName} opened", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                activeAppAction = null
                            }
                        }
                    },
                    onDownload = {
                        coroutineScope.launch {
                            downloadingAppId = carPlayApp.id
                            downloadProgress = 0f
                            downloadStatusText = "Connecting..."
                            manager.log("[*] Downloading ${carPlayApp.displayName} (${carPlayApp.sizeText})...")
                            try {
                                ApkDownloader.download(carPlayApp.downloadUrl, carPlayFile) { fraction, readBytes, totalBytes ->
                                    downloadProgress = if (fraction >= 0f) fraction else 0f
                                    val readMb = String.format("%.1f", readBytes / (1024.0 * 1024.0))
                                    val totalMb = if (totalBytes > 0) String.format("%.1f MB", totalBytes / (1024.0 * 1024.0)) else "unknown"
                                    downloadStatusText = "$readMb / $totalMb"
                                }
                                carPlayExists = true
                                manager.log("[+] Successfully downloaded ${carPlayApp.displayName}")
                                Toast.makeText(context, "${carPlayApp.displayName} downloaded!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                manager.log("[!] Download failed for ${carPlayApp.displayName}: ${e.message}")
                                Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                downloadingAppId = null
                            }
                        }
                    },
                    onInstall = {
                        pendingConfirmation = AppConfirmation(
                            title = "Install ${carPlayApp.displayName}?",
                            message = "Are you sure you want to install ${carPlayApp.displayName} on your scooter?",
                            confirmButtonText = "Install",
                            isDestructive = false
                        ) {
                            coroutineScope.launch {
                                activeAppAction = "${carPlayApp.id}:installing"
                                try {
                                    manager.installApkFile(carPlayFile, carPlayApp.displayName)
                                    Toast.makeText(context, "${carPlayApp.displayName} installed on scooter!", Toast.LENGTH_SHORT).show()
                                    refreshAllData()
                                } catch (e: Exception) {
                                    manager.log("[!] Install failed: ${e.message}")
                                    Toast.makeText(context, "Install failed: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    activeAppAction = null
                                }
                            }
                        }
                    },
                    onUninstall = {
                        pendingConfirmation = AppConfirmation(
                            title = "Uninstall ${carPlayApp.displayName}?",
                            message = "Are you sure you want to uninstall ${carPlayApp.displayName} from your scooter?",
                            confirmButtonText = "Uninstall",
                            isDestructive = true
                        ) {
                            coroutineScope.launch {
                                activeAppAction = "${carPlayApp.id}:uninstalling"
                                try {
                                    manager.uninstallPackage(carPlayApp.packageName)
                                    Toast.makeText(context, "${carPlayApp.displayName} uninstalled", Toast.LENGTH_SHORT).show()
                                    refreshAllData()
                                } catch (e: Exception) {
                                    manager.log("[!] Uninstall failed: ${e.message}")
                                    Toast.makeText(context, "Uninstall failed: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    activeAppAction = null
                                }
                            }
                        }
                    }
                )
            }
        }

        // 4. DOWNLOADABLE ONLINE APPS SECTION
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Downloads",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                OnlineAppCatalog.apps.filter { it.id != OnlineAppCatalog.carPlayApp.id }.forEachIndexed { index, onlineApp ->
                    if (index > 0) Spacer(modifier = Modifier.height(10.dp))

                    val localFile = remember(onlineApp.fileName) {
                        File(downloadDir, onlineApp.fileName)
                    }
                    var fileExists by remember(onlineApp.fileName) {
                        mutableStateOf(localFile.exists() && localFile.length() > 500_000)
                    }

                    val isInstalled = installedPackages.contains(onlineApp.packageName)
                    val isDownloading = downloadingAppId == onlineApp.id

                    OnlineAppRow(
                        app = onlineApp,
                        icon = Icons.Default.Language,
                        isInstalled = isInstalled,
                        isDownloaded = fileExists,
                        isDownloading = isDownloading,
                        downloadProgress = downloadProgress,
                        downloadStatusText = downloadStatusText,
                        actionInProgress = activeAppAction,
                        onOpen = {
                            coroutineScope.launch {
                                activeAppAction = "${onlineApp.id}:opening"
                                try {
                                    val ok = manager.openApp(onlineApp.packageName, onlineApp.displayName)
                                    if (ok) {
                                        Toast.makeText(context, "${onlineApp.displayName} opened", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    activeAppAction = null
                                }
                            }
                        },
                        onDownload = {
                            coroutineScope.launch {
                                downloadingAppId = onlineApp.id
                                downloadProgress = 0f
                                downloadStatusText = "Connecting..."
                                manager.log("[*] Downloading ${onlineApp.displayName} (${onlineApp.sizeText}) from archive...")
                                try {
                                    ApkDownloader.download(onlineApp.downloadUrl, localFile) { fraction, readBytes, totalBytes ->
                                        downloadProgress = if (fraction >= 0f) fraction else 0f
                                        val readMb = String.format("%.1f", readBytes / (1024.0 * 1024.0))
                                        val totalMb = if (totalBytes > 0) String.format("%.1f MB", totalBytes / (1024.0 * 1024.0)) else "unknown"
                                        downloadStatusText = "$readMb / $totalMb"
                                    }
                                    fileExists = true
                                    manager.log("[+] Successfully downloaded ${onlineApp.displayName} (${String.format("%.1f", localFile.length() / (1024.0 * 1024.0))} MB)")
                                    Toast.makeText(context, "${onlineApp.displayName} downloaded!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    manager.log("[!] Download failed for ${onlineApp.displayName}: ${e.message}")
                                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    downloadingAppId = null
                                }
                            }
                        },
                        onInstall = {
                            pendingConfirmation = AppConfirmation(
                                title = "Install ${onlineApp.displayName}?",
                                message = "Browsers use a lot of RAM.",
                                confirmButtonText = "Install",
                                isDestructive = false
                            ) {
                                coroutineScope.launch {
                                    activeAppAction = "${onlineApp.id}:installing"
                                    try {
                                        manager.installApkFile(localFile, onlineApp.displayName)
                                        Toast.makeText(context, "${onlineApp.displayName} installed on scooter!", Toast.LENGTH_SHORT).show()
                                        refreshAllData()
                                    } catch (e: Exception) {
                                        manager.log("[!] Install failed: ${e.message}")
                                        Toast.makeText(context, "Install failed: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        activeAppAction = null
                                    }
                                }
                            }
                        },
                        onUninstall = {
                            pendingConfirmation = AppConfirmation(
                                title = "Uninstall ${onlineApp.displayName}?",
                                message = "It will be removed from the scooter.",
                                confirmButtonText = "Uninstall",
                                isDestructive = true
                            ) {
                                coroutineScope.launch {
                                    activeAppAction = "${onlineApp.id}:uninstalling"
                                    try {
                                        manager.uninstallPackage(onlineApp.packageName)
                                        Toast.makeText(context, "${onlineApp.displayName} uninstalled", Toast.LENGTH_SHORT).show()
                                        refreshAllData()
                                    } catch (e: Exception) {
                                        manager.log("[!] Uninstall failed: ${e.message}")
                                        Toast.makeText(context, "Uninstall failed: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        activeAppAction = null
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }

        // 5. CUSTOM APK SIDELOAD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Install APK",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark, contentColor = TextPrimary)
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedName != null) "Change file" else "Choose file", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                if (selectedName != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceVariantDark)
                            .padding(12.dp)
                    ) {
                                                Text(text = selectedName ?: "", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = String.format("%.2f MB", selectedSizeMb ?: 0.0), fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val uri = selectedUri ?: return@Button
                            val name = selectedName ?: "Custom APK"
                            pendingConfirmation = AppConfirmation(
                                title = "Install $name?",
                                message = "Install $name on the scooter?",
                                confirmButtonText = "Install",
                                isDestructive = false
                            ) {
                                coroutineScope.launch {
                                    isSideloadInstalling = true
                                    sideloadResult = null
                                    try {
                                        manager.log("[*] Preparing custom APK for installation: $name...")
                                        val tempFile = File(context.cacheDir, name)
                                        context.contentResolver.openInputStream(uri)?.use { input ->
                                            FileOutputStream(tempFile).use { output ->
                                                input.copyTo(output)
                                            }
                                        }
                                        val res = manager.installApkFile(tempFile, name)
                                        tempFile.delete()
                                        sideloadResult = "Install completed: $res"
                                        Toast.makeText(context, "Package installed", Toast.LENGTH_SHORT).show()
                                        refreshAllData()
                                    } catch (e: Exception) {
                                        manager.log("[!] Sideload install failed: ${e.message}")
                                        sideloadResult = "Installation failed: ${e.message}"
                                        Toast.makeText(context, "Install failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isSideloadInstalling = false
                                    }
                                }
                            }
                        },
                        enabled = !isSideloadInstalling,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = BgDark)
                    ) {
                        if (isSideloadInstalling) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BgDark, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Installing...", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Install Package", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                if (sideloadResult != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = sideloadResult ?: "",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (sideloadResult?.contains("failed") == true) StatusDanger else AccentMint
                    )
                }
            }
        }
    }

    // Confirmation AlertDialog for Install & Uninstall actions
    pendingConfirmation?.let { confirm ->
        AlertDialog(
            onDismissRequest = { pendingConfirmation = null },
            icon = {
                Icon(
                    if (confirm.isDestructive) Icons.Default.Warning else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (confirm.isDestructive) StatusDanger else AccentMint
                )
            },
            title = {
                Text(
                    text = confirm.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = confirm.message,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val action = confirm.onConfirm
                        pendingConfirmation = null
                        action()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (confirm.isDestructive) StatusDanger else AccentMint,
                        contentColor = if (confirm.isDestructive) TextPrimary else BgDark
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(confirm.confirmButtonText, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { pendingConfirmation = null },
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Text("Cancel", fontSize = 12.sp, color = TextMuted)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

/**
 * Modern non-clipping card for S1-Optimized Mod Apps.
 * Structured vertically so title, badges, action buttons, and description never overlap or clip.
 */
@Composable
fun ModAppRow(
    app: ModApp,
    icon: ImageVector,
    size: String,
    desc: String,
    isInstalled: Boolean,
    actionInProgress: String?,
    onOpen: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit
) {
    val isInstalling = actionInProgress == "${app.id}:installing"
    val isUninstalling = actionInProgress == "${app.id}:uninstalling"
    val isOpening = actionInProgress == "${app.id}:opening"
    val isBusy = isInstalling || isUninstalling || isOpening

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: icon + name/size on the left, status chip on the right.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceHighlight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.displayName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(size, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                }
                Spacer(modifier = Modifier.width(8.dp))
                val chipColor = if (isInstalled) StatusOk else TextMuted
                Text(
                    text = if (isInstalled) "Installed" else "Not installed",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = chipColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(chipColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            // Description on its own line.
            Text(text = desc, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)

            // Action buttons: full-width row, equal width, no horizontal squeeze.
            if (isInstalled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpen,
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        if (isOpening) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = TextPrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Launch, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Button(
                        onClick = onUninstall,
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusDanger.copy(alpha = 0.15f),
                            contentColor = StatusDanger
                        )
                    ) {
                        if (isUninstalling) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = StatusDanger, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Uninstall", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            } else {
                Button(
                    onClick = onInstall,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = BgDark)
                ) {
                    if (isInstalling) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = BgDark, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Install", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * Modern non-clipping card for Downloadable Online Apps.
 * Displays title, full-width badges (High RAM Usage tag), description, and download progress without clipping.
 */
@Composable
fun OnlineAppRow(
    app: OnlineApp,
    icon: ImageVector,
    isInstalled: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    downloadStatusText: String,
    actionInProgress: String?,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit
) {
    val isInstalling = actionInProgress == "${app.id}:installing"
    val isUninstalling = actionInProgress == "${app.id}:uninstalling"
    val isOpening = actionInProgress == "${app.id}:opening"
    val isBusy = isInstalling || isUninstalling || isDownloading || isOpening

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tier 1: App Icon + Title & Size + Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isInstalled) AccentMintSubtle else SurfaceHighlight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (isInstalled) AccentMint else TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = app.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Size: ${app.sizeText}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Action Button Logic: Open & Uninstall (if installed) OR Install / Download
                if (isInstalled) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Open Button
                        Button(
                            onClick = onOpen,
                            enabled = !isBusy,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentMintSubtle,
                                contentColor = AccentMint
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentMint.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            if (isOpening) {
                                CircularProgressIndicator(modifier = Modifier.size(13.dp), color = AccentMint, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Launch, contentDescription = "Open", modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Uninstall Button
                        Button(
                            onClick = onUninstall,
                            enabled = !isBusy,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusDanger.copy(alpha = 0.15f),
                                contentColor = StatusDanger
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            if (isUninstalling) {
                                CircularProgressIndicator(modifier = Modifier.size(13.dp), color = StatusDanger, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Uninstall", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else if (isDownloaded) {
                    Button(
                        onClick = onInstall,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentMint,
                            contentColor = BgDark
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        if (isInstalling) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = BgDark, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Install", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else if (!isDownloading) {
                    Button(
                        onClick = onDownload,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentSky,
                            contentColor = BgDark
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Tier 2: Badges (Full width row: Status badge + High RAM Usage badge, ZERO clipping)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Badge
                if (isInstalled) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AccentMintSubtle)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("✓ Installed on Scooter", fontSize = 10.sp, color = AccentMint, fontWeight = FontWeight.Bold)
                    }
                } else if (isDownloaded) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AccentSkySubtle)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("✓ Downloaded to Phone", fontSize = 10.sp, color = AccentSky, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceDark)
                            .border(1.dp, CardBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("Not Installed", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                    }
                }

                // High RAM Usage Warning Badge
                if (app.tag != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(StatusWarning.copy(alpha = 0.15f))
                            .border(1.dp, StatusWarning.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(app.tag, fontSize = 10.sp, color = StatusWarning, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Tier 3: Full Width Description
            Text(
                text = app.description,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )

            // Tier 4: Download Progress Bar (When downloading)
            if (isDownloading) {
                Spacer(modifier = Modifier.height(4.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Downloading APK...",
                            fontSize = 11.sp,
                            color = AccentSky,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (downloadProgress > 0f) "${(downloadProgress * 100).toInt()}% ($downloadStatusText)" else downloadStatusText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (downloadProgress > 0f) {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = AccentSky,
                            trackColor = SurfaceHighlight
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = AccentSky,
                            trackColor = SurfaceHighlight
                        )
                    }
                }
            }
        }
    }
}
