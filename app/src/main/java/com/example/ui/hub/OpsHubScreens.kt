package com.example.ui.hub

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminRole
import com.example.data.model.AdminUser
import com.example.data.model.AuditLog
import com.example.data.model.NotificationItem
import com.example.data.model.SupportTicket
import com.example.data.model.TicketStatus
import com.example.data.model.TournamentWinner
import com.example.data.model.UiState
import com.example.ui.auth.AdminRouteGuard
import com.example.ui.auth.RouteAccessDeniedCard
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
import com.example.ui.theme.ArenaInkMuted
import com.example.ui.theme.ArenaInkPrimary
import com.example.ui.theme.ArenaInkSecondary
import com.example.ui.theme.ArenaWhite
import com.example.ui.theme.JetBrainsMonoFamily

enum class HubSection {
    MENU,
    WINNERS,
    ANNOUNCEMENTS,
    SUPPORT,
    ADMINS,
    AUDIT_LOGS
}

@Composable
fun OpsHubScreen(
    initialSection: HubSection = HubSection.MENU,
    currentAdmin: AdminUser,
    winnersState: UiState<List<TournamentWinner>>,
    notificationsState: UiState<List<NotificationItem>>,
    supportTicketsState: UiState<List<SupportTicket>>,
    adminsState: UiState<List<AdminUser>>,
    auditLogsState: UiState<List<AuditLog>>,
    actionBusy: Boolean,
    onSendNotification: (String, String, String, String, String) -> Unit,
    onUpdateTicket: (SupportTicket, TicketStatus, String) -> Unit,
    onUpsertAdmin: (String, String, String, AdminRole, Boolean) -> Unit,
    onRemoveAdmin: (AdminUser) -> Unit,
    onSignOut: () -> Unit
) {
    var activeSection by remember(initialSection) {
        mutableStateOf(
            if (AdminRouteGuard.canAccessHubSection(currentAdmin, initialSection)) initialSection
            else HubSection.MENU
        )
    }

    if (activeSection != HubSection.MENU) {
        BackHandler { activeSection = HubSection.MENU }
    }

    if (activeSection != HubSection.MENU && !AdminRouteGuard.canAccessHubSection(currentAdmin, activeSection)) {
        com.example.ui.auth.RouteAccessDeniedCard(
            requiredRoles = "SUPER_ADMIN or authorized module role",
            currentRole = currentAdmin.role,
            onReturnToOverview = { activeSection = HubSection.MENU }
        )
        return
    }

    when (activeSection) {
        HubSection.MENU -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ArenaCanvas),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ArenaCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    currentAdmin.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ArenaInkPrimary
                                )
                                Text(
                                    currentAdmin.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ArenaInkSecondary
                                )
                            }
                            StatusPill(text = currentAdmin.role, tone = StatusTone.BLUE)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("admin_sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out")
                        }
                    }
                }

                item {
                    SectionHeader(
                        title = "Operations & Governance",
                        subtitle = "Winners, announcements, support, roles & audit logs"
                    )
                }

                if (AdminRouteGuard.canAccessHubSection(currentAdmin, HubSection.WINNERS)) {
                    item {
                        HubMenuCard(
                            title = "Winners & Prize History",
                            subtitle = "Verified placements, kills & allocated rewards",
                            icon = Icons.Filled.EmojiEvents,
                            tag = "hub_winners",
                            onClick = { activeSection = HubSection.WINNERS }
                        )
                    }
                }
                if (AdminRouteGuard.canAccessHubSection(currentAdmin, HubSection.ANNOUNCEMENTS)) {
                    item {
                        HubMenuCard(
                            title = "Announcements & Notifications",
                            subtitle = "Broadcast match alerts & updates to all players",
                            icon = Icons.Filled.Campaign,
                            tag = "hub_announcements",
                            onClick = { activeSection = HubSection.ANNOUNCEMENTS }
                        )
                    }
                }
                if (AdminRouteGuard.canAccessHubSection(currentAdmin, HubSection.SUPPORT)) {
                    item {
                        HubMenuCard(
                            title = "Customer Support Tickets",
                            subtitle = "Review player inquiries, reply & resolve",
                            icon = Icons.Filled.SupportAgent,
                            tag = "hub_support",
                            onClick = { activeSection = HubSection.SUPPORT }
                        )
                    }
                }
                if (AdminRouteGuard.canAccessHubSection(currentAdmin, HubSection.ADMINS)) {
                    item {
                        HubMenuCard(
                            title = "Admin Roles & Permissions",
                            subtitle = "Manage Super Admins, Finance & Tournament Admins",
                            icon = Icons.Filled.AdminPanelSettings,
                            tag = "hub_admins",
                            onClick = { activeSection = HubSection.ADMINS }
                        )
                    }
                }
                if (AdminRouteGuard.canAccessHubSection(currentAdmin, HubSection.AUDIT_LOGS)) {
                    item {
                        HubMenuCard(
                            title = "Admin Activity Audit Logs",
                            subtitle = "Immutable security & financial action log",
                            icon = Icons.Filled.History,
                            tag = "hub_audit_logs",
                            onClick = { activeSection = HubSection.AUDIT_LOGS }
                        )
                    }
                }
            }
        }

        HubSection.WINNERS -> WinnersSection(
            winnersState = winnersState,
            onBack = { activeSection = HubSection.MENU }
        )

        HubSection.ANNOUNCEMENTS -> AnnouncementsSection(
            notificationsState = notificationsState,
            actionBusy = actionBusy,
            onBack = { activeSection = HubSection.MENU },
            onSend = onSendNotification
        )

        HubSection.SUPPORT -> SupportSection(
            supportState = supportTicketsState,
            actionBusy = actionBusy,
            onBack = { activeSection = HubSection.MENU },
            onUpdate = onUpdateTicket
        )

        HubSection.ADMINS -> AdminRolesSection(
            currentAdminUid = currentAdmin.uid,
            adminsState = adminsState,
            actionBusy = actionBusy,
            onBack = { activeSection = HubSection.MENU },
            onUpsert = onUpsertAdmin,
            onRemove = onRemoveAdmin
        )

        HubSection.AUDIT_LOGS -> AuditLogsSection(
            auditLogsState = auditLogsState,
            onBack = { activeSection = HubSection.MENU }
        )
    }
}

