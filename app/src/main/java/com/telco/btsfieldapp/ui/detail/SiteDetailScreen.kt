package com.telco.btsfieldapp.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.telco.btsfieldapp.domain.model.AuditRecord
import com.telco.btsfieldapp.domain.model.Site
import com.telco.btsfieldapp.ui.audit.AuditType
import com.telco.btsfieldapp.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteDetailScreen(
    onBack: () -> Unit,
    onStartAudit: (Long, String) -> Unit,
    viewModel: SiteDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.site?.name ?: "Site Details",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryGreen)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Site info header
                uiState.site?.let { site ->
                    item {
                        SiteInfoHeader(site = site)
                    }
                }

                // Quick action buttons
                item {
                    QuickActionsRow(
                        siteId = uiState.site?.id ?: 0L,
                        onStartAudit = onStartAudit
                    )
                }

                // Audit history section
                item {
                    Text(
                        text = "Audit History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                if (uiState.audits.isEmpty()) {
                    item {
                        EmptyAuditState()
                    }
                } else {
                    items(uiState.audits, key = { it.id }) { audit ->
                        AuditHistoryCard(audit = audit)
                    }
                }
            }
        }
    }
}

@Composable
private fun SiteInfoHeader(site: Site) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = PrimaryGreen.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CellTower,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = site.btsId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                    Text(
                        text = site.type.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryGreen
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                StatusBadge(status = site.status)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = PrimaryGreen.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(icon = Icons.Default.LocationOn, label = "Address", value = site.address)
            site.latitude?.let { lat ->
                site.longitude?.let { lng ->
                    InfoRow(icon = Icons.Default.MyLocation, label = "Coordinates",
                        value = "%.5f, %.5f".format(lat, lng))
                }
            }
            InfoRow(icon = Icons.Default.Schedule, label = "Last Updated",
                value = formatDate(site.updatedAt))
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val color = when (status.lowercase()) {
        "active", "operational" -> StatusActive
        "inactive", "offline" -> StatusInactive
        "critical", "fault" -> StatusCritical
        "pending" -> StatusPending
        else -> StatusInactive
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = status.replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = OnSurfaceVariantLight
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariantLight
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionsRow(
    siteId: Long,
    onStartAudit: (Long, String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Start New Audit",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AuditType.entries.take(2).forEach { type ->
                AuditTypeButton(
                    type = type,
                    onClick = { onStartAudit(siteId, type.key) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AuditType.entries.drop(2).forEach { type ->
                AuditTypeButton(
                    type = type,
                    onClick = { onStartAudit(siteId, type.key) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AuditTypeButton(
    type: AuditType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (type) {
        AuditType.GROUND -> Icons.Default.Landscape to Color(0xFF22C55E)
        AuditType.DCDB -> Icons.Default.ElectricalServices to Color(0xFFF59E0B)
        AuditType.TOWER -> Icons.Default.Architecture to Color(0xFF3B82F6)
        AuditType.EQUIPMENT -> Icons.Default.DevicesOther to Color(0xFF8B5CF6)
    }

    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = color.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = type.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
private fun AuditHistoryCard(audit: AuditRecord) {
    val typeColor = when (audit.type.lowercase()) {
        "ground" -> StatusActive
        "dcdb" -> WarningColor
        "tower" -> InfoColor
        "equipment" -> Color(0xFF8B5CF6)
        else -> StatusInactive
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(typeColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (audit.type.lowercase()) {
                        "ground" -> Icons.Default.Landscape
                        "dcdb" -> Icons.Default.ElectricalServices
                        "tower" -> Icons.Default.Architecture
                        else -> Icons.Default.DevicesOther
                    },
                    contentDescription = null,
                    tint = typeColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = audit.type.replaceFirstChar { it.uppercase() } + " Audit",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = audit.engineerName,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariantLight
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatDateShort(audit.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariantLight
                )
                if (!audit.synced) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = WarningColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "Pending sync",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = WarningColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAuditState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Assignment,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = OnSurfaceVariantLight.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "No audits yet",
                style = MaterialTheme.typography.titleSmall,
                color = OnSurfaceVariantLight
            )
            Text(
                "Start an audit above",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariantLight.copy(alpha = 0.7f)
            )
        }
    }
}

private fun formatDate(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (_: Exception) {
        isoString
    }
}

private fun formatDateShort(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val formatter = DateTimeFormatter.ofPattern("dd MMM")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (_: Exception) {
        isoString.take(10)
    }
}
