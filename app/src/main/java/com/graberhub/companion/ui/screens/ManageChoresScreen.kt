package com.graberhub.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.navigation.NavController
import com.graberhub.companion.data.models.*
import com.graberhub.companion.ui.components.EmojiPickerDialog
import com.graberhub.companion.ui.theme.*
import com.graberhub.companion.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ManageChoresScreen(viewModel: MainViewModel, navController: NavController) {
    val templates by viewModel.choreTemplates.collectAsStateWithLifecycle()
    val kids by viewModel.kids.collectAsStateWithLifecycle()
    val kidsConfig by viewModel.kidsConfig.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<ChoreTemplate?>(null) }
    var confirmDeleteId by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                "📋 Chore Templates",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Template", tint = ShedBlue)
            }
        }

        if (templates.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No recurring chores yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap + to create a template",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextHint
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
            ) {
                // Group by kid
                val grouped = templates.groupBy { it.kid_id }
                grouped.forEach { (kidId, kidTemplates) ->
                    val kidConfig = kidsConfig.find { it.id == kidId }
                    val kid = kids.find { it.id == kidId }
                    val kidName = kidConfig?.name ?: kid?.name ?: "Kid $kidId"
                    val kidColorHex = kidConfig?.color ?: kid?.color ?: "#03A9F4"
                    val kidColor = try {
                        Color(android.graphics.Color.parseColor(kidColorHex))
                    } catch (_: Exception) { ShedBlue }

                    item(key = "header_$kidId") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(kidColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    kidName.first().uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                kidName,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    items(kidTemplates, key = { it.id }) { template ->
                        TemplateCard(
                            template = template,
                            kidColor = kidColor,
                            onEdit = { editingTemplate = template },
                            onDelete = { confirmDeleteId = template.id }
                        )
                    }
                }
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        TemplateDialog(
            template = null,
            kids = kids.map { kid ->
                val cfg = kidsConfig.find { it.id == kid.id }
                KidConfig(kid.id, cfg?.name ?: kid.name, cfg?.color ?: kid.color)
            },
            today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
            onSave = { req ->
                viewModel.createChoreTemplate(req)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // Edit dialog
    editingTemplate?.let { template ->
        TemplateDialog(
            template = template,
            kids = kids.map { kid ->
                val cfg = kidsConfig.find { it.id == kid.id }
                KidConfig(kid.id, cfg?.name ?: kid.name, cfg?.color ?: kid.color)
            },
            today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
            onSave = { req ->
                viewModel.updateChoreTemplate(
                    template.id,
                    UpdateChoreTemplateRequest(
                        title = req.title,
                        emoji = req.emoji,
                        repeat = req.repeat,
                        days_of_week = req.days_of_week,
                        week_interval = req.week_interval,
                        every_other_week = req.every_other_week,
                        days_of_month = req.days_of_month,
                        start_date = req.start_date,
                        active = req.active
                    )
                )
                editingTemplate = null
            },
            onDismiss = { editingTemplate = null }
        )
    }

    // Delete confirmation
    confirmDeleteId?.let { templateId ->
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text("Delete Template") },
            text = { Text("Are you sure you want to delete this recurring chore template?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteChoreTemplate(templateId)
                    confirmDeleteId = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteId = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TemplateCard(
    template: ChoreTemplate,
    kidColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = CardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Active indicator dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (template.active) CheckGreen else TextHint)
            )
            Spacer(Modifier.width(10.dp))

            // Emoji
            if (template.emoji.isNotEmpty()) {
                Text(template.emoji, fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
            }

            // Title + repeat info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    template.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    repeatSummary(template),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // Actions
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = TextHint,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = TextHint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun repeatSummary(template: ChoreTemplate): String {
    return when (template.repeat) {
        "daily" -> "Every day"
        "weekly" -> {
            val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            val days = template.days_of_week.sorted().mapNotNull { dayNames.getOrNull(it) }
            val interval = when {
                template.week_interval == 4 -> 4
                template.week_interval == 2 || template.every_other_week -> 2
                else -> 1
            }
            val prefix = when (interval) {
                4 -> "Every 4 weeks"
                2 -> "Every 2 weeks"
                else -> "Weekly"
            }
            if (days.isEmpty()) prefix else "$prefix: ${days.joinToString(", ")}"
        }
        "monthly" -> {
            val days = template.days_of_month.sorted()
            if (days.isEmpty()) "Monthly"
            else "Monthly on day ${days.joinToString(", ")}"
        }
        else -> "One-time"
    }
}

// ── Template Dialog ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateDialog(
    template: ChoreTemplate?,
    kids: List<KidConfig>,
    today: String,
    onSave: (CreateChoreTemplateRequest) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(template?.title ?: "") }
    var emoji by remember { mutableStateOf(template?.emoji ?: "") }
    var repeat by remember { mutableStateOf(template?.repeat ?: "daily") }
    var selectedKidId by remember { mutableStateOf(template?.kid_id ?: kids.firstOrNull()?.id ?: 0) }
    var daysOfWeek by remember { mutableStateOf(template?.days_of_week?.toSet() ?: emptySet()) }
    var weekInterval by remember {
        mutableIntStateOf(
            when {
                template?.week_interval == 4 -> 4
                template?.week_interval == 2 || template?.every_other_week == true -> 2
                else -> 1
            }
        )
    }
    var daysOfMonth by remember { mutableStateOf(template?.days_of_month?.toSet() ?: emptySet()) }
    var startDate by remember { mutableStateOf(template?.start_date ?: today) }
    var active by remember { mutableStateOf(template?.active ?: true) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = remember(startDate) {
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(startDate)?.time
            } catch (_: Exception) { null }
        }
    )

    val repeatOptions = listOf("daily", "weekly", "monthly", "none")
    val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
    val dayFullNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        if (template != null) "Edit Template" else "New Recurring Chore",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                }

                // Kid selector (only for new)
                if (template == null && kids.isNotEmpty()) {
                    item {
                        Text("For kid:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            kids.forEach { kid ->
                                val selected = kid.id == selectedKidId
                                val kidColor = try {
                                    Color(android.graphics.Color.parseColor(kid.color))
                                } catch (_: Exception) { ShedBlue }
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedKidId = kid.id },
                                    label = { Text(kid.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = kidColor.copy(alpha = 0.2f)
                                    )
                                )
                            }
                        }
                    }
                }

                // Title
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Chore Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Emoji
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BackgroundGray)
                            .clickable { showEmojiPicker = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (emoji.isNotEmpty()) "$emoji  Tap to change" else "Choose Emoji (optional)",
                            fontSize = if (emoji.isNotEmpty()) 16.sp else 14.sp,
                            color = if (emoji.isNotEmpty()) TextPrimary else TextHint
                        )
                    }
                }

                // Repeat picker
                item {
                    Text("Repeat:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeatOptions.forEach { opt ->
                            FilterChip(
                                selected = repeat == opt,
                                onClick = { repeat = opt },
                                modifier = Modifier.widthIn(min = 72.dp),
                                label = {
                                    Text(
                                        opt.replaceFirstChar { it.uppercase() },
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }
                }

                // Weekly: day picker
                if (repeat == "weekly") {
                    item {
                        Text("Days of week:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            dayLabels.forEachIndexed { index, label ->
                                val selected = index in daysOfWeek
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) ShedBlue else BackgroundGray)
                                        .clickable {
                                            daysOfWeek = if (selected) daysOfWeek - index else daysOfWeek + index
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        color = if (selected) Color.White else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Frequency:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1 to "Every week", 2 to "Every 2 weeks", 4 to "Every 4 weeks").forEach { (interval, label) ->
                                FilterChip(
                                    selected = weekInterval == interval,
                                    onClick = { weekInterval = interval },
                                    label = { Text(label, maxLines = 1) }
                                )
                            }
                        }
                    }
                }

                // Monthly: day-of-month picker
                if (repeat == "monthly") {
                    item {
                        Text("Days of month:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Spacer(Modifier.height(6.dp))
                        // 7-column grid of days 1-31
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..31).chunked(7).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    row.forEach { day ->
                                        val selected = day in daysOfMonth
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(if (selected) ShedBlue else BackgroundGray)
                                                .clickable {
                                                    daysOfMonth = if (selected) daysOfMonth - day else daysOfMonth + day
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "$day",
                                                color = if (selected) Color.White else TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Start date
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = {},
                            label = { Text("Start Date") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            singleLine = true
                        )
                        // Transparent overlay captures the tap
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDatePicker = true }
                        )
                    }
                }

                // Active toggle
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }

                // Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    onSave(
                                        CreateChoreTemplateRequest(
                                            kid_id = selectedKidId,
                                            title = title.trim(),
                                            emoji = emoji,
                                            repeat = repeat,
                                            days_of_week = daysOfWeek.sorted(),
                                            week_interval = weekInterval,
                                            every_other_week = weekInterval == 2,
                                            days_of_month = daysOfMonth.sorted(),
                                            start_date = startDate.ifBlank { null },
                                            active = active
                                        )
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ShedBlue),
                            enabled = title.isNotBlank()
                        ) { Text("Save") }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        startDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }.format(Date(millis))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEmojiPicker) {
        EmojiPickerDialog(
            selectedEmoji = emoji,
            onEmojiSelected = { emoji = it },
            onDismiss = { showEmojiPicker = false }
        )
    }
}
