package com.telco.btsfieldapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

import com.telco.btsfieldapp.ui.audit.DcdbInfoScreen
import com.telco.btsfieldapp.ui.audit.GroundEquipmentScreen
import com.telco.btsfieldapp.ui.audit.TowerInfoScreen
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
    data object Camera : Screen("camera/{siteId}/{auditType}") {
        fun createRoute(siteId: String, auditType: String) = "camera/$siteId/$auditType"
    }
    data object GroundEquipment : Screen("ground_equipment/{siteId}") {
        fun createRoute(siteId: String) = "ground_equipment/$siteId"
    }
    data object DcdbInfo : Screen("dcdb_info/{siteId}") {
        fun createRoute(siteId: String) = "dcdb_info/$siteId"
    }
    data object TowerInfo : Screen("tower_info/{siteId}") {
        fun createRoute(siteId: String) = "tower_info/$siteId"
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
                onOpenGroundEquipment = { siteId ->
                    navController.navigate(Screen.GroundEquipment.createRoute(siteId))
                },
                onOpenDcdbInfo = { siteId ->
                    navController.navigate(Screen.DcdbInfo.createRoute(siteId))
                },
                onOpenTowerInfo = { siteId ->
                    navController.navigate(Screen.TowerInfo.createRoute(siteId))
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

        composable(
            route = Screen.GroundEquipment.route,
            arguments = listOf(navArgument("siteId") { type = NavType.StringType })
        ) {
            GroundEquipmentScreen(
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() },
                onCapturePhoto = { siteId, sectionKey ->
                    navController.navigate(Screen.Camera.createRoute(siteId, sectionKey))
                }
            )
        }

        composable(
            route = Screen.DcdbInfo.route,
            arguments = listOf(navArgument("siteId") { type = NavType.StringType })
        ) {
            DcdbInfoScreen(
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() },
                onCapturePhoto = { siteId, sectionKey ->
                    navController.navigate(Screen.Camera.createRoute(siteId, sectionKey))
                }
            )
        }

        composable(
            route = Screen.TowerInfo.route,
            arguments = listOf(navArgument("siteId") { type = NavType.StringType })
        ) {
            TowerInfoScreen(
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() },
                onCapturePhoto = { siteId, sectionKey ->
                    navController.navigate(Screen.Camera.createRoute(siteId, sectionKey))
                }
            )
        }
    }
}
