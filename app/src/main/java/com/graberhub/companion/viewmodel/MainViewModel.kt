package com.graberhub.companion.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.graberhub.companion.alarm.AlarmService
import com.graberhub.companion.data.models.*
import com.graberhub.companion.data.prefs.PrefsManager
import com.graberhub.companion.network.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val prefs = PrefsManager(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _kids = MutableStateFlow<List<Kid>>(emptyList())
    val kids: StateFlow<List<Kid>> = _kids.asStateFlow()

    private val _schedule = MutableStateFlow<List<ScheduleBlock>>(emptyList())
    val schedule: StateFlow<List<ScheduleBlock>> = _schedule.asStateFlow()

    private val _countdowns = MutableStateFlow<List<Countdown>>(emptyList())
    val countdowns: StateFlow<List<Countdown>> = _countdowns.asStateFlow()

    private val _verse = MutableStateFlow<Verse?>(null)
    val verse: StateFlow<Verse?> = _verse.asStateFlow()

    private val _verses = MutableStateFlow<List<Verse>>(emptyList())
    val verses: StateFlow<List<Verse>> = _verses.asStateFlow()

    private val _connectionStatus = MutableStateFlow<String?>(null)
    val connectionStatus: StateFlow<String?> = _connectionStatus.asStateFlow()

    private val _kidsConfig = MutableStateFlow(prefs.kidsConfig)
    val kidsConfig: StateFlow<List<KidConfig>> = _kidsConfig.asStateFlow()

    private val _timerStatus = MutableStateFlow<TimerStatus?>(null)
    val timerStatus: StateFlow<TimerStatus?> = _timerStatus.asStateFlow()

    private val _timerSecondsLeft = MutableStateFlow(0L)
    val timerSecondsLeft: StateFlow<Long> = _timerSecondsLeft.asStateFlow()

    private val _timerFinished = MutableStateFlow(false)
    val timerFinished: StateFlow<Boolean> = _timerFinished.asStateFlow()

    private val _alarmPlaying = MutableStateFlow(false)
    val alarmPlaying: StateFlow<Boolean> = _alarmPlaying.asStateFlow()

    private val _photoSettings = MutableStateFlow(PhotoSettings())
    val photoSettings: StateFlow<PhotoSettings> = _photoSettings.asStateFlow()

    private var timerTickJob: Job? = null
    private var alarmAutoStopJob: Job? = null

    private val alarmStopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            _alarmPlaying.value = false
            alarmAutoStopJob?.cancel()
        }
    }

    init {
        initRetrofit()
        loadDashboard()
        loadTimer()
        loadPhotoSettings()
        val filter = IntentFilter(AlarmService.ACTION_STOP)
        if (Build.VERSION.SDK_INT >= 33) {
            getApplication<Application>().registerReceiver(alarmStopReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            getApplication<Application>().registerReceiver(alarmStopReceiver, filter)
        }
    }

    override fun onCleared() {
        super.onCleared()
        runCatching { getApplication<Application>().unregisterReceiver(alarmStopReceiver) }
    }

    private fun initRetrofit() {
        RetrofitClient.init(prefs.serverUrl)
    }

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun loadDashboard() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val response = RetrofitClient.getApiService().getDashboard()
                if (response.isSuccessful) {
                    response.body()?.let { dashboard ->
                        _kids.value = dashboard.kids
                        _schedule.value = dashboard.schedule.sortedBy { it.time_start }
                        _countdowns.value = dashboard.countdowns
                        _verse.value = dashboard.verse
                    }
                } else {
                    _error.value = "Server error: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Connection failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleChore(choreId: Int) {
        viewModelScope.launch {
            try {
                RetrofitClient.getApiService().toggleChore(choreId, today())
                loadDashboard()
            } catch (e: Exception) {
                _error.value = "Failed to update chore"
            }
        }
    }

    fun addChore(kidId: Int, title: String, emoji: String = "") {
        viewModelScope.launch {
            try {
                RetrofitClient.getApiService().createChore(CreateChoreRequest(kidId, title, today(), emoji))
                loadDashboard()
            } catch (e: Exception) {
                _error.value = "Failed to add chore"
            }
        }
    }

    fun editChore(choreId: Int, title: String, emoji: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.getApiService().updateChore(choreId, UpdateChoreRequest(title, emoji))
                loadDashboard()
            } catch (e: Exception) {
                _error.value = "Failed to edit chore"
            }
        }
    }

    fun deleteChore(choreId: Int) {
        viewModelScope.launch {
            try {
                RetrofitClient.getApiService().deleteChore(choreId, today())
                loadDashboard()
            } catch (e: Exception) {
                _error.value = "Failed to delete chore"
            }
        }
    }

    fun addScheduleBlock(timeStart: String, timeEnd: String, title: String, emoji: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.getApiService().createScheduleBlock(
                    CreateScheduleRequest(timeStart, timeEnd, title, emoji, today())
                )
                loadDashboard()
            } catch (e: Exception) {
                _error.value = "Failed to add schedule block"
            }
        }
    }

    fun editScheduleBlock(blockId: Int, title: String, emoji: String, timeStart: String, timeEnd: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.getApiService().updateScheduleBlock(
                    blockId, UpdateScheduleRequest(title, emoji, timeStart, timeEnd)
                )
                loadDashboard()
            } catch (e: Exception) {
                _error.value = "Failed to edit schedule block"
            }
        }
    }

    fun deleteScheduleBlock(blockId: Int) {
        viewModelScope.launch {
            try {
                RetrofitClient.getApiService().deleteScheduleBlock(blockId, today())
                loadDashboard()
            } catch (e: Exception) {
                _error.value = "Failed to delete schedule block"
            }
        }
    }

    fun applyTemplate() {
        viewModelScope.launch {
            try {
                val blocks = _schedule.value.map {
                    CreateScheduleRequest(it.time_start, it.time_end, it.title, it.emoji)
                }
                RetrofitClient.getApiService().applyTemplate(TemplateRequest(blocks))
                _error.value = null
                loadDashboard()
            } catch (e: Exception) {
                _error.value = "Failed to save template"
            }
        }
    }

    fun loadCountdowns() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().getCountdowns()
                if (response.isSuccessful) {
                    _countdowns.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                _error.value = "Failed to load countdowns"
            }
        }
    }

    fun addCountdown(label: String, emoji: String, targetDate: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.getApiService().createCountdown(CreateCountdownRequest(label, emoji, targetDate))
                loadCountdowns()
            } catch (e: Exception) {
                _error.value = "Failed to add countdown"
            }
        }
    }

    fun deleteCountdown(id: Int) {
        viewModelScope.launch {
            try {
                RetrofitClient.getApiService().deleteCountdown(id)
                loadCountdowns()
            } catch (e: Exception) {
                _error.value = "Failed to delete countdown"
            }
        }
    }

    fun loadVerses() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().getVerses()
                if (response.isSuccessful) {
                    _verses.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                _error.value = "Failed to load verses"
            }
        }
    }

    fun addVerse(text: String, reference: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.getApiService().createVerse(CreateVerseRequest(text, reference))
                loadVerses()
            } catch (e: Exception) {
                _error.value = "Failed to add verse"
            }
        }
    }

    fun loadTimer() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().getTimer()
                if (response.isSuccessful) {
                    val timer = response.body()
                    _timerStatus.value = timer
                    if (timer?.active == true && timer.end_time != null) {
                        startLocalCountdown(timer.end_time)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun startTimer(minutes: Int, label: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.getApiService().createTimer(CreateTimerRequest(minutes, label))
                _timerFinished.value = false
                loadTimer()
            } catch (e: Exception) {
                _error.value = "Failed to start timer"
            }
        }
    }

    fun cancelTimer() {
        viewModelScope.launch {
            try {
                RetrofitClient.getApiService().deleteTimer()
                timerTickJob?.cancel()
                timerTickJob = null
                _timerStatus.value = null
                _timerSecondsLeft.value = 0
                _timerFinished.value = false
                stopAlarmSound()
            } catch (e: Exception) {
                _error.value = "Failed to cancel timer"
            }
        }
    }

    fun dismissTimerFinished() {
        stopAlarmSound()
        _timerFinished.value = false
    }

    fun startAlarmSound() {
        _alarmPlaying.value = true
        val app = getApplication<Application>()
        val intent = Intent(app, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START
        }
        app.startForegroundService(intent)
        alarmAutoStopJob?.cancel()
        alarmAutoStopJob = viewModelScope.launch {
            delay(15000)
            _alarmPlaying.value = false
        }
    }

    fun stopAlarmSound() {
        alarmAutoStopJob?.cancel()
        _alarmPlaying.value = false
        val app = getApplication<Application>()
        val intent = Intent(app, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP
        }
        app.startService(intent)
    }

    private fun startLocalCountdown(endTimeIso: String) {
        timerTickJob?.cancel()
        timerTickJob = viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
            val endMs = try {
                sdf.parse(endTimeIso)?.time ?: return@launch
            } catch (e: Exception) {
                try {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                        .parse(endTimeIso)?.time ?: return@launch
                } catch (e2: Exception) {
                    return@launch
                }
            }

            while (true) {
                val secondsLeft = ((endMs - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                _timerSecondsLeft.value = secondsLeft
                if (secondsLeft <= 0) {
                    _timerStatus.value = _timerStatus.value?.copy(active = false)
                    _timerFinished.value = true
                    startAlarmSound()
                    return@launch
                }
                delay(500)
            }
        }
    }

    fun updateServerUrl(url: String) {
        prefs.serverUrl = url
        RetrofitClient.init(url)
        loadDashboard()
    }

    fun testConnection() {
        viewModelScope.launch {
            _connectionStatus.value = "Testing..."
            val ok = RetrofitClient.testConnection()
            _connectionStatus.value = if (ok) "✓ Connected" else "✗ Failed"
        }
    }

    fun clearConnectionStatus() {
        _connectionStatus.value = null
    }

    fun updateKidsConfig(kids: List<KidConfig>) {
        prefs.kidsConfig = kids
        _kidsConfig.value = kids
    }

    fun updateKidColor(kidId: Int, colorHex: String) {
        val updatedConfigs = _kidsConfig.value.map { config ->
            if (config.id == kidId) config.copy(color = colorHex) else config
        }
        prefs.kidsConfig = updatedConfigs
        _kidsConfig.value = updatedConfigs

        _kids.value = _kids.value.map { kid ->
            if (kid.id == kidId) kid.copy(color = colorHex) else kid
        }

        viewModelScope.launch {
            try {
                RetrofitClient.getApiService().updateKid(kidId, UpdateKidColorRequest(colorHex))
            } catch (e: Exception) {
                Log.w("MainViewModel", "Failed to sync kid color to server: ${e.message}")
            }
        }
    }

    fun loadPhotoSettings() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().getPhotoSettings()
                if (response.isSuccessful) {
                    response.body()?.let { _photoSettings.value = it }
                }
            } catch (_: Exception) {}
        }
    }

    fun updateDisplayMode(mode: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.getApiService().updatePhotoSettings(mapOf("mode" to mode))
                _photoSettings.value = _photoSettings.value.copy(mode = mode)
            } catch (e: Exception) {
                _error.value = "Failed to update display mode"
            }
        }
    }

    fun updateAlbumUrl(url: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.getApiService().updatePhotoSettings(mapOf("album_url" to url))
                _photoSettings.value = _photoSettings.value.copy(album_url = url)
            } catch (e: Exception) {
                _error.value = "Failed to update album URL"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
