package com.example.data.repository

import com.example.data.model.AdminRole
import com.example.data.model.AdminUser
import com.example.data.model.PaymentSettings
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

sealed interface AdminSessionState {
    data object Loading : AdminSessionState
    data object Unauthenticated : AdminSessionState
    data class Authorized(val user: FirebaseUser, val admin: AdminUser) : AdminSessionState
    data class AccessDenied(val user: FirebaseUser, val reason: String) : AdminSessionState
}

class AdminAuthRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    companion object {
        const val PREDEFINED_ADMIN_EMAIL = "shubadeepsing909@gmail.com"
        const val PREDEFINED_ADMIN_UID = "kOXOAgPVKudCdefAI8x22hD32TQ2"

        fun isPredefinedAdmin(
            email: String?,
            isEmailVerified: Boolean,
            uid: String?
        ): Boolean {
            val matchesVerifiedEmail = !email.isNullOrBlank() &&
                email.equals(PREDEFINED_ADMIN_EMAIL, ignoreCase = true) &&
                isEmailVerified
            val matchesPredefinedUid = !uid.isNullOrBlank() && uid == PREDEFINED_ADMIN_UID
            return matchesVerifiedEmail || matchesPredefinedUid
        }
    }

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun observeAdminSession(user: FirebaseUser): Flow<AdminSessionState> = flow {
        emit(AdminSessionState.Loading)
        val initialResult = verifyAndBootstrapAdmin(user)
        if (initialResult is AdminSessionState.AccessDenied) {
            emit(initialResult)
            return@flow
        }

        val email = user.email.orEmpty()
        val isPredefined = isPredefinedAdmin(email, user.isEmailVerified, user.uid)
        val adminRef = db.collection("admins").document(user.uid)

        emitAll(
            adminRef.snapshots()
                .map { snapshot ->
                    val adminDoc = if (snapshot.exists()) {
                        snapshot.toObject(
                            AdminUser::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null

                    when {
                        adminDoc != null && adminDoc.active -> {
                            AdminSessionState.Authorized(user, adminDoc)
                        }
                        adminDoc != null && !adminDoc.active -> {
                            AdminSessionState.AccessDenied(
                                user,
                                "Administrator privileges for $email are currently suspended."
                            )
                        }
                        isPredefined -> {
                            AdminSessionState.Authorized(
                                user,
                                AdminUser(
                                    id = user.uid,
                                    uid = user.uid,
                                    email = email.ifBlank { PREDEFINED_ADMIN_EMAIL },
                                    displayName = user.displayName?.takeIf { it.isNotBlank() }
                                        ?: "Battle Arena Super Admin",
                                    role = AdminRole.SUPER_ADMIN.name,
                                    active = true
                                )
                            )
                        }
                        else -> {
                            AdminSessionState.AccessDenied(
                                user,
                                "Administrator access has been revoked for $email."
                            )
                        }
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == user.uid) {
                        handleFirestoreError(
                            firestoreError ?: error,
                            OperationType.GET,
                            "admins/${user.uid}"
                        )
                    }
                    emit(
                        AdminSessionState.AccessDenied(
                            user,
                            extractFirestoreErrorMessage(
                                error,
                                "Unable to verify live administrator permissions."
                            )
                        )
                    )
                }
        )
    }

    suspend fun verifyAndBootstrapAdmin(user: FirebaseUser): AdminSessionState {
        return try {
            val email = user.email.orEmpty()
            val isPredefined = isPredefinedAdmin(email, user.isEmailVerified, user.uid)
            val adminRef = db.collection("admins").document(user.uid)
            val snapshot = adminRef.get().await()

            if (snapshot.exists()) {
                val adminUser = snapshot.toObject(
                    AdminUser::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (adminUser != null && adminUser.active) {
                    ensureDefaultPaymentSettings(user.uid)
                    recordAdminLoginAudit(user.uid, email.ifBlank { adminUser.email })
                    AdminSessionState.Authorized(user, adminUser)
                } else {
                    AdminSessionState.AccessDenied(
                        user,
                        "This administrator account is currently deactivated."
                    )
                }
            } else if (isPredefined) {
                val resolvedEmail = email.ifBlank { PREDEFINED_ADMIN_EMAIL }
                val resolvedName = user.displayName?.takeIf { it.isNotBlank() } ?: "Battle Arena Super Admin"
                val newAdminPayload = mapOf(
                    "uid" to user.uid,
                    "email" to resolvedEmail,
                    "displayName" to resolvedName,
                    "role" to AdminRole.SUPER_ADMIN.name,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                adminRef.set(newAdminPayload).await()
                ensureDefaultPaymentSettings(user.uid)
                recordAdminLoginAudit(user.uid, resolvedEmail)

                val created = adminRef.get().await().toObject(
                    AdminUser::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: AdminUser(
                    id = user.uid,
                    uid = user.uid,
                    email = resolvedEmail,
                    displayName = resolvedName,
                    role = AdminRole.SUPER_ADMIN.name,
                    active = true
                )
                AdminSessionState.Authorized(user, created)
            } else {
                AdminSessionState.AccessDenied(
                    user,
                    "Signed in as ${email.ifBlank { user.uid }}. Only authorized Battle Arena administrators may access this console."
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "admins/${user.uid}")
            AdminSessionState.AccessDenied(
                user,
                e.localizedMessage ?: "Failed to verify administrator permissions."
            )
        }
    }

    private suspend fun ensureDefaultPaymentSettings(adminUid: String) {
        val settingsRef = db.collection("settings").document("payment")
        val existing = settingsRef.get().await()
        if (!existing.exists()) {
            val defaultSettings = mapOf(
                "tier50Link" to PaymentSettings.DEFAULT_TIER_50_LINK,
                "tier100Link" to PaymentSettings.DEFAULT_TIER_100_LINK,
                "tier200Link" to PaymentSettings.DEFAULT_TIER_200_LINK,
                "tier500Link" to PaymentSettings.DEFAULT_TIER_500_LINK,
                "bankWithdrawalEnabled" to true,
                "upiWithdrawalEnabled" to true,
                "minDeposit" to 50.0,
                "minWithdrawal" to 100.0,
                "supportEmail" to PREDEFINED_ADMIN_EMAIL,
                "updatedBy" to adminUid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            settingsRef.set(defaultSettings).await()
        }
    }

    private suspend fun recordAdminLoginAudit(adminUid: String, adminEmail: String) {
        try {
            db.collection("auditLogs").document().set(
                mapOf(
                    "adminUid" to adminUid,
                    "adminEmail" to adminEmail.ifBlank { PREDEFINED_ADMIN_EMAIL },
                    "action" to "ADMIN_LOGIN",
                    "targetType" to "SESSION",
                    "targetId" to adminUid,
                    "details" to "Verified Google Sign-In session started for Admin Console",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Non-fatal if audit log write fails due to transient offline cache
        }
    }
}
