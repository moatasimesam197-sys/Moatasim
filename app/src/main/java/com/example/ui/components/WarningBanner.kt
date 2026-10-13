package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionState
import com.example.ui.theme.StageAmber
import com.example.ui.theme.StageRed
import com.example.ui.theme.StageTextPrimary

@Composable
fun WarningBanner(
    connectionState: ConnectionState,
    onReconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDisconnected = connectionState is ConnectionState.Disconnected ||
            connectionState is ConnectionState.Error ||
            connectionState is ConnectionState.Connecting

    AnimatedVisibility(
        visible = isDisconnected,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        val (bgColor, borderColor, text, icon) = when (connectionState) {
            is ConnectionState.Connecting -> {
                Quadruple(
                    Color(0x33FFAB00),
                    StageAmber,
                    "Connecting to grandMA2 onPC (Attempt #${connectionState.attempt})...",
                    Icons.Default.Refresh
                )
            }
            is ConnectionState.Error -> {
                Quadruple(
                    Color(0x33FF1744),
                    StageRed,
                    "Connection Error: ${connectionState.message}",
                    Icons.Default.WarningAmber
                )
            }
            else -> {
                Quadruple(
                    Color(0x3394A3B8),
                    Color(0xFF64748B),
                    "Disconnected from console. Check PC IP & Wi-Fi.",
                    Icons.Default.WifiOff
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .background(bgColor, RoundedCornerShape(8.dp))
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                if (connectionState is ConnectionState.Connecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = StageAmber
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Connection Warning",
                        tint = borderColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = text,
                    color = StageTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 8.dp),
                    maxLines = 2
                )
            }

            OutlinedButton(
                onClick = onReconnectClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = StageTextPrimary
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("reconnect_button")
            ) {
                Text("Connect", fontSize = 12.sp)
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
