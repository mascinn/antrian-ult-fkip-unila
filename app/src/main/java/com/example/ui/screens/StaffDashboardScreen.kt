package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.QueueStatus
import com.example.data.model.QueueTicketDto
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
import com.example.ui.theme.StatusCompletedFg
import com.example.ui.theme.StatusSkippedFg
import com.example.viewmodel.StaffUiState

@Composable
fun StaffDashboardScreen(
    uiState: StaffUiState,
    onNavigateHome: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onSelectCounter: (String) -> Unit,
    onSelectServiceFilter: (String?) -> Unit,
    onCallNext: () -> Unit,
    onCallSpecific: (QueueTicketDto) -> Unit,
    onRecall: () -> Unit,
    onStartService: () -> Unit,
    onCompleteService: () -> Unit,
    onSkipTicket: () -> Unit,
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
                    .widthIn(max = 720.dp)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Solid Flat FKIP Purple Header (Zero Gradient)
                item {
                    StaffSolidPurpleHeader(
                        staffName = uiState.session?.fullName ?: "Petugas ULT",
                        staffRole = uiState.session?.role ?: "staff",
                        isRealtimeConnected = uiState.isRealtimeConnected,
                        isLoading = uiState.isLoading,
                        onNavigateHome = onNavigateHome,
                        onRefresh = onRefresh,
                        onLogout = onLogout
                    )
                }

                if (uiState.errorMessage != null) {
                    item {
                        ErrorNoticeBanner(
                            message = uiState.errorMessage,
                            onRetry = onRefresh,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }

                // Daily Metrics Row (3 Modern Rounded Cards)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StaffStatCard(
                            label = "Menunggu",
                            value = uiState.waitingCount.toString(),
                            highlighted = true,
                            modifier = Modifier.weight(1f)
                        )
                        StaffStatCard(
                            label = "Diproses",
                            value = uiState.activeCount.toString(),
                            highlighted = false,
                            modifier = Modifier.weight(1f)
                        )
                        StaffStatCard(
                            label = "Selesai",
                            value = uiState.completedCount.toString(),
                            highlighted = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Counter Assignment Selector Card (Loket 1 / 2 / 3)
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
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MeetingRoom,
                                    contentDescription = null,
                                    tint = RoyalPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Loket Tugas Anda",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DeepInk
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.counters.forEach { counter ->
                                    val selected = counter.name.equals(uiState.selectedCounter, ignoreCase = true)
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { onSelectCounter(counter.name) }
                                            .testTag("counter_chip_${counter.id}"),
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (selected) RoyalPurple else SoftLavender,
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) RoyalPurple else LavenderBorder
                                        )
                                    ) {
                                        Text(
                                            text = counter.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (selected) PureWhite else RoyalPurple,
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Active Ticket Control Panel or Call Next Card
                item {
                    val active = uiState.activeTicketAtCounter
                    if (active != null) {
                        ActiveCounterControlCard(
                            ticket = active,
                            serviceName = uiState.serviceNameFor(active.serviceId),
                            counterName = uiState.selectedCounter,
                            callAttempts = uiState.callAttemptsFor(active.id),
                            isMutating = uiState.isMutating,
                            onRecall = onRecall,
                            onStartService = onStartService,
                            onCompleteService = onCompleteService,
                            onSkipTicket = onSkipTicket,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    } else {
                        CallNextHeroCard(
                            counterName = uiState.selectedCounter,
                            nextWaitingTicket = uiState.filteredWaitingQueues.firstOrNull(),
                            waitingCount = uiState.filteredWaitingQueues.size,
                            isMutating = uiState.isMutating,
                            onCallNext = onCallNext,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }

                // Category Filter Bar (Pill Chips)
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Daftar Antrean Menunggu (${uiState.filteredWaitingQueues.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepInk
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = uiState.selectedServiceCode == null,
                                onClick = { onSelectServiceFilter(null) },
                                label = { Text("Semua") },
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier.testTag("filter_all"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RoyalPurple,
                                    selectedLabelColor = PureWhite
                                )
                            )
                            val filterCodes = if (uiState.services.isNotEmpty()) {
                                uiState.services.map { it.code to "${it.code} • ${shortServiceLabel(it.code)}" }
                            } else {
                                listOf("A" to "A • KKN", "B" to "B • Wisuda", "C" to "C • Surat", "D" to "D • Lainnya")
                            }
                            filterCodes.forEach { (code, label) ->
                                FilterChip(
                                    selected = uiState.selectedServiceCode == code,
                                    onClick = { onSelectServiceFilter(code) },
                                    label = { Text(label) },
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier.testTag("filter_${code.lowercase()}"),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RoyalPurple,
                                        selectedLabelColor = PureWhite
                                    )
                                )
                            }
                        }
                    }
                }

                if (uiState.filteredWaitingQueues.isEmpty()) {
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Inbox,
                                    contentDescription = null,
                                    tint = RoyalPurple,
                                    modifier = Modifier.size(34.dp)
                                )
                                Text(
                                    text = "Tidak Ada Antrean Menunggu",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Antrean baru dari mahasiswa akan muncul secara otomatis di sini.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedInk
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.filteredWaitingQueues, key = { it.id }) { ticket ->
                        WaitingQueueItemCard(
                            ticket = ticket,
                            serviceName = uiState.serviceNameFor(ticket.serviceId),
                            canCall = uiState.activeTicketAtCounter == null && !uiState.isMutating,
                            onCall = { onCallSpecific(ticket) },
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }

                if (uiState.filteredHistoryQueues.isNotEmpty()) {
                    item {
                        Text(
                            text = "Riwayat Hari Ini (${uiState.filteredHistoryQueues.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepInk,
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .padding(top = 6.dp)
                        )
                    }
                    items(uiState.filteredHistoryQueues.take(15), key = { "hist_${it.id}" }) { ticket ->
                        HistoryQueueItemCard(
                            ticket = ticket,
                            serviceName = uiState.serviceNameFor(ticket.serviceId),
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffSolidPurpleHeader(
    staffName: String,
    staffRole: String,
    isRealtimeConnected: Boolean,
    isLoading: Boolean,
    onNavigateHome: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
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
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onNavigateHome,
                        modifier = Modifier.testTag("staff_dashboard_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = PureWhite
                        )
                    }
                    UltLogoMark(size = 36.dp, onPurpleHeader = true)
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.staff_dashboard_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = "$staffName • ${staffRole.uppercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PureWhite.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("staff_refresh_button")
                    ) {
                        if (isLoading) {
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
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("staff_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = stringResource(R.string.cd_logout),
                            tint = PureWhite
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiveConnectionPill(isConnected = isRealtimeConnected, onPurpleSurface = true)
            }
        }
    }
}

@Composable
private fun StaffStatCard(
    label: String,
    value: String,
    highlighted: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (highlighted) SoftLavender else PureWhite,
        border = BorderStroke(1.dp, if (highlighted) RoyalPurple.copy(alpha = 0.35f) else LavenderBorder)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                fontFamily = QueueMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = if (highlighted) RoyalPurple else DeepInk
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MutedInk
            )
        }
    }
}

@Composable
private fun CallNextHeroCard(
    counterName: String,
    nextWaitingTicket: QueueTicketDto?,
    waitingCount: Int,
    isMutating: Boolean,
    onCallNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = PureWhite,
        border = BorderStroke(1.5.dp, RoyalPurple),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "$counterName Siap Melayani",
                    style = MaterialTheme.typography.titleMedium,
                    color = RoyalPurple
                )
                Text(
                    text = if (nextWaitingTicket != null) {
                        "Berikutnya: ${nextWaitingTicket.queueNumber} — ${nextWaitingTicket.studentName}"
                    } else {
                        "Belum ada antrean pada filter terpilih"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedInk
                )
            }

            Button(
                onClick = onCallNext,
                enabled = waitingCount > 0 && !isMutating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("call_next_ticket_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalPurple,
                    contentColor = PureWhite
                )
            ) {
                if (isMutating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = PureWhite,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Memproses Panggilan...")
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Campaign,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_call_next),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveCounterControlCard(
    ticket: QueueTicketDto,
    serviceName: String,
    counterName: String,
    callAttempts: Int,
    isMutating: Boolean,
    onRecall: () -> Unit,
    onStartService: () -> Unit,
    onCompleteService: () -> Unit,
    onSkipTicket: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = PureWhite,
        border = BorderStroke(2.dp, RoyalPurple),
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aktif di $counterName",
                    style = MaterialTheme.typography.labelLarge,
                    color = RoyalPurple
                )
                QueueStatusBadge(status = ticket.queueStatus)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = RoyalPurple
                ) {
                    Text(
                        text = ticket.queueNumber,
                        fontFamily = QueueMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = PureWhite,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ticket.studentName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepInk
                    )
                    Text(
                        text = "NPM ${ticket.studentNpm}",
                        fontFamily = QueueMonoFontFamily,
                        fontSize = 12.sp,
                        color = MutedInk
                    )
                    Text(
                        text = "${ticket.studentStudyProgram} • $serviceName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedInk,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (ticket.queueStatus == QueueStatus.CALLED) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SoftLavender
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status Panggilan: Panggilan ke-$callAttempts",
                            style = MaterialTheme.typography.labelMedium,
                            color = RoyalPurple
                        )
                        if (callAttempts >= 3) {
                            Text(
                                text = "Dapat dilewati (3x)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusSkippedFg
                            )
                        }
                    }
                }

                Button(
                    onClick = onStartService,
                    enabled = !isMutating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("start_service_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_start_service))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRecall,
                        enabled = !isMutating,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("recall_ticket_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, RoyalPurple)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Campaign,
                            contentDescription = null,
                            tint = RoyalPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.btn_recall), color = RoyalPurple, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = onSkipTicket,
                        enabled = !isMutating,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("skip_ticket_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, StatusSkippedFg.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = null,
                            tint = StatusSkippedFg,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lewati", color = StatusSkippedFg, maxLines = 1)
                    }
                }
            } else if (ticket.queueStatus == QueueStatus.IN_SERVICE) {
                Button(
                    onClick = onCompleteService,
                    enabled = !isMutating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("complete_service_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusCompletedFg,
                        contentColor = PureWhite
                    )
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_complete_service))
                }
            }
        }
    }
}

