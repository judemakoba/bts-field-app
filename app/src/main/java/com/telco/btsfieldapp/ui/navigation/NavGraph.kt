package com.telco.btsfieldapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.telco.btsfieldapp.ui.audit.AuditScreen
import com.telco.btsfieldapp.ui.auth.LoginScreen
import com.telco.btsfieldapp.ui.camera.CameraScreenPlaceholder
import com.telco.btsfieldapp.ui.detail.SiteDetailScreen
import com.telco.btsfieldapp.ui.sites.SitesScreen

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Sites : Screen("sites")
    data object SiteDetail : Screen("site/{siteId}") {
        fun createRoute(siteId: String) = "site/$siteId"
    }
    data object Audit : Screen("audit/{siteId}/{auditType}") {
        fun createRoute(siteId: String, auditType: String) = "audit/$siteId/$auditType"
    }
    data object Camera : Screen("camera/{siteId}/{auditType}") {
        fun createRoute(siteId: String, auditType: String) = "camera/$siteId/$auditType"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Sites.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Sites.route) {
            SitesScreen(
                onSiteClick = { siteId ->
                    navController.navigate(Screen.SiteDetail.createRoute(siteId))
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Sites.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.SiteDetail.route,
            arguments = listOf(navArgument("siteId") { type = NavType.StringType })
        ) {
            SiteDetailScreen(
                onBack = { navController.popBackStack() },
                onStartAudit = { siteId, auditType ->
                    navController.navigate(Screen.Audit.createRoute(siteId, auditType))
                }
            )
        }

        composable(
            route = Screen.Audit.route,
            arguments = listOf(
                navArgument("siteId") { type = NavType.StringType },
                navArgument("auditType") { type = NavType.StringType }
            )
        ) {
            AuditScreen(
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() },
                onCapturePhoto = { siteId, auditType ->
                    navController.navigate(Screen.Camera.createRoute(siteId, auditType))
                }
            )
        }

        composable(
            route = Screen.Camera.route,
            arguments = listOf(
                navArgument("siteId") { type = NavType.StringType },
                navArgument("auditType") { type = NavType.StringType }
            )
        ) {
            CameraScreenPlaceholder(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
