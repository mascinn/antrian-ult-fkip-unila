package com.example.data.model

import com.squareup.moshi.Json

enum class QueueStatus(val raw: String, val labelId: String) {
    WAITING("waiting", "Menunggu Panggilan"),
    CALLED("called", "Dipanggil ke Loket"),
    IN_SERVICE("in_service", "Sedang Dilayani"),
    COMPLETED("completed", "Pelayanan Selesai"),
    SKIPPED("skipped", "Dilewati / Tidak Hadir");

    val isActive: Boolean
        get() = this == WAITING || this == CALLED || this == IN_SERVICE

    companion object {
        fun fromRaw(value: String?): QueueStatus =
            entries.firstOrNull { it.raw.equals(value, ignoreCase = true) } ?: WAITING
    }
}

data class ServiceDto(
    val id: String,
    val code: String,
    val name: String,
    val description: String? = null,
    @Json(name = "quota_per_day") val quotaPerDay: Int = 50,
    @Json(name = "is_active") val isActive: Boolean = true
)

data class CounterDto(
    val id: Int,
    val name: String,
    @Json(name = "is_active") val isActive: Boolean = true
)

data class QueueTicketDto(
    val id: String,
    @Json(name = "service_id") val serviceId: String,
    @Json(name = "queue_number") val queueNumber: String,
    @Json(name = "sequence_number") val sequenceNumber: Int,
    @Json(name = "student_name") val studentName: String,
    @Json(name = "student_npm") val studentNpm: String,
    @Json(name = "student_study_program") val studentStudyProgram: String,
    val status: String,
    @Json(name = "counter_name") val counterName: String? = null,
    @Json(name = "called_by") val calledBy: String? = null,
    @Json(name = "queue_date") val queueDate: String? = null,
    @Json(name = "called_at") val calledAt: String? = null,
    @Json(name = "served_at") val servedAt: String? = null,
    @Json(name = "completed_at") val completedAt: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
) {
    val queueStatus: QueueStatus
        get() = QueueStatus.fromRaw(status)

    val serviceCode: String
        get() = queueNumber.substringBefore("-").trim().uppercase()
}

data class TakeTicketRpcRequest(
    @Json(name = "p_service_id") val serviceId: String,
    @Json(name = "p_student_name") val studentName: String,
    @Json(name = "p_student_npm") val studentNpm: String,
    @Json(name = "p_study_program") val studyProgram: String
)

data class TakeTicketRpcResponse(
    val success: Boolean = false,
    val message: String? = null,
    @Json(name = "existing_ticket_id") val existingTicketId: String? = null,
    val data: QueueTicketDto? = null
)

data class CallTicketPatch(
    val status: String = QueueStatus.CALLED.raw,
    @Json(name = "counter_name") val counterName: String,
    @Json(name = "called_by") val calledBy: String,
    @Json(name = "called_at") val calledAt: String
)

data class RecallTicketPatch(
    @Json(name = "called_at") val calledAt: String
)

data class StartServicePatch(
    val status: String = QueueStatus.IN_SERVICE.raw,
    @Json(name = "served_at") val servedAt: String
)

data class CompleteServicePatch(
    val status: String = QueueStatus.COMPLETED.raw,
    @Json(name = "completed_at") val completedAt: String
)

data class SkipTicketPatch(
    val status: String = QueueStatus.SKIPPED.raw,
    @Json(name = "completed_at") val completedAt: String
)

data class StaffLoginRequest(
    val email: String,
    val password: String
)

data class StaffRefreshRequest(
    @Json(name = "refresh_token") val refreshToken: String
)

data class SupabaseUserDto(
    val id: String,
    val email: String? = null
)

data class SupabaseAuthResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
    val user: SupabaseUserDto
)

data class ProfileDto(
    val id: String,
    @Json(name = "full_name") val fullName: String,
    val role: String
)

data class SupabaseErrorDto(
    val message: String? = null,
    val error: String? = null,
    @Json(name = "error_description") val errorDescription: String? = null,
    val msg: String? = null,
    val hint: String? = null
)

data class ServiceSummary(
    val service: ServiceDto,
    val todayTotalCount: Int,
    val waitingCount: Int,
    val currentNumber: String?
) {
    val remainingQuota: Int
        get() = (service.quotaPerDay - todayTotalCount).coerceAtLeast(0)

    val isFull: Boolean
        get() = remainingQuota <= 0
}

data class CounterLiveDisplay(
    val counterName: String,
    val activeTicket: QueueTicketDto?
)

data class StaffSession(
    val userId: String,
    val email: String,
    val fullName: String,
    val role: String,
    val accessToken: String,
    val refreshToken: String
)

data class RememberedStudentInfo(
    val name: String = "",
    val npm: String = "",
    val studyProgram: String = ""
)

sealed interface TakeTicketOutcome {
    data class Created(val ticket: QueueTicketDto, val message: String) : TakeTicketOutcome
    data class AlreadyHasActive(val existingTicketId: String, val message: String) : TakeTicketOutcome
    data class Error(val message: String) : TakeTicketOutcome
}
