package com.telco.btsfieldapp.ui.gps

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class GpsCaptureUiState(
    val locationStatus: String = "fetching", // "fetching" | "ready" | "unavailable"
    val latitude: String = "Fetching...",
    val longitude: String = "Fetching...",
    val gpsAccuracy: String = "—",
    val altitude: String = "—",
    val isMapReady: Boolean = false,
    val isCapturing: Boolean = false,
    val captureError: String? = null,
    val savedPhotoPath: String? = null,
    val siteId: String = "",
    val siteName: String = "",
)

/** Generates the HTML/JS for an OpenStreetMap WebView showing the current GPS location. */
fun buildOsmMapHtml(lat: Double?, lng: Double?, accuracy: Float?): String {
    val latVal = lat ?: 0.0
    val lngVal = lng ?: 0.0
    val accVal = accuracy ?: 0f
    return """
<!DOCTYPE html>
<html>
<head>
  <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    html, body, #map { width: 100%; height: 100%; }
    #overlay {
      position: absolute; bottom: 0; left: 0; right: 0;
      background: rgba(0,0,0,0.88); color: white; z-index: 9999;
      padding: 10px 14px; font-family: 'Courier New', monospace; font-size: 13px;
      border-top: 3px solid #4ade80;
    }
    #overlay .row { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 4px; }
    #overlay .row > div { min-width: 0; flex: 1; }
    #overlay .label { color: #86efac; font-size: 11px; font-weight: 600; letter-spacing: .05em; }
    #overlay .value { font-weight: bold; color: #ffffff;
      text-shadow: 0 1px 3px rgba(0,0,0,0.9), 0 0 8px rgba(0,0,0,0.5);
    }
    #accuracy-bar {
      width: 100%; height: 4px; background: #374151; border-radius: 2px; margin-top: 6px;
    }
    #accuracy-fill {
      height: 4px; background: #4ade80; border-radius: 2px; transition: width 0.5s;
      max-width: 100%;
    }
    #status-msg {
      text-align: center; color: #86efac; font-size: 12px; margin-top: 4px;
    }
    #crosshair {
      position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%);
      width: 30px; height: 30px; pointer-events: none; z-index: 9998;
    }
    #crosshair::before, #crosshair::after {
      content: ''; position: absolute; background: rgba(74,222,128,0.8);
    }
    #crosshair::before { width: 2px; height: 100%; left: 50%; transform: translateX(-50%); }
    #crosshair::after  { height: 2px; width: 100%; top: 50%; transform: translateY(-50%); }
    .leaflet-control-attribution { font-size: 9px !important; }
  </style>
</head>
<body>
<div id="map"></div>
<div id="crosshair"></div>
<div id="overlay">
  <div class="row">
    <div>
      <div class="label">LATITUDE</div>
      <div class="value" id="d-lat">${if (lat != null) "%.6f".format(lat) else "---"}</div>
    </div>
    <div>
      <div class="label">LONGITUDE</div>
      <div class="value" id="d-lng">${if (lng != null) "%.6f".format(lng) else "---"}</div>
    </div>
    <div>
      <div class="label">ACCURACY</div>
      <div class="value" id="d-acc">${if (accuracy != null) "<${accuracy.toInt()}m" else "---"}</div>
    </div>
    <div>
      <div class="label">ALTITUDE</div>
      <div class="value" id="d-alt">---</div>
    </div>
  </div>
  <div id="accuracy-bar"><div id="accuracy-fill" style="width:100%"></div></div>
  <div id="status-msg">Fetching GPS...</div>
</div>
<script>
  var map, marker, circle;
  function initMap(lat, lng) {
    map = L.map('map', { zoomControl: false, attributionControl: true })
      .setView([lat, lng], 18);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(map);

    // Remove default zoom buttons for cleaner look
    map.zoomControl.remove();

    // Draw accuracy circle if available
    if (typeof acc !== 'undefined' && acc > 0) {
      circle = L.circle([lat, lng], { radius: acc, color: '#4ade80', fillColor: '#4ade8030', weight: 2 }).addTo(map);
    }

    marker = L.circleMarker([lat, lng], {
      radius: 10, color: '#22c55e', fillColor: '#4ade80', weight: 3, fillOpacity: 1
    }).addTo(map);

    // Signal to Android that map is ready
    window.AndroidMapReady && window.AndroidMapReady();
  }

  function updateLocation(lat, lng, acc, alt) {
    document.getElementById('d-lat').textContent = lat.toFixed(6);
    document.getElementById('d-lng').textContent = lng.toFixed(6);
    document.getElementById('d-acc').textContent = (acc ? '<' + Math.round(acc) + 'm' : '---');
    document.getElementById('d-alt').textContent = (alt ? alt + 'm' : '---');

    // Update accuracy bar (cap at 50m for visual)
    var fill = acc ? Math.max(0, Math.min(100, (1 - (acc / 50)) * 100)) : 0;
    document.getElementById('accuracy-fill').style.width = fill + '%';
    document.getElementById('accuracy-fill').style.background = acc <= 4 ? '#4ade80' : acc <= 15 ? '#fbbf24' : '#f87171';

    var msg = acc ? (acc <= 4 ? 'GPS LOCKED — Ready to capture' : 'Accuracy: ' + Math.round(acc) + 'm — waiting...') : 'Waiting for GPS...';
    document.getElementById('status-msg').textContent = msg;

    if (map) {
      map.setView([lat, lng], map.getZoom(), { animate: true });
      if (marker) {
        marker.setLatLng([lat, lng]);
      }
      if (circle) {
        circle.setLatLng([lat, lng]);
      }
    } else {
      initMap(lat, lng);
    }
  }

  var acc = ${accuracy ?: 0f};
  var alt_val = null;
  function setAltitude(alt) { alt_val = alt; document.getElementById('d-alt').textContent = (alt ? alt + 'm' : '---'); }
  // Always init the map immediately — even before GPS arrives.
  // If no coords yet, show a neutral location (0,0). updateLocation()
  // will be called from Android once GPS arrives and will pan to the real spot.
  if (${lat != null} && ${lng != null}) {
    initMap(${lat}, ${lng});
  } else {
    initMap(0.0, 0.0);
  }
</script>
</body>
</html>
""".trim()
}

