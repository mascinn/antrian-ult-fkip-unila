package com.example.data.remote

import com.example.data.model.CallTicketPatch
import com.example.data.model.CompleteServicePatch
import com.example.data.model.CounterDto
import com.example.data.model.ProfileDto
import com.example.data.model.QueueTicketDto
import com.example.data.model.RecallTicketPatch
import com.example.data.model.ServiceDto
import com.example.data.model.SkipTicketPatch
import com.example.data.model.StaffLoginRequest
import com.example.data.model.StaffRefreshRequest
import com.example.data.model.StartServicePatch
import com.example.data.model.SupabaseAuthResponse
import com.example.data.model.TakeTicketRpcRequest
import com.example.data.model.TakeTicketRpcResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseApiService {
    @GET("rest/v1/services")
    suspend fun getActiveServices(
        @Query("select") select: String = "*",
        @Query("is_active") isActive: String = "eq.true",
        @Query("order") order: String = "code.asc"
    ): Response<List<ServiceDto>>

    @GET("rest/v1/counters")
    suspend fun getActiveCounters(
        @Query("select") select: String = "*",
        @Query("is_active") isActive: String = "eq.true",
        @Query("order") order: String = "id.asc"
    ): Response<List<CounterDto>>

    @GET("rest/v1/queues")
    suspend fun getTodayQueues(
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.asc"
    ): Response<List<QueueTicketDto>>

    @GET("rest/v1/queues")
    suspend fun getQueueById(
        @Query("select") select: String = "*",
        @Query("id") idFilter: String
    ): Response<List<QueueTicketDto>>

    @POST("rest/v1/rpc/take_queue_ticket")
    suspend fun takeQueueTicket(
        @Body request: TakeTicketRpcRequest
    ): Response<TakeTicketRpcResponse>

    @POST("auth/v1/token")
    suspend fun loginStaff(
        @Query("grant_type") grantType: String = "password",
        @Body request: StaffLoginRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/token")
    suspend fun refreshStaffToken(
        @Query("grant_type") grantType: String = "refresh_token",
        @Body request: StaffRefreshRequest
    ): Response<SupabaseAuthResponse>

    @GET("rest/v1/profiles")
    suspend fun getProfileById(
        @Header("Authorization") bearer: String,
        @Query("select") select: String = "*",
        @Query("id") idFilter: String
    ): Response<List<ProfileDto>>

    @PATCH("rest/v1/queues")
    suspend fun callTicket(
        @Header("Authorization") bearer: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Query("id") idFilter: String,
        @Body body: CallTicketPatch
    ): Response<List<QueueTicketDto>>

    @PATCH("rest/v1/queues")
    suspend fun recallTicket(
        @Header("Authorization") bearer: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Query("id") idFilter: String,
        @Body body: RecallTicketPatch
    ): Response<List<QueueTicketDto>>

    @PATCH("rest/v1/queues")
    suspend fun startService(
        @Header("Authorization") bearer: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Query("id") idFilter: String,
        @Body body: StartServicePatch
    ): Response<List<QueueTicketDto>>

    @PATCH("rest/v1/queues")
    suspend fun completeService(
        @Header("Authorization") bearer: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Query("id") idFilter: String,
        @Body body: CompleteServicePatch
    ): Response<List<QueueTicketDto>>

    @PATCH("rest/v1/queues")
    suspend fun skipTicket(
        @Header("Authorization") bearer: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Query("id") idFilter: String,
        @Body body: SkipTicketPatch
    ): Response<List<QueueTicketDto>>
}
