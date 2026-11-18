package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.AdminAuthRepository
import com.example.data.repository.AdminSessionState
import com.example.data.repository.FinanceRepository
import com.example.data.repository.TournamentRepository
import com.example.data.repository.UserAndOpsRepository
import com.example.ui.AdminAppRoot
import com.example.ui.auth.AccessDeniedScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.performSignOut
import com.example.ui.components.LoadingStateView
import com.example.ui.theme.ArenaCanvas
import com.example.ui.theme.BattleArenaTheme
import com.example.ui.viewmodel.AdminPanelViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BattleArenaTheme {
                BattleArenaRoot()
            }
        }
    }
}

@Composable
fun BattleArenaRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val db = remember(context) {
        val databaseId = context.applicationContext.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(databaseId)
    }
    val authRepo = remember(db) { AdminAuthRepository(db) }
    val currentUser by authRepo.authStateFlow().collectAsStateWithLifecycle(
        initialValue = Firebase.auth.currentUser
    )

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            val sessionFlow = remember(user.uid) { authRepo.observeAdminSession(user) }
            val sessionState by sessionFlow.collectAsStateWithLifecycle(
                initialValue = AdminSessionState.Loading
            )

            when (val state = sessionState) {
                AdminSessionState.Loading,
                AdminSessionState.Unauthenticated -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ArenaCanvas),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingStateView("Verifying Battle Arena Admin access...")
                    }
                }

                is AdminSessionState.AccessDenied -> {
                    AccessDeniedScreen(
                        email = state.user.email ?: "Unknown email",
                        uid = state.user.uid,
                        isEmailVerified = state.user.isEmailVerified,
                        reason = state.reason,
                        onSignOut = {
                            performSignOut(credentialManager, scope)
                        }
                    )
                }

                is AdminSessionState.Authorized -> {
                    val adminViewModel: AdminPanelViewModel = viewModel(
                        key = user.uid,
                        factory = viewModelFactory {
                            initializer {
                                val app = checkNotNull(this[APPLICATION_KEY]) {
                                    "APPLICATION_KEY missing from CreationExtras"
                                }
                                val databaseId = app.getString(R.string.firestore_database_id)
                                val firestore = FirebaseFirestore.getInstance(databaseId)
                                AdminPanelViewModel(
                                    tournamentRepo = TournamentRepository(firestore),
                                    financeRepo = FinanceRepository(firestore),
                                    opsRepo = UserAndOpsRepository(firestore)
                                )
                            }
                        }
                    )

                    AdminAppRoot(
                        currentAdmin = state.admin,
                        viewModel = adminViewModel,
                        onSignOut = {
                            performSignOut(credentialManager, scope)
                        }
                    )
                }
            }
        }
    }
}
