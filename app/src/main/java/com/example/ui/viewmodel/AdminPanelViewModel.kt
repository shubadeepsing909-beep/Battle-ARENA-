package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminRole
import com.example.data.model.AdminUser
import com.example.data.model.AuditLog
import com.example.data.model.DashboardStats
import com.example.data.model.DepositRequest
import com.example.data.model.NotificationItem
import com.example.data.model.PaymentSettings
import com.example.data.model.PlayerUser
import com.example.data.model.PlayerWallet
import com.example.data.model.Registration
import com.example.data.model.RequestStatus
import com.example.data.model.SupportTicket
import com.example.data.model.TicketStatus
import com.example.data.model.Tournament
import com.example.data.model.TournamentStatus
import com.example.data.model.TournamentWinner
import com.example.data.model.UiState
import com.example.data.model.WalletTransaction
import com.example.data.model.WithdrawalRequest
import com.example.data.repository.FinanceRepository
import com.example.data.repository.TournamentFormInput
import com.example.data.repository.TournamentRepository
import com.example.data.repository.UserAndOpsRepository
import com.example.data.repository.extractFirestoreErrorMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AdminPanelViewModel(
    private val tournamentRepo: TournamentRepository,
    private val financeRepo: FinanceRepository,
    private val opsRepo: UserAndOpsRepository
) : ViewModel() {

    private val _actionBusy = MutableStateFlow(false)
    val actionBusy: StateFlow<Boolean> = _actionBusy.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private fun <T> Flow<T>.toUiStateFlow(fallbackError: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                emit(UiState.Error(extractFirestoreErrorMessage(error, fallbackError)))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    val tournamentsState: StateFlow<UiState<List<Tournament>>> =
        tournamentRepo.observeTournaments().toUiStateFlow("Failed to load tournaments")

    val depositsState: StateFlow<UiState<List<DepositRequest>>> =
        financeRepo.observeDeposits().toUiStateFlow("Failed to load deposits")

    val withdrawalsState: StateFlow<UiState<List<WithdrawalRequest>>> =
        financeRepo.observeWithdrawals().toUiStateFlow("Failed to load withdrawals")

    val paymentSettingsState: StateFlow<UiState<PaymentSettings>> =
        financeRepo.observePaymentSettings().toUiStateFlow("Failed to load payment settings")

    val usersState: StateFlow<UiState<List<PlayerUser>>> =
        opsRepo.observeUsers().toUiStateFlow("Failed to load players")

    val walletsState: StateFlow<UiState<Map<String, PlayerWallet>>> =
        opsRepo.observeWallets().toUiStateFlow("Failed to load wallets")

    val winnersState: StateFlow<UiState<List<TournamentWinner>>> =
        tournamentRepo.observeWinners().toUiStateFlow("Failed to load winners")

    val notificationsState: StateFlow<UiState<List<NotificationItem>>> =
        opsRepo.observeNotifications().toUiStateFlow("Failed to load notifications")

    val supportTicketsState: StateFlow<UiState<List<SupportTicket>>> =
        opsRepo.observeSupportTickets().toUiStateFlow("Failed to load support tickets")

    val auditLogsState: StateFlow<UiState<List<AuditLog>>> =
        opsRepo.observeAuditLogs().toUiStateFlow("Failed to load audit logs")

    val adminsState: StateFlow<UiState<List<AdminUser>>> =
        opsRepo.observeAdmins().toUiStateFlow("Failed to load admin roles")

    val dashboardStats: StateFlow<DashboardStats> = combine(
        usersState,
        tournamentsState,
        depositsState,
        withdrawalsState
    ) { users, tours, deps, wds ->
        val userList = (users as? UiState.Success)?.data.orEmpty()
        val tourList = (tours as? UiState.Success)?.data.orEmpty()
        val depList = (deps as? UiState.Success)?.data.orEmpty()
        val wdList = (wds as? UiState.Success)?.data.orEmpty()

        DashboardStats(
            totalUsers = userList.size,
            totalTournaments = tourList.size,
            activeTournaments = tourList.count {
                it.status == TournamentStatus.UPCOMING.name || it.status == TournamentStatus.LIVE.name
            },
            completedTournaments = tourList.count { it.status == TournamentStatus.COMPLETED.name },
            pendingDeposits = depList.count { it.status == RequestStatus.PENDING.name },
            pendingWithdrawals = wdList.count { it.status == RequestStatus.PENDING.name },
            totalVerifiedDeposits = depList
                .filter { it.status == RequestStatus.APPROVED.name }
                .sumOf { it.amount },
            totalApprovedPayouts = wdList
                .filter { it.status == RequestStatus.APPROVED.name }
                .sumOf { it.amount }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = DashboardStats()
    )

    private val selectedTournamentId = MutableStateFlow<String?>(null)
    val selectedRegistrationsState: StateFlow<UiState<List<Registration>>> =
        selectedTournamentId
            .flatMapLatest { id ->
                if (id.isNullOrBlank()) flowOf(emptyList())
                else tournamentRepo.observeRegistrations(id)
            }
            .toUiStateFlow("Failed to load participants")

    private val selectedUserId = MutableStateFlow<String?>(null)
    val selectedUserTransactionsState: StateFlow<UiState<List<WalletTransaction>>> =
        selectedUserId
            .flatMapLatest { uid ->
                if (uid.isNullOrBlank()) flowOf(emptyList())
                else financeRepo.observeUserTransactions(uid)
            }
            .toUiStateFlow("Failed to load user transactions")

    fun selectTournamentForDetail(tournamentId: String?) {
        selectedTournamentId.value = tournamentId
    }

    fun selectUserForDetail(userId: String?) {
        selectedUserId.value = userId
    }

    private fun runAdminAction(
        successMessage: String,
        block: suspend () -> Result<*>
    ) {
        if (_actionBusy.value) return
        viewModelScope.launch {
            _actionBusy.value = true
            try {
                val result = block()
                result.fold(
                    onSuccess = { _snackbarEvents.tryEmit(successMessage) },
                    onFailure = { err ->
                        _snackbarEvents.tryEmit(err.localizedMessage ?: "Operation failed")
                    }
                )
            } finally {
                _actionBusy.value = false
            }
        }
    }

    fun createTournament(input: TournamentFormInput) {
        runAdminAction("Tournament created") { tournamentRepo.createTournament(input) }
    }

    fun updateTournament(tournamentId: String, input: TournamentFormInput) {
        runAdminAction("Tournament updated") {
            tournamentRepo.updateTournament(tournamentId, input)
        }
    }

    fun togglePublished(tournament: Tournament) {
        val label = if (tournament.published) "Tournament unpublished" else "Tournament published"
        runAdminAction(label) { tournamentRepo.togglePublished(tournament) }
    }

    fun updateRoomCredentials(
        tournamentId: String,
        tournamentTitle: String,
        roomId: String,
        roomPassword: String
    ) {
        runAdminAction("Room credentials released") {
            tournamentRepo.updateRoomCredentials(tournamentId, tournamentTitle, roomId, roomPassword)
        }
    }

    fun updateTournamentStatus(
        tournamentId: String,
        tournamentTitle: String,
        status: TournamentStatus
    ) {
        runAdminAction("Status updated to ${status.label}") {
            tournamentRepo.updateTournamentStatus(tournamentId, tournamentTitle, status)
        }
    }

    fun deleteTournament(tournament: Tournament) {
        runAdminAction("Tournament deleted") { tournamentRepo.deleteTournament(tournament) }
    }

    fun removeParticipant(registration: Registration, tournamentTitle: String) {
        runAdminAction("Participant removed") {
            tournamentRepo.removeParticipant(registration, tournamentTitle)
        }
    }

    fun allocateWinnerPrize(
        tournament: Tournament,
        registration: Registration,
        rank: Long,
        kills: Long,
        prizeAmount: Double
    ) {
        runAdminAction("Prize allocated & wallet credited") {
            tournamentRepo.allocateWinnerPrize(tournament, registration, rank, kills, prizeAmount)
        }
    }

    fun approveDeposit(deposit: DepositRequest, adminNote: String) {
        runAdminAction("Deposit verified & wallet credited") {
            financeRepo.approveDeposit(deposit, adminNote)
        }
    }

    fun rejectDeposit(deposit: DepositRequest, reason: String) {
        runAdminAction("Deposit rejected") { financeRepo.rejectDeposit(deposit, reason) }
    }

    fun approveWithdrawal(
        withdrawal: WithdrawalRequest,
        payoutReference: String,
        adminNote: String
    ) {
        runAdminAction("Withdrawal payout approved") {
            financeRepo.approveWithdrawal(withdrawal, payoutReference, adminNote)
        }
    }

    fun rejectWithdrawal(withdrawal: WithdrawalRequest, reason: String) {
        runAdminAction("Withdrawal rejected") { financeRepo.rejectWithdrawal(withdrawal, reason) }
    }

    fun savePaymentSettings(settings: PaymentSettings) {
        runAdminAction("Gateway links & payout settings saved") {
            financeRepo.savePaymentSettings(settings)
        }
    }

    fun toggleUserBlocked(user: PlayerUser) {
        val msg = if (user.blocked) "Player unblocked" else "Player blocked"
        runAdminAction(msg) { opsRepo.toggleUserBlocked(user) }
    }

    fun adjustUserWallet(user: PlayerUser, amount: Double, isCredit: Boolean, reason: String) {
        val msg = if (isCredit) "Wallet credited" else "Wallet debited"
        runAdminAction(msg) { opsRepo.adjustUserWallet(user, amount, isCredit, reason) }
    }

    fun sendNotification(
        title: String,
        message: String,
        targetUserId: String,
        type: String,
        tournamentId: String
    ) {
        runAdminAction("Notification dispatched") {
            opsRepo.sendNotification(title, message, targetUserId, type, tournamentId)
        }
    }

    fun updateSupportTicket(ticket: SupportTicket, status: TicketStatus, adminReply: String) {
        runAdminAction("Support ticket updated") {
            opsRepo.updateSupportTicket(ticket, status, adminReply)
        }
    }

    fun upsertAdminRole(
        targetUid: String,
        email: String,
        displayName: String,
        role: AdminRole,
        active: Boolean
    ) {
        runAdminAction("Administrator role saved") {
            opsRepo.upsertAdminRole(targetUid, email, displayName, role, active)
        }
    }

    fun removeAdminRole(adminUser: AdminUser) {
        runAdminAction("Administrator access revoked") { opsRepo.removeAdminRole(adminUser) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5000L
    }
}
