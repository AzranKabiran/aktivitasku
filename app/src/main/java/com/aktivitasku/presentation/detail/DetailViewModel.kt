package com.aktivitasku.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktivitasku.data.repository.ActivityRepository
import com.aktivitasku.domain.model.Activity
import com.aktivitasku.service.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val activity: Activity? = null,
    val isLoading: Boolean  = true,
    val deleted: Boolean    = false
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: ActivityRepository,
    private val scheduler: AlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch {
            repository.observeById(id).collect { entity ->
                _uiState.update { it.copy(activity = entity, isLoading = false) }
            }
        }
    }

    fun toggleComplete() {
        val activity = _uiState.value.activity ?: return
        viewModelScope.launch {
            val newCompleted = !activity.isCompleted
            repository.setCompleted(activity.id, newCompleted)
            if (newCompleted) scheduler.cancel(activity.id)
            else scheduler.schedule(activity.copy(isCompleted = false))
        }
    }

    fun delete() {
        val activity = _uiState.value.activity ?: return
        viewModelScope.launch {
            scheduler.cancel(activity.id)
            repository.delete(activity)
            _uiState.update { it.copy(deleted = true) }
        }
    }
}
