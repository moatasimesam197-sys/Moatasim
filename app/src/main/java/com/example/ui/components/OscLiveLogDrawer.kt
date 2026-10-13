package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LogDirection
import com.example.model.OscLogItem
import com.example.ui.theme.StageAmber
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageCyan
import com.example.ui.theme.StageGreen
import com.example.ui.theme.StageRed
import com.example.ui.theme.StageSurface
import com.example.ui.theme.StageSurfaceElevated
import com.example.ui.theme.StageTextMuted
import com.example.ui.theme.StageTextPrimary
import com.example.ui.theme.StageTextSecondary

@Composable
fun OscLiveLogDrawer(
    logs: List<OscLogItem>,
    isOpen: Boolean,
    onClose: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return
    val context = LocalContext.current

    var isPaused by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf<LogDirection?>(null) }

    val filteredLogs = remember(logs, selectedFilter, isPaused) {
        if (selectedFilter == null) logs else logs.filter { it.direction == selectedFilter }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(StageSurface)
            .border(1.dp, StageCyan.copy(alpha = 0.5f))
    ) {
        // Log Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StageSurfaceElevated)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "OSC LIVE LOG",
                    color = StageCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${filteredLogs.size} events)",
                    color = StageTextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pause / Resume Auto-scroll
                IconButton(
                    onClick = { isPaused = !isPaused },
                    modifier = Modifier.size(32.dp).testTag("log_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        tint = if (isPaused) StageAmber else StageCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Copy all logs
                IconButton(
                    onClick = {
                        val text = filteredLogs.joinToString("\n") {
                            "[${it.timestamp}] [${it.direction}] ${it.address} ${it.payload}"
                        }
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("OSC Log", text))
                        Toast.makeText(context, "Copied logs to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp).testTag("log_copy_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Logs",
                        tint = StageTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Clear logs
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(32.dp).testTag("log_clear_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Logs",
                        tint = StageRed,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Close drawer
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp).testTag("log_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Log Panel",
                        tint = StageTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Filter chips bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { selectedFilter = null },
                label = { Text("ALL", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StageCyan.copy(alpha = 0.2f),
                    selectedLabelColor = StageCyan
                )
            )
            FilterChip(
                selected = selectedFilter == LogDirection.TX,
                onClick = { selectedFilter = LogDirection.TX },
                label = { Text("TX (SENT)", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StageGreen.copy(alpha = 0.2f),
                    selectedLabelColor = StageGreen
                )
            )
            FilterChip(
                selected = selectedFilter == LogDirection.RX,
                onClick = { selectedFilter = LogDirection.RX },
                label = { Text("RX (RECV)", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StageAmber.copy(alpha = 0.2f),
                    selectedLabelColor = StageAmber
                )
            )
            FilterChip(
                selected = selectedFilter == LogDirection.SYS,
                onClick = { selectedFilter = LogDirection.SYS },
                label = { Text("SYS", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF64748B).copy(alpha = 0.2f),
                    selectedLabelColor = StageTextSecondary
                )
            )
        }

        // Log Entries List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF07080B))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            items(filteredLogs, key = { it.id }) { item ->
                val dirColor = when (item.direction) {
                    LogDirection.TX -> StageGreen
                    LogDirection.RX -> StageAmber
                    LogDirection.SYS -> if (item.isError) StageRed else Color(0xFF64748B)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = item.timestamp,
                        color = StageTextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(76.dp)
                    )

                    Box(
                        modifier = Modifier
                            .background(dirColor.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
                            .border(0.5.dp, dirColor, RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = item.direction.name,
                            color = dirColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = item.address,
                        color = if (item.isError) StageRed else StageCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(130.dp)
                    )

                    Text(
                        text = item.payload,
                        color = if (item.isError) StageRed else StageTextPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
