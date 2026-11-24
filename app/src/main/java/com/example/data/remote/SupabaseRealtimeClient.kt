package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

sealed interface RealtimeSignal {
    data object Connected : RealtimeSignal
    data object QueueChanged : RealtimeSignal
    data object Disconnected : RealtimeSignal
}

/**
 * Lightweight Supabase Realtime client using OkHttp WebSocket and Phoenix Channel protocol
 * subscribing to postgres_changes on public.queues.
 */
class SupabaseRealtimeClient(
    private val okHttpClient: OkHttpClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refCounter = AtomicInteger(1)
    private val shouldStayConnected = AtomicBoolean(false)

    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0

    private val _signals = MutableSharedFlow<RealtimeSignal>(extraBufferCapacity = 32)
    val signals: SharedFlow<RealtimeSignal> = _signals.asSharedFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun connect() {
        val wsUrl = SupabaseConfig.realtimeWebSocketUrl() ?: return
        if (shouldStayConnected.getAndSet(true) && webSocket != null) return
        openSocket(wsUrl)
    }

    fun disconnect() {
        shouldStayConnected.set(false)
        heartbeatJob?.cancel()
        reconnectJob?.cancel()
        webSocket?.close(1000, "Client closed")
        webSocket = null
        _isConnected.value = false
    }

    private fun openSocket(wsUrl: String) {
        reconnectJob?.cancel()
        heartbeatJob?.cancel()
        webSocket?.cancel()

        val request = Request.Builder().url(wsUrl).build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                reconnectAttempt = 0
                _isConnected.value = true
                _signals.tryEmit(RealtimeSignal.Connected)
                sendJoinQueuesChannel(ws)
                startHeartbeat(ws)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val event = json.optString("event")
                    if (event == "postgres_changes") {
                        _signals.tryEmit(RealtimeSignal.QueueChanged)
                    }
                } catch (e: Exception) {
                    Log.w("SupabaseRealtime", "Ignored malformed WS payload")
                }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                handleDisconnect()
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.w("SupabaseRealtime", "WebSocket disconnected: ${t.message}")
                handleDisconnect()
            }
        })
    }

    private fun sendJoinQueuesChannel(ws: WebSocket) {
        val pgChange = JSONObject().apply {
            put("event", "*")
            put("schema", "public")
            put("table", "queues")
        }
        val config = JSONObject().apply {
            put("broadcast", JSONObject().put("self", false))
            put("presence", JSONObject().put("key", ""))
            put("postgres_changes", JSONArray().put(pgChange))
        }
        val payload = JSONObject().apply {
            put("config", config)
        }
        val joinFrame = JSONObject().apply {
            put("topic", "realtime:public:queues")
            put("event", "phx_join")
            put("payload", payload)
            put("ref", refCounter.getAndIncrement().toString())
        }
        ws.send(joinFrame.toString())
    }

    private fun startHeartbeat(ws: WebSocket) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && shouldStayConnected.get()) {
                delay(25_000L)
                val hb = JSONObject().apply {
                    put("topic", "phoenix")
                    put("event", "heartbeat")
                    put("payload", JSONObject())
                    put("ref", refCounter.getAndIncrement().toString())
                }
                ws.send(hb.toString())
            }
        }
    }

    private fun handleDisconnect() {
        heartbeatJob?.cancel()
        webSocket = null
        _isConnected.value = false
        _signals.tryEmit(RealtimeSignal.Disconnected)

        if (!shouldStayConnected.get()) return
        val wsUrl = SupabaseConfig.realtimeWebSocketUrl() ?: return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val backoffMs = (2_000L * (1 shl reconnectAttempt.coerceAtMost(4))).coerceAtMost(30_000L)
            reconnectAttempt++
            delay(backoffMs)
            if (shouldStayConnected.get()) {
                openSocket(wsUrl)
            }
        }
    }
}
