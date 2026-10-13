package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.ui.components.GrandMA2CommandLineInput
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.OscViewModel
import com.example.ui.components.HapticUtil
import com.example.ui.theme.StageAmber
import com.example.ui.theme.StageBlack
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageCyan
import com.example.ui.theme.StageGreen
import com.example.ui.theme.StageOrange
import com.example.ui.theme.StageRed
import com.example.ui.theme.StageSurface
import com.example.ui.theme.StageSurfaceElevated
import com.example.ui.theme.StageTextMuted
import com.example.ui.theme.StageTextPrimary
import com.example.ui.theme.StageTextSecondary

@Composable
fun PlaybackScreen(
    viewModel: OscViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedExec = viewModel.selectedExec.value
    val hapticEnabled = viewModel.config.value.hapticEnabled

    var execInputText by remember(selectedExec) { mutableStateOf(selectedExec) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StageBlack)
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Selected Executor Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StageSurface, RoundedCornerShape(12.dp))
                .border(1.dp, StageBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "TARGET EXECUTOR",
                    color = StageTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "EXEC $selectedExec",
                    color = StageCyan,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = execInputText,
                    onValueChange = { execInputText = it },
                    singleLine = true,
                    placeholder = { Text("1.1", fontSize = 12.sp) },
                    modifier = Modifier
                        .width(100.dp)
                        .height(52.dp)
                        .testTag("selected_exec_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (execInputText.isNotBlank()) {
                            viewModel.setSelectedExec(execInputText.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StageCyan, contentColor = StageBlack),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(44.dp).testTag("set_selected_exec_btn")
                ) {
                    Text("SET", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = { viewModel.requestDeleteSelected() },
                    modifier = Modifier.testTag("delete_selected_exec_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Executor",
                        tint = StageRed
                    )
                }
            }
        }

        // Primary Playback Buttons (GO+, BACK, PAUSE, FLASH, OFF)
        // 1. Huge GO+ Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(StageGreen.copy(alpha = 0.25f))
                .border(2.dp, StageGreen, RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectTapGestures {
                        if (hapticEnabled) HapticUtil.performHaptic(context, isStrong = true)
                        viewModel.playbackGo()
                    }
                }
                .testTag("playback_go_button"),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "GO",
                    tint = StageGreen,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "GO +",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Trigger next cue in sequence",
                        color = StageGreen,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 2. Middle Row: BACK / GO- and PAUSE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // GO- / BACK
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(StageAmber.copy(alpha = 0.2f))
                    .border(2.dp, StageAmber, RoundedCornerShape(14.dp))
                    .pointerInput(Unit) {
                        detectTapGestures {
                            if (hapticEnabled) HapticUtil.performHaptic(context)
                            viewModel.playbackBack()
                        }
                    }
                    .testTag("playback_back_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "BACK",
                        tint = StageAmber,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GO -",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // PAUSE
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(StageOrange.copy(alpha = 0.2f))
                    .border(2.dp, StageOrange, RoundedCornerShape(14.dp))
                    .pointerInput(Unit) {
                        detectTapGestures {
                            if (hapticEnabled) HapticUtil.performHaptic(context)
                            viewModel.playbackPause()
                        }
                    }
                    .testTag("playback_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "PAUSE",
                        tint = StageOrange,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PAUSE",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // 3. Lower Row: Momentary FLASH and OFF
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // FLASH (Momentary on press down / release up)
            var isFlashing by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(84.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isFlashing) Color.White.copy(alpha = 0.4f) else StageCyan.copy(alpha = 0.2f))
                    .border(2.dp, if (isFlashing) Color.White else StageCyan, RoundedCornerShape(14.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isFlashing = true
                                if (hapticEnabled) HapticUtil.performHaptic(context, isStrong = true)
                                viewModel.playbackFlash(true)
                                tryAwaitRelease()
                                isFlashing = false
                                viewModel.playbackFlash(false)
                            }
                        )
                    }
                    .testTag("playback_flash_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "FLASH",
                        tint = if (isFlashing) Color.White else StageCyan,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FLASH",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // OFF
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(84.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(StageRed.copy(alpha = 0.2f))
                    .border(2.dp, StageRed, RoundedCornerShape(14.dp))
                    .pointerInput(Unit) {
                        detectTapGestures {
                            if (hapticEnabled) HapticUtil.performHaptic(context)
                            viewModel.playbackOff()
                        }
                    }
                    .testTag("playback_off_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "OFF",
                        tint = StageRed,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "OFF",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Secondary Utilities: Top Rewind and Rate Multipliers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StageSurfaceElevated, RoundedCornerShape(12.dp))
                .border(1.dp, StageBorder, RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = { viewModel.playbackTop() },
                modifier = Modifier.testTag("playback_top_btn")
            ) {
                Icon(Icons.Default.FastRewind, contentDescription = "Top", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("CUE TOP", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("0.5", "1.0", "2.0").forEach { rate ->
                    OutlinedButton(
                        onClick = { viewModel.playbackRate(rate) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("${rate}x", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = StageCyan)
                    }
                }
            }
        }

        // Live Quick grandMA2 Command Line Input
        GrandMA2CommandLineInput(
            viewModel = viewModel,
            placeholder = "Quick Command (e.g. Goto Cue 1, Off Exec 1.1)...",
            showQuickSyntaxChips = false,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
