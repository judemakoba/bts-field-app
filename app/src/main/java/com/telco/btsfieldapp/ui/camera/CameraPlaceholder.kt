package com.telco.btsfieldapp.ui.camera

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreenPlaceholder(
    onBack: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Placeholder — camera init not available without PreviewView
    // viewModel.initializeCamera(lifecycleOwner, previewView)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Capture Photo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("✕")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Camera Preview",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Point camera at the equipment and tap Capture",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { viewModel.capturePhoto() }) {
                Text("Capture")
            }
            if (uiState.capturedPhotoPath != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Photo captured: ${uiState.capturedPhotoPath}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
