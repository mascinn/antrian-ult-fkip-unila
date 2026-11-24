package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CounterLiveDisplay
import com.example.data.model.QueueTicketDto
import com.example.data.model.ServiceSummary
import com.example.ui.components.ConfigurationWarningBanner
import com.example.ui.components.ErrorNoticeBanner
import com.example.ui.components.LiveConnectionPill
import com.example.ui.components.QueueStatusBadge
import com.example.ui.components.ServiceCodeBadge
import com.example.ui.components.StatusBarAppearance
import com.example.ui.components.UltLogoMark
import com.example.ui.theme.DeepInk
import com.example.ui.theme.LavenderBorder
import com.example.ui.theme.MutedInk
import com.example.ui.theme.PureWhite
import com.example.ui.theme.QueueMonoFontFamily
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.StatusClosedBg
import com.example.ui.theme.StatusClosedFg
import com.example.ui.theme.StatusCompletedBg
import com.example.ui.theme.StatusCompletedFg
import com.example.viewmodel.StudentUiState

@Composable
fun StudentHomeScreen(
    uiState: StudentUiState,
    onRefresh: () -> Unit,
    onSelectService: (String) -> Unit,
    onOpenActiveTicket: () -> Unit,
    onOpenStaffPortal: () -> Unit,
    onClearMessages: () -> Unit
) {
    StatusBarAppearance(darkIcons = false)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onClearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Solid Flat FKIP Purple Rounded Header (Zero Gradients)
                item {
                    SolidPurpleHeader(
                        uiState = uiState,
                        onRefresh = onRefresh,
                        onOpenStaffPortal = onOpenStaffPortal
                    )
                }

                if (!uiState.isConfigured) {
                    item {
                        ConfigurationWarningBanner(
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }

                if (uiState.errorMessage != null && uiState.isConfigured) {
                    item {
                        ErrorNoticeBanner(
                            message = uiState.errorMessage,
                            onRetry = onRefresh,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }

                // Active Ticket Quick-Return Card
                uiState.activeTicket?.let { activeTicket ->
                    item {
                        ActiveTicketQuickCard(
                            ticket = activeTicket,
                            serviceName = uiState.activeTicketServiceName,
                            peopleAhead = uiState.peopleAhead,
                            onClick = onOpenActiveTicket,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }

                // Operational Hours Closed Info Card (No Simulation Toggle)
                item {
                    AnimatedVisibility(visible = !uiState.operationalState.isOpen) {
                        ClosedHoursNoticeCard(
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }

                // Live Counter Board (3 Modern Rounded Cards)
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.section_live_counters),
                                style = MaterialTheme.typography.titleMedium,
                                color = DeepInk
                            )
                            LiveConnectionPill(isConnected = uiState.isRealtimeConnected)
                        }

                        LiveCountersRow(counters = uiState.liveCounters)
                    }
                }

                // Services Section Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.section_services),
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepInk
                        )
                        Text(
                            text = "${uiState.serviceSummaries.size} Layanan",
                            style = MaterialTheme.typography.labelMedium,
                            color = RoyalPurple
                        )
                    }
                }

                if (uiState.isInitialLoading && uiState.serviceSummaries.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = RoyalPurple)
                        }
                    }
                } else if (uiState.serviceSummaries.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(22.dp),
                            color = PureWhite,
                            border = BorderStroke(1.dp, LavenderBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SupportAgent,
                                    contentDescription = null,
                                    tint = RoyalPurple,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "Belum Ada Layanan Aktif",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Pastikan tabel services di Supabase sudah diisi data layanan A, B, C, dan D.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedInk
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.serviceSummaries, key = { it.service.id }) { summary ->
                        ServiceOptionCard(
                            summary = summary,
                            isOpen = uiState.operationalState.isOpen,
                            onClick = { onSelectService(summary.service.id) },
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SolidPurpleHeader(
    uiState: StudentUiState,
    onRefresh: () -> Unit,
    onOpenStaffPortal: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        color = RoyalPurple,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    UltLogoMark(size = 44.dp, onPurpleHeader = true)
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = PureWhite
                        )
                        Text(
                            text = "FKIP Universitas Lampung",
                            style = MaterialTheme.typography.bodySmall,
                            color = PureWhite.copy(alpha = 0.85f)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("home_refresh_button")
                    ) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = PureWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = stringResource(R.string.cd_refresh),
                                tint = PureWhite
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = PureWhite,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(onClick = onOpenStaffPortal)
                            .testTag("staff_portal_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AdminPanelSettings,
                                contentDescription = stringResource(R.string.cd_staff_login),
                                tint = RoyalPurple,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "Petugas",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = RoyalPurple
                            )
                        }
                    }
                }
            }

            // Live WIB Date, Time & Open/Closed Strip
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = PureWhite.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = uiState.operationalState.formattedDate,
                                style = MaterialTheme.typography.labelMedium,
                                color = PureWhite
                            )
                            Text(
                                text = uiState.operationalState.formattedTime,
                                fontFamily = QueueMonoFontFamily,
                                fontSize = 12.sp,
                                color = PureWhite.copy(alpha = 0.85f)
                            )
                        }
                    }

                    val isOpen = uiState.operationalState.isOpen
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (isOpen) StatusCompletedBg else PureWhite
                    ) {
                        Text(
                            text = if (isOpen) stringResource(R.string.status_open) else stringResource(R.string.status_closed),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isOpen) StatusCompletedFg else StatusClosedFg,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClosedHoursNoticeCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, LavenderBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(StatusClosedBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = StatusClosedFg,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.closed_banner_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = DeepInk
                )
                Text(
                    text = stringResource(R.string.closed_banner_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedInk
                )
            }
        }
    }
}

