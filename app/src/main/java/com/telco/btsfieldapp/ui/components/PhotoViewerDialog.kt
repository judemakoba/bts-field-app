package com.telco.btsfieldapp.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Full-screen photo viewer dialog.
 * Shows the image with pinch-to-zoom, rotation, and delete options.
 *
 * @param photoPath   Local file path of the photo to display.
 * @param onDismiss  Called when the dialog is dismissed.
 * @param onDelete   Called when delete is confirmed — does NOT remove the file automatically.
 */
@Composable
fun PhotoViewerDialog(
    photoPath: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var rotationDeg by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showGpsOverlay by remember { mutableStateOf(true) }
    val file = remember(photoPath) { File(photoPath) }
    val isGpsScreenshot = remember(photoPath) {
        val fname = photoPath.substringAfterLast("/").substringAfterLast("\\")
        fname.startsWith("GPS_", ignoreCase = true)
    }
    // GPS timestamp from filename e.g. GPS_20260930_071539
    val gpsLabel = remember(photoPath) {
        if (!isGpsScreenshot) null
        else file.nameWithoutExtension.removePrefix("GPS_")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
        ) {
            // ── Top toolbar ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp)
                    .statusBarsPadding()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }

                Text(
                    text = photoPath.substringAfterLast("/").substringAfterLast("\\").take(30),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    maxLines = 1
                )

                if (isGpsScreenshot) {
                    IconButton(onClick = { showGpsOverlay = !showGpsOverlay }) {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = "Toggle GPS overlay",
                            tint = if (showGpsOverlay) Color(0xFF4ade80) else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444)
                    )
                }
            }

            // ── Photo with pinch-to-zoom ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 72.dp, bottom = 80.dp)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 5f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(context)
                            .data(file)
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = "Photo",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            rotationZ = rotationDeg
                        },
                    contentScale = ContentScale.Fit
                )

                // GPS screenshot overlay — dark gradient footer so text is always readable
                if (isGpsScreenshot && showGpsOverlay) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.8f)
                                    )
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        gpsLabel?.let {
                            Text(
                                text = "📍 GPS Map Screenshot — $it",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ── Bottom toolbar ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .navigationBarsPadding()
                    .padding(vertical = 12.dp, horizontal = 24.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarButton(
                    icon = Icons.Default.ZoomOut,
                    label = "Zoom Out",
                    onClick = { scale = (scale - 0.3f).coerceIn(0.5f, 5f) }
                )
                ToolbarButton(
                    icon = Icons.Default.ZoomIn,
                    label = "Zoom In",
                    onClick = { scale = (scale + 0.3f).coerceIn(0.5f, 5f) }
                )
                ToolbarButton(
                    icon = Icons.Default.RotateLeft,
                    label = "Rotate Left",
                    onClick = { rotationDeg -= 90f }
                )
                ToolbarButton(
                    icon = Icons.Default.RotateRight,
                    label = "Rotate Right",
                    onClick = { rotationDeg += 90f }
                )
            }

            // ── Reset zoom FAB ─────────────────────────────────────────────────────
            if (scale != 1f) {
                FloatingActionButton(
                    onClick = { scale = 1f; rotationDeg = 0f },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 88.dp)
                        .size(40.dp),
                    containerColor = Color(0xFF374151),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Reset", modifier = Modifier.size(18.dp))
                }
            }

            // ── Delete confirmation dialog ─────────────────────────────────────────
            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    icon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    title = { Text("Delete Photo?") },
                    text = {
                        Text(
                            "This will remove the photo from this record. The action cannot be undone.",
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteConfirm = false
                                // Delete the file
                                try { file.delete() } catch (_: Exception) { }
                                onDelete()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Text("Delete", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
    }
}
