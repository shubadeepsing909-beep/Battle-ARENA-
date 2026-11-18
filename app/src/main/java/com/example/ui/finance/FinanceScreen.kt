package com.example.ui.finance

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.DepositRequest
import com.example.data.model.PaymentSettings
import com.example.data.model.PayoutMethod
import com.example.data.model.RequestStatus
import com.example.data.model.UiState
import com.example.data.model.WithdrawalRequest
import com.example.ui.components.ArenaCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LoadingStateView
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone
import com.example.ui.components.formatInr
import com.example.ui.components.formatTimestamp
import com.example.ui.components.statusToneFor
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
fun FinanceScreen(
    initialTab: Int = 0,
    depositsState: UiState<List<DepositRequest>>,
    withdrawalsState: UiState<List<WithdrawalRequest>>,
    paymentSettingsState: UiState<PaymentSettings>,
    actionBusy: Boolean,
    onApproveDeposit: (DepositRequest, String) -> Unit,
    onRejectDeposit: (DepositRequest, String) -> Unit,
    onApproveWithdrawal: (WithdrawalRequest, String, String) -> Unit,
    onRejectWithdrawal: (WithdrawalRequest, String) -> Unit,
    onSavePaymentSettings: (PaymentSettings) -> Unit
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    val tabs = listOf("Deposits", "Withdrawals", "Gateway & Links")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaCanvas)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = ArenaWhite,
            contentColor = ArenaBluePrimary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, maxLines = 1) },
                    modifier = Modifier.testTag("finance_tab_$index")
                )
            }
        }

        when (selectedTab) {
            0 -> DepositsTab(
                depositsState = depositsState,
                actionBusy = actionBusy,
                onApprove = onApproveDeposit,
                onReject = onRejectDeposit
            )
            1 -> WithdrawalsTab(
                withdrawalsState = withdrawalsState,
                actionBusy = actionBusy,
                onApprove = onApproveWithdrawal,
                onReject = onRejectWithdrawal
            )
            2 -> GatewaySettingsTab(
                settingsState = paymentSettingsState,
                actionBusy = actionBusy,
                onSave = onSavePaymentSettings
            )
        }
    }
}