@Composable
private fun WaitingQueueItemCard(
    ticket: QueueTicketDto,
    serviceName: String,
    canCall: Boolean,
    onCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, LavenderBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ServiceCodeBadge(code = ticket.serviceCode)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = ticket.queueNumber,
                        fontFamily = QueueMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = RoyalPurple
                    )
                    Text(
                        text = "• ${ticket.studentNpm}",
                        fontFamily = QueueMonoFontFamily,
                        fontSize = 11.sp,
                        color = MutedInk
                    )
                }
                Text(
                    text = ticket.studentName,
                    style = MaterialTheme.typography.titleSmall,
                    color = DeepInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${ticket.studentStudyProgram} • $serviceName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            OutlinedButton(
                onClick = onCall,
                enabled = canCall,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (canCall) RoyalPurple else LavenderBorder),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Panggil",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (canCall) RoyalPurple else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun HistoryQueueItemCard(
    ticket: QueueTicketDto,
    serviceName: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, LavenderBorder.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = ticket.queueNumber,
                fontFamily = QueueMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MutedInk
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ticket.studentName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${ticket.counterName ?: "-"} • $serviceName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            QueueStatusBadge(status = ticket.queueStatus)
        }
    }
}

private fun shortServiceLabel(code: String): String = when (code.uppercase()) {
    "A" -> "KKN"
    "B" -> "Wisuda"
    "C" -> "Surat"
    "D" -> "Lainnya"
    else -> code
}
