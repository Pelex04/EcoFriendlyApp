package com.example.ecofriendlyapp

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.net.http.SslError
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.JsResult
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.UUID
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.views.overlay.Marker

// API Interface and Setup (unchanged)
interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body token: Map<String, String>): retrofit2.Response<AuthResponse>
}

data class AuthResponse(val token: String, val uid: String)

val retrofit = Retrofit.Builder()
    .baseUrl("http://10.0.2.2:3000/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()
val apiService = retrofit.create(ApiService::class.java)

@Composable
fun WelcomeScreen(navController: NavController) {
    var progress by remember { mutableStateOf(0f) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isSmallScreen = screenWidth < 360.dp

    LaunchedEffect(Unit) {
        while (progress < 1f) {
            delay(100)
            progress += 0.01f
            if (progress >= 1f) {
                navController.navigate("login") { popUpTo("welcome") { inclusive = true } }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isSmallScreen) 8.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(if (isSmallScreen) 40.dp else 80.dp))
            Text("WELCOME TO GREEN GRID", fontSize = if (isSmallScreen) 20.sp else 24.sp, color = Color(0xFF2E7D32))
            Spacer(modifier = Modifier.height(if (isSmallScreen) 20.dp else 46.dp))
            Image(
                painter = painterResource(id = R.drawable.eco_friendly_logo),
                contentDescription = "Eco Logo",
                modifier = Modifier.size(if (isSmallScreen) 150.dp else 250.dp)
            )
            Spacer(modifier = Modifier.height(if (isSmallScreen) 20.dp else 46.dp))
            Text("${(progress * 100).toInt()}%", fontSize = if (isSmallScreen) 16.sp else 20.sp, color = Color.Black)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(if (isSmallScreen) 6.dp else 8.dp),
                color = Color(0xFF2E7D32),
                trackColor = Color.White
            )
        }
        Text(
            buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color.Black)) { append("Powered by BUILD-") }
                withStyle(style = SpanStyle(color = Color(0xFF2E7D32))) { append("GREEN") }
            },
            fontSize = if (isSmallScreen) 10.sp else 12.sp
        )
    }
}

