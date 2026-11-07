package com.asjohn720.s1mods.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asjohn720.s1mods.model.ScooterManager
import com.asjohn720.s1mods.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ShellScreen(manager: ScooterManager) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var hasAcceptedRisk by remember { mutableStateOf(false) }
    var agreementChecked by remember { mutableStateOf(false) }

    var commandInput by remember { mutableStateOf("id -u") }
    var isRunning by remember { mutableStateOf(false) }
    var outputHistory by remember { mutableStateOf(listOf("# root shell")) }

    val presets = listOf(
        "id -u",
        "getprop ro.product.model",
        "getprop persist.ola.security.scootermode",
        "pm list packages -3",
        "dumpsys battery",
        "df -h /data",
        "cat /proc/meminfo"
    )

    if (!hasAcceptedRisk) {
        // High Risk Warning Dialog / Gate
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(StatusDanger.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Root shell", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceVariantDark)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Commands run as root on the scooter. A wrong command can brick the display. No warranty; the author is not liable.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 19.sp
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { agreementChecked = !agreementChecked },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = agreementChecked,
                            onCheckedChange = { agreementChecked = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = StatusDanger,
                                checkmarkColor = TextPrimary,
                                uncheckedColor = CardBorder
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "I understand and accept the risk",
                            fontSize = 11.sp,
                            color = TextPrimary,
                            lineHeight = 15.sp
                        )
                    }

                    Button(
                        onClick = { hasAcceptedRisk = true },
                        enabled = agreementChecked,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusDanger,
                            contentColor = TextPrimary,
                            disabledContainerColor = SurfaceVariantDark,
                            disabledContentColor = TextMuted
                        )
                    ) {
                        Text("Unlock", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }
    } else {
        // Unlocked Root Terminal Screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Presets",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted
            )

            // Preset command chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceVariantDark)
                            .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
                            .clickable { commandInput = preset }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = preset,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Command input bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    placeholder = { Text("Command", color = TextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentMint,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                IconButton(
                    onClick = {
                        if (commandInput.isNotBlank() && !isRunning) {
                            val cmd = commandInput.trim()
                            coroutineScope.launch {
                                isRunning = true
                                try {
                                    outputHistory = outputHistory + "\n$ # $cmd"
                                    val result = manager.runRootShell(cmd)
                                    outputHistory = outputHistory + (result.ifEmpty { "(no output)" })
                                } catch (e: Exception) {
                                    outputHistory = outputHistory + "ERROR: ${e.message}"
                                } finally {
                                    isRunning = false
                                }
                            }
                        }
                    },
                    enabled = !isRunning,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isRunning) SurfaceVariantDark else AccentMint)
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TextMuted, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Run", tint = BgDark, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Terminal Console Area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = TerminalBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Output",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted
                        )

                        Row {
                            IconButton(
                                onClick = {
                                    val fullText = outputHistory.joinToString("\n")
                                    clipboard.setText(AnnotatedString(fullText))
                                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextMuted, modifier = Modifier.size(15.dp))
                            }

                            IconButton(
                                onClick = { outputHistory = emptyList() },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(15.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    SelectionContainer {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(outputHistory) { line ->
                                val color = when {
                                    line.startsWith("$ #") -> AccentSky
                                    line.startsWith("ERROR") -> StatusDanger
                                    line.startsWith("#") -> TextMuted
                                    else -> TextPrimary
                                }
                                Text(
                                    text = line,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = color,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
