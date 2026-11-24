package com.example.data.repository

import com.example.core.WibTimeHelper
import com.example.data.local.LocalPreferencesStore
import com.example.data.model.CallTicketPatch
import com.example.data.model.CompleteServicePatch
import com.example.data.model.CounterDto
import com.example.data.model.QueueTicketDto
import com.example.data.model.RecallTicketPatch
import com.example.data.model.RememberedStudentInfo
import com.example.data.model.ServiceDto
import com.example.data.model.SkipTicketPatch
import com.example.data.model.StaffLoginRequest
import com.example.data.model.StaffRefreshRequest
import com.example.data.model.StaffSession
import com.example.data.model.StartServicePatch
import com.example.data.model.SupabaseErrorDto
import com.example.data.model.TakeTicketOutcome
import com.example.data.model.TakeTicketRpcRequest
import com.example.data.remote.RealtimeSignal
import com.example.data.remote.SupabaseApiService
import com.example.data.remote.SupabaseConfig
import com.example.data.remote.SupabaseRealtimeClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class QueueRepository(
    val preferences: LocalPreferencesStore
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val errorAdapter = moshi.adapter(SupabaseErrorDto::class.java)

    private val supabaseHeaderInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
            .header("apikey", SupabaseConfig.publishableKey)
            .header("Accept", "application/json")
        if (original.header("Content-Type") == null && original.body != null) {
            builder.header("Content-Type", "application/json")
        }
        // Per Supabase publishable key rules, sb_publishable_... is ONLY sent in `apikey`,
        // while `Authorization: Bearer <jwt>` is reserved for authenticated Staff sessions.
        chain.proceed(builder.build())
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(supabaseHeaderInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private val api: SupabaseApiService = Retrofit.Builder()
        .baseUrl(SupabaseConfig.retrofitBaseUrl())
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(SupabaseApiService::class.java)

    private val realtimeClient = SupabaseRealtimeClient(okHttpClient)

    val realtimeSignals: SharedFlow<RealtimeSignal> = realtimeClient.signals
    val isRealtimeConnected: StateFlow<Boolean> = realtimeClient.isConnected
    val isConfigured: Boolean
        get() = SupabaseConfig.isConfigured

    val activeTicketIdFlow: Flow<String?> = preferences.activeTicketIdFlow
    val simulationOpenModeFlow: Flow<Boolean> = preferences.simulationOpenModeFlow
    val rememberedStudentFlow: Flow<RememberedStudentInfo> = preferences.rememberedStudentFlow
    val staffSessionFlow: Flow<StaffSession?> = preferences.staffSessionFlow
    val selectedCounterFlow: Flow<String> = preferences.selectedCounterFlow

    fun connectRealtime() {
        if (isConfigured) {
            realtimeClient.connect()
        }
    }

    fun disconnectRealtime() {
        realtimeClient.disconnect()
    }

    suspend fun fetchServices(): Result<List<ServiceDto>> = safeApiCall {
        val response = api.getActiveServices()
        if (response.isSuccessful) {
            Result.success(response.body().orEmpty())
        } else {
            Result.failure(Exception(parseErrorMessage(response, "Gagal memuat daftar layanan ULT.")))
        }
    }

    suspend fun fetchCounters(): Result<List<CounterDto>> = safeApiCall {
        val response = api.getActiveCounters()
        if (response.isSuccessful) {
            val list = response.body().orEmpty()
            if (list.isNotEmpty()) Result.success(list) else Result.success(defaultCounters())
        } else {
            Result.success(defaultCounters())
        }
    }

    suspend fun fetchTodayQueues(): Result<List<QueueTicketDto>> = safeApiCall {
        val response = api.getTodayQueues()
        if (response.isSuccessful) {
            val rows = response.body().orEmpty()
            Result.success(rows)
        } else {
            Result.failure(Exception(parseErrorMessage(response, "Gagal memuat antrean hari ini.")))
        }
    }

    suspend fun fetchTicketById(ticketId: String): Result<QueueTicketDto?> = safeApiCall {
        val response = api.getQueueById(idFilter = "eq.$ticketId")
        if (response.isSuccessful) {
            Result.success(response.body().orEmpty().firstOrNull())
        } else {
            Result.failure(Exception(parseErrorMessage(response, "Gagal memuat detail tiket.")))
        }
    }

    suspend fun takeQueueTicket(
        serviceId: String,
        studentName: String,
        studentNpm: String,
        studyProgram: String
    ): TakeTicketOutcome {
        if (!isConfigured) {
            return TakeTicketOutcome.Error(
                "Konfigurasi Supabase belum diatur. Tambahkan SUPABASE_URL dan SUPABASE_PUBLISHABLE_KEY pada panel Secrets AI Studio."
            )
        }
        return try {
            val response = api.takeQueueTicket(
                TakeTicketRpcRequest(
                    serviceId = serviceId,
                    studentName = studentName.trim(),
                    studentNpm = studentNpm.trim(),
                    studyProgram = studyProgram.trim()
                )
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body == null) {
                    TakeTicketOutcome.Error("Respons server kosong saat mengambil tiket.")
                } else if (body.success && body.data != null) {
                    preferences.saveActiveTicket(body.data.id)
                    preferences.saveStudentInfo(studentName, studentNpm, studyProgram)
                    TakeTicketOutcome.Created(
                        ticket = body.data,
                        message = body.message ?: "Nomor antrean berhasil diambil."
                    )
                } else if (!body.existingTicketId.isNullOrBlank()) {
                    preferences.saveActiveTicket(body.existingTicketId)
                    preferences.saveStudentInfo(studentName, studentNpm, studyProgram)
                    TakeTicketOutcome.AlreadyHasActive(
                        existingTicketId = body.existingTicketId,
                        message = body.message ?: "Anda masih memiliki tiket antrean aktif hari ini."
                    )
                } else {
                    TakeTicketOutcome.Error(body.message ?: "Tidak dapat mengambil nomor antrean.")
                }
            } else {
                TakeTicketOutcome.Error(parseErrorMessage(response, "Gagal mengambil nomor antrean."))
            }
        } catch (e: IOException) {
            TakeTicketOutcome.Error("Koneksi internet bermasalah. Periksa jaringan Anda lalu coba lagi.")
        } catch (e: Exception) {
            TakeTicketOutcome.Error(e.message ?: "Terjadi kesalahan saat mengambil tiket.")
        }
    }

    suspend fun loginStaff(email: String, password: String): Result<StaffSession> = safeApiCall {
        val authRes = api.loginStaff(
            request = StaffLoginRequest(email = email.trim(), password = password)
        )
        if (!authRes.isSuccessful) {
            return@safeApiCall Result.failure(
                Exception(parseErrorMessage(authRes, "Email atau kata sandi petugas tidak valid."))
            )
        }
        val authBody = authRes.body()
            ?: return@safeApiCall Result.failure(Exception("Respons autentikasi tidak valid."))

        val bearer = "Bearer ${authBody.accessToken}"
        val profileRes = api.getProfileById(bearer = bearer, idFilter = "eq.${authBody.user.id}")
        if (!profileRes.isSuccessful) {
            return@safeApiCall Result.failure(
                Exception(parseErrorMessage(profileRes, "Gagal memverifikasi profil petugas."))
            )
        }
        val profile = profileRes.body().orEmpty().firstOrNull()
        if (profile == null || (profile.role != "staff" && profile.role != "admin")) {
            return@safeApiCall Result.failure(
                Exception("Akun ini tidak terdaftar sebagai petugas ULT (role 'staff'/'admin' belum diatur di tabel profiles).")
            )
        }

        val session = StaffSession(
            userId = authBody.user.id,
            email = authBody.user.email ?: email.trim(),
            fullName = profile.fullName,
            role = profile.role,
            accessToken = authBody.accessToken,
            refreshToken = authBody.refreshToken
        )
        preferences.saveStaffSession(session)
        Result.success(session)
    }

    suspend fun logoutStaff() {
        preferences.clearStaffSession()
    }

    suspend fun callTicket(ticketId: String, counterName: String): Result<QueueTicketDto> =
        executeStaffMutation { session ->
            api.callTicket(
                bearer = "Bearer ${session.accessToken}",
                idFilter = "eq.$ticketId",
                body = CallTicketPatch(
                    counterName = counterName,
                    calledBy = session.userId,
                    calledAt = WibTimeHelper.currentIsoUtcTimestamp()
                )
            )
        }

    suspend fun recallTicket(ticketId: String): Result<QueueTicketDto> =
        executeStaffMutation { session ->
            api.recallTicket(
                bearer = "Bearer ${session.accessToken}",
                idFilter = "eq.$ticketId",
                body = RecallTicketPatch(calledAt = WibTimeHelper.currentIsoUtcTimestamp())
            )
        }

    suspend fun startService(ticketId: String): Result<QueueTicketDto> =
        executeStaffMutation { session ->
            api.startService(
                bearer = "Bearer ${session.accessToken}",
                idFilter = "eq.$ticketId",
                body = StartServicePatch(servedAt = WibTimeHelper.currentIsoUtcTimestamp())
            )
        }

    suspend fun completeService(ticketId: String): Result<QueueTicketDto> =
        executeStaffMutation { session ->
            api.completeService(
                bearer = "Bearer ${session.accessToken}",
                idFilter = "eq.$ticketId",
                body = CompleteServicePatch(completedAt = WibTimeHelper.currentIsoUtcTimestamp())
            )
        }

    suspend fun skipTicket(ticketId: String): Result<QueueTicketDto> =
        executeStaffMutation { session ->
            api.skipTicket(
                bearer = "Bearer ${session.accessToken}",
                idFilter = "eq.$ticketId",
                body = SkipTicketPatch(completedAt = WibTimeHelper.currentIsoUtcTimestamp())
            )
        }

    private suspend fun executeStaffMutation(
        call: suspend (StaffSession) -> Response<List<QueueTicketDto>>
    ): Result<QueueTicketDto> = safeApiCall {
        val currentSession = preferences.staffSessionFlow.first()
            ?: return@safeApiCall Result.failure(Exception("Sesi petugas berakhir. Silakan masuk kembali."))

        var response = call(currentSession)
        if (response.code() == 401) {
            val refreshed = refreshStaffSession(currentSession)
            if (refreshed != null) {
                response = call(refreshed)
            } else {
                preferences.clearStaffSession()
                return@safeApiCall Result.failure(Exception("Sesi petugas kedaluwarsa. Silakan login ulang."))
            }
        }

        if (response.isSuccessful) {
            val updated = response.body().orEmpty().firstOrNull()
            if (updated != null) {
                Result.success(updated)
            } else {
                Result.failure(
                    Exception("Perubahan ditolak kebijakan RLS atau tiket tidak ditemukan. Pastikan akun memiliki role staff/admin.")
                )
            }
        } else {
            Result.failure(Exception(parseErrorMessage(response, "Gagal memperbarui status antrean.")))
        }
    }

    private suspend fun refreshStaffSession(session: StaffSession): StaffSession? {
        return try {
            val refreshRes = api.refreshStaffToken(
                request = StaffRefreshRequest(refreshToken = session.refreshToken)
            )
            if (refreshRes.isSuccessful) {
                val body = refreshRes.body() ?: return null
                val updated = session.copy(
                    accessToken = body.accessToken,
                    refreshToken = body.refreshToken
                )
                preferences.saveStaffSession(updated)
                updated
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun <T> safeApiCall(block: suspend () -> Result<T>): Result<T> {
        if (!isConfigured) {
            return Result.failure(
                Exception("Konfigurasi Supabase belum diisi. Atur SUPABASE_URL dan SUPABASE_PUBLISHABLE_KEY pada panel Secrets AI Studio.")
            )
        }
        return try {
            block()
        } catch (e: IOException) {
            Result.failure(Exception("Tidak dapat terhubung ke server Supabase. Periksa koneksi internet Anda."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Terjadi kesalahan yang tidak terduga."))
        }
    }

    private fun parseErrorMessage(response: Response<*>, fallback: String): String {
        val raw = try {
            response.errorBody()?.string()
        } catch (_: Exception) {
            null
        }
        if (!raw.isNullOrBlank()) {
            try {
                val parsed = errorAdapter.fromJson(raw)
                val msg = parsed?.errorDescription ?: parsed?.message ?: parsed?.msg ?: parsed?.error
                if (!msg.isNullOrBlank()) {
                    return translateSupabaseError(msg)
                }
            } catch (_: Exception) {
                // Fall back to default message
            }
        }
        return "$fallback (HTTP ${response.code()})"
    }

    private fun translateSupabaseError(raw: String): String {
        return when {
            raw.contains("Invalid login credentials", ignoreCase = true) ->
                "Email atau kata sandi petugas salah."
            raw.contains("Email not confirmed", ignoreCase = true) ->
                "Email akun petugas belum dikonfirmasi di Supabase Auth."
            raw.contains("Could not find the table", ignoreCase = true) ||
                raw.contains("does not exist", ignoreCase = true) ->
                "Tabel/RPC belum tersedia di Supabase. Pastikan skrip SQL DDL telah dijalankan di SQL Editor."
            else -> raw
        }
    }

    private fun defaultCounters(): List<CounterDto> = listOf(
        CounterDto(1, "Loket 1", true),
        CounterDto(2, "Loket 2", true),
        CounterDto(3, "Loket 3", true)
    )
}