@Composable
fun LoginScreen(navController: NavController) {
    val auth = Firebase.auth
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isSmallScreen = screenWidth < 360.dp

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("678746402534-j22uetg29sn8bjobcsj04v3mnknk3avm.apps.googleusercontent.com")
            .requestEmail()
            .build()
    }
    val googleSignInClient = GoogleSignIn.getClient(navController.context, gso)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        scope.launch {
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    isLoading = true
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            isLoading = false
                            navController.navigate("dashboard") { popUpTo("login") { inclusive = true } }
                        } else {
                            isLoading = false
                            errorMessage = authTask.exception?.message ?: "Google login failed"
                        }
                    }
                }
            } catch (e: ApiException) {
                isLoading = false
                errorMessage = "Sign-in error: ${e.statusCode} - ${e.message}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isSmallScreen) 8.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.eco_logo),
            contentDescription = "Eco Friendly Logo",
            modifier = Modifier.size(if (isSmallScreen) 40.dp else 50.dp)
        )
        Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 16.dp))
        Text("Login to your account", fontSize = if (isSmallScreen) 18.sp else 20.sp, color = Color(0xFF2E7D32), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(if (isSmallScreen) 16.dp else 25.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 25.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(if (isSmallScreen) 4.dp else 8.dp))
        Text(
            text = "Forgot Password?",
            color = Color(0xFF2E7D32),
            modifier = Modifier.align(Alignment.End).clickable {
                scope.launch {
                    if (email.isNotEmpty()) {
                        try {
                            auth.sendPasswordResetEmail(email).await()
                            errorMessage = "Password reset email sent to $email."
                        } catch (e: Exception) {
                            errorMessage = "Failed to send reset email: ${e.message}"
                        }
                    } else {
                        errorMessage = "Please enter your email first."
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(if (isSmallScreen) 16.dp else 25.dp))
        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    isLoading = true
                    errorMessage = ""
                    scope.launch {
                        try {
                            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    isLoading = false
                                    navController.navigate("dashboard") { popUpTo("login") { inclusive = true } }
                                } else {
                                    isLoading = false
                                    errorMessage = task.exception?.message ?: "Login failed"
                                }
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = e.message ?: "An error occurred"
                        }
                    }
                } else {
                    errorMessage = "Please fill all fields"
                }
            },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            modifier = Modifier.fillMaxWidth().height(if (isSmallScreen) 40.dp else 48.dp)
        ) { Text("Sign in", color = Color.White) }
        Spacer(modifier = Modifier.height(if (isSmallScreen) 4.dp else 8.dp))
        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
        }
        Text("or", fontSize = if (isSmallScreen) 14.sp else 16.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(if (isSmallScreen) 4.dp else 8.dp))
        Button(
            onClick = {
                isLoading = true
                scope.launch { val signInIntent = googleSignInClient.signInIntent; launcher.launch(signInIntent) }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(if (isSmallScreen) 40.dp else 48.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(id = R.drawable.google), contentDescription = "Google Icon", tint = Color.Unspecified)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign in with Google", color = Color(0xFF2E7D32))
            }
        }
        if (isLoading) CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        Spacer(modifier = Modifier.height(if (isSmallScreen) 10.dp else 14.dp))
        Text(
            "Don't have an account? Sign up",
            fontSize = if (isSmallScreen) 10.sp else 12.sp,
            color = Color(0xFF2E7D32),
            modifier = Modifier.clickable { navController.navigate("signup") }
        )
        Spacer(modifier = Modifier.height(if (isSmallScreen) 20.dp else 30.dp))
        Text(
            buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color.Black)) { append("Powered by BUILD-") }
                withStyle(style = SpanStyle(color = Color(0xFF2E7D32))) { append("GREEN") }
            },
            fontSize = if (isSmallScreen) 10.sp else 12.sp
        )
    }
}

