package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.AlertAudioEngine
import com.example.core.WibOperationalState
import com.example.core.WibTimeHelper
import com.example.data.model.CounterDto
import com.example.data.model.CounterLiveDisplay
import com.example.data.model.QueueStatus
import com.example.data.model.QueueTicketDto
import com.example.data.model.RememberedStudentInfo
import com.example.data.model.ServiceDto
import com.example.data.model.ServiceSummary
import com.example.data.model.TakeTicketOutcome
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

data class StudentUiState(
    val isConfigured: Boolean = true,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isRealtimeConnected: Boolean = false,
    val operationalState: WibOperationalState = WibTimeHelper.snapshot(),
    val services: List<ServiceDto> = emptyList(),
    val serviceSummaries: List<ServiceSummary> = emptyList(),
    val liveCounters: List<CounterLiveDisplay> = emptyList(),
    val todayQueues: List<QueueTicketDto> = emptyList(),
    val activeTicket: QueueTicketDto? = null,
    val activeTicketServiceName: String = "",
    val peopleAhead: Int = 0,
    val rememberedStudent: RememberedStudentInfo = RememberedStudentInfo(),
    val callAlertTicket: QueueTicketDto? = null,
    val isSubmittingTicket: Boolean = false,
    val errorMessage: String? = null,
    val existingTicketConflict: Pair<String, String>? = null,
    val snackbarMessage: String? = null
)

