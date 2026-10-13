package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FaderItem
import com.example.ui.OscViewModel
import com.example.ui.components.GrandMA2VerticalSlider
import com.example.ui.components.HapticUtil
import com.example.ui.theme.StageAmber
import com.example.ui.theme.StageBlack
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageCyan
import com.example.ui.theme.StageRed
import com.example.ui.theme.StageSurface
import com.example.ui.theme.StageSurfaceElevated
import com.example.ui.theme.StageTextMuted
import com.example.ui.theme.StageTextPrimary
import com.example.ui.theme.StageTextSecondary

@Composable
fun FaderScreen(
    viewModel: OscViewModel,
    modifier: Modifier = Modifier
) {
    val layout = viewModel.layout.value
    val currentPage = viewModel.currentPage.value
    val hapticEnabled = viewModel.config.value.hapticEnabled
    val context = LocalContext.current

    val masterFader = layout.faders.firstOrNull { it.isMaster }
    val regularFaders = layout.faders.filterNot { it.isMaster }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StageBlack)
            .testTag("faders_screen_container")
    ) {
        // Sub-header with Console Telemetry & Active Page
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StageSurface)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = StageCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "GRANDMA2 CONSOLE FADERS",
                        color = StageCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "100mm Motorized Faders • High-Precision OSC Intensity",
                        color = StageTextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(StageSurfaceElevated, RoundedCornerShape(4.dp))
                        .border(1.dp, StageBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "PAGE $currentPage",
                        color = StageAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Faders Area: Pinned Grand Master on left + horizontally scrollable executor faders
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Pinned Grand Master Fader on the left
            masterFader?.let { master ->
                StageVerticalFader(
                    fader = master,
                    currentPage = currentPage,
                    onLevelChange = { newLevel ->
                        if (hapticEnabled) HapticUtil.performHaptic(context)
                        viewModel.setFaderLevel(master, newLevel)
                    },
                    modifier = Modifier.width(108.dp),
                    hapticFeedbackEnabled = hapticEnabled
                )

                // High-contrast vertical console separator line
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(StageBorder)
                )
            }

            // Horizontally scrollable list of regular executor faders
            if (regularFaders.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    regularFaders.forEach { fader ->
                        StageVerticalFader(
                            fader = fader,
                            currentPage = currentPage,
                            onLevelChange = { newLevel ->
                                if (hapticEnabled) HapticUtil.performHaptic(context)
                                viewModel.setFaderLevel(fader, newLevel)
                            },
                            modifier = Modifier.width(104.dp),
                            hapticFeedbackEnabled = hapticEnabled
                        )
                    }
                }
            } else {
                // Empty state if no executor faders yet configured
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(StageSurfaceElevated, RoundedCornerShape(12.dp))
                        .border(1.dp, StageBorder, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LinearScale,
                            contentDescription = null,
                            tint = StageTextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Regular Faders on Page $currentPage",
                            color = StageTextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add faders in the Layout Editor tab to control playback executors.",
                            color = StageTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Adapter composable mapping a [FaderItem] to [GrandMA2VerticalSlider]
 */
@Composable
fun StageVerticalFader(
    fader: FaderItem,
    currentPage: Int,
    onLevelChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    hapticFeedbackEnabled: Boolean = true
) {
    val faderColor = if (fader.isMaster) StageRed else {
        try {
            Color(android.graphics.Color.parseColor(fader.colorHex))
        } catch (e: Exception) {
            StageCyan
        }
    }
    val pageNum = if (fader.page > 0) fader.page else currentPage
    val execId = if (fader.isMaster) "MASTER" else "$pageNum.${fader.execNumber}"

    GrandMA2VerticalSlider(
        level = fader.level,
        onLevelChange = onLevelChange,
        modifier = modifier,
        label = fader.label,
        execIdentifier = execId,
        isMaster = fader.isMaster,
        color = faderColor,
        showScaleTicks = true,
        showDmxValue = true,
        showFlashButton = true,
        showFineTuning = true,
        showQuickPresets = true,
        hapticFeedbackEnabled = hapticFeedbackEnabled
    )
}