@Composable
fun SignUpScreen(navController: NavController) {
    val auth = Firebase.auth
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isSmallScreen = screenWidth < 360.dp

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("678746402534-j22uetg29sn8bjobcsj04v3mnknk3avm.apps.googleusercontent.com")
            .requestEmail()
            .build()
    }
    val googleSignInClient = GoogleSignIn.getClient(navController.context, gso)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        scope.launch {
            isLoading = true
            errorMessage = ""
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            isLoading = false
                            scope.launch {
                                val user = auth.currentUser
                                if (user != null) {
                                    Firebase.firestore.collection("users").document(user.uid)
                                        .set(mapOf("email" to user.email, "displayName" to user.displayName))
                                        .await()
                                }
                            }
                            navController.navigate("dashboard") { popUpTo("signup") { inclusive = true } }
                        } else {
                            isLoading = false
                            errorMessage = authTask.exception?.message ?: "Google sign-up failed"
                        }
                    }
                }
            } catch (e: ApiException) {
                isLoading = false
                errorMessage = "Sign-up error: ${e.statusCode} - ${e.message}"
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "Unexpected error: ${e.message}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isSmallScreen) 8.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(painterResource(id = android.R.drawable.ic_menu_revert), contentDescription = "Back")
            }
        }
        Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 16.dp))
        Image(
            painter = painterResource(id = R.drawable.eco_logo),
            contentDescription = "Eco Friendly Logo",
            modifier = Modifier.size(if (isSmallScreen) 40.dp else 50.dp)
        )
        Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 16.dp))
        Text(
            buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color.Black)) { append("Join the ") }
                withStyle(style = SpanStyle(color = Color(0xFF2E7D32))) { append("Green") }
                withStyle(style = SpanStyle(color = Color.Black)) { append(" Revolution") }
            },
            fontSize = if (isSmallScreen) 14.sp else 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 16.dp))
        Text("Create your Account", fontSize = if (isSmallScreen) 18.sp else 20.sp, color = Color(0xFF2E7D32))
        Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 16.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(if (isSmallScreen) 4.dp else 8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(if (isSmallScreen) 4.dp else 8.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 16.dp))
        if (isLoading) CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        else {
            Button(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                        if (password == confirmPassword) {
                            isLoading = true
                            errorMessage = ""
                            scope.launch {
                                try {
                                    auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            isLoading = false
                                            navController.navigate("dashboard") { popUpTo("signup") { inclusive = true } }
                                        } else {
                                            isLoading = false
                                            errorMessage = task.exception?.message ?: "Signup failed"
                                        }
                                    }
                                } catch (e: Exception) {
                                    isLoading = false
                                    errorMessage = e.message ?: "An error occurred"
                                }
                            }
                        } else errorMessage = "Passwords do not match"
                    } else errorMessage = "Please fill all fields"
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                modifier = Modifier.fillMaxWidth().height(if (isSmallScreen) 40.dp else 48.dp)
            ) { Text("Sign up", color = Color.White) }
        }
        Spacer(modifier = Modifier.height(if (isSmallScreen) 4.dp else 8.dp))
        if (errorMessage.isNotEmpty()) Text(errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
        Text("or", fontSize = if (isSmallScreen) 14.sp else 16.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(if (isSmallScreen) 4.dp else 8.dp))
        Button(
            onClick = {
                isLoading = true
                scope.launch { val signInIntent = googleSignInClient.signInIntent; launcher.launch(signInIntent) }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(if (isSmallScreen) 40.dp else 48.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(id = R.drawable.google), contentDescription = "Google Icon", tint = Color.Unspecified)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign in with Google", color = Color(0xFF2E7D32))
            }
        }
        Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 16.dp))
        Text(
            buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color.Black)) { append("Powered by BUILD-") }
                withStyle(style = SpanStyle(color = Color(0xFF2E7D32))) { append("GREEN") }
            },
            fontSize = if (isSmallScreen) 10.sp else 12.sp
        )
    }
}

@Composable
fun DashboardScreen(navController: NavController) {
    val auth = Firebase.auth
    val currentUser = auth.currentUser
    var username by remember { mutableStateOf("Green Guardian") }
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val isSmallScreen = configuration.screenWidthDp.dp < 360.dp
    val context = LocalContext.current
    val mapView = remember { org.osmdroid.views.MapView(context) }

    LaunchedEffect(Unit) {
        org.osmdroid.config.Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(14.0)
        mapView.controller.setCenter(org.osmdroid.util.GeoPoint(-15.7861, 35.0088))
        val binIcon = ContextCompat.getDrawable(context, R.drawable.bin)?.let { drawable ->
            Bitmap.createScaledBitmap(
                (drawable.toBitmap().copy(Bitmap.Config.ARGB_8888, true)),
                25, 41, true
            ).let { BitmapDrawable(context.resources, it) }
        }
        val wasteBins = listOf(
            org.osmdroid.util.GeoPoint(-15.78, 35.02),
            org.osmdroid.util.GeoPoint(-15.79, 35.05),
            org.osmdroid.util.GeoPoint(-15.77, 35.00),
            org.osmdroid.util.GeoPoint(-15.80, 35.08)
        )
        wasteBins.forEach { point ->
            val marker = org.osmdroid.views.overlay.Marker(mapView)
            marker.position = point
            marker.title = "Waste Bin"
            marker.icon = binIcon
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(marker)
        }
        mapView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val geoPoint = mapView.projection.fromPixels(event.x.toInt(), event.y.toInt())
                val lat = String.format("%.4f", geoPoint.latitude)
                val lon = String.format("%.4f", geoPoint.longitude)
                Toast.makeText(context, "Coordinates: Lat $lat, Lon $lon", Toast.LENGTH_SHORT).show()
                true
            } else {
                false
            }
        }
        mapView.invalidate()
    }

    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            val userDoc = Firebase.firestore.collection("users").document(currentUser.uid).get().await()
            username = userDoc.getString("displayName") ?: currentUser.email?.split("@")?.getOrNull(0) ?: "Green Guardian"
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Welcome Text Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (isSmallScreen) 8.dp else 16.dp, start = 16.dp, end = 16.dp)
        ) {
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color.Black)) { append("WELCOME BACK ") }
                    withStyle(style = SpanStyle(color = Color(0xFF2E7D32))) { append("Green ") }
                    withStyle(style = SpanStyle(color = Color.Black)) { append("Guardian ") }
                    withStyle(style = SpanStyle(color = Color.Black)) { append(username) }
                },
                fontSize = if (isSmallScreen) 18.sp else 20.sp,
                textAlign = TextAlign.Center
            )
        }

        // Map Section with Dynamic Height
        val topPadding = if (isSmallScreen) 8.dp else 16.dp
        val bottomNavHeight = 56.dp // Typical BottomNavigationBar height, adjust if different
        val availableHeight = screenHeight - topPadding - bottomNavHeight // Approximate available space
        Box(
            modifier = Modifier
                .weight(1f, fill = false) // Use weight without forcing full height
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(availableHeight.coerceAtLeast(200.dp)) // Ensure minimum height
        ) {
            AndroidView(
                { mapView },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(availableHeight.coerceAtLeast(200.dp))
            )
        }

        // Bottom Navigation Bar
        BottomNavigationBar(navController)
    }
}

