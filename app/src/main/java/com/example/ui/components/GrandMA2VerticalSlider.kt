package com.example.ui.components

import android.graphics.Paint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StageAmber
import com.example.ui.theme.StageBlack
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageCyan
import com.example.ui.theme.StageGreen
import com.example.ui.theme.StageRed
import com.example.ui.theme.StageSurface
import com.example.ui.theme.StageSurfaceElevated
import com.example.ui.theme.StageSurfaceVariant
import com.example.ui.theme.StageTextMuted
import com.example.ui.theme.StageTextPrimary
import com.example.ui.theme.StageTextSecondary
import kotlin.math.roundToInt

/**
 * grandMA2 console authentic vertical fader component in Jetpack Compose.
 *
 * Recreates the physical hardware aesthetic of grandMA2 100mm motorized faders:
 * - Recessed fader groove track with illuminated level glow
 * - Tactile, beveled grandMA2 metallic fader knob cap with grip ridges & illuminated index notch
 * - Authentic 0–100% scale graduation tick marks
 * - Dual percentage (%) and 8-bit DMX (0–255) readouts
 * - Fluid vertical drag and direct tap-to-level gestures
 * - Momentary FLASH button support and precision +/- step nudging
 */
@Composable
fun GrandMA2VerticalSlider(
    level: Float,
    onLevelChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Exec",
    execIdentifier: String = "1.1",
    isMaster: Boolean = false,
    color: Color = StageCyan,
    showScaleTicks: Boolean = true,
    showDmxValue: Boolean = true,
    showFlashButton: Boolean = true,
    showFineTuning: Boolean = true,
    showQuickPresets: Boolean = true,
    hapticFeedbackEnabled: Boolean = true,
    enabled: Boolean = true,
    onFlashPress: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val clampedLevel = level.coerceIn(0f, 1f)
    val percent = (clampedLevel * 100f).roundToInt()
    val dmxValue = (clampedLevel * 255f).roundToInt()

    // Level state for momentary flash release
    var levelBeforeFlash by remember { mutableFloatStateOf(clampedLevel) }
    var isFlashActive by remember { mutableStateOf(false) }

    val activeColor = if (isMaster) StageRed else color

    Column(
        modifier = modifier
            .testTag("grandma2_fader_$execIdentifier")
            .background(StageSurfaceElevated, RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = if (isMaster) StageRed.copy(alpha = 0.7f) else StageBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- 1. Top Header: Hardware Label & Digital Level Readout ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Exec / Master Tag Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isMaster) StageRed.copy(alpha = 0.2f) else activeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isMaster) "GRAND MASTER" else "EXEC $execIdentifier",
                    color = activeColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }

            // User Cue / Fixture Label
            Text(
                text = label.ifBlank { if (isMaster) "Master Intensity" else "Executor $execIdentifier" },
                color = StageTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )

            // Digital MA2 Intensity Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF090B10))
                    .border(1.dp, activeColor.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = if (percent == 100) "FULL" else if (percent == 0) "OFF" else "$percent%",
                        color = activeColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("fader_value_text_$execIdentifier")
                    )
                    if (showDmxValue) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "DMX $dmxValue",
                            color = StageTextMuted,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                    }
                }
            }
        }

        // --- 2. Main Fader Track Area with Scale Graduation & Tactile Knob ---
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            val totalTrackHeightPx = constraints.maxHeight.toFloat()
            val knobHeightDp = 34.dp
            val knobWidthDp = 44.dp
            val knobHeightPx = with(LocalDensity.current) { knobHeightDp.toPx() }
            val travelRangePx = (totalTrackHeightPx - knobHeightPx).coerceAtLeast(1f)

            // Current offset from top of track
            val knobOffsetYPx = ((1f - clampedLevel) * travelRangePx).coerceIn(0f, travelRangePx)

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(enabled, totalTrackHeightPx) {
                        if (!enabled) return@pointerInput
                        detectDragGestures(
                            onDragStart = { offset ->
                                val targetNorm = 1f - ((offset.y - (knobHeightPx / 2f)) / travelRangePx).coerceIn(0f, 1f)
                                if (hapticFeedbackEnabled) HapticUtil.performHaptic(context)
                                onLevelChange(targetNorm)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val deltaNorm = -dragAmount.y / travelRangePx
                                val nextLevel = (clampedLevel + deltaNorm).coerceIn(0f, 1f)
                                if (hapticFeedbackEnabled && (nextLevel == 0f || nextLevel == 1f || (nextLevel * 100).toInt() % 25 == 0)) {
                                    HapticUtil.performHaptic(context)
                                }
                                onLevelChange(nextLevel)
                            }
                        )
                    }
                    .pointerInput(enabled, totalTrackHeightPx) {
                        if (!enabled) return@pointerInput
                        detectTapGestures { offset ->
                            val targetNorm = 1f - ((offset.y - (knobHeightPx / 2f)) / travelRangePx).coerceIn(0f, 1f)
                            if (hapticFeedbackEnabled) HapticUtil.performHaptic(context)
                            onLevelChange(targetNorm)
                        }
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Side Graduation Scale Ticks (Left)
                if (showScaleTicks) {
                    FaderScaleTicks(
                        modifier = Modifier
                            .width(18.dp)
                            .fillMaxHeight()
                            .padding(vertical = with(LocalDensity.current) { (knobHeightPx / 2f).toDp() }),
                        tickColor = StageTextMuted
                    )
                }

                // Central Recessed Fader Slot + Cap
                Box(
                    modifier = Modifier
                        .width(knobWidthDp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Recessed Guide Slot (Slit)
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFF07080C))
                            .border(1.dp, Color(0xFF1E2330), RoundedCornerShape(5.dp))
                    ) {
                        // Level Illumination Glow Tube (Fills upward from bottom)
                        val fillHeightFraction = clampedLevel.coerceIn(0.01f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction = fillHeightFraction)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            activeColor.copy(alpha = 0.85f),
                                            activeColor.copy(alpha = 0.45f),
                                            activeColor.copy(alpha = 0.15f)
                                        )
                                    )
                                )
                        )
                    }

                    // Authentic grandMA2 Motorized Fader Knob (Cap)
                    GrandMA2FaderCap(
                        modifier = Modifier
                            .offset { IntOffset(0, knobOffsetYPx.roundToInt()) }
                            .size(width = knobWidthDp, height = knobHeightDp)
                            .testTag("fader_knob_$execIdentifier"),
                        accentColor = activeColor,
                        isMaster = isMaster
                    )
                }

                // Right Graduation Scale Ticks (Right)
                if (showScaleTicks) {
                    FaderScaleTicks(
                        modifier = Modifier
                            .width(18.dp)
                            .fillMaxHeight()
                            .padding(vertical = with(LocalDensity.current) { (knobHeightPx / 2f).toDp() }),
                        tickColor = StageTextMuted,
                        isRightSide = true
                    )
                }
            }
        }

        // --- 3. Bottom Controls: Flash, Nudge, & Quick Jumps ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Fine Tuning Step Nudges (-1% / +1%)
            if (showFineTuning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    StepNudgeButton(
                        text = "-1%",
                        onClick = {
                            if (hapticFeedbackEnabled) HapticUtil.performHaptic(context)
                            onLevelChange((clampedLevel - 0.01f).coerceIn(0f, 1f))
                        },
                        modifier = Modifier.weight(1f)
                    )
                    StepNudgeButton(
                        text = "+1%",
                        onClick = {
                            if (hapticFeedbackEnabled) HapticUtil.performHaptic(context)
                            onLevelChange((clampedLevel + 0.01f).coerceIn(0f, 1f))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Preset Jumps (0% / 50% / FULL)
            if (showQuickPresets) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    PresetLevelButton(
                        label = "0%",
                        onClick = {
                            if (hapticFeedbackEnabled) HapticUtil.performHaptic(context)
                            onLevelChange(0.0f)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("fader_zero_button_$execIdentifier")
                    )
                    PresetLevelButton(
                        label = "50%",
                        onClick = {
                            if (hapticFeedbackEnabled) HapticUtil.performHaptic(context)
                            onLevelChange(0.5f)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PresetLevelButton(
                        label = "FULL",
                        onClick = {
                            if (hapticFeedbackEnabled) HapticUtil.performHaptic(context)
                            onLevelChange(1.0f)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("fader_full_button_$execIdentifier")
                    )
                }
            }

            // grandMA2 Momentary Flash Button
            if (showFlashButton) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isFlashActive) activeColor else activeColor.copy(alpha = 0.18f)
                        )
                        .border(
                            1.dp,
                            if (isFlashActive) activeColor else activeColor.copy(alpha = 0.5f),
                            RoundedCornerShape(6.dp)
                        )
                        .pointerInput(enabled, execIdentifier) {
                            if (!enabled) return@pointerInput
                            detectTapGestures(
                                onPress = {
                                    isFlashActive = true
                                    levelBeforeFlash = clampedLevel
                                    if (hapticFeedbackEnabled) HapticUtil.performHaptic(context, isStrong = true)
                                    onFlashPress?.invoke(true)
                                    onLevelChange(1.0f)

                                    tryAwaitRelease()

                                    isFlashActive = false
                                    if (hapticFeedbackEnabled) HapticUtil.performHaptic(context)
                                    onFlashPress?.invoke(false)
                                    onLevelChange(levelBeforeFlash)
                                }
                            )
                        }
                        .testTag("fader_flash_button_$execIdentifier"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Flash Button",
                            tint = if (isFlashActive) StageBlack else activeColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "FLASH",
                            color = if (isFlashActive) StageBlack else activeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

/**
 * grandMA2 Ergonomic Beveled Fader Knob / Cap.
 * Recreates the authentic console motorized fader cap with metallic body,
 * grip grooves, and illuminated center index notch.
 */
@Composable
private fun GrandMA2FaderCap(
    accentColor: Color,
    isMaster: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(6.dp), spotColor = Color.Black)
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE2E8F0), // Top bevel highlight
                        Color(0xFF94A3B8), // Mid aluminum
                        Color(0xFF475569), // Dark anodized base
                        Color(0xFF1E293B)  // Bottom shadow
                    )
                )
            )
            .border(1.dp, Color(0xFF64748B), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Horizontal Grip Ridges & Illuminated Index Center Line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            // Upper Grip Notch
            Box(
                modifier = Modifier
                    .size(width = 24.dp, height = 2.dp)
                    .background(Color(0xFF334155), RoundedCornerShape(1.dp))
            )

            // Center Illuminated Index Notch (Glows with accentColor)
            Box(
                modifier = Modifier
                    .size(width = 30.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
                    .border(0.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
            )

            // Lower Grip Notch
            Box(
                modifier = Modifier
                    .size(width = 24.dp, height = 2.dp)
                    .background(Color(0xFF334155), RoundedCornerShape(1.dp))
            )
        }
    }
}

