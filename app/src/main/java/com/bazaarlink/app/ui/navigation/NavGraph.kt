package com.bazaarlink.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bazaarlink.app.models.User
import com.bazaarlink.app.ui.buyer.BuyerShell
import com.bazaarlink.app.ui.buyer.BuyerWaitingRadarScreen
import com.bazaarlink.app.ui.buyer.QuoteFeedScreen
import com.bazaarlink.app.ui.chat.ChatDetailScreen
import com.bazaarlink.app.ui.vendor.VendorShell
import com.bazaarlink.app.ui.auth.AuthScreen
import com.bazaarlink.app.viewmodels.AuthViewModel
import com.bazaarlink.app.viewmodels.BuyerViewModel
import com.bazaarlink.app.viewmodels.ChatViewModel
import com.bazaarlink.app.viewmodels.VendorViewModel

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object BuyerShell : Screen("buyer_shell/{buyerId}") {
        fun createRoute(buyerId: String) = "buyer_shell/$buyerId"
    }
    object BuyerShellChats : Screen("buyer_shell_chats/{buyerId}") {
        fun createRoute(buyerId: String) = "buyer_shell_chats/$buyerId"
    }
    object BuyerRadar : Screen("buyer_radar/{requestId}") {
        fun createRoute(requestId: String) = "buyer_radar/$requestId"
    }
    object BuyerQuotes : Screen("buyer_quotes/{requestId}/{buyerId}") {
        fun createRoute(requestId: String, buyerId: String) = "buyer_quotes/$requestId/$buyerId"
    }
    object VendorShell : Screen("vendor_shell")
    object ChatDetail : Screen("chat/{chatId}/{currentUserId}") {
        fun createRoute(chatId: String, currentUserId: String) = "chat/$chatId/$currentUserId"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel(),
    buyerViewModel: BuyerViewModel = viewModel(),
    vendorViewModel: VendorViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(
                viewModel = authViewModel,
                onAuthSuccess = { isBuyer ->
                    val user = authViewModel.currentUser.value
                    val uid = user?.userId ?: "demo_user"
                    chatViewModel.loadUserChats(uid)
                    if (isBuyer) {
                        navController.navigate(Screen.BuyerShell.createRoute(uid)) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.VendorShell.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(
            route = Screen.BuyerShell.route,
            arguments = listOf(navArgument("buyerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val routeBuyerId = backStackEntry.arguments?.getString("buyerId") ?: "demo_buyer"
            val actualUid = currentUser?.userId ?: routeBuyerId
            val user = currentUser ?: User(userId = actualUid, role = "BUYER")
            BuyerShell(
                buyerViewModel = buyerViewModel,
                chatViewModel = chatViewModel,
                authViewModel = authViewModel,
                currentUser = user,
                buyerId = actualUid,
                initialTab = 0,
                onBroadcastStarted = { requestId ->
                    navController.navigate(Screen.BuyerRadar.createRoute(requestId))
                },
                onChatClicked = { chatId ->
                    navController.navigate(Screen.ChatDetail.createRoute(chatId, actualUid))
                },
                onSignOut = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onRoleSwitched = {
                    val activeUser = authViewModel.currentUser.value
                    val uid = activeUser?.userId ?: actualUid
                    chatViewModel.loadUserChats(uid)
                    if (activeUser?.role == "VENDOR") {
                        navController.navigate(Screen.VendorShell.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.BuyerShell.createRoute(uid)) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(
            route = Screen.BuyerShellChats.route,
            arguments = listOf(navArgument("buyerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val routeBuyerId = backStackEntry.arguments?.getString("buyerId") ?: "demo_buyer"
            val actualUid = currentUser?.userId ?: routeBuyerId
            val user = currentUser ?: User(userId = actualUid, role = "BUYER")
            BuyerShell(
                buyerViewModel = buyerViewModel,
                chatViewModel = chatViewModel,
                authViewModel = authViewModel,
                currentUser = user,
                buyerId = actualUid,
                initialTab = 1,
                onBroadcastStarted = { requestId ->
                    navController.navigate(Screen.BuyerRadar.createRoute(requestId))
                },
                onChatClicked = { chatId ->
                    navController.navigate(Screen.ChatDetail.createRoute(chatId, actualUid))
                },
                onSignOut = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onRoleSwitched = {
                    val activeUser = authViewModel.currentUser.value
                    val uid = activeUser?.userId ?: actualUid
                    chatViewModel.loadUserChats(uid)
                    if (activeUser?.role == "VENDOR") {
                        navController.navigate(Screen.VendorShell.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.BuyerShell.createRoute(uid)) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(
            route = Screen.BuyerRadar.route,
            arguments = listOf(navArgument("requestId") { type = NavType.StringType })
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
            BuyerWaitingRadarScreen(
                viewModel = buyerViewModel,
                requestId = requestId,
                onViewQuotesClicked = {
                    val buyerId = currentUser?.userId ?: "demo_buyer"
                    navController.navigate(Screen.BuyerQuotes.createRoute(requestId, buyerId))
                }
            )
        }

        composable(
            route = Screen.BuyerQuotes.route,
            arguments = listOf(
                navArgument("requestId") { type = NavType.StringType },
                navArgument("buyerId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
            val buyerId = backStackEntry.arguments?.getString("buyerId") ?: ""
            val actualBuyerId = currentUser?.userId ?: buyerId
            QuoteFeedScreen(
                viewModel = buyerViewModel,
                chatViewModel = chatViewModel,
                requestId = requestId,
                buyerId = actualBuyerId,
                buyerDisplayName = currentUser?.displayName ?: "Buyer",
                onDealDone = { _ ->
                    navController.navigate(Screen.BuyerShellChats.createRoute(actualBuyerId)) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.VendorShell.route) {
            val vendorUser = currentUser ?: User(
                userId = "vendor_demo",
                role = "VENDOR",
                displayName = "Star Mobiles"
            )
            VendorShell(
                vendorViewModel = vendorViewModel,
                chatViewModel = chatViewModel,
                authViewModel = authViewModel,
                vendorUser = vendorUser,
                onChatClicked = { chatId ->
                    navController.navigate(Screen.ChatDetail.createRoute(chatId, vendorUser.userId))
                },
                onSignOut = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onRoleSwitched = {
                    val activeUser = authViewModel.currentUser.value
                    val uid = activeUser?.userId ?: vendorUser.userId
                    chatViewModel.loadUserChats(uid)
                    if (activeUser?.role == "BUYER") {
                        navController.navigate(Screen.BuyerShell.createRoute(uid)) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.VendorShell.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(
            route = Screen.ChatDetail.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("currentUserId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            val currentUserId = backStackEntry.arguments?.getString("currentUserId") ?: ""
            ChatDetailScreen(
                viewModel = chatViewModel,
                chatId = chatId,
                currentUserId = currentUserId,
                onBack = {
                    val uid = currentUser?.userId ?: currentUserId
                    if (uid.isNotBlank()) chatViewModel.loadUserChats(uid)
                    navController.popBackStack()
                }
            )
        }
    }
}
