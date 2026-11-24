package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.AlertAudioEngine
import com.example.data.local.LocalPreferencesStore
import com.example.data.repository.QueueRepository
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StaffDashboardScreen
import com.example.ui.screens.StaffLoginScreen
import com.example.ui.screens.StudentHomeScreen
import com.example.ui.screens.TakeQueueScreen
import com.example.ui.screens.TicketMonitoringScreen
import com.example.ui.theme.UltQueueTheme
import com.example.viewmodel.StaffDashboardViewModel
import com.example.viewmodel.StudentQueueViewModel

private object AppRoutes {
    const val SPLASH = "splash"
    const val STUDENT_HOME = "student_home"
    const val TAKE_QUEUE = "take_queue/{serviceId}"
    const val TICKET_MONITOR = "ticket_monitor"
    const val STAFF_LOGIN = "staff_login"
    const val STAFF_DASHBOARD = "staff_dashboard"

    fun takeQueue(serviceId: String) = "take_queue/$serviceId"
}

class MainActivity : ComponentActivity() {
    private lateinit var alertAudioEngine: AlertAudioEngine
    private lateinit var queueRepository: QueueRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        alertAudioEngine = AlertAudioEngine(applicationContext)
        queueRepository = QueueRepository(LocalPreferencesStore(applicationContext))

        setContent {
            UltQueueTheme(darkTheme = false) {
                UltQueueApp(
                    repository = queueRepository,
                    audioEngine = alertAudioEngine
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::queueRepository.isInitialized) {
            queueRepository.disconnectRealtime()
        }
        if (::alertAudioEngine.isInitialized) {
            alertAudioEngine.shutdown()
        }
    }
}

@Composable
private fun UltQueueApp(
    repository: QueueRepository,
    audioEngine: AlertAudioEngine
) {
    val navController = rememberNavController()

    val studentViewModel: StudentQueueViewModel = viewModel(
        factory = StudentQueueViewModel.Factory(repository, audioEngine)
    )
    val staffViewModel: StaffDashboardViewModel = viewModel(
        factory = StaffDashboardViewModel.Factory(repository)
    )

    val studentUiState by studentViewModel.uiState.collectAsStateWithLifecycle()
    val staffUiState by staffViewModel.uiState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.SPLASH
    ) {
        composable(AppRoutes.SPLASH) {
            SplashScreen(
                onFinished = {
                    navController.navigate(AppRoutes.STUDENT_HOME) {
                        popUpTo(AppRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoutes.STUDENT_HOME) {
            StudentHomeScreen(
                uiState = studentUiState,
                onRefresh = { studentViewModel.refreshAll() },
                onSelectService = { serviceId ->
                    navController.navigate(AppRoutes.takeQueue(serviceId))
                },
                onOpenActiveTicket = {
                    navController.navigate(AppRoutes.TICKET_MONITOR)
                },
                onOpenStaffPortal = {
                    if (staffUiState.session != null) {
                        navController.navigate(AppRoutes.STAFF_DASHBOARD)
                    } else {
                        navController.navigate(AppRoutes.STAFF_LOGIN)
                    }
                },
                onClearMessages = { studentViewModel.clearMessages() }
            )
        }

        composable(
            route = AppRoutes.TAKE_QUEUE,
            arguments = listOf(navArgument("serviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getString("serviceId").orEmpty()
            TakeQueueScreen(
                serviceId = serviceId,
                uiState = studentUiState,
                onNavigateBack = { navController.popBackStack() },
                onSubmitTicket = { svcId, name, npm, prodi ->
                    studentViewModel.submitQueueTicket(svcId, name, npm, prodi) {
                        navController.navigate(AppRoutes.TICKET_MONITOR) {
                            popUpTo(AppRoutes.STUDENT_HOME) { inclusive = false }
                        }
                    }
                },
                onOpenExistingTicket = {
                    studentViewModel.openExistingConflictTicket {
                        navController.navigate(AppRoutes.TICKET_MONITOR) {
                            popUpTo(AppRoutes.STUDENT_HOME) { inclusive = false }
                        }
                    }
                },
                onDismissExistingConflict = { studentViewModel.dismissExistingTicketConflict() }
            )
        }

        composable(AppRoutes.TICKET_MONITOR) {
            TicketMonitoringScreen(
                uiState = studentUiState,
                onNavigateBack = { navController.popBackStack() },
                onRefresh = { studentViewModel.refreshAll() },
                onReplayAnnouncement = { studentViewModel.replayCallAnnouncement() },
                onDismissCallAlert = { studentViewModel.dismissCallAlert() },
                onFinishAndClearTicket = {
                    studentViewModel.clearFinishedTicketAndReset()
                    navController.popBackStack(AppRoutes.STUDENT_HOME, inclusive = false)
                }
            )
        }

        composable(AppRoutes.STAFF_LOGIN) {
            StaffLoginScreen(
                uiState = staffUiState,
                onNavigateBack = { navController.popBackStack() },
                onLogin = { email, password ->
                    staffViewModel.login(email, password) {
                        navController.navigate(AppRoutes.STAFF_DASHBOARD) {
                            popUpTo(AppRoutes.STAFF_LOGIN) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(AppRoutes.STAFF_DASHBOARD) {
            StaffDashboardScreen(
                uiState = staffUiState,
                onNavigateHome = {
                    navController.popBackStack(AppRoutes.STUDENT_HOME, inclusive = false)
                },
                onRefresh = { staffViewModel.refreshDashboard() },
                onLogout = {
                    staffViewModel.logout {
                        navController.popBackStack(AppRoutes.STUDENT_HOME, inclusive = false)
                    }
                },
                onSelectCounter = { counter -> staffViewModel.selectCounter(counter) },
                onSelectServiceFilter = { code -> staffViewModel.selectServiceFilter(code) },
                onCallNext = { staffViewModel.callNextTicket() },
                onCallSpecific = { ticket -> staffViewModel.callSpecificTicket(ticket) },
                onRecall = { staffViewModel.recallActiveTicket() },
                onStartService = { staffViewModel.startServingActiveTicket() },
                onCompleteService = { staffViewModel.completeActiveTicket() },
                onSkipTicket = { staffViewModel.skipActiveTicket() },
                onClearMessages = { staffViewModel.clearMessages() }
            )
        }
    }
}
