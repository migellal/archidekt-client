package pl.michalgellert.archidektclient.interceptor

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NetworkLog(
    val timestamp: String,
    val method: String,
    val url: String,
    val requestHeaders: Map<String, String> = emptyMap(),
    val requestBody: String? = null,
    val responseCode: Int? = null,
    val responseBody: String? = null,
    val isApiRequest: Boolean = false
)

object NetworkLogger {
    private const val TAG = "ArchidektAPI"

    private val _logs = MutableStateFlow<List<NetworkLog>>(emptyList())
    val logs: StateFlow<List<NetworkLog>> = _logs.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun logRequest(
        method: String,
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null
    ) {
        val isApi = url.contains("/api/")
        val log = NetworkLog(
            timestamp = dateFormat.format(Date()),
            method = method,
            url = url,
            requestHeaders = headers,
            requestBody = body,
            isApiRequest = isApi
        )

        _logs.value = _logs.value + log

        // Log do Logcat dla łatwego podglądu
        if (isApi) {
            Log.d(TAG, "═══════════════════════════════════════════")
            Log.d(TAG, "🔵 API REQUEST: $method $url")
            if (headers.isNotEmpty()) {
                Log.d(TAG, "📋 Headers: $headers")
            }
            if (!body.isNullOrBlank()) {
                Log.d(TAG, "📦 Body: $body")
            }
            Log.d(TAG, "═══════════════════════════════════════════")
        }
    }

    fun logResponse(url: String, responseCode: Int, body: String?) {
        val isApi = url.contains("/api/")

        // Zaktualizuj ostatni log z tym URL
        _logs.value = _logs.value.map { log ->
            if (log.url == url && log.responseCode == null) {
                log.copy(responseCode = responseCode, responseBody = body)
            } else {
                log
            }
        }

        if (isApi) {
            Log.d(TAG, "🟢 API RESPONSE [$responseCode]: $url")
            if (!body.isNullOrBlank()) {
                // Truncate long responses
                val truncated = if (body.length > 2000) body.take(2000) + "..." else body
                Log.d(TAG, "📦 Response: $truncated")
            }
        }
    }

    fun clear() {
        _logs.value = emptyList()
    }

    fun getApiLogs(): List<NetworkLog> = _logs.value.filter { it.isApiRequest }
}