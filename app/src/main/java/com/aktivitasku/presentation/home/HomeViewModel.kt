package com.aktivitasku.presentation.home

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aktivitasku.data.repository.ActivityRepository
import com.aktivitasku.domain.model.Activity
import com.aktivitasku.service.AlarmScheduler
import android.app.Application
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class HomeUiState(
    val todayActivities: List<Activity>  = emptyList(),
    val upcomingActivities: List<Activity> = emptyList(),
    val completedCount: Int              = 0,
    val pendingCount: Int                = 0,
    val selectedDate: LocalDate          = LocalDate.now(),
    val searchQuery: String              = "",
    val searchResults: List<Activity>    = emptyList(),
    val isSearching: Boolean             = false,
    val isLoading: Boolean               = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val repository: ActivityRepository,
    private val scheduler: AlarmScheduler
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        observeToday()
        observeUpcoming()
        observeCounts()
        observeSearch()
    }

    private fun observeToday() {
        viewModelScope.launch {
            repository.observeToday().collect { list ->
                _uiState.update { it.copy(todayActivities = list, isLoading = false) }
            }
        }
    }

    private fun observeUpcoming() {
        viewModelScope.launch {
            repository.observeNext(limit = 10).collect { list ->
                _uiState.update { it.copy(upcomingActivities = list) }
            }
        }
    }

    private fun observeCounts() {
        viewModelScope.launch {
            combine(
                repository.countCompleted(),
                repository.countPending()
            ) { completed, pending -> Pair(completed, pending) }.collect { (c, p) ->
                _uiState.update { it.copy(completedCount = c, pendingCount = p) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .flatMapLatest { q ->
                    if (q.isBlank()) flowOf(emptyList())
                    else repository.search(q)
                }
                .collect { results ->
                    _uiState.update { it.copy(searchResults = results) }
                }
        }
    }

    // ── Actions ───────────────────────────────────────────

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query, isSearching = query.isNotBlank()) }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _uiState.update { it.copy(searchQuery = "", isSearching = false, searchResults = emptyList()) }
    }

    fun toggleCompleted(activity: Activity) {
        viewModelScope.launch {
            val newCompleted = !activity.isCompleted
            repository.setCompleted(activity.id, newCompleted)
            if (newCompleted) scheduler.cancel(activity.id)
            else scheduler.schedule(activity.copy(isCompleted = false))
            com.aktivitasku.service.WidgetRefreshWorker.runOnce(getApplication())
        }
    }

    fun deleteActivity(activity: Activity) {
        viewModelScope.launch {
            scheduler.cancel(activity.id)
            repository.delete(activity)
            com.aktivitasku.service.WidgetRefreshWorker.runOnce(getApplication())
        }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        viewModelScope.launch {
            repository.observeByDateRange(
                start = date.atStartOfDay(),
                end   = date.plusDays(1).atStartOfDay()
            ).collect { list ->
                _uiState.update { it.copy(todayActivities = list) }
            }
        }
    }
}