/**
 * Scale tick marks along the fader travel path (100, 75, 50, 25, 0).
 */
@Composable
private fun FaderScaleTicks(
    modifier: Modifier = Modifier,
    tickColor: Color = StageTextMuted,
    isRightSide: Boolean = false
) {
    Canvas(modifier = modifier) {
        val totalH = size.height
        val totalW = size.width

        // Major graduations at 0%, 25%, 50%, 75%, 100%
        val steps = listOf(0.0f, 0.25f, 0.50f, 0.75f, 1.0f)
        steps.forEach { stepNorm ->
            val y = (1f - stepNorm) * totalH
            val tickLen = totalW * 0.85f
            val startX = if (isRightSide) 0f else totalW - tickLen
            val endX = if (isRightSide) tickLen else totalW

            drawLine(
                color = if (stepNorm == 1.0f || stepNorm == 0.0f) Color.White.copy(alpha = 0.6f) else tickColor,
                start = Offset(startX, y),
                end = Offset(endX, y),
                strokeWidth = if (stepNorm == 1.0f || stepNorm == 0.0f) 1.5.dp.toPx() else 1.dp.toPx()
            )
        }

        // Minor ticks every 10%
        for (i in 1..9) {
            if (i == 5) continue // handled by 50%
            val stepNorm = i / 10f
            val y = (1f - stepNorm) * totalH
            val minorLen = totalW * 0.45f
            val startX = if (isRightSide) 0f else totalW - minorLen
            val endX = if (isRightSide) minorLen else totalW

            drawLine(
                color = tickColor.copy(alpha = 0.4f),
                start = Offset(startX, y),
                end = Offset(endX, y),
                strokeWidth = 0.8.dp.toPx()
            )
        }
    }
}

@Composable
private fun StepNudgeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF141822))
            .border(1.dp, StageBorder, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = StageTextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun PresetLevelButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(26.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF161B26))
            .border(1.dp, StageBorder, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (label == "FULL") Color.White else StageTextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
