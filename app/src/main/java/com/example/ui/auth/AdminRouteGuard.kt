package com.example.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminRole
import com.example.data.model.AdminUser
import com.example.ui.AdminDestination
import com.example.ui.hub.HubSection
import com.example.ui.theme.ArenaBluePrimary
import com.example.ui.theme.ArenaCrimson
import com.example.ui.theme.ArenaInkPrimary
import com.example.ui.theme.ArenaInkSecondary
import com.example.ui.components.ArenaCard

object AdminRouteGuard {

    fun canAccessRoute(admin: AdminUser?, route: String): Boolean {
        if (admin == null || !admin.active) return false
        return when (AdminRole.fromRaw(admin.role)) {
            AdminRole.SUPER_ADMIN -> true
            AdminRole.FINANCE_ADMIN -> route in setOf(
                AdminDestination.Dashboard.route,
                AdminDestination.Finance.route,
                AdminDestination.Users.route,
                AdminDestination.Hub.route
            )
            AdminRole.TOURNAMENT_ADMIN -> route in setOf(
                AdminDestination.Dashboard.route,
                AdminDestination.Tournaments.route,
                AdminDestination.Users.route,
                AdminDestination.Hub.route
            )
        }
    }

    fun allowedDestinations(admin: AdminUser?): List<AdminDestination> {
        if (admin == null || !admin.active) return emptyList()
        return listOf(
            AdminDestination.Dashboard,
            AdminDestination.Tournaments,
            AdminDestination.Finance,
            AdminDestination.Users,
            AdminDestination.Hub
        ).filter { canAccessRoute(admin, it.route) }
    }

    fun canAccessHubSection(admin: AdminUser?, section: HubSection): Boolean {
        if (admin == null || !admin.active) return false
        val role = AdminRole.fromRaw(admin.role)
        return when (section) {
            HubSection.MENU -> true
            HubSection.ADMINS -> role == AdminRole.SUPER_ADMIN
            HubSection.AUDIT_LOGS -> role == AdminRole.SUPER_ADMIN || role == AdminRole.FINANCE_ADMIN
            HubSection.WINNERS,
            HubSection.ANNOUNCEMENTS -> role == AdminRole.SUPER_ADMIN || role == AdminRole.TOURNAMENT_ADMIN
            HubSection.SUPPORT -> true
        }
    }
}

@Composable
fun RouteAccessDeniedCard(
    requiredRoles: String,
    currentRole: String,
    onReturnToOverview: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        ArenaCard(modifier = Modifier.widthIn(max = 460.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.GppBad,
                    contentDescription = "Protected Admin Route",
                    tint = ArenaCrimson,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Protected Admin Route",
                    style = MaterialTheme.typography.titleLarge,
                    color = ArenaInkPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Your active role ($currentRole) does not have permission for this module. Required: $requiredRoles.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ArenaInkSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onReturnToOverview,
                    colors = ButtonDefaults.buttonColors(containerColor = ArenaBluePrimary),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Return to Overview")
                }
            }
        }
    }
}
