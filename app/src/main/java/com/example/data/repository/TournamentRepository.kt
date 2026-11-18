package com.example.data.repository

import com.example.data.model.PlayerWallet
import com.example.data.model.Registration
import com.example.data.model.Tournament
import com.example.data.model.TournamentStatus
import com.example.data.model.TournamentWinner
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

data class TournamentFormInput(
    val title: String,
    val gameMode: String,
    val mapName: String,
    val entryFee: Double,
    val prizePool: Double,
    val firstPrize: Double,
    val secondPrize: Double,
    val thirdPrize: Double,
    val perKillPrize: Double,
    val maxPlayers: Long,
    val scheduledDate: String,
    val startTime: String,
    val registrationCloseTime: String,
    val rules: String,
    val published: Boolean
)

class TournamentRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireAdminIdentity(): Pair<String, String> {
        val user = auth.currentUser
            ?: throw IllegalStateException("Administrator must be signed in.")
        return user.uid to (user.email ?: "admin@battlearena")
    }

    fun observeTournaments(): Flow<List<Tournament>> =
        db.collection("tournaments")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Tournament::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "tournaments")
                }
                throw error
            }

    fun observeRegistrations(tournamentId: String): Flow<List<Registration>> =
        db.collection("registrations")
            .whereEqualTo("tournamentId", tournamentId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Registration::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.slotNumber }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "registrations")
                }
                throw error
            }

    fun observeWinners(): Flow<List<TournamentWinner>> =
        db.collection("winners")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    TournamentWinner::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "winners")
                }
                throw error
            }

    suspend fun createTournament(input: TournamentFormInput): Result<String> {
        val docRef = db.collection("tournaments").document()
        return try {
            val (adminUid, adminEmail) = requireAdminIdentity()
            require(input.title.trim().length in 2..120) { "Title must be 2 to 120 characters." }
            require(input.maxPlayers in 2..1000) { "Max players must be between 2 and 1000." }

            val payload = mapOf(
                "title" to input.title.trim(),
                "gameMode" to input.gameMode,
                "mapName" to input.mapName.trim(),
                "entryFee" to input.entryFee.coerceAtLeast(0.0),
                "prizePool" to input.prizePool.coerceAtLeast(0.0),
                "firstPrize" to input.firstPrize.coerceAtLeast(0.0),
                "secondPrize" to input.secondPrize.coerceAtLeast(0.0),
                "thirdPrize" to input.thirdPrize.coerceAtLeast(0.0),
                "perKillPrize" to input.perKillPrize.coerceAtLeast(0.0),
                "maxPlayers" to input.maxPlayers,
                "registeredCount" to 0L,
                "scheduledDate" to input.scheduledDate.trim(),
                "startTime" to input.startTime.trim(),
                "registrationCloseTime" to input.registrationCloseTime.trim(),
                "rules" to input.rules.trim(),
                "status" to TournamentStatus.UPCOMING.name,
                "published" to input.published,
                "roomId" to "",
                "roomPassword" to "",
                "createdBy" to adminUid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            writeAuditLog(
                adminUid, adminEmail, "CREATE_TOURNAMENT", "TOURNAMENT", docRef.id,
                "Created ${input.title.trim()} (${input.gameMode}) Prize ₹${input.prizePool.toInt()}"
            )
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateTournament(tournamentId: String, input: TournamentFormInput): Result<Unit> {
        val path = "tournaments/$tournamentId"
        return try {
            val (adminUid, adminEmail) = requireAdminIdentity()
            require(input.title.trim().length in 2..120) { "Title must be 2 to 120 characters." }
            require(input.maxPlayers in 2..1000) { "Max players must be between 2 and 1000." }

            db.collection("tournaments").document(tournamentId).update(
                mapOf(
                    "title" to input.title.trim(),
                    "gameMode" to input.gameMode,
                    "mapName" to input.mapName.trim(),
                    "entryFee" to input.entryFee.coerceAtLeast(0.0),
                    "prizePool" to input.prizePool.coerceAtLeast(0.0),
                    "firstPrize" to input.firstPrize.coerceAtLeast(0.0),
                    "secondPrize" to input.secondPrize.coerceAtLeast(0.0),
                    "thirdPrize" to input.thirdPrize.coerceAtLeast(0.0),
                    "perKillPrize" to input.perKillPrize.coerceAtLeast(0.0),
                    "maxPlayers" to input.maxPlayers,
                    "scheduledDate" to input.scheduledDate.trim(),
                    "startTime" to input.startTime.trim(),
                    "registrationCloseTime" to input.registrationCloseTime.trim(),
                    "rules" to input.rules.trim(),
                    "published" to input.published,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog(
                adminUid, adminEmail, "UPDATE_TOURNAMENT", "TOURNAMENT", tournamentId,
                "Updated tournament ${input.title.trim()}"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun togglePublished(tournament: Tournament): Result<Unit> {
        val path = "tournaments/${tournament.id}"
        return try {
            val (adminUid, adminEmail) = requireAdminIdentity()
            val nextPublished = !tournament.published
            db.collection("tournaments").document(tournament.id).update(
                mapOf(
                    "published" to nextPublished,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog(
                adminUid, adminEmail,
                if (nextPublished) "PUBLISH_TOURNAMENT" else "UNPUBLISH_TOURNAMENT",
                "TOURNAMENT", tournament.id,
                "${if (nextPublished) "Published" else "Unpublished"} ${tournament.title}"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateRoomCredentials(
        tournamentId: String,
        tournamentTitle: String,
        roomId: String,
        roomPassword: String
    ): Result<Unit> {
        val path = "tournaments/$tournamentId"
        return try {
            val (adminUid, adminEmail) = requireAdminIdentity()
            require(roomId.length <= 64 && roomPassword.length <= 64) {
                "Room credentials cannot exceed 64 characters."
            }
            db.collection("tournaments").document(tournamentId).update(
                mapOf(
                    "roomId" to roomId.trim(),
                    "roomPassword" to roomPassword.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog(
                adminUid, adminEmail, "ASSIGN_ROOM_CREDENTIALS", "TOURNAMENT", tournamentId,
                "Updated Room ID ($roomId) for $tournamentTitle"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateTournamentStatus(
        tournamentId: String,
        tournamentTitle: String,
        newStatus: TournamentStatus
    ): Result<Unit> {
        val path = "tournaments/$tournamentId"
        return try {
            val (adminUid, adminEmail) = requireAdminIdentity()
            db.collection("tournaments").document(tournamentId).update(
                mapOf(
                    "status" to newStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog(
                adminUid, adminEmail, "STATUS_${newStatus.name}", "TOURNAMENT", tournamentId,
                "Changed $tournamentTitle status to ${newStatus.name}"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTournament(tournament: Tournament): Result<Unit> {
        val path = "tournaments/${tournament.id}"
        return try {
            val (adminUid, adminEmail) = requireAdminIdentity()
            db.collection("tournaments").document(tournament.id).delete().await()
            writeAuditLog(
                adminUid, adminEmail, "DELETE_TOURNAMENT", "TOURNAMENT", tournament.id,
                "Deleted tournament ${tournament.title}"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun removeParticipant(registration: Registration, tournamentTitle: String): Result<Unit> {
        val path = "registrations/${registration.id}"
        return try {
            val (adminUid, adminEmail) = requireAdminIdentity()
            val regRef = db.collection("registrations").document(registration.id)
            val tourRef = db.collection("tournaments").document(registration.tournamentId)

            db.runTransaction { tx ->
                val tourSnap = tx.get(tourRef)
                val currentCount = tourSnap.getLong("registeredCount") ?: 0L
                tx.update(
                    regRef,
                    mapOf(
                        "status" to "KICKED",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (tourSnap.exists() && currentCount > 0) {
                    tx.update(
                        tourRef,
                        mapOf(
                            "registeredCount" to (currentCount - 1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()

            writeAuditLog(
                adminUid, adminEmail, "REMOVE_PARTICIPANT", "REGISTRATION", registration.id,
                "Removed ${registration.playerName} (FF UID: ${registration.freeFireUid}) from $tournamentTitle"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun allocateWinnerPrize(
        tournament: Tournament,
        registration: Registration,
        rank: Long,
        kills: Long,
        prizeAmount: Double
    ): Result<Unit> {
        val winnerRef = db.collection("winners").document()
        return try {
            val (adminUid, adminEmail) = requireAdminIdentity()
            require(rank in 1..100) { "Rank must be between 1 and 100." }
            require(kills in 0..100) { "Kills must be between 0 and 100." }
            require(prizeAmount >= 0.0) { "Prize amount cannot be negative." }

            val walletRef = db.collection("wallets").document(registration.userId)
            val userRef = db.collection("users").document(registration.userId)
            val txRef = db.collection("transactions").document()
            val auditRef = db.collection("auditLogs").document()

            db.runTransaction { tx ->
                val walletSnap = tx.get(walletRef)
                val userSnap = tx.get(userRef)

                val existingWallet = walletSnap.toObject(
                    PlayerWallet::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )

                if (prizeAmount > 0.0) {
                    if (walletSnap.exists() && existingWallet != null) {
                        tx.update(
                            walletRef,
                            mapOf(
                                "balance" to (existingWallet.balance + prizeAmount),
                                "totalWinnings" to (existingWallet.totalWinnings + prizeAmount),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    } else {
                        tx.set(
                            walletRef,
                            mapOf(
                                "userId" to registration.userId,
                                "balance" to prizeAmount,
                                "totalDeposited" to 0.0,
                                "totalWithdrawn" to 0.0,
                                "totalWinnings" to prizeAmount,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }

                    tx.set(
                        txRef,
                        mapOf(
                            "userId" to registration.userId,
                            "type" to "PRIZE_WON",
                            "amount" to prizeAmount,
                            "referenceId" to winnerRef.id,
                            "description" to "Rank #$rank prize in ${tournament.title}",
                            "status" to "COMPLETED",
                            "createdBy" to adminUid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                if (userSnap.exists() && rank == 1L) {
                    val wins = userSnap.getLong("totalWins") ?: 0L
                    tx.update(
                        userRef,
                        mapOf(
                            "totalWins" to (wins + 1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                tx.set(
                    winnerRef,
                    mapOf(
                        "tournamentId" to tournament.id,
                        "tournamentTitle" to tournament.title,
                        "userId" to registration.userId,
                        "playerName" to registration.playerName,
                        "freeFireUid" to registration.freeFireUid,
                        "rank" to rank,
                        "kills" to kills,
                        "prizeAmount" to prizeAmount,
                        "allocatedBy" to adminUid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(
                    auditRef,
                    mapOf(
                        "adminUid" to adminUid,
                        "adminEmail" to adminEmail,
                        "action" to "ALLOCATE_WINNER_PRIZE",
                        "targetType" to "WINNER",
                        "targetId" to winnerRef.id,
                        "details" to "Rank #$rank (${registration.playerName}) won ₹$prizeAmount in ${tournament.title}",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, winnerRef.path)
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
