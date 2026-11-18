package com.example.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AdminUser
import com.example.data.model.AuditLog
import com.example.data.model.DashboardStats
import com.example.data.model.UiState
import com.example.ui.components.ArenaCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBanner
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone
import com.example.ui.components.formatInr
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.ArenaAmber
import com.example.ui.theme.ArenaAmberBg
import com.example.ui.theme.ArenaBlueDark
import com.example.ui.theme.ArenaBluePrimary
import com.example.ui.theme.ArenaCanvas
import com.example.ui.theme.ArenaCyanAccent
import com.example.ui.theme.ArenaEmerald
import com.example.ui.theme.ArenaEmeraldBg
import com.example.ui.theme.ArenaInkMuted
import com.example.ui.theme.ArenaInkPrimary
import com.example.ui.theme.ArenaInkSecondary
import com.example.ui.theme.ArenaSurfaceBlueTint
import com.example.ui.theme.ArenaWhite

@Composable
fun DashboardScreen(
    admin: AdminUser,
    stats: DashboardStats,
    auditLogsState: UiState<List<AuditLog>>,
    onOpenTournaments: () -> Unit,
    onOpenCreateTournament: () -> Unit,
    onOpenDeposits: () -> Unit,
    onOpenWithdrawals: () -> Unit,
    onOpenGatewaySettings: () -> Unit,
    onOpenAnnouncements: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaCanvas),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ArenaCard(
                borderColor = ArenaSurfaceBlueTint,
                containerColor = ArenaWhite
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_arena_banner),
                        contentDescription = "Battle Arena Dashboard Header",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        ArenaBlueDark.copy(alpha = 0.88f),
                                        ArenaBluePrimary.copy(alpha = 0.55f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            StatusPill(
                                text = admin.role.replace('_', ' '),
                                tone = StatusTone.BLUE
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Battle Arena Command",
                                style = MaterialTheme.typography.headlineSmall,
                                color = ArenaWhite
                            )
                            Text(
                                text = "${admin.displayName} • ${admin.email}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ArenaWhite.copy(alpha = 0.88f)
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Quick Admin Actions",
                subtitle = "Zero-trust server-backed operations"
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenCreateTournament,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("quick_create_tournament"),
                    colors = ButtonDefaults.buttonColors(containerColor = ArenaBluePrimary)
                ) {
                    Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Match")
                }
                QuickActionChip(
                    label = "Deposits (${stats.pendingDeposits})",
                    icon = Icons.Filled.AccountBalanceWallet,
                    tag = "quick_deposits",
                    onClick = onOpenDeposits
                )
                QuickActionChip(
                    label = "Payouts (${stats.pendingWithdrawals})",
                    icon = Icons.Filled.Payments,
                    tag = "quick_withdrawals",
                    onClick = onOpenWithdrawals
                )
                QuickActionChip(
                    label = "Payment Links",
                    icon = Icons.Filled.Settings,
                    tag = "quick_gateway_settings",
                    onClick = onOpenGatewaySettings
                )
                QuickActionChip(
                    label = "Broadcast",
                    icon = Icons.Filled.Campaign,
                    tag = "quick_broadcast",
                    onClick = onOpenAnnouncements
                )
            }
        }

        item {
            SectionHeader(
                title = "Platform Overview",
                subtitle = "Live statistics from Firestore"
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = "Registered Users",
                        value = stats.totalUsers.toString(),
                        subtitle = "Players",
                        icon = Icons.Filled.Groups,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "Total Tournaments",
                        value = stats.totalTournaments.toString(),
                        subtitle = "All Matches",
                        icon = Icons.Filled.SportsEsports,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenTournaments
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = "Active Matches",
                        value = stats.activeTournaments.toString(),
                        subtitle = "Upcoming/Live",
                        icon = Icons.Filled.PlayCircle,
                        accentColor = ArenaCyanAccent,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenTournaments
                    )
                    KpiMetricCard(
                        title = "Completed",
                        value = stats.completedTournaments.toString(),
                        subtitle = "Finished",
                        icon = Icons.Filled.EmojiEvents,
                        accentColor = ArenaEmerald,
                        accentBg = ArenaEmeraldBg,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenTournaments
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = "Pending Deposits",
                        value = stats.pendingDeposits.toString(),
                        subtitle = "Awaiting Review",
                        icon = Icons.Filled.HourglassTop,
                        accentColor = ArenaAmber,
                        accentBg = ArenaAmberBg,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenDeposits
                    )
                    KpiMetricCard(
                        title = "Pending Payouts",
                        value = stats.pendingWithdrawals.toString(),
                        subtitle = "Bank / UPI",
                        icon = Icons.Filled.Payments,
                        accentColor = ArenaAmber,
                        accentBg = ArenaAmberBg,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenWithdrawals
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = "Verified Deposits",
                        value = formatInr(stats.totalVerifiedDeposits),
                        subtitle = "Approved Inflow",
                        icon = Icons.Filled.CheckCircle,
                        accentColor = ArenaEmerald,
                        accentBg = ArenaEmeraldBg,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenDeposits
                    )
                    KpiMetricCard(
                        title = "Approved Payouts",
                        value = formatInr(stats.totalApprovedPayouts),
                        subtitle = "Settled Outflow",
                        icon = Icons.Filled.AccountBalanceWallet,
                        accentColor = ArenaBluePrimary,
                        accentBg = ArenaSurfaceBlueTint,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenWithdrawals
                    )
                }
            }
        }

        item {
            SectionHeader(
                title = "Recent Admin Audit Trail",
                subtitle = "Immutable security & wallet activity"
            )
        }

        when (auditLogsState) {
            UiState.Loading -> item {
                ArenaCard {
                    Text(
                        "Loading audit trail...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ArenaInkMuted
                    )
                }
            }

            is UiState.Error -> item {
                ErrorBanner(auditLogsState.message)
            }

            is UiState.Success -> {
                val recent = auditLogsState.data.take(6)
                if (recent.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No Admin Activity Yet",
                            message = "Actions like creating tournaments, verifying deposits, and approving bank payouts are recorded here permanently."
                        )
                    }
                } else {
                    items(recent, key = { it.id }) { log ->
                        ArenaCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusPill(text = log.action.replace('_', ' '), tone = StatusTone.BLUE)
                                Text(
                                    text = formatTimestamp(log.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ArenaInkMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = log.details,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ArenaInkPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "By ${log.adminEmail}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArenaInkSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionChip(
    label: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .height(48.dp)
            .testTag(tag),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = ArenaWhite,
            contentColor = ArenaBluePrimary
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label)
    }
}
