package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CounterDto
import com.example.data.model.QueueStatus
import com.example.data.model.QueueTicketDto
import com.example.data.model.ServiceDto
import com.example.data.model.StaffSession
import com.example.data.remote.RealtimeSignal
import com.example.data.repository.QueueRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class StaffUiState(
    val isConfigured: Boolean = true,
    val session: StaffSession? = null,
    val isLoggingIn: Boolean = false,
    val loginError: String? = null,
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val isRealtimeConnected: Boolean = false,
    val counters: List<CounterDto> = listOf(
        CounterDto(1, "Loket 1", true),
        CounterDto(2, "Loket 2", true),
        CounterDto(3, "Loket 3", true)
    ),
    val selectedCounter: String = "Loket 1",
    val services: List<ServiceDto> = emptyList(),
    val selectedServiceCode: String? = null,
    val todayQueues: List<QueueTicketDto> = emptyList(),
    val callAttemptCounts: Map<String, Int> = emptyMap(),
    val snackbarMessage: String? = null,
    val errorMessage: String? = null
) {
    val activeTicketAtCounter: QueueTicketDto?
        get() = todayQueues
            .filter {
                it.counterName.equals(selectedCounter, ignoreCase = true) &&
                    (it.queueStatus == QueueStatus.CALLED || it.queueStatus == QueueStatus.IN_SERVICE)
            }
            .maxByOrNull { it.servedAt ?: it.calledAt ?: it.createdAt.orEmpty() }

    val filteredWaitingQueues: List<QueueTicketDto>
        get() = todayQueues
            .filter { ticket ->
                ticket.queueStatus == QueueStatus.WAITING &&
                    (selectedServiceCode == null || ticket.serviceCode == selectedServiceCode)
            }
            .sortedBy { it.createdAt ?: it.queueNumber }

    val filteredHistoryQueues: List<QueueTicketDto>
        get() = todayQueues
            .filter { ticket ->
                (ticket.queueStatus == QueueStatus.COMPLETED || ticket.queueStatus == QueueStatus.SKIPPED) &&
                    (selectedServiceCode == null || ticket.serviceCode == selectedServiceCode)
            }
            .sortedByDescending { it.completedAt ?: it.createdAt.orEmpty() }

    val waitingCount: Int
        get() = todayQueues.count { it.queueStatus == QueueStatus.WAITING }

    val activeCount: Int
        get() = todayQueues.count {
            it.queueStatus == QueueStatus.CALLED || it.queueStatus == QueueStatus.IN_SERVICE
        }

    val completedCount: Int
        get() = todayQueues.count { it.queueStatus == QueueStatus.COMPLETED }

    fun serviceNameFor(serviceId: String): String =
        services.firstOrNull { it.id == serviceId }?.name ?: "Layanan ULT"

    fun callAttemptsFor(ticketId: String): Int =
        (callAttemptCounts[ticketId] ?: 1).coerceAtLeast(1)
}

