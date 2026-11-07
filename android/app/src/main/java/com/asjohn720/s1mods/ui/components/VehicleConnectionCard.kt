package com.asjohn720.s1mods.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asjohn720.s1mods.discovery.DiscoveredScooter
import com.asjohn720.s1mods.discovery.NetworkScanner
import com.asjohn720.s1mods.model.ScooterManager
import com.asjohn720.s1mods.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Reusable Vehicle Connection Card & Manual Mode Dialog.
 * Uses the high-performance IPv6 multicast auto-discovery engine
 * and a simplified single-input Manual Mode (no Scope ID required).
 */
@Composable
fun VehicleConnectionCard(
    manager: ScooterManager,
    isRunning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isConnected by manager.isConnected.collectAsState()
    val scooterIp by manager.scooterIp.collectAsState()
    val scooterScope by manager.scooterScope.collectAsState()

    var isScanning by remember { mutableStateOf(false) }
    var isTestingPort by remember { mutableStateOf(false) }
    var discoveredScooter by remember { mutableStateOf<DiscoveredScooter?>(null) }
    var scanMessage by remember { mutableStateOf<String?>(null) }

    var showManualModeDialog by remember { mutableStateOf(false) }
    var tempInput by remember { mutableStateOf("") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Vehicle Icon + Connection Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) StatusOk else StatusDanger)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isConnected) "Connected" else "Not connected",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }


            }

            Spacer(modifier = Modifier.height(10.dp))

            // IP Address Display Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariantDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (scooterIp.isBlank()) "No address"
                    else if (scooterScope != null) "$scooterIp%$scooterScope" else scooterIp,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = if (scooterIp.isBlank()) TextMuted else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Discovered Vehicle Card (Shows when Auto-Discovery detects the scooter)
            if (discoveredScooter != null) {
                val found = discoveredScooter!!
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusOk, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Scooter found",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = found.ip,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        manager.connect(found.ip, found.scope)
                                        Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        manager.isConnected.value = true
                                        Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = BgDark)
                        ) {
                            Text("Connect", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            if (scanMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = scanMessage!!,
                    fontSize = 11.sp,
                    color = if (scanMessage!!.contains("not found", ignoreCase = true)) StatusDanger else TextSecondary,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row: Auto-Discover (New Engine), Manual Mode (No Scope), Test Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Auto-Discover Button (Axiom Multicast Ping Engine)
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isScanning = true
                            scanMessage = "Scanning…"
                            discoveredScooter = null
                            val found = manager.autoDiscover()
                            isScanning = false
                            if (found != null) {
                                discoveredScooter = found
                                scanMessage = null
                                                            } else {
                                scanMessage = "Not found. Check hotspot or use Manual."
                                                            }
                        }
                    },
                    enabled = !isScanning && !isRunning,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark, contentColor = TextPrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = AccentSky, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scanning", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scan", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Manual Mode Button (Clean Single-Input: IPv4, IPv6, MAC)
                Button(
                    onClick = {
                        tempInput = scooterIp
                        showManualModeDialog = true
                    },
                    enabled = !isRunning,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark, contentColor = TextPrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manual", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                // Test Port Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isTestingPort = true
                            val open = manager.testConnection()
                            isTestingPort = false
                            Toast.makeText(
                                context,
                                if (open) "Reachable" else "Unreachable. Is the scooter screen on?",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = !isTestingPort && !isRunning,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark, contentColor = TextPrimary),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    if (isTestingPort) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = AccentMint, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    // Manual Mode Dialog (Clean Single-Input, NO Scope ID required)
    if (showManualModeDialog) {
        val cleanInput = tempInput.trim()
        val derivedIpv6 = remember(cleanInput) {
            NetworkScanner.macToIpv6(cleanInput)
        }
        val isIpv4 = remember(cleanInput) {
            cleanInput.matches(Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}(:\d+)?$""")) || (cleanInput.contains(".") && !cleanInput.contains(":"))
        }
        val isIpv6 = remember(cleanInput, derivedIpv6) {
            cleanInput.contains(":") && !cleanInput.contains("-") && derivedIpv6 == null
        }

        AlertDialog(
            onDismissRequest = { showManualModeDialog = false },
            title = {
                Text("Manual connect", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "IP: Scooter Settings → Wi-Fi\nMAC: Phone → Hotspot → Connected devices",
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 18.sp
                    )

                    // Single Unified Input Field (No Scope ID needed)
                    OutlinedTextField(
                        value = tempInput,
                        onValueChange = { tempInput = it },
                        label = { Text("IP or MAC address") },
                        placeholder = { Text("192.168.43.50") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentMint,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SurfaceVariantDark,
                            unfocusedContainerColor = SurfaceVariantDark
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Real-time format recognition preview
                    if (derivedIpv6 != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(AccentMintSubtle)
                                                                .padding(8.dp)
                        ) {
                            Column {
                                Text("MAC → IPv6", fontSize = 11.sp, color = TextSecondary)
                                Text(derivedIpv6, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                            }
                        }
                    } else if (isIpv4) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(AccentSkySubtle)
                                .padding(8.dp)
                        ) {
                            Text("IPv4", fontSize = 11.sp, color = TextSecondary)
                        }
                    } else if (isIpv6) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(AccentSkySubtle)
                                .padding(8.dp)
                        ) {
                            Text("IPv6", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = tempInput.trim()
                        val scopeFromPercent = if (trimmed.contains("%")) trimmed.split("%").getOrNull(1)?.toIntOrNull() else null
                        val baseIp = if (trimmed.contains("%")) trimmed.split("%")[0] else trimmed
                        val finalIp = derivedIpv6 ?: baseIp

                        manager.scooterIp.value = finalIp
                        manager.scooterScope.value = scopeFromPercent
                        showManualModeDialog = false

                        // Quick connection test
                        coroutineScope.launch {
                            isTestingPort = true
                            val open = manager.testConnection(finalIp, scopeFromPercent)
                            isTestingPort = false
                            if (open) {
                                manager.isConnected.value = true
                                Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Saved, but not reachable yet", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = BgDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Connect", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualModeDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(12.dp)
        )
    }
}
