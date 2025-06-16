package com.example.gooddeedfeed.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.gooddeedfeed.data.remote.User
import com.example.gooddeedfeed.presentation.ui.components.CustomToastHost
import com.example.gooddeedfeed.presentation.ui.components.ToastManager
import com.example.gooddeedfeed.presentation.ui.components.rememberToastState

data class TabItem(
    val title: String,
    val icon: ImageVector,
    val screen: @Composable (User, () -> Unit) -> Unit,
)

@Composable
fun TabNavigationScreen(
    user: User,
    onLogout: () -> Unit,
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val toastState by rememberToastState()

    // Debug logging to help identify navigation issues
    println("TabNavigationScreen: User ID=${user.id}, Username=${user.username}, UserType=${user.user_type}")
    
    val tabs = NavigationConfig.getTabsForUserType(user.user_type)
    println("TabNavigationScreen: Generated ${tabs.size} tabs for user type ${user.user_type}")

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                    )
                }
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            tabs[selectedTabIndex].screen(user, onLogout)
            
            // Add toast overlay
            CustomToastHost(
                toastData = toastState,
                onDismiss = { ToastManager.dismiss() }
            )
        }
    }
}

 
