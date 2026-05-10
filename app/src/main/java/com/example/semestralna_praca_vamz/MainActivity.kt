package com.example.semestralna_praca_vamz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.semestralna_praca_vamz.ui.SplitViewModel
import com.example.semestralna_praca_vamz.ui.screens.AddExpenseScreen
import com.example.semestralna_praca_vamz.ui.screens.GroupsScreen
import com.example.semestralna_praca_vamz.ui.screens.MainScreen
import com.example.semestralna_praca_vamz.ui.theme.Semestralna_praca_VAMZTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Semestralna_praca_VAMZTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SplitApp()
                }
            }
        }
    }
}

@Composable
fun SplitApp() {
    val navController = rememberNavController()
    val viewModel: SplitViewModel = viewModel()

    NavHost(navController = navController, startDestination = "groups") {
        composable("groups") {
            GroupsScreen(
                viewModel = viewModel,
                onGroupClick = { groupId -> navController.navigate("main/$groupId") }
            )
        }
        composable(
            route = "main/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.LongType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId") ?: -1L
            MainScreen(
                viewModel = viewModel,
                groupId = groupId,
                onBackClick = { navController.popBackStack() },
                onAddExpenseClick = { gid -> navController.navigate("add_expense/$gid") },
                onEditExpenseClick = { gid, eid -> navController.navigate("edit_expense/$gid/$eid") }
            )
        }
        composable(
            route = "add_expense/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.LongType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId") ?: -1L
            AddExpenseScreen(
                viewModel = viewModel,
                groupId = groupId,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = "edit_expense/{groupId}/{expenseId}",
            arguments = listOf(
                navArgument("groupId") { type = NavType.LongType },
                navArgument("expenseId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId") ?: -1L
            val expenseId = backStackEntry.arguments?.getLong("expenseId") ?: -1L
            AddExpenseScreen(
                viewModel = viewModel,
                groupId = groupId,
                onBackClick = { navController.popBackStack() },
                expenseId = expenseId
            )
        }
    }
}
