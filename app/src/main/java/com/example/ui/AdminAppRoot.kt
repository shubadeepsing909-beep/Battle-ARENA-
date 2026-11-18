package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.AdminUser
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.finance.FinanceScreen
import com.example.ui.hub.HubSection
import com.example.ui.hub.OpsHubScreen
import com.example.ui.theme.ArenaBluePrimary
import com.example.ui.theme.ArenaCanvas
import com.example.ui.theme.ArenaInkPrimary
import com.example.ui.theme.ArenaInkSecondary
import com.example.ui.theme.ArenaSurfaceBlueTint
import com.example.ui.theme.ArenaWhite
import com.example.ui.tournaments.TournamentsScreen
import com.example.ui.users.UsersScreen
import com.example.ui.viewmodel.AdminPanelViewModel

sealed class AdminDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Dashboard : AdminDestination(
        "dashboard", "Overview", Icons.Filled.Dashboard, Icons.Outlined.Dashboard
    )
    data object Tournaments : AdminDestination(
        "tournaments", "Matches", Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports
    )
    data object Finance : AdminDestination(
        "finance", "Finance", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet
    )
    data object Users : AdminDestination(
        "users", "Players", Icons.Filled.Groups, Icons.Outlined.Groups
    )
    data object Hub : AdminDestination(
        "hub", "Hub", Icons.Filled.Menu, Icons.Outlined.Menu
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAppRoot(
    currentAdmin: AdminUser,
    viewModel: AdminPanelViewModel,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AdminDestination.Dashboard.route

    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val tournamentsState by viewModel.tournamentsState.collectAsStateWithLifecycle()
    val registrationsState by viewModel.selectedRegistrationsState.collectAsStateWithLifecycle()
    val depositsState by viewModel.depositsState.collectAsStateWithLifecycle()
    val withdrawalsState by viewModel.withdrawalsState.collectAsStateWithLifecycle()
    val paymentSettingsState by viewModel.paymentSettingsState.collectAsStateWithLifecycle()
    val usersState by viewModel.usersState.collectAsStateWithLifecycle()
    val walletsState by viewModel.walletsState.collectAsStateWithLifecycle()
    val userTransactionsState by viewModel.selectedUserTransactionsState.collectAsStateWithLifecycle()
    val winnersState by viewModel.winnersState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val supportTicketsState by viewModel.supportTicketsState.collectAsStateWithLifecycle()
    val auditLogsState by viewModel.auditLogsState.collectAsStateWithLifecycle()
    val adminsState by viewModel.adminsState.collectAsStateWithLifecycle()
    val actionBusy by viewModel.actionBusy.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var financeInitialTab by remember { mutableIntStateOf(0) }
    var openCreateTournament by remember { mutableStateOf(false) }
    var hubInitialSection by remember { mutableStateOf(HubSection.MENU) }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val visibleDestinations = remember(currentAdmin.role, currentAdmin.active) {
        com.example.ui.auth.AdminRouteGuard.allowedDestinations(currentAdmin)
    }

    val pendingFinanceCount = stats.pendingDeposits + stats.pendingWithdrawals

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ArenaCanvas,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "BATTLE ARENA",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ArenaInkPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusPill(text = "ADMIN", tone = StatusTone.BLUE)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ArenaWhite,
                        titleContentColor = ArenaInkPrimary
                    )
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = ArenaWhite
                    ) {
                        visibleDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (dest == AdminDestination.Hub) hubInitialSection = HubSection.MENU
                                    navigateToTab(dest.route)
                                },
                                icon = {
                                    DestinationIcon(dest, selected, pendingFinanceCount)
                                },
                                label = { Text(dest.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_${dest.route}"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ArenaBluePrimary,
                                    selectedTextColor = ArenaBluePrimary,
                                    indicatorColor = ArenaSurfaceBlueTint,
                                    unselectedIconColor = ArenaInkSecondary,
                                    unselectedTextColor = ArenaInkSecondary
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = ArenaWhite,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        visibleDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    if (dest == AdminDestination.Hub) hubInitialSection = HubSection.MENU
                                    navigateToTab(dest.route)
                                },
                                icon = {
                                    DestinationIcon(dest, selected, pendingFinanceCount)
                                },
                                label = { Text(dest.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ArenaBluePrimary,
                                    selectedTextColor = ArenaBluePrimary,
                                    indicatorColor = ArenaSurfaceBlueTint,
                                    unselectedIconColor = ArenaInkSecondary,
                                    unselectedTextColor = ArenaInkSecondary
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(ArenaCanvas)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = AdminDestination.Dashboard.route
                    ) {
                        composable(AdminDestination.Dashboard.route) {
                            DashboardScreen(
                                admin = currentAdmin,
                                stats = stats,
                                auditLogsState = auditLogsState,
                                onOpenTournaments = { navigateToTab(AdminDestination.Tournaments.route) },
                                onOpenCreateTournament = {
                                    openCreateTournament = true
                                    navigateToTab(AdminDestination.Tournaments.route)
                                },
                                onOpenDeposits = {
                                    financeInitialTab = 0
                                    navigateToTab(AdminDestination.Finance.route)
                                },
                                onOpenWithdrawals = {
                                    financeInitialTab = 1
                                    navigateToTab(AdminDestination.Finance.route)
                                },
                                onOpenGatewaySettings = {
                                    financeInitialTab = 2
                                    navigateToTab(AdminDestination.Finance.route)
                                },
                                onOpenAnnouncements = {
                                    hubInitialSection = HubSection.ANNOUNCEMENTS
                                    navigateToTab(AdminDestination.Hub.route)
                                }
                            )
                        }

                        composable(AdminDestination.Tournaments.route) {
                            if (com.example.ui.auth.AdminRouteGuard.canAccessRoute(currentAdmin, AdminDestination.Tournaments.route)) {
                                TournamentsScreen(
                                    tournamentsState = tournamentsState,
                                    registrationsState = registrationsState,
                                    actionBusy = actionBusy,
                                    openCreateInitially = openCreateTournament,
                                    onCreateConsumed = { openCreateTournament = false },
                                    onSelectTournament = viewModel::selectTournamentForDetail,
                                    onCreateTournament = viewModel::createTournament,
                                    onUpdateTournament = viewModel::updateTournament,
                                    onTogglePublished = viewModel::togglePublished,
                                    onUpdateRoomCredentials = viewModel::updateRoomCredentials,
                                    onUpdateStatus = viewModel::updateTournamentStatus,
                                    onDeleteTournament = viewModel::deleteTournament,
                                    onRemoveParticipant = viewModel::removeParticipant,
                                    onAllocateWinner = viewModel::allocateWinnerPrize
                                )
                            } else {
                                com.example.ui.auth.RouteAccessDeniedCard(
                                    requiredRoles = "SUPER_ADMIN or TOURNAMENT_ADMIN",
                                    currentRole = currentAdmin.role,
                                    onReturnToOverview = { navigateToTab(AdminDestination.Dashboard.route) }
                                )
                            }
                        }

                        composable(AdminDestination.Finance.route) {
                            if (com.example.ui.auth.AdminRouteGuard.canAccessRoute(currentAdmin, AdminDestination.Finance.route)) {
                                FinanceScreen(
                                    initialTab = financeInitialTab,
                                    depositsState = depositsState,
                                    withdrawalsState = withdrawalsState,
                                    paymentSettingsState = paymentSettingsState,
                                    actionBusy = actionBusy,
                                    onApproveDeposit = viewModel::approveDeposit,
                                    onRejectDeposit = viewModel::rejectDeposit,
                                    onApproveWithdrawal = viewModel::approveWithdrawal,
                                    onRejectWithdrawal = viewModel::rejectWithdrawal,
                                    onSavePaymentSettings = viewModel::savePaymentSettings
                                )
                            } else {
                                com.example.ui.auth.RouteAccessDeniedCard(
                                    requiredRoles = "SUPER_ADMIN or FINANCE_ADMIN",
                                    currentRole = currentAdmin.role,
                                    onReturnToOverview = { navigateToTab(AdminDestination.Dashboard.route) }
                                )
                            }
                        }

                        composable(AdminDestination.Users.route) {
                            if (com.example.ui.auth.AdminRouteGuard.canAccessRoute(currentAdmin, AdminDestination.Users.route)) {
                                UsersScreen(
                                    usersState = usersState,
                                    walletsState = walletsState,
                                    userTransactionsState = userTransactionsState,
                                    actionBusy = actionBusy,
                                    onSelectUser = viewModel::selectUserForDetail,
                                    onToggleBlocked = viewModel::toggleUserBlocked,
                                    onAdjustWallet = viewModel::adjustUserWallet
                                )
                            } else {
                                com.example.ui.auth.RouteAccessDeniedCard(
                                    requiredRoles = "Authorized Admin",
                                    currentRole = currentAdmin.role,
                                    onReturnToOverview = { navigateToTab(AdminDestination.Dashboard.route) }
                                )
                            }
                        }

                        composable(AdminDestination.Hub.route) {
                            OpsHubScreen(
                                initialSection = hubInitialSection,
                                currentAdmin = currentAdmin,
                                winnersState = winnersState,
                                notificationsState = notificationsState,
                                supportTicketsState = supportTicketsState,
                                adminsState = adminsState,
                                auditLogsState = auditLogsState,
                                actionBusy = actionBusy,
                                onSendNotification = viewModel::sendNotification,
                                onUpdateTicket = viewModel::updateSupportTicket,
                                onUpsertAdmin = viewModel::upsertAdminRole,
                                onRemoveAdmin = viewModel::removeAdminRole,
                                onSignOut = onSignOut
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationIcon(
    dest: AdminDestination,
    selected: Boolean,
    pendingFinanceCount: Int
) {
    val icon = if (selected) dest.selectedIcon else dest.unselectedIcon
    if (dest == AdminDestination.Finance && pendingFinanceCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(if (pendingFinanceCount > 99) "99+" else pendingFinanceCount.toString())
                }
            }
        ) {
            Icon(icon, contentDescription = dest.label)
        }
    } else {
        Icon(icon, contentDescription = dest.label)
    }
}
