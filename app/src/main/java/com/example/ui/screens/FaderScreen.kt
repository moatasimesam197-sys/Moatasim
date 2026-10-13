package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FaderItem
import com.example.ui.OscViewModel
import com.example.ui.components.HapticUtil
import com.example.ui.theme.StageAmber
import com.example.ui.theme.StageBlack
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageCyan
import com.example.ui.theme.StageGreen
import com.example.ui.theme.StageRed
import com.example.ui.theme.StageSurface
import com.example.ui.theme.StageSurfaceElevated
import com.example.ui.theme.StageTextMuted
import com.example.ui.theme.StageTextPrimary
import com.example.ui.theme.StageTextSecondary
import kotlin.math.roundToInt

@Composable
fun FaderScreen(
    viewModel: OscViewModel,
    modifier: Modifier = Modifier
) {
    val layout = viewModel.layout.value
    val currentPage = viewModel.currentPage.value
    val config = viewModel.config.value
    val hapticEnabled = config.hapticEnabled
    val context = LocalContext.current

    val masterFader = layout.faders.firstOrNull { it.isMaster }
    val regularFaders = layout.faders.filterNot { it.isMaster }

    var selectedBank by remember { mutableIntStateOf(0) } // 0: All Faders, 1: 1-4, 2: 5+

    val displayedFaders = remember(regularFaders, selectedBank) {
        when (selectedBank) {
            1 -> regularFaders.take(4)
            2 -> regularFaders.drop(4)
            else -> regularFaders
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StageBlack)
    ) {
        // --- 1. Top Rate-Limiting & Engine Status Banner ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StageSurface)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(StageGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "CONSOLE FADER DESK",
                        color = StageCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Rate-Limit: ${config.faderRateLimitHz} Hz max (33ms interval) • Trailing edge protection",
                        color = StageTextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Quick Bank Filter Chips
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                BankFilterChip("ALL", selected = selectedBank == 0) { selectedBank = 0 }
                if (regularFaders.size > 4) {
                    BankFilterChip("1-4", selected = selectedBank == 1) { selectedBank = 1 }
                    BankFilterChip("5+", selected = selectedBank == 2) { selectedBank = 2 }
                }
            }
        }

        // --- 2. Main Fader Bank Panel ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Pinned Grand Master Fader on the left
            masterFader?.let { master ->
                StageVerticalFader(
                    fader = master,
                    currentPage = currentPage,
                    rateLimitHz = config.faderRateLimitHz,
                    onLevelChange = { newLevel ->
                        if (hapticEnabled) HapticUtil.performHaptic(context)
                        viewModel.setFaderLevel(master, newLevel)
                    },
                    onOffClick = {
                        viewModel.oscService.sendCommand("SpecialMaster 2.1 At 0")
                    },
                    modifier = Modifier
                        .width(96.dp)
                        .testTag("grand_master_fader")
                )

                // High-contrast console chassis separator
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(StageBorder)
                )
            }

            // Horizontally scrollable list of regular executor faders
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                displayedFaders.forEach { fader ->
                    StageVerticalFader(
                        fader = fader,
                        currentPage = currentPage,
                        rateLimitHz = config.faderRateLimitHz,
                        onLevelChange = { newLevel ->
                            if (hapticEnabled) HapticUtil.performHaptic(context)
                            viewModel.setFaderLevel(fader, newLevel)
                        },
                        onOffClick = {
                            val p = if (fader.page > 0) fader.page else currentPage
                            viewModel.oscService.sendCommand("Off Exec $p.${fader.execNumber}")
                        },
                        modifier = Modifier
                            .width(88.dp)
                            .testTag("exec_fader_${fader.execNumber}")
                    )
                }
            }
        }
    }
}

