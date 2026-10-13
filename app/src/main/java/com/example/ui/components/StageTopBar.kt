package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionState
import com.example.model.OscConfig
import com.example.ui.theme.StageAmber
import com.example.ui.theme.StageBlack
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageCyan
import com.example.ui.theme.StageGreen
import com.example.ui.theme.StageRed
import com.example.ui.theme.StageSurface
import com.example.ui.theme.StageTextMuted
import com.example.ui.theme.StageTextPrimary

@Composable
fun StageTopBar(
    connectionState: ConnectionState,
    config: OscConfig,
    blackoutActive: Boolean,
    currentPage: Int,
    onBlackoutToggle: () -> Unit,
    onStopAllClick: () -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onToggleLogs: () -> Unit,
    onStatusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blackoutBg by animateColorAsState(
        targetValue = if (blackoutActive) StageRed else Color(0xFF2A0A10),
        label = "blackout_bg"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StageSurface)
            .border(1.dp, StageBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Blackout Button (Prominent, visible on all screens)
            Button(
                onClick = onBlackoutToggle,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = blackoutBg,
                    contentColor = if (blackoutActive) Color.White else StageRed
                ),
                modifier = Modifier
                    .height(44.dp)
                    .border(
                        width = if (blackoutActive) 2.dp else 1.dp,
                        color = StageRed,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .testTag("blackout_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (blackoutActive) Color.White else StageRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (blackoutActive) "BLACKOUT ACTIVE" else "BLACKOUT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Center: Page Switcher
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xFF161A24), RoundedCornerShape(8.dp))
                    .border(1.dp, StageBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                IconButton(
                    onClick = onPrevPage,
                    modifier = Modifier.size(36.dp).testTag("prev_page_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous Page",
                        tint = StageCyan
                    )
                }

                Text(
                    text = "PAGE $currentPage",
                    color = StageCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = onNextPage,
                    modifier = Modifier.size(36.dp).testTag("next_page_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Page",
                        tint = StageCyan
                    )
                }
            }

            // Right Actions: Stop All & Log Diagnostics Toggle & Connection Status
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Stop All button (Safety prompt)
                OutlinedButton(
                    onClick = onStopAllClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StageRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StageRed.copy(alpha = 0.8f)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .padding(end = 6.dp)
                        .testTag("stop_all_button")
                ) {
                    Text(
                        text = "STOP ALL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Diagnostics Log Toggle
                IconButton(
                    onClick = onToggleLogs,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF161A24), RoundedCornerShape(8.dp))
                        .border(1.dp, StageBorder, RoundedCornerShape(8.dp))
                        .testTag("toggle_logs_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Toggle OSC Log",
                        tint = StageAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Sub-bar: Network IP & Port indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0C0E14))
                .padding(horizontal = 12.dp, vertical = 3.dp)
                .clickable { onStatusClick() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val (statusText, statusColor) = when (connectionState) {
                is ConnectionState.Connected -> "ONPC CONNECTED: ${config.targetIp}:${config.oscReceivePort}" to StageGreen
                is ConnectionState.Connecting -> "CONNECTING: ${config.targetIp}:${config.oscReceivePort}" to StageAmber
                is ConnectionState.Error -> "DISCONNECTED (${connectionState.message})" to StageRed
                is ConnectionState.Disconnected -> "DISCONNECTED (Target: ${config.targetIp}:${config.oscReceivePort})" to StageTextMuted
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = "In: ${config.oscReceivePort} | Out: ${config.oscSendPort} | Prefix: ${config.prefix}",
                color = StageTextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