class StaffDashboardViewModel(
    private val repository: QueueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StaffUiState(isConfigured = repository.isConfigured)
    )
    val uiState: StateFlow<StaffUiState> = _uiState.asStateFlow()

    init {
        observeSessionAndPrefs()
        observeRealtime()
        startFallbackSync()
    }

    private fun observeSessionAndPrefs() {
        viewModelScope.launch {
            repository.staffSessionFlow.collect { session ->
                val wasLoggedOut = _uiState.value.session == null
                _uiState.update { it.copy(session = session) }
                if (session != null && wasLoggedOut) {
                    refreshDashboard()
                }
            }
        }
        viewModelScope.launch {
            repository.selectedCounterFlow.collect { counter ->
                _uiState.update { it.copy(selectedCounter = counter) }
            }
        }
        viewModelScope.launch {
            repository.isRealtimeConnected.collect { connected ->
                _uiState.update { it.copy(isRealtimeConnected = connected) }
            }
        }
    }

    private fun observeRealtime() {
        repository.connectRealtime()
        viewModelScope.launch {
            repository.realtimeSignals.collect { signal ->
                if (signal == RealtimeSignal.QueueChanged && _uiState.value.session != null) {
                    refreshQueuesSilent()
                }
            }
        }
    }

    private fun startFallbackSync() {
        viewModelScope.launch {
            while (isActive) {
                delay(12_000L)
                if (_uiState.value.session != null && repository.isConfigured) {
                    refreshQueuesSilent()
                }
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(loginError = "Email dan kata sandi wajib diisi.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true, loginError = null) }
            repository.loginStaff(email, password)
                .onSuccess { session ->
                    _uiState.update {
                        it.copy(
                            isLoggingIn = false,
                            session = session,
                            loginError = null
                        )
                    }
                    refreshDashboard()
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoggingIn = false,
                            loginError = error.message ?: "Login petugas gagal."
                        )
                    }
                }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            repository.logoutStaff()
            _uiState.update {
                it.copy(
                    session = null,
                    loginError = null,
                    snackbarMessage = "Anda telah keluar dari Portal Petugas."
                )
            }
            onLoggedOut()
        }
    }

    fun refreshDashboard() {
        if (!repository.isConfigured) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val servicesDef = async { repository.fetchServices() }
            val countersDef = async { repository.fetchCounters() }
            val queuesDef = async { repository.fetchTodayQueues() }

            val servicesRes = servicesDef.await()
            val countersRes = countersDef.await()
            val queuesRes = queuesDef.await()

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    services = servicesRes.getOrElse { state.services },
                    counters = countersRes.getOrElse { state.counters },
                    todayQueues = queuesRes.getOrElse { state.todayQueues },
                    errorMessage = queuesRes.exceptionOrNull()?.message
                        ?: servicesRes.exceptionOrNull()?.message
                )
            }
        }
    }

    private fun refreshQueuesSilent() {
        viewModelScope.launch {
            repository.fetchTodayQueues().onSuccess { queues ->
                _uiState.update { it.copy(todayQueues = queues, errorMessage = null) }
            }
        }
    }

    fun selectCounter(counterName: String) {
        viewModelScope.launch {
            repository.preferences.saveSelectedCounter(counterName)
            _uiState.update { it.copy(selectedCounter = counterName) }
        }
    }

    fun selectServiceFilter(serviceCode: String?) {
        _uiState.update { it.copy(selectedServiceCode = serviceCode) }
    }

    fun callNextTicket() {
        val state = _uiState.value
        if (state.activeTicketAtCounter != null) {
            _uiState.update {
                it.copy(
                    snackbarMessage = "Selesaikan atau lewati tiket ${state.activeTicketAtCounter?.queueNumber} di ${state.selectedCounter} terlebih dahulu."
                )
            }
            return
        }
        val nextTicket = state.filteredWaitingQueues.firstOrNull()
        if (nextTicket == null) {
            _uiState.update {
                it.copy(snackbarMessage = "Tidak ada antrean menunggu pada filter saat ini.")
            }
            return
        }
        callSpecificTicket(nextTicket)
    }

    fun callSpecificTicket(ticket: QueueTicketDto) {
        val counter = _uiState.value.selectedCounter
        val currentActive = _uiState.value.activeTicketAtCounter
        if (currentActive != null && currentActive.id != ticket.id) {
            _uiState.update {
                it.copy(
                    snackbarMessage = "Loket $counter masih menangani ${currentActive.queueNumber}. Selesaikan atau lewati dulu."
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            repository.callTicket(ticket.id, counter)
                .onSuccess { updated ->
                    applyUpdatedTicket(updated)
                    _uiState.update { state ->
                        state.copy(
                            isMutating = false,
                            callAttemptCounts = state.callAttemptCounts + (updated.id to 1),
                            snackbarMessage = "Memanggil ${updated.queueNumber} ke $counter."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isMutating = false, errorMessage = error.message)
                    }
                }
        }
    }

    fun recallActiveTicket() {
        val active = _uiState.value.activeTicketAtCounter ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            repository.recallTicket(active.id)
                .onSuccess { updated ->
                    applyUpdatedTicket(updated)
                    val nextAttempt = (_uiState.value.callAttemptCounts[active.id] ?: 1) + 1
                    _uiState.update { state ->
                        state.copy(
                            isMutating = false,
                            callAttemptCounts = state.callAttemptCounts + (active.id to nextAttempt),
                            snackbarMessage = "Panggilan ulang ke-$nextAttempt untuk ${active.queueNumber} dikirim."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isMutating = false, errorMessage = error.message)
                    }
                }
        }
    }

    fun startServingActiveTicket() {
        val active = _uiState.value.activeTicketAtCounter ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            repository.startService(active.id)
                .onSuccess { updated ->
                    applyUpdatedTicket(updated)
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            snackbarMessage = "Melayani ${updated.queueNumber} (${updated.studentName})."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isMutating = false, errorMessage = error.message)
                    }
                }
        }
    }

    fun completeActiveTicket() {
        val active = _uiState.value.activeTicketAtCounter ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            repository.completeService(active.id)
                .onSuccess { updated ->
                    applyUpdatedTicket(updated)
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            snackbarMessage = "Pelayanan ${updated.queueNumber} telah selesai."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isMutating = false, errorMessage = error.message)
                    }
                }
        }
    }

    fun skipActiveTicket() {
        val active = _uiState.value.activeTicketAtCounter ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            repository.skipTicket(active.id)
                .onSuccess { updated ->
                    applyUpdatedTicket(updated)
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            snackbarMessage = "Antrean ${updated.queueNumber} ditandai tidak hadir (dilewati)."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isMutating = false, errorMessage = error.message)
                    }
                }
        }
    }

    private fun applyUpdatedTicket(updated: QueueTicketDto) {
        _uiState.update { state ->
            val replaced = state.todayQueues.map { if (it.id == updated.id) updated else it }
            val finalList = if (replaced.any { it.id == updated.id }) replaced else replaced + updated
            state.copy(todayQueues = finalList)
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(errorMessage = null, snackbarMessage = null, loginError = null)
        }
    }

    class Factory(
        private val repository: QueueRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StaffDashboardViewModel(repository) as T
        }
    }
}
