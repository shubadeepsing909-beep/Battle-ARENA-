package com.example.data.repository

import com.example.data.model.AdminRole
import com.example.data.model.AdminUser
import com.example.data.model.AuditLog
import com.example.data.model.NotificationItem
import com.example.data.model.PlayerUser
import com.example.data.model.PlayerWallet
import com.example.data.model.SupportTicket
import com.example.data.model.TicketStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.tasks.await

class UserAndOpsRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireAdminIdentity(): Pair<String, String> {
        val user = auth.currentUser
            ?: throw IllegalStateException("Administrator must be signed in.")
        return user.uid to (user.email ?: "admin@battlearena")
    }

    fun observeUsers(): Flow<List<PlayerUser>> =
        db.collection("users")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PlayerUser::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "users")
                }
                throw error
            }

    fun observeWallets(): Flow<Map<String, PlayerWallet>> =
        db.collection("wallets")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PlayerWallet::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).associateBy { it.userId.ifBlank { it.id } }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "wallets")
                }
                throw error
            }

    fun observeNotifications(): Flow<List<NotificationItem>> =
        db.collection("notifications")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    NotificationItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "notifications")
                }
                throw error
            }

    fun observeSupportTickets(): Flow<List<SupportTicket>> =
        db.collection("support")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    SupportTicket::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "support")
                }
                throw error
            }

    fun observeAuditLogs(): Flow<List<AuditLog>> =
        db.collection("auditLogs")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    AuditLog::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "auditLogs")
                }
                throw error
            }

    fun observeAdmins(): Flow<List<AdminUser>> =
        db.collection("admins")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    AdminUser::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.email }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "admins")
                }
                throw error
            }

    suspend fun toggleUserBlocked(user: PlayerUser): Result<Unit> {
        val path = "users/${user.id}"
        return try {
            val (adminUid, adminEmail) = requireAdminIdentity()
            val nextBlocked = !user.blocked
            db.collection("users").document(user.id).update(
                mapOf(
                    "blocked" to nextBlocked,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog(
                adminUid, adminEmail,
                if (nextBlocked) "BLOCK_USER" else "UNBLOCK_USER",
                "USER", user.id,
                "${if (nextBlocked) "Blocked" else "Unblocked"} player ${user.displayName} (${user.email})"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adjustUserWallet(
        user: PlayerUser,
        amount: Double,
        isCredit: Boolean,
        reason: String
    ): Result<Unit> {
        val walletRef = db.collection("wallets").document(user.id)
        return try {
            val (adminUid, adminEmail) = requireAdminIdentity()
            require(amount > 0.0) { "Amount must be greater than ₹0." }
            require(reason.trim().length >= 3) { "Provide an audit reason (min 3 chars)." }

            val txRef = db.collection("transactions").document()
            val auditRef = db.collection("auditLogs").document()

            db.runTransaction { tx ->
                val walletSnap = tx.get(walletRef)
                val existing = walletSnap.toObject(
                    PlayerWallet::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                val currentBalance = existing?.balance ?: 0.0
                val nextBalance = if (isCredit) currentBalance + amount else currentBalance - amount
                if (nextBalance < 0.0) {
                    throw IllegalStateException("Debit exceeds current balance (₹$currentBalance).")
                }

                if (walletSnap.exists() && existing != null) {
                    tx.update(
                        walletRef,
                        mapOf(
                            "balance" to nextBalance,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    tx.set(
                        walletRef,
                        mapOf(
                            "userId" to user.id,
                            "balance" to nextBalance,
                            "totalDeposited" to 0.0,
                            "totalWithdrawn" to 0.0,
                            "totalWinnings" to 0.0,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                tx.set(
                    txRef,
                    mapOf(
                        "userId" to user.id,
                        "type" to if (isCredit) "ADMIN_CREDIT" else "ADMIN_DEBIT",
                        "amount" to amount,
                        "referenceId" to txRef.id,
                        "description" to reason.trim(),
                        "status" to "COMPLETED",
                        "createdBy" to adminUid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(
                    auditRef,
                    mapOf(
                        "adminUid" to adminUid,
                        "adminEmail" to adminEmail,
                        "action" to if (isCredit) "ADMIN_WALLET_CREDIT" else "ADMIN_WALLET_DEBIT",
                        "targetType" to "WALLET",
                        "targetId" to user.id,
                        "details" to "${if (isCredit) "Credited" else "Debited"} ₹$amount for ${user.displayName}: ${reason.trim()}",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, walletRef.path)
            Result.failure(e)
        }
    }

    suspend fun sendNotification(
        title: String,
        message: String,
        targetUserId: String,
        type: String,
        tournamentId: String
    ): Result<Unit> {
        val docRef = db.collection("notifications").document()
        return try {
            val (adminUid, adminEmail) = requireAdminIdentity()
            require(title.trim().isNotEmpty() && message.trim().isNotEmpty()) {
                "Title and message cannot be empty."
            }
            val cleanPayload = buildMap {
                put("title", title.trim())
                put("message", message.trim())
                put("targetUserId", targetUserId.trim().ifBlank { "ALL" })
                put("type", type)
                if (tournamentId.isNotBlank()) put("tournamentId", tournamentId.trim())
                put("createdBy", adminUid)
                put("createdAt", FieldValue.serverTimestamp())
            }
            docRef.set(cleanPayload).await()
            writeAuditLog(
                adminUid, adminEmail, "SEND_NOTIFICATION", "NOTIFICATION", docRef.id,
                "Sent $type notification: ${title.trim()} -> ${targetUserId.ifBlank { "ALL" }}"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateSupportTicket(
        ticket: SupportTicket,
        newStatus: TicketStatus,
        adminReply: String
    ): Result<Unit> {
        val path = "support/${ticket.id}"
        return try {
            val (adminUid, adminEmail) = requireAdminIdentity()
            db.collection("support").document(ticket.id).update(
                mapOf(
                    "status" to newStatus.name,
                    "adminReply" to adminReply.trim(),
                    "resolvedBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog(
                adminUid, adminEmail, "UPDATE_SUPPORT_TICKET", "SUPPORT", ticket.id,
                "Set ticket '${ticket.subject}' to ${newStatus.name}"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertAdminRole(
        targetUid: String,
        email: String,
        displayName: String,
        role: AdminRole,
        active: Boolean
    ): Result<Unit> {
        val cleanUid = targetUid.trim()
        val path = "admins/$cleanUid"
        return try {
            val (adminUid, adminEmail) = requireAdminIdentity()
            require(cleanUid.matches(Regex("^[a-zA-Z0-9_\\-]{1,128}$"))) {
                "Enter a valid Firebase UID."
            }
            require(email.trim().contains("@")) { "Enter a valid Gmail address." }
            val ref = db.collection("admins").document(cleanUid)
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(
                    mapOf(
                        "displayName" to displayName.trim().ifBlank { "Arena Admin" },
                        "role" to role.name,
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "uid" to cleanUid,
                        "email" to email.trim(),
                        "displayName" to displayName.trim().ifBlank { "Arena Admin" },
                        "role" to role.name,
                        "active" to active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            writeAuditLog(
                adminUid, adminEmail, "UPSERT_ADMIN_ROLE", "ADMIN", cleanUid,
                "Configured admin ${email.trim()} as ${role.name} (active=$active)"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun removeAdminRole(targetAdmin: AdminUser): Result<Unit> {
        val path = "admins/${targetAdmin.id}"
        return try {
            val (adminUid, adminEmail) = requireAdminIdentity()
            require(targetAdmin.id != adminUid) { "You cannot revoke your own active admin session." }
            db.collection("admins").document(targetAdmin.id).delete().await()
            writeAuditLog(
                adminUid, adminEmail, "REVOKE_ADMIN_ROLE", "ADMIN", targetAdmin.id,
                "Revoked admin access for ${targetAdmin.email}"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private suspend fun writeAuditLog(
        adminUid: String,
        adminEmail: String,
        action: String,
        targetType: String,
        targetId: String,
        details: String
    ) {
        db.collection("auditLogs").document().set(
            mapOf(
                "adminUid" to adminUid,
                "adminEmail" to adminEmail,
                "action" to action,
                "targetType" to targetType,
                "targetId" to targetId,
                "details" to details.take(500),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }
}
