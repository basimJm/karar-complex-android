package com.alkadad.compound.ui.screens

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alkadad.compound.data.repository.AuthRepository
import com.alkadad.compound.ui.components.AppMotion

private const val NAV_DURATION = 380

@Composable
fun NavGraph() {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    var isAuthenticated by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isAuthenticated = authRepository.isLoggedIn()
    }
    
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = if (isAuthenticated) "dashboard" else "login",
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(NAV_DURATION, easing = AppMotion.Emphasized), initialOffset = { it / 4 }) +
                fadeIn(tween(NAV_DURATION))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(NAV_DURATION, easing = AppMotion.Emphasized), targetOffset = { it / 8 }) +
                fadeOut(tween(NAV_DURATION / 2))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(NAV_DURATION, easing = AppMotion.Emphasized), initialOffset = { it / 8 }) +
                fadeIn(tween(NAV_DURATION))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(NAV_DURATION, easing = AppMotion.Emphasized), targetOffset = { it / 4 }) +
                fadeOut(tween(NAV_DURATION / 2))
        }
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    isAuthenticated = true
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        
        composable("dashboard") {
            DashboardScreen(
                onTableClick = { tableId ->
                    navController.navigate("table/$tableId")
                },
                onLogout = {
                    isAuthenticated = false
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(
            route = "table/{tableId}",
            arguments = listOf(
                navArgument("tableId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val tableId = backStackEntry.arguments?.getString("tableId") ?: ""
            TableDetailScreen(
                tableId = tableId,
                onBack = { navController.popBackStack() },
                onOpenRow = { rowId -> navController.navigate("table/$tableId/row/$rowId") }
            )
        }

        composable(
            route = "table/{tableId}/row/{rowId}",
            arguments = listOf(
                navArgument("tableId") { type = NavType.StringType },
                navArgument("rowId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            RowDetailScreen(
                tableId = backStackEntry.arguments?.getString("tableId") ?: "",
                rowId = backStackEntry.arguments?.getString("rowId") ?: "",
                onBack = { navController.popBackStack() }
            )
        }
    }
}
