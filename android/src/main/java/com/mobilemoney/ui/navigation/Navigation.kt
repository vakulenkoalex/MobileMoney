package com.mobilemoney.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mobilemoney.MobileMoneyApp
import com.mobilemoney.domain.usecase.transaction.ParseClipboardTransactionUseCase
import com.mobilemoney.ui.screens.AccountFormScreen
import com.mobilemoney.ui.screens.CategoryFormScreen
import com.mobilemoney.ui.screens.LoginScreen
import com.mobilemoney.ui.screens.MessageRegexFormScreen
import com.mobilemoney.ui.screens.MessageRegexListScreen
import com.mobilemoney.ui.screens.MessageListScreen
import com.mobilemoney.ui.screens.SenderFormScreen
import com.mobilemoney.ui.screens.SenderListScreen
import com.mobilemoney.ui.screens.SettingsScreen
import com.mobilemoney.ui.screens.TransactionFormScreen
import com.mobilemoney.ui.screens.TransactionListScreen
import com.mobilemoney.viewmodel.AccountFormViewModel
import com.mobilemoney.viewmodel.AccountListViewModel
import com.mobilemoney.viewmodel.CategoryFormViewModel
import com.mobilemoney.viewmodel.CategoryListViewModel
import com.mobilemoney.viewmodel.LoginViewModel
import com.mobilemoney.viewmodel.MessageListViewModel
import com.mobilemoney.viewmodel.MessageRegexFormViewModel
import com.mobilemoney.viewmodel.MessageRegexListViewModel
import com.mobilemoney.viewmodel.SenderFormViewModel
import com.mobilemoney.viewmodel.SenderListViewModel
import com.mobilemoney.viewmodel.SettingsViewModel
import com.mobilemoney.viewmodel.TransactionFormViewModel
import com.mobilemoney.viewmodel.TransactionListViewModel
import java.util.UUID

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object TransactionList : Screen("transactions")
    data object CreateTransaction : Screen("create_transaction/{uuid}") {
        fun createRoute() = "create_transaction/${UUID.randomUUID()}"
    }
    data object EditTransaction : Screen("edit/{transactionId}") {
        fun createRoute(transactionId: UUID) = "edit/$transactionId"
    }
    data object Accounts : Screen("accounts")
    data object CreateAccount : Screen("account/create")
    data object EditAccount : Screen("account/edit/{accountId}") {
        fun createRoute(accountId: UUID) = "account/edit/$accountId"
    }
    data object Categories : Screen("categories")
    data object CreateCategory : Screen("category/create")
    data object EditCategory : Screen("category/edit/{categoryId}") {
        fun createRoute(categoryId: UUID) = "category/edit/$categoryId"
    }
    data object Settings : Screen("settings")
    data object RegexList : Screen("regexes")
    data object CreateRegex : Screen("regex/create")
    data object EditRegex : Screen("regex/edit/{regexId}") {
        fun createRoute(regexId: UUID) = "regex/edit/$regexId"
    }
    data object Messages : Screen("messages")
    data object Senders : Screen("senders")
    data object CreateSender : Screen("sender/create")
    data object EditSender : Screen("sender/edit/{senderId}") {
        fun createRoute(senderId: String) = "sender/edit/$senderId"
    }
}

val bottomNavItems = listOf(
    Screen.TransactionList,
    Screen.Accounts,
    Screen.Settings
)

