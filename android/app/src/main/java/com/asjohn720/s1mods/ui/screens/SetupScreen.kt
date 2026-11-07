package com.asjohn720.s1mods.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
    var showStepsDialog by remember { mutableStateOf(false) }
    var isFullscreenLog by remember { mutableStateOf(false) }

    var tempInput by remember { mutableStateOf(scooterIp) }
    var isTestingPort by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }

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
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Section: Collapsible when in Fullscreen Log Mode
        AnimatedVisibility(visible = !isFullscreenLog) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Compact Target Vehicle Header Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isConnected) AccentMint else StatusDanger)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (scooterIp.isBlank()) "No Scooter Connected" else if (scooterScope != null) "$scooterIp%$scooterScope" else scooterIp,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (scooterIp.isBlank()) TextMuted else TextPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (isConnected) "MoveOS gRPC Active (Port 50005)" else "Tap Scan or Manual IP to connect",
                                        fontSize = 11.sp,
                                        color = if (isConnected) AccentMint else TextMuted
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        tempInput = scooterIp
                                        showEditDialog = true
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark, contentColor = AccentSky),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Manual IP", modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Manual IP", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            isScanning = true
                                            val found = manager.autoDiscover()
                                            isScanning = false
                                            if (found == null) {
                                                Toast.makeText(context, "Scooter not detected. Use Manual IP to enter from scooter screen.", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Connected to ${found.ip}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    enabled = !isScanning,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark, contentColor = AccentMint),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    if (isScanning) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = AccentMint)
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = "Scan", modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Scan", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        // Diagnostic Quick Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Setup: Balls + Music Mod",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Diagnose",
                                    fontSize = 11.sp,
                                    color = AccentMint,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            coroutineScope.launch {
                                                manager.runNetworkDiagnostics()
                                            }
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )

                                Text(
                                    text = if (isTestingPort) "Testing..." else "Test Port",
                                    fontSize = 11.sp,
                                    color = AccentSky,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable(enabled = !isTestingPort) {
                                            coroutineScope.launch {
                                                isTestingPort = true
                                                val open = manager.testConnection()
                                                isTestingPort = false
                                                Toast.makeText(
                                                    context,
                                                    if (open) "Port 50005 reachable" else "Port 50005 unreachable. Check log.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )

                                Text(
                                    text = "View 6 Steps",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { showStepsDialog = true }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Action / Running Progress Banner
                if (isRunning) {
                    val stepNum = currentStep?.stepNum ?: 1
                    val progressFloat = (stepNum.toFloat() / 6f).coerceIn(0.05f, 1f)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AccentMintSubtle, RoundedCornerShape(10.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = AccentMint
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Step $stepNum/6: ${currentStep?.title ?: "Processing..."}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AccentMint
                                    )
                                }
                                Text(
                                    text = "${(progressFloat * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary
                                )
                            }

                            LinearProgressIndicator(
                                progress = { progressFloat },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = AccentMint,
                                trackColor = SurfaceVariantDark
                            )
                        }
                    }
                } else if (success) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AccentMint, RoundedCornerShape(10.dp)),
                        colors = CardDefaults.cardColors(containerColor = AccentMintSubtle),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentMint, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Setup Complete! Mods Installed.",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }

                            Button(
                                onClick = { showPreFlightDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = BgDark),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Re-run", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = { showPreFlightDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentMint,
                            contentColor = BgDark
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Automated Setup (Balls + Music)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Hero Component: Live Diagnostics & Output Console
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = TerminalBg),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Console Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = AccentMint, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Live Console Log",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${logs.size} lines)",
                            fontSize = 10.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Fullscreen / Minimize Toggle
                        IconButton(
                            onClick = { isFullscreenLog = !isFullscreenLog },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                if (isFullscreenLog) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = AccentSky,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Copy Log Button
                        Button(
                            onClick = {
                                val fullLog = logs.joinToString("\n")
                                clipboard.setText(AnnotatedString(fullLog))
                                Toast.makeText(context, "Log copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark, contentColor = AccentMint),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }

                        // Clear Button
                        IconButton(
                            onClick = { manager.clearLogs() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(15.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = TerminalBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable Terminal Logs
                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
                            Text(
                                text = "Ready. Tap 'Scan' or 'Manual IP' to detect vehicle.",
                                fontSize = 12.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        items(logs) { logLine ->
                            val color = when {
                                logLine.contains("[+]") || logLine.contains("SUCCESS") -> AccentMint
                                logLine.contains("[!]") || logLine.contains("failed") -> StatusDanger
                                logLine.contains("[*]") || logLine.contains("====") -> AccentSky
                                logLine.contains("[-]") -> TextSecondary
                                else -> TextSecondary
                            }
                            Text(
                                text = logLine,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = color,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Manual IP / MAC Dialog with English Instructions
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
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "How to find IP on scooter display:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentSky
                            )
                            Text(
                                text = "1. On scooter, open Settings ➔ Wi-Fi.",
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "2. Tap your connected Wi-Fi / Hotspot name.",
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "3. Note the IP Address (e.g. 192.168.43.150) or MAC.",
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
                            text = "✓ IPv4 detected -> Auto-maps to IPv6 SLAAC for customer firewall bypass",
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
                                    Toast.makeText(context, "Address saved. Check diagnostics log.", Toast.LENGTH_LONG).show()
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

    // View 6 Steps Overview Dialog
    if (showStepsDialog) {
        AlertDialog(
            onDismissRequest = { showStepsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FormatListNumbered, contentDescription = null, tint = AccentMint, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Automated Setup Pipeline", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SetupStep.values().forEach { step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceVariantDark)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(AccentMintSubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${step.stepNum}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentMint
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = step.title,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showStepsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = BgDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Medium)
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
                            text = "• Side Stand UP: Keeps vehicle display and systems awake.",
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "• Keep Screen Active: Tap display during setup to prevent sleep.",
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "• Zero Partition Writes: Bind-mount ADB unlock in RAM only.",
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
