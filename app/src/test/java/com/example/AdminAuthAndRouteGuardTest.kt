package com.example

import com.example.data.model.AdminRole
import com.example.data.model.AdminUser
import com.example.data.repository.AdminAuthRepository
import com.example.ui.AdminDestination
import com.example.ui.auth.AdminRouteGuard
import com.example.ui.hub.HubSection
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminAuthAndRouteGuardTest {

    @Test
    fun predefinedAdmin_requiresVerifiedEmailOrPredefinedUid() {
        assertTrue(
            AdminAuthRepository.isPredefinedAdmin(
                email = "shubadeepsing909@gmail.com",
                isEmailVerified = true,
                uid = "other_uid"
            )
        )
        assertTrue(
            AdminAuthRepository.isPredefinedAdmin(
                email = "other@gmail.com",
                isEmailVerified = true,
                uid = "kOXOAgPVKudCdefAI8x22hD32TQ2"
            )
        )
        assertFalse(
            AdminAuthRepository.isPredefinedAdmin(
                email = "shubadeepsing909@gmail.com",
                isEmailVerified = false,
                uid = "spoof_uid"
            )
        )
        assertFalse(
            AdminAuthRepository.isPredefinedAdmin(
                email = "player@gmail.com",
                isEmailVerified = true,
                uid = "random_player_uid"
            )
        )
    }

    @Test
    fun inactiveAdmin_isDeniedAllAdminRoutes() {
        val suspendedAdmin = AdminUser(
            uid = "u1",
            email = "mod@gmail.com",
            role = AdminRole.SUPER_ADMIN.name,
            active = false
        )
        assertFalse(AdminRouteGuard.canAccessRoute(suspendedAdmin, AdminDestination.Dashboard.route))
        assertFalse(AdminRouteGuard.canAccessRoute(suspendedAdmin, AdminDestination.Finance.route))
        assertTrue(AdminRouteGuard.allowedDestinations(suspendedAdmin).isEmpty())
    }

    @Test
    fun roleBasedRouteProtection_enforcesFinanceAndTournamentBoundaries() {
        val financeAdmin = AdminUser(
            uid = "f1",
            email = "finance@gmail.com",
            role = AdminRole.FINANCE_ADMIN.name,
            active = true
        )
        assertTrue(AdminRouteGuard.canAccessRoute(financeAdmin, AdminDestination.Finance.route))
        assertFalse(AdminRouteGuard.canAccessRoute(financeAdmin, AdminDestination.Tournaments.route))
        assertFalse(AdminRouteGuard.canAccessHubSection(financeAdmin, HubSection.ADMINS))
        assertTrue(AdminRouteGuard.canAccessHubSection(financeAdmin, HubSection.AUDIT_LOGS))

        val tournamentAdmin = AdminUser(
            uid = "t1",
            email = "tourney@gmail.com",
            role = AdminRole.TOURNAMENT_ADMIN.name,
            active = true
        )
        assertTrue(AdminRouteGuard.canAccessRoute(tournamentAdmin, AdminDestination.Tournaments.route))
        assertFalse(AdminRouteGuard.canAccessRoute(tournamentAdmin, AdminDestination.Finance.route))
        assertFalse(AdminRouteGuard.canAccessHubSection(tournamentAdmin, HubSection.ADMINS))
        assertTrue(AdminRouteGuard.canAccessHubSection(tournamentAdmin, HubSection.WINNERS))
    }
}
