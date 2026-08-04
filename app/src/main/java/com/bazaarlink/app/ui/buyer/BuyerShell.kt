package com.bazaarlink.app.ui.buyer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bazaarlink.app.R
import com.bazaarlink.app.models.User
import com.bazaarlink.app.ui.account.AccountScreen
import com.bazaarlink.app.ui.chat.ChatListScreen
import com.bazaarlink.app.viewmodels.AuthViewModel
import com.bazaarlink.app.viewmodels.BuyerViewModel
import com.bazaarlink.app.viewmodels.ChatViewModel

@Composable
fun BuyerShell(
    buyerViewModel: BuyerViewModel,
    chatViewModel: ChatViewModel,
    authViewModel: AuthViewModel,
    currentUser: User,
    buyerId: String,
    initialTab: Int = 0,
    onBroadcastStarted: (requestId: String) -> Unit,
    onViewSentRequestsClicked: () -> Unit = {},
    onChatClicked: (chatId: String) -> Unit,
    onSignOut: () -> Unit,
    onRoleSwitched: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = null) },
                    label = { Text(text = stringResource(id = R.string.tab_my_request)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(imageVector = Icons.Default.Chat, contentDescription = null) },
                    label = { Text(text = stringResource(id = R.string.tab_chats)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                    label = { Text(text = stringResource(id = R.string.tab_account)) }
                )

            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
            when (selectedTab) {
                0 -> BuyerHomeScreen(
                    viewModel = buyerViewModel,
                    buyerId = buyerId,
                    onBroadcastStarted = onBroadcastStarted,
                    onViewSentRequestsClicked = onViewSentRequestsClicked
                )

                1 -> ChatListScreen(
                    viewModel = chatViewModel,
                    currentUserId = buyerId,
                    onChatClicked = onChatClicked
                )
                2 -> AccountScreen(
                    authViewModel = authViewModel,
                    user = currentUser,
                    onSignOut = onSignOut,
                    onRoleSwitched = onRoleSwitched
                )
            }
        }
    }
}
