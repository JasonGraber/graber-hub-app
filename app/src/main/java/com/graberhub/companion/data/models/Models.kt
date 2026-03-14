package com.graberhub.companion.data.models

import com.google.gson.annotations.SerializedName

data class DashboardResponse(
    val kids: List<Kid>,
    val schedule: List<ScheduleBlock>,
    val countdowns: List<Countdown>,
    val verse: Verse?,
    val date: String,
    val data_version: Int? = null
)

data class Kid(
    val id: Int,
    val name: String,
    val color: String,
    val chores: List<Chore> = emptyList()
)

data class Chore(
    val id: Int,
    val kid_id: Int,
    val title: String,
    val done: Boolean,
    val date: String? = null,
    val emoji: String = ""
)

data class ScheduleBlock(
    val id: Int,
    val time_start: String,
    val time_end: String,
    val title: String,
    val emoji: String,
    val date: String? = null,
    val sort: Int = 0
)

data class Countdown(
    val id: Int,
    val label: String,
    val emoji: String,
    val target_date: String
)

data class Verse(
    val id: Int,
    val text: String,
    @SerializedName("ref") val reference: String
)

data class TimerStatus(
    val active: Boolean,
    val end_time: String? = null,
    val label: String? = null
)

data class KidConfig(
    val id: Int,
    val name: String,
    val color: String
)

data class ApiResponse(
    val success: Boolean,
    val message: String? = null,
    val id: Int? = null
)

// Request models
data class CreateChoreRequest(
    val kid_id: Int,
    val title: String,
    val date: String? = null,
    val emoji: String = ""
)

data class UpdateChoreRequest(
    val title: String,
    val emoji: String = ""
)

data class CreateScheduleRequest(
    val time_start: String,
    val time_end: String,
    val title: String,
    val emoji: String,
    val date: String? = null,
    val sort: Int? = null
)

data class UpdateScheduleRequest(
    val title: String,
    val emoji: String,
    val time_start: String,
    val time_end: String
)

data class TemplateRequest(
    val blocks: List<CreateScheduleRequest>,
    val date: String? = null
)

data class CreateCountdownRequest(
    val label: String,
    val emoji: String,
    val target_date: String
)

data class CreateTimerRequest(
    val minutes: Int,
    val label: String = ""
)

data class CreateVerseRequest(
    val text: String,
    val reference: String
)

data class UpdateVerseRequest(
    val text: String,
    val reference: String
)

data class UpdateKidColorRequest(
    val color: String
)

// Photo Settings
data class AutoSchedule(
    val photos_start: String? = "20:00",
    val photos_end: String? = "07:00"
)

data class PhotoSettings(
    val mode: String? = "dashboard",
    val album_url: String? = null,
    val transition_seconds: Int? = 10,
    val auto_schedule: AutoSchedule? = null,
    val show_clock: Boolean? = true,
    val show_weather: Boolean? = true,
    val show_date: Boolean? = true,
    val ken_burns: Boolean? = true
)

data class PhotoSettingsRequest(
    val mode: String? = null,
    val album_url: String? = null,
    val photo_source: String? = null
)

// ── Photo Upload ───────────────────────────────────────────

data class UploadedPhoto(
    val filename: String,
    val name: String = "",
    val url: String,
    val size: Long = 0L,
    val modified: String? = null,
    val uploaded: String? = null
)

// ── Schedule Templates (local + custom) ───────────────────

data class ScheduleTemplate(
    val name: String,
    val emoji: String,
    val blocks: List<CreateScheduleRequest>,
    val isCustom: Boolean = false
)

// ── Chore Templates ────────────────────────────────────────

data class ChoreTemplate(
    val id: Int,
    val kid_id: Int,
    val title: String,
    val emoji: String = "",
    val repeat: String = "none",          // "daily" | "weekly" | "monthly" | "none"
    val days_of_week: List<Int> = emptyList(),  // 0=Sun, 6=Sat
    val every_other_week: Boolean = false,
    val days_of_month: List<Int> = emptyList(), // 1-31
    val start_date: String? = null,
    val active: Boolean = true,
    val week_parity: Int? = null
)

data class CreateChoreTemplateRequest(
    val kid_id: Int,
    val title: String,
    val emoji: String = "",
    val repeat: String = "none",
    val days_of_week: List<Int> = emptyList(),
    val every_other_week: Boolean = false,
    val days_of_month: List<Int> = emptyList(),
    val start_date: String? = null,
    val active: Boolean = true
)

data class UpdateChoreTemplateRequest(
    val title: String,
    val emoji: String = "",
    val repeat: String = "none",
    val days_of_week: List<Int> = emptyList(),
    val every_other_week: Boolean = false,
    val days_of_month: List<Int> = emptyList(),
    val start_date: String? = null,
    val active: Boolean = true
)
