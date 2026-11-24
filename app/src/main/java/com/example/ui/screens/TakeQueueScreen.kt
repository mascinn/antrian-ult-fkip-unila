package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ServiceSummary
import com.example.ui.components.ErrorNoticeBanner
import com.example.ui.components.SearchableProdiBottomSheet
import com.example.ui.components.ServiceCodeBadge
import com.example.ui.components.StatusBarAppearance
import com.example.ui.theme.DeepInk
import com.example.ui.theme.LavenderBorder
import com.example.ui.theme.MutedInk
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SoftLavender
import com.example.viewmodel.StudentUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeQueueScreen(
    serviceId: String,
    uiState: StudentUiState,
    onNavigateBack: () -> Unit,
    onSubmitTicket: (serviceId: String, name: String, npm: String, prodi: String) -> Unit,
    onOpenExistingTicket: () -> Unit,
    onDismissExistingConflict: () -> Unit
) {
    StatusBarAppearance(darkIcons = true)

    val selectedSummary: ServiceSummary? = remember(uiState.serviceSummaries, serviceId) {
        uiState.serviceSummaries.firstOrNull { it.service.id == serviceId }
    }

    var studentName by rememberSaveable { mutableStateOf("") }
    var studentNpm by rememberSaveable { mutableStateOf("") }
    var studyProgram by rememberSaveable { mutableStateOf("") }
    var initializedFromPrefs by rememberSaveable { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var npmError by remember { mutableStateOf<String?>(null) }
    var prodiError by remember { mutableStateOf<String?>(null) }
    var showProdiSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.rememberedStudent) {
        if (!initializedFromPrefs) {
            val saved = uiState.rememberedStudent
            if (saved.name.isNotBlank() || saved.npm.isNotBlank() || saved.studyProgram.isNotBlank()) {
                studentName = saved.name
                studentNpm = saved.npm
                studyProgram = saved.studyProgram
                initializedFromPrefs = true
            }
        }
    }

    val nameErrText = stringResource(R.string.error_name_required)
    val npmErrText = stringResource(R.string.error_npm_required)
    val prodiErrText = stringResource(R.string.error_prodi_required)

    fun validateAndSubmit() {
        val cleanName = studentName.trim()
        val cleanNpm = studentNpm.trim()
        val cleanProdi = studyProgram.trim()

        nameError = if (cleanName.length < 3) nameErrText else null
        npmError = if (cleanNpm.length !in 8..15 || !cleanNpm.all { it.isDigit() }) npmErrText else null
        prodiError = if (cleanProdi.isBlank()) prodiErrText else null

        if (nameError == null && npmError == null && prodiError == null) {
            onSubmitTicket(serviceId, cleanName, cleanNpm, cleanProdi)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.take_ticket_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("take_queue_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
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
                .imePadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 620.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (selectedSummary != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = SoftLavender,
                        border = BorderStroke(1.dp, LavenderBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            ServiceCodeBadge(
                                code = selectedSummary.service.code,
                                highlighted = true
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedSummary.service.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = RoyalPurple
                                )
                                if (!selectedSummary.service.description.isNullOrBlank()) {
                                    Text(
                                        text = selectedSummary.service.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedInk
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Sisa kuota hari ini: ${selectedSummary.remainingQuota}/${selectedSummary.service.quotaPerDay} • ${selectedSummary.waitingCount} menunggu",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalPurple
                                )
                            }
                        }
                    }
                }

                if (uiState.errorMessage != null) {
                    ErrorNoticeBanner(message = uiState.errorMessage)
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = PureWhite,
                    border = BorderStroke(1.dp, LavenderBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Data Identitas Mahasiswa",
                                style = MaterialTheme.typography.titleMedium,
                                color = DeepInk
                            )
                            Text(
                                text = "Tanpa login. Maksimal 1 tiket aktif per hari untuk setiap NPM.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedInk
                            )
                        }

                        OutlinedTextField(
                            value = studentName,
                            onValueChange = {
                                studentName = it
                                nameError = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("student_name_input"),
                            label = { Text(stringResource(R.string.label_student_name)) },
                            placeholder = { Text(stringResource(R.string.placeholder_student_name)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = RoyalPurple
                                )
                            },
                            isError = nameError != null,
                            supportingText = nameError?.let { err -> { Text(err) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RoyalPurple,
                                unfocusedBorderColor = LavenderBorder
                            )
                        )

                        OutlinedTextField(
                            value = studentNpm,
                            onValueChange = { value ->
                                studentNpm = value.filter { it.isDigit() }.take(15)
                                npmError = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("student_npm_input"),
                            label = { Text(stringResource(R.string.label_student_npm)) },
                            placeholder = { Text(stringResource(R.string.placeholder_student_npm)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Badge,
                                    contentDescription = null,
                                    tint = RoyalPurple
                                )
                            },
                            isError = npmError != null,
                            supportingText = npmError?.let { err -> { Text(err) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RoyalPurple,
                                unfocusedBorderColor = LavenderBorder
                            )
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.label_study_program),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (prodiError != null) MaterialTheme.colorScheme.error else MutedInk
                            )
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { showProdiSheet = true }
                                    .testTag("study_program_selector"),
                                shape = RoundedCornerShape(16.dp),
                                color = if (studyProgram.isNotBlank()) SoftLavender.copy(alpha = 0.45f) else PureWhite,
                                border = BorderStroke(
                                    width = if (prodiError != null) 1.5.dp else 1.dp,
                                    color = if (prodiError != null) MaterialTheme.colorScheme.error else LavenderBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.School,
                                        contentDescription = null,
                                        tint = RoyalPurple
                                    )
                                    Text(
                                        text = studyProgram.ifBlank {
                                            stringResource(R.string.placeholder_study_program)
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (studyProgram.isNotBlank()) DeepInk else MutedInk,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = "Buka daftar Program Studi",
                                        tint = RoyalPurple
                                    )
                                }
                            }
                            if (prodiError != null) {
                                Text(
                                    text = prodiError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = ::validateAndSubmit,
                            enabled = !uiState.isSubmittingTicket && uiState.operationalState.isOpen,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("submit_queue_ticket_button"),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalPurple,
                                contentColor = PureWhite
                            )
                        ) {
                            if (uiState.isSubmittingTicket) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = PureWhite,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Menerbitkan Tiket...")
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.ConfirmationNumber,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.btn_submit_ticket),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProdiSheet) {
        SearchableProdiBottomSheet(
            selectedProdi = studyProgram,
            onSelect = { selected ->
                studyProgram = selected.name
                prodiError = null
                showProdiSheet = false
            },
            onDismiss = { showProdiSheet = false }
        )
    }

    uiState.existingTicketConflict?.let { (_, message) ->
        AlertDialog(
            onDismissRequest = onDismissExistingConflict,
            containerColor = PureWhite,
            shape = RoundedCornerShape(24.dp),
            icon = {
                Icon(
                    imageVector = Icons.Rounded.ConfirmationNumber,
                    contentDescription = null,
                    tint = RoyalPurple
                )
            },
            title = {
                Text(
                    text = "Tiket Aktif Ditemukan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "$message. Anda dapat langsung membuka tiket antrean aktif yang sedang berjalan.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = onOpenExistingTicket,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                    modifier = Modifier.testTag("open_existing_ticket_button")
                ) {
                    Text("Buka Tiket Aktif")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismissExistingConflict,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, LavenderBorder)
                ) {
                    Text("Tutup", color = RoyalPurple)
                }
            }
        )
    }
}
