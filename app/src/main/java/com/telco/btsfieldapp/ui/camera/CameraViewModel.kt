package com.telco.btsfieldapp.ui.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telco.btsfieldapp.data.repository.SiteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class CameraUiState(
    val isInitialized: Boolean = false,
    val capturedPhotoPath: String? = null,
    val error: String? = null,
    val isCapturing: Boolean = false,
    // GPS state — "fetching" | "ready" | "unavailable"
    val locationStatus: String = "fetching",
    val latitude: String = "Fetching...",
    val longitude: String = "Fetching...",
    val gpsAccuracy: String = "—",
    val altitude: String = "—",
    val isGpsAvailable: Boolean = false,
    // Site metadata
    val siteId: String = "",
    val siteName: String = "",
    val locationSummary: String = "Kampala",
    val auditType: String = ""
)

sealed class CameraEvent {
    data class PhotoSaved(val path: String) : CameraEvent()
    data class Error(val message: String) : CameraEvent()
}

@HiltViewModel
class CameraViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val siteRepository: SiteRepository
) : ViewModel() {

    private val siteId: String = savedStateHandle.get<String>("siteId") ?: ""
    private val auditType: String = savedStateHandle.get<String>("auditType") ?: ""

    private val _uiState = MutableStateFlow(CameraUiState(siteId = siteId, auditType = auditType))
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CameraEvent>()
    val events: SharedFlow<CameraEvent> = _events.asSharedFlow()

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var locationManager: LocationManager? = null
    private var usingNetworkProvider = false

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(loc: Location) {
            _uiState.update { state ->
                state.copy(
                    latitude = "%.6f".format(loc.latitude),
                    longitude = "%.6f".format(loc.longitude),
                    gpsAccuracy = if (loc.hasAccuracy()) "<${loc.accuracy.toInt()}m" else "—",
                    altitude = if (loc.hasAltitude()) "${loc.altitude.toInt()}m" else "—",
                    isGpsAvailable = true,
                    locationStatus = "ready"
                )
            }
        }

        @Deprecated("Deprecated in API")
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {
            _uiState.update { it.copy(isGpsAvailable = false) }
        }
    }

    init {
        startLocationUpdates()
        loadSiteMetadata()
    }

    private fun loadSiteMetadata() {
        viewModelScope.launch {
            try {
                val site = siteRepository.getSiteById(siteId)
                _uiState.update { s ->
                    s.copy(
                        siteName = site?.name ?: "",
                        locationSummary = site?.address?.takeIf { it.isNotBlank() } ?: "Kampala"
                    )
                }
            } catch (_: Exception) {
                // Use defaults
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            // Step 1: Try to get cached location immediately (fastest)
            val cachedLoc = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (cachedLoc != null) {
                _uiState.update { s ->
                    s.copy(
                        latitude = "%.6f".format(cachedLoc.latitude),
                        longitude = "%.6f".format(cachedLoc.longitude),
                        gpsAccuracy = if (cachedLoc.hasAccuracy()) "<${cachedLoc.accuracy.toInt()}m" else "—",
                        altitude = if (cachedLoc.hasAltitude()) "${cachedLoc.altitude.toInt()}m" else "—",
                        isGpsAvailable = true,
                        locationStatus = "ready"
                    )
                }
                // Keep registering for live GPS updates
                requestGpsUpdates()
                return
            }

            // Step 2: No cached location — try GPS first, network as fallback
            requestGpsUpdates()
            requestNetworkUpdates() // also start network in parallel for fastest fallback

            // Give GPS up to 30 seconds to get a cold-start fix
            viewModelScope.launch {
                kotlinx.coroutines.delay(30_000)
                // GPS timed out — mark unavailable so user can still capture
                if (_uiState.value.locationStatus == "fetching") {
                    _uiState.update { it.copy(locationStatus = "unavailable") }
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Location error: ${e.message}") }
            requestNetworkUpdates()
            viewModelScope.launch {
                kotlinx.coroutines.delay(5_000)
                if (_uiState.value.locationStatus == "fetching") {
                    _uiState.update { it.copy(locationStatus = "unavailable") }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestGpsUpdates() {
        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L, 1f, locationListener
            )
        } catch (_: Exception) {
            // GPS request failed — network will serve as fallback
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNetworkUpdates() {
        usingNetworkProvider = true
        try {
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1000L, 1f, locationListener
            )
            // Try cached network location immediately
            val cached = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (cached != null) {
                _uiState.update { s ->
                    s.copy(
                        latitude = "%.6f".format(cached.latitude),
                        longitude = "%.6f".format(cached.longitude),
                        gpsAccuracy = if (cached.hasAccuracy()) "<${cached.accuracy.toInt()}m" else "—",
                        altitude = "—",
                        isGpsAvailable = true,
                        locationStatus = "ready"
                    )
                }
            }
        } catch (_: Exception) {
            // Network also failed — will be covered by GPS timeout
        }
    }

    fun initializeCamera(lifecycleOwner: LifecycleOwner, previewView: androidx.camera.view.PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.getSurfaceProvider())
                }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
                _uiState.update { it.copy(isInitialized = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Camera init failed") }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun capturePhoto() {
        val capture = imageCapture
        if (capture == null) {
            viewModelScope.launch { _events.emit(CameraEvent.Error("Camera not ready")) }
            return
        }

        val state = _uiState.value
        if (state.latitude == "Fetching...") {
            viewModelScope.launch { _events.emit(CameraEvent.Error("Still acquiring location — please wait a moment")) }
            return
        }
        // Allow capture even with N/A (no GPS) — watermark will show N/A but photo can still be taken

        _uiState.update { it.copy(isCapturing = true) }

        val tempFile = File(
            context.cacheDir,
            "temp_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    viewModelScope.launch {
                        applyWatermarkAndSave(
                            tempFile = tempFile,
                            lat = _uiState.value.latitude,
                            lng = _uiState.value.longitude,
                            siteId = _uiState.value.siteId,
                            siteName = _uiState.value.siteName,
                            locationSummary = _uiState.value.locationSummary
                        )
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    _uiState.update { it.copy(isCapturing = false) }
                    viewModelScope.launch {
                        _events.emit(CameraEvent.Error(exception.message ?: "Capture failed"))
                    }
                }
            }
        )
    }

    private suspend fun applyWatermarkAndSave(
        tempFile: File,
        lat: String,
        lng: String,
        siteId: String,
        siteName: String,
        locationSummary: String
    ) = withContext(Dispatchers.IO) {
        try {
            val options = BitmapFactory.Options().apply { inMutable = true }
            val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath, options)
                ?: throw Exception("Could not decode captured image")

            val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBitmap)
            val w = mutableBitmap.width
            val h = mutableBitmap.height

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = if (w > 1080) h * 0.022f else h * 0.035f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                setShadowLayer(2f, 1f, 1f, Color.BLACK)
            }

            val timeStr = SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date())
            val line1 = "$lat , $lng"
            val line2 = timeStr
            val line3 = if (siteName.isNotBlank()) "$siteId  |  $siteName" else siteId
            val line4 = locationSummary

            val lines = listOf(line1, line2, line3, line4)
            val lineHeight = textPaint.fontSpacing
            val paddingH = w * 0.025f
            val paddingV = h * 0.018f
            val boxWidth = lines.maxOf { textPaint.measureText(it) } + paddingH * 2
            val boxHeight = lineHeight * lines.size + paddingV * 2
            val boxLeft = w - boxWidth
            val boxTop = h - boxHeight
            val boxRect = RectF(boxLeft, boxTop, w.toFloat(), h.toFloat())

            val bgPaint = Paint().apply {
                color = Color.argb(51, 0, 0, 0)
                style = Paint.Style.FILL
            }
            canvas.drawRect(boxRect, bgPaint)

            val textX = w - paddingH
            var textY = boxTop + paddingV + textPaint.textSize
            for (line in lines) {
                canvas.drawText(line, textX, textY, textPaint)
                textY += lineHeight
            }

            val photosDir = File(context.getExternalFilesDir(null), "audit_photos").apply { mkdirs() }
            val outputFile = File(
                photosDir,
                "BTS_${siteId.ifBlank { "site" }}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
            )

            FileOutputStream(outputFile).use { out ->
                mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }

            tempFile.delete()
            mutableBitmap.recycle()
            bitmap.recycle()

            _uiState.update { it.copy(isCapturing = false, capturedPhotoPath = outputFile.absolutePath) }
            viewModelScope.launch {
                _events.emit(CameraEvent.PhotoSaved(outputFile.absolutePath))
            }

        } catch (e: Exception) {
            _uiState.update { it.copy(isCapturing = false) }
            viewModelScope.launch {
                _events.emit(CameraEvent.Error("Failed to save photo: ${e.message}"))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try { locationManager?.removeUpdates(locationListener) } catch (_: Exception) {}
        cameraProvider?.unbindAll()
    }
}
