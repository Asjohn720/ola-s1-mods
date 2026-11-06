package com.asjohn720.olamods.ui.screens

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
import com.asjohn720.olamods.model.ScooterInfo
import com.asjohn720.olamods.model.ScooterManager
import com.asjohn720.olamods.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun ToolboxScreen(manager: ScooterManager) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

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
            text = "Vehicle Controls",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextMuted
        )

        // Quick Action Grid (Speedometer Home & Settings)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolboxActionCard(
                title = "Dashboard Home",
                subtitle = "Speedometer & Main UI",
                icon = Icons.Default.Home,
                modifier = Modifier.weight(1f)
            ) {
                coroutineScope.launch {
                    manager.openOlaHome()
                    Toast.makeText(context, "Dashboard Home opened", Toast.LENGTH_SHORT).show()
                }
            }

            ToolboxActionCard(
                title = "System Settings",
                subtitle = "Native Settings Panel",
                icon = Icons.Default.Settings,
                modifier = Modifier.weight(1f)
            ) {
                coroutineScope.launch {
                    manager.openSettings("android.settings.SETTINGS")
                    Toast.makeText(context, "Settings opened", Toast.LENGTH_SHORT).show()
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
                    text = "System Settings Panels",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingChip("Wi-Fi", "android.settings.WIFI_SETTINGS", manager, coroutineScope, context)
                    SettingChip("Bluetooth", "android.settings.BLUETOOTH_SETTINGS", manager, coroutineScope, context)
                    SettingChip("Apps", "android.settings.APPLICATION_SETTINGS", manager, coroutineScope, context)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingChip("Display", "android.settings.DISPLAY_SETTINGS", manager, coroutineScope, context)
                    SettingChip("Sound", "android.settings.SOUND_SETTINGS", manager, coroutineScope, context)
                    SettingChip("Date/Time", "android.settings.DATE_SETTINGS", manager, coroutineScope, context)
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
                        text = "Hardware & Software Info",
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
                            Text("Inspect Specs", fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
                    Text(
                        text = "Query hardware identifiers, MoveOS release version, and UID directly over gRPC.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
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
                        text = "Display Capture",
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
                                    Toast.makeText(context, "Screenshot captured", Toast.LENGTH_SHORT).show()
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
                            Text("Capture Screen", fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
                                val file = File(picturesDir, "ola_screenshot_${System.currentTimeMillis()}.png")
                                FileOutputStream(file).use { out ->
                                    screenshotBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, out)
                                }
                                Toast.makeText(context, "Saved to ${file.name}", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
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
                        Text("Save to Storage", fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
                            text = "No screenshot captured yet.",
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() },
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
                Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 11.sp, color = TextMuted)
        }
    }
}

@Composable
fun RowScope.SettingChip(
    label: String,
    action: String,
    manager: ScooterManager,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context
) {
    Button(
        onClick = {
            coroutineScope.launch {
                manager.openSettings(action)
                Toast.makeText(context, "$label opened", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark, contentColor = TextPrimary)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
