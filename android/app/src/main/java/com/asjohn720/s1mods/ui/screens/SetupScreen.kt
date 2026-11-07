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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
    val clipboard = LocalClipboardManager.current
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

    // App selection state for setup (Balls + Music)
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
                        Button(
                            onClick = {
                                tempInput = scooterIp
                                tempScope = scooterScope?.toString() ?: ""
                                showEditDialog = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark, contentColor = AccentSky),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Manual IP", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Manual IP", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val found = manager.autoDiscover()
                                    if (found == null) {
                                        Toast.makeText(context, "Vehicle not detected. Use 'Manual IP' to enter from scooter screen.", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Found vehicle at ${found.ip}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark, contentColor = AccentMint),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Scan", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan", fontSize = 11.sp, fontWeight = FontWeight.Medium)
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

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    isTestingPort = true
                                    manager.runNetworkDiagnostics()
                                    isTestingPort = false
                                }
                            },
                            enabled = !isTestingPort,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Diagnose", fontSize = 12.sp, color = AccentMint)
                        }

                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    isTestingPort = true
                                    val open = manager.testConnection()
                                    isTestingPort = false
                                    Toast.makeText(
                                        context,
                                        if (open) "Port 50005 reachable" else "Port 50005 unreachable. Check Diagnostics log.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            enabled = !isTestingPort,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
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
                        text = "Diagnostics & Output",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val fullLog = logs.joinToString("\n")
                                clipboard.setText(AnnotatedString(fullLog))
                                Toast.makeText(context, "Full diagnostic log copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark, contentColor = AccentMint),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Log", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }

                        IconButton(onClick = { manager.clearLogs() }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
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
                            fontSize = 13.sp, // Enlarged from 11.sp for high legibility
                            fontFamily = FontFamily.Monospace,
                            color = color,
                            lineHeight = 18.sp // Clean line-height
                        )
                    }
                }
            }
        }
    }

    // Manual IP / MAC / Scope Edit Dialog
    if (showEditDialog) {
        var isConnectingManual by remember { mutableStateOf(false) }

        val derivedIpv6 = remember(tempInput) {
            NetworkScanner.macToIpv6(tempInput)
        }

        AlertDialog(
            onDismissRequest = { if (!isConnectingManual) showEditDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = AccentMint, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manual Scooter IP Setup", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Step-by-Step Instruction Card in English
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "How to find your IP on the scooter display:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentSky
                            )
                            Text(
                                text = "1. On the scooter screen, open Settings ➔ Wi-Fi.",
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "2. Tap on your connected Wi-Fi / Hotspot name.",
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "3. Note down the IP Address (e.g. 192.168.43.150) or MAC.",
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = tempInput,
                        onValueChange = { tempInput = it },
                        label = { Text("Scooter IP or MAC Address") },
                        placeholder = { Text("e.g. 192.168.43.150 or aa:bb:cc:dd:ee:ff") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentSky,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (derivedIpv6 != null) {
                        Text(
                            text = "✓ MAC detected -> SLAAC IPv6: $derivedIpv6",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = AccentMint
                        )
                    } else if (tempInput.contains(".")) {
                        Text(
                            text = "✓ IPv4 detected -> S1Mods will auto-derive IPv6 to bypass Customer mode firewall",
                            fontSize = 11.sp,
                            color = AccentSky
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val input = tempInput.trim()
                        if (input.isNotBlank()) {
                            coroutineScope.launch {
                                isConnectingManual = true
                                val res = manager.setManualAddress(input)
                                isConnectingManual = false
                                showEditDialog = false
                                if (res.isPortOpen) {
                                    Toast.makeText(context, "Connected to scooter successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Address saved. Port 50005: check diagnostics log.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    enabled = !isConnectingManual && tempInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = BgDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isConnectingManual) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = BgDark)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Connect & Verify", fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditDialog = false },
                    enabled = !isConnectingManual
                ) {
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
                        text = "Applications to install:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    // App Selection Checkboxes (Balls + Music)
                    ModApp.values().forEach { app ->
                        val isChecked = selectedApps.contains(app)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(enabled = !app.isMandatory) {
                                    selectedApps = if (isChecked) {
                                        selectedApps - app
                                    } else {
                                        selectedApps + app
                                    }
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
