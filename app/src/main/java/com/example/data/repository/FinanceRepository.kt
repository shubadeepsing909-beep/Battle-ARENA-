package com.example.data.repository

import com.example.data.model.DepositRequest
import com.example.data.model.PaymentSettings
import com.example.data.model.PlayerWallet
import com.example.data.model.RequestStatus
import com.example.data.model.WalletTransaction
import com.example.data.model.WithdrawalRequest
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

class FinanceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireAdminIdentity(): Pair<String, String> {
        val user = auth.currentUser
            ?: throw IllegalStateException("Administrator must be signed in.")
        return user.uid to (user.email ?: "admin@battlearena")
    }

    fun observeDeposits(): Flow<List<DepositRequest>> =
        db.collection("deposits")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    DepositRequest::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "deposits")
                }
                throw error
            }

    fun observeWithdrawals(): Flow<List<WithdrawalRequest>> =
        db.collection("withdrawals")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    WithdrawalRequest::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "withdrawals")
                }
                throw error
            }

    fun observePaymentSettings(): Flow<PaymentSettings> =
        db.collection("settings").document("payment")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        PaymentSettings::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: PaymentSettings()
                } else {
                    PaymentSettings()
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, "settings/payment")
                }
                throw error
            }

    fun observeUserTransactions(userId: String): Flow<List<WalletTransaction>> =
        db.collection("transactions")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    WalletTransaction::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "transactions")
                }
                throw error
            }

    // Atomic Deposit Verification & Approval (prevents duplicate approvals)
    suspend fun approveDeposit(deposit: DepositRequest, adminNote: String): Result<Unit> {
        val depositRef = db.collection("deposits").document(deposit.id)
        return try {
            val (adminUid, adminEmail) = requireAdminIdentity()
            val walletRef = db.collection("wallets").document(deposit.userId)
            val txRef = db.collection("transactions").document()
            val auditRef = db.collection("auditLogs").document()

            db.runTransaction { tx ->
                val latestDeposit = tx.get(depositRef)
                val currentStatus = latestDeposit.getString("status") ?: ""
                if (currentStatus != RequestStatus.PENDING.name) {
                    throw IllegalStateException("Deposit already processed ($currentStatus). Duplicate approval blocked.")
                }

                val walletSnap = tx.get(walletRef)
                val existingWallet = walletSnap.toObject(
                    PlayerWallet::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )

                if (walletSnap.exists() && existingWallet != null) {
                    tx.update(
                        walletRef,
                        mapOf(
                            "balance" to (existingWallet.balance + deposit.amount),
                            "totalDeposited" to (existingWallet.totalDeposited + deposit.amount),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    tx.set(
                        walletRef,
                        mapOf(
                            "userId" to deposit.userId,
                            "balance" to deposit.amount,
                            "totalDeposited" to deposit.amount,
                            "totalWithdrawn" to 0.0,
                            "totalWinnings" to 0.0,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                tx.update(
                    depositRef,
                    mapOf(
                        "status" to RequestStatus.APPROVED.name,
                        "adminNote" to adminNote.trim().ifBlank { "Verified & credited" },
                        "reviewedBy" to adminUid,
                        "reviewedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(
                    txRef,
                    mapOf(
                        "userId" to deposit.userId,
                        "type" to "DEPOSIT",
                        "amount" to deposit.amount,
                        "referenceId" to deposit.transactionRef,
                        "description" to "Verified ${deposit.paymentMethod} deposit",
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
                        "action" to "APPROVE_DEPOSIT",
                        "targetType" to "DEPOSIT",
                        "targetId" to deposit.id,
                        "details" to "Approved ₹${deposit.amount} deposit for ${deposit.playerName} (Ref: ${deposit.transactionRef})",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, depositRef.path)
            Result.failure(e)
        }
    }

    suspend fun rejectDeposit(deposit: DepositRequest, reason: String): Result<Unit> {
        val depositRef = db.collection("deposits").document(deposit.id)
        return try {
            val (adminUid, adminEmail) = requireAdminIdentity()
            val auditRef = db.collection("auditLogs").document()

            db.runTransaction { tx ->
                val latest = tx.get(depositRef)
                val currentStatus = latest.getString("status") ?: ""
                if (currentStatus != RequestStatus.PENDING.name) {
                    throw IllegalStateException("Deposit already processed ($currentStatus).")
                }
                tx.update(
                    depositRef,
                    mapOf(
                        "status" to RequestStatus.REJECTED.name,
                        "adminNote" to reason.trim().ifBlank { "Payment reference could not be verified" },
                        "reviewedBy" to adminUid,
                        "reviewedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    auditRef,
                    mapOf(
                        "adminUid" to adminUid,
                        "adminEmail" to adminEmail,
                        "action" to "REJECT_DEPOSIT",
                        "targetType" to "DEPOSIT",
                        "targetId" to deposit.id,
                        "details" to "Rejected ₹${deposit.amount} deposit for ${deposit.playerName}: ${reason.trim()}",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, depositRef.path)
            Result.failure(e)
        }
    }

    // Atomic Withdrawal Approval (Bank Account or UPI) with balance check & payout reference
    suspend fun approveWithdrawal(
        withdrawal: WithdrawalRequest,
        payoutReference: String,
        adminNote: String
    ): Result<Unit> {
        val withdrawalRef = db.collection("withdrawals").document(withdrawal.id)
        return try {
            val (adminUid, adminEmail) = requireAdminIdentity()
            require(payoutReference.trim().length >= 3) {
                "Enter a valid payout transaction reference ID."
            }
            val walletRef = db.collection("wallets").document(withdrawal.userId)
            val txRef = db.collection("transactions").document()
            val auditRef = db.collection("auditLogs").document()

            db.runTransaction { tx ->
                val latestWd = tx.get(withdrawalRef)
                val currentStatus = latestWd.getString("status") ?: ""
                if (currentStatus != RequestStatus.PENDING.name) {
                    throw IllegalStateException("Withdrawal already processed ($currentStatus). Duplicate payout blocked.")
                }

                val walletSnap = tx.get(walletRef)
                val wallet = walletSnap.toObject(
                    PlayerWallet::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: throw IllegalStateException("Player wallet not found.")

                if (wallet.balance < withdrawal.amount) {
                    throw IllegalStateException(
                        "Insufficient wallet balance (₹${wallet.balance}) for ₹${withdrawal.amount} payout."
                    )
                }

                tx.update(
                    walletRef,
                    mapOf(
                        "balance" to (wallet.balance - withdrawal.amount),
                        "totalWithdrawn" to (wallet.totalWithdrawn + withdrawal.amount),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.update(
                    withdrawalRef,
                    mapOf(
                        "status" to RequestStatus.APPROVED.name,
                        "payoutReference" to payoutReference.trim(),
                        "adminNote" to adminNote.trim().ifBlank { "Payout settled via ${withdrawal.payoutMethod}" },
                        "reviewedBy" to adminUid,
                        "reviewedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(
                    txRef,
                    mapOf(
                        "userId" to withdrawal.userId,
                        "type" to "WITHDRAWAL",
                        "amount" to withdrawal.amount,
                        "referenceId" to payoutReference.trim(),
                        "description" to "${withdrawal.payoutMethod} payout settled",
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
                        "action" to "APPROVE_WITHDRAWAL",
                        "targetType" to "WITHDRAWAL",
                        "targetId" to withdrawal.id,
                        "details" to "Paid ₹${withdrawal.amount} via ${withdrawal.payoutMethod} to ${withdrawal.playerName} (Ref: ${payoutReference.trim()})",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, withdrawalRef.path)
            Result.failure(e)
        }
    }

    suspend fun rejectWithdrawal(withdrawal: WithdrawalRequest, reason: String): Result<Unit> {
        val withdrawalRef = db.collection("withdrawals").document(withdrawal.id)
        return try {
            val (adminUid, adminEmail) = requireAdminIdentity()
            val auditRef = db.collection("auditLogs").document()

            db.runTransaction { tx ->
                val latest = tx.get(withdrawalRef)
                val currentStatus = latest.getString("status") ?: ""
                if (currentStatus != RequestStatus.PENDING.name) {
                    throw IllegalStateException("Withdrawal already processed ($currentStatus).")
                }
                tx.update(
                    withdrawalRef,
                    mapOf(
                        "status" to RequestStatus.REJECTED.name,
                        "payoutReference" to "",
                        "adminNote" to reason.trim().ifBlank { "Withdrawal rejected by admin" },
                        "reviewedBy" to adminUid,
                        "reviewedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    auditRef,
                    mapOf(
                        "adminUid" to adminUid,
                        "adminEmail" to adminEmail,
                        "action" to "REJECT_WITHDRAWAL",
                        "targetType" to "WITHDRAWAL",
                        "targetId" to withdrawal.id,
                        "details" to "Rejected ₹${withdrawal.amount} withdrawal for ${withdrawal.playerName}: ${reason.trim()}",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, withdrawalRef.path)
            Result.failure(e)
        }
    }

    suspend fun savePaymentSettings(settings: PaymentSettings): Result<Unit> {
        val path = "settings/payment"
        return try {
            val (adminUid, adminEmail) = requireAdminIdentity()
            require(settings.minDeposit >= 1.0 && settings.minWithdrawal >= 1.0) {
                "Minimum deposit and withdrawal must be at least ₹1."
            }
            db.collection("settings").document("payment").set(
                mapOf(
                    "tier50Link" to settings.tier50Link.trim(),
                    "tier100Link" to settings.tier100Link.trim(),
                    "tier200Link" to settings.tier200Link.trim(),
                    "tier500Link" to settings.tier500Link.trim(),
                    "bankWithdrawalEnabled" to settings.bankWithdrawalEnabled,
                    "upiWithdrawalEnabled" to settings.upiWithdrawalEnabled,
                    "minDeposit" to settings.minDeposit,
                    "minWithdrawal" to settings.minWithdrawal,
                    "supportEmail" to settings.supportEmail.trim(),
                    "updatedBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("auditLogs").document().set(
                mapOf(
                    "adminUid" to adminUid,
                    "adminEmail" to adminEmail,
                    "action" to "UPDATE_PAYMENT_SETTINGS",
                    "targetType" to "SETTINGS",
                    "targetId" to "payment",
                    "details" to "Updated deposit gateway tier links & Bank/UPI withdrawal controls",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
