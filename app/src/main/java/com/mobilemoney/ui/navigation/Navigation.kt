package com.mobilemoney.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mobilemoney.ui.screens.AccountFormScreen
import com.mobilemoney.ui.screens.TransactionFormScreen
import com.mobilemoney.ui.screens.TransactionListScreen
import java.util.UUID

sealed class Screen(val route: String) {
    data object TransactionList : Screen("transactions")
    data object CreateTransaction : Screen("create")
    data object EditTransaction : Screen("edit/{transactionId}") {
        fun createRoute(transactionId: UUID) = "edit/$transactionId"
    }
    data object Accounts : Screen("accounts")
    data object CreateAccount : Screen("account/create")
    data object EditAccount : Screen("account/edit/{accountId}") {
        fun createRoute(accountId: UUID) = "account/edit/$accountId"
    }
}

@Composable
fun MobileMoneyNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.TransactionList.route
    ) {
        composable(Screen.TransactionList.route) {
            TransactionListScreen(
                onAddClick = {
                    navController.navigate(Screen.CreateTransaction.route)
                },
                onTransactionClick = { transactionId ->
                    navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                },
                onAccountsClick = {
                    navController.navigate(Screen.Accounts.route)
                }
            )
        }

        composable(Screen.CreateTransaction.route) {
            TransactionFormScreen(
                transactionId = null,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(
                navArgument("transactionId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId")?.let {
                UUID.fromString(it)
            }
            TransactionFormScreen(
                transactionId = transactionId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Accounts.route) {
            com.mobilemoney.ui.screens.AccountListScreen(
                onAddClick = {
                    navController.navigate(Screen.CreateAccount.route)
                },
                onAccountClick = { accountId ->
                    navController.navigate(Screen.EditAccount.createRoute(accountId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.CreateAccount.route) {
            AccountFormScreen(
                accountId = null,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.EditAccount.route,
            arguments = listOf(
                navArgument("accountId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId")?.let {
                UUID.fromString(it)
            }
            AccountFormScreen(
                accountId = accountId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}