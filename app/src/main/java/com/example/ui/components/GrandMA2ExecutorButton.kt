package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExecutorAction
import com.example.model.ExecutorButton
import com.example.ui.theme.StageAmber
import com.example.ui.theme.StageBlack
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageCyan
import com.example.ui.theme.StageGreen
import com.example.ui.theme.StageRed
import com.example.ui.theme.StageSurfaceElevated
import com.example.ui.theme.StageSurfaceVariant
import com.example.ui.theme.StageTextMuted
import com.example.ui.theme.StageTextPrimary
import com.example.ui.theme.StageTextSecondary

/**
 * Reusable grandMA2 Executor button component in Jetpack Compose.
 *
 * Models physical console playback buttons:
 * - Dedicated press/release tracking for Go, Flash, Temp, and Off actions
 * - Visual feedback for active cues: glowing neon LED, pulsing cue state, and illuminated border
 * - Mechanical tactile button press animation (scale, elevation & color wash)
 * - Built-in quick Go/Off playback toggle button
 * - Long-press affordance for configuration
 */
@Composable
fun GrandMA2ExecutorButton(
    execNumber: Int,
    page: Int,
    label: String,
    action: ExecutorAction = ExecutorAction.GO,
    colorHex: String = "#FFAB00",
    isActiveCue: Boolean = false,
    activeCueLabel: String? = null,
    onPressChange: ((isDown: Boolean) -> Unit)? = null,
    onTogglePlayback: (() -> Unit)? = null,
    onConfigureClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    hapticFeedbackEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPressedDown by remember { mutableStateOf(false) }

    // Parse button base accent color safely
    val baseColor = remember(colorHex) {
        try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            StageAmber
        }
    }

    // Subtle pulsing animation when active cue is playing
    val infiniteTransition = rememberInfiniteTransition(label = "cue_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cue_pulse_alpha"
    )

    // Animated press depth scale
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressedDown) 0.96f else 1.0f,
        animationSpec = tween(durationMillis = 80),
        label = "btn_press_scale"
    )

    // Dynamic background color matching press & active cue states
    val targetBackgroundColor by animateColorAsState(
        targetValue = when {
            isPressedDown -> baseColor.copy(alpha = 0.55f)
            isActiveCue -> baseColor.copy(alpha = 0.28f)
            else -> StageSurfaceElevated
        },
        label = "btn_bg_color"
    )

    // Dynamic border color
    val targetBorderColor by animateColorAsState(
        targetValue = when {
            isPressedDown -> Color.White
            isActiveCue -> if (action == ExecutorAction.FLASH) StageRed else baseColor
            else -> StageBorder
        },
        label = "btn_border_color"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = buttonScale
                scaleY = buttonScale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(targetBackgroundColor)
            .border(
                width = if (isActiveCue || isPressedDown) 2.dp else 1.dp,
                color = targetBorderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .pointerInput(enabled, execNumber, page) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        isPressedDown = true
                        if (hapticFeedbackEnabled) {
                            HapticUtil.performHaptic(context, isStrong = action == ExecutorAction.FLASH)
                        }
                        onPressChange?.invoke(true)

                        val released = tryAwaitRelease()

                        isPressedDown = false
                        if (hapticFeedbackEnabled) {
                            HapticUtil.performHaptic(context, isStrong = false)
                        }
                        onPressChange?.invoke(false)
                    },
                    onLongPress = {
                        if (hapticFeedbackEnabled) {
                            HapticUtil.performHaptic(context, isStrong = true)
                        }
                        onConfigureClick?.invoke()
                    }
                )
            }
            .padding(10.dp)
            .testTag("executor_btn_${page}_$execNumber")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- Top Header: Hardware Exec Identifier & Action Badge ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EXEC $page.$execNumber",
                    color = if (isActiveCue) baseColor else StageTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("exec_id_text_${page}_$execNumber")
                )

                // Action Type Badge (GO, FLASH, TEMP, TOGGLE, OFF)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isActiveCue) baseColor.copy(alpha = 0.35f) else baseColor.copy(alpha = 0.18f)
                        )
                        .border(
                            0.5.dp,
                            if (isActiveCue) baseColor else baseColor.copy(alpha = 0.4f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = action.name,
                        color = if (isActiveCue) Color.White else baseColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // --- Middle: Cue / Function Label & Active Cue Info ---
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = label.ifBlank { "Executor $execNumber" },
                    color = if (isActiveCue) Color.White else StageTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Visual Active Cue Indicator Tag
                if (isActiveCue) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = activeCueLabel ?: "CUE ACTIVE",
                        color = StageGreen.copy(alpha = pulseAlpha),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // --- Bottom: Hardware State LED & Direct Go/Off Playback Toggle ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Active Cue LED status dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("cue_status_led_${page}_$execNumber")
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActiveCue) {
                                    StageGreen.copy(alpha = pulseAlpha)
                                } else {
                                    Color(0xFF475569)
                                }
                            )
                            .border(
                                1.dp,
                                if (isActiveCue) StageGreen else Color.Transparent,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isActiveCue) "RUNNING" else "OFF",
                        color = if (isActiveCue) StageGreen else StageTextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Dedicated Quick Go/Off Toggle Button
                if (onTogglePlayback != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isActiveCue) StageGreen.copy(alpha = 0.22f) else StageSurfaceVariant
                            )
                            .border(
                                1.dp,
                                if (isActiveCue) StageGreen.copy(alpha = 0.6f) else StageBorder,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                if (hapticFeedbackEnabled) {
                                    HapticUtil.performHaptic(context, isStrong = true)
                                }
                                onTogglePlayback()
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("executor_toggle_${page}_$execNumber"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isActiveCue) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isActiveCue) "Stop Playback" else "Start Playback",
                                tint = if (isActiveCue) StageGreen else StageTextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isActiveCue) "OFF" else "GO",
                                color = if (isActiveCue) StageGreen else StageTextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Convenience overload for [GrandMA2ExecutorButton] accepting a [ExecutorButton] model.
 */
@Composable
fun GrandMA2ExecutorButton(
    button: ExecutorButton,
    page: Int,
    isActiveCue: Boolean,
    onPressChange: (isDown: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    activeCueLabel: String? = null,
    onTogglePlayback: (() -> Unit)? = null,
    onConfigureClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    hapticFeedbackEnabled: Boolean = true
) {
    GrandMA2ExecutorButton(
        execNumber = button.execNumber,
        page = if (button.page > 0) button.page else page,
        label = button.label,
        action = button.action,
        colorHex = button.colorHex,
        isActiveCue = isActiveCue,
        activeCueLabel = activeCueLabel,
        onPressChange = onPressChange,
        onTogglePlayback = onTogglePlayback,
        onConfigureClick = onConfigureClick,
        enabled = enabled,
        hapticFeedbackEnabled = hapticFeedbackEnabled,
        modifier = modifier
    )
}
