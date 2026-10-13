package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExecutorAction
import com.example.model.ExecutorButton
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

@Composable
fun GridScreen(
    viewModel: OscViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val layout = viewModel.layout.value
    val currentPage = viewModel.currentPage.value
    val activeMap = viewModel.activeExecutorsMap.value
    val hapticEnabled = viewModel.config.value.hapticEnabled

    var editingButton by remember { mutableStateOf<ExecutorButton?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(StageBlack)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header bar for grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StageSurface)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = layout.name.uppercase(),
                        color = StageCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${layout.executorButtons.size} Buttons • Tap to trigger, hold to Flash, long press to edit",
                        color = StageTextMuted,
                        fontSize = 11.sp
                    )
                }

                Row {
                    OutlinedButton(
                        onClick = { showAddDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("add_grid_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Button", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontSize = 12.sp)
                    }
                }
            }

            // Grid of executor buttons
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(layout.executorButtons, key = { it.id }) { btn ->
                    val page = if (btn.page > 0) btn.page else currentPage
                    val execKey = "$page.${btn.execNumber}"
                    val isActive = activeMap[execKey] ?: false

                    ExecutorGridItem(
                        button = btn,
                        page = page,
                        isActive = isActive,
                        onPress = { isDown ->
                            if (hapticEnabled && isDown) {
                                HapticUtil.performHaptic(context, isStrong = btn.action == ExecutorAction.FLASH)
                            }
                            viewModel.triggerExecutorButton(btn, isDown)
                        },
                        onLongClick = {
                            if (hapticEnabled) HapticUtil.performHaptic(context, isStrong = true)
                            editingButton = btn
                        }
                    )
                }
            }
        }
    }

    // Edit Button Dialog
    editingButton?.let { btn ->
        ButtonConfigDialog(
            button = btn,
            onDismiss = { editingButton = null },
            onSave = { updated ->
                viewModel.updateExecutorButton(updated)
                editingButton = null
            },
            onDelete = {
                viewModel.removeExecutorButton(btn.id)
                editingButton = null
            }
        )
    }

    // Add Button Dialog
    if (showAddDialog) {
        val nextNum = (layout.executorButtons.maxOfOrNull { it.execNumber } ?: 200) + 1
        ButtonConfigDialog(
            button = ExecutorButton(
                id = "btn_${System.currentTimeMillis()}",
                page = currentPage,
                execNumber = nextNum,
                label = "New Exec $nextNum",
                action = ExecutorAction.GO,
                colorHex = "#00E5FF"
            ),
            isNew = true,
            onDismiss = { showAddDialog = false },
            onSave = { newBtn ->
                viewModel.addExecutorButton(newBtn)
                showAddDialog = false
            },
            onDelete = {}
        )
    }
}

@Composable
fun ExecutorGridItem(
    button: ExecutorButton,
    page: Int,
    isActive: Boolean,
    onPress: (isDown: Boolean) -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressedState by remember { mutableStateOf(false) }

    val baseColor = try {
        Color(android.graphics.Color.parseColor(button.colorHex))
    } catch (e: Exception) {
        StageAmber
    }

    val animatedBg by animateColorAsState(
        targetValue = when {
            isPressedState -> baseColor.copy(alpha = 0.5f)
            isActive -> baseColor.copy(alpha = 0.25f)
            else -> StageSurfaceElevated
        },
        label = "grid_btn_bg"
    )

    val animatedBorder by animateColorAsState(
        targetValue = when {
            isPressedState || isActive -> baseColor
            else -> StageBorder
        },
        label = "grid_btn_border"
    )

    Box(
        modifier = modifier
            .height(105.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(animatedBg)
            .border(
                width = if (isActive || isPressedState) 2.dp else 1.dp,
                color = animatedBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .pointerInput(button.id) {
                detectTapGestures(
                    onPress = {
                        isPressedState = true
                        onPress(true)
                        val released = tryAwaitRelease()
                        isPressedState = false
                        onPress(false)
                    },
                    onLongPress = {
                        onLongClick()
                    }
                )
            }
            .padding(10.dp)
            .testTag("exec_button_${button.execNumber}")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Exec ID and Action Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EXEC $page.${button.execNumber}",
                    color = if (isActive) baseColor else StageTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                // Action Pill
                Box(
                    modifier = Modifier
                        .background(baseColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = button.action.name,
                        color = baseColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Middle: Custom User Label
            Text(
                text = button.label,
                color = if (isActive) Color.White else StageTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Bottom: Active Status Indicator & Color bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isActive) StageGreen else Color(0xFF475569))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isActive) "ACTIVE" else "READY",
                        color = if (isActive) StageGreen else StageTextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .size(width = 24.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(baseColor)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonConfigDialog(
    button: ExecutorButton,
    isNew: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (ExecutorButton) -> Unit,
    onDelete: () -> Unit
) {
    var label by remember { mutableStateOf(button.label) }
    var pageText by remember { mutableStateOf(button.page.toString()) }
    var execText by remember { mutableStateOf(button.execNumber.toString()) }
    var selectedAction by remember { mutableStateOf(button.action) }
    var selectedColor by remember { mutableStateOf(button.colorHex) }

    var actionExpanded by remember { mutableStateOf(false) }

    val presetColors = listOf(
        "#00E5FF", "#FFAB00", "#00E676", "#FF1744",
        "#D500F9", "#FF9100", "#2979FF", "#1DE9B6"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StageSurfaceElevated,
        title = {
            Text(if (isNew) "Add Executor Button" else "Edit Executor Button", color = StageTextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pageText,
                        onValueChange = { pageText = it },
                        label = { Text("Page") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = execText,
                        onValueChange = { execText = it },
                        label = { Text("Exec #") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Action Selector
                ExposedDropdownMenuBox(
                    expanded = actionExpanded,
                    onExpandedChange = { actionExpanded = !actionExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedAction.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Trigger Action") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = actionExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = actionExpanded,
                        onDismissRequest = { actionExpanded = false }
                    ) {
                        ExecutorAction.values().forEach { act ->
                            DropdownMenuItem(
                                text = { Text(act.name) },
                                onClick = {
                                    selectedAction = act
                                    actionExpanded = false
                                }
                            )
                        }
                    }
                }

                // Color picker palette
                Text("Color Tag:", color = StageTextSecondary, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    presetColors.forEach { hex ->
                        val c = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (selectedColor == hex) 3.dp else 1.dp,
                                    color = if (selectedColor == hex) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = pageText.toIntOrNull() ?: button.page
                    val e = execText.toIntOrNull() ?: button.execNumber
                    onSave(
                        button.copy(
                            label = label.ifBlank { "Exec $e" },
                            page = p,
                            execNumber = e,
                            action = selectedAction,
                            colorHex = selectedColor
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = StageCyan, contentColor = StageBlack)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (!isNew) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = StageRed)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = StageTextSecondary)
                }
            }
        }
    )
}
