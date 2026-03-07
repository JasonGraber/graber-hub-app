package com.graberhub.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.graberhub.companion.data.models.KidConfig
import com.graberhub.companion.ui.components.*
import com.graberhub.companion.ui.theme.*
import com.graberhub.companion.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val kidsConfig by viewModel.kidsConfig.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val verses by viewModel.verses.collectAsState()
    val photoSettings by viewModel.photoSettings.collectAsState()

    var serverUrl by remember { mutableStateOf(viewModel.prefs.serverUrl) }
    var showVerseDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadVerses() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SectionHeader("⚙️ Settings")

        // === SERVER SECTION ===
        SettingsSectionCard("Server") {
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("Server URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(
                    onClick = {
                        viewModel.updateServerUrl(serverUrl)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ShedBlue)
                ) {
                    Icon(Icons.Default.Save, "Save", tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save", color = Color.White)
                }
                OutlinedButton(onClick = { viewModel.testConnection() }) {
                    Icon(Icons.Default.Wifi, "Test")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test")
                }
            }
            connectionStatus?.let { status ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    status,
                    color = if (status.startsWith("✓")) CheckGreen else if (status.startsWith("✗")) ShedPink else TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // === DISPLAY MODE SECTION (NEW) ===
        SettingsSectionCard("Display Mode") {
            val modes = listOf(
                Triple("dashboard", "Dashboard", "Chores, schedule, and countdowns"),
                Triple("photos", "Photos", "Full-screen photo slideshow"),
                Triple("auto", "Auto", "Photos at night (8 PM–7 AM), dashboard during day")
            )
            modes.forEach { (mode, label, description) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.updateDisplayMode(mode) }
                        .background(if (photoSettings.mode == mode) ShedBlueLight else Color.Transparent)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = photoSettings.mode == mode,
                        onClick = { viewModel.updateDisplayMode(mode) },
                        colors = RadioButtonDefaults.colors(selectedColor = ShedBlue)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(label, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
                if (mode != "auto") Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Photo Album URL
            var albumUrl by remember(photoSettings.album_url) { mutableStateOf(photoSettings.album_url) }
            OutlinedTextField(
                value = albumUrl,
                onValueChange = { albumUrl = it },
                label = { Text("Google Photos Album") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("https://photos.google.com/...") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.updateAlbumUrl(albumUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = ShedBlue),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Save Album URL", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // === KIDS SECTION ===
        SettingsSectionCard("Kids") {
            kidsConfig.forEach { kid ->
                KidColorRow(kid = kid, onColorChange = { color ->
                    viewModel.updateKidColor(kid.id, color)
                })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // === VERSE OF THE DAY SECTION ===
        SettingsSectionCard("Verse of the Day") {
            if (verses.isEmpty()) {
                Text("No verses added yet", color = TextSecondary)
            } else {
                verses.forEach { verse ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = BackgroundGray)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "\"${verse.text}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Text(
                                "— ${verse.reference}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showVerseDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, "Add Verse")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Verse")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Version
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Graber Hub v1.1.0", style = MaterialTheme.typography.bodySmall, color = TextHint)
            }
        }
    }

    if (showVerseDialog) {
        VerseDialog(
            onSave = { text, reference ->
                viewModel.addVerse(text, reference)
            },
            onDismiss = { showVerseDialog = false }
        )
    }
}

@Composable
fun SettingsSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ShedBlue,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun KidColorRow(kid: KidConfig, onColorChange: (String) -> Unit) {
    var showColorPicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(parseColor(kid.color))
                .clickable { showColorPicker = true },
            contentAlignment = Alignment.Center
        ) {
            Text(
                kid.name.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(kid.name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { showColorPicker = true }) {
            Text("Change Color", style = MaterialTheme.typography.labelSmall)
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            currentColor = kid.color,
            onColorSelected = onColorChange,
            onDismiss = { showColorPicker = false }
        )
    }
}

@Composable
fun VerseDialog(
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Verse") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Verse text") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Reference (e.g., John 3:16)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank() && reference.isNotBlank()) {
                        onSave(text, reference)
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
}
