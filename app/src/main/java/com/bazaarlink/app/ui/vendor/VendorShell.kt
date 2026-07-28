package com.bazaarlink.app.ui.vendor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
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
import com.bazaarlink.app.viewmodels.ChatViewModel
import com.bazaarlink.app.viewmodels.VendorViewModel

@Composable
fun VendorShell(
    vendorViewModel: VendorViewModel,
    chatViewModel: ChatViewModel,
    authViewModel: AuthViewModel,
    vendorUser: User,
    initialTab: Int = 0,
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
                    icon = { Icon(imageVector = Icons.Default.Store, contentDescription = null) },
                    label = { Text(text = stringResource(id = R.string.tab_requests)) }
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
                    label = { Text(text = "Account") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
            when (selectedTab) {
                0 -> VendorFeedScreen(
                    viewModel = vendorViewModel,
                    vendorUser = vendorUser
                )
                1 -> ChatListScreen(
                    viewModel = chatViewModel,
                    currentUserId = vendorUser.userId,
                    onChatClicked = onChatClicked
                )
                2 -> AccountScreen(
                    authViewModel = authViewModel,
                    user = vendorUser,
                    onSignOut = onSignOut,
                    onRoleSwitched = onRoleSwitched
                )
            }
        }
    }
}
