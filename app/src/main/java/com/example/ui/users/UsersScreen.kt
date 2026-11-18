package com.example.ui.users

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.PlayerUser
import com.example.data.model.PlayerWallet
import com.example.data.model.UiState
import com.example.data.model.WalletTransaction
import com.example.ui.components.ArenaCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LoadingStateView
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone
import com.example.ui.components.formatInr
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.ArenaBluePrimary
import com.example.ui.theme.ArenaCanvas
import com.example.ui.theme.ArenaCrimson
import com.example.ui.theme.ArenaEmerald
import com.example.ui.theme.ArenaInkMuted
import com.example.ui.theme.ArenaInkPrimary
import com.example.ui.theme.ArenaInkSecondary
import com.example.ui.theme.ArenaWhite
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun UsersScreen(
    usersState: UiState<List<PlayerUser>>,
    walletsState: UiState<Map<String, PlayerWallet>>,
    userTransactionsState: UiState<List<WalletTransaction>>,
    actionBusy: Boolean,
    onSelectUser: (String?) -> Unit,
    onToggleBlocked: (PlayerUser) -> Unit,
    onAdjustWallet: (PlayerUser, Double, Boolean, String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("ALL") }
    var activeUserId by remember { mutableStateOf<String?>(null) }

    val walletMap = (walletsState as? UiState.Success)?.data.orEmpty()
    val activeUser = remember(usersState, activeUserId) {
        (usersState as? UiState.Success)?.data?.firstOrNull { it.id == activeUserId }
    }

    if (activeUser != null) {
        BackHandler {
            activeUserId = null
            onSelectUser(null)
        }
        UserDetailView(
            user = activeUser,
            wallet = walletMap[activeUser.id],
            transactionsState = userTransactionsState,
            actionBusy = actionBusy,
            onBack = {
                activeUserId = null
                onSelectUser(null)
            },
            onToggleBlocked = { onToggleBlocked(activeUser) },
            onAdjustWallet = { amt, isCredit, reason ->
                onAdjustWallet(activeUser, amt, isCredit, reason)
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ArenaCanvas)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search players by name, Gmail, or FF UID...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_users_input")
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL", "ACTIVE", "BLOCKED").forEach { item ->
                    FilterChip(
                        selected = filter == item,
                        onClick = { filter = item },
                        label = { Text(item) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ArenaBluePrimary,
                            selectedLabelColor = ArenaWhite
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            when (usersState) {
                UiState.Loading -> LoadingStateView("Loading registered players...")
                is UiState.Error -> ErrorBanner(usersState.message)
                is UiState.Success -> {
                    val filtered = usersState.data.filter { u ->
                        val matchesFilter = when (filter) {
                            "ACTIVE" -> !u.blocked
                            "BLOCKED" -> u.blocked
                            else -> true
                        }
                        val matchesQuery = query.isBlank() ||
                            u.displayName.contains(query, ignoreCase = true) ||
                            u.email.contains(query, ignoreCase = true) ||
                            u.freeFireUid.contains(query, ignoreCase = true) ||
                            u.phone.contains(query, ignoreCase = true)
                        matchesFilter && matchesQuery
                    }

                    if (filtered.isEmpty()) {
                        EmptyStateCard(
                            title = "No Players Found",
                            message = "Registered Free Fire players will appear here with wallet balances and UIDs.",
                            icon = Icons.Filled.Groups
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(filtered, key = { it.id }) { user ->
                                val wallet = walletMap[user.id]
                                ArenaCard(
                                    onClick = {
                                        activeUserId = user.id
                                        onSelectUser(user.id)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = user.displayName,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = ArenaInkPrimary
                                            )
                                            Text(
                                                text = user.email,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ArenaInkSecondary
                                            )
                                            Text(
                                                text = "FF UID: ${user.freeFireUid.ifBlank { "Not set" }}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                                color = ArenaInkMuted
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            StatusPill(
                                                text = if (user.blocked) "BLOCKED" else "ACTIVE",
                                                tone = if (user.blocked) StatusTone.RED else StatusTone.GREEN
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = formatInr(wallet?.balance ?: 0.0),
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontFamily = JetBrainsMonoFamily,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = ArenaBluePrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserDetailView(
    user: PlayerUser,
    wallet: PlayerWallet?,
    transactionsState: UiState<List<WalletTransaction>>,
    actionBusy: Boolean,
    onBack: () -> Unit,
    onToggleBlocked: () -> Unit,
    onAdjustWallet: (Double, Boolean, String) -> Unit
) {
    var showAdjustDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaCanvas),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("user_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Player Operations", style = MaterialTheme.typography.titleLarge, color = ArenaInkPrimary)
            }
        }

        item {
            ArenaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.displayName, style = MaterialTheme.typography.headlineSmall, color = ArenaInkPrimary)
                        Text(user.email, style = MaterialTheme.typography.bodySmall, color = ArenaInkSecondary)
                    }
                    StatusPill(
                        text = if (user.blocked) "BLOCKED" else "ACTIVE",
                        tone = if (user.blocked) StatusTone.RED else StatusTone.GREEN
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Free Fire UID: ${user.freeFireUid.ifBlank { "Not linked" }}  •  Phone: ${user.phone.ifBlank { "—" }}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = ArenaInkSecondary
                )
                Text(
                    text = "Matches: ${user.matchesPlayed}  •  Wins: ${user.totalWins}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ArenaInkMuted
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Wallet Balance: ${formatInr(wallet?.balance ?: 0.0)}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = ArenaBluePrimary
                )
                Text(
                    text = "Deposited: ${formatInr(wallet?.totalDeposited ?: 0.0)} • Withdrawn: ${formatInr(wallet?.totalWithdrawn ?: 0.0)} • Won: ${formatInr(wallet?.totalWinnings ?: 0.0)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ArenaInkMuted
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showAdjustDialog = true },
                        enabled = !actionBusy,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Adjust Wallet")
                    }
                    OutlinedButton(
                        onClick = onToggleBlocked,
                        enabled = !actionBusy,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(
                            if (user.blocked) Icons.Filled.CheckCircle else Icons.Filled.Block,
                            contentDescription = null,
                            tint = if (user.blocked) ArenaEmerald else ArenaCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (user.blocked) "Unblock" else "Block User",
                            color = if (user.blocked) ArenaEmerald else ArenaCrimson
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Immutable Transaction Ledger",
                subtitle = "All deposits, withdrawals, fees, and prize payouts"
            )
        }

        when (transactionsState) {
            UiState.Loading -> item { LoadingStateView("Loading transactions...") }
            is UiState.Error -> item { ErrorBanner(transactionsState.message) }
            is UiState.Success -> {
                if (transactionsState.data.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No Transactions Yet",
                            message = "Verified financial entries for this player will appear here."
                        )
                    }
                } else {
                    items(transactionsState.data, key = { it.id }) { tx ->
                        ArenaCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusPill(text = tx.type, tone = StatusTone.BLUE)
                                Text(
                                    text = formatInr(tx.amount),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = ArenaInkPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(tx.description, style = MaterialTheme.typography.bodyMedium, color = ArenaInkPrimary)
                            Text(
                                "Ref: ${tx.referenceId} • ${formatTimestamp(tx.createdAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArenaInkMuted
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdjustDialog) {
        AdjustWalletDialog(
            playerName = user.displayName,
            onDismiss = { showAdjustDialog = false },
            onConfirm = { amount, isCredit, reason ->
                onAdjustWallet(amount, isCredit, reason)
                showAdjustDialog = false
            }
        )
    }
}

@Composable
private fun AdjustWalletDialog(
    playerName: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, Boolean, String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var isCredit by remember { mutableStateOf(true) }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArenaWhite,
        title = { Text("Audited Wallet Adjustment • $playerName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isCredit,
                        onClick = { isCredit = true },
                        label = { Text("Credit (+)") }
                    )
                    FilterChip(
                        selected = !isCredit,
                        onClick = { isCredit = false },
                        label = { Text("Debit (-)") }
                    )
                }
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Mandatory Audit Reason") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    onConfirm(amt, isCredit, reason)
                },
                enabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0.0 && reason.trim().length >= 3,
                colors = ButtonDefaults.buttonColors(containerColor = ArenaBluePrimary)
            ) {
                Text("Execute Adjustment")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