@Composable
fun ProfileScreen(navController: NavController) {
    val auth = Firebase.auth
    val currentUser = auth.currentUser
    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var isEditingName by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(displayName) }
    var showEditDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isSmallScreen = screenWidth < 360.dp

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            displayName = currentUser.displayName ?: currentUser.email?.split("@")?.getOrNull(0) ?: "Green Guardian"
        } else displayName = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isSmallScreen) 8.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(painterResource(id = android.R.drawable.ic_menu_revert), contentDescription = "Back", tint = Color.Gray)
            }
            Text("Profile", fontSize = if (isSmallScreen) 18.sp else 20.sp, color = Color.Black)
            IconButton(onClick = { /* Handle menu */ }) {
                Icon(painterResource(id = android.R.drawable.ic_menu_more), contentDescription = "Menu", tint = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 16.dp))
        Box(
            modifier = Modifier
                .size(if (isSmallScreen) 80.dp else 120.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
        )
        Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 16.dp))

        if (isEditingName) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row {
                Button(onClick = {
                    scope.launch {
                        currentUser?.updateProfile(com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(newName)
                            .build())?.await()
                        currentUser?.let {
                            Firebase.firestore.collection("users").document(it.uid)
                                .set(mapOf("displayName" to newName), SetOptions.merge())?.await()
                        }
                        displayName = newName
                        isEditingName = false
                    }
                }, modifier = Modifier.padding(end = 8.dp)) { Text("Save") }
                Button(onClick = { isEditingName = false }) { Text("Cancel") }
            }
        } else {
            Text(displayName.ifEmpty { currentUser?.email?.split("@")?.getOrNull(0) ?: "User" }, fontSize = if (isSmallScreen) 16.sp else 18.sp, color = Color.Black)
            Text("@${currentUser?.email ?: "noemail"}", fontSize = if (isSmallScreen) 12.sp else 14.sp, color = Color.Gray)
            TextButton(onClick = { isEditingName = true }) { Text("Edit Name", color = Color(0xFF2E7D32)) }
        }
        Spacer(modifier = Modifier.height(if (isSmallScreen) 16.dp else 24.dp))

        Button(
            onClick = { showEditDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth(if (isSmallScreen) 0.7f else 0.5f).height(if (isSmallScreen) 40.dp else 48.dp)
        ) { Text("Edit Profile", color = Color.White) }
        Spacer(modifier = Modifier.height(if (isSmallScreen) 20.dp else 32.dp))

        if (showEditDialog) EditProfileDialog(onDismiss = { showEditDialog = false }, onSave = { newEmail, newPassword ->
            scope.launch {
                val refreshedUser = auth.currentUser
                if (refreshedUser != null) {
                    displayName = refreshedUser.displayName ?: ""
                }
            }
        })

        Text("Settings", fontSize = if (isSmallScreen) 14.sp else 16.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 16.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(id = android.R.drawable.ic_menu_manage), contentDescription = "User Management", tint = Color(0xFF3F51B5))
            Spacer(modifier = Modifier.width(if (isSmallScreen) 8.dp else 16.dp))
            Text("User Management", fontSize = if (isSmallScreen) 14.sp else 16.sp, color = Color.Black)
            Spacer(modifier = Modifier.weight(1f))
            Icon(painterResource(id = android.R.drawable.ic_menu_more), contentDescription = "More", tint = Color.Black)
        }
        Spacer(modifier = Modifier.height(if (isSmallScreen) 10.dp else 15.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(id = android.R.drawable.ic_menu_info_details), contentDescription = "Information", tint = Color(0xFF3F51B5))
            Spacer(modifier = Modifier.width(if (isSmallScreen) 8.dp else 16.dp))
            Text("Information", fontSize = if (isSmallScreen) 14.sp else 16.sp, color = Color.Black)
            Spacer(modifier = Modifier.weight(1f))
            Icon(painterResource(id = android.R.drawable.ic_menu_more), contentDescription = "More", tint = Color.Black)
        }
        Spacer(modifier = Modifier.height(if (isSmallScreen) 10.dp else 15.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(id = android.R.drawable.ic_menu_revert), contentDescription = "Log out", tint = Color(0xFF3F51B5))
            Spacer(modifier = Modifier.width(if (isSmallScreen) 8.dp else 16.dp))
            Text("Log out", fontSize = if (isSmallScreen) 14.sp else 16.sp, color = Color.Black, modifier = Modifier.clickable {
                auth.signOut()
                navController.navigate("login") { popUpTo("login") { inclusive = true } }
            })
            Spacer(modifier = Modifier.weight(1f))
            Icon(painterResource(id = android.R.drawable.ic_menu_more), contentDescription = "More", tint = Color.Black)
        }
        Spacer(modifier = Modifier.height(if (isSmallScreen) 10.dp else 15.dp))
        Text(
            buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color.Black)) { append("Powered by BUILD-") }
                withStyle(style = SpanStyle(color = Color(0xFF2E7D32))) { append("GREEN") }
            },
            fontSize = if (isSmallScreen) 10.sp else 12.sp
        )
    }
}

