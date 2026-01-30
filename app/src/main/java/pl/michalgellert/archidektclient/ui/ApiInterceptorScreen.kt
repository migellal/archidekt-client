package pl.michalgellert.archidektclient.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import pl.michalgellert.archidektclient.interceptor.ApiInterceptorWebViewClient
import pl.michalgellert.archidektclient.interceptor.NetworkLog
import pl.michalgellert.archidektclient.interceptor.NetworkLogger

@Composable
fun ApiInterceptorScreen(
    modifier: Modifier = Modifier
) {
    var showLogs by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf("https://archidekt.com") }
    var isLoading by remember { mutableStateOf(false) }
    val logs by NetworkLogger.logs.collectAsState()
    val apiLogs = logs.filter { it.isApiRequest }

    Column(modifier = modifier.fillMaxSize()) {
        // URL Bar
        UrlBar(
            url = currentUrl,
            isLoading = isLoading,
            apiLogCount = apiLogs.size,
            showLogs = showLogs,
            onToggleLogs = { showLogs = !showLogs },
            onClearLogs = { NetworkLogger.clear() }
        )

        // Content
        Box(modifier = Modifier.weight(1f)) {
            // WebView
            ArchidektWebView(
                modifier = Modifier.fillMaxSize(),
                onUrlChanged = { currentUrl = it },
                onLoadingChanged = { isLoading = it }
            )

            // Log panel (overlay)
            if (showLogs && apiLogs.isNotEmpty()) {
                LogPanel(
                    logs = apiLogs,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(300.dp)
                )
            }
        }
    }
}

@Composable
private fun UrlBar(
    url: String,
    isLoading: Boolean,
    apiLogCount: Int,
    showLogs: Boolean,
    onToggleLogs: () -> Unit,
    onClearLogs: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // URL display
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF9800))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // API log counter badge
            Surface(
                color = if (apiLogCount > 0) Color(0xFF2196F3) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.clickable { onToggleLogs() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "API",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (apiLogCount > 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = apiLogCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (apiLogCount > 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Clear button
            if (apiLogCount > 0) {
                IconButton(onClick = onClearLogs) {
                    Text("🗑️", fontSize = 16.sp)
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ArchidektWebView(
    modifier: Modifier = Modifier,
    onUrlChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Enable debugging for Chrome DevTools
                WebView.setWebContentsDebuggingEnabled(true)

                // Configure WebView settings
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    userAgentString = settings.userAgentString.replace("; wv", "")
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }

                // Enable cookies
                val webView = this
                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    setAcceptThirdPartyCookies(webView, true)
                }

                // Set custom WebViewClient with interceptor
                webViewClient = ApiInterceptorWebViewClient(
                    onPageStarted = { url ->
                        onUrlChanged(url)
                        onLoadingChanged(true)
                    },
                    onPageFinished = { url ->
                        onUrlChanged(url)
                        onLoadingChanged(false)
                    }
                )

                // Load Archidekt
                loadUrl("https://archidekt.com")
            }
        },
        modifier = modifier
    )
}

@Composable
private fun LogPanel(
    logs: List<NetworkLog>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current

    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Surface(
        modifier = modifier,
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        shadowElevation = 8.dp
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔍 API Requests (${logs.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
                Text(
                    text = "Tap to copy",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            HorizontalDivider(color = Color(0xFF333333))

            // Logs list
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(logs) { log ->
                    LogItem(
                        log = log,
                        onClick = {
                            val text = buildString {
                                appendLine("${log.method} ${log.url}")
                                if (log.requestHeaders.isNotEmpty()) {
                                    appendLine("Headers: ${log.requestHeaders}")
                                }
                                if (!log.requestBody.isNullOrBlank()) {
                                    appendLine("Request Body: ${log.requestBody}")
                                }
                                if (log.responseCode != null) {
                                    appendLine("Response: ${log.responseCode}")
                                }
                                if (!log.responseBody.isNullOrBlank()) {
                                    appendLine("Response Body: ${log.responseBody}")
                                }
                            }
                            clipboardManager.setText(AnnotatedString(text))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LogItem(
    log: NetworkLog,
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                expanded = !expanded
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Method badge
            val methodColor = when (log.method) {
                "GET" -> Color(0xFF4CAF50)
                "POST" -> Color(0xFF2196F3)
                "PUT" -> Color(0xFFFF9800)
                "DELETE" -> Color(0xFFF44336)
                else -> Color.Gray
            }

            Surface(
                color = methodColor,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = log.method,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Timestamp
            Text(
                text = log.timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Response code
            log.responseCode?.let { code ->
                val codeColor = when {
                    code in 200..299 -> Color(0xFF4CAF50)
                    code in 300..399 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                }
                Text(
                    text = "[$code]",
                    style = MaterialTheme.typography.labelSmall,
                    color = codeColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // URL (simplified path)
        val displayUrl = log.url.substringAfter("archidekt.com")
        Text(
            text = displayUrl,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFBBBBBB),
            fontFamily = FontFamily.Monospace,
            maxLines = if (expanded) Int.MAX_VALUE else 1,
            overflow = TextOverflow.Ellipsis
        )

        // Expanded details
        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))

            // Headers
            if (log.requestHeaders.isNotEmpty()) {
                Text(
                    text = "Headers:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF888888)
                )
                log.requestHeaders.forEach { (key, value) ->
                    Text(
                        text = "  $key: $value",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }

            // Response body preview
            log.responseBody?.let { body ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Response:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF888888)
                )
                Text(
                    text = if (body.length > 500) body.take(500) + "..." else body,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = Color(0xFF333333)
        )
    }
}