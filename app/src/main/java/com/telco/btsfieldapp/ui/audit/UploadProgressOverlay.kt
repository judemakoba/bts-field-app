package com.telco.btsfieldapp.ui.audit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Full-screen modal overlay showing per-photo upload progress.
 * Used during Save Draft and Submit for Review when photos are attached.
 *
 * @param title        Dialog title ("Saving Draft..." / "Submitting...")
 * @param isVisible    Whether to show the overlay
 * @param total        Total number of photos to upload
 * @param current      How many have been processed (started, done, or failed)
 * @param statuses     Map of fieldName → upload status for per-row icons
 */
@Composable
fun UploadProgressOverlay(
    title: String,
    isVisible: Boolean,
    total: Int,
    current: Int,
    statuses: Map<String, PhotoUploadStatus>,
    onDismiss: () -> Unit = {}
) {
    if (!isVisible) return

    Dialog(
        onDismissRequest = { /* no-op — uploads in progress, don't let user cancel */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // ── Header ───────────────────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ── Overall progress ────────────────────────────────────────────
                val doneCount = statuses.values.count { it == PhotoUploadStatus.DONE }
                val progressFraction = if (total > 0) doneCount.toFloat() / total else 0f

                Text(
                    text = "$doneCount / $total photos uploaded",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                Spacer(Modifier.height(20.dp))

                HorizontalDivider()

                Spacer(Modifier.height(12.dp))

                // ── Per-photo rows ──────────────────────────────────────────────
                if (statuses.isNotEmpty()) {
                    Text(
                        text = "Photos",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(statuses.entries.toList()) { (fieldName, status) ->
                            PhotoUploadRow(
                                fieldName = fieldName,
                                status = status
                            )
                        }
                    }
                } else {
                    // No photos — just show spinner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Preparing upload...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoUploadRow(
    fieldName: String,
    status: PhotoUploadStatus
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Status icon
        when (status) {
            PhotoUploadStatus.PENDING -> {
                Icon(
                    Icons.Default.HourglassEmpty,
                    contentDescription = "Pending",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(20.dp)
                )
            }
            PhotoUploadStatus.UPLOADING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            PhotoUploadStatus.DONE -> {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Done",
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(20.dp)
                )
            }
            PhotoUploadStatus.FAILED -> {
                Icon(
                    Icons.Default.Error,
                    contentDescription = "Failed",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Icon(
            Icons.Default.Photo,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )

        // Field name — clean it up for display
        Text(
            text = fieldName.replaceFirstChar { it.uppercase() }
                .replace("_", " "),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        // Status label
        Text(
            text = when (status) {
                PhotoUploadStatus.PENDING -> "Waiting"
                PhotoUploadStatus.UPLOADING -> "Uploading..."
                PhotoUploadStatus.DONE -> "Done"
                PhotoUploadStatus.FAILED -> "Failed"
            },
            style = MaterialTheme.typography.labelSmall,
            color = when (status) {
                PhotoUploadStatus.PENDING -> Color(0xFF9E9E9E)
                PhotoUploadStatus.UPLOADING -> MaterialTheme.colorScheme.primary
                PhotoUploadStatus.DONE -> Color(0xFF22C55E)
                PhotoUploadStatus.FAILED -> Color(0xFFEF4444)
            }
        )
    }
}
