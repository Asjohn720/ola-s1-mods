package com.asjohn720.s1mods.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asjohn720.s1mods.model.ScooterManager
import com.asjohn720.s1mods.ui.components.VehicleConnectionCard
import com.asjohn720.s1mods.ui.theme.*

/**
 * Startup Connection Gate Screen.
 * Uses the exact same connection card and manual mode dialog as SetupScreen.
 */
@Composable
fun ConnectVehicleScreen(
    manager: ScooterManager,
    onBypass: () -> Unit = {},
    onOpenHelp: () -> Unit = {}
) {
    val isConnected by manager.isConnected.collectAsState()
    val scooterIp by manager.scooterIp.collectAsState()

    var showHelpDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // App Logo & Branding
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(AccentMintSubtle)
                .border(1.dp, AccentMint.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ElectricScooter,
                contentDescription = null,
                tint = AccentMint,
                modifier = Modifier.size(36.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "S1Mods",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Connect your Ola S1 to access vehicle controls and mods",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // The EXACT same Connection Card from SetupScreen
        VehicleConnectionCard(
            manager = manager,
            isRunning = false
        )

        // Proceed to Dashboard Button (When connected)
        if (isConnected) {
            Button(
                onClick = onBypass,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = BgDark)
            ) {
                Text(
                    text = "Proceed to Dashboard",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Quick Connection Steps Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = null,
                        tint = AccentSky,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quick Setup Guide",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentSky
                    )
                }

                Text(
                    text = "1. Turn ON Phone Hotspot.\n2. On Scooter Screen: Connect to your phone's Hotspot.\n3. Keep scooter screen awake.\n4. Tap 'Auto Scan'. If not found, tap 'Manual Mode' and enter Hotspot MAC or Scooter IP.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bypass / Offline Mode
        TextButton(
            onClick = onBypass,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isConnected) "Enter Dashboard" else "Continue in Offline Mode (Skip)",
                color = if (isConnected) AccentMint else TextMuted,
                fontSize = 12.sp
            )
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("How to Connect", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "1. Enable Hotspot on your phone.\n" +
                    "2. Connect your Ola Scooter to this Hotspot.\n" +
                    "3. Make sure the scooter display stays ON.\n" +
                    "4. Tap 'Auto Scan' or use 'Manual Mode'.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Got it", color = AccentMint)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(12.dp)
        )
    }
}