@Composable
private fun DepositsTab(
    depositsState: UiState<List<DepositRequest>>,
    actionBusy: Boolean,
    onApprove: (DepositRequest, String) -> Unit,
    onReject: (DepositRequest, String) -> Unit
) {
    var filter by remember { mutableStateOf("PENDING") }
    var query by remember { mutableStateOf("") }
    var approveTarget by remember { mutableStateOf<DepositRequest?>(null) }
    var rejectTarget by remember { mutableStateOf<DepositRequest?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search by UTR / Ref ID or player...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("PENDING", "APPROVED", "REJECTED", "ALL").forEach { item ->
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

        when (depositsState) {
            UiState.Loading -> LoadingStateView("Loading deposit requests...")
            is UiState.Error -> ErrorBanner(depositsState.message)
            is UiState.Success -> {
                val list = depositsState.data.filter { d ->
                    val statusOk = filter == "ALL" || d.status == filter
                    val queryOk = query.isBlank() ||
                        d.transactionRef.contains(query, ignoreCase = true) ||
                        d.playerName.contains(query, ignoreCase = true) ||
                        d.userId.contains(query, ignoreCase = true)
                    statusOk && queryOk
                }
                if (list.isEmpty()) {
                    EmptyStateCard(
                        title = "No Deposit Requests",
                        message = "Player UPI / gateway deposit requests awaiting verification will appear here.",
                        icon = Icons.Filled.AccountBalanceWallet
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(list, key = { it.id }) { deposit ->
                            ArenaCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatInr(deposit.amount),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = ArenaBluePrimary
                                    )
                                    StatusPill(
                                        text = deposit.status,
                                        tone = statusToneFor(deposit.status)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${deposit.playerName} • ${deposit.paymentMethod}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ArenaInkPrimary
                                )
                                Text(
                                    text = "TXN Ref: ${deposit.transactionRef}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = ArenaInkSecondary
                                )
                                if (deposit.paymentLinkUsed.isNotBlank()) {
                                    Text(
                                        text = "Gateway: ${deposit.paymentLinkUsed}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ArenaInkMuted,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = "Submitted: ${formatTimestamp(deposit.createdAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ArenaInkMuted
                                )
                                if (deposit.adminNote.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Admin Note: ${deposit.adminNote}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ArenaInkSecondary
                                    )
                                }
                                if (deposit.status == RequestStatus.PENDING.name) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { approveTarget = deposit },
                                            enabled = !actionBusy,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = ArenaEmerald)
                                        ) {
                                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Verify & Credit")
                                        }
                                        OutlinedButton(
                                            onClick = { rejectTarget = deposit },
                                            enabled = !actionBusy,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = null, tint = ArenaCrimson, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Reject", color = ArenaCrimson)
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

    approveTarget?.let { dep ->
        NoteActionDialog(
            title = "Verify & Approve ${formatInr(dep.amount)}",
            subtitle = "Player: ${dep.playerName}\nTXN Ref: ${dep.transactionRef}\nDuplicate approval is blocked server-side.",
            fieldLabel = "Verification Note",
            defaultValue = "Verified gateway UTR",
            confirmText = "Approve & Credit Wallet",
            isDestructive = false,
            onDismiss = { approveTarget = null },
            onConfirm = { note ->
                onApprove(dep, note)
                approveTarget = null
            }
        )
    }

    rejectTarget?.let { dep ->
        NoteActionDialog(
            title = "Reject Deposit ${formatInr(dep.amount)}",
            subtitle = "Ref: ${dep.transactionRef}",
            fieldLabel = "Rejection Reason",
            defaultValue = "Transaction reference not found on gateway",
            confirmText = "Reject Deposit",
            isDestructive = true,
            onDismiss = { rejectTarget = null },
            onConfirm = { reason ->
                onReject(dep, reason)
                rejectTarget = null
            }
        )
    }
}

@Composable
private fun WithdrawalsTab(
    withdrawalsState: UiState<List<WithdrawalRequest>>,
    actionBusy: Boolean,
    onApprove: (WithdrawalRequest, String, String) -> Unit,
    onReject: (WithdrawalRequest, String) -> Unit
) {
    var filter by remember { mutableStateOf("PENDING") }
    var approveTarget by remember { mutableStateOf<WithdrawalRequest?>(null) }
    var rejectTarget by remember { mutableStateOf<WithdrawalRequest?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("PENDING", "APPROVED", "REJECTED", "ALL").forEach { item ->
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

        when (withdrawalsState) {
            UiState.Loading -> LoadingStateView("Loading withdrawal requests...")
            is UiState.Error -> ErrorBanner(withdrawalsState.message)
            is UiState.Success -> {
                val list = withdrawalsState.data.filter { filter == "ALL" || it.status == filter }
                if (list.isEmpty()) {
                    EmptyStateCard(
                        title = "No Withdrawal Requests",
                        message = "Player Bank Account & UPI payout requests will appear here for review.",
                        icon = Icons.Filled.Payments
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(list, key = { it.id }) { wd ->
                            ArenaCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatInr(wd.amount),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = ArenaBluePrimary
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        StatusPill(
                                            text = if (wd.payoutMethod == PayoutMethod.BANK_ACCOUNT.name) "BANK ACCOUNT" else "UPI",
                                            tone = StatusTone.BLUE
                                        )
                                        StatusPill(text = wd.status, tone = statusToneFor(wd.status))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = wd.playerName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ArenaInkPrimary
                                )
                                if (wd.payoutMethod == PayoutMethod.BANK_ACCOUNT.name) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.AccountBalance,
                                            contentDescription = null,
                                            tint = ArenaBluePrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${wd.bankName} • ${wd.accountHolderName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ArenaInkSecondary
                                        )
                                    }
                                    Text(
                                        text = "A/C: ${wd.accountNumber}  •  IFSC: ${wd.ifscCode}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                        color = ArenaInkPrimary
                                    )
                                } else {
                                    Text(
                                        text = "UPI ID: ${wd.upiId}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                        color = ArenaInkPrimary
                                    )
                                }
                                if (wd.payoutReference.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Payout Ref: ${wd.payoutReference}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                        color = ArenaEmerald
                                    )
                                }
                                Text(
                                    text = "Requested: ${formatTimestamp(wd.createdAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ArenaInkMuted
                                )
                                if (wd.status == RequestStatus.PENDING.name) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { approveTarget = wd },
                                            enabled = !actionBusy,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = ArenaBluePrimary)
                                        ) {
                                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Approve Payout")
                                        }
                                        OutlinedButton(
                                            onClick = { rejectTarget = wd },
                                            enabled = !actionBusy,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = null, tint = ArenaCrimson, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Reject", color = ArenaCrimson)
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

    approveTarget?.let { wd ->
        ApproveWithdrawalDialog(
            withdrawal = wd,
            onDismiss = { approveTarget = null },
            onConfirm = { payoutRef, note ->
                onApprove(wd, payoutRef, note)
                approveTarget = null
            }
        )
    }

    rejectTarget?.let { wd ->
        NoteActionDialog(
            title = "Reject Withdrawal ${formatInr(wd.amount)}",
            subtitle = "Player: ${wd.playerName}",
            fieldLabel = "Rejection Reason",
            defaultValue = "Invalid bank / UPI details",
            confirmText = "Reject Request",
            isDestructive = true,
            onDismiss = { rejectTarget = null },
            onConfirm = { reason ->
                onReject(wd, reason)
                rejectTarget = null
            }
        )
    }
}

@Composable
private fun GatewaySettingsTab(
    settingsState: UiState<PaymentSettings>,
    actionBusy: Boolean,
    onSave: (PaymentSettings) -> Unit
) {
    val context = LocalContext.current
    val loaded = (settingsState as? UiState.Success)?.data ?: PaymentSettings()

    var tier50 by remember(loaded) { mutableStateOf(loaded.tier50Link) }
    var tier100 by remember(loaded) { mutableStateOf(loaded.tier100Link) }
    var tier200 by remember(loaded) { mutableStateOf(loaded.tier200Link) }
    var tier500 by remember(loaded) { mutableStateOf(loaded.tier500Link) }
    var bankEnabled by remember(loaded) { mutableStateOf(loaded.bankWithdrawalEnabled) }
    var upiEnabled by remember(loaded) { mutableStateOf(loaded.upiWithdrawalEnabled) }
    var minDeposit by remember(loaded) { mutableStateOf(loaded.minDeposit.toInt().toString()) }
    var minWithdraw by remember(loaded) { mutableStateOf(loaded.minWithdrawal.toInt().toString()) }
    var supportEmail by remember(loaded) { mutableStateOf(loaded.supportEmail) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Deposit Tier Payment Links",
                subtitle = "Configured links launched automatically for player deposit amounts"
            )
        }

        item {
            ArenaCard {
                PaymentLinkField(
                    amountLabel = "₹50.00 Deposit Link",
                    value = tier50,
                    onValueChange = { tier50 = it },
                    onOpenLink = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(tier50.trim())))
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                PaymentLinkField(
                    amountLabel = "₹100.00 Deposit Link",
                    value = tier100,
                    onValueChange = { tier100 = it },
                    onOpenLink = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(tier100.trim())))
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                PaymentLinkField(
                    amountLabel = "₹200.00 Deposit Link",
                    value = tier200,
                    onValueChange = { tier200 = it },
                    onOpenLink = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(tier200.trim())))
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                PaymentLinkField(
                    amountLabel = "₹500.00 Deposit Link",
                    value = tier500,
                    onValueChange = { tier500 = it },
                    onOpenLink = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(tier500.trim())))
                    }
                )
            }
        }

        item {
            ArenaCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Settings, contentDescription = null, tint = ArenaBluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Withdrawal & Limit Controls", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bank Account Withdrawal Option", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Allow players to submit Account No + IFSC payouts", style = MaterialTheme.typography.bodySmall, color = ArenaInkMuted)
                    }
                    Switch(checked = bankEnabled, onCheckedChange = { bankEnabled = it })
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("UPI Withdrawal Option", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Allow players to submit UPI ID payouts", style = MaterialTheme.typography.bodySmall, color = ArenaInkMuted)
                    }
                    Switch(checked = upiEnabled, onCheckedChange = { upiEnabled = it })
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = minDeposit,
                        onValueChange = { minDeposit = it },
                        label = { Text("Min Deposit ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minWithdraw,
                        onValueChange = { minWithdraw = it },
                        label = { Text("Min Withdraw ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = supportEmail,
                    onValueChange = { supportEmail = it },
                    label = { Text("Support Contact Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        onSave(
                            loaded.copy(
                                tier50Link = tier50,
                                tier100Link = tier100,
                                tier200Link = tier200,
                                tier500Link = tier500,
                                bankWithdrawalEnabled = bankEnabled,
                                upiWithdrawalEnabled = upiEnabled,
                                minDeposit = minDeposit.toDoubleOrNull() ?: 50.0,
                                minWithdrawal = minWithdraw.toDoubleOrNull() ?: 100.0,
                                supportEmail = supportEmail
                            )
                        )
                    },
                    enabled = !actionBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_payment_settings_button")
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Gateway & Withdrawal Settings")
                }
            }
        }
    }
}

@Composable
private fun PaymentLinkField(
    amountLabel: String,
    value: String,
    onValueChange: (String) -> Unit,
    onOpenLink: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(amountLabel) },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        Spacer(modifier = Modifier.width(6.dp))
        IconButton(
            onClick = onOpenLink,
            enabled = value.startsWith("http")
        ) {
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Test $amountLabel",
                tint = ArenaBluePrimary
            )
        }
    }
}

