package com.example.walksafe

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("WalkSafePrefs", Context.MODE_PRIVATE)
        val savedLang = sharedPrefs.getString("LANGUAGE", "en") ?: "en"
        setLocaleMain(this, savedLang)

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            val isDarkTheme = sharedPrefs.getBoolean("DARK_MODE", false)

            MaterialTheme(
                colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(currentUser = currentUser, lang = savedLang)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val sharedPrefs = getSharedPreferences("WalkSafePrefs", Context.MODE_PRIVATE)
        val savedLang = sharedPrefs.getString("LANGUAGE", "en") ?: "en"
        val isDarkTheme = sharedPrefs.getBoolean("DARK_MODE", false)
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        setLocaleMain(this, savedLang)

        setContent {
            MaterialTheme(colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HomeScreen(currentUser = currentUser, lang = savedLang)
                }
            }
        }
    }
}

fun setLocaleMain(context: Context, languageCode: String) {
    val locale = Locale(languageCode)
    Locale.setDefault(locale)
    val config = Configuration()
    config.setLocale(locale)
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(currentUser: com.google.firebase.auth.FirebaseUser, lang: String) {
    val context = LocalContext.current
    var userName by remember { mutableStateOf("User") }
    var userEmail by remember { mutableStateOf(currentUser.email ?: "") }
    var userPhotoUrl by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser.uid, showMenu) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(currentUser.uid).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                userName = document.getString("firstName") ?: "User"
                userPhotoUrl = document.getString("profilePhotoUrl") ?: ""
            } else {
                userName = currentUser.displayName?.split(" ")?.firstOrNull() ?: "User"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WalkSafe") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                navigationIcon = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            // User Info
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (userPhotoUrl.isNotEmpty()) {
                                        Image(
                                            painter = rememberAsyncImagePainter(userPhotoUrl),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(40.dp).clip(CircleShape)
                                        )
                                    } else {
                                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(text = userEmail, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }

                            Divider()

                            // --- NEW: MY REPORTS LINK ---
                            DropdownMenuItem(
                                text = { Text("My Reports") }, // You can add translation for this later
                                leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    context.startActivity(Intent(context, MyReportsActivity::class.java))
                                }
                            )

                            // SETTINGS LINK
                            DropdownMenuItem(
                                text = { Text(Translator.get("Settings", lang)) },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    context.startActivity(Intent(context, SettingsActivity::class.java))
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${Translator.get("Welcome", lang)}, $userName!",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = Translator.get("You are securely logged in.", lang),
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Start Safe Walk Button
            Button(
                onClick = {
                    context.startActivity(Intent(context, ReportActivity::class.java))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(Translator.get("Start Safe Walk", lang))
            }
        }
    }
}