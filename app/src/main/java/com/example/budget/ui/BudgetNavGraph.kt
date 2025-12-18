package com.example.budget.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.example.budget.data.db.BudgetDatabase
import com.example.budget.data.repository.*
import com.example.budget.ui.borrowlend.AddBorrowLendScreen
import com.example.budget.ui.borrowlend.BorrowLendScreen
import com.example.budget.ui.borrowlend.PersonDetailScreen
import com.example.budget.ui.expenses.AddExpenseScreen
import com.example.budget.ui.expenses.ExpenseScreen
import com.example.budget.ui.home.HomeScreen
import com.example.budget.viewmodel.*

@Composable
fun BudgetNavGraph(
    navController: NavHostController,
    themeViewModel: ThemeViewModel
) {
    val context = LocalContext.current
    val database = remember { BudgetDatabase.getDatabase(context) }
    
    val expenseRepo = remember { ExpenseTransactionRepository(database.expenseTransactionDao()) }
    val categoryRepo = remember { ExpenseCategoryRepository(database.expenseCategoryDao()) }
    val personRepo = remember { PersonRepository(database.personDao()) }
    val borrowLendRepo = remember { BorrowLendRepository(database.borrowLendTransactionDao(), database.settlementDao()) }

    NavHost(
        navController = navController,
        startDestination = "home_graph"
    ) {
        // HOME GRAPH
        navigation(startDestination = "home_dashboard", route = "home_graph") {
            composable("home_dashboard") {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModelFactory(expenseRepo, borrowLendRepo)
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    themeViewModel = themeViewModel,
                    onNavigateToAddExpense = { navController.navigate("expense_graph/add") },
                    onNavigateToAddBorrowLend = { navController.navigate("borrow_graph/add") }
                )
            }
        }

        // EXPENSE GRAPH
        navigation(startDestination = "expense_list", route = "expense_graph") {
            composable("expense_list") { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("expense_graph")
                }
                val expenseViewModel: ExpenseViewModel = viewModel(
                    viewModelStoreOwner = parentEntry,
                    factory = ExpenseViewModelFactory(expenseRepo, categoryRepo)
                )
                ExpenseScreen(
                    viewModel = expenseViewModel,
                    navController = navController,
                    onNavigateToAdd = { navController.navigate("expense_graph/add") }
                )
            }
            composable(
                "expense_graph/add?transactionId={transactionId}",
                arguments = listOf(navArgument("transactionId") { 
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("expense_graph")
                }
                val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: -1L
                val expenseViewModel: ExpenseViewModel = viewModel(
                    viewModelStoreOwner = parentEntry,
                    factory = ExpenseViewModelFactory(expenseRepo, categoryRepo)
                )
                AddExpenseScreen(
                    viewModel = expenseViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    transactionId = if (transactionId != -1L) transactionId else null
                )
            }
        }

        // BORROW LEND GRAPH
        navigation(startDestination = "borrow_list", route = "borrow_graph") {
            composable("borrow_list") { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("borrow_graph")
                }
                val borrowLendViewModel: BorrowLendViewModel = viewModel(
                    viewModelStoreOwner = parentEntry,
                    factory = BorrowLendViewModelFactory(borrowLendRepo, personRepo)
                )
                BorrowLendScreen(
                    viewModel = borrowLendViewModel,
                    borrowLendRepo = borrowLendRepo,
                    onNavigateToAdd = { navController.navigate("borrow_graph/add") },
                    onPersonClick = { id -> navController.navigate("borrow_graph/person/$id") }
                )
            }
            composable(
                "borrow_graph/add?transactionId={transactionId}",
                arguments = listOf(navArgument("transactionId") { 
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("borrow_graph")
                }
                val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: -1L
                val borrowLendViewModel: BorrowLendViewModel = viewModel(
                    viewModelStoreOwner = parentEntry,
                    factory = BorrowLendViewModelFactory(borrowLendRepo, personRepo)
                )
                AddBorrowLendScreen(
                    viewModel = borrowLendViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    transactionId = if (transactionId != -1L) transactionId else null
                )
            }
            composable(
                "borrow_graph/person/{personId}",
                arguments = listOf(navArgument("personId") { type = NavType.LongType })
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("borrow_graph")
                }
                val personId = backStackEntry.arguments?.getLong("personId") ?: 0L
                val borrowLendViewModel: BorrowLendViewModel = viewModel(
                    viewModelStoreOwner = parentEntry,
                    factory = BorrowLendViewModelFactory(borrowLendRepo, personRepo)
                )
                PersonDetailScreen(
                    personId = personId,
                    viewModel = borrowLendViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onEditTransaction = { transId -> navController.navigate("borrow_graph/add?transactionId=$transId") }
                )
            }
        }
    }
}