@Composable
fun ReportScreen(navController: NavController) {
    val auth = Firebase.auth
    val db = Firebase.firestore
    val storage = Firebase.storage
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isSmallScreen = screenWidth < 360.dp
    val titleOptions = listOf("Illegal Dumpsite", "Uncollected Wastes")
    var expanded by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        photoUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isSmallScreen) 8.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(painterResource(id = android.R.drawable.ic_menu_revert), contentDescription = "Back", tint = Color.Gray)
            }
            Text("Report Waste Issue", fontSize = if (isSmallScreen) 18.sp else 20.sp, color = Color.Black)
            Spacer(modifier = Modifier.width(if (isSmallScreen) 32.dp else 48.dp))
        }
        Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 16.dp))

        Text("Title", fontSize = if (isSmallScreen) 14.sp else 16.sp, color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(start = if (isSmallScreen) 8.dp else 16.dp))
        Spacer(modifier = Modifier.height(if (isSmallScreen) 4.dp else 8.dp))
        Box {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Select Title...") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                trailingIcon = {
                    Icon(painterResource(id = android.R.drawable.arrow_down_float), contentDescription = "Dropdown Icon", Modifier.clickable { expanded = !expanded })
                },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Gray, unfocusedBorderColor = Color.Gray)
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth()) {
                titleOptions.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { title = option; expanded = false })
                }
            }
        }
        Spacer(modifier = Modifier.height(if (isSmallScreen) 4.dp else 8.dp))

        Text("Description", fontSize = if (isSmallScreen) 14.sp else 16.sp, color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(start = if (isSmallScreen) 8.dp else 16.dp))
        Spacer(modifier = Modifier.height(if (isSmallScreen) 4.dp else 8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Add description") },
            modifier = Modifier.fillMaxWidth().height(if (isSmallScreen) 100.dp else 120.dp),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Gray, unfocusedBorderColor = Color.Gray, focusedContainerColor = Color(0xFFE0F2E9), unfocusedContainerColor = Color(0xFFE0F2E9))
        )
        Spacer(modifier = Modifier.height(if (isSmallScreen) 4.dp else 8.dp))

        Text("Location (Hint: click map)", fontSize = if (isSmallScreen) 14.sp else 16.sp, color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(start = if (isSmallScreen) 8.dp else 16.dp))
        Spacer(modifier = Modifier.height(if (isSmallScreen) 4.dp else 8.dp))
        OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Enter location by coordinates on map...") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Gray, unfocusedBorderColor = Color.Gray))
        Spacer(modifier = Modifier.height(if (isSmallScreen) 4.dp else 8.dp))

        Text("Upload Photo", fontSize = if (isSmallScreen) 14.sp else 16.sp, color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(start = if (isSmallScreen) 8.dp else 16.dp))
        Spacer(modifier = Modifier.height(if (isSmallScreen) 4.dp else 8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isSmallScreen) 80.dp else 100.dp)
                .background(Color(0xFFE0F2E9), RoundedCornerShape(8.dp))
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(painterResource(id = android.R.drawable.ic_menu_gallery), contentDescription = "Add photo", tint = Color.Gray)
                Spacer(modifier = Modifier.height(if (isSmallScreen) 4.dp else 8.dp))
                Text("Add photo", color = Color.Gray)
                if (photoUri != null) Text("Photo selected", color = Color(0xFF2E7D32))
            }
        }
        Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 16.dp))

        if (isLoading) CircularProgressIndicator(modifier = Modifier.padding(10.dp))
        else {
            Button(
                onClick = {
                    if (title.isNotEmpty() && description.isNotEmpty() && location.isNotEmpty()) {
                        isLoading = true
                        errorMessage = ""
                        scope.launch {
                            try {
                                val imageRef = storage.reference.child("reports/${UUID.randomUUID()}.jpg")
                                val uploadTask = photoUri?.let { uri -> imageRef.putFile(uri) }
                                val imageUrl = if (uploadTask != null) uploadTask.await().storage.downloadUrl.await().toString() else ""
                                val report = hashMapOf(
                                    "title" to title,
                                    "description" to description,
                                    "location" to location,
                                    "email" to (auth.currentUser?.email ?: "anonymous"),
                                    "timestamp" to System.currentTimeMillis(),
                                    "imageUrl" to imageUrl
                                )
                                db.collection("reports").add(report).addOnSuccessListener {
                                    isLoading = false
                                    navController.navigate("dashboard") { popUpTo("report") { inclusive = true } }
                                }.addOnFailureListener { e ->
                                    isLoading = false
                                    errorMessage = e.message ?: "Failed to submit report"
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = e.message ?: "An error occurred"
                            }
                        }
                    } else errorMessage = "Please fill all fields"
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(if (isSmallScreen) 40.dp else 48.dp)
            ) { Text("Submit Report", color = Color.White) }
        }
        Spacer(modifier = Modifier.height(if (isSmallScreen) 4.dp else 8.dp))
        if (errorMessage.isNotEmpty()) Text(errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
        Spacer(modifier = Modifier.height(if (isSmallScreen) 4.dp else 8.dp))

        Text(
            buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color.Black)) { append("Powered by BUILD-") }
                withStyle(style = SpanStyle(color = Color(0xFF2E7D32))) { append("GREEN") }
            },
            fontSize = if (isSmallScreen) 10.sp else 12.sp
        )
    }
}

