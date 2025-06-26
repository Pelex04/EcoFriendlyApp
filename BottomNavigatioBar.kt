package com.example.ecofriendlyapp

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf("dashboard", "report", "plans", "profile") // Match design routes
    val labels = listOf("Home", "Report", "Plans", "Profile") // Match design labels
    val icons = listOf(
        painterResource(id = R.drawable.home),
        painterResource(id = R.drawable.report),
        painterResource(id = R.drawable.plans),
        painterResource(id = R.drawable.profile)
    )

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isSmallScreen = screenWidth < 360.dp // Threshold for small screens (~5 inches)

    NavigationBar(
        containerColor = Color(0xFF2E7D32),
        contentColor = Color.White,
        modifier = Modifier.height(if (isSmallScreen) 80.dp else 120.dp) // Dynamic height
    ) {
        val navBackStackEntry = navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry.value?.destination?.route

        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(icons[index], contentDescription = labels[index], tint = Color.White, modifier = Modifier.size(if (isSmallScreen) 16.dp else 24.dp)) },
                label = { Text(labels[index], color = Color.White, fontSize = if (isSmallScreen) 12.sp else 14.sp) },
                selected = currentRoute == item,
                onClick = { navController.navigate(item) { launchSingleTop = true } },
                modifier = Modifier.weight(1f)
            )
        }
    }
}