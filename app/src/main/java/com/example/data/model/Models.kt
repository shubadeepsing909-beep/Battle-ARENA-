package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AdminRole {
    SUPER_ADMIN,
    FINANCE_ADMIN,
    TOURNAMENT_ADMIN;

    companion object {
        fun fromRaw(raw: String?): AdminRole =
            entries.firstOrNull { it.name == raw } ?: TOURNAMENT_ADMIN
    }
}

enum class GameMode(val label: String) {
    SOLO("Solo"),
    DUO("Duo"),
    SQUAD("Squad");

    companion object {
        fun fromRaw(raw: String?): GameMode =
            entries.firstOrNull { it.name == raw } ?: SQUAD
    }
}

enum class TournamentStatus(val label: String) {
    UPCOMING("Upcoming"),
    LIVE("Live"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    companion object {
        fun fromRaw(raw: String?): TournamentStatus =
            entries.firstOrNull { it.name == raw } ?: UPCOMING
    }
}

enum class RequestStatus(val label: String) {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    companion object {
        fun fromRaw(raw: String?): RequestStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class PayoutMethod(val label: String) {
    BANK_ACCOUNT("Bank Account"),
    UPI("UPI");

    companion object {
        fun fromRaw(raw: String?): PayoutMethod =
            entries.firstOrNull { it.name == raw } ?: BANK_ACCOUNT
    }
}

enum class TicketStatus(val label: String) {
    OPEN("Open"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved");

    companion object {
        fun fromRaw(raw: String?): TicketStatus =
            entries.firstOrNull { it.name == raw } ?: OPEN
    }
}

@Keep
@IgnoreExtraProperties
data class AdminUser(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = AdminRole.SUPER_ADMIN.name,
    val active: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PlayerUser(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val freeFireUid: String = "",
    val photoUrl: String = "",
    val blocked: Boolean = false,
    val matchesPlayed: Long = 0L,
    val totalWins: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PlayerWallet(
    @DocumentId val id: String = "",
    val userId: String = "",
    val balance: Double = 0.0,
    val totalDeposited: Double = 0.0,
    val totalWithdrawn: Double = 0.0,
    val totalWinnings: Double = 0.0,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Tournament(
    @DocumentId val id: String = "",
    val title: String = "",
    val gameMode: String = GameMode.SQUAD.name,
    val mapName: String = "Bermuda",
    val entryFee: Double = 0.0,
    val prizePool: Double = 0.0,
    val firstPrize: Double = 0.0,
    val secondPrize: Double = 0.0,
    val thirdPrize: Double = 0.0,
    val perKillPrize: Double = 0.0,
    val maxPlayers: Long = 48L,
    val registeredCount: Long = 0L,
    val scheduledDate: String = "",
    val startTime: String = "",
    val registrationCloseTime: String = "",
    val rules: String = "",
    val status: String = TournamentStatus.UPCOMING.name,
    val published: Boolean = true,
    val roomId: String = "",
    val roomPassword: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Registration(
    @DocumentId val id: String = "",
    val tournamentId: String = "",
    val userId: String = "",
    val playerName: String = "",
    val freeFireUid: String = "",
    val teamName: String = "",
    val slotNumber: Long = 1L,
    val entryFeePaid: Double = 0.0,
    val status: String = "CONFIRMED",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DepositRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val playerName: String = "",
    val amount: Double = 0.0,
    val paymentMethod: String = "UPI",
    val paymentLinkUsed: String = "",
    val transactionRef: String = "",
    val status: String = RequestStatus.PENDING.name,
    val adminNote: String = "",
    val reviewedBy: String = "",
    val reviewedAt: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WithdrawalRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val playerName: String = "",
    val amount: Double = 0.0,
    val payoutMethod: String = PayoutMethod.BANK_ACCOUNT.name,
    val accountHolderName: String = "",
    val accountNumber: String = "",
    val ifscCode: String = "",
    val bankName: String = "",
    val upiId: String = "",
    val status: String = RequestStatus.PENDING.name,
    val payoutReference: String = "",
    val adminNote: String = "",
    val reviewedBy: String = "",
    val reviewedAt: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WalletTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "DEPOSIT",
    val amount: Double = 0.0,
    val referenceId: String = "",
    val description: String = "",
    val status: String = "COMPLETED",
    val createdBy: String = "",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TournamentWinner(
    @DocumentId val id: String = "",
    val tournamentId: String = "",
    val tournamentTitle: String = "",
    val userId: String = "",
    val playerName: String = "",
    val freeFireUid: String = "",
    val rank: Long = 1L,
    val kills: Long = 0L,
    val prizeAmount: Double = 0.0,
    val allocatedBy: String = "",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val targetUserId: String = "ALL",
    val type: String = "ANNOUNCEMENT",
    val tournamentId: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val playerName: String = "",
    val email: String = "",
    val subject: String = "",
    val message: String = "",
    val status: String = TicketStatus.OPEN.name,
    val adminReply: String = "",
    val resolvedBy: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PaymentSettings(
    @DocumentId val id: String = "payment",
    val tier50Link: String = DEFAULT_TIER_50_LINK,
    val tier100Link: String = DEFAULT_TIER_100_LINK,
    val tier200Link: String = DEFAULT_TIER_200_LINK,
    val tier500Link: String = DEFAULT_TIER_500_LINK,
    val bankWithdrawalEnabled: Boolean = true,
    val upiWithdrawalEnabled: Boolean = true,
    val minDeposit: Double = 50.0,
    val minWithdrawal: Double = 100.0,
    val supportEmail: String = "shubadeepsing909@gmail.com",
    val updatedBy: String = "",
    val updatedAt: Timestamp? = null
) {
    companion object {
        const val DEFAULT_TIER_50_LINK = "https://rupayex.net/pay-link/a675b1030f8ad78b0194cd0291c9e568"
        const val DEFAULT_TIER_100_LINK = "https://www.lg-pay.com/order/upi/ORDCEQIG0Z5EU"
        const val DEFAULT_TIER_200_LINK = "https://www.lg-pay.com/order/upi/ORD2AIXJVLQZZ"
        const val DEFAULT_TIER_500_LINK = "https://www.lg-pay.com/order/upi/ORD4JABS5EJ5FJ"
    }
}

@Keep
@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val action: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val details: String = "",
    val createdAt: Timestamp? = null
)

data class DashboardStats(
    val totalUsers: Int = 0,
    val totalTournaments: Int = 0,
    val activeTournaments: Int = 0,
    val completedTournaments: Int = 0,
    val pendingDeposits: Int = 0,
    val pendingWithdrawals: Int = 0,
    val totalVerifiedDeposits: Double = 0.0,
    val totalApprovedPayouts: Double = 0.0
)
