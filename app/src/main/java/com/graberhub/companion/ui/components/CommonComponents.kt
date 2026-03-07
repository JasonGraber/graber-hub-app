package com.graberhub.companion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.window.Dialog
import com.graberhub.companion.ui.theme.*
import java.util.Calendar

@Composable
fun SectionHeader(title: String, icon: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier.padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

@Composable
fun ClockTimePicker(
    hour: Int,
    minute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Select Time"
) {
    var selectedHour by remember { mutableIntStateOf(hour) }
    var selectedMinute by remember { mutableIntStateOf(minute) }
    var selectingHour by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardBackground,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Time display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    TimeBox(
                        value = String.format("%02d", selectedHour),
                        selected = selectingHour,
                        onClick = { selectingHour = true }
                    )
                    Text(":", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp))
                    TimeBox(
                        value = String.format("%02d", selectedMinute),
                        selected = !selectingHour,
                        onClick = { selectingHour = false }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Clock face
                if (selectingHour) {
                    ClockFaceGrid(
                        values = (0..23).toList(),
                        selected = selectedHour,
                        onSelect = {
                            selectedHour = it
                            selectingHour = false
                        },
                        columns = 6,
                        format = { String.format("%02d", it) }
                    )
                } else {
                    ClockFaceGrid(
                        values = (0..55 step 5).toList(),
                        selected = selectedMinute,
                        onSelect = { selectedMinute = it },
                        columns = 4,
                        format = { String.format("%02d", it) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onTimeSelected(selectedHour, selectedMinute); onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = ShedBlue)
                    ) {
                        Text("OK", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeBox(value: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) ShedBlueLight else BackgroundGray)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            value,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) ShedBlueDark else TextPrimary
        )
    }
}

@Composable
private fun ClockFaceGrid(
    values: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    columns: Int,
    format: (Int) -> String
) {
    Column {
        values.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { v ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (v == selected) ShedBlue else Color.Transparent
                            )
                            .clickable { onSelect(v) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            format(v),
                            fontSize = 14.sp,
                            fontWeight = if (v == selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (v == selected) Color.White else TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDatePicker(
    initialYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    initialMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    initialDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
    onDateSelected: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var year by remember { mutableIntStateOf(initialYear) }
    var month by remember { mutableIntStateOf(initialMonth) }
    var selectedDay by remember { mutableIntStateOf(initialDay) }

    val monthNames = listOf("January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardBackground,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header with month/year navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        if (month == 0) { month = 11; year-- } else month--
                    }) {
                        Icon(Icons.Default.ChevronLeft, "Previous", tint = ShedBlue)
                    }
                    Text(
                        "${monthNames[month]} $year",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = {
                        if (month == 11) { month = 0; year++ } else month++
                    }) {
                        Icon(Icons.Default.ChevronRight, "Next", tint = ShedBlue)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Day of week headers
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = TextSecondary,
                            modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Calendar grid
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val days = (1..daysInMonth).toList()
                val paddedDays = List(firstDayOfWeek) { 0 } + days

                paddedDays.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        week.forEach { day ->
                            if (day == 0) {
                                Spacer(modifier = Modifier.size(36.dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (day == selectedDay) ShedBlue else Color.Transparent)
                                        .clickable { selectedDay = day },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$day",
                                        fontSize = 14.sp,
                                        color = if (day == selectedDay) Color.White else TextPrimary,
                                        fontWeight = if (day == selectedDay) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                        // Pad remaining cells
                        repeat(7 - week.size) {
                            Spacer(modifier = Modifier.size(36.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onDateSelected(year, month, selectedDay)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ShedBlue)
                    ) {
                        Text("OK", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ColorPickerDialog(
    currentColor: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardBackground,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Choose Color",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Color grid (3 columns x 4 rows)
                PresetColors.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { (hex, name) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    onColorSelected(hex)
                                    onDismiss()
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(parseColor(hex))
                                        .then(
                                            if (hex == currentColor)
                                                Modifier.border(3.dp, ShedBlueDark, CircleShape)
                                            else Modifier
                                        )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(name, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.Gray
    }
}
