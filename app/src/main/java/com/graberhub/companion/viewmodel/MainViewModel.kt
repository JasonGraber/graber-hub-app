package com.graberhub.companion.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

enum class ConnectionState {
    UNKNOWN, CONNECTING, CONNECTED, FAILED
}

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

    private val _activeVerseId = MutableStateFlow<Int?>(null)
    val activeVerseId: StateFlow<Int?> = _activeVerseId.asStateFlow()

    private val _connectionStatus = MutableStateFlow<String?>(null)
    val connectionStatus: StateFlow<String?> = _connectionStatus.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.UNKNOWN)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _activeNetwork = MutableStateFlow<String?>(null)
    val activeNetwork: StateFlow<String?> = _activeNetwork.asStateFlow()

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

    private val _photoSettings = MutableStateFlow<PhotoSettings?>(null)
    val photoSettings: StateFlow<PhotoSettings?> = _photoSettings.asStateFlow()

    private val _photoSettingsSaved = MutableStateFlow<String?>(null)
    val photoSettingsSaved: StateFlow<String?> = _photoSettingsSaved.asStateFlow()

    private val _displayMode = MutableStateFlow("dashboard")
    val displayMode: StateFlow<String> = _displayMode.asStateFlow()

    private val _photos = MutableStateFlow<List<UploadedPhoto>>(emptyList())
    val photos: StateFlow<List<UploadedPhoto>> = _photos.asStateFlow()

    private val _photoUploadInProgress = MutableStateFlow(false)
    val photoUploadInProgress: StateFlow<Boolean> = _photoUploadInProgress.asStateFlow()

    private val _customScheduleTemplates = MutableStateFlow<List<com.graberhub.companion.data.models.ScheduleTemplate>>(prefs.customScheduleTemplates)
    val customScheduleTemplates: StateFlow<List<com.graberhub.companion.data.models.ScheduleTemplate>> = _customScheduleTemplates.asStateFlow()

    private val _photoSource = MutableStateFlow(prefs.photoSource)
    val photoSource: StateFlow<String> = _photoSource.asStateFlow()

    // ── Data version tracking (prevents stale writes) ──────
    private val _dataVersion = MutableStateFlow(0)
    val dataVersion: StateFlow<Int> = _dataVersion.asStateFlow()

    /** True only after loadDashboard() succeeds. Blocks all write ops until fresh data is loaded. */
    private val _dataReady = MutableStateFlow(false)
    val dataReady: StateFlow<Boolean> = _dataReady.asStateFlow()

    private var displayModePollJob: Job? = null
    private var timerTickJob: Job? = null
    private var alarmAutoStopJob: Job? = null

    private val alarmStopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            _alarmPlaying.value = false
            alarmAutoStopJob?.cancel()
        }
    }

    init {
        updateActiveNetworkLabel()
        connectWithFallback()
        loadTimer()
        startDisplayModePoll()

        val filter = IntentFilter(AlarmService.ACTION_STOP)
        if (Build.VERSION.SDK_INT >= 33) {
            getApplication<Application>().registerReceiver(alarmStopReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            getApplication<Application>().registerReceiver(alarmStopReceiver, filter)
        }
    }

    override fun onCleared() {
        super.onCleared()
        displayModePollJob?.cancel()
        try {
            getApplication<Application>().unregisterReceiver(alarmStopReceiver)
        } catch (_: Exception) {}
    }

    // ── Helpers ────────────────────────────────────────────

    /**
     * Guard that blocks write operations until fresh data has been loaded.
     * Returns true if ready; sets error and returns false otherwise.
     */
    private fun requireDataReady(): Boolean {
        if (!_dataReady.value) {
            _error.value = "Please wait — syncing with hub..."
            return false
        }
        return true
    }

    /**
     * Update the local data version from a dashboard response and sync to RetrofitClient.
     */
    private fun updateDataVersion(version: Int?) {
        if (version != null && version > 0) {
            _dataVersion.value = version
            RetrofitClient.dataVersion.set(version)
            Log.d("MainViewModel", "Data version updated to $version")
        }
    }

    /**
     * Check if a Retrofit response is a 409 Conflict (stale version).
     * If so, refresh dashboard and return true so the caller can retry.
     */
    private suspend fun <T> handle409(response: Response<T>): Boolean {
        if (response.code() == 409) {
            Log.w("MainViewModel", "409 Conflict — data version is stale, refreshing...")
            _error.value = "Data was updated on another device. Refreshing..."
            // Refresh dashboard to get latest data + version
            try {
                val dashResponse = RetrofitClient.getApiService().getDashboard()
                if (dashResponse.isSuccessful) {
                    dashResponse.body()?.let { dashboard ->
                        _kids.value = dashboard.kids
                        _schedule.value = dashboard.schedule.sortedBy { it.time_start }
                        _countdowns.value = dashboard.countdowns
                        _verse.value = dashboard.verse
                        updateDataVersion(dashboard.data_version)
                        _dataReady.value = true
                    }
                    _connectionState.value = ConnectionState.CONNECTED
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to refresh after 409: ${e.message}")
            }
            return true
        }
        return false
    }

    private fun startDisplayModePoll() {
        displayModePollJob?.cancel()
        displayModePollJob = viewModelScope.launch {
            while (true) {
                try {
                    val response = RetrofitClient.getApiService().getPhotoSettings()
                    if (response.isSuccessful) {
                        response.body()?.let { settings ->
                            _displayMode.value = settings.mode ?: "dashboard"
                            _photoSettings.value = settings
                        }
                    }
                } catch (e: Exception) {
                    Log.d("MainViewModel", "Display mode poll failed: ${e.message}")
                }
                delay(5000)
            }
        }
    }

    private fun initRetrofit() {
        val url = prefs.serverUrl
        Log.d("MainViewModel", "initRetrofit() with url=$url")
        RetrofitClient.init(url)
    }

    private fun updateActiveNetworkLabel() {
        _activeNetwork.value = when (prefs.serverUrl) {
            PrefsManager.HOME_URL -> "home"
            PrefsManager.TAILSCALE_URL -> "tailscale"
            else -> "custom"
        }
    }

    /**
     * Auto-fallback connection logic:
     * 1. Try saved URL
     * 2. If fails, try alternate URL
     * 3. If alternate works, save it
     * 4. If both fail, show error
     */
    private fun connectWithFallback() {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.CONNECTING
            _connectionStatus.value = "Connecting..."
            // Reset data readiness on reconnect — must re-fetch before edits
            _dataReady.value = false

            // Try saved URL first
            val savedUrl = prefs.serverUrl
            Log.d("MainViewModel", "connectWithFallback: trying saved URL=$savedUrl")
            RetrofitClient.init(savedUrl)

            if (RetrofitClient.testConnection()) {
                Log.d("MainViewModel", "connectWithFallback: saved URL connected!")
                _connectionState.value = ConnectionState.CONNECTED
                _connectionStatus.value = "✓ Connected"
                updateActiveNetworkLabel()
                loadDashboard()
                return@launch
            }

            // Try alternate URL
            val altUrl = prefs.alternateUrl
            Log.d("MainViewModel", "connectWithFallback: saved URL failed, trying alternate=$altUrl")
            RetrofitClient.init(altUrl)

            if (RetrofitClient.testConnection()) {
                Log.d("MainViewModel", "connectWithFallback: alternate URL connected!")
                prefs.serverUrl = altUrl
                _connectionState.value = ConnectionState.CONNECTED
                _connectionStatus.value = "✓ Connected (switched network)"
                updateActiveNetworkLabel()
                loadDashboard()
                return@launch
            }

            // Both failed
            Log.d("MainViewModel", "connectWithFallback: both URLs failed")
            // Reset to saved URL
            RetrofitClient.init(savedUrl)
            _connectionState.value = ConnectionState.FAILED
            _connectionStatus.value = "✗ Connection failed"
            _error.value = "Cannot reach server. Check your network."
        }
    }

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // ── Dashboard ──────────────────────────────────────────

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
                        updateDataVersion(dashboard.data_version)
                        _dataReady.value = true
                    }
                    _connectionState.value = ConnectionState.CONNECTED
                } else {
                    _error.value = "Server error: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Connection failed: ${e.message}"
                _connectionState.value = ConnectionState.FAILED
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Chores ─────────────────────────────────────────────

    fun toggleChore(choreId: Int) {
        if (!requireDataReady()) return
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().toggleChore(choreId, today())
                if (handle409(response)) {
                    // Retry once after refresh
                    RetrofitClient.getApiService().toggleChore(choreId, today())
                }
                loadDashboard()
            } catch (_: Exception) {
                _error.value = "Failed to update chore"
            }
        }
    }

    fun addChore(kidId: Int, title: String, emoji: String = "") {
        if (!requireDataReady()) return
        viewModelScope.launch {
            try {
                val request = CreateChoreRequest(kidId, title, today(), emoji)
                val response = RetrofitClient.getApiService().createChore(request)
                if (handle409(response)) {
                    RetrofitClient.getApiService().createChore(request)
                }
                loadDashboard()
            } catch (_: Exception) {
                _error.value = "Failed to add chore"
            }
        }
    }

    fun editChore(choreId: Int, title: String, emoji: String) {
        if (!requireDataReady()) return
        viewModelScope.launch {
            try {
                val request = UpdateChoreRequest(title, emoji)
                val response = RetrofitClient.getApiService().updateChore(choreId, request)
                if (handle409(response)) {
                    RetrofitClient.getApiService().updateChore(choreId, request)
                }
                loadDashboard()
            } catch (_: Exception) {
                _error.value = "Failed to edit chore"
            }
        }
    }

    fun deleteChore(choreId: Int) {
        if (!requireDataReady()) return
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().deleteChore(choreId, today())
                if (handle409(response)) {
                    RetrofitClient.getApiService().deleteChore(choreId, today())
                }
                loadDashboard()
            } catch (_: Exception) {
                _error.value = "Failed to delete chore"
            }
        }
    }

    // ── Schedule ───────────────────────────────────────────

    fun addScheduleBlock(timeStart: String, timeEnd: String, title: String, emoji: String) {
        if (!requireDataReady()) return
        viewModelScope.launch {
            try {
                val request = CreateScheduleRequest(timeStart, timeEnd, title, emoji, today())
                val response = RetrofitClient.getApiService().createScheduleBlock(request)
                if (handle409(response)) {
                    RetrofitClient.getApiService().createScheduleBlock(request)
                }
                loadDashboard()
            } catch (_: Exception) {
                _error.value = "Failed to add schedule block"
            }
        }
    }

    fun editScheduleBlock(blockId: Int, title: String, emoji: String, timeStart: String, timeEnd: String) {
        if (!requireDataReady()) return
        viewModelScope.launch {
            try {
                val request = UpdateScheduleRequest(title, emoji, timeStart, timeEnd)
                val response = RetrofitClient.getApiService().updateScheduleBlock(blockId, request)
                if (handle409(response)) {
                    RetrofitClient.getApiService().updateScheduleBlock(blockId, request)
                }
                loadDashboard()
            } catch (_: Exception) {
                _error.value = "Failed to edit schedule block"
            }
        }
    }

    fun deleteScheduleBlock(blockId: Int) {
        if (!requireDataReady()) return
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().deleteScheduleBlock(blockId, today())
                if (handle409(response)) {
                    RetrofitClient.getApiService().deleteScheduleBlock(blockId, today())
                }
                loadDashboard()
            } catch (_: Exception) {
                _error.value = "Failed to delete schedule block"
            }
        }
    }

    fun applyTemplate() {
        if (!requireDataReady()) return
        viewModelScope.launch {
            try {
                val blocks = _schedule.value.map {
                    CreateScheduleRequest(it.time_start, it.time_end, it.title, it.emoji)
                }
                val request = TemplateRequest(blocks)
                val response = RetrofitClient.getApiService().applyTemplate(request)
                if (handle409(response)) {
                    RetrofitClient.getApiService().applyTemplate(request)
                }
                _error.value = null
                loadDashboard()
            } catch (_: Exception) {
                _error.value = "Failed to save template"
            }
        }
    }

    fun saveCustomScheduleTemplate(name: String, emoji: String) {
        val blocks = _schedule.value.map {
            CreateScheduleRequest(it.time_start, it.time_end, it.title, it.emoji)
        }
        val template = com.graberhub.companion.data.models.ScheduleTemplate(
            name = name.trim(),
            emoji = emoji.trim().ifEmpty { "📋" },
            blocks = blocks,
            isCustom = true
        )
        val updated = _customScheduleTemplates.value + template
        prefs.customScheduleTemplates = updated
        _customScheduleTemplates.value = updated
        Log.d("MainViewModel", "Saved custom template: ${template.name} (${blocks.size} blocks)")
    }

    fun deleteCustomScheduleTemplate(name: String) {
        val updated = _customScheduleTemplates.value.filter { it.name != name }
        prefs.customScheduleTemplates = updated
        _customScheduleTemplates.value = updated
    }

    fun setPhotoSource(source: String) {
        prefs.photoSource = source
        _photoSource.value = source
        // Also persist to server so the Pi dashboard knows which source to use
        viewModelScope.launch {
            try {
                RetrofitClient.getApiService().updatePhotoSettings(
                    PhotoSettingsRequest(photo_source = source)
                )
            } catch (_: Exception) {}
        }
    }

    // ── Countdowns ─────────────────────────────────────────

    fun loadCountdowns() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().getCountdowns()
                if (response.isSuccessful) {
                    _countdowns.value = response.body() ?: emptyList()
                }
            } catch (_: Exception) {
                _error.value = "Failed to load countdowns"
            }
        }
    }

    fun addCountdown(label: String, emoji: String, targetDate: String) {
        if (!requireDataReady()) return
        viewModelScope.launch {
            try {
                val request = CreateCountdownRequest(label, emoji, targetDate)
                val response = RetrofitClient.getApiService().createCountdown(request)
                if (handle409(response)) {
                    RetrofitClient.getApiService().createCountdown(request)
                }
                loadCountdowns()
            } catch (_: Exception) {
                _error.value = "Failed to add countdown"
            }
        }
    }

    fun deleteCountdown(id: Int) {
        if (!requireDataReady()) return
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().deleteCountdown(id)
                if (handle409(response)) {
                    RetrofitClient.getApiService().deleteCountdown(id)
                }
                loadCountdowns()
            } catch (_: Exception) {
                _error.value = "Failed to delete countdown"
            }
        }
    }

    // ── Verses ─────────────────────────────────────────────

    fun loadVerses() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().getVerses()
                if (response.isSuccessful) {
                    _verses.value = response.body() ?: emptyList()
                }
            } catch (_: Exception) {
                _error.value = "Failed to load verses"
            }
        }
    }

    fun loadActiveVerse() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().getActiveVerse()
                if (response.isSuccessful) {
                    _activeVerseId.value = response.body()?.get("active_verse_id")
                }
            } catch (_: Exception) {
                Log.w("MainViewModel", "Failed to load active verse")
            }
        }
    }

    fun setActiveVerse(id: Int) {
        if (!requireDataReady()) return
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().setActiveVerse(id)
                if (response.isSuccessful) {
                    _activeVerseId.value = id
                }
            } catch (_: Exception) {
                _error.value = "Failed to set active verse"
            }
        }
    }

    fun clearActiveVerse() {
        if (!requireDataReady()) return
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().clearActiveVerse()
                if (response.isSuccessful) {
                    _activeVerseId.value = null
                }
            } catch (_: Exception) {
                _error.value = "Failed to clear active verse"
            }
        }
    }

    fun addVerse(text: String, reference: String) {
        if (!requireDataReady()) return
        viewModelScope.launch {
            try {
                val request = CreateVerseRequest(text, reference)
                val response = RetrofitClient.getApiService().createVerse(request)
                if (handle409(response)) {
                    RetrofitClient.getApiService().createVerse(request)
                }
                loadVerses()
            } catch (_: Exception) {
                _error.value = "Failed to add verse"
            }
        }
    }

    fun editVerse(id: Int, text: String, reference: String) {
        if (!requireDataReady()) return
        viewModelScope.launch {
            try {
                val request = UpdateVerseRequest(text, reference)
                val response = RetrofitClient.getApiService().updateVerse(id, request)
                if (handle409(response)) {
                    RetrofitClient.getApiService().updateVerse(id, request)
                }
                loadVerses()
            } catch (_: Exception) {
                _error.value = "Failed to edit verse"
            }
        }
    }

    fun deleteVerse(id: Int) {
        if (!requireDataReady()) return
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().deleteVerse(id)
                if (handle409(response)) {
                    RetrofitClient.getApiService().deleteVerse(id)
                }
                _verses.value = _verses.value.filter { it.id != id }
                if (_activeVerseId.value == id) _activeVerseId.value = null
            } catch (_: Exception) {
                _error.value = "Failed to delete verse"
            }
        }
    }

    // ── Timer ──────────────────────────────────────────────

    fun loadTimer() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().getTimer()
                if (response.isSuccessful) {
                    val status = response.body()
                    _timerStatus.value = status
                    if (status?.active == true && status.end_time != null) {
                        startLocalCountdown(status.end_time)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun startTimer(minutes: Int, label: String) {
        if (!requireDataReady()) return
        viewModelScope.launch {
            try {
                val request = CreateTimerRequest(minutes, label)
                val response = RetrofitClient.getApiService().createTimer(request)
                if (handle409(response)) {
                    RetrofitClient.getApiService().createTimer(request)
                }
                _timerFinished.value = false
                loadTimer()
            } catch (_: Exception) {
                _error.value = "Failed to start timer"
            }
        }
    }

    fun cancelTimer() {
        if (!requireDataReady()) return
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().deleteTimer()
                if (handle409(response)) {
                    RetrofitClient.getApiService().deleteTimer()
                }
                timerTickJob?.cancel()
                timerTickJob = null
                _timerStatus.value = null
                _timerSecondsLeft.value = 0
                _timerFinished.value = false
                stopAlarmSound()
            } catch (_: Exception) {
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
                sdf.parse(endTimeIso)?.time
            } catch (_: Exception) {
                try {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(endTimeIso)?.time
                } catch (_: Exception) {
                    null
                }
            } ?: return@launch

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

    // ── Settings ───────────────────────────────────────────

    fun updateServerUrl(url: String) {
        prefs.serverUrl = url
        RetrofitClient.init(prefs.serverUrl)
        updateActiveNetworkLabel()
        _dataReady.value = false
        loadDashboard()
    }

    /**
     * Quick-switch to a specific network URL.
     * Saves, reinitializes Retrofit, tests connection, reloads if connected.
     */
    fun switchNetwork(url: String) {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.CONNECTING
            _connectionStatus.value = "Connecting..."
            _dataReady.value = false

            prefs.serverUrl = url
            RetrofitClient.init(prefs.serverUrl)
            updateActiveNetworkLabel()

            val ok = RetrofitClient.testConnection()
            if (ok) {
                _connectionState.value = ConnectionState.CONNECTED
                _connectionStatus.value = "✓ Connected"
                loadDashboard()
            } else {
                _connectionState.value = ConnectionState.FAILED
                _connectionStatus.value = "✗ Connection failed"
            }
        }
    }

    fun reconnect() {
        connectWithFallback()
    }

    fun testConnection() {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.CONNECTING
            _connectionStatus.value = "Testing..."
            val ok = RetrofitClient.testConnection()
            _connectionState.value = if (ok) ConnectionState.CONNECTED else ConnectionState.FAILED
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
        if (!requireDataReady()) return
        val updatedConfigs = _kidsConfig.value.map {
            if (it.id == kidId) it.copy(color = colorHex) else it
        }
        prefs.kidsConfig = updatedConfigs
        _kidsConfig.value = updatedConfigs

        _kids.value = _kids.value.map {
            if (it.id == kidId) it.copy(color = colorHex) else it
        }

        viewModelScope.launch {
            try {
                RetrofitClient.getApiService().updateKid(kidId, UpdateKidColorRequest(colorHex))
            } catch (e: Exception) {
                Log.w("MainViewModel", "Failed to sync kid color to server: ${e.message}")
            }
        }
    }

    // ── Photo Settings ─────────────────────────────────────

    fun loadPhotoSettings() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().getPhotoSettings()
                if (response.isSuccessful) {
                    _photoSettings.value = response.body()
                }
            } catch (_: Exception) {
                Log.w("MainViewModel", "Failed to load photo settings")
            }
        }
    }

    fun updateDisplayMode(mode: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().updatePhotoSettings(
                    PhotoSettingsRequest(mode = mode)
                )
                if (response.isSuccessful) {
                    _photoSettings.value = response.body()
                    _photoSettingsSaved.value = "mode"
                    delay(2000)
                    _photoSettingsSaved.value = null
                }
            } catch (_: Exception) {
                _error.value = "Failed to update display mode"
            }
        }
    }

    fun updateAlbumUrl(url: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().updatePhotoSettings(
                    PhotoSettingsRequest(album_url = url)
                )
                if (response.isSuccessful) {
                    _photoSettings.value = response.body()
                    _photoSettingsSaved.value = "album"
                    delay(2000)
                    _photoSettingsSaved.value = null
                }
            } catch (_: Exception) {
                _error.value = "Failed to save album URL"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    // ── Photo Upload (picture frame) ───────────────────────

    fun loadPhotos() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().getPhotos()
                if (response.isSuccessful) {
                    _photos.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.w("MainViewModel", "Failed to load photos: ${e.message}")
            }
        }
    }

    fun uploadPhoto(uri: Uri, name: String = "") {
        viewModelScope.launch {
            _photoUploadInProgress.value = true
            try {
                val context = getApplication<Application>()
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val extension = when (mimeType) {
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    else -> "jpg"
                }
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Cannot open input stream for URI")
                val bytes = inputStream.readBytes()
                inputStream.close()

                val requestBody = bytes.toRequestBody(mimeType.toMediaType())
                val part = MultipartBody.Part.createFormData("photo", "upload.$extension", requestBody)

                val namePart = if (name.isNotBlank()) {
                    name.toRequestBody("text/plain".toMediaType())
                } else null

                val response = RetrofitClient.getApiService().uploadPhoto(part, namePart)
                if (response.isSuccessful) {
                    loadPhotos()
                } else {
                    _error.value = "Upload failed: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Upload failed: ${e.message}"
            } finally {
                _photoUploadInProgress.value = false
            }
        }
    }

    fun renamePhoto(filename: String, name: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().renameUploadedPhoto(filename, mapOf("name" to name))
                if (response.isSuccessful) {
                    loadPhotos()
                }
            } catch (_: Exception) {}
        }
    }

    fun deleteUploadedPhoto(filename: String) {
        viewModelScope.launch {
            try {
                // Optimistic local removal
                _photos.value = _photos.value.filter { it.filename != filename }
                val response = RetrofitClient.getApiService().deleteUploadedPhoto(filename)
                if (response.isSuccessful) {
                    // Force refresh from server to confirm
                    loadPhotos()
                } else {
                    _error.value = "Delete failed: ${response.code()}"
                    loadPhotos() // Refresh to restore if server rejected
                }
            } catch (e: Exception) {
                _error.value = "Delete failed: ${e.message}"
                loadPhotos() // Refresh to restore actual state
            }
        }
    }

    // ── Chore Templates ────────────────────────────────────

    private val _choreTemplates = MutableStateFlow<List<ChoreTemplate>>(emptyList())
    val choreTemplates: StateFlow<List<ChoreTemplate>> = _choreTemplates.asStateFlow()

    fun loadChoreTemplates() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().getChoreTemplates()
                if (response.isSuccessful) {
                    _choreTemplates.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Failed to load chore templates: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Failed to load chore templates: ${e.message}"
            }
        }
    }

    fun createChoreTemplate(request: CreateChoreTemplateRequest) {
        if (!requireDataReady()) return
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().createChoreTemplate(request)
                if (handle409(response)) {
                    RetrofitClient.getApiService().createChoreTemplate(request)
                }
                if (response.isSuccessful) {
                    loadChoreTemplates()
                } else {
                    _error.value = "Failed to create template"
                }
            } catch (e: Exception) {
                _error.value = "Failed to create template: ${e.message}"
            }
        }
    }

    fun updateChoreTemplate(id: Int, request: UpdateChoreTemplateRequest) {
        if (!requireDataReady()) return
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().updateChoreTemplate(id, request)
                if (handle409(response)) {
                    RetrofitClient.getApiService().updateChoreTemplate(id, request)
                }
                if (response.isSuccessful) {
                    loadChoreTemplates()
                } else {
                    _error.value = "Failed to update template"
                }
            } catch (e: Exception) {
                _error.value = "Failed to update template: ${e.message}"
            }
        }
    }

    fun deleteChoreTemplate(id: Int) {
        if (!requireDataReady()) return
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().deleteChoreTemplate(id)
                if (handle409(response)) {
                    RetrofitClient.getApiService().deleteChoreTemplate(id)
                }
                if (response.isSuccessful) {
                    _choreTemplates.value = _choreTemplates.value.filter { it.id != id }
                } else {
                    _error.value = "Failed to delete template"
                }
            } catch (e: Exception) {
                _error.value = "Failed to delete template: ${e.message}"
            }
        }
    }
}
