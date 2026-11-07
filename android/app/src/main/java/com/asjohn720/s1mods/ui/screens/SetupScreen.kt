package com.asjohn720.s1mods.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asjohn720.s1mods.discovery.NetworkScanner
import com.asjohn720.s1mods.model.ModApp
import com.asjohn720.s1mods.model.ScooterManager
import com.asjohn720.s1mods.model.SetupStep
import com.asjohn720.s1mods.ui.components.VehicleConnectionCard
import com.asjohn720.s1mods.ui.theme.*
import kotlinx.coroutines.launch

enum class SetupSubTab(val title: String) {
    SETUP("Setup & Vehicle"),
    LOGS("Live Terminal")
}

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
    val errorMessage by manager.errorMessage.collectAsState()
    val logs by manager.logs.collectAsState()

    var activeSubTab by remember { mutableStateOf(SetupSubTab.SETUP) }
    var showSetupPackageDialog by remember { mutableStateOf(false) }

    // App selection state for setup: Balls, Android Auto, Music Mod checked by default
    var selectedApps by remember { mutableStateOf(ModApp.values().toSet()) }
    var isStepsExpanded by remember { mutableStateOf(true) }
    var logFilter by remember { mutableStateOf("ALL") }

    val terminalListState = rememberLazyListState()

    // Filter logs according to active filter chip
    val filteredLogs = remember(logs, logFilter) {
        when (logFilter) {
            "SUCCESS" -> logs.filter { it.contains("[+]") || it.contains("SUCCESS") || it.contains("CONGRATULATIONS") }
            "ERRORS" -> logs.filter { it.contains("[!]") || it.contains("failed") || it.contains("ERROR") }
            "INFO" -> logs.filter { it.contains("[*]") }
            else -> logs
        }
    }

    // Auto-scroll terminal on new log lines
    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            terminalListState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    // Auto-expand steps when setup is active
    LaunchedEffect(isRunning) {
        if (isRunning) {
            isStepsExpanded = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Top Segmented Bar: Switch between Setup Controls & Dedicated Live Terminal
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceDark)
                .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Setup & Vehicle SubTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activeSubTab == SetupSubTab.SETUP) SurfaceVariantDark else SurfaceDark)
                    .clickable { activeSubTab = SetupSubTab.SETUP }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = null,
                        tint = if (activeSubTab == SetupSubTab.SETUP) TextPrimary else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Setup",
                        fontSize = 12.sp,
                        fontWeight = if (activeSubTab == SetupSubTab.SETUP) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (activeSubTab == SetupSubTab.SETUP) TextPrimary else TextMuted
                    )
                }
            }

            // Live Terminal SubTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activeSubTab == SetupSubTab.LOGS) SurfaceVariantDark else SurfaceDark)
                    .clickable { activeSubTab = SetupSubTab.LOGS }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = null,
                        tint = if (activeSubTab == SetupSubTab.LOGS) TextPrimary else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Console",
                        fontSize = 12.sp,
                        fontWeight = if (activeSubTab == SetupSubTab.LOGS) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (activeSubTab == SetupSubTab.LOGS) TextPrimary else TextMuted
                    )
                    if (logs.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isRunning) AccentSkySubtle else SurfaceHighlight)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${logs.size}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isRunning) AccentSky else TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // View 1: Setup & Connection Wizard (Fully Scroll-Safe to eliminate clipping)
        if (activeSubTab == SetupSubTab.SETUP) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Connection & Scooter IP Card (Exact same card available on startup)
                VehicleConnectionCard(
                    manager = manager,
                    isRunning = isRunning
                )

                // Active Setup Status / Start Action Button
                if (isRunning) {
                    // Running Progress Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentMint, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Installing",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "${currentStep?.stepNum ?: 1}/5",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val progressFraction = ((currentStep?.stepNum ?: 1) / 5f)
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = AccentMint,
                                trackColor = SurfaceVariantDark
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentStep?.title ?: "Working",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                TextButton(
                                    onClick = { activeSubTab = SetupSubTab.LOGS },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) {
                                    Text("Logs", fontSize = 12.sp, color = TextPrimary)
                                }
                            }
                        }
                    }
                } else if (success) {
                    // Success Banner Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusOk, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Setup complete", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { showSetupPackageDialog = true },
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark, contentColor = TextPrimary)
                            ) {
                                Text("Run again", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else if (errorMessage != null) {
                    // Error Banner Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, StatusDanger.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Setup failed", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text(errorMessage ?: "Unknown error", fontSize = 12.sp, color = TextSecondary)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { showSetupPackageDialog = true },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = BgDark)
                                ) {
                                    Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                OutlinedButton(
                                    onClick = { activeSubTab = SetupSubTab.LOGS },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                                ) {
                                    Text("View logs", fontSize = 12.sp, color = TextPrimary)
                                }
                            }
                        }
                    }
                } else {
                    // Idle: Start Setup CTA Button (opens package selection popup)
                    Button(
                        onClick = {
                            showSetupPackageDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentMint,
                            contentColor = BgDark
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start setup",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Setup Steps Progress Accordion
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isStepsExpanded = !isStepsExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Steps",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextMuted
                            )
                            Icon(
                                imageVector = if (isStepsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (isStepsExpanded) {
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
                                            isDone -> Icon(Icons.Default.Check, contentDescription = null, tint = StatusOk, modifier = Modifier.size(13.dp))
                                            isCurrent -> CircularProgressIndicator(modifier = Modifier.size(12.dp), color = TextPrimary, strokeWidth = 2.dp)
                                            else -> Text(text = "${step.stepNum}", fontSize = 10.sp, color = TextMuted)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = step.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                        color = when {
                                            isDone -> TextPrimary
                                            isCurrent -> TextPrimary
                                            else -> TextMuted
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            // View 2: Dedicated Full-Height Live Terminal Screen (Zero Clipping, Full Visibility)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Bar: Quick Status, Copy, Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isRunning) AccentSkySubtle else SurfaceVariantDark)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isRunning) "Running" else "Idle",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isRunning) TextPrimary else TextMuted
                                )
                            }
                        }
                        Text(
                            text = scooterIp.ifBlank { "No target" },
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Copy Button
                        IconButton(
                            onClick = {
                                if (logs.isNotEmpty()) {
                                    val full = logs.joinToString("\n")
                                    clipboard.setText(AnnotatedString(full))
                                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceDark)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }

                        // Clear Button
                        IconButton(
                            onClick = { manager.clearLogs() },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceDark)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        "ALL" to "All (${logs.size})",
                        "SUCCESS" to "Success",
                        "ERRORS" to "Errors",
                        "INFO" to "Info"
                    )

                    filters.forEach { (key, label) ->
                        val isSelected = logFilter == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) AccentSkySubtle else SurfaceDark)
                                .border(1.dp, if (isSelected) TextMuted else CardBorder, RoundedCornerShape(6.dp))
                                .clickable { logFilter = key }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                        }
                    }
                }

                // Dedicated Full Terminal Window
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = TerminalBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (filteredLogs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Terminal, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (logs.isEmpty()) "No logs yet" else "Nothing matches",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    } else {
                        SelectionContainer {
                            LazyColumn(
                                state = terminalListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                items(filteredLogs) { logLine ->
                                    val color = when {
                                        logLine.contains("[+]") || logLine.contains("SUCCESS") || logLine.contains("CONGRATULATIONS") -> StatusOk
                                        logLine.contains("[!]") || logLine.contains("failed") || logLine.contains("ERROR") -> StatusDanger
                                        logLine.contains("[*]") -> TextPrimary
                                        logLine.startsWith("=") -> TextMuted
                                        else -> TextSecondary
                                    }
                                    val fontWeight = when {
                                        logLine.startsWith("[") && (logLine.contains("/5]") || logLine.contains("/6]")) -> FontWeight.Bold
                                        logLine.startsWith("=") -> FontWeight.Normal
                                        else -> FontWeight.Normal
                                    }
                                    Text(
                                        text = logLine,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = color,
                                        fontWeight = fontWeight,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }



    // 2. Package Selection & Pre-Check Dialog (triggered when clicking Start Automated Setup)
    if (showSetupPackageDialog) {
        AlertDialog(
            onDismissRequest = { showSetupPackageDialog = false },
            title = {
                Text("Choose mods", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ModApp Checkboxes
                    ModApp.values().forEach { app ->
                        val isChecked = selectedApps.contains(app)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceVariantDark)
                                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                .clickable(enabled = !app.isMandatory) {
                                    selectedApps = if (isChecked) selectedApps - app else selectedApps + app
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
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
                                    disabledCheckedColor = TextMuted
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = app.displayName + if (app.isMandatory) " · required" else "",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = app.description,
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Pre-flight Reminders Card
                    Text(
                        text = "Keep the side stand up and screen on. Use at your own risk.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSetupPackageDialog = false
                        coroutineScope.launch {
                            manager.runFullSetup(selectedApps)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = BgDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Install (${selectedApps.size})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetupPackageDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(12.dp)
        )
    }
}
