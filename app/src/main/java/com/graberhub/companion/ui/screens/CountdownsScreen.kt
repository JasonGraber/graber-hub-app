package com.graberhub.companion.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.graberhub.companion.data.models.Countdown
import com.graberhub.companion.ui.components.*
import com.graberhub.companion.ui.theme.*
import com.graberhub.companion.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun CountdownsScreen(viewModel: MainViewModel) {
    val countdowns by viewModel.countdowns.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadCountdowns() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader("🎯 Countdowns")
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add", tint = ShedBlue)
            }
        }

        if (countdowns.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎯", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No countdowns yet", color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ShedBlue)
                    ) {
                        Text("Add Countdown", color = Color.White)
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(countdowns, key = { it.id }) { countdown ->
                    CountdownCard(
                        countdown = countdown,
                        onDelete = { viewModel.deleteCountdown(countdown.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        CountdownDialog(
            onSave = { label, emoji, targetDate ->
                viewModel.addCountdown(label, emoji, targetDate)
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun CountdownCard(countdown: Countdown, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val daysLeft = calculateDaysLeft(countdown.target_date)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji
            if (countdown.emoji.isNotEmpty()) {
                Text(countdown.emoji, fontSize = 36.sp)
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    countdown.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    formatDate(countdown.target_date),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${daysLeft}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (daysLeft <= 0) ShedPink else ShedBlue
                )
                Text(
                    if (daysLeft == 1L) "day" else "days",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, "Delete", tint = TextHint, modifier = Modifier.size(20.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Countdown") },
            text = { Text("Remove \"${countdown.label}\"?") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ShedPink)
                ) { Text("Delete", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun CountdownDialog(
    onSave: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🎯") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var targetDate by remember { mutableStateOf("") }

    val cal = Calendar.getInstance()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Countdown") },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Event name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Emoji: ", color = TextSecondary)
                    Text(emoji.ifEmpty { "None" }, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = { showEmojiPicker = true }) {
                        Text("Choose")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarMonth, "Date")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (targetDate.isEmpty()) "Select Date" else targetDate)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (label.isNotBlank() && targetDate.isNotBlank()) {
                        onSave(label, emoji, targetDate)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ShedBlue)
            ) { Text("Save", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )

    if (showEmojiPicker) {
        EmojiPickerDialog(
            selected = emoji,
            onSelect = { emoji = it },
            onDismiss = { showEmojiPicker = false }
        )
    }

    if (showDatePicker) {
        CalendarDatePicker(
            onDateSelected = { year, month, day ->
                targetDate = String.format("%04d-%02d-%02d", year, month + 1, day)
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

private fun calculateDaysLeft(dateStr: String): Long {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val target = sdf.parse(dateStr) ?: return 0
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        val diff = target.time - now.time
        TimeUnit.MILLISECONDS.toDays(diff).coerceAtLeast(0)
    } catch (e: Exception) {
        0
    }
}

private fun formatDate(dateStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = sdf.parse(dateStr) ?: return dateStr
        SimpleDateFormat("MMMM d, yyyy", Locale.US).format(date)
    } catch (e: Exception) {
        dateStr
    }
}
