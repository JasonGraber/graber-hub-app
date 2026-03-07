package com.graberhub.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.graberhub.companion.data.models.Verse
import com.graberhub.companion.ui.components.ColorPickerDialog
import com.graberhub.companion.ui.theme.*
import com.graberhub.companion.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val kidsConfig by viewModel.kidsConfig.collectAsStateWithLifecycle()
    val verses by viewModel.verses.collectAsStateWithLifecycle()
    var serverUrl by remember { mutableStateOf(viewModel.prefs.serverUrl) }
    var showVerseDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) { viewModel.loadVerses() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "⚙️ Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        // Server URL section
        item {
            SettingsCard(title = "🌐 Server Connection") {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("Server URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Connection status
                    connectionStatus?.let { status ->
                        Text(
                            status,
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                status.contains("✓") -> CheckGreen
                                status.contains("✗") -> ShedPink
                                else -> TextSecondary
                            }
                        )
                    }

                    Row {
                        OutlinedButton(
                            onClick = {
                                viewModel.updateServerUrl(serverUrl)
                                viewModel.testConnection()
                            }
                        ) {
                            Text("Save & Test")
                        }
                    }
                }
            }
        }

        // Kid Colors section
        item {
            SettingsCard(title = "🎨 Kid Colors") {
                kidsConfig.forEach { kidConfig ->
                    val color = try {
                        Color(android.graphics.Color.parseColor(kidConfig.color))
                    } catch (_: Exception) { ShedBlue }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { showColorPicker = kidConfig.id },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                kidConfig.name.first().uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            kidConfig.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            kidConfig.color,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Verses section
        item {
            SettingsCard(title = "📖 Bible Verses") {
                if (verses.isEmpty()) {
                    Text(
                        "No verses yet. Add one to display on the dashboard.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextHint,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    verses.forEach { verse ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = BackgroundGray
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "\"${verse.text}\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "— ${verse.reference}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ShedBlue,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showVerseDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Verse")
                }
            }
        }

        // Version
        item {
            SettingsCard(title = "ℹ️ About") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Version", color = TextSecondary)
                    Text("1.0.9", color = TextPrimary, fontWeight = FontWeight.Medium)
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }

    // Color picker dialog
    showColorPicker?.let { kidId ->
        val currentColor = kidsConfig.find { it.id == kidId }?.color ?: "#03a9f4"
        ColorPickerDialog(
            selectedColor = currentColor,
            onColorSelected = { color ->
                viewModel.updateKidColor(kidId, color)
                showColorPicker = null
            },
            onDismiss = { showColorPicker = null }
        )
    }

    // Add verse dialog
    if (showVerseDialog) {
        AddVerseDialog(
            onSave = { text, reference ->
                viewModel.addVerse(text, reference)
                showVerseDialog = false
            },
            onDismiss = { showVerseDialog = false }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = CardBackground
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun AddVerseDialog(
    onSave: (text: String, reference: String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardBackground
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Add Bible Verse",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Verse Text") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Reference (e.g. John 3:16)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (text.isNotBlank() && reference.isNotBlank()) {
                                onSave(text, reference)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ShedBlue),
                        enabled = text.isNotBlank() && reference.isNotBlank()
                    ) { Text("Save") }
                }
            }
        }
    }
}
