package pl.michalgellert.archidektclient.interceptor

import android.graphics.Bitmap
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.io.ByteArrayInputStream

class ApiInterceptorWebViewClient(
    private val onPageStarted: (String) -> Unit = {},
    private val onPageFinished: (String) -> Unit = {}
) : WebViewClient() {

    companion object {
        private const val TAG = "ApiInterceptor"
        private val API_PATTERNS = listOf(
            "/api/",
            "/rest-auth/",
            "/accounts/"
        )
    }

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.v(TAG, message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        url?.let { onPageStarted(it) }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        url?.let { onPageFinished(it) }
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)
        val method = request.method ?: "GET"

        // Sprawdź czy to request do API
        val isApiRequest = API_PATTERNS.any { url.contains(it) }

        if (isApiRequest) {
            return interceptApiRequest(request, url, method)
        }

        return super.shouldInterceptRequest(view, request)
    }

    private fun interceptApiRequest(
        request: WebResourceRequest,
        url: String,
        method: String
    ): WebResourceResponse? {
        try {
            // Zbierz headery
            val headers = mutableMapOf<String, String>()
            request.requestHeaders?.forEach { (key, value) ->
                headers[key] = value
            }

            // Dodaj cookies
            val cookies = CookieManager.getInstance().getCookie(url)
            if (!cookies.isNullOrBlank()) {
                headers["Cookie"] = cookies
            }

            // Loguj request
            NetworkLogger.logRequest(
                method = method,
                url = url,
                headers = headers
            )

            // Wykonaj request przez OkHttp (tylko dla GET, żeby nie zepsuć POST/PUT)
            if (method == "GET") {
                val requestBuilder = Request.Builder()
                    .url(url)
                    .method(method, null)

                headers.forEach { (key, value) ->
                    requestBuilder.addHeader(key, value)
                }

                val response = okHttpClient.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string()

                // Loguj response
                NetworkLogger.logResponse(url, response.code, responseBody)

                // Zwróć response do WebView
                return WebResourceResponse(
                    response.header("Content-Type") ?: "application/json",
                    response.header("Content-Encoding") ?: "UTF-8",
                    response.code,
                    response.message.ifEmpty { "OK" },
                    response.headers.toMap(),
                    ByteArrayInputStream(responseBody?.toByteArray() ?: ByteArray(0))
                )
            }

            // Dla POST/PUT/DELETE nie interceptujemy - pozwalamy WebView obsłużyć
            // (nie możemy łatwo pobrać body z WebResourceRequest)
            Log.d(TAG, "⚠️ Non-GET API request detected: $method $url")
            Log.d(TAG, "Headers: ${request.requestHeaders}")

        } catch (e: Exception) {
            Log.e(TAG, "Error intercepting request: ${e.message}", e)
        }

        return super.shouldInterceptRequest(null, request)
    }

    private fun okhttp3.Headers.toMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (i in 0 until size) {
            map[name(i)] = value(i)
        }
        return map
    }
}