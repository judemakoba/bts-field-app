package com.telco.btsfieldapp.ui.camera

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onBack: (photoPath: String?) -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    val previewViewState = remember { mutableStateOf<PreviewView?>(null) }

    // Permission launchers
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasCameraPermission = perms[Manifest.permission.CAMERA] == true
    }

    // Request permissions on launch
    LaunchedEffect(Unit) {
        cameraLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Show errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // Listen for photo saved — save to prefs and go back
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is CameraEvent.PhotoSaved -> {
                    // Save to SharedPreferences for the calling screen to pick up
                    val prefs = context.getSharedPreferences("photo_results", Context.MODE_PRIVATE)
                    val key = "${uiState.siteId}__${uiState.auditType}"
                    prefs.edit().putString(key, event.path).apply()
                    onBack(event.path)
                }
                is CameraEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    // Initialize camera once permission is granted and PreviewView is created
    LaunchedEffect(hasCameraPermission) {
        snapshotFlow { previewViewState.value }
            .filterNotNull()
            .collect { pv ->
                if (hasCameraPermission && !uiState.isInitialized) {
                    viewModel.initializeCamera(lifecycleOwner, pv)
                }
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!hasCameraPermission) {
            // Permission denied screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ComposeColor.Black)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Camera & Location Access Required",
                    style = MaterialTheme.typography.titleLarge,
                    color = ComposeColor.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Please grant Camera and Location permissions to capture photos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ComposeColor.White.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        cameraLauncher.launch(
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ComposeColor(0xFF22C55E))
                ) { Text("Grant Permissions") }
            }
        } else {
            // Camera preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }.also { previewViewState.value = it }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Top gradient overlay (for GPS info readability)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                ComposeColor.Black.copy(alpha = 0.75f),
                                ComposeColor.Transparent
                            )
                        )
                    )
            )

            // GPS info overlay
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                GpsInfoCard(
                    latitude = uiState.latitude,
                    longitude = uiState.longitude,
                    accuracy = uiState.gpsAccuracy,
                    altitude = uiState.altitude,
                    siteId = uiState.siteId,
                    siteName = uiState.siteName,
                    locationSummary = uiState.locationSummary
                )
            }

            // Close button
            IconButton(
                onClick = { onBack(null) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(8.dp)
                    .size(40.dp)
                    .background(ComposeColor.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = ComposeColor.White
                )
            }

            // Bottom gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                ComposeColor.Transparent,
                                ComposeColor.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            )

            // Capture button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp)
            ) {
                CaptureButton(
                    enabled = uiState.latitude != "Fetching..." &&
                            uiState.latitude != "N/A" &&
                            uiState.isInitialized &&
                            !uiState.isCapturing,
                    isCapturing = uiState.isCapturing,
                    gpsReady = uiState.isGpsAvailable,
                    onClick = { viewModel.capturePhoto() }
                )
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 140.dp)
        )
    }
}

@Composable
private fun GpsInfoCard(
    latitude: String,
    longitude: String,
    accuracy: String,
    altitude: String,
    siteId: String,
    siteName: String,
    locationSummary: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ComposeColor.Black.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Lat: $latitude",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = ComposeColor.White
                    )
                    Text(
                        text = "Lng: $longitude",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = ComposeColor.White
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = accuracy,
                        style = MaterialTheme.typography.bodySmall,
                        color = ComposeColor.White.copy(alpha = 0.7f)
                    )
                    if (altitude != "—") {
                        Text(
                            text = altitude,
                            style = MaterialTheme.typography.bodySmall,
                            color = ComposeColor.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            if (siteId.isNotBlank() || siteName.isNotBlank()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = ComposeColor.White.copy(alpha = 0.2f)
                )
                Text(
                    text = if (siteName.isNotBlank()) "$siteId  |  $siteName" else siteId,
                    style = MaterialTheme.typography.bodySmall,
                    color = ComposeColor.White.copy(alpha = 0.85f)
                )
                if (locationSummary.isNotBlank()) {
                    Text(
                        text = locationSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = ComposeColor.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptureButton(
    enabled: Boolean,
    isCapturing: Boolean,
    gpsReady: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (!gpsReady) {
            Text(
                text = "Acquiring GPS signal...",
                style = MaterialTheme.typography.labelSmall,
                color = ComposeColor.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else if (enabled) {
            Text(
                text = "Tap to capture",
                style = MaterialTheme.typography.labelSmall,
                color = ComposeColor.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    if (enabled) ComposeColor.White else ComposeColor.Gray
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCapturing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = ComposeColor(0xFF22C55E),
                    strokeWidth = 3.dp
                )
            } else {
                IconButton(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                if (enabled) ComposeColor(0xFF22C55E) else ComposeColor.Gray.copy(alpha = 0.5f)
                            )
                    )
                }
            }
        }
    }
}
