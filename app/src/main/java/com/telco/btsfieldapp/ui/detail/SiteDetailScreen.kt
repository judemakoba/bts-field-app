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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.telco.btsfieldapp.domain.model.DcdbRecord
import com.telco.btsfieldapp.domain.model.GroundRecord
import com.telco.btsfieldapp.domain.model.TowerRecord
import com.telco.btsfieldapp.domain.model.Site

import com.telco.btsfieldapp.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteDetailScreen(
    onBack: () -> Unit,
    onOpenGroundEquipment: (String) -> Unit,
    onOpenDcdbInfo: (String) -> Unit,
    onOpenTowerInfo: (String) -> Unit,
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
                    containerColor = PrimaryCoral,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryCoral)
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
                        siteId = uiState.site?.siteId ?: "",
                        onOpenGroundEquipment = onOpenGroundEquipment,
                        onOpenDcdbInfo = onOpenDcdbInfo,
                        onOpenTowerInfo = onOpenTowerInfo
                    )
                }

                // Audit history section
                item {
                    Text(
                        text = "Audit History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PrimaryCoral.copy(alpha = 0.9f))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                val auditData = uiState.auditData
                val hasRecords = (auditData?.groundRecords?.isNotEmpty() == true) ||
                        (auditData?.dcdbRecords?.isNotEmpty() == true) ||
                        (auditData?.towerRecords?.isNotEmpty() == true)

                if (uiState.isFetchingAudit) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryCoral, strokeWidth = 2.dp)
                        }
                    }
                } else if (!hasRecords) {
                    item {
                        EmptyAuditState()
                    }
                } else {
                    // Ground records
                    auditData?.groundRecords?.take(5)?.let { records ->
                        item {
                            AuditSectionHeader(
                                title = "Ground",
                                count = auditData.groundRecords.size,
                                icon = Icons.Default.Landscape
                            )
                        }
                        items(records.size) { idx ->
                            GroundRecordCard(record = records[idx])
                        }
                    }
                    // DCDB records
                    auditData?.dcdbRecords?.take(5)?.let { records ->
                        item {
                            AuditSectionHeader(
                                title = "DCDB",
                                count = auditData.dcdbRecords.size,
                                icon = Icons.Default.ElectricalServices
                            )
                        }
                        items(records.size) { idx ->
                            DcdbRecordCard(record = records[idx])
                        }
                    }
                    // Tower records
                    auditData?.towerRecords?.take(5)?.let { records ->
                        item {
                            AuditSectionHeader(
                                title = "Tower",
                                count = auditData.towerRecords.size,
                                icon = Icons.Default.Architecture
                            )
                        }
                        items(records.size) { idx ->
                            TowerRecordCard(record = records[idx])
                        }
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
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = PrimaryCoral.copy(alpha = 0.12f),
                spotColor = PrimaryCoral.copy(alpha = 0.12f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(PrimaryCoralLight, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CellTower,
                        contentDescription = null,
                        tint = PrimaryCoral,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = site.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight
                    )
                    Text(
                        text = site.siteId,
                        style = MaterialTheme.typography.bodySmall,
                        color = PrimaryCoral,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = site.type.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariantLight
                    )
                }
                StatusBadge(status = site.status)
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(14.dp))

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
        "active", "operational" -> SuccessColor
        "inactive", "offline" -> StatusInactive
        "critical", "fault" -> ErrorColor
        "pending" -> WarningColor
        else -> StatusInactive
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f)
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
            color = OnSurfaceLight,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionsRow(
    siteId: String,
    onOpenGroundEquipment: (String) -> Unit,
    onOpenDcdbInfo: (String) -> Unit,
    onOpenTowerInfo: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Start New Audit",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = OnSurfaceLight,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Ground Equipment — coral filled button
        ScopeButton(
            text = "Ground Equipment Scope",
            icon = Icons.Default.ListAlt,
            color = PrimaryCoral,
            enabled = true,
            onClick = { onOpenGroundEquipment(siteId) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))

        // Tower Equipment — coral filled button
        ScopeButton(
            text = "Tower Equipment Scope",
            icon = Icons.Default.Architecture,
            color = PrimaryCoral,
            enabled = true,
            onClick = { onOpenTowerInfo(siteId) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))

        // DCDB Information — coral filled button
        ScopeButton(
            text = "DCDB Information",
            icon = Icons.Default.ElectricalServices,
            color = PrimaryCoral,
            enabled = true,
            onClick = { onOpenDcdbInfo(siteId) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ScopeButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (enabled) {
        Button(
            onClick = onClick,
            modifier = modifier.height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = color,
                contentColor = Color.White
            )
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(52.dp),
            enabled = false,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = color.copy(alpha = 0.06f),
                contentColor = color.copy(alpha = 0.5f),
                disabledContainerColor = color.copy(alpha = 0.06f),
                disabledContentColor = color.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Text(
                "(Coming Soon)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun AuditSectionHeader(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF3D2E1E)) // warm dark background
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Coral left accent bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .background(PrimaryCoral, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = PrimaryCoral
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$title Records",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = PrimaryCoralLight
        ) {
            Text(
                text = count.toString(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryCoral,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GroundRecordCard(record: GroundRecord) {
    RecordCard(
        icon = Icons.Default.Landscape,
        color = PrimaryCoral,
        title = "Ground",
        subtitle = buildString {
            val fence = record.fenceCondition.trim().ifEmpty { null }
            val resistance = record.groundResistance.trim().ifEmpty { null }
            when {
                fence != null && resistance != null -> append("$fence | $resistance ohm")
                fence != null -> append("Fence: $fence")
                resistance != null -> append("Resistance: $resistance ohm")
                else -> append("No data")
            }
        },
        notes = record.notes,
        date = record.createdAt
    )
}

@Composable
private fun DcdbRecordCard(record: DcdbRecord) {
    RecordCard(
        icon = Icons.Default.ElectricalServices,
        color = PrimaryCoral,
        title = "DCDB",
        subtitle = buildString {
            val type = record.dcdbType.trim().ifEmpty { null }
            val cap = record.dcdbCapacity.trim().ifEmpty { null }
            when {
                type != null && cap != null -> append("$type | ${cap}A")
                type != null -> append("Type: $type")
                cap != null -> append("Capacity: ${cap}A")
                else -> append("No data")
            }
        },
        notes = record.notes,
        date = record.createdAt
    )
}

@Composable
private fun TowerRecordCard(record: TowerRecord) {
    RecordCard(
        icon = Icons.Default.Architecture,
        color = PrimaryCoral,
        title = "Tower",
        subtitle = buildString {
            val type = record.towerType.trim().ifEmpty { null }
            val height = record.towerHeight.trim().ifEmpty { null }
            when {
                type != null && height != null -> append("$type | ${height}m")
                type != null -> append("Type: $type")
                height != null -> append("Height: ${height}m")
                else -> append("No data")
            }
        },
        notes = record.notes,
        date = record.createdAt
    )
}

@Composable
private fun RecordCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    title: String,
    subtitle: String,
    notes: String,
    date: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = PrimaryCoral.copy(alpha = 0.08f),
                spotColor = PrimaryCoral.copy(alpha = 0.08f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Coral header strip at the top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(color, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(PrimaryCoralLight, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariantLight
                    )
                    if (notes.isNotEmpty()) {
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariantLight.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = formatDateShort(date),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariantLight
                )
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
                color = OnSurfaceLight
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
