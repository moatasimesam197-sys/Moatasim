package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionState
import com.example.model.OscConfig
import com.example.ui.OscViewModel
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
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: OscViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentConfig = viewModel.config.value
    val connectionState = viewModel.connectionState.value

    var ipText by remember(currentConfig.targetIp) { mutableStateOf(currentConfig.targetIp) }
    var inPortText by remember(currentConfig.oscReceivePort) { mutableStateOf(currentConfig.oscReceivePort.toString()) }
    var outPortText by remember(currentConfig.oscSendPort) { mutableStateOf(currentConfig.oscSendPort.toString()) }
    var prefixText by remember(currentConfig.prefix) { mutableStateOf(currentConfig.prefix) }
    var cmdAddrText by remember(currentConfig.cmdAddress) { mutableStateOf(currentConfig.cmdAddress) }
    var syntaxAt by remember(currentConfig.atSymbol) { mutableStateOf(currentConfig.atSymbol) }
    var rateLimitHz by remember(currentConfig.faderRateLimitHz) { mutableFloatStateOf(currentConfig.faderRateLimitHz.toFloat()) }
    var autoReconnect by remember(currentConfig.autoReconnect) { mutableStateOf(currentConfig.autoReconnect) }
    var hapticEnabled by remember(currentConfig.hapticEnabled) { mutableStateOf(currentConfig.hapticEnabled) }

    var showRawJson by remember { mutableStateOf(false) }
    var showSetupGuide by remember { mutableStateOf(false) }

    val isConnected = connectionState is ConnectionState.Connected
    val isConnecting = connectionState is ConnectionState.Connecting

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StageBlack)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Connection Control Card
        Card(
            colors = CardDefaults.cardColors(containerColor = StageSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StageBorder, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Wifi, contentDescription = "Network", tint = StageCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NETWORK & CONSOLE LINK",
                            color = StageCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Status Badge
                    val (statusColor, statusLabel) = when (connectionState) {
                        is ConnectionState.Connected -> StageGreen to "CONNECTED"
                        is ConnectionState.Connecting -> StageAmber to "CONNECTING"
                        is ConnectionState.Error -> StageRed to "ERROR"
                        is ConnectionState.Disconnected -> StageTextMuted to "DISCONNECTED"
                    }

                    Row(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .border(1.dp, statusColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(statusColor))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(statusLabel, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                // PC IP Address Input
                OutlinedTextField(
                    value = ipText,
                    onValueChange = { ipText = it },
                    label = { Text("PC IP Address (e.g. 192.168.1.100)") },
                    supportingText = { Text("Windows PC running grandMA2 onPC on same Wi-Fi", color = StageTextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("ip_input_field")
                )

                // OSC In & Out Ports
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = inPortText,
                        onValueChange = { inPortText = it },
                        label = { Text("In Port (PC Listens)") },
                        supportingText = { Text("Console: In Port (default: 8000)", color = StageTextMuted, fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("in_port_input_field")
                    )
                    OutlinedTextField(
                        value = outPortText,
                        onValueChange = { outPortText = it },
                        label = { Text("Feedback Port (Phone)") },
                        supportingText = { Text("Console: Out Port (default: 8001)", color = StageTextMuted, fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("out_port_input_field")
                    )
                }

                // Connect / Disconnect Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isConnected) {
                        Button(
                            onClick = { viewModel.disconnect() },
                            colors = ButtonDefaults.buttonColors(containerColor = StageRed, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(46.dp).testTag("disconnect_action_button")
                        ) {
                            Icon(Icons.Default.WifiOff, contentDescription = "Disconnect")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DISCONNECT", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                val inP = inPortText.toIntOrNull() ?: 8000
                                val outP = outPortText.toIntOrNull() ?: 8001
                                val newCfg = currentConfig.copy(
                                    targetIp = ipText.trim(),
                                    oscReceivePort = inP,
                                    oscSendPort = outP
                                )
                                viewModel.updateConfig(newCfg)
                                viewModel.connect()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StageGreen, contentColor = StageBlack),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(46.dp).testTag("connect_action_button")
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = StageBlack, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CONNECTING...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Wifi, contentDescription = "Connect")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CONNECT TO ONPC", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // OSC Mapping & Syntax Rules Card
        Card(
            colors = CardDefaults.cardColors(containerColor = StageSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, StageBorder, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = "Config", tint = StageAmber, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OSC ADDRESS PATTERNS & SYNTAX",
                        color = StageAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = prefixText,
                        onValueChange = { prefixText = it },
                        label = { Text("OSC Prefix") },
                        supportingText = { Text("grandMA2 OSC Prefix (default: /gma2)", color = StageTextMuted, fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cmdAddrText,
                        onValueChange = { cmdAddrText = it },
                        label = { Text("Command Address") },
                        supportingText = { Text("Command string path (default: /gma2/cmd)", color = StageTextMuted, fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                }

                // Command Line Syntax Toggle ("@" vs "At")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("grandMA2 Syntax Mapping", color = StageTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Choose '@' (modern) or 'At' (classic console keyword)", color = StageTextMuted, fontSize = 11.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { syntaxAt = "@" },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (syntaxAt == "@") StageCyan.copy(alpha = 0.2f) else Color.Transparent
                            )
                        ) {
                            Text("@", color = if (syntaxAt == "@") StageCyan else StageTextSecondary, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { syntaxAt = "At" },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (syntaxAt == "At") StageCyan.copy(alpha = 0.2f) else Color.Transparent
                            )
                        ) {
                            Text("At", color = if (syntaxAt == "At") StageCyan else StageTextSecondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Divider(color = StageBorder)

                // Fader Rate Limiter Slider (10 Hz to 60 Hz)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Fader Rate Limiter (Throttling)", color = StageTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("${rateLimitHz.roundToInt()} Hz (max ~${rateLimitHz.roundToInt()} msgs/sec)", color = StageCyan, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                    Text("Avoids UDP buffer overflows on PC console network card", color = StageTextMuted, fontSize = 11.sp)
                    Slider(
                        value = rateLimitHz,
                        onValueChange = { rateLimitHz = it },
                        valueRange = 10f..60f,
                        steps = 9,
                        colors = SliderDefaults.colors(thumbColor = StageCyan, activeTrackColor = StageCyan)
                    )
                }

                Divider(color = StageBorder)

                // Auto-Reconnect Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Auto-Reconnect with Backoff", color = StageTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Automatically recovers connection with exponential retry (2s..30s)", color = StageTextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = autoReconnect,
                        onCheckedChange = { autoReconnect = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = StageCyan)
                    )
                }

                // Haptic Feedback Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Haptic Vibration Feedback", color = StageTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Tactile impulse on GO, Flash, and Keypad buttons", color = StageTextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = hapticEnabled,
                        onCheckedChange = { hapticEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = StageAmber)
                    )
                }

                // Save Config Button
                Button(
                    onClick = {
                        val inP = inPortText.toIntOrNull() ?: 8000
                        val outP = outPortText.toIntOrNull() ?: 8001
                        val newCfg = currentConfig.copy(
                            targetIp = ipText.trim(),
                            oscReceivePort = inP,
                            oscSendPort = outP,
                            prefix = prefixText.trim(),
                            cmdAddress = cmdAddrText.trim(),
                            atSymbol = syntaxAt,
                            faderRateLimitHz = rateLimitHz.roundToInt(),
                            autoReconnect = autoReconnect,
                            hapticEnabled = hapticEnabled
                        )
                        viewModel.updateConfig(newCfg)
                        Toast.makeText(context, "Saved OSC configuration", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StageCyan, contentColor = StageBlack),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("save_settings_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SAVE SETTINGS", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Setup Instructions & Console Guide Card (Expandable)
        Card(
            colors = CardDefaults.cardColors(containerColor = StageSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, StageBorder, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showSetupGuide = !showSetupGuide },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Help", tint = StageGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GRANDMA2 ONPC SETUP & TEST CHECKLIST",
                            color = StageGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(if (showSetupGuide) "HIDE" else "VIEW GUIDE", color = StageCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                AnimatedVisibility(visible = showSetupGuide) {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GuideStep(
                            step = "1",
                            title = "Enable OSC in grandMA2 onPC",
                            details = "1. Open grandMA2 onPC on your Windows PC.\n2. Click Setup -> Network -> OSC Configuration.\n3. Verify Status: Active (Green).\n4. In Port: 8000 (Matches app In Port).\n5. Out Port: 8001 (Matches app Feedback Port).\n6. Prefix: /gma2\n7. Mode: In & Out."
                        )

                        GuideStep(
                            step = "2",
                            title = "Find PC's Local IP Address",
                            details = "1. Press Windows Key + R, type 'cmd' and hit Enter.\n2. In Command Prompt, type 'ipconfig'.\n3. Look for 'IPv4 Address' under your active Wi-Fi or Ethernet adapter (e.g. 192.168.1.150).\n4. Ensure your phone is connected to the EXACT same Wi-Fi SSID / subnet."
                        )

                        GuideStep(
                            step = "3",
                            title = "Windows Firewall UDP Port Rule",
                            details = "Windows Firewall often blocks incoming UDP packets by default:\n1. Open Windows Defender Firewall -> Advanced Settings.\n2. Click Inbound Rules -> New Rule.\n3. Rule Type: Port -> UDP -> Specific local ports: 8000, 8001.\n4. Action: Allow the connection.\n5. Profile: Domain, Private, Public -> Finish."
                        )

                        GuideStep(
                            step = "4",
                            title = "Verification & Testing Checklist",
                            details = "• Connection Test: Tap 'Connect to onPC'. Status turns green and local port binds.\n• Fader Latency Test: Slide Master or Exec 1 fader; check OSC live log for TX timestamp.\n• Reconnect Test: Toggle phone Wi-Fi off and on; notice warning banner and auto-reconnect backoff.\n• Blackout Test: Tap Blackout; verify onPC master playback halts.\n• Firewall Test: If TX sends but onPC does not react, verify UDP port 8000 in Windows Firewall."
                        )
                    }
                }
            }
        }

        // Raw osc_config.json Viewer
        Card(
            colors = CardDefaults.cardColors(containerColor = StageSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, StageBorder, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showRawJson = !showRawJson },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, contentDescription = "JSON", tint = StageTextSecondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RAW CONFIG FILE (osc_config.json)",
                            color = StageTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(if (showRawJson) "COLLAPSE" else "EXPAND", color = StageCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                AnimatedVisibility(visible = showRawJson) {
                    val rawAssetJson = remember { viewModel.repository.loadRawAssetConfigJson() }
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text(
                            text = "Bundled default schema with comments and port mappings:",
                            color = StageTextMuted,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = rawAssetJson,
                            onValueChange = {},
                            readOnly = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = StageTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().height(240.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GuideStep(step: String, title: String, details: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F131C), RoundedCornerShape(8.dp))
            .border(1.dp, StageBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(StageCyan),
                contentAlignment = Alignment.Center
            ) {
                Text(step, color = StageBlack, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, color = StageCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = details,
            color = StageTextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
