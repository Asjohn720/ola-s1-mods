package com.asjohn720.s1mods.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asjohn720.s1mods.model.ScooterInfo
import com.asjohn720.s1mods.model.ScooterManager
import com.asjohn720.s1mods.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun ToolboxScreen(manager: ScooterManager) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var activeAction by remember { mutableStateOf<String?>(null) }

    var screenshotBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    var scooterInfo by remember { mutableStateOf<ScooterInfo?>(null) }
    var isFetchingInfo by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Text(
            text = "Controls",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextMuted
        )

        // Quick Action Grid (Speedometer Home & Settings)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val isHomeLoading = activeAction == "home"
            ToolboxActionCard(
                title = "Home",
                subtitle = "Dashboard",
                icon = Icons.Default.Home,
                isLoading = isHomeLoading,
                modifier = Modifier.weight(1f)
            ) {
                if (activeAction == null) {
                    coroutineScope.launch {
                        activeAction = "home"
                        try {
                            val ok = manager.openDashboardHome()
                            if (ok) {
                                Toast.makeText(context, "Opened", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Could not open", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            activeAction = null
                        }
                    }
                }
            }

            val isSettingsLoading = activeAction == "settings"
            ToolboxActionCard(
                title = "Settings",
                subtitle = "Android",
                icon = Icons.Default.Settings,
                isLoading = isSettingsLoading,
                modifier = Modifier.weight(1f)
            ) {
                if (activeAction == null) {
                    coroutineScope.launch {
                        activeAction = "settings"
                        try {
                            val ok = manager.openSettings("android.settings.SETTINGS")
                            if (ok) {
                                Toast.makeText(context, "Opened", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Could not open", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            activeAction = null
                        }
                    }
                }
            }
        }

        // Android Sub-Settings Chips
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Shortcuts",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingChip("Wi-Fi", "android.settings.WIFI_SETTINGS", "wifi", activeAction, manager, coroutineScope, context) { activeAction = it }
                    SettingChip("Bluetooth", "android.settings.BLUETOOTH_SETTINGS", "bt", activeAction, manager, coroutineScope, context) { activeAction = it }
                    SettingChip("Apps", "android.settings.APPLICATION_SETTINGS", "apps", activeAction, manager, coroutineScope, context) { activeAction = it }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingChip("Display", "android.settings.DISPLAY_SETTINGS", "display", activeAction, manager, coroutineScope, context) { activeAction = it }
                    SettingChip("Sound", "android.settings.SOUND_SETTINGS", "sound", activeAction, manager, coroutineScope, context) { activeAction = it }
                    SettingChip("Date/Time", "android.settings.DATE_SETTINGS", "datetime", activeAction, manager, coroutineScope, context) { activeAction = it }
                }
            }
        }

        // Scooter Hardware & System Info Card
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
                    Text(
                        text = "Device info",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isFetchingInfo = true
                                try {
                                    scooterInfo = manager.fetchScooterInfo()
                                                                    } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isFetchingInfo = false
                                }
                            }
                        },
                        enabled = !isFetchingInfo,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark, contentColor = TextPrimary)
                    ) {
                        if (isFetchingInfo) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = TextPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("Load", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (scooterInfo != null) {
                    InfoRow("Model", scooterInfo?.model ?: "-")
                    InfoRow("MoveOS Build", scooterInfo?.buildId ?: "-")
                    InfoRow("Android Version", scooterInfo?.androidVersion ?: "-")
                    InfoRow("Scooter Mode", scooterInfo?.scooterMode ?: "-")
                    InfoRow("Root UID", scooterInfo?.uid ?: "-")
                } else {
                    Text(text = "Not loaded", fontSize = 12.sp, color = TextMuted)
                }
            }
        }

        // Live Screenshot Viewer Card
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
                    Text(
                        text = "Screenshot",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isCapturing = true
                                try {
                                    screenshotBitmap = manager.captureScreenshot()
                                                                    } catch (e: Exception) {
                                    Toast.makeText(context, "Capture failed: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isCapturing = false
                                }
                            }
                        },
                        enabled = !isCapturing,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = BgDark)
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = BgDark, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Capture", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (screenshotBitmap != null) {
                    Image(
                        bitmap = screenshotBitmap!!.asImageBitmap(),
                        contentDescription = "Scooter Display Screenshot",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            try {
                                val picturesDir = context.getExternalFilesDir(null)
                                val file = File(picturesDir, "s1_screenshot_${System.currentTimeMillis()}.png")
                                FileOutputStream(file).use { out ->
                                    screenshotBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, out)
                                }
                                manager.log("[+] Screenshot saved to phone storage: ${file.name}")
                                Toast.makeText(context, "Saved to ${file.name}", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                manager.log("[!] Failed to save screenshot: ${e.message}")
                                Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceVariantDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No screenshot",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToolboxActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable(enabled = !isLoading) { onClick() },
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariantDark),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TextPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 12.sp, color = TextMuted)
        }
    }
}

@Composable
fun RowScope.SettingChip(
    label: String,
    action: String,
    actionKey: String,
    activeAction: String?,
    manager: ScooterManager,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    setActiveAction: (String?) -> Unit
) {
    val isLoading = activeAction == actionKey
    Button(
        onClick = {
            if (activeAction == null) {
                coroutineScope.launch {
                    setActiveAction(actionKey)
                    try {
                        val ok = manager.openSettings(action)
                        if (ok) {
                            Toast.makeText(context, "Opened", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Could not open $label", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        setActiveAction(null)
                    }
                }
            }
        },
        enabled = activeAction == null || isLoading,
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark, contentColor = TextPrimary)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = TextPrimary, strokeWidth = 2.dp)
        } else {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Text(value, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}