@Composable
private fun ApproveWithdrawalDialog(
    withdrawal: WithdrawalRequest,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var payoutRef by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("Bank / UPI payout settled") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArenaWhite,
        title = { Text("Approve Payout • ${formatInr(withdrawal.amount)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (withdrawal.payoutMethod == PayoutMethod.BANK_ACCOUNT.name) {
                        "Bank: ${withdrawal.bankName}\nA/C: ${withdrawal.accountNumber}\nIFSC: ${withdrawal.ifscCode}\nHolder: ${withdrawal.accountHolderName}"
                    } else {
                        "UPI ID: ${withdrawal.upiId}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ArenaInkSecondary
                )
                OutlinedTextField(
                    value = payoutRef,
                    onValueChange = { payoutRef = it },
                    label = { Text("Bank UTR / IMPS / UPI Reference") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Admin Note") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(payoutRef, note) },
                enabled = payoutRef.trim().length >= 3
            ) {
                Text("Confirm & Deduct Wallet")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun NoteActionDialog(
    title: String,
    subtitle: String,
    fieldLabel: String,
    defaultValue: String,
    confirmText: String,
    isDestructive: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var note by remember { mutableStateOf(defaultValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArenaWhite,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ArenaInkSecondary)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(fieldLabel) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(note) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) ArenaCrimson else ArenaEmerald
                )
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
