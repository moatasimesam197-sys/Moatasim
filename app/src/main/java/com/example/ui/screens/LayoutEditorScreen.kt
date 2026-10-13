package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import com.example.model.MacroButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.zIndex
import com.example.model.ExecutorAction
import com.example.model.ExecutorButton
import com.example.model.FaderItem
import com.example.ui.OscViewModel
import com.example.ui.components.HapticUtil
import com.example.ui.theme.StageAmber
import com.example.ui.theme.StageBlack
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageCyan
import com.example.ui.theme.StageGreen
import com.example.ui.theme.StagePurple
import com.example.ui.theme.StageRed
import com.example.ui.theme.StageSurface
import com.example.ui.theme.StageSurfaceElevated
import com.example.ui.theme.StageSurfaceVariant
import com.example.ui.theme.StageTextMuted
import com.example.ui.theme.StageTextPrimary
import com.example.ui.theme.StageTextSecondary
import kotlin.math.roundToInt

private const val GRID_COLUMNS = 4
private const val CELL_BASE_HEIGHT_DP = 95
private const val GRID_SPACING_DP = 8

@Composable
fun LayoutEditorScreen(
    viewModel: OscViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val layout = viewModel.layout.value
    val hapticEnabled = viewModel.config.value.hapticEnabled
    val activeMap = viewModel.activeExecutorsMap.value
    val currentPage = viewModel.currentPage.value

    var isEditMode by remember { mutableStateOf(true) }
    var selectedItemId by remember { mutableStateOf<String?>(null) } // id of selected item

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var editingButtonConfig by remember { mutableStateOf<ExecutorButton?>(null) }
    var editingFaderConfig by remember { mutableStateOf<FaderItem?>(null) }
    var showAddDrawer by remember { mutableStateOf(false) }

    var activeLayoutTab by remember { mutableIntStateOf(0) } // 0: Grid Canvas, 1: MA2 Macros
    var editingMacroConfig by remember { mutableStateOf<MacroButton?>(null) }
    var showAddMacroDialog by remember { mutableStateOf(false) }

    val selectedButton = layout.executorButtons.firstOrNull { it.id == selectedItemId }
    val selectedFader = layout.faders.firstOrNull { it.id == selectedItemId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StageBlack)
    ) {
        // --- Tab Row: GRID CANVAS vs MA2 MACROS ---
        TabRow(
            selectedTabIndex = activeLayoutTab,
            containerColor = StageSurfaceElevated,
            contentColor = StageCyan,
            modifier = Modifier.fillMaxWidth().testTag("layout_editor_tab_row")
        ) {
            Tab(
                selected = activeLayoutTab == 0,
                onClick = { activeLayoutTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GridView, contentDescription = "Grid Canvas", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "GRID CANVAS (${layout.executorButtons.size + layout.faders.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                modifier = Modifier.testTag("tab_grid_canvas")
            )
            Tab(
                selected = activeLayoutTab == 1,
                onClick = { activeLayoutTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = "MA2 Macros",
                            tint = if (activeLayoutTab == 1) StagePurple else StageTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "MA2 MACROS (${layout.macros.size})",
                            color = if (activeLayoutTab == 1) StagePurple else StageTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                modifier = Modifier.testTag("tab_ma2_macros")
            )
        }

        if (activeLayoutTab == 0) {
        // --- 1. Mode Bar: Edit/Design vs Live Show Mode ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StageSurface)
                .border(1.dp, StageBorder)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Mode Toggle (Design Mode vs Live Show Mode)
            Row(
                modifier = Modifier
                    .background(Color(0xFF0F1219), RoundedCornerShape(8.dp))
                    .border(1.dp, StageBorder, RoundedCornerShape(8.dp))
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit Mode Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isEditMode) StageCyan.copy(alpha = 0.25f) else Color.Transparent)
                        .border(1.dp, if (isEditMode) StageCyan else Color.Transparent, RoundedCornerShape(6.dp))
                        .clickable {
                            isEditMode = true
                            if (hapticEnabled) HapticUtil.performHaptic(context)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Mode", tint = if (isEditMode) StageCyan else StageTextMuted, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("DESIGN & RESIZE", color = if (isEditMode) StageCyan else StageTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                // Live Show Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (!isEditMode) StageGreen.copy(alpha = 0.25f) else Color.Transparent)
                        .border(1.dp, if (!isEditMode) StageGreen else Color.Transparent, RoundedCornerShape(6.dp))
                        .clickable {
                            isEditMode = false
                            selectedItemId = null
                            if (hapticEnabled) HapticUtil.performHaptic(context)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Live Show", tint = if (!isEditMode) StageGreen else StageTextMuted, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("LIVE SHOW", color = if (!isEditMode) StageGreen else StageTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // Right Actions: Presets, Import, Export, Add
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isEditMode) {
                    Button(
                        onClick = { showAddDrawer = true },
                        colors = ButtonDefaults.buttonColors(containerColor = StageCyan, contentColor = StageBlack),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp).testTag("grid_add_component_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("ADD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(onClick = { showExportDialog = true }, modifier = Modifier.size(34.dp).testTag("export_grid_json_btn")) {
                    Icon(Icons.Default.FileUpload, contentDescription = "Export", tint = StageCyan, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { showImportDialog = true }, modifier = Modifier.size(34.dp).testTag("import_grid_json_btn")) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Import", tint = StageAmber, modifier = Modifier.size(18.dp))
                }
            }
        }

        // --- 2. Interactive Selection & Resizing Control Toolbar (in Edit Mode) ---
        AnimatedVisibility(visible = isEditMode && (selectedButton != null || selectedFader != null)) {
            val label = selectedButton?.label ?: selectedFader?.label ?: "Component"
            val colSpan = selectedButton?.colSpan ?: selectedFader?.colSpan ?: 1
            val rowSpan = selectedButton?.rowSpan ?: selectedFader?.rowSpan ?: 1

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141A26)),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StageCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "SELECTED: $label",
                            color = StageCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                        Text(
                            text = "Size: ${colSpan}x${rowSpan} • Drag to reposition",
                            color = StageTextMuted,
                            fontSize = 10.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Width Span Controls (- / +)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0xFF0A0D14), RoundedCornerShape(6.dp))
                                .border(1.dp, StageBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("W:", color = StageTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(3.dp))
                            TextButton(
                                onClick = {
                                    if (selectedButton != null && selectedButton.colSpan > 1) {
                                        viewModel.updateExecutorButton(selectedButton.copy(colSpan = selectedButton.colSpan - 1))
                                    } else if (selectedFader != null && selectedFader.colSpan > 1) {
                                        viewModel.updateFader(selectedFader.copy(colSpan = selectedFader.colSpan - 1))
                                    }
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("-", color = StageCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("$colSpan", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            TextButton(
                                onClick = {
                                    val maxAllowed = (GRID_COLUMNS - (selectedButton?.gridCol ?: selectedFader?.gridCol ?: 0)).coerceAtLeast(1)
                                    if (selectedButton != null && selectedButton.colSpan < maxAllowed) {
                                        viewModel.updateExecutorButton(selectedButton.copy(colSpan = selectedButton.colSpan + 1))
                                    } else if (selectedFader != null && selectedFader.colSpan < maxAllowed) {
                                        viewModel.updateFader(selectedFader.copy(colSpan = selectedFader.colSpan + 1))
                                    }
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("+", color = StageCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Height Span Controls (- / +)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0xFF0A0D14), RoundedCornerShape(6.dp))
                                .border(1.dp, StageBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("H:", color = StageTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(3.dp))
                            TextButton(
                                onClick = {
                                    if (selectedButton != null && selectedButton.rowSpan > 1) {
                                        viewModel.updateExecutorButton(selectedButton.copy(rowSpan = selectedButton.rowSpan - 1))
                                    } else if (selectedFader != null && selectedFader.rowSpan > 1) {
                                        viewModel.updateFader(selectedFader.copy(rowSpan = selectedFader.rowSpan - 1))
                                    }
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("-", color = StageAmber, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("$rowSpan", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            TextButton(
                                onClick = {
                                    if (selectedButton != null && selectedButton.rowSpan < 3) {
                                        viewModel.updateExecutorButton(selectedButton.copy(rowSpan = selectedButton.rowSpan + 1))
                                    } else if (selectedFader != null && selectedFader.rowSpan < 4) {
                                        viewModel.updateFader(selectedFader.copy(rowSpan = selectedFader.rowSpan + 1))
                                    }
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("+", color = StageAmber, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Edit Details
                        IconButton(
                            onClick = {
                                if (selectedButton != null) editingButtonConfig = selectedButton
                                if (selectedFader != null) editingFaderConfig = selectedFader
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Properties", tint = StageCyan, modifier = Modifier.size(16.dp))
                        }

                        // Delete Component
                        IconButton(
                            onClick = {
                                selectedButton?.let { viewModel.removeExecutorButton(it.id) }
                                selectedFader?.let { viewModel.removeFader(it.id) }
                                selectedItemId = null
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Component", tint = StageRed, modifier = Modifier.size(16.dp))
                        }

                        // Deselect / Close
                        IconButton(onClick = { selectedItemId = null }, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = StageTextMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // --- 3. The Drag-and-Drop Canvas Workspace ---
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
            val density = LocalDensity.current
            val totalWidthPx = constraints.maxWidth.toFloat()
            val spacingPx = with(density) { GRID_SPACING_DP.dp.toPx() }
            val cellHeightPx = with(density) { CELL_BASE_HEIGHT_DP.dp.toPx() }

            // Width of 1 grid column cell
            val cellWidthPx = (totalWidthPx - (GRID_COLUMNS - 1) * spacingPx) / GRID_COLUMNS

            // Determine maximum rows needed for canvas
            val maxButtonRow = layout.executorButtons.maxOfOrNull { it.gridRow + it.rowSpan } ?: 4
            val maxFaderRow = layout.faders.maxOfOrNull { it.gridRow + it.rowSpan } ?: 4
            val totalRows = (maxOf(maxButtonRow, maxFaderRow) + 2).coerceAtLeast(6)
            val canvasHeightDp = (totalRows * (CELL_BASE_HEIGHT_DP + GRID_SPACING_DP)).dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(canvasHeightDp)
            ) {
                // Background Blueprint Grid Tiles (visible in Edit Mode to guide placement)
                if (isEditMode) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(GRID_SPACING_DP.dp)
                    ) {
                        for (r in 0 until totalRows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(GRID_SPACING_DP.dp)
                            ) {
                                for (c in 0 until GRID_COLUMNS) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(CELL_BASE_HEIGHT_DP.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0C0F16))
                                            .border(0.6.dp, Color(0xFF1B202D), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.TopStart
                                    ) {
                                        Text(
                                            text = "$c,$r",
                                            color = Color(0xFF242C3E),
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Render Executor Buttons
                layout.executorButtons.forEach { btn ->
                    DraggableGridButton(
                        button = btn,
                        isEditMode = isEditMode,
                        isSelected = selectedItemId == btn.id,
                        cellWidthPx = cellWidthPx,
                        cellHeightPx = cellHeightPx,
                        spacingPx = spacingPx,
                        isActive = activeMap["${btn.page}.${btn.execNumber}"] ?: false,
                        onSelect = {
                            selectedItemId = btn.id
                            if (hapticEnabled) HapticUtil.performHaptic(context)
                        },
                        onDrop = { newCol, newRow ->
                            val clampedCol = newCol.coerceIn(0, GRID_COLUMNS - btn.colSpan)
                            val clampedRow = newRow.coerceAtLeast(0)
                            viewModel.updateExecutorButton(btn.copy(gridCol = clampedCol, gridRow = clampedRow))
                            if (hapticEnabled) HapticUtil.performHaptic(context, isStrong = true)
                        },
                        onTrigger = { isDown ->
                            viewModel.triggerExecutorButton(btn, isDown)
                        }
                    )
                }

                // Render Executor & Master Faders
                layout.faders.forEach { fader ->
                    DraggableGridFader(
                        fader = fader,
                        isEditMode = isEditMode,
                        isSelected = selectedItemId == fader.id,
                        cellWidthPx = cellWidthPx,
                        cellHeightPx = cellHeightPx,
                        spacingPx = spacingPx,
                        onSelect = {
                            selectedItemId = fader.id
                            if (hapticEnabled) HapticUtil.performHaptic(context)
                        },
                        onDrop = { newCol, newRow ->
                            val clampedCol = newCol.coerceIn(0, GRID_COLUMNS - fader.colSpan)
                            val clampedRow = newRow.coerceAtLeast(0)
                            viewModel.updateFader(fader.copy(gridCol = clampedCol, gridRow = clampedRow))
                            if (hapticEnabled) HapticUtil.performHaptic(context, isStrong = true)
                        },
                        onLevelChange = { newLevel ->
                            viewModel.setFaderLevel(fader, newLevel)
                        }
                    )
                }
            }
        }
        } else {
            // Dedicated MA2 Macros Workspace Tab
            Ma2MacroTabContent(
                macros = layout.macros,
                onTriggerMacro = { macro ->
                    if (hapticEnabled) HapticUtil.performHaptic(context, isStrong = true)
                    viewModel.triggerMacro(macro)
                },
                onAddMacro = { showAddMacroDialog = true },
                onEditMacro = { macro -> editingMacroConfig = macro },
                onDeleteMacro = { macroId -> viewModel.removeMacro(macroId) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // --- Macro Management Dialogs ---
    editingMacroConfig?.let { macro ->
        MacroConfigDialog(
            macro = macro,
            onDismiss = { editingMacroConfig = null },
            onSave = { updated ->
                viewModel.updateMacro(updated)
                editingMacroConfig = null
            },
            onDelete = {
                viewModel.removeMacro(macro.id)
                editingMacroConfig = null
            }
        )
    }

    if (showAddMacroDialog) {
        val nextNum = (layout.macros.maxOfOrNull { it.macroNumber } ?: 0) + 1
        MacroConfigDialog(
            macro = MacroButton(
                id = "m_${System.currentTimeMillis()}",
                macroNumber = nextNum,
                label = "Macro $nextNum",
                command = "Macro $nextNum",
                colorHex = "#D500F9"
            ),
            isNew = true,
            onDismiss = { showAddMacroDialog = false },
            onSave = { newMacro ->
                viewModel.addMacro(newMacro)
                showAddMacroDialog = false
            },
            onDelete = {}
        )
    }

    // --- 4. Component Creation Drawer / Modal ---
    if (showAddDrawer) {
        AddComponentDialog(
            nextExec = (layout.executorButtons.maxOfOrNull { it.execNumber } ?: 200) + 1,
            currentPage = currentPage,
            onDismiss = { showAddDrawer = false },
            onAddButton = { newBtn ->
                // Find next free cell
                val usedPositions = (layout.executorButtons.map { it.gridCol to it.gridRow } +
                        layout.faders.map { it.gridCol to it.gridRow }).toSet()
                var targetCol = 0
                var targetRow = 2
                while (usedPositions.contains(targetCol to targetRow)) {
                    targetCol++
                    if (targetCol >= GRID_COLUMNS) {
                        targetCol = 0
                        targetRow++
                    }
                }
                viewModel.addExecutorButton(newBtn.copy(gridCol = targetCol, gridRow = targetRow))
                showAddDrawer = false
            },
            onAddFader = { newFader ->
                var targetCol = 0
                var targetRow = 0
                val used = layout.faders.map { it.gridCol to it.gridRow }.toSet()
                while (used.contains(targetCol to targetRow)) {
                    targetCol++
                    if (targetCol >= GRID_COLUMNS) {
                        targetCol = 0
                        targetRow += 2
                    }
                }
                viewModel.addFader(newFader.copy(gridCol = targetCol, gridRow = targetRow))
                showAddDrawer = false
            }
        )
    }

    // Edit Button Details Dialog
    editingButtonConfig?.let { btn ->
        ButtonConfigDialog(
            button = btn,
            onDismiss = { editingButtonConfig = null },
            onSave = { updated ->
                viewModel.updateExecutorButton(updated)
                editingButtonConfig = null
            },
            onDelete = {
                viewModel.removeExecutorButton(btn.id)
                editingButtonConfig = null
                selectedItemId = null
            }
        )
    }

    // Edit Fader Details Dialog
    editingFaderConfig?.let { fader ->
        EditFaderConfigDialog(
            fader = fader,
            onDismiss = { editingFaderConfig = null },
            onSave = { updated ->
                viewModel.updateFader(updated)
                editingFaderConfig = null
            },
            onDelete = {
                viewModel.removeFader(fader.id)
                editingFaderConfig = null
                selectedItemId = null
            }
        )
    }

    // Export Dialog
    if (showExportDialog) {
        val exportedJson = remember { viewModel.exportLayoutJson() }
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            containerColor = StageSurfaceElevated,
            title = { Text("Export Grid Layout JSON", color = StageTextPrimary) },
            text = {
                Column {
                    Text("Copy this personalized grid configuration to clipboard:", color = StageTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportedJson,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Layout JSON", exportedJson))
                        Toast.makeText(context, "Copied JSON to clipboard", Toast.LENGTH_SHORT).show()
                        showExportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StageCyan, contentColor = StageBlack)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("Close", color = StageTextSecondary) }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        var importInput by remember { mutableStateOf("") }
        var importError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = StageSurfaceElevated,
            title = { Text("Import Grid Layout JSON", color = StageTextPrimary) },
            text = {
                Column {
                    Text("Paste custom layout JSON to reconstruct buttons and faders:", color = StageTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importInput,
                        onValueChange = {
                            importInput = it
                            importError = null
                        },
                        placeholder = { Text("Paste JSON here...") },
                        isError = importError != null,
                        supportingText = { importError?.let { Text(it, color = StageRed) } },
                        modifier = Modifier.fillMaxWidth().height(180.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importInput.isBlank()) {
                            importError = "Please enter valid JSON"
                            return@Button
                        }
                        val success = viewModel.importLayoutJson(importInput)
                        if (success) {
                            Toast.makeText(context, "Grid layout loaded successfully!", Toast.LENGTH_SHORT).show()
                            showImportDialog = false
                        } else {
                            importError = "Invalid JSON structure. Please verify keys."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StageGreen, contentColor = StageBlack)
                ) {
                    Text("Import Layout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel", color = StageTextSecondary) }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Draggable Grid Button Component
// ---------------------------------------------------------------------------
@Composable
fun DraggableGridButton(
    button: ExecutorButton,
    isEditMode: Boolean,
    isSelected: Boolean,
    cellWidthPx: Float,
    cellHeightPx: Float,
    spacingPx: Float,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDrop: (newCol: Int, newRow: Int) -> Unit,
    onTrigger: (isDown: Boolean) -> Unit
) {
    val density = LocalDensity.current

    val itemWidthPx = button.colSpan * cellWidthPx + (button.colSpan - 1) * spacingPx
    val itemHeightPx = button.rowSpan * cellHeightPx + (button.rowSpan - 1) * spacingPx

    val originX = button.gridCol * (cellWidthPx + spacingPx)
    val originY = button.gridRow * (cellHeightPx + spacingPx)

    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    val baseColor = try {
        Color(android.graphics.Color.parseColor(button.colorHex))
    } catch (e: Exception) {
        StageAmber
    }

    val finalX = originX + dragOffset.x
    val finalY = originY + dragOffset.y

    // Calculate nearest snap cell while dragging
    val snapCol = ((finalX + (cellWidthPx / 2)) / (cellWidthPx + spacingPx)).toInt().coerceIn(0, GRID_COLUMNS - button.colSpan)
    val snapRow = ((finalY + (cellHeightPx / 2)) / (cellHeightPx + spacingPx)).toInt().coerceAtLeast(0)

    Box(
        modifier = Modifier
            .offset { IntOffset(finalX.roundToInt(), finalY.roundToInt()) }
            .size(
                width = with(density) { itemWidthPx.toDp() },
                height = with(density) { itemHeightPx.toDp() }
            )
            .zIndex(if (isDragging) 10f else if (isSelected) 5f else 1f)
            .shadow(if (isDragging) 16.dp else 0.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    if (isDragging) listOf(baseColor.copy(alpha = 0.5f), Color(0xFF1E2433))
                    else if (isActive) listOf(baseColor.copy(alpha = 0.4f), Color(0xFF161E2E))
                    else listOf(Color(0xFF1A1F2C), Color(0xFF10131B))
                )
            )
            .border(
                width = if (isSelected) 2.5.dp else if (isDragging) 2.dp else 1.dp,
                color = if (isSelected) StageCyan else if (isDragging || isActive) baseColor else StageBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .pointerInput(button.id, isEditMode) {
                if (isEditMode) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                            onSelect()
                        },
                        onDragEnd = {
                            isDragging = false
                            onDrop(snapCol, snapRow)
                            dragOffset = Offset.Zero
                        },
                        onDragCancel = {
                            isDragging = false
                            dragOffset = Offset.Zero
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount
                        }
                    )
                } else {
                    detectTapGestures(
                        onPress = {
                            onTrigger(true)
                            tryAwaitRelease()
                            onTrigger(false)
                        }
                    )
                }
            }
            .pointerInput(button.id, isEditMode) {
                if (isEditMode) {
                    detectTapGestures { onSelect() }
                }
            }
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EXEC ${button.page}.${button.execNumber}",
                    color = if (isActive) baseColor else StageTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                // Action Pill
                Box(
                    modifier = Modifier
                        .background(baseColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = button.action.name,
                        color = baseColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Text(
                text = button.label,
                color = Color.White,
                fontSize = if (button.colSpan > 1) 14.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isActive) StageGreen else Color(0xFF4A5568))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isEditMode) "${button.colSpan}x${button.rowSpan}" else if (isActive) "ON" else "OFF",
                        color = StageTextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .size(width = 18.dp, height = 3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(baseColor)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Draggable Grid Fader Component
// ---------------------------------------------------------------------------
@Composable
fun DraggableGridFader(
    fader: FaderItem,
    isEditMode: Boolean,
    isSelected: Boolean,
    cellWidthPx: Float,
    cellHeightPx: Float,
    spacingPx: Float,
    onSelect: () -> Unit,
    onDrop: (newCol: Int, newRow: Int) -> Unit,
    onLevelChange: (Float) -> Unit
) {
    val density = LocalDensity.current

    val itemWidthPx = fader.colSpan * cellWidthPx + (fader.colSpan - 1) * spacingPx
    val itemHeightPx = fader.rowSpan * cellHeightPx + (fader.rowSpan - 1) * spacingPx

    val originX = fader.gridCol * (cellWidthPx + spacingPx)
    val originY = fader.gridRow * (cellHeightPx + spacingPx)

    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    val faderColor = if (fader.isMaster) StageRed else {
        try { Color(android.graphics.Color.parseColor(fader.colorHex)) } catch (e: Exception) { StageCyan }
    }

    val finalX = originX + dragOffset.x
    val finalY = originY + dragOffset.y

    val snapCol = ((finalX + (cellWidthPx / 2)) / (cellWidthPx + spacingPx)).toInt().coerceIn(0, GRID_COLUMNS - fader.colSpan)
    val snapRow = ((finalY + (cellHeightPx / 2)) / (cellHeightPx + spacingPx)).toInt().coerceAtLeast(0)

    val percent = (fader.level * 100f).roundToInt()

    Box(
        modifier = Modifier
            .offset { IntOffset(finalX.roundToInt(), finalY.roundToInt()) }
            .size(
                width = with(density) { itemWidthPx.toDp() },
                height = with(density) { itemHeightPx.toDp() }
            )
            .zIndex(if (isDragging) 10f else if (isSelected) 5f else 1f)
            .shadow(if (isDragging) 16.dp else 0.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    if (isDragging) listOf(faderColor.copy(alpha = 0.4f), Color(0xFF1E2433))
                    else listOf(Color(0xFF181C26), Color(0xFF10131B))
                )
            )
            .border(
                width = if (isSelected) 2.5.dp else if (isDragging) 2.dp else 1.dp,
                color = if (isSelected) StageCyan else if (isDragging) faderColor else if (fader.isMaster) StageRed.copy(alpha = 0.7f) else StageBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .pointerInput(fader.id, isEditMode) {
                if (isEditMode) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                            onSelect()
                        },
                        onDragEnd = {
                            isDragging = false
                            onDrop(snapCol, snapRow)
                            dragOffset = Offset.Zero
                        },
                        onDragCancel = {
                            isDragging = false
                            dragOffset = Offset.Zero
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount
                        }
                    )
                }
            }
            .pointerInput(fader.id, isEditMode) {
                if (isEditMode) {
                    detectTapGestures { onSelect() }
                }
            }
            .padding(6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (fader.isMaster) "MASTER" else "EXEC ${fader.page}.${fader.execNumber}",
                    color = faderColor,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = fader.label,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "$percent%",
                    color = faderColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Fader Track
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val trackHeightPx = constraints.maxHeight.toFloat()

                Box(
                    modifier = Modifier
                        .width(if (fader.colSpan > 1) 36.dp else 24.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF090B10))
                        .border(1.dp, StageBorder, RoundedCornerShape(12.dp))
                        .pointerInput(fader.id, isEditMode, trackHeightPx) {
                            if (!isEditMode) {
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
                        }
                        .pointerInput(fader.id, isEditMode) {
                            if (!isEditMode) {
                                detectTapGestures { offset ->
                                    val norm = 1f - (offset.y / trackHeightPx).coerceIn(0f, 1f)
                                    onLevelChange(norm)
                                }
                            }
                        }
                ) {
                    // Level fill bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction = fader.level.coerceIn(0.01f, 1f))
                            .align(Alignment.BottomCenter)
                            .background(Brush.verticalGradient(listOf(faderColor, faderColor.copy(alpha = 0.4f))))
                    )

                    // Knob handle with accurate vertical displacement
                    val travelRangePx = (trackHeightPx - 20f).coerceAtLeast(0f)
                    val knobOffsetFromTop = ((1f - fader.level.coerceIn(0f, 1f)) * travelRangePx).toInt()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .align(Alignment.TopCenter)
                            .offset { androidx.compose.ui.unit.IntOffset(0, knobOffsetFromTop) }
                            .background(Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
                            .border(1.dp, faderColor, RoundedCornerShape(4.dp))
                    )
                }
            }

            // Quick Jump or Flash button in Live Mode
            if (!isEditMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(22.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(faderColor.copy(alpha = 0.2f))
                            .border(1.dp, faderColor, RoundedCornerShape(4.dp))
                            .clickable { onLevelChange(1.0f) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("FL", color = faderColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(22.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF161A24))
                            .border(1.dp, StageBorder, RoundedCornerShape(4.dp))
                            .clickable { onLevelChange(0.0f) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("0%", color = StageTextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Add Component Dialog
// ---------------------------------------------------------------------------
@Composable
fun AddComponentDialog(
    nextExec: Int,
    currentPage: Int,
    onDismiss: () -> Unit,
    onAddButton: (ExecutorButton) -> Unit,
    onAddFader: (FaderItem) -> Unit
) {
    var selectedType by remember { mutableStateOf("BUTTON") }
    var label by remember { mutableStateOf("Exec $nextExec") }
    var execNumber by remember { mutableStateOf(nextExec.toString()) }
    var colSpan by remember { mutableIntStateOf(1) }
    var rowSpan by remember { mutableIntStateOf(1) }
    var action by remember { mutableStateOf(ExecutorAction.GO) }
    var colorHex by remember { mutableStateOf("#00E5FF") }
    var isMasterFader by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StageSurfaceElevated,
        title = { Text("Add Component to Grid", color = StageTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Type Switcher
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            selectedType = "BUTTON"
                            rowSpan = 1
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "BUTTON") StageCyan else Color(0xFF1E2535),
                            contentColor = if (selectedType == "BUTTON") StageBlack else StageTextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("BUTTON")
                    }

                    Button(
                        onClick = {
                            selectedType = "FADER"
                            rowSpan = 2
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "FADER") StageAmber else Color(0xFF1E2535),
                            contentColor = if (selectedType == "FADER") StageBlack else StageTextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("FADER")
                    }
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = execNumber,
                    onValueChange = { execNumber = it },
                    label = { Text("Executor #") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Initial Size Spans
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = colSpan.toString(),
                        onValueChange = { colSpan = it.toIntOrNull()?.coerceIn(1, 4) ?: 1 },
                        label = { Text("Col Span (1-4)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rowSpan.toString(),
                        onValueChange = { rowSpan = it.toIntOrNull()?.coerceIn(1, 3) ?: 1 },
                        label = { Text("Row Span (1-3)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val exec = execNumber.toIntOrNull() ?: nextExec
                    if (selectedType == "BUTTON") {
                        onAddButton(
                            ExecutorButton(
                                id = "btn_${System.currentTimeMillis()}",
                                page = currentPage,
                                execNumber = exec,
                                label = label.ifBlank { "Exec $exec" },
                                action = action,
                                colorHex = colorHex,
                                colSpan = colSpan,
                                rowSpan = rowSpan
                            )
                        )
                    } else {
                        onAddFader(
                            FaderItem(
                                id = "fader_${System.currentTimeMillis()}",
                                page = currentPage,
                                execNumber = exec,
                                label = label.ifBlank { "Exec $exec" },
                                level = 0f,
                                isMaster = isMasterFader,
                                colorHex = colorHex,
                                colSpan = colSpan,
                                rowSpan = rowSpan
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StageCyan, contentColor = StageBlack)
            ) {
                Text("Add to Grid")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = StageTextSecondary) }
        }
    )
}

// ---------------------------------------------------------------------------
// Edit Fader Config Dialog
// ---------------------------------------------------------------------------
@Composable
fun EditFaderConfigDialog(
    fader: FaderItem,
    onDismiss: () -> Unit,
    onSave: (FaderItem) -> Unit,
    onDelete: () -> Unit
) {
    var label by remember { mutableStateOf(fader.label) }
    var execNumber by remember { mutableStateOf(fader.execNumber.toString()) }
    var pageText by remember { mutableStateOf(fader.page.toString()) }
    var colorHex by remember { mutableStateOf(fader.colorHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StageSurfaceElevated,
        title = { Text("Edit Fader Properties", color = StageTextPrimary) },
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
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = execNumber,
                        onValueChange = { execNumber = it },
                        label = { Text("Exec #") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = pageText.toIntOrNull() ?: fader.page
                    val e = execNumber.toIntOrNull() ?: fader.execNumber
                    onSave(fader.copy(label = label.ifBlank { "Exec $e" }, page = p, execNumber = e))
                },
                colors = ButtonDefaults.buttonColors(containerColor = StageCyan, contentColor = StageBlack)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Delete", color = StageRed) }
                TextButton(onClick = onDismiss) { Text("Cancel", color = StageTextSecondary) }
            }
        }
    )
}

// ---------------------------------------------------------------------------
// Dedicated MA2 Macro Tab & Components
// ---------------------------------------------------------------------------
@Composable
fun Ma2MacroTabContent(
    macros: List<MacroButton>,
    onTriggerMacro: (MacroButton) -> Unit,
    onAddMacro: () -> Unit,
    onEditMacro: (MacroButton) -> Unit,
    onDeleteMacro: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Macro Header Info bar
        Card(
            colors = CardDefaults.cardColors(containerColor = StageSurfaceElevated),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(StagePurple.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = StagePurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "grandMA2 System Macros",
                            color = StageTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Tap to trigger 'Macro <N>' OSC command • Visual state telemetry",
                            color = StageTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Button(
                    onClick = onAddMacro,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StagePurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_add_macro")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Macro",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Macro", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (macros.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = StageTextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No Macros Configured",
                        color = StageTextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Tap '+ New Macro' to create quick shortcuts for MA2 macro commands",
                        color = StageTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(macros, key = { it.id }) { macro ->
                    MacroCardItem(
                        macro = macro,
                        onTrigger = { onTriggerMacro(macro) },
                        onEdit = { onEditMacro(macro) },
                        onDelete = { onDeleteMacro(macro.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun MacroCardItem(
    macro: MacroButton,
    onTrigger: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val macroColor = try {
        Color(android.graphics.Color.parseColor(macro.colorHex))
    } catch (_: Exception) {
        StagePurple
    }

    val animatedBorderColor by animateColorAsState(
        targetValue = if (macro.isExecuting) macroColor else StageBorder,
        label = "macroBorder"
    )

    val animatedBgColor by animateColorAsState(
        targetValue = if (macro.isExecuting) macroColor.copy(alpha = 0.22f) else StageSurfaceElevated,
        label = "macroBg"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (macro.isExecuting) 2.dp else 1.dp,
                color = animatedBorderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onTrigger() }
            .testTag("macro_btn_${macro.macroNumber}"),
        colors = CardDefaults.cardColors(containerColor = animatedBgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Top row: ID badge + state indicator + action icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Macro number badge
                Box(
                    modifier = Modifier
                        .background(macroColor.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "M${macro.macroNumber}",
                        color = macroColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }

                // Status indicator pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            if (macro.isExecuting) StageGreen.copy(alpha = 0.2f) else StageSurfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                if (macro.isExecuting) StageGreen else StageTextMuted,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (macro.isExecuting) "RUNNING" else "READY",
                        color = if (macro.isExecuting) StageGreen else StageTextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Macro Label
            Text(
                text = macro.label,
                color = StageTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Command preview
            Text(
                text = macro.command.ifBlank { "Macro ${macro.macroNumber}" },
                color = StageCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (macro.lastExecutedTime != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Last fired: ${macro.lastExecutedTime}",
                    color = StageTextMuted,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row: Fire button + Edit/Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onTrigger,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (macro.isExecuting) StageGreen else macroColor,
                        contentColor = if (macro.isExecuting) StageBlack else Color.White
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (macro.isExecuting) Icons.Default.Sync else Icons.Default.PlayArrow,
                        contentDescription = "Trigger",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (macro.isExecuting) "RUNNING" else "EXECUTE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Macro",
                        tint = StageTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Macro",
                        tint = StageRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MacroConfigDialog(
    macro: MacroButton,
    isNew: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (MacroButton) -> Unit,
    onDelete: () -> Unit
) {
    var label by remember { mutableStateOf(macro.label) }
    var macroNumText by remember { mutableStateOf(macro.macroNumber.toString()) }
    var command by remember { mutableStateOf(macro.command) }
    var colorHex by remember { mutableStateOf(macro.colorHex) }
    var durationText by remember { mutableStateOf((macro.executionDurationMs / 1000f).toString()) }

    val presetColors = listOf(
        "#D500F9" to "Violet",
        "#00E5FF" to "Cyan",
        "#FFAB00" to "Amber",
        "#00E676" to "Green",
        "#FF1744" to "Red",
        "#2979FF" to "Blue"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StageSurfaceElevated,
        title = {
            Text(
                text = if (isNew) "Create MA2 Macro Button" else "Edit Macro Button",
                color = StageTextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Button Label") },
                    placeholder = { Text("e.g. Fog Burst / Strobe Off") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = macroNumText,
                        onValueChange = {
                            macroNumText = it
                            if (command == "Macro ${macro.macroNumber}" || command.isBlank()) {
                                command = "Macro $it"
                            }
                        },
                        label = { Text("MA2 Macro #") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it },
                        label = { Text("Duration (s)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text("Command sent via OSC /cmd") },
                    placeholder = { Text("e.g. Macro 5 or Go Macro 5") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Accent Color:",
                    color = StageTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.forEach { (hex, _) ->
                        val c = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (_: Exception) {
                            StagePurple
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(c, CircleShape)
                                .border(
                                    width = if (colorHex == hex) 2.dp else 1.dp,
                                    color = if (colorHex == hex) Color.White else StageBorder,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val num = macroNumText.toIntOrNull() ?: macro.macroNumber
                    val durSec = durationText.toFloatOrNull() ?: 1.8f
                    val durMs = (durSec * 1000L).toLong().coerceIn(200L, 10000L)
                    onSave(
                        macro.copy(
                            macroNumber = num,
                            label = label.ifBlank { "Macro $num" },
                            command = command.ifBlank { "Macro $num" },
                            colorHex = colorHex,
                            executionDurationMs = durMs
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = StagePurple,
                    contentColor = Color.White
                )
            ) {
                Text(if (isNew) "Create Macro" else "Save")
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
