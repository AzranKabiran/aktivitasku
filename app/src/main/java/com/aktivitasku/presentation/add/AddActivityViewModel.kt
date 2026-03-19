package com.aktivitasku.presentation.add

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aktivitasku.data.repository.ActivityRepository
import com.aktivitasku.domain.model.*
import com.aktivitasku.service.AlarmScheduler
import com.aktivitasku.util.VoiceParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

enum class VoiceState { IDLE, LISTENING, PROCESSING, SUCCESS, ERROR }

data class AddActivityUiState(
    val id: Long?                         = null,
    val title: String                     = "",
    val description: String               = "",
    val date: LocalDate                   = LocalDate.now().plusDays(0),
    val time: LocalTime                   = LocalTime.now().plusHours(1).withMinute(0),
    val category: ActivityCategory        = ActivityCategory.OTHER,
    val priority: Priority                = Priority.MEDIUM,
    val selectedReminders: List<Int>      = listOf(15),
    val repeatType: RepeatType            = RepeatType.NONE,
    val repeatDays: List<Int>             = emptyList(),
    val voiceState: VoiceState            = VoiceState.IDLE,
    val voiceTranscript: String           = "",
    val isSaving: Boolean                 = false,
    val savedSuccessfully: Boolean        = false,
    val errorMessage: String?             = null,
    // Validation
    val titleError: String?               = null
)

@HiltViewModel
class AddActivityViewModel @Inject constructor(
    application: Application,
    private val repository: ActivityRepository,
    private val scheduler: AlarmScheduler
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AddActivityUiState())
    val uiState: StateFlow<AddActivityUiState> = _uiState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    // ── Field Updates ─────────────────────────────────────

    fun onTitleChange(v: String)       = _uiState.update { it.copy(title = v, titleError = null) }
    fun onDescChange(v: String)        = _uiState.update { it.copy(description = v) }
    fun onDateChange(v: LocalDate)     = _uiState.update { it.copy(date = v) }
    fun onTimeChange(v: LocalTime)     = _uiState.update { it.copy(time = v) }
    fun onCategoryChange(v: ActivityCategory) = _uiState.update { it.copy(category = v) }
    fun onPriorityChange(v: Priority)  = _uiState.update { it.copy(priority = v) }
    fun onRepeatTypeChange(v: RepeatType)     = _uiState.update { it.copy(repeatType = v) }

    fun toggleReminder(minutes: Int) {
        _uiState.update { state ->
            val current = state.selectedReminders.toMutableList()
            if (minutes in current) current.remove(minutes) else current.add(minutes)
            state.copy(selectedReminders = current.sorted())
        }
    }

    fun toggleRepeatDay(day: Int) {
        _uiState.update { state ->
            val days = state.repeatDays.toMutableList()
            if (day in days) days.remove(day) else days.add(day)
            state.copy(repeatDays = days.sorted())
        }
    }

    // ── Voice Input ───────────────────────────────────────

    fun startVoiceInput() {
        val ctx = getApplication<Application>()
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            _uiState.update { it.copy(errorMessage = "Speech recognition tidak tersedia di perangkat ini") }
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = null
        _uiState.update { it.copy(voiceState = VoiceState.IDLE) }

        viewModelScope.launch {
            kotlinx.coroutines.delay(200)
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(ctx).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p: Bundle?) {
                    _uiState.update { it.copy(voiceState = VoiceState.LISTENING) }
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text    = matches?.firstOrNull() ?: return
                    _uiState.update { it.copy(voiceState = VoiceState.PROCESSING, voiceTranscript = text) }
                    applyVoiceParsing(text)
                }
                override fun onError(error: Int) {
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH     -> "Suara tidak terdeteksi, coba lagi"
                        SpeechRecognizer.ERROR_NETWORK      -> "Tidak ada koneksi — coba mode offline"
                        SpeechRecognizer.ERROR_AUDIO        -> "Masalah mikrofon"
                        else                                 -> "Terjadi kesalahan, coba lagi"
                    }
                    _uiState.update { it.copy(voiceState = VoiceState.ERROR, errorMessage = msg) }
                }
                override fun onBeginningOfSpeech()  {}
                override fun onEndOfSpeech()        { _uiState.update { it.copy(voiceState = VoiceState.PROCESSING) } }
                override fun onRmsChanged(v: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onPartialResults(p: Bundle?) {}
                override fun onEvent(t: Int, p: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ucapkan kegiatan kamu...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Attempt offline recognition
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        speechRecognizer?.startListening(intent)
        } // end viewModelScope.launch
    }

    fun stopVoiceInput() {
        speechRecognizer?.stopListening()
    }

    fun resetVoiceState() {
        _uiState.update { it.copy(voiceState = VoiceState.IDLE, voiceTranscript = "") }
    }

    private fun applyVoiceParsing(text: String) {
        val parsed = VoiceParser.parse(text)
        _uiState.update { state ->
            state.copy(
                title       = parsed.title.ifBlank { state.title },
                date        = parsed.dateTime?.toLocalDate() ?: state.date,
                time        = parsed.dateTime?.toLocalTime() ?: state.time,
                category    = parsed.detectedCategory
                    ?.let { runCatching { ActivityCategory.valueOf(it) }.getOrNull() }
                    ?: state.category,
                voiceState  = VoiceState.SUCCESS
            )
        }
    }

    // ── Save ──────────────────────────────────────────────

    fun save() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(titleError = "Judul tidak boleh kosong") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val activity = Activity(
                id            = state.id ?: 0,
                title         = state.title.trim(),
                description   = state.description.trim(),
                startDateTime = LocalDateTime.of(state.date, state.time),
                category      = state.category,
                priority      = state.priority,
                reminders     = state.selectedReminders,
                repeatType    = state.repeatType,
                repeatDays    = state.repeatDays
            )
            val savedId = if (state.id == null) {
                repository.save(activity)
            } else {
                repository.update(activity); activity.id
            }
            scheduler.cancel(savedId)
            scheduler.schedule(activity.copy(id = savedId))
            com.aktivitasku.service.WidgetRefreshWorker.runOnce(getApplication())
            _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
    }
}
