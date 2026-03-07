package com.graberhub.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.graberhub.companion.ui.components.SectionHeader
import com.graberhub.companion.ui.theme.*
import com.graberhub.companion.viewmodel.MainViewModel

@Composable
fun TimerScreen(viewModel: MainViewModel) {
    val timerStatus by viewModel.timerStatus.collectAsState()
    val secondsLeft by viewModel.timerSecondsLeft.collectAsState()
    val timerFinished by viewModel.timerFinished.collectAsState()
    val alarmPlaying by viewModel.alarmPlaying.collectAsState()

    var minutes by remember { mutableIntStateOf(5) }
    var label by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionHeader("⏰ Timer")
        Spacer(modifier = Modifier.height(24.dp))

        if (timerFinished) {
            // Timer finished - show alarm UI
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ShedPinkLight)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⏰", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Time's Up!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = ShedPink
                    )
                    if (timerStatus?.label?.isNotEmpty() == true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(timerStatus?.label ?: "", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    if (alarmPlaying) {
                        Button(
                            onClick = { viewModel.stopAlarmSound() },
                            colors = ButtonDefaults.buttonColors(containerColor = ShedPink),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.VolumeOff, "Stop", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop Alarm", color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedButton(
                        onClick = { viewModel.dismissTimerFinished() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dismiss")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.cancelTimer() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset", color = TextSecondary)
                    }
                }
            }
        } else if (timerStatus?.active == true) {
            // Timer running
            val mins = secondsLeft / 60
            val secs = secondsLeft % 60

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .background(ShedBlueLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            String.format("%02d:%02d", mins, secs),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = ShedBlueDark
                        )
                    }

                    if (timerStatus?.label?.isNotEmpty() == true) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            timerStatus?.label ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.cancelTimer() },
                        colors = ButtonDefaults.buttonColors(containerColor = ShedPink),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Stop, "Cancel", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel Timer", color = Color.White)
                    }
                }
            }
        } else {
            // Set timer
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Set Timer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Quick presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(1, 5, 10, 15, 30).forEach { preset ->
                            OutlinedButton(
                                onClick = { minutes = preset },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (minutes == preset) ShedBlueLight else Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("${preset}m", color = if (minutes == preset) ShedBlueDark else TextSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Custom minutes
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(onClick = { if (minutes > 1) minutes-- }) {
                            Icon(Icons.Default.Remove, "Decrease", tint = ShedBlue)
                        }
                        Text(
                            "$minutes",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = ShedBlue,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        IconButton(onClick = { if (minutes < 120) minutes++ }) {
                            Icon(Icons.Default.Add, "Increase", tint = ShedBlue)
                        }
                    }
                    Text("minutes", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Label (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.startTimer(minutes, label) },
                        colors = ButtonDefaults.buttonColors(containerColor = ShedBlue),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, "Start", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Timer", color = Color.White, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}
