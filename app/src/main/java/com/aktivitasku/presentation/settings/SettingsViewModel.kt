package com.aktivitasku.presentation.settings

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktivitasku.data.repository.ActivityRepository
import com.aktivitasku.service.AlarmScheduler
import com.aktivitasku.util.BackupManager
import com.aktivitasku.util.BackupResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── DataStore extension ───────────────────────────────────
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object PrefKeys {
    val FIRST_LAUNCH     = booleanPreferencesKey("first_launch")
    val VIBRATE_ENABLED  = booleanPreferencesKey("vibrate_enabled")
    val SOUND_ENABLED    = booleanPreferencesKey("sound_enabled")
    val DND_ENABLED      = booleanPreferencesKey("dnd_enabled")
    val DND_START_HOUR   = intPreferencesKey("dnd_start_hour")
    val DND_END_HOUR     = intPreferencesKey("dnd_end_hour")
    val DARK_MODE        = booleanPreferencesKey("dark_mode")
}

data class SettingsUiState(
    val vibrateEnabled: Boolean  = true,
    val soundEnabled: Boolean    = true,
    val dndEnabled: Boolean      = false,
    val dndStartHour: Int        = 22,
    val dndEndHour: Int          = 7,
    val darkModeEnabled: Boolean = false,
    val totalActivities: Int     = 0,
    val isExporting: Boolean     = false,
    val isImporting: Boolean     = false,
    val message: String?         = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ActivityRepository,
    private val backupManager: BackupManager,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Load preferences
        viewModelScope.launch {
            context.dataStore.data.collect { prefs ->
                _uiState.update {
                    it.copy(
                        vibrateEnabled  = prefs[PrefKeys.VIBRATE_ENABLED]  ?: true,
                        soundEnabled    = prefs[PrefKeys.SOUND_ENABLED]    ?: true,
                        dndEnabled      = prefs[PrefKeys.DND_ENABLED]      ?: false,
                        dndStartHour    = prefs[PrefKeys.DND_START_HOUR]   ?: 22,
                        dndEndHour      = prefs[PrefKeys.DND_END_HOUR]     ?: 7,
                        darkModeEnabled = prefs[PrefKeys.DARK_MODE]        ?: false
                    )
                }
            }
        }
        // Count activities
        viewModelScope.launch {
            repository.countCompleted().combine(repository.countPending()) { c, p -> c + p }
                .collect { total -> _uiState.update { it.copy(totalActivities = total) } }
        }
    }

    // ── Preference toggles ────────────────────────────────

    fun toggleVibrate(v: Boolean)  = savePref(PrefKeys.VIBRATE_ENABLED, v)
    fun toggleSound(v: Boolean)    = savePref(PrefKeys.SOUND_ENABLED, v)
    fun toggleDnd(v: Boolean)      = savePref(PrefKeys.DND_ENABLED, v)
    fun toggleDarkMode(v: Boolean) = savePref(PrefKeys.DARK_MODE, v)
    fun setDndStart(h: Int)        = savePref(PrefKeys.DND_START_HOUR, h)
    fun setDndEnd(h: Int)          = savePref(PrefKeys.DND_END_HOUR, h)

    private fun <T> savePref(key: Preferences.Key<T>, value: T) {
        viewModelScope.launch {
            context.dataStore.edit { it[key] = value }
        }
    }

    // ── Backup ────────────────────────────────────────────

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val result = backupManager.exportToUri(uri)
            _uiState.update {
                it.copy(
                    isExporting = false,
                    message = when (result) {
                        is BackupResult.Success -> "Berhasil ekspor ${result.count} kegiatan"
                        is BackupResult.Error   -> result.message
                    }
                )
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }
            val result = backupManager.importFromUri(uri)
            if (result is BackupResult.Success) {
                // Reschedule all alarms for imported activities
                val activities = repository.observeUpcoming().first()
                alarmScheduler.rescheduleAll(activities)
            }
            _uiState.update {
                it.copy(
                    isImporting = false,
                    message = when (result) {
                        is BackupResult.Success -> "Berhasil import ${result.count} kegiatan"
                        is BackupResult.Error   -> result.message
                    }
                )
            }
        }
    }

    // ── Clear all ─────────────────────────────────────────

    fun clearAll() {
        viewModelScope.launch {
            repository.observeAll().first().forEach { activity ->
                alarmScheduler.cancel(activity.id)
                repository.delete(activity)
            }
            _uiState.update { it.copy(message = "Semua kegiatan telah dihapus") }
        }
    }

    fun clearMessage() { _uiState.update { it.copy(message = null) } }
}