@Composable
fun EditProfileDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var newEmail by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val auth = Firebase.auth
    val currentUser = auth.currentUser

    LaunchedEffect(currentUser) {
        if (currentUser != null) println("Current User: UID=${currentUser.uid}, Email=${currentUser.email}, Verified=${currentUser.isEmailVerified}")
        else println("No current user")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column {
                OutlinedTextField(value = newEmail, onValueChange = { newEmail = it }, label = { Text("New Email (Current: ${currentUser?.email ?: "N/A"})") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = newPassword, onValueChange = { newPassword = it }, label = { Text("New Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = currentPassword, onValueChange = { currentPassword = it }, label = { Text("Current Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    scope.launch {
                        if (currentUser != null) {
                            try {
                                currentUser.sendEmailVerification().await()
                                errorMessage = "Test verification email sent to ${currentUser.email}."
                            } catch (e: Exception) {
                                errorMessage = "Failed to send test email: ${e.message}"
                                println("Email Send Error: ${e.message}")
                            }
                        }
                    }
                }) { Text("Send Test Verification") }
                if (errorMessage.isNotEmpty()) Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    if (currentUser != null) {
                        try {
                            val credential = EmailAuthProvider.getCredential(currentUser.email!!, currentPassword)
                            currentUser.reauthenticate(credential).await()
                            if (newEmail.isNotEmpty()) currentUser.verifyBeforeUpdateEmail(newEmail).await()
                            if (newPassword.isNotEmpty()) currentUser.updatePassword(newPassword).await()
                            auth.signOut()
                            onSave(newEmail, newPassword)
                            onDismiss()
                        } catch (e: Exception) {
                            errorMessage = "Update failed: ${e.message}"
                            println("Update Exception: ${e.message}")
                        }
                    } else errorMessage = "No user signed in"
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PlansScreen(navController: NavController) {
    val tips = listOf(
        "Reduce water usage while brushing your teeth - save up to 4 liters daily!" to "Great job, eco-warrior!",
        "Switch to reusable bags - help reduce plastic waste!" to "You’re making a difference!",
        "Turn off lights when not in use - conserve energy!" to "Every step counts!",
        "Plant a tree in your backyard - boost local wildlife!" to "You’re a planet hero!"
    )
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isSmallScreen = screenWidth < 360.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isSmallScreen) 8.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Eco Tips & Encouragement", fontSize = if (isSmallScreen) 20.sp else 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 16.dp))
        tips.forEach { (tip, encouragement) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (isSmallScreen) 4.dp else 8.dp),
                elevation = CardDefaults.cardElevation(if (isSmallScreen) 2.dp else 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(if (isSmallScreen) 8.dp else 16.dp)) {
                    Text(tip, fontSize = if (isSmallScreen) 14.sp else 16.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(if (isSmallScreen) 4.dp else 8.dp))
                    Text(encouragement, fontSize = if (isSmallScreen) 12.sp else 14.sp, color = Color(0xFF2E7D32), fontStyle = FontStyle.Italic)
                }
            }
        }
        Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 16.dp))
        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth(if (isSmallScreen) 0.6f else 0.5f).height(if (isSmallScreen) 40.dp else 48.dp)
        ) { Text("Back", color = Color.White) }
        Spacer(modifier = Modifier.height(if (isSmallScreen) 16.dp else 20.dp))
        Text(
            buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color.Black)) { append("Powered by BUILD-") }
                withStyle(style = SpanStyle(color = Color(0xFF2E7D32))) { append("GREEN") }
            },
            fontSize = if (isSmallScreen) 10.sp else 12.sp
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestStoragePermission() {
    val context = LocalContext.current
    val permissionState = rememberPermissionState(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

    LaunchedEffect(Unit) {
        permissionState.launchPermissionRequest()
    }
}