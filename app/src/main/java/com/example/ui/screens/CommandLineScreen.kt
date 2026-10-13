package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
import com.example.ui.theme.StagePurple
import com.example.ui.theme.StageRed
import com.example.ui.theme.StageSurface
import com.example.ui.theme.StageSurfaceElevated
import com.example.ui.theme.StageTextMuted
import com.example.ui.theme.StageTextPrimary
import com.example.ui.theme.StageTextSecondary

@Composable
fun CommandLineScreen(
    viewModel: OscViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cmdText = viewModel.commandLineText.value
    val history = viewModel.commandHistory.value
    val config = viewModel.config.value
    val hapticEnabled = config.hapticEnabled

    var activeTab by remember { mutableIntStateOf(0) } // 0: Hardware Keypad, 1: History
    var showSoftKeyboard by remember { mutableStateOf(false) }
    var lastExecutedEcho by remember { mutableStateOf<String?>("Ready for syntax command.") }
    val focusRequester = remember { FocusRequester() }

    // Blinking cursor transition for authentic console CRT effect
    val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 480, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StageBlack)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- 1. Authentic MA2 Hardware Command Line Display Header ---
        Ma2ConsoleHeader(
            lastEcho = lastExecutedEcho,
            modifier = Modifier.fillMaxWidth()
        )

        // --- Quick-Access Common MA2 Syntax Buttons (Above Input Field) ---
        Ma2QuickSyntaxRow(
            atSymbol = config.atSymbol,
            onTokenClick = { token ->
                if (hapticEnabled) HapticUtil.performHaptic(context)
                viewModel.appendToCommandLine(token)
            },
            modifier = Modifier.fillMaxWidth()
        )

        // --- 2. The grandMA2 Hardware Command Line Input Strip ---
        Ma2CommandLineInputStrip(
            commandText = cmdText,
            atSymbol = config.atSymbol,
            cursorAlpha = cursorAlpha,
            showSoftKeyboard = showSoftKeyboard,
            focusRequester = focusRequester,
            onCommandChange = { viewModel.updateCommandLineText(it) },
            onClear = {
                if (hapticEnabled) HapticUtil.performHaptic(context)
                viewModel.clearCommandLine()
            },
            onToggleKeyboard = {
                showSoftKeyboard = !showSoftKeyboard
                if (showSoftKeyboard) {
                    try { focusRequester.requestFocus() } catch (ignored: Exception) {}
                }
            },
            onExecute = {
                if (cmdText.isNotBlank()) {
                    if (hapticEnabled) HapticUtil.performHaptic(context, isStrong = true)
                    lastExecutedEcho = "[OK] Executed: $cmdText"
                    viewModel.submitCommandLine()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Tab Navigation: Hardware Keypad vs History
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StageSurface, RoundedCornerShape(8.dp))
                .border(1.dp, StageBorder, RoundedCornerShape(8.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Ma2ModeTab(
                    label = "HARDWARE KEYPAD",
                    icon = Icons.Default.Dialpad,
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 }
                )
                Ma2ModeTab(
                    label = "HISTORY (${history.size})",
                    icon = Icons.Default.History,
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 }
                )
            }

            // Syntax helper preview
            Text(
                text = formatSyntaxHelper(cmdText, config.atSymbol),
                color = StageAmber,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        // --- 3. Main Workspace: Keypad vs History ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (activeTab == 0) {
                // Full grandMA2 Console Keypad Layout with iconic PLEASE key
                Ma2HardwareKeypadLayout(
                    atSymbol = config.atSymbol,
                    onAppend = { token ->
                        if (hapticEnabled) HapticUtil.performHaptic(context)
                        viewModel.appendToCommandLine(token)
                    },
                    onBackspace = {
                        if (hapticEnabled) HapticUtil.performHaptic(context)
                        val curr = viewModel.commandLineText.value
                        if (curr.isNotEmpty()) {
                            viewModel.updateCommandLineText(curr.dropLast(1).trimEnd())
                        }
                    },
                    onClear = {
                        if (hapticEnabled) HapticUtil.performHaptic(context)
                        viewModel.clearCommandLine()
                    },
                    onClearAll = {
                        viewModel.requestClearAll()
                    },
                    onPlease = {
                        if (cmdText.isNotBlank()) {
                            if (hapticEnabled) HapticUtil.performHaptic(context, isStrong = true)
                            lastExecutedEcho = "[OK] Executed: $cmdText"
                            viewModel.submitCommandLine()
                        } else {
                            if (hapticEnabled) HapticUtil.performHaptic(context)
                            lastExecutedEcho = "[Warning] Command line empty."
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // History List
                Ma2HistoryList(
                    history = history,
                    onSelect = { item ->
                        if (hapticEnabled) HapticUtil.performHaptic(context)
                        viewModel.updateCommandLineText(item)
                        activeTab = 0
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 1. grandMA2 Console Display Header & Echo Bar
// ---------------------------------------------------------------------------
@Composable
fun Ma2ConsoleHeader(
    lastEcho: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF0F121A), RoundedCornerShape(8.dp))
            .border(1.dp, StageBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(StageGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "grandMA2 onPC COMMAND LINE",
                    color = StageCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }

            Text(
                text = "USER: ADMIN • STAGE SHOW",
                color = StageTextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Echo response line (looks like genuine CRT console feedback)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "CONSOLE ECHO:",
                color = StageAmber,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = lastEcho ?: "Ready.",
                color = if (lastEcho?.contains("Warning") == true || lastEcho?.contains("Error") == true) StageRed else Color(0xFF68D391),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 2. grandMA2 Hardware Command Line Input Strip with Syntax Highlighting & CRT Cursor
// ---------------------------------------------------------------------------
@Composable
fun Ma2CommandLineInputStrip(
    commandText: String,
    atSymbol: String,
    cursorAlpha: Float,
    showSoftKeyboard: Boolean,
    focusRequester: FocusRequester,
    onCommandChange: (String) -> Unit,
    onClear: () -> Unit,
    onToggleKeyboard: () -> Unit,
    onExecute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val annotatedSyntax = remember(commandText, atSymbol) {
        highlightMa2Syntax(commandText, atSymbol)
    }

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF141722), Color(0xFF0C0E15))
                ),
                RoundedCornerShape(10.dp)
            )
            .border(1.5.dp, StageBorder, RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Authentic MA2 Command Prompt Badge [Cmd]
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF282310))
                    .border(1.dp, StageAmber.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Cmd",
                        color = StageAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "#",
                        color = StageGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // CRT Phosphor Display Window with Syntax Highlighting & Blinking Cursor
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF060709))
                    .border(1.dp, Color(0xFF1A2234), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (showSoftKeyboard) {
                    // Transparent BasicTextField to allow system keyboard input while preserving styling
                    BasicTextField(
                        value = commandText,
                        onValueChange = onCommandChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        ),
                        cursorBrush = SolidColor(StageGreen),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onExecute() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .testTag("hardware_command_input_field")
                    )
                } else {
                    // Visual Hardware Terminal Renderer with Syntax Colors & Phosphor Cursor
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (commandText.isEmpty()) {
                            Text(
                                text = "Enter syntax (e.g. Fixture 1 Thru 5 $atSymbol 80)...",
                                color = Color(0xFF475569),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            Text(
                                text = annotatedSyntax,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Blinking block cursor █ like the real hardware console
                        Text(
                            text = "█",
                            color = StageGreen.copy(alpha = cursorAlpha),
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Keyboard Toggle
            IconButton(
                onClick = onToggleKeyboard,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF161A24), RoundedCornerShape(6.dp))
                    .border(1.dp, StageBorder, RoundedCornerShape(6.dp))
                    .testTag("toggle_soft_keyboard_btn")
            ) {
                Icon(
                    imageVector = if (showSoftKeyboard) Icons.Default.Dialpad else Icons.Default.Keyboard,
                    contentDescription = "Toggle Keyboard",
                    tint = if (showSoftKeyboard) StageCyan else StageTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Quick Clear / Backspace Button
            IconButton(
                onClick = onClear,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF161A24), RoundedCornerShape(6.dp))
                    .border(1.dp, StageBorder, RoundedCornerShape(6.dp))
                    .testTag("clear_command_strip_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "Clear Input",
                    tint = if (commandText.isNotEmpty()) StageAmber else StageTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 3. Complete grandMA2 Hardware Keypad with Iconic PLEASE Key
// ---------------------------------------------------------------------------
@Composable
fun Ma2HardwareKeypadLayout(
    atSymbol: String,
    onAppend: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onClearAll: () -> Unit,
    onPlease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF10131B), RoundedCornerShape(12.dp))
            .border(1.2.dp, StageBorder, RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: Function / Object Target Selectors (MA Classic Gold/Cyan)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Ma2Keycap(label = "Fixture", subLabel = "FIX", color = StageCyan, modifier = Modifier.weight(1f)) { onAppend("Fixture") }
            Ma2Keycap(label = "Group", subLabel = "GRP", color = StageCyan, modifier = Modifier.weight(1f)) { onAppend("Group") }
            Ma2Keycap(label = "Preset", subLabel = "PST", color = StageCyan, modifier = Modifier.weight(1f)) { onAppend("Preset") }
            Ma2Keycap(label = "Cue", subLabel = "CUE", color = StageAmber, modifier = Modifier.weight(1f)) { onAppend("Cue") }
            Ma2Keycap(label = "Exec", subLabel = "EXE", color = StageAmber, modifier = Modifier.weight(1f)) { onAppend("Exec") }
        }

        // Row 2: Range Modifiers & Levels
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Ma2Keycap(label = "Thru", subLabel = "RANGE", color = StageAmber, modifier = Modifier.weight(1f)) { onAppend("Thru") }
            Ma2Keycap(label = "+", subLabel = "ADD", color = StageTextPrimary, modifier = Modifier.weight(1f)) { onAppend("+") }
            Ma2Keycap(label = "-", subLabel = "SUB", color = StageTextPrimary, modifier = Modifier.weight(1f)) { onAppend("-") }
            Ma2Keycap(label = atSymbol, subLabel = "AT", color = StageGreen, isEmphasized = true, modifier = Modifier.weight(1f)) { onAppend(atSymbol) }
            Ma2Keycap(label = "Full", subLabel = "100%", color = StageGreen, isEmphasized = true, modifier = Modifier.weight(1f)) { onAppend("Full") }
        }

        // Main Numeric Section + PLEASE Key Area (Organized like MA2 Command Section)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Left 4-Columns: Numeric Keypad (7 8 9, 4 5 6, 1 2 3, 0 . Zero) + Special Tools
            Column(
                modifier = Modifier
                    .weight(3.4f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Keypad Row 7, 8, 9, Highlight
                Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Ma2NumericKeycap("7", modifier = Modifier.weight(1f)) { onAppend("7") }
                    Ma2NumericKeycap("8", modifier = Modifier.weight(1f)) { onAppend("8") }
                    Ma2NumericKeycap("9", modifier = Modifier.weight(1f)) { onAppend("9") }
                    Ma2Keycap(label = "Highlt", subLabel = "HL", color = StagePurple, modifier = Modifier.weight(1.1f)) { onAppend("Highlight") }
                }

                // Keypad Row 4, 5, 6, Solo
                Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Ma2NumericKeycap("4", modifier = Modifier.weight(1f)) { onAppend("4") }
                    Ma2NumericKeycap("5", modifier = Modifier.weight(1f)) { onAppend("5") }
                    Ma2NumericKeycap("6", modifier = Modifier.weight(1f)) { onAppend("6") }
                    Ma2Keycap(label = "Solo", subLabel = "SOLO", color = StagePurple, modifier = Modifier.weight(1.1f)) { onAppend("Solo") }
                }

                // Keypad Row 1, 2, 3, Blind
                Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Ma2NumericKeycap("1", modifier = Modifier.weight(1f)) { onAppend("1") }
                    Ma2NumericKeycap("2", modifier = Modifier.weight(1f)) { onAppend("2") }
                    Ma2NumericKeycap("3", modifier = Modifier.weight(1f)) { onAppend("3") }
                    Ma2Keycap(label = "Blind", subLabel = "BLN", color = StagePurple, modifier = Modifier.weight(1.1f)) { onAppend("Blind") }
                }

                // Keypad Row 0, ., Zero, Clear
                Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Ma2NumericKeycap("0", modifier = Modifier.weight(1f)) { onAppend("0") }
                    Ma2NumericKeycap(".", modifier = Modifier.weight(1f)) { onAppend(".") }
                    Ma2Keycap(label = "Clear", subLabel = "CLR", color = StageAmber, modifier = Modifier.weight(1f)) { onClear() }
                    Ma2Keycap(label = "Oops", subLabel = "UNDO", color = StageOrange, modifier = Modifier.weight(1.1f)) { onAppend("Oops") }
                }

                // Bottom Safety & Store Functions
                Row(modifier = Modifier.fillMaxWidth().weight(0.9f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Ma2Keycap(label = "Store", subLabel = "REC", color = StageAmber, modifier = Modifier.weight(1.2f)) { onAppend("Store") }
                    Ma2Keycap(label = "Update", subLabel = "UPD", color = StageCyan, modifier = Modifier.weight(1.2f)) { onAppend("Update") }
                    Ma2Keycap(label = "Clear All", subLabel = "DANGER", color = StageRed, isDestructive = true, modifier = Modifier.weight(1.8f)) { onClearAll() }
                }
            }

            // Right Column: The Famous GrandMA2 "PLEASE" Key (Large, tactile, illuminated hardware enter keycap)
            Column(
                modifier = Modifier
                    .weight(1.6f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Secondary Enter shortcut or Backspace
                Ma2Keycap(
                    label = "BKSP",
                    subLabel = "DEL",
                    color = StageTextSecondary,
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    onBackspace()
                }

                // The Monumental "PLEASE" (Enter) Key
                Ma2HardwarePleaseButton(
                    onClick = onPlease,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("hardware_please_button")
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 4. Iconic MA2 PLEASE (Enter) Hardware Key Component
// ---------------------------------------------------------------------------
@Composable
fun Ma2HardwarePleaseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val keyBg by animateColorAsState(
        targetValue = if (isPressed) Color(0xFF00E676) else Color(0xFF132B1C),
        label = "please_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isPressed) Color.White else Color(0xFF00FF66),
        label = "please_border"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(Color(0xFF00E676), Color(0xFF00B248))
                    } else {
                        listOf(Color(0xFF163824), Color(0xFF0B1E13))
                    }
                )
            )
            .border(
                width = if (isPressed) 3.dp else 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Jewel LED status diode on top of the key
            Box(
                modifier = Modifier
                    .size(width = 24.dp, height = 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isPressed) Color.White else Color(0xFF00FF66))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Authentic grandMA2 keycap legend
            Text(
                text = "PLEASE",
                color = if (isPressed) StageBlack else Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "ENTER ↵",
                color = if (isPressed) StageBlack else Color(0xFF00FF66),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle hint matching MA hardware
            Text(
                text = "EXECUTE SYNTAX",
                color = if (isPressed) StageBlack.copy(alpha = 0.8f) else StageTextMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 5. Individual MA2 Hardware Keycap Component
// ---------------------------------------------------------------------------
@Composable
fun Ma2Keycap(
    label: String,
    subLabel: String? = null,
    color: Color = StageTextPrimary,
    isEmphasized: Boolean = false,
    isDestructive: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val bgGradient = if (isPressed) {
        listOf(color.copy(alpha = 0.5f), color.copy(alpha = 0.3f))
    } else if (isEmphasized) {
        listOf(Color(0xFF1E2836), Color(0xFF131A24))
    } else {
        listOf(Color(0xFF181C26), Color(0xFF10131B))
    }

    val finalBorder = when {
        isPressed -> color
        isDestructive -> StageRed
        isEmphasized -> color.copy(alpha = 0.8f)
        else -> StageBorder
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(bgGradient))
            .border(1.2.dp, finalBorder, RoundedCornerShape(8.dp))
            .pointerInput(label) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = if (isDestructive) StageRed else color,
                fontSize = if (label.length > 5) 11.sp else 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                textAlign = TextAlign.Center
            )

            if (subLabel != null) {
                Text(
                    text = subLabel,
                    color = if (isDestructive) StageRed.copy(alpha = 0.7f) else StageTextMuted,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun Ma2NumericKeycap(
    number: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(
                    if (isPressed) listOf(Color(0xFF2A364F), Color(0xFF1E273A))
                    else listOf(Color(0xFF1B2230), Color(0xFF121722))
                )
            )
            .border(
                1.dp,
                if (isPressed) StageCyan else StageBorder,
                RoundedCornerShape(8.dp)
            )
            .pointerInput(number) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

// ---------------------------------------------------------------------------
// 6. Navigation Tab Pill
// ---------------------------------------------------------------------------
@Composable
fun Ma2ModeTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) StageCyan.copy(alpha = 0.2f) else Color.Transparent)
            .border(1.dp, if (selected) StageCyan else Color.Transparent, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) StageCyan else StageTextMuted,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (selected) StageCyan else StageTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 7. Command History Viewer
// ---------------------------------------------------------------------------
@Composable
fun Ma2HistoryList(
    history: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF0C0E15), RoundedCornerShape(12.dp))
            .border(1.dp, StageBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LOGGED EXECUTIONS (MOST RECENT FIRST)",
                color = StageCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tap item to recall to command line",
                color = StageTextMuted,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No previous commands executed.", color = StageTextMuted, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(history) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF141822))
                            .border(1.dp, StageBorder.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .clickable { onSelect(item) }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CMD>",
                            color = StageAmber,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item,
                            color = StageTextPrimary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 8. Syntax Highlighting Parser for grandMA2 Grammar
// ---------------------------------------------------------------------------
fun highlightMa2Syntax(input: String, atSymbol: String): AnnotatedString {
    if (input.isBlank()) return AnnotatedString("")

    return buildAnnotatedString {
        val tokens = input.split(" ")
        tokens.forEachIndexed { index, token ->
            val upper = token.uppercase()
            when {
                // Target objects (Yellow/Amber)
                upper in listOf("FIXTURE", "GROUP", "PRESET", "CUE", "EXEC", "PAGE", "CHANNEL", "SEQUENCE") -> {
                    withStyle(SpanStyle(color = StageAmber, fontWeight = FontWeight.ExtraBold)) {
                        append(token)
                    }
                }
                // Range & Connectors (Cyan)
                upper in listOf("THRU", "+", "-", "/") || token == atSymbol || upper == "AT" -> {
                    withStyle(SpanStyle(color = StageCyan, fontWeight = FontWeight.Bold)) {
                        append(token)
                    }
                }
                // Levels & Numbers (Green)
                upper in listOf("FULL", "ZERO") || token.matches(Regex("^[0-9]+(\\.[0-9]+)?$")) -> {
                    withStyle(SpanStyle(color = StageGreen, fontWeight = FontWeight.Bold)) {
                        append(token)
                    }
                }
                // Program functions (Purple)
                upper in listOf("STORE", "UPDATE", "HIGHLIGHT", "SOLO", "BLIND") -> {
                    withStyle(SpanStyle(color = StagePurple, fontWeight = FontWeight.Bold)) {
                        append(token)
                    }
                }
                // Destructive / Oops (Red)
                upper in listOf("CLEAR", "CLEARALL", "DELETE", "OOPS") -> {
                    withStyle(SpanStyle(color = StageRed, fontWeight = FontWeight.Bold)) {
                        append(token)
                    }
                }
                else -> {
                    withStyle(SpanStyle(color = Color.White)) {
                        append(token)
                    }
                }
            }
            if (index < tokens.size - 1) append(" ")
        }
    }
}

fun formatSyntaxHelper(input: String, atSymbol: String): String {
    if (input.isBlank()) return "Syntax: Target (Fixture/Group) + ID + $atSymbol + Value"
    val trimmed = input.trim()
    return when {
        trimmed.startsWith("Fixture", ignoreCase = true) -> "Syntax: Fixture [ID] [Thru ID] [$atSymbol Value]"
        trimmed.startsWith("Group", ignoreCase = true) -> "Syntax: Group [ID] [$atSymbol Value]"
        trimmed.startsWith("Preset", ignoreCase = true) -> "Syntax: Preset [Type.ID]"
        trimmed.startsWith("Cue", ignoreCase = true) -> "Syntax: Cue [Number] [Go/Pause]"
        trimmed.startsWith("Store", ignoreCase = true) -> "Syntax: Store Cue/Preset [ID]"
        else -> "Syntax: Command will fire over OSC"
    }
}

// ---------------------------------------------------------------------------
// 9. Quick-Access Common MA2 Syntax Buttons (Placed Directly Above Input Field)
// ---------------------------------------------------------------------------
@Composable
fun Ma2QuickSyntaxRow(
    atSymbol: String,
    onTokenClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickSyntaxButton(
            label = "Group",
            token = "Group",
            accentColor = StageCyan,
            tag = "quick_syntax_group",
            onClick = onTokenClick
        )
        QuickSyntaxButton(
            label = "Preset",
            token = "Preset",
            accentColor = StagePurple,
            tag = "quick_syntax_preset",
            onClick = onTokenClick
        )
        QuickSyntaxButton(
            label = "Fixture",
            token = "Fixture",
            accentColor = StageAmber,
            tag = "quick_syntax_fixture",
            onClick = onTokenClick
        )
        QuickSyntaxButton(
            label = atSymbol,
            token = atSymbol,
            accentColor = StageGreen,
            tag = "quick_syntax_at",
            onClick = onTokenClick
        )
        QuickSyntaxButton(
            label = "Full",
            token = "Full",
            accentColor = StageGreen,
            tag = "quick_syntax_full",
            onClick = onTokenClick
        )
        QuickSyntaxButton(
            label = "Thru",
            token = "Thru",
            accentColor = StageOrange,
            tag = "quick_syntax_thru",
            onClick = onTokenClick
        )
        QuickSyntaxButton(
            label = "+",
            token = "+",
            accentColor = StageTextPrimary,
            tag = "quick_syntax_plus",
            onClick = onTokenClick
        )
        QuickSyntaxButton(
            label = "-",
            token = "-",
            accentColor = StageTextPrimary,
            tag = "quick_syntax_minus",
            onClick = onTokenClick
        )
        QuickSyntaxButton(
            label = "Cue",
            token = "Cue",
            accentColor = StageAmber,
            tag = "quick_syntax_cue",
            onClick = onTokenClick
        )
        QuickSyntaxButton(
            label = "Zero",
            token = "Zero",
            accentColor = StageTextSecondary,
            tag = "quick_syntax_zero",
            onClick = onTokenClick
        )
    }
}

@Composable
fun QuickSyntaxButton(
    label: String,
    token: String,
    accentColor: Color,
    tag: String,
    onClick: (String) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(
                    if (isPressed) {
                        listOf(accentColor.copy(alpha = 0.45f), accentColor.copy(alpha = 0.25f))
                    } else {
                        listOf(Color(0xFF1E2433), Color(0xFF121622))
                    }
                )
            )
            .border(
                width = if (isPressed) 1.8.dp else 1.dp,
                color = if (isPressed) accentColor else accentColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(token) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick(token)
                    }
                )
            }
            .padding(horizontal = 12.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (isPressed) Color.White else accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }
    }
}