class StudentQueueViewModel(
    private val repository: QueueRepository,
    private val audioEngine: AlertAudioEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StudentUiState(isConfigured = repository.isConfigured)
    )
    val uiState: StateFlow<StudentUiState> = _uiState.asStateFlow()

    private var cachedServices: List<ServiceDto> = emptyList()
    private var cachedCounters: List<CounterDto> = listOf(
        CounterDto(1, "Loket 1", true),
        CounterDto(2, "Loket 2", true),
        CounterDto(3, "Loket 3", true)
    )
    private var activeTicketId: String? = null
    private var lastAlertSignature: String? = null

    init {
        observePreferences()
        startWibClockTicker()
        observeRealtime()
        refreshAll(initial = true)
        startFallbackSyncLoop()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            repository.rememberedStudentFlow.collect { remembered ->
                _uiState.update { it.copy(rememberedStudent = remembered) }
            }
        }
        viewModelScope.launch {
            repository.activeTicketIdFlow.collect { savedId ->
                activeTicketId = savedId
                if (savedId != null && _uiState.value.todayQueues.isNotEmpty()) {
                    applyQueues(_uiState.value.todayQueues)
                } else if (savedId == null) {
                    _uiState.update { it.copy(activeTicket = null, peopleAhead = 0) }
                }
            }
        }
        viewModelScope.launch {
            repository.isRealtimeConnected.collect { connected ->
                _uiState.update { it.copy(isRealtimeConnected = connected) }
            }
        }
    }

    private fun startWibClockTicker() {
        viewModelScope.launch {
            while (isActive) {
                _uiState.update { state ->
                    state.copy(operationalState = WibTimeHelper.snapshot())
                }
                delay(1_000L)
            }
        }
    }

    private fun observeRealtime() {
        repository.connectRealtime()
        viewModelScope.launch {
            repository.realtimeSignals.collect { signal ->
                when (signal) {
                    RealtimeSignal.Connected,
                    RealtimeSignal.QueueChanged -> refreshQueuesOnly()
                    RealtimeSignal.Disconnected -> Unit
                }
            }
        }
    }

    private fun startFallbackSyncLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(12_000L)
                if (repository.isConfigured) {
                    refreshQueuesOnly()
                }
            }
        }
    }

    fun refreshAll(initial: Boolean = false) {
        if (!repository.isConfigured) {
            _uiState.update {
                it.copy(
                    isConfigured = false,
                    isInitialLoading = false,
                    isRefreshing = false
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isInitialLoading = initial,
                    isRefreshing = !initial,
                    errorMessage = null
                )
            }
            val servicesDeferred = async { repository.fetchServices() }
            val countersDeferred = async { repository.fetchCounters() }
            val queuesDeferred = async { repository.fetchTodayQueues() }

            val servicesRes = servicesDeferred.await()
            val countersRes = countersDeferred.await()
            val queuesRes = queuesDeferred.await()

            servicesRes.onSuccess { list -> cachedServices = list }
            countersRes.onSuccess { list -> cachedCounters = list }

            val firstError = servicesRes.exceptionOrNull()?.message
                ?: queuesRes.exceptionOrNull()?.message

            queuesRes.onSuccess { queues ->
                applyQueues(queues)
            }

            if (activeTicketId != null && _uiState.value.activeTicket == null) {
                repository.fetchTicketById(activeTicketId!!).onSuccess { fetched ->
                    if (fetched != null) {
                        val combined = (_uiState.value.todayQueues + fetched).distinctBy { it.id }
                        applyQueues(combined)
                    }
                }
            }

            _uiState.update {
                it.copy(
                    isInitialLoading = false,
                    isRefreshing = false,
                    errorMessage = firstError
                )
            }
        }
    }

    private fun refreshQueuesOnly() {
        if (!repository.isConfigured) return
        viewModelScope.launch {
            if (cachedServices.isEmpty()) {
                repository.fetchServices().onSuccess { cachedServices = it }
            }
            repository.fetchTodayQueues().onSuccess { queues ->
                applyQueues(queues)
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    private fun applyQueues(queues: List<QueueTicketDto>) {
        val summaries = cachedServices.map { service ->
            val serviceQueues = queues.filter { it.serviceId == service.id }
            val waitingCount = serviceQueues.count { it.queueStatus == QueueStatus.WAITING }
            val currentTicket = serviceQueues
                .filter { it.queueStatus == QueueStatus.IN_SERVICE || it.queueStatus == QueueStatus.CALLED }
                .maxByOrNull { it.calledAt ?: it.createdAt.orEmpty() }
            ServiceSummary(
                service = service,
                todayTotalCount = serviceQueues.size,
                waitingCount = waitingCount,
                currentNumber = currentTicket?.queueNumber
            )
        }

        val counterDisplays = cachedCounters.map { counter ->
            val serving = queues
                .filter {
                    it.counterName.equals(counter.name, ignoreCase = true) &&
                        (it.queueStatus == QueueStatus.IN_SERVICE || it.queueStatus == QueueStatus.CALLED)
                }
                .maxByOrNull { it.servedAt ?: it.calledAt ?: it.createdAt.orEmpty() }
            CounterLiveDisplay(counterName = counter.name, activeTicket = serving)
        }

        val currentActive = activeTicketId?.let { id ->
            queues.firstOrNull { it.id == id }
        }
        val activeServiceName = currentActive?.let { ticket ->
            cachedServices.firstOrNull { it.id == ticket.serviceId }?.name
                ?: "Layanan ${ticket.serviceCode}"
        }.orEmpty()

        val peopleAhead = if (currentActive != null && currentActive.queueStatus == QueueStatus.WAITING) {
            queues.count {
                it.serviceId == currentActive.serviceId &&
                    it.queueStatus == QueueStatus.WAITING &&
                    it.sequenceNumber < currentActive.sequenceNumber
            }
        } else {
            0
        }

        var newlyAlertedTicket: QueueTicketDto? = _uiState.value.callAlertTicket
        if (currentActive != null && currentActive.queueStatus == QueueStatus.CALLED) {
            val signature = "${currentActive.id}_${currentActive.status}_${currentActive.calledAt}"
            if (signature != lastAlertSignature) {
                lastAlertSignature = signature
                newlyAlertedTicket = currentActive
                audioEngine.triggerCallAlert(
                    queueNumber = currentActive.queueNumber,
                    studentName = currentActive.studentName,
                    counterName = currentActive.counterName ?: "Loket ULT"
                )
            }
        } else if (currentActive == null || currentActive.queueStatus != QueueStatus.CALLED) {
            newlyAlertedTicket = null
        }

        _uiState.update {
            it.copy(
                services = cachedServices,
                serviceSummaries = summaries,
                liveCounters = counterDisplays,
                todayQueues = queues,
                activeTicket = currentActive,
                activeTicketServiceName = activeServiceName,
                peopleAhead = peopleAhead,
                callAlertTicket = newlyAlertedTicket
            )
        }
    }

    fun submitQueueTicket(
        serviceId: String,
        studentName: String,
        studentNpm: String,
        studyProgram: String,
        onTicketReady: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmittingTicket = true,
                    errorMessage = null,
                    existingTicketConflict = null
                )
            }
            when (val outcome = repository.takeQueueTicket(serviceId, studentName, studentNpm, studyProgram)) {
                is TakeTicketOutcome.Created -> {
                    activeTicketId = outcome.ticket.id
                    val updatedList = (_uiState.value.todayQueues + outcome.ticket).distinctBy { it.id }
                    applyQueues(updatedList)
                    _uiState.update {
                        it.copy(
                            isSubmittingTicket = false,
                            snackbarMessage = "Tiket ${outcome.ticket.queueNumber} berhasil diterbitkan!"
                        )
                    }
                    onTicketReady()
                }
                is TakeTicketOutcome.AlreadyHasActive -> {
                    activeTicketId = outcome.existingTicketId
                    repository.fetchTicketById(outcome.existingTicketId).onSuccess { existing ->
                        if (existing != null) {
                            val updatedList = (_uiState.value.todayQueues + existing).distinctBy { it.id }
                            applyQueues(updatedList)
                        }
                    }
                    _uiState.update {
                        it.copy(
                            isSubmittingTicket = false,
                            existingTicketConflict = outcome.existingTicketId to outcome.message
                        )
                    }
                }
                is TakeTicketOutcome.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingTicket = false,
                            errorMessage = outcome.message
                        )
                    }
                }
            }
        }
    }

    fun openExistingConflictTicket(onNavigateToTicket: () -> Unit) {
        _uiState.update { it.copy(existingTicketConflict = null) }
        refreshQueuesOnly()
        onNavigateToTicket()
    }

    fun dismissExistingTicketConflict() {
        _uiState.update { it.copy(existingTicketConflict = null) }
    }

    fun dismissCallAlert() {
        _uiState.update { it.copy(callAlertTicket = null) }
    }

    fun replayCallAnnouncement() {
        val ticket = _uiState.value.activeTicket ?: return
        audioEngine.triggerCallAlert(
            queueNumber = ticket.queueNumber,
            studentName = ticket.studentName,
            counterName = ticket.counterName ?: "Loket ULT"
        )
    }

    fun clearFinishedTicketAndReset() {
        viewModelScope.launch {
            repository.preferences.clearActiveTicket()
            activeTicketId = null
            lastAlertSignature = null
            _uiState.update {
                it.copy(
                    activeTicket = null,
                    callAlertTicket = null,
                    peopleAhead = 0
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, snackbarMessage = null) }
    }

    class Factory(
        private val repository: QueueRepository,
        private val audioEngine: AlertAudioEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StudentQueueViewModel(repository, audioEngine) as T
        }
    }
}