@Composable
fun BankFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) StageCyan.copy(alpha = 0.2f) else Color(0xFF161A24))
            .border(1.dp, if (selected) StageCyan else StageBorder, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) StageCyan else StageTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun StageVerticalFader(
    fader: FaderItem,
    currentPage: Int,
    rateLimitHz: Int,
    onLevelChange: (Float) -> Unit,
    onOffClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val faderColor = if (fader.isMaster) StageRed else {
        try {
            Color(android.graphics.Color.parseColor(fader.colorHex))
        } catch (e: Exception) {
            StageCyan
        }
    }

    val percent = (fader.level * 100f).roundToInt()
    val dmxByte = (fader.level * 255f).roundToInt()

    var previousLevelBeforeFlash by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(StageSurfaceElevated, RoundedCornerShape(12.dp))
            .border(
                width = if (fader.isMaster) 1.5.dp else 1.dp,
                color = if (fader.isMaster) StageRed.copy(alpha = 0.8f) else StageBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- Header Section: Exec Number, Label, and Numerical Badges ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (fader.isMaster) "MASTER" else "EXEC ${fader.page}.${fader.execNumber}",
                color = faderColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
            Text(
                text = fader.label,
                color = StageTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Dual Readout: Percentage and DMX Byte
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0A0C11))
                    .border(1.dp, faderColor.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$percent%",
                    color = faderColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "($dmxByte)",
                    color = StageTextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // --- Vertical Fader Track with Calibrated Offset Handle & Level Bar ---
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            val trackHeightPx = constraints.maxHeight.toFloat()
            val knobHeightDp = 34.dp
            val totalTrackHeightDp = maxHeight
            val travelDistanceDp = (totalTrackHeightDp - knobHeightDp).coerceAtLeast(0.dp)

            // Tick Mark Scale Lines alongside track
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FaderTickMarks(modifier = Modifier.width(10.dp).fillMaxHeight())

                // Physical Slot Track
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(17.dp))
                        .background(Color(0xFF08090C))
                        .border(1.dp, StageBorder, RoundedCornerShape(17.dp))
                        .pointerInput(fader.id, trackHeightPx) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val norm = 1f - (offset.y / trackHeightPx).coerceIn(0f, 1f)
                                    onLevelChange(norm)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val norm = 1f - (change.position.y / trackHeightPx).coerceIn(0f, 1f)
                                    onLevelChange(norm)
                                }
                            )
                        }
                        .pointerInput(fader.id) {
                            detectTapGestures { offset ->
                                val norm = 1f - (offset.y / trackHeightPx).coerceIn(0f, 1f)
                                onLevelChange(norm)
                            }
                        }
                ) {
                    // Illuminated Filled Level Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction = fader.level.coerceIn(0f, 1f))
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        faderColor,
                                        faderColor.copy(alpha = 0.3f)
                                    )
                                )
                            )
                    )

                    // Physically Moving 3D Tactile Fader Handle Knob
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(knobHeightDp)
                            .align(Alignment.TopCenter)
                            .offset(y = travelDistanceDp * (1f - fader.level))
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFFFFFFFF), Color(0xFFCBD5E1), Color(0xFF94A3B8))
                                )
                            )
                            .border(2.dp, faderColor, RoundedCornerShape(6.dp))
                    ) {
                        // Center index grip line & tactile ridges
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 18.dp, height = 3.dp)
                                    .background(faderColor, RoundedCornerShape(1.dp))
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(width = 12.dp, height = 1.dp)
                                    .background(Color(0xFF334155))
                            )
                        }
                    }
                }

                FaderTickMarks(modifier = Modifier.width(10.dp).fillMaxHeight())
            }
        }

        // --- Bottom Tactile Buttons: FLASH, OFF, 0%, FULL ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Momentary FLASH Button (Press Down -> 100%, Release -> Restore Level)
            var isFlashing by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isFlashing) Color.White.copy(alpha = 0.4f) else faderColor.copy(alpha = 0.18f))
                    .border(1.dp, if (isFlashing) Color.White else faderColor, RoundedCornerShape(6.dp))
                    .pointerInput(fader.id) {
                        detectTapGestures(
                            onPress = {
                                isFlashing = true
                                previousLevelBeforeFlash = fader.level
                                onLevelChange(1.0f)
                                tryAwaitRelease()
                                isFlashing = false
                                onLevelChange(previousLevelBeforeFlash)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Flash",
                        tint = if (isFlashing) Color.White else faderColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "FLASH",
                        color = if (isFlashing) Color.White else faderColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Quick Preset Jumps (0%, FULL, OFF)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                QuickLevelPresetButton(
                    label = "0%",
                    modifier = Modifier.weight(1f)
                ) { onLevelChange(0.0f) }

                QuickLevelPresetButton(
                    label = "50%",
                    modifier = Modifier.weight(1f)
                ) { onLevelChange(0.5f) }

                QuickLevelPresetButton(
                    label = "FULL",
                    modifier = Modifier.weight(1.1f),
                    color = faderColor
                ) { onLevelChange(1.0f) }

                // OFF / MUTE Button
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF261014))
                        .border(1.dp, StageRed.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .clickable { onOffClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "OFF",
                        color = StageRed,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun QuickLevelPresetButton(
    label: String,
    modifier: Modifier = Modifier,
    color: Color = StageTextSecondary,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF141822))
            .border(1.dp, StageBorder, RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun FaderTickMarks(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        listOf("100", "75", "50", "25", "0").forEach { mark ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 1.dp)
                        .background(StageTextMuted.copy(alpha = 0.5f))
                )
            }
        }
    }
}
