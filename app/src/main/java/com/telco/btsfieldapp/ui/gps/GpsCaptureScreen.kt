package com.telco.btsfieldapp.ui.gps

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.telco.btsfieldapp.ui.theme.PrimaryGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsCaptureScreen(
    onBack: () -> Unit,
    onNavigateBackWithResult: (latitude: String, longitude: String, altitude: String, accuracy: String, screenshotPath: String) -> Unit,
    viewModel: GpsCaptureViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var webViewReady by remember { mutableStateOf(false) }
    var captureRequested by remember { mutableStateOf(false) }

    // Track if we've already fired the callback
    var callbackFired by remember { mutableStateOf(false) }

    // When screenshot is captured, fire callback once
    LaunchedEffect(uiState.savedPhotoPath) {
        val path = uiState.savedPhotoPath ?: return@LaunchedEffect
        if (!callbackFired) {
            callbackFired = true
            onNavigateBackWithResult(
                uiState.latitude,
                uiState.longitude,
                uiState.altitude,
                uiState.gpsAccuracy,
                path
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = PrimaryGreen)
                        Text("GPS Capture", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── GPS Status Card ────────────────────────────────────────────────
            GpsStatusCard(
                latitude = uiState.latitude,
                longitude = uiState.longitude,
                accuracy = uiState.gpsAccuracy,
                altitude = uiState.altitude,
                status = uiState.locationStatus,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // ── Map WebView ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
            ) {
                OsmWebView(
                    viewModel = viewModel,
                    captureRequested = captureRequested,
                    onMapReady = { webViewReady = true },
                    onCaptureRequestConsumed = { captureRequested = false }
                )

                // Loading overlay
                if (!webViewReady) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color(0xFFF3F4F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(color = PrimaryGreen)
                            Text("Loading map...", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }

                // Map ready badge
                if (webViewReady) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color(0xFF22C55E), RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "🗺️ Map Ready",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Instructions ──────────────────────────────────────────────────
            Text(
                text = when {
                    uiState.locationStatus == "fetching" -> "Waiting for GPS signal..."
                    uiState.gpsAccuracy != "—" && uiState.gpsAccuracy.removePrefix("<").removeSuffix("m").toIntOrNull()?.let { it <= 4 } == true ->
                        "✅ GPS locked — tap Capture to save"
                    uiState.gpsAccuracy != "—" -> "Accuracy: ${uiState.gpsAccuracy} — move outdoors for better signal"
                    else -> "Capturing current location..."
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Capture Button ────────────────────────────────────────────────
            val canCapture = webViewReady && uiState.latitude != "Fetching..."
            Button(
                onClick = { captureRequested = true },
                enabled = canCapture && !uiState.isCapturing,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    disabledContainerColor = Color.LightGray
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isCapturing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Capturing...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.MyLocation, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (uiState.gpsAccuracy != "—" && uiState.gpsAccuracy.removePrefix("<").removeSuffix("m").toIntOrNull()?.let { it <= 4 } == true)
                            "📸 Capture GPS Screenshot" else "📸 Capture Screenshot",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Error message ───────────────────────────────────────────────────
            uiState.captureError?.let { err ->
                Text(
                    text = err,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun GpsStatusCard(
    latitude: String,
    longitude: String,
    accuracy: String,
    altitude: String,
    status: String,
    modifier: Modifier = Modifier
) {
    val isLocked = status == "ready"
    val isFetching = status == "fetching"
    val cardColor = when {
        isLocked && accuracy.removePrefix("<").removeSuffix("m").toIntOrNull()?.let { it <= 4 } == true -> Color(0xFFF0FDF4)
        isLocked -> Color(0xFFFFFBEB)
        else -> Color(0xFFFFF7ED)
    }
    val borderColor = when {
        isLocked && accuracy.removePrefix("<").removeSuffix("m").toIntOrNull()?.let { it <= 4 } == true -> Color(0xFF22C55E)
        isLocked -> Color(0xFFFBBF24)
        else -> Color(0xFFF97316)
    }

    Card(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("LATITUDE", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Text(latitude, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("LONGITUDE", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Text(longitude, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ACCURACY", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Text(
                    accuracy, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = when {
                        accuracy.removePrefix("<").removeSuffix("m").toIntOrNull()?.let { it <= 4 } == true -> Color(0xFF22C55E)
                        accuracy != "—" -> Color(0xFFB45309)
                        else -> Color.Gray
                    }
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("ALTITUDE", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Text(altitude.ifBlank { "—" }, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun OsmWebView(
    viewModel: GpsCaptureViewModel,
    captureRequested: Boolean,
    onMapReady: () -> Unit,
    onCaptureRequestConsumed: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    // Trigger capture when captureRequested goes true
    LaunchedEffect(captureRequested) {
        if (captureRequested) {
            webViewRef.value?.let { wv ->
                viewModel.captureScreenshot(wv)
            }
            onCaptureRequestConsumed()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            CaptureWebView(ctx).apply {
                webViewRef.value = this
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    setSupportZoom(true)
                    // Allow access to OpenStreetMap tiles
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                }

                // White background while loading
                setBackgroundColor(android.graphics.Color.WHITE)

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Register JS callback for map-ready
                        view?.evaluateJavascript("""
                            window.AndroidMapReady = function() {
                                if (window.NativeMapReady) {
                                    NativeMapReady();
                                }
                            };
                        """.trimIndent(), null)
                        // Register Android callback for map-ready
                        view?.addJavascriptInterface(object : Any() {
                            @android.webkit.JavascriptInterface
                            fun onMapReady() {
                                (ctx as? android.app.Activity)?.runOnUiThread { onMapReady() }
                            }
                        }, "NativeMapReady")
                    }
                }

                // Set the WebView reference in the ViewModel
                viewModel.setWebView(this)

                // Load OSM map HTML
                val html = viewModel.buildMapHtml()
                loadDataWithBaseURL("https://tile.openstreetmap.org/", html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            // Update map when GPS state changes
            val state = viewModel.uiState.value
            if (state.locationStatus == "ready" && state.latitude != "Fetching...") {
                val lat = state.latitude.toDoubleOrNull() ?: return@AndroidView
                val lng = state.longitude.toDoubleOrNull() ?: return@AndroidView
                val acc = state.gpsAccuracy.removePrefix("<").removeSuffix("m").toFloatOrNull()
                val alt = state.altitude.removeSuffix("m").toFloatOrNull()
                val js = "updateLocation($lat, $lng, ${acc ?: "null"}, ${alt ?: "null"});"
                webView.evaluateJavascript(js, null)
            }
        }
    )
}
