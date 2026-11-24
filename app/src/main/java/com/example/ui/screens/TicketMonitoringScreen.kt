package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.QueueStatus
import com.example.data.model.QueueTicketDto
import com.example.ui.components.LiveConnectionPill
import com.example.ui.components.QueueStatusBadge
import com.example.ui.components.StatusBarAppearance
import com.example.ui.components.UltLogoMark
import com.example.ui.theme.DeepInk
import com.example.ui.theme.LavenderBorder
import com.example.ui.theme.LavenderMist
import com.example.ui.theme.MutedInk
import com.example.ui.theme.PureWhite
import com.example.ui.theme.QueueMonoFontFamily
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.StatusCompletedBg
import com.example.ui.theme.StatusCompletedFg
import com.example.ui.theme.StatusSkippedBg
import com.example.ui.theme.StatusSkippedFg
import com.example.viewmodel.StudentUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketMonitoringScreen(
    uiState: StudentUiState,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
    onReplayAnnouncement: () -> Unit,
    onDismissCallAlert: () -> Unit,
    onFinishAndClearTicket: () -> Unit
) {
    StatusBarAppearance(darkIcons = true)
    val ticket = uiState.activeTicket

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.ticket_monitor_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("ticket_monitor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    LiveConnectionPill(
                        isConnected = uiState.isRealtimeConnected,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("ticket_monitor_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.cd_refresh),
                            tint = RoyalPurple
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PureWhite,
                    titleContentColor = DeepInk,
                    navigationIconContentColor = RoyalPurple
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (ticket == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.HourglassTop,
                        contentDescription = null,
                        tint = RoyalPurple,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Belum Ada Tiket Aktif",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Silakan kembali ke Beranda untuk memilih jenis layanan administrasi ULT FKIP Unila.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedInk,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Kembali ke Beranda")
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    BoardingPassTicketCard(
                        ticket = ticket,
                        serviceName = uiState.activeTicketServiceName,
                        peopleAhead = uiState.peopleAhead
                    )

                    TicketStageStepperCard(status = ticket.queueStatus)

                    when (ticket.queueStatus) {
                        QueueStatus.CALLED -> {
                            Button(
                                onClick = onReplayAnnouncement,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("replay_announcement_button"),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RoyalPurple,
                                    contentColor = PureWhite
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = stringResource(R.string.cd_sound_test),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.btn_replay_announcement))
                            }
                        }

                        QueueStatus.COMPLETED, QueueStatus.SKIPPED -> {
                            val isCompleted = ticket.queueStatus == QueueStatus.COMPLETED
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = if (isCompleted) StatusCompletedBg else StatusSkippedBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (isCompleted) StatusCompletedFg.copy(alpha = 0.25f) else StatusSkippedFg.copy(alpha = 0.25f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.EventBusy,
                                        contentDescription = null,
                                        tint = if (isCompleted) StatusCompletedFg else StatusSkippedFg,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Column {
                                        Text(
                                            text = if (isCompleted) "Terima Kasih, Layanan Selesai" else "Sesi Antrean Berakhir (Dilewati)",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isCompleted) StatusCompletedFg else StatusSkippedFg
                                        )
                                        Text(
                                            text = if (isCompleted) {
                                                "Berkas administrasi Anda telah selesai diproses di ULT FKIP Unila."
                                            } else {
                                                "Nomor Anda dilewati karena tidak hadir saat pemanggilan. Silakan ambil nomor baru jika masih tersedia kuota."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DeepInk
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = onFinishAndClearTicket,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("clear_finished_ticket_button"),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RoyalPurple,
                                    contentColor = PureWhite
                                )
                            ) {
                                Text("Selesai & Kembali ke Beranda")
                            }
                        }

                        else -> {
                            OutlinedButton(
                                onClick = onReplayAnnouncement,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("test_audio_button"),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, LavenderBorder)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = stringResource(R.string.cd_sound_test),
                                    tint = RoyalPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tes Suara Panggilan (TTS)",
                                    color = RoyalPurple
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    uiState.callAlertTicket?.let { calledTicket ->
        AlertDialog(
            onDismissRequest = onDismissCallAlert,
            containerColor = PureWhite,
            shape = RoundedCornerShape(26.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(RoyalPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Campaign,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(34.dp)
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.call_alert_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = RoyalPurple,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SoftLavender,
                        border = BorderStroke(1.5.dp, RoyalPurple)
                    ) {
                        Text(
                            text = calledTicket.queueNumber,
                            fontFamily = QueueMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp,
                            color = RoyalPurple,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                        )
                    }
                    Text(
                        text = "Atas nama ${calledTicket.studentName}, silakan segera menuju:",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MutedInk
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = RoyalPurple
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MeetingRoom,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = calledTicket.counterName ?: "Loket ULT",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismissCallAlert,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("acknowledge_call_dialog_button")
                ) {
                    Text(stringResource(R.string.btn_acknowledge_call))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onReplayAnnouncement,
                    border = BorderStroke(1.dp, LavenderBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = null,
                        tint = RoyalPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Putar Ulang Suara", color = RoyalPurple)
                }
            }
        )
    }
}

@Composable
private fun BoardingPassTicketCard(
    ticket: QueueTicketDto,
    serviceName: String,
    peopleAhead: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, LavenderBorder),
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Solid Flat FKIP Purple Ticket Header (Zero Gradient)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RoyalPurple)
                    .padding(horizontal = 22.dp, vertical = 22.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UltLogoMark(size = 26.dp, onPurpleHeader = true)
                        Text(
                            text = "ULT FKIP UNIVERSITAS LAMPUNG",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.2.sp,
                            color = PureWhite.copy(alpha = 0.9f)
                        )
                    }
                    Text(
                        text = ticket.queueNumber,
                        fontFamily = QueueMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 54.sp,
                        lineHeight = 58.sp,
                        color = PureWhite,
                        modifier = Modifier.testTag("ticket_queue_number_text")
                    )
                    Text(
                        text = serviceName.ifBlank { "Layanan Administrasi ${ticket.serviceCode}" },
                        style = MaterialTheme.typography.titleSmall,
                        color = PureWhite,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    QueueStatusBadge(status = ticket.queueStatus)
                }
            }

            // Boarding-Pass Perforation Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val midY = size.height / 2f
                    val cutoutRadius = 14.dp.toPx()
                    drawLine(
                        color = LavenderBorder,
                        start = Offset(cutoutRadius + 8.dp.toPx(), midY),
                        end = Offset(size.width - cutoutRadius - 8.dp.toPx(), midY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    )
                    drawCircle(color = LavenderMist, radius = cutoutRadius, center = Offset(0f, midY))
                    drawCircle(color = LavenderMist, radius = cutoutRadius, center = Offset(size.width, midY))
                }
            }

            // Counter & Queue Position Rounded Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    color = SoftLavender
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Loket Tujuan",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedInk
                        )
                        Text(
                            text = ticket.counterName ?: "Menunggu",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = RoyalPurple
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    color = SoftLavender
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (ticket.queueStatus == QueueStatus.WAITING) "Antrean di Depan" else "Urutan Harian",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedInk
                        )
                        Text(
                            text = if (ticket.queueStatus == QueueStatus.WAITING) {
                                "$peopleAhead Orang"
                            } else {
                                "#${ticket.sequenceNumber}"
                            },
                            fontFamily = QueueMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = RoyalPurple
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TicketDetailRow(label = "Nama Mahasiswa", value = ticket.studentName)
                TicketDetailRow(label = "NPM", value = ticket.studentNpm, mono = true)
                TicketDetailRow(label = "Program Studi", value = ticket.studentStudyProgram)
                TicketDetailRow(label = "Tanggal Antrean", value = ticket.queueDate ?: "Hari Ini", mono = true)
            }
        }
    }
}

@Composable
private fun TicketDetailRow(
    label: String,
    value: String,
    mono: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MutedInk,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (mono) QueueMonoFontFamily else MaterialTheme.typography.bodyMedium.fontFamily,
            fontWeight = FontWeight.SemiBold,
            color = DeepInk,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TicketStageStepperCard(status: QueueStatus) {
    val steps = listOf("Menunggu" to 0, "Dipanggil" to 1, "Dilayani" to 2, "Selesai" to 3)
    val activeIndex = when (status) {
        QueueStatus.WAITING -> 0
        QueueStatus.CALLED -> 1
        QueueStatus.IN_SERVICE -> 2
        QueueStatus.COMPLETED -> 3
        QueueStatus.SKIPPED -> 1
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, LavenderBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Tahapan Pelayanan",
                style = MaterialTheme.typography.titleSmall,
                color = DeepInk
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEach { (label, index) ->
                    val reached = index <= activeIndex && status != QueueStatus.SKIPPED
                    val isCurrent = index == activeIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        status == QueueStatus.SKIPPED && index == 1 -> StatusSkippedFg
                                        reached -> RoyalPurple
                                        else -> SoftLavender
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (reached && !isCurrent) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = "${index + 1}",
                                    fontFamily = QueueMonoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (reached || (status == QueueStatus.SKIPPED && index == 1)) PureWhite else RoyalPurple
                                )
                            }
                        }
                        Text(
                            text = if (status == QueueStatus.SKIPPED && index == 1) "Dilewati" else label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrent) RoyalPurple else MutedInk,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
