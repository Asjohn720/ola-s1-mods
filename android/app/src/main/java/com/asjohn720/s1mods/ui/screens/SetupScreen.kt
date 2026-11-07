package com.asjohn720.s1mods.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import com.asjohn720.s1mods.discovery.NetworkScanner
import com.asjohn720.s1mods.model.ModApp
import com.asjohn720.s1mods.model.ScooterManager
import com.asjohn720.s1mods.model.SetupStep
import com.asjohn720.s1mods.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(manager: ScooterManager) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scooterIp by manager.scooterIp.collectAsState()
    val scooterScope by manager.scooterScope.collectAsState()
    val isConnected by manager.isConnected.collectAsState()
    val currentStep by manager.currentStep.collectAsState()
    val isRunning by manager.isSetupRunning.collectAsState()
    val success by manager.setupSuccess.collectAsState()
    val logs by manager.logs.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var showPreFlightDialog by remember { mutableStateOf(false) }
    var tempInput by remember { mutableStateOf(scooterIp) }
    var tempScope by remember { mutableStateOf(scooterScope?.toString() ?: "") }
    var isTestingPort by remember { mutableStateOf(false) }

    // App selection state for setup: Balls, Android Auto, Music Mod checked by default
    var selectedApps by remember { mutableStateOf(ModApp.values().toSet()) }

    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Connection & Scooter IP Card
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
                            text = "Target Vehicle",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (scooterIp.isBlank()) "Tap Scan or enter IP manually" else if (scooterScope != null) "$scooterIp%$scooterScope" else scooterIp,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            color = if (scooterIp.isBlank()) TextMuted else TextPrimary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                tempInput = scooterIp
                                tempScope = scooterScope?.toString() ?: ""
                                showEditDialog = true
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceVariantDark)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit IP", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    val found = manager.autoDiscover()
                                    if (found == null) {
                                        Toast.makeText(context, "Vehicle not detected. Check Hotspot connected devices or enter MAC.", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Found vehicle at ${found.ip}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceVariantDark)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Scan", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DividerColor, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) AccentMint else StatusDanger)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isConnected) "MoveOS gRPC Online (Port 50005)" else "gRPC Service Offline",
                            fontSize = 12.sp,
                            color = if (isConnected) AccentMint else TextMuted
                        )
                    }

                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                isTestingPort = true
                                val open = manager.testConnection()
                                isTestingPort = false
                                Toast.makeText(
                                    context,
                                    if (open) "Port 50005 reachable" else "Port 50005 unreachable. Wake scooter screen.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = !isTestingPort,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        if (isTestingPort) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = TextSecondary)
                        } else {
                            Text("Test Port", fontSize = 12.sp, color = AccentSky)
                        }
                    }
                }
            }
        }

        // Start Setup Action Button
        Button(
            onClick = {
                showPreFlightDialog = true
            },
            enabled = !isRunning,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentMint,
                contentColor = BgDark,
                disabledContainerColor = SurfaceVariantDark,
                disabledContentColor = TextMuted
            )
        ) {
            if (isRunning) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BgDark, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Installing Mods...", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Automated Setup", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Setup Steps Progress Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Installation Steps",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(10.dp))

                SetupStep.values().forEach { step ->
                    val isDone = currentStep != null && (step.stepNum < (currentStep?.stepNum ?: 0) || success)
                    val isCurrent = currentStep == step && isRunning

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isDone -> AccentMintSubtle
                                        isCurrent -> AccentSkySubtle
                                        else -> SurfaceVariantDark
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isDone -> Icon(Icons.Default.Check, contentDescription = null, tint = AccentMint, modifier = Modifier.size(14.dp))
                                isCurrent -> CircularProgressIndicator(modifier = Modifier.size(12.dp), color = AccentSky, strokeWidth = 2.dp)
                                else -> Text(text = "${step.stepNum}", fontSize = 10.sp, color = TextMuted)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = step.title,
                            fontSize = 12.sp,
                            fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                            color = when {
                                isDone -> TextPrimary
                                isCurrent -> AccentSky
                                else -> TextMuted
                            }
                        )
                    }
                }
            }
        }

        // Live Log Terminal
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
                        text = "Console Output",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                    IconButton(onClick = { manager.clearLogs() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(logs) { logLine ->
                        val color = when {
                            logLine.contains("[+]") || logLine.contains("SUCCESS") -> AccentMint
                            logLine.contains("[!]") || logLine.contains("failed") -> StatusDanger
                            logLine.contains("[*]") -> AccentSky
                            else -> TextSecondary
                        }
                        Text(
                            text = logLine,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = color,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }

    // Manual IP / MAC / Scope Edit Dialog
    if (showEditDialog) {
        val derivedIpv6 = remember(tempInput) {
            NetworkScanner.macToIpv6(tempInput)
        }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Vehicle Address Configuration", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter scooter IPv6, IPv4 or MAC address from your phone's Hotspot > Connected devices:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = tempInput,
                        onValueChange = { tempInput = it },
                        label = { Text("IPv6, IPv4 or MAC Address") },
                        placeholder = { Text("e.g. aa:bb:cc:dd:ee:ff or IPv6") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentSky,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    if (derivedIpv6 != null) {
                        Text(
                            text = "Derived IPv6: $derivedIpv6",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = AccentMint
                        )
                    }

                    OutlinedTextField(
                        value = tempScope,
                        onValueChange = { tempScope = it },
                        label = { Text("Scope ID (optional, e.g. 22)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentSky,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedScope = tempScope.toIntOrNull()
                        val finalIp = derivedIpv6 ?: tempInput.trim()
                        manager.scooterIp.value = finalIp
                        manager.scooterScope.value = parsedScope
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = BgDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Apply Address", fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Pre-Flight Safety & App Selection Dialog
    if (showPreFlightDialog) {
        AlertDialog(
            onDismissRequest = { showPreFlightDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = AccentMint, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Setup Configuration", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pre-flight notices
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceVariantDark)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "• Side Stand UP: Sensor keeps vehicle systems active.",
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "• Keep Screen Active: Tap screen during mods to prevent sleep.",
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "• Auto-Lock Safe: Auto-lock security is never disabled.",
                            fontSize = 11.sp,
                            color = AccentSky
                        )
                    }

                    Text(
                        text = "Choose applications to install:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    // App Selection Checkboxes (Total 4 apps)
                    // App Selection Checkboxes (3 core apps)
                    ModApp.values().forEach { app ->
                        val isChecked = selectedApps.contains(app)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(enabled = !app.isMandatory) {
                                    selectedApps = if (isChecked) selectedApps - app else selectedApps + app
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (!app.isMandatory) {
                                        selectedApps = if (checked) selectedApps + app else selectedApps - app
                                    }
                                },
                                enabled = !app.isMandatory,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AccentMint,
                                    uncheckedColor = CardBorder,
                                    checkmarkColor = BgDark,
                                    disabledCheckedColor = AccentMint.copy(alpha = 0.6f)
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = app.displayName + if (app.isMandatory) " (Required)" else "",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (app.isMandatory) AccentMint else TextPrimary
                                )
                                Text(
                                    text = app.description,
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPreFlightDialog = false
                        coroutineScope.launch {
                            manager.runFullSetup(selectedApps)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = BgDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Start Setup (${selectedApps.size} Apps)", fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPreFlightDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(12.dp)
        )
    }
}