@Composable
private fun HubMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    ArenaCard(
        onClick = onClick,
        modifier = Modifier.testTag(tag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = ArenaBluePrimary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = ArenaInkPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ArenaInkSecondary)
            }
        }
    }
}

@Composable
private fun SubSectionTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("hub_sub_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = ArenaInkPrimary)
    }
}

@Composable
private fun WinnersSection(
    winnersState: UiState<List<TournamentWinner>>,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(ArenaCanvas)) {
        SubSectionTopBar("Winners & Prize Ledger", onBack)
        when (winnersState) {
            UiState.Loading -> LoadingStateView("Loading winners...")
            is UiState.Error -> ErrorBanner(winnersState.message)
            is UiState.Success -> {
                if (winnersState.data.isEmpty()) {
                    Column(Modifier.padding(16.dp)) {
                        EmptyStateCard(
                            title = "No Winners Allocated Yet",
                            message = "Open any tournament's participants list and tap Award Prize to verify winners."
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(winnersState.data, key = { it.id }) { w ->
                            ArenaCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatusPill(text = "RANK #${w.rank}", tone = StatusTone.GREEN)
                                    Text(
                                        text = formatInr(w.prizeAmount),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = ArenaBluePrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(w.playerName, style = MaterialTheme.typography.titleSmall, color = ArenaInkPrimary)
                                Text(
                                    "${w.tournamentTitle} • Kills: ${w.kills} • FF UID: ${w.freeFireUid}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ArenaInkSecondary
                                )
                                Text(
                                    formatTimestamp(w.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ArenaInkMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnouncementsSection(
    notificationsState: UiState<List<NotificationItem>>,
    actionBusy: Boolean,
    onBack: () -> Unit,
    onSend: (String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var targetUid by remember { mutableStateOf("ALL") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(ArenaCanvas),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SubSectionTopBar("Notifications & Announcements", onBack) }
        item {
            ArenaCard {
                Text("Broadcast New Alert", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Announcement Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = targetUid,
                    onValueChange = { targetUid = it },
                    label = { Text("Target UID (or ALL for broadcast)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        onSend(title, message, targetUid, "ANNOUNCEMENT", "")
                        title = ""
                        message = ""
                    },
                    enabled = !actionBusy && title.isNotBlank() && message.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Send Notification")
                }
            }
        }

        when (notificationsState) {
            UiState.Loading -> item { LoadingStateView("Loading announcements...") }
            is UiState.Error -> item { ErrorBanner(notificationsState.message) }
            is UiState.Success -> {
                if (notificationsState.data.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No Notifications Sent",
                            message = "Sent tournament broadcasts will appear here."
                        )
                    }
                } else {
                    items(notificationsState.data, key = { it.id }) { n ->
                        ArenaCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatusPill(text = n.type, tone = StatusTone.BLUE)
                                Text(formatTimestamp(n.createdAt), style = MaterialTheme.typography.labelSmall, color = ArenaInkMuted)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(n.title, style = MaterialTheme.typography.titleSmall)
                            Text(n.message, style = MaterialTheme.typography.bodySmall, color = ArenaInkSecondary)
                            Text("Target: ${n.targetUserId}", style = MaterialTheme.typography.labelSmall, color = ArenaInkMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportSection(
    supportState: UiState<List<SupportTicket>>,
    actionBusy: Boolean,
    onBack: () -> Unit,
    onUpdate: (SupportTicket, TicketStatus, String) -> Unit
) {
    var replyTarget by remember { mutableStateOf<SupportTicket?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(ArenaCanvas)) {
        SubSectionTopBar("Customer Support Tickets", onBack)
        when (supportState) {
            UiState.Loading -> LoadingStateView("Loading support tickets...")
            is UiState.Error -> ErrorBanner(supportState.message)
            is UiState.Success -> {
                if (supportState.data.isEmpty()) {
                    Column(Modifier.padding(16.dp)) {
                        EmptyStateCard(
                            title = "No Support Tickets",
                            message = "Player support requests will appear here for resolution."
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(supportState.data, key = { it.id }) { ticket ->
                            ArenaCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(ticket.subject, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    StatusPill(text = ticket.status, tone = statusToneFor(ticket.status))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${ticket.playerName} (${ticket.email})", style = MaterialTheme.typography.labelSmall, color = ArenaInkMuted)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(ticket.message, style = MaterialTheme.typography.bodySmall, color = ArenaInkSecondary)
                                if (ticket.adminReply.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Reply: ${ticket.adminReply}", style = MaterialTheme.typography.bodySmall, color = ArenaBluePrimary)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { replyTarget = ticket },
                                    enabled = !actionBusy,
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("Reply & Update Status")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    replyTarget?.let { ticket ->
        var reply by remember { mutableStateOf(ticket.adminReply) }
        var status by remember { mutableStateOf(TicketStatus.RESOLVED) }

        AlertDialog(
            onDismissRequest = { replyTarget = null },
            containerColor = ArenaWhite,
            title = { Text("Respond to Ticket") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TicketStatus.entries.forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st.label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = reply,
                        onValueChange = { reply = it },
                        label = { Text("Admin Response") },
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdate(ticket, status, reply)
                    replyTarget = null
                }) { Text("Save") }
            },
            dismissButton = {
                OutlinedButton(onClick = { replyTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AdminRolesSection(
    currentAdminUid: String,
    adminsState: UiState<List<AdminUser>>,
    actionBusy: Boolean,
    onBack: () -> Unit,
    onUpsert: (String, String, String, AdminRole, Boolean) -> Unit,
    onRemove: (AdminUser) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(ArenaCanvas)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SubSectionTopBar("Admin Roles", onBack)
            Button(onClick = { showAddDialog = true }, enabled = !actionBusy) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Admin")
            }
        }

        when (adminsState) {
            UiState.Loading -> LoadingStateView("Loading admins...")
            is UiState.Error -> ErrorBanner(adminsState.message)
            is UiState.Success -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(adminsState.data, key = { it.id }) { adm ->
                        ArenaCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(adm.displayName, style = MaterialTheme.typography.titleSmall)
                                    Text(adm.email, style = MaterialTheme.typography.bodySmall, color = ArenaInkSecondary)
                                    Text("UID: ${adm.uid}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily), color = ArenaInkMuted)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    StatusPill(text = adm.role, tone = StatusTone.BLUE)
                                    if (adm.id != currentAdminUid) {
                                        IconButton(onClick = { onRemove(adm) }, enabled = !actionBusy) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Revoke Admin", tint = ArenaCrimson)
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

    if (showAddDialog) {
        var uid by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var role by remember { mutableStateOf(AdminRole.TOURNAMENT_ADMIN) }
        var active by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = ArenaWhite,
            title = { Text("Authorize Administrator") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = uid, onValueChange = { uid = it }, label = { Text("Firebase Auth UID") }, singleLine = true)
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Google Email") }, singleLine = true)
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display Name") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AdminRole.entries.forEach { r ->
                            FilterChip(
                                selected = role == r,
                                onClick = { role = r },
                                label = { Text(r.name.substringBefore('_')) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active")
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpsert(uid, email, name, role, active)
                        showAddDialog = false
                    },
                    enabled = uid.isNotBlank() && email.contains("@")
                ) { Text("Save") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AuditLogsSection(
    auditLogsState: UiState<List<AuditLog>>,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(ArenaCanvas)) {
        SubSectionTopBar("Admin Activity Audit Logs", onBack)
        when (auditLogsState) {
            UiState.Loading -> LoadingStateView("Loading audit logs...")
            is UiState.Error -> ErrorBanner(auditLogsState.message)
            is UiState.Success -> {
                if (auditLogsState.data.isEmpty()) {
                    Column(Modifier.padding(16.dp)) {
                        EmptyStateCard(
                            title = "No Audit Entries",
                            message = "Every admin operation is recorded permanently here."
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(auditLogsState.data, key = { it.id }) { log ->
                            ArenaCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    StatusPill(text = log.action, tone = StatusTone.BLUE)
                                    Text(formatTimestamp(log.createdAt), style = MaterialTheme.typography.labelSmall, color = ArenaInkMuted)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(log.details, style = MaterialTheme.typography.bodyMedium, color = ArenaInkPrimary)
                                Text(
                                    "${log.adminEmail} • ${log.targetType}:${log.targetId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ArenaInkMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
