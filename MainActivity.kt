package com.example.ecofriendlyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ecofriendlyapp.ui.theme.EcoFriendlyTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this) // Initialize Firebase
        setContent {
            EcoFriendlyTheme {
                EcoFriendlyApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcoFriendlyApp() {
    val navController = rememberNavController()
    val auth = Firebase.auth // Access Firebase Authentication
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isSmallScreen = screenWidth < 360.dp // Threshold for small screens (approx. 5 inches)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Green Grid") },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFF2E7D32)
                ),
                modifier = Modifier.fillMaxWidth() // Ensure it spans the full width
            )
        },
        content = { padding ->
            Box(modifier = Modifier.padding(padding)) {
                NavHost(
                    navController = navController,
                    startDestination = "welcome",
                    modifier = Modifier
                        .fillMaxSize() // Fill available space
                        .padding(
                            horizontal = if (isSmallScreen) 8.dp else 16.dp, // Reduced padding on small screens
                            vertical = if (isSmallScreen) 4.dp else 8.dp
                        )
                ) {
                    composable("welcome") { WelcomeScreen(navController) }
                    composable("login") { LoginScreen(navController) }
                    composable("signup") { SignUpScreen(navController) }
                    composable("dashboard") { DashboardScreen(navController) }
                    composable("profile") { ProfileScreen(navController) }
                    composable("report") { ReportScreen(navController) }
                    composable("plans") { PlansScreen(navController) }
                }
            }
        }
    )
}