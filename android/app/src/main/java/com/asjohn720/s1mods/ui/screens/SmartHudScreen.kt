package com.asjohn720.s1mods.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asjohn720.s1mods.model.ScooterManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// Zenith Lumina 2.0 Luxury Palette
private val ObsidianBlack = Color(0xFF05070A)
private val ChampagneGold = Color(0xFFE6D5B8)
private val SolarBronze = Color(0xFFD97706)
private val WarmAmber = Color(0xFFF59E0B)
private val SoftEmerald = Color(0xFF10B981)
private val CardSurface = Color(0xFF0C0F14)
private val CardBorderTone = Color(0xFF1C222D)
private val StoneMuted = Color(0xFF78716C)

@Composable
fun SmartHudScreen(manager: ScooterManager) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Interactive Telemetry State
    var currentSpeed by remember { mutableStateOf(64f) }
    var currentSoc by remember { mutableStateOf(82f) }
    var currentMode by remember { mutableStateOf("HYPER") }
    var isRegen by remember { mutableStateOf(false) }
    var leftBlinker by remember { mutableStateOf(false) }
    var rightBlinker by remember { mutableStateOf(false) }
    var highBeam by remember { mutableStateOf(true) }
    var cruiseControl by remember { mutableStateOf(false) }
    var sideStandUp by remember { mutableStateOf(true) }
    var isMusicPlaying by remember { mutableStateOf(true) }

    // Live clock
    var currentTimeString by remember { mutableStateOf("18:10") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            currentTimeString = sdf.format(Date())
            delay(1000)
        }
    }

    // Blinker Animation
    val infiniteTransition = rememberInfiniteTransition(label = "blinker")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blinkAlpha"
    )

    // Waveform bar animations
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "w1"
    )
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(550, delayMillis = 100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "w2"
    )
    val wave3 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850, delayMillis = 200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "w3"
    )

    // Animated Speed & Power
    val animatedSpeed by animateFloatAsState(targetValue = currentSpeed, label = "speed")
    val animatedSoc by animateFloatAsState(targetValue = currentSoc, label = "soc")

    val rangeKm = (animatedSoc * if (currentMode == "ECO") 1.7f else if (currentMode == "NORMAL") 1.4f else 1.15f).roundToInt()
    val powerKw = (animatedSpeed * 0.065f).let { String.format(Locale.US, "%.1f", it) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // TOP STATUS HORIZON
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Quick Toggles
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Left Signal
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardSurface)
                            .clickable { leftBlinker = !leftBlinker },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "◀",
                            color = if (leftBlinker) SoftEmerald.copy(alpha = blinkAlpha) else StoneMuted,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // High Beam
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { highBeam = !highBeam },
                        color = if (highBeam) Color(0x223B82F6) else CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (highBeam) Color(0x663B82F6) else CardBorderTone)
                    ) {
                        Text(
                            text = "💡 HIGH BEAM",
                            color = if (highBeam) Color(0xFF60A5FA) else StoneMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Cruise Control
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { cruiseControl = !cruiseControl },
                        color = if (cruiseControl) WarmAmber.copy(alpha = 0.15f) else CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (cruiseControl) WarmAmber else CardBorderTone)
                    ) {
                        Text(
                            text = if (cruiseControl) "⚙️ CRUISE 60" else "⚙️ CRUISE",
                            color = if (cruiseControl) WarmAmber else StoneMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Side Stand Status
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { sideStandUp = !sideStandUp },
                        color = if (sideStandUp) SoftEmerald.copy(alpha = 0.12f) else Color(0x33EF4444),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (sideStandUp) SoftEmerald.copy(alpha = 0.3f) else Color(0x88EF4444))
                    ) {
                        Text(
                            text = if (sideStandUp) "STAND UP" else "STAND DOWN",
                            color = if (sideStandUp) SoftEmerald else Color(0xFFEF4444),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Center Clock & Ambient Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = currentTimeString,
                        color = ChampagneGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text("•", color = StoneMuted)
                    Text("28°C Delhi", color = Color(0xFFA8A29E), fontSize = 11.sp)
                    Text("•", color = StoneMuted)
                    Text("ALT: 218m", color = WarmAmber, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                // Right Info & Blinker
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📶 4G", color = Color(0xFF60A5FA), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("ᛒ", color = ChampagneGold, fontSize = 11.sp)

                    Surface(
                        color = CardSurface,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderTone)
                    ) {
                        Text(
                            text = "PACK ${animatedSoc.roundToInt()}%",
                            color = WarmAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Right Signal
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardSurface)
                            .clickable { rightBlinker = !rightBlinker },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▶",
                            color = if (rightBlinker) SoftEmerald.copy(alpha = blinkAlpha) else StoneMuted,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(color = CardBorderTone, thickness = 1.dp)

            // MAIN CLUSTER INSTRUMENT BODY
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT: Concentric Dual-Halo
                Column(
                    modifier = Modifier.width(220.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(190.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidthOuter = 11.dp.toPx()
                            val strokeWidthInner = 7.dp.toPx()

                            // Outer background track
                            drawArc(
                                color = Color(0x14FFFFFF),
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(strokeWidthOuter, cap = StrokeCap.Round)
                            )
                            // Outer Power/Regen Arc
                            val powerProgress = (animatedSpeed / 120f).coerceIn(0f, 1f)
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(SoftEmerald, WarmAmber, SolarBronze)
                                ),
                                startAngle = 135f,
                                sweepAngle = 270f * powerProgress,
                                useCenter = false,
                                style = Stroke(strokeWidthOuter, cap = StrokeCap.Round)
                            )

                            // Inner Battery Track
                            drawArc(
                                color = Color(0x14FFFFFF),
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                topLeft = Offset(strokeWidthOuter + 8f, strokeWidthOuter + 8f),
                                size = Size(size.width - 2 * (strokeWidthOuter + 8f), size.height - 2 * (strokeWidthOuter + 8f)),
                                style = Stroke(strokeWidthInner, cap = StrokeCap.Round)
                            )
                            // Inner Battery Arc
                            val socProgress = (animatedSoc / 100f).coerceIn(0f, 1f)
                            drawArc(
                                color = SolarBronze,
                                startAngle = 135f,
                                sweepAngle = 270f * socProgress,
                                useCenter = false,
                                topLeft = Offset(strokeWidthOuter + 8f, strokeWidthOuter + 8f),
                                size = Size(size.width - 2 * (strokeWidthOuter + 8f), size.height - 2 * (strokeWidthOuter + 8f)),
                                style = Stroke(strokeWidthInner, cap = StrokeCap.Round)
                            )
                        }

                        // Center Telemetry inside Halo
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ENERGY", color = StoneMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Text(
                                text = "${animatedSoc.roundToInt()}%",
                                color = ChampagneGold,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Light,
                                fontFamily = FontFamily.Monospace
                            )
                            Text("+${powerKw} kW", color = WarmAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Wh/km Range Coach Pill
                    Surface(
                        modifier = Modifier.padding(top = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderTone)
                    ) {
                        Text(
                            text = if (currentSpeed <= 45f) "● 22 Wh/km (Optimal)" else if (currentSpeed <= 75f) "● 32 Wh/km (Cruising)" else "▲ 52 Wh/km (High)",
                            color = if (currentSpeed <= 45f) SoftEmerald else if (currentSpeed <= 75f) WarmAmber else Color(0xFFEF4444),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // CENTER: Giant Architectural Speedometer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    // Speed Limit Assist
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderTone)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color.Red, CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("60", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("LIMIT 60", color = StoneMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Main Speed Readout
                    Text(
                        text = "${animatedSpeed.roundToInt()}",
                        color = Color.White,
                        fontSize = 105.sp,
                        fontWeight = FontWeight.ExtraLight,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 105.sp
                    )
                    Text(
                        text = "KM / H",
                        color = WarmAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp
                    )

                    // Trip & Odo Micro Metrics
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("AVG 48 km/h", color = StoneMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("•", color = CardBorderTone)
                        Text("TRIP 42.8 km", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("•", color = CardBorderTone)
                        Text("ODO 1,482 km", color = StoneMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                // RIGHT: Grand Touring Navigation & Acoustic Media
                Column(
                    modifier = Modifier.width(230.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Navigation Pod
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarmAmber.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(WarmAmber.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("↱", color = WarmAmber, fontSize = 24.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("In 180 m", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                    Text("NH-48 Flyover", color = Color(0xFFA8A29E), fontSize = 11.sp)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = CardBorderTone, thickness = 1.dp)
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ETA 18:34", color = WarmAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("LANE 3/3 ↗", color = SoftEmerald, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    // Acoustic Waveform Audio Pod
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderTone),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(WarmAmber.copy(alpha = 0.2f))
                                        .clickable { isMusicPlaying = !isMusicPlaying },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(if (isMusicPlaying) "⏸" else "▶", color = WarmAmber, fontSize = 10.sp)
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.width(100.dp)) {
                                    Text("Starboy", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text("The Weeknd", color = StoneMuted, fontSize = 9.sp, maxLines = 1)
                                }
                            }

                            // Dynamic Wave Bars
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier.height(18.dp)
                            ) {
                                Box(Modifier.width(3.dp).fillMaxHeight(if (isMusicPlaying) wave1 else 0.2f).background(WarmAmber, CircleShape))
                                Box(Modifier.width(3.dp).fillMaxHeight(if (isMusicPlaying) wave2 else 0.2f).background(WarmAmber, CircleShape))
                                Box(Modifier.width(3.dp).fillMaxHeight(if (isMusicPlaying) wave3 else 0.2f).background(WarmAmber, CircleShape))
                                Box(Modifier.width(3.dp).fillMaxHeight(if (isMusicPlaying) wave1 else 0.2f).background(WarmAmber, CircleShape))
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = CardBorderTone, thickness = 1.dp)

            // SUB BAR (Drive Mode & Estimated Range & Failsafe Recovery)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // MoveOS Mode Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("MODE:", color = StoneMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    listOf("ECO", "NORMAL", "SPORT", "HYPER").forEach { m ->
                        val isSel = currentMode == m
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) WarmAmber.copy(alpha = 0.2f) else CardSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) WarmAmber else CardBorderTone),
                            modifier = Modifier.clickable { currentMode = m }
                        ) {
                            Text(
                                text = m,
                                color = if (isSel) WarmAmber else StoneMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Failsafe Speedometer Recover Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x22EF4444),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x55EF4444)),
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            try {
                                manager.openDashboardHome()
                                Toast.makeText(context, "MoveOS Cluster Restored", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text(
                        text = "RESTORE STOCK HMI",
                        color = Color(0xFFF87171),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Estimated Endurance Range
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ENDURANCE: ", color = StoneMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "$rangeKm KM",
                        color = WarmAmber,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

        }
    }
}
