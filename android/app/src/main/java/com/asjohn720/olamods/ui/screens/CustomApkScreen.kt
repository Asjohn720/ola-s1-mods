package com.asjohn720.olamods.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asjohn720.olamods.model.ScooterManager
import com.asjohn720.olamods.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun CustomApkScreen(manager: ScooterManager) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf<String?>(null) }
    var selectedSizeMb by remember { mutableStateOf<Double?>(null) }

    var isInstalling by remember { mutableStateOf(false) }
    var installResult by remember { mutableStateOf<String?>(null) }

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
                    selectedSizeMb = sizeBytes.toDouble() / (1024.0 * 1024.0)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Custom Package Installer",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextMuted
        )

        // Select Custom APK Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Select Android Package",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose any .apk file from local device storage to install onto MoveOS via wireless ADB stream.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
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
                    Text(if (selectedName != null) "Change Package" else "Browse APK File", fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
                        Text(
                            text = "Selected Package",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = selectedName ?: "",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format("%.2f MB", selectedSizeMb ?: 0.0),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val uri = selectedUri ?: return@Button
                            coroutineScope.launch {
                                isInstalling = true
                                installResult = null
                                try {
                                    val tempFile = File(context.cacheDir, selectedName ?: "temp.apk")
                                    context.contentResolver.openInputStream(uri)?.use { input ->
                                        FileOutputStream(tempFile).use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    val res = manager.installCustomApk(tempFile)
                                    tempFile.delete()
                                    installResult = "Install completed: $res"
                                    Toast.makeText(context, "Package installed", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    installResult = "Installation failed: ${e.message}"
                                    Toast.makeText(context, "Install failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isInstalling = false
                                }
                            }
                        },
                        enabled = !isInstalling,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = BgDark)
                    ) {
                        if (isInstalling) {
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

                if (installResult != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = installResult ?: "",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (installResult?.contains("failed") == true) StatusDanger else AccentMint
                    )
                }
            }
        }

        // Bundled APKs Overview
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Included Applications",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(10.dp))

                BundledApkRow("Balls Navigation", "balls-1.0.0-lite-release.apk", "1.4 MB", "Floating navigation overlay")
                Spacer(modifier = Modifier.height(8.dp))
                BundledApkRow("Music Mod", "musicmod-release.apk", "18.3 MB", "Lightweight offline audio player")
            }
        }
    }
}

@Composable
fun BundledApkRow(title: String, fileName: String, size: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceVariantDark)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceHighlight),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Android, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Text(desc, fontSize = 11.sp, color = TextSecondary)
        }
        Text(size, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
    }
}