@HiltViewModel
class GpsCaptureViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GpsCaptureUiState())
    val uiState: StateFlow<GpsCaptureUiState> = _uiState.asStateFlow()

    private var locationManager: LocationManager? = null
    private var usingNetworkProvider = false
    private var webViewRef: WebView? = null

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(loc: Location) {
            _uiState.update { state ->
                state.copy(
                    latitude = "%.6f".format(loc.latitude),
                    longitude = "%.6f".format(loc.longitude),
                    gpsAccuracy = if (loc.hasAccuracy()) "<${loc.accuracy.toInt()}m" else "—",
                    altitude = if (loc.hasAltitude()) "${loc.altitude.toInt()}m" else "—",
                    locationStatus = "ready"
                )
            }
            // Update WebView map if ready
            webViewRef?.let { wv ->
                val js = "updateLocation(${loc.latitude}, ${loc.longitude}, ${loc.accuracy}, ${if (loc.hasAltitude()) loc.altitude else "null"});"
                wv.evaluateJavascript(js, null)
            }
        }
        @Deprecated("Deprecated in API")
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    init {
        startLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Step 1: Try cached location first
        val cachedLoc = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (cachedLoc != null) {
            _uiState.update { it.copy(
                latitude = "%.6f".format(cachedLoc.latitude),
                longitude = "%.6f".format(cachedLoc.longitude),
                gpsAccuracy = if (cachedLoc.hasAccuracy()) "<${cachedLoc.accuracy.toInt()}m" else "—",
                altitude = if (cachedLoc.hasAltitude()) "${cachedLoc.altitude.toInt()}m" else "—",
                locationStatus = "ready"
            )}
            requestGpsUpdates()
            requestNetworkUpdates()
            return
        }

        // No cached location — start GPS + network in parallel
        requestGpsUpdates()
        requestNetworkUpdates()

        // Timeout after 30s
        viewModelScope.launch {
            kotlinx.coroutines.delay(30_000)
            if (_uiState.value.locationStatus == "fetching") {
                _uiState.update { it.copy(locationStatus = "unavailable") }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestGpsUpdates() {
        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 1000L, 1f, locationListener
            )
        } catch (_: Exception) { /* GPS unavailable */ }
    }

    @SuppressLint("MissingPermission")
    private fun requestNetworkUpdates() {
        usingNetworkProvider = true
        try {
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 1000L, 1f, locationListener
            )
            val cached = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (cached != null && _uiState.value.locationStatus == "fetching") {
                _uiState.update { it.copy(
                    latitude = "%.6f".format(cached.latitude),
                    longitude = "%.6f".format(cached.longitude),
                    gpsAccuracy = if (cached.hasAccuracy()) "<${cached.accuracy.toInt()}m" else "—",
                    altitude = "—",
                    locationStatus = "ready"
                )}
            }
        } catch (_: Exception) { /* Network unavailable */ }
    }

    /** Called from GpsCaptureScreen once the WebView is ready so we can push JS updates to it. */
    fun setWebView(webView: WebView) {
        webViewRef = webView
    }

    fun buildMapHtml(): String {
        val state = _uiState.value
        val lat = state.latitude.toDoubleOrNull()
        val lng = state.longitude.toDoubleOrNull()
        val acc = state.gpsAccuracy.removePrefix("<").removeSuffix("m").toFloatOrNull()
        return buildOsmMapHtml(lat, lng, acc)
    }

    /**
     * Capture a screenshot of the WebView.
     * Uses WebView.draw() to a bitmap → saves to app's audit_photos dir.
     */
    fun captureScreenshot(webView: WebView) {
        if (_uiState.value.isCapturing) return
        _uiState.update { it.copy(isCapturing = true, captureError = null) }

        viewModelScope.launch {
            try {
                val path = withContext(Dispatchers.IO) {
                    // Use WebView.draw() to capture the full view content
                    val w = webView.width.takeIf { it > 0 } ?: 800
                    val h = webView.height.takeIf { it > 0 } ?: 600
                    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    webView.draw(canvas)

                    // Save to file
                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val dir = File(context.getExternalFilesDir(null), "audit_photos")
                    if (!dir.exists()) dir.mkdirs()
                    val file = File(dir, "GPS_${ts}.jpg")

                    FileOutputStream(file).use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                    }
                    bitmap.recycle()
                    file.absolutePath
                }

                _uiState.update { it.copy(
                    isCapturing = false,
                    savedPhotoPath = path
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isCapturing = false,
                    captureError = "Failed to capture: ${e.message}"
                )}
            }
        }
    }

    fun clearCapturedPhoto() {
        _uiState.update { it.copy(savedPhotoPath = null) }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            locationManager?.removeUpdates(locationListener)
        } catch (_: Exception) { }
    }
}
