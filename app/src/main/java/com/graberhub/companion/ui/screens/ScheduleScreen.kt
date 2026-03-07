package com.graberhub.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.graberhub.companion.data.models.ScheduleBlock
import com.graberhub.companion.ui.components.*
import com.graberhub.companion.ui.theme.*
import com.graberhub.companion.viewmodel.MainViewModel

@Composable
fun ScheduleScreen(viewModel: MainViewModel) {
    val schedule by viewModel.schedule.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingBlock by remember { mutableStateOf<ScheduleBlock?>(null) }
    var showTemplateConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader("📅 Today's Schedule")
            Row {
                IconButton(onClick = { showTemplateConfirm = true }) {
                    Icon(Icons.Default.Save, "Save Template", tint = ShedBlue)
                }
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, "Add", tint = ShedBlue)
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ShedBlue)
            }
        } else if (schedule.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📅", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No schedule blocks yet", color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ShedBlue)
                    ) {
                        Text("Add First Block", color = Color.White)
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(schedule, key = { it.id }) { block ->
                    ScheduleBlockCard(
                        block = block,
                        onEdit = { editingBlock = block },
                        onDelete = { viewModel.deleteScheduleBlock(block.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        ScheduleBlockDialog(
            onSave = { timeStart, timeEnd, title, emoji ->
                viewModel.addScheduleBlock(timeStart, timeEnd, title, emoji)
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editingBlock?.let { block ->
        ScheduleBlockDialog(
            initialTitle = block.title,
            initialEmoji = block.emoji,
            initialTimeStart = block.time_start,
            initialTimeEnd = block.time_end,
            onSave = { timeStart, timeEnd, title, emoji ->
                viewModel.editScheduleBlock(block.id, title, emoji, timeStart, timeEnd)
            },
            onDismiss = { editingBlock = null }
        )
    }

    if (showTemplateConfirm) {
        AlertDialog(
            onDismissRequest = { showTemplateConfirm = false },
            title = { Text("Save as Template") },
            text = { Text("Save today's schedule as the default template for future days?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.applyTemplate(); showTemplateConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ShedBlue)
                ) { Text("Save", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showTemplateConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ScheduleBlockCard(
    block: ScheduleBlock,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time column
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ShedBlueLight)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(block.time_start, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ShedBlueDark)
                Text("–", fontSize = 12.sp, color = TextSecondary)
                Text(block.time_end, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ShedBlueDark)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (block.emoji.isNotEmpty()) {
                        Text(block.emoji, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(block.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = TextPrimary)
                }
            }

            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, "Delete", tint = TextHint, modifier = Modifier.size(20.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Block") },
            text = { Text("Remove \"${block.title}\" from today's schedule?") },
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
fun ScheduleBlockDialog(
    initialTitle: String = "",
    initialEmoji: String = "",
    initialTimeStart: String = "08:00",
    initialTimeEnd: String = "09:00",
    onSave: (timeStart: String, timeEnd: String, title: String, emoji: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var emoji by remember { mutableStateOf(initialEmoji) }
    var timeStart by remember { mutableStateOf(initialTimeStart) }
    var timeEnd by remember { mutableStateOf(initialTimeEnd) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTitle.isEmpty()) "Add Schedule Block" else "Edit Schedule Block") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Activity") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(onClick = { showStartTimePicker = true }) {
                        Text("Start: $timeStart")
                    }
                    OutlinedButton(onClick = { showEndTimePicker = true }) {
                        Text("End: $timeEnd")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Emoji: ", color = TextSecondary)
                    Text(emoji.ifEmpty { "None" }, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = { showEmojiPicker = true }) {
                        Text("Choose")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(timeStart, timeEnd, title, emoji)
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

    if (showStartTimePicker) {
        val parts = timeStart.split(":")
        ClockTimePicker(
            hour = parts.getOrElse(0) { "8" }.toIntOrNull() ?: 8,
            minute = parts.getOrElse(1) { "0" }.toIntOrNull() ?: 0,
            onTimeSelected = { h, m -> timeStart = String.format("%02d:%02d", h, m) },
            onDismiss = { showStartTimePicker = false },
            title = "Start Time"
        )
    }

    if (showEndTimePicker) {
        val parts = timeEnd.split(":")
        ClockTimePicker(
            hour = parts.getOrElse(0) { "9" }.toIntOrNull() ?: 9,
            minute = parts.getOrElse(1) { "0" }.toIntOrNull() ?: 0,
            onTimeSelected = { h, m -> timeEnd = String.format("%02d:%02d", h, m) },
            onDismiss = { showEndTimePicker = false },
            title = "End Time"
        )
    }
}