@Composable
private fun ActiveTicketQuickCard(
    ticket: QueueTicketDto,
    serviceName: String,
    peopleAhead: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .testTag("active_ticket_quick_card"),
        shape = RoundedCornerShape(22.dp),
        color = PureWhite,
        border = BorderStroke(1.5.dp, RoyalPurple),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(RoyalPurple)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ticket.queueNumber,
                    fontFamily = QueueMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = PureWhite
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Tiket Aktif Anda Hari Ini",
                    style = MaterialTheme.typography.labelSmall,
                    color = RoyalPurple,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = serviceName.ifBlank { "Layanan ULT" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QueueStatusBadge(status = ticket.queueStatus)
                    if (ticket.queueStatus.name == "WAITING") {
                        Text(
                            text = "$peopleAhead di depan",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedInk
                        )
                    } else if (!ticket.counterName.isNullOrBlank()) {
                        Text(
                            text = ticket.counterName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = RoyalPurple
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = stringResource(R.string.btn_view_active_ticket),
                tint = RoyalPurple
            )
        }
    }
}

@Composable
private fun LiveCountersRow(counters: List<CounterLiveDisplay>) {
    val items = counters.ifEmpty {
        listOf(
            CounterLiveDisplay("Loket 1", null),
            CounterLiveDisplay("Loket 2", null),
            CounterLiveDisplay("Loket 3", null)
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { display ->
            val hasActive = display.activeTicket != null
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = PureWhite,
                border = BorderStroke(
                    width = if (hasActive) 1.5.dp else 1.dp,
                    color = if (hasActive) RoyalPurple else LavenderBorder
                ),
                shadowElevation = if (hasActive) 2.dp else 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = display.counterName,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (hasActive) RoyalPurple else MutedInk
                    )
                    Text(
                        text = display.activeTicket?.queueNumber ?: "---",
                        fontFamily = QueueMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = if (hasActive) RoyalPurple else MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = display.activeTicket?.queueStatus?.let {
                            if (it.name == "CALLED") "Dipanggil" else "Dilayani"
                        } ?: "Tersedia",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasActive) RoyalPurple else MutedInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceOptionCard(
    summary: ServiceSummary,
    isOpen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = isOpen && !summary.isFull
    val progress = if (summary.service.quotaPerDay > 0) {
        (summary.todayTotalCount.toFloat() / summary.service.quotaPerDay.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.6f)
            .testTag("service_card_${summary.service.code.lowercase()}"),
        shape = RoundedCornerShape(22.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, LavenderBorder),
        shadowElevation = if (enabled) 1.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ServiceCodeBadge(
                    code = summary.service.code,
                    highlighted = enabled
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = summary.service.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepInk
                    )
                    if (!summary.service.description.isNullOrBlank()) {
                        Text(
                            text = summary.service.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedInk,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Pilih ${summary.service.name}",
                    tint = if (enabled) RoyalPurple else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = if (summary.isFull) StatusClosedFg else RoyalPurple,
                    trackColor = SoftLavender
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ConfirmationNumber,
                            contentDescription = null,
                            tint = RoyalPurple,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${summary.waitingCount} menunggu",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedInk
                        )
                        if (summary.currentNumber != null) {
                            Text(
                                text = "• Kini ${summary.currentNumber}",
                                fontFamily = QueueMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalPurple
                            )
                        }
                    }

                    Text(
                        text = if (summary.isFull) {
                            "Kuota Penuh"
                        } else {
                            "Sisa ${summary.remainingQuota}/${summary.service.quotaPerDay}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.isFull) StatusClosedFg else RoyalPurple
                    )
                }
            }
        }
    }
}