@Composable
fun MobileMoneyNavigation(
    isLoggedIn: Boolean,
    onLoginSuccess: () -> Unit,
    transactionListViewModel: TransactionListViewModel,
    transactionFormViewModel: TransactionFormViewModel,
    parseClipboardTransactionUseCase: ParseClipboardTransactionUseCase,
    accountListViewModel: AccountListViewModel,
    accountFormViewModel: AccountFormViewModel,
    categoryListViewModel: CategoryListViewModel,
    categoryFormViewModel: CategoryFormViewModel,
    settingsViewModel: SettingsViewModel,
    messageRegexListViewModel: MessageRegexListViewModel,
    messageRegexFormViewModel: MessageRegexFormViewModel,
    messageListViewModel: MessageListViewModel,
    senderListViewModel: SenderListViewModel,
    senderFormViewModel: SenderFormViewModel,
    loginViewModel: LoginViewModel
) {
    val context = LocalContext.current
    val app = context.applicationContext as MobileMoneyApp
    var showLoading by remember { mutableStateOf(app.isFirstRun()) }

    if (showLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Text("Инициализация базы данных...", modifier = Modifier.padding(top = 16.dp))
        }
        return
    }

    if (!isLoggedIn) {
        LoginScreen(
            onLoginSuccess = onLoginSuccess,
            viewModel = loginViewModel
        )
        return
    }

    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = when (screen) {
                                    Screen.TransactionList -> Icons.Default.Home
                                    Screen.Accounts -> Icons.Default.CreditCard
                                    Screen.Settings -> Icons.Default.Settings
                                    else -> Icons.Default.Home
                                },
                                contentDescription = null
                            )
                        },
                        label = {
                            Text(
                                when (screen) {
                                    Screen.TransactionList -> "Операции"
                                    Screen.Accounts -> "Счета"
                                    Screen.Settings -> "Настройки"
                                    else -> ""
                                }
                            )
                        },
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.TransactionList.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { _ ->
        NavHost(
            navController = navController,
            startDestination = Screen.TransactionList.route
        ) {
                composable(Screen.TransactionList.route) {
                    TransactionListScreen(
                        onAddClick = {
                            transactionFormViewModel.resetForNewTransaction()
                            navController.navigate(Screen.CreateTransaction.createRoute())
                        },
                        onTransactionClick = { transactionId ->
                            navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                        },
                        onClipboardPrefill = { prefillData ->
                            transactionFormViewModel.prefillFromClipboard(prefillData)
                            navController.navigate(Screen.CreateTransaction.createRoute())
                        },
                        viewModel = transactionListViewModel,
                        parseClipboardTransactionUseCase = parseClipboardTransactionUseCase
                    )
                }

                composable(
                    route = "create_transaction/{uuid}",
                    arguments = listOf(
                        navArgument("uuid") { type = NavType.StringType }
                    )
                ) {
                    TransactionFormScreen(
                        transactionId = null,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        viewModel = transactionFormViewModel
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
                        },
                        viewModel = transactionFormViewModel
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
                        viewModel = accountListViewModel
                    )
                }

                composable(Screen.CreateAccount.route) {
                    AccountFormScreen(
                        accountId = null,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        viewModel = accountFormViewModel
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
                        },
                        viewModel = accountFormViewModel
                    )
                }

                composable(Screen.Categories.route) {
                    com.mobilemoney.ui.screens.CategoryListScreen(
                        onAddClick = {
                            navController.navigate(Screen.CreateCategory.route)
                        },
                        onCategoryClick = { categoryId ->
                            navController.navigate(Screen.EditCategory.createRoute(categoryId))
                        },
                        onNavigateBack = { navController.popBackStack() },
                        viewModel = categoryListViewModel
                    )
                }

                composable(Screen.CreateCategory.route) {
                    CategoryFormScreen(
                        categoryId = null,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        viewModel = categoryFormViewModel
                    )
                }

                composable(
                    route = Screen.EditCategory.route,
                    arguments = listOf(
                        navArgument("categoryId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val categoryId = backStackEntry.arguments?.getString("categoryId")?.let {
                        UUID.fromString(it)
                    }
                    CategoryFormScreen(
                        categoryId = categoryId,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        viewModel = categoryFormViewModel
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onNavigateToCategories = {
                            navController.navigate(Screen.Categories.route)
                        },
                        onNavigateToRegexes = {
                            navController.navigate(Screen.RegexList.route)
                        },
                        onNavigateToMessages = {
                            navController.navigate(Screen.Messages.route)
                        },
                        onNavigateToSenders = {
                            navController.navigate(Screen.Senders.route)
                        },
                        viewModel = settingsViewModel
                    )
                }

                composable(Screen.RegexList.route) {
                    MessageRegexListScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onAddClick = {
                            navController.navigate(Screen.CreateRegex.route)
                        },
                        onRegexClick = { regexId ->
                            navController.navigate(Screen.EditRegex.createRoute(regexId))
                        },
                        viewModel = messageRegexListViewModel
                    )
                }

                composable(Screen.CreateRegex.route) {
                    MessageRegexFormScreen(
                        regexId = null,
                        onNavigateBack = { navController.popBackStack() },
                        viewModel = messageRegexFormViewModel
                    )
                }

                composable(
                    route = Screen.EditRegex.route,
                    arguments = listOf(
                        navArgument("regexId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val regexId = backStackEntry.arguments?.getString("regexId")?.let {
                        UUID.fromString(it)
                    }
                    MessageRegexFormScreen(
                        regexId = regexId,
                        onNavigateBack = { navController.popBackStack() },
                        viewModel = messageRegexFormViewModel
                    )
                }

                composable(Screen.Messages.route) {
                    MessageListScreen(
                        onNavigateBack = { navController.popBackStack() },
                        viewModel = messageListViewModel
                    )
                }

                composable(Screen.Senders.route) {
                    SenderListScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onAddClick = { navController.navigate(Screen.CreateSender.route) },
                        onSenderClick = { senderId ->
                            navController.navigate(Screen.EditSender.createRoute(senderId))
                        },
                        viewModel = senderListViewModel
                    )
                }

                composable(Screen.CreateSender.route) {
                    SenderFormScreen(
                        senderId = null,
                        onNavigateBack = { navController.popBackStack() },
                        viewModel = senderFormViewModel
                    )
                }

                composable(
                    route = Screen.EditSender.route,
                    arguments = listOf(
                        navArgument("senderId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val senderId = backStackEntry.arguments?.getString("senderId")
                    SenderFormScreen(
                        senderId = senderId,
                        onNavigateBack = { navController.popBackStack() },
                        viewModel = senderFormViewModel
                    )
                }
            }
        }
}
