package com.example.ui.components

import android.app.Activity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.R
import com.example.core.FkipStudyPrograms
import com.example.core.StudyProgramOption
import com.example.data.model.QueueStatus
import com.example.ui.theme.LavenderBorder
import com.example.ui.theme.MutedInk
import com.example.ui.theme.PureWhite
import com.example.ui.theme.QueueMonoFontFamily
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.StatusCalledBg
import com.example.ui.theme.StatusCalledFg
import com.example.ui.theme.StatusCompletedBg
import com.example.ui.theme.StatusCompletedFg
import com.example.ui.theme.StatusInServiceBg
import com.example.ui.theme.StatusInServiceFg
import com.example.ui.theme.StatusSkippedBg
import com.example.ui.theme.StatusSkippedFg
import com.example.ui.theme.StatusWaitingBg
import com.example.ui.theme.StatusWaitingFg

@Composable
fun StatusBarAppearance(darkIcons: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkIcons
            }
        }
    }
}

/**
 * Official circular FKIP Unila logo with optional white halo ring for purple headers.
 */
@Composable
fun UltLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    onPurpleHeader: Boolean = false
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (onPurpleHeader) PureWhite else Color.Transparent)
            .then(
                if (onPurpleHeader) {
                    Modifier.border(2.dp, PureWhite, CircleShape)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_fkip_unila_logo),
            contentDescription = stringResource(R.string.cd_app_logo),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(if (onPurpleHeader) size - 3.dp else size)
                .clip(CircleShape)
        )
    }
}

@Composable
fun ServiceCodeBadge(
    code: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
) {
    val bg = if (highlighted) RoyalPurple else SoftLavender
    val fg = if (highlighted) PureWhite else RoyalPurple
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, if (highlighted) RoyalPurple else LavenderBorder, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = code.uppercase(),
            fontFamily = QueueMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = fg
        )
    }
}

@Composable
fun QueueStatusBadge(
    status: QueueStatus,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (status) {
        QueueStatus.WAITING -> StatusWaitingBg to StatusWaitingFg
        QueueStatus.CALLED -> StatusCalledBg to StatusCalledFg
        QueueStatus.IN_SERVICE -> StatusInServiceBg to StatusInServiceFg
        QueueStatus.COMPLETED -> StatusCompletedBg to StatusCompletedFg
        QueueStatus.SKIPPED -> StatusSkippedBg to StatusSkippedFg
    }

    val infiniteTransition = rememberInfiniteTransition(label = "called_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (status == QueueStatus.CALLED) 0.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(999.dp),
        border = if (status == QueueStatus.WAITING || status == QueueStatus.IN_SERVICE) {
            BorderStroke(1.dp, LavenderBorder)
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .alpha(pulseAlpha)
                    .clip(CircleShape)
                    .background(fg)
            )
            Text(
                text = status.labelId,
                style = MaterialTheme.typography.labelMedium,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LiveConnectionPill(
    isConnected: Boolean,
    modifier: Modifier = Modifier,
    onPurpleSurface: Boolean = false
) {
    val dotColor = if (isConnected) {
        if (onPurpleSurface) Color(0xFFA7F3D0) else StatusCompletedFg
    } else {
        if (onPurpleSurface) PureWhite.copy(alpha = 0.7f) else MutedInk
    }
    val textColor = if (onPurpleSurface) PureWhite else MutedInk
    val bgColor = if (onPurpleSurface) PureWhite.copy(alpha = 0.16f) else SoftLavender

    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = if (isConnected) "Realtime Aktif" else "Sinkronisasi",
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
        }
    }
}

@Composable
fun ConfigurationWarningBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = StatusSkippedBg,
        border = BorderStroke(1.dp, StatusSkippedFg.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = StatusSkippedFg,
                modifier = Modifier.size(22.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.config_missing_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = StatusSkippedFg
                )
                Text(
                    text = stringResource(R.string.config_missing_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ErrorNoticeBanner(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StatusSkippedBg,
        border = BorderStroke(1.dp, StatusSkippedFg.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = StatusSkippedFg,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = StatusSkippedFg,
                modifier = Modifier.weight(1f)
            )
            if (onRetry != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PureWhite,
                    border = BorderStroke(1.dp, StatusSkippedFg.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onRetry)
                ) {
                    Text(
                        text = "Coba Lagi",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusSkippedFg,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableProdiBottomSheet(
    selectedProdi: String,
    onSelect: (StudyProgramOption) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    val filteredPrograms = remember(searchQuery) { FkipStudyPrograms.filter(searchQuery) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureWhite,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Program Studi FKIP Unila",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Pilih dari daftar resmi S1, S2, S3 & PPG",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedInk
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_prodi_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_prodi_input"),
                placeholder = { Text(stringResource(R.string.search_prodi_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = RoyalPurple
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Hapus pencarian")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalPurple,
                    unfocusedBorderColor = LavenderBorder,
                    focusedContainerColor = SoftLavender.copy(alpha = 0.4f),
                    unfocusedContainerColor = SoftLavender.copy(alpha = 0.25f)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredPrograms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Program studi \"$searchQuery\" tidak ditemukan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedInk
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filteredPrograms, key = { it.name }) { option ->
                        val isSelected = option.name == selectedProdi
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) SoftLavender else Color.Transparent)
                                .clickable { onSelect(option) }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) RoyalPurple else SoftLavender
                            ) {
                                Text(
                                    text = option.level,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) PureWhite else RoyalPurple,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = option.department,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedInk
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Terpilih",
                                    tint = RoyalPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = LavenderBorder.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}
