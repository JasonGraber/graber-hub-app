package com.graberhub.companion.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.graberhub.companion.data.models.Chore
import com.graberhub.companion.data.models.Kid
import com.graberhub.companion.data.models.KidConfig
import com.graberhub.companion.ui.components.*
import com.graberhub.companion.ui.theme.*
import com.graberhub.companion.viewmodel.MainViewModel

@Composable
fun ChoresScreen(viewModel: MainViewModel) {
    val kids by viewModel.kids.collectAsState()
    val kidsConfig by viewModel.kidsConfig.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedKidForAdd by remember { mutableIntStateOf(-1) }
    var editingChore by remember { mutableStateOf<Chore?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader("✅ Chores")
            IconButton(onClick = { viewModel.loadDashboard() }) {
                Icon(Icons.Default.Refresh, "Refresh", tint = ShedBlue)
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ShedBlue)
            }
        } else if (kids.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✅", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No kids/chores loaded", color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.loadDashboard() },
                        colors = ButtonDefaults.buttonColors(containerColor = ShedBlue)
                    ) {
                        Text("Refresh", color = Color.White)
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(kids, key = { it.id }) { kid ->
                    val config = kidsConfig.find { it.id == kid.id }
                    val kidColor = parseColor(config?.color ?: kid.color.ifEmpty { "#03a9f4" })

                    KidChoresCard(
                        kid = kid,
                        kidColor = kidColor,
                        onToggle = { choreId -> viewModel.toggleChore(choreId) },
                        onAdd = { selectedKidForAdd = kid.id; showAddDialog = true },
                        onEdit = { chore -> editingChore = chore },
                        onDelete = { choreId -> viewModel.deleteChore(choreId) }
                    )
                }
            }
        }
    }

    if (showAddDialog && selectedKidForAdd > 0) {
        ChoreDialog(
            kidName = kids.find { it.id == selectedKidForAdd }?.name ?: "",
            onSave = { title, emoji ->
                viewModel.addChore(selectedKidForAdd, title, emoji)
            },
            onDismiss = { showAddDialog = false; selectedKidForAdd = -1 }
        )
    }

    editingChore?.let { chore ->
        ChoreDialog(
            initialTitle = chore.title,
            initialEmoji = chore.emoji,
            kidName = kids.find { it.id == chore.kid_id }?.name ?: "",
            onSave = { title, emoji ->
                viewModel.editChore(chore.id, title, emoji)
            },
            onDismiss = { editingChore = null }
        )
    }
}

@Composable
private fun KidChoresCard(
    kid: Kid,
    kidColor: Color,
    onToggle: (Int) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Chore) -> Unit,
    onDelete: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Kid header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(kidColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            kid.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(kid.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)

                    if (kid.chores.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val done = kid.chores.count { it.done }
                        Text(
                            "$done/${kid.chores.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (done == kid.chores.size) CheckGreen else TextSecondary
                        )
                    }
                }
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, "Add chore", tint = kidColor)
                }
            }

            if (kid.chores.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                kid.chores.forEach { chore ->
                    ChoreItem(
                        chore = chore,
                        kidColor = kidColor,
                        onToggle = { onToggle(chore.id) },
                        onEdit = { onEdit(chore) },
                        onDelete = { onDelete(chore.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChoreItem(
    chore: Chore,
    kidColor: Color,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (chore.done) CheckGreen else BackgroundGray)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            if (chore.done) {
                Icon(Icons.Default.Check, "Done", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        if (chore.emoji.isNotEmpty()) {
            Text(chore.emoji, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            chore.title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (chore.done) TextHint else TextPrimary,
            textDecoration = if (chore.done) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, "Edit", tint = TextHint, modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, "Delete", tint = TextHint, modifier = Modifier.size(16.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Chore") },
            text = { Text("Remove \"${chore.title}\"?") },
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
fun ChoreDialog(
    initialTitle: String = "",
    initialEmoji: String = "",
    kidName: String = "",
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var emoji by remember { mutableStateOf(initialEmoji) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTitle.isEmpty()) "Add Chore for $kidName" else "Edit Chore") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Chore name") },
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title, emoji)
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
}
