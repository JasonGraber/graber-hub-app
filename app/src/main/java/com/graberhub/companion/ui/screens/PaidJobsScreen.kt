package com.graberhub.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.graberhub.companion.data.models.Kid
import com.graberhub.companion.data.models.PaidJob
import com.graberhub.companion.ui.theme.*
import com.graberhub.companion.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PaidJobsScreen(viewModel: MainViewModel) {
    val jobs by viewModel.paidJobs.collectAsStateWithLifecycle()
    val kids by viewModel.kids.collectAsStateWithLifecycle()
    val mutating by viewModel.paidJobMutationInProgress.collectAsStateWithLifecycle()
    var editingJob by remember { mutableStateOf<PaidJob?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var claimJob by remember { mutableStateOf<PaidJob?>(null) }
    var deleteJob by remember { mutableStateOf<PaidJob?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(8.dp))
            Text("$", color = CheckGreen, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(4.dp))
            Text(
                "Paid Jobs",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showCreate = true }, enabled = !mutating) {
                Icon(Icons.Default.Add, contentDescription = "Post paid job", tint = ShedBlue)
            }
        }

        if (mutating) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = CheckGreen)
        }

        if (jobs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$", color = CheckGreen, fontSize = 52.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(12.dp))
                    Text("No paid jobs posted", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                    Text("Tap + to post the first one", style = MaterialTheme.typography.bodyMedium, color = TextHint)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
            ) {
                items(jobs, key = { it.id }) { job ->
                    PaidJobCard(
                        job = job,
                        enabled = !mutating,
                        onClaim = { claimJob = job },
                        onComplete = { viewModel.completePaidJob(job.id, job.status != "completed") },
                        onEdit = { editingJob = job },
                        onDelete = { deleteJob = job }
                    )
                }
            }
        }
    }

    if (showCreate) {
        PaidJobDialog(
            job = null,
            onSave = { title, description, amount, dueDate ->
                viewModel.addPaidJob(title, description, amount, dueDate)
                showCreate = false
            },
            onDismiss = { showCreate = false }
        )
    }

    editingJob?.let { job ->
        PaidJobDialog(
            job = job,
            onSave = { title, description, amount, dueDate ->
                viewModel.editPaidJob(job.id, title, description, amount, dueDate)
                editingJob = null
            },
            onDismiss = { editingJob = null }
        )
    }

    claimJob?.let { job ->
        ClaimPaidJobDialog(
            job = job,
            kids = kids,
            onClaim = { kidId ->
                viewModel.claimPaidJob(job.id, kidId)
                claimJob = null
            },
            onDismiss = { claimJob = null }
        )
    }

    deleteJob?.let { job ->
        AlertDialog(
            onDismissRequest = { deleteJob = null },
            title = { Text("Delete paid job?") },
            text = { Text("${job.title} will be removed for everyone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePaidJob(job.id)
                    deleteJob = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteJob = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PaidJobCard(
    job: PaidJob,
    enabled: Boolean,
    onClaim: () -> Unit,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (job.status) {
        "claimed" -> Color(0xFFD97706)
        "completed" -> ShedBlue
        else -> CheckGreen
    }
    val statusText = when (job.status) {
        "claimed" -> "Claimed by ${job.claimed_by_name ?: "a child"}"
        "completed" -> "Completed"
        else -> if (job.claim_reopened) "Available again" else "Available to claim"
    }

    Surface(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = CardBackground
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CheckGreenLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "\$${job.amount}",
                        color = Color(0xFF15803D),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(job.title, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                    if (job.description.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(job.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                        Spacer(Modifier.width(6.dp))
                        Text(statusText, style = MaterialTheme.typography.labelLarge, color = statusColor, fontWeight = FontWeight.SemiBold)
                    }
                    Text("Due ${formatPaidJobDate(job.due_date)}", style = MaterialTheme.typography.bodySmall, color = TextHint)
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                when (job.status) {
                    "available" -> Button(
                        onClick = onClaim,
                        enabled = enabled,
                        colors = ButtonDefaults.buttonColors(containerColor = CheckGreen)
                    ) { Text("Claim job") }
                    "claimed" -> Button(
                        onClick = onComplete,
                        enabled = enabled,
                        colors = ButtonDefaults.buttonColors(containerColor = ShedBlue)
                    ) { Text("Mark complete") }
                    else -> OutlinedButton(onClick = onComplete, enabled = enabled) { Text("Reopen") }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit, enabled = enabled) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit ${job.title}", tint = TextHint)
                }
                IconButton(onClick = onDelete, enabled = enabled) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete ${job.title}", tint = TextHint)
                }
            }
        }
    }
}

@Composable
private fun ClaimPaidJobDialog(job: PaidJob, kids: List<Kid>, onClaim: (Int) -> Unit, onDismiss: () -> Unit) {
    var selectedKidId by remember { mutableStateOf<Int?>(null) }
    val eligibleKids = kids.filter { kid ->
        kid.chores.all { it.done } && (!job.claim_reopened || job.last_claimed_by_kid_id != kid.id)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Who is claiming ${job.title}?") },
        text = {
            Column {
            Text("Only children with all of today's chores finished can claim this \$${job.amount} job.", color = TextSecondary)
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    kids.chunked(2).forEach { rowKids ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowKids.forEach { kid ->
                                val eligible = eligibleKids.any { it.id == kid.id }
                                FilterChip(
                                    selected = selectedKidId == kid.id,
                                    onClick = { if (eligible) selectedKidId = kid.id },
                                    enabled = eligible,
                                    label = { Text(kid.name) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowKids.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                if (eligibleKids.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("No one is eligible yet.", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedKidId?.let(onClaim) },
                enabled = selectedKidId != null,
                colors = ButtonDefaults.buttonColors(containerColor = CheckGreen)
            ) { Text("Claim") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaidJobDialog(
    job: PaidJob?,
    onSave: (String, String, Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultDueDate = remember {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }.time
    }
    var title by remember { mutableStateOf(job?.title ?: "") }
    var description by remember { mutableStateOf(job?.description ?: "") }
    var amount by remember { mutableStateOf(job?.amount?.toString() ?: "5") }
    var dueDate by remember {
        mutableStateOf(job?.due_date ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(defaultDueDate))
    }
    var showDatePicker by remember { mutableStateOf(false) }
    val amountValue = amount.toIntOrNull()
    val valid = title.isNotBlank() && description.isNotBlank() && description.length <= 180 && amountValue != null && amountValue in 1..10

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = CardBackground, modifier = Modifier.fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        if (job == null) "Post a Paid Job" else "Edit Paid Job",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 80) title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= 180) description = it },
                        label = { Text("Short description") },
                        supportingText = { Text("${description.length}/180") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }
                item {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { value -> amount = value.filter(Char::isDigit).take(2) },
                        label = { Text("Pays ($1–$10)") },
                        leadingIcon = { Text("$", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = amountValue == null || amountValue !in 1..10,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = formatPaidJobDate(dueDate),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Due date") },
                        trailingIcon = {
                            TextButton(onClick = { showDatePicker = true }) { Text("Change") }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { onSave(title, description, amountValue ?: 0, dueDate) },
                            enabled = valid,
                            colors = ButtonDefaults.buttonColors(containerColor = CheckGreen)
                        ) { Text(if (job == null) "Post job" else "Save") }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = parsePaidJobDate(dueDate),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val todayStart = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    return utcTimeMillis >= todayStart
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        dueDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }.format(Date(millis))
                    }
                    showDatePicker = false
                }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = pickerState) }
    }
}

private fun parsePaidJobDate(value: String): Long? = try {
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(value)?.time
} catch (_: Exception) { null }

private fun formatPaidJobDate(value: String): String = try {
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    val parsed = parser.parse(value) ?: return value
    SimpleDateFormat("MMM d, yyyy", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(parsed)
} catch (_: Exception) { value }
