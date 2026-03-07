package com.graberhub.companion.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.graberhub.companion.ui.theme.*
import kotlin.math.*

// ── Color Picker ───────────────────────────────────────

val presetColors = listOf(
    "#ec5281", "#03a9f4", "#a78bfa", "#4ade80",
    "#f59e0b", "#ef4444", "#06b6d4", "#8b5cf6",
    "#f97316", "#10b981", "#6366f1", "#ec4899"
)

@Composable
fun ColorPickerDialog(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardBackground
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Choose Color",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Color grid: 4 columns × 3 rows
                for (row in 0 until 3) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (col in 0 until 4) {
                            val color = presetColors[row * 4 + col]
                            val isSelected = color.equals(selectedColor, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(color)))
                                    .then(
                                        if (isSelected)
                                            Modifier.border(3.dp, TextPrimary, CircleShape)
                                        else Modifier
                                    )
                                    .clickable {
                                        onColorSelected(color)
                                        onDismiss()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Clock Face Time Picker ─────────────────────────────

@Composable
fun ClockTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableIntStateOf(initialHour) }
    var minute by remember { mutableIntStateOf(initialMinute) }
    var isPm by remember { mutableStateOf(initialHour >= 12) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardBackground
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Time",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Time display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Hour
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TextButton(onClick = {
                            val h12 = if (hour % 12 == 0) 12 else hour % 12
                            val next = if (h12 >= 12) 1 else h12 + 1
                            hour = if (isPm) (if (next == 12) 12 else next + 12) else (if (next == 12) 0 else next)
                        }) { Text("▲", fontSize = 20.sp) }
                        Text(
                            text = String.format("%02d", if (hour % 12 == 0) 12 else hour % 12),
                            style = MaterialTheme.typography.displayLarge,
                            color = ShedBlue
                        )
                        TextButton(onClick = {
                            val h12 = if (hour % 12 == 0) 12 else hour % 12
                            val prev = if (h12 <= 1) 12 else h12 - 1
                            hour = if (isPm) (if (prev == 12) 12 else prev + 12) else (if (prev == 12) 0 else prev)
                        }) { Text("▼", fontSize = 20.sp) }
                    }

                    Text(
                        ":",
                        style = MaterialTheme.typography.displayLarge,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Minute
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TextButton(onClick = { minute = (minute + 5) % 60 }) {
                            Text("▲", fontSize = 20.sp)
                        }
                        Text(
                            text = String.format("%02d", minute),
                            style = MaterialTheme.typography.displayLarge,
                            color = ShedBlue
                        )
                        TextButton(onClick = { minute = if (minute < 5) 55 else minute - 5 }) {
                            Text("▼", fontSize = 20.sp)
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    // AM/PM
                    Column {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    isPm = false
                                    hour = hour % 12
                                },
                            color = if (!isPm) ShedBlue else BackgroundGray
                        ) {
                            Text(
                                "AM",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = if (!isPm) Color.White else TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    isPm = true
                                    hour = if (hour < 12) hour + 12 else hour
                                },
                            color = if (isPm) ShedBlue else BackgroundGray
                        ) {
                            Text(
                                "PM",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = if (isPm) Color.White else TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onTimeSelected(hour, minute) },
                        colors = ButtonDefaults.buttonColors(containerColor = ShedBlue)
                    ) { Text("OK") }
                }
            }
        }
    }
}

// ── Utility ────────────────────────────────────────────

fun formatTime(hour: Int, minute: Int): String {
    val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    val amPm = if (hour < 12) "AM" else "PM"
    return String.format("%d:%02d %s", h, minute, amPm)
}

fun parseTimeString(time: String): Pair<Int, Int> {
    return try {
        val parts = time.split(":")
        Pair(parts[0].toInt(), parts[1].toInt())
    } catch (_: Exception) {
        Pair(8, 0)
    }
}

fun formatTime24(hour: Int, minute: Int): String = String.format("%02d:%02d", hour, minute)
