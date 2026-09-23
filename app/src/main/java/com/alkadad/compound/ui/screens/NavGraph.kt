package com.alkadad.compound.ui.screens

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alkadad.compound.data.repository.AuthRepository

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
        startDestination = if (isAuthenticated) "dashboard" else "login"
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
                onBack = { navController.popBackStack() }
            )
        }
    }
}
