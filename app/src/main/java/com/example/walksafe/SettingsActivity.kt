package com.example.walksafe

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val langCode = applySavedLocale(this)
        val sharedPrefs = getSharedPreferences("WalkSafePrefs", Context.MODE_PRIVATE)
        val isDarkTheme = sharedPrefs.getBoolean("DARK_MODE", false)

        setContent {
            MaterialTheme(colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SettingsScreen(onBack = { finish() }, lang = langCode)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SettingsScreen(onBack: () -> Unit, lang: String) {
        val context = LocalContext.current
        val sharedPrefs = context.getSharedPreferences("WalkSafePrefs", Context.MODE_PRIVATE)

        // Settings State
        var isDarkTheme by remember { mutableStateOf(sharedPrefs.getBoolean("DARK_MODE", false)) }
        var useMetric by remember { mutableStateOf(sharedPrefs.getBoolean("USE_METRIC", true)) }
        var defaultAiOnline by remember { mutableStateOf(sharedPrefs.getBoolean("DEFAULT_AI_ONLINE", true)) }
        var saveOriginalPhotos by remember { mutableStateOf(sharedPrefs.getBoolean("SAVE_ORIGINAL_PHOTOS", false)) }

        var showDeleteDialog by remember { mutableStateOf(false) }
        var showLangDialog by remember { mutableStateOf(false) } // State for Language Dialog

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(Translator.get("Settings", lang)) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer, titleContentColor = MaterialTheme.colorScheme.primary)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // --- 1. GENERAL ---
                SettingsSectionTitle(Translator.get("General", lang))

                // Language Picker (Dialog Trigger)
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = Translator.get("Language", lang),
                    subtitle = Locale(lang).displayLanguage,
                    onClick = { showLangDialog = true }
                )

                // Theme
                SettingsSwitchItem(
                    icon = Icons.Default.DarkMode,
                    title = Translator.get("Dark Mode", lang),
                    checked = isDarkTheme,
                    onCheckedChange = {
                        isDarkTheme = it
                        sharedPrefs.edit().putBoolean("DARK_MODE", it).apply()
                        (context as? ComponentActivity)?.recreate()
                    }
                )

                // Units
                SettingsSwitchItem(
                    icon = Icons.Default.Straighten,
                    title = Translator.get("Use Metric (meters)", lang),
                    checked = useMetric,
                    onCheckedChange = {
                        useMetric = it
                        sharedPrefs.edit().putBoolean("USE_METRIC", it).apply()
                    }
                )

                Divider()

                // --- 2. AI & ANALYSIS ---
                SettingsSectionTitle(Translator.get("AI & Analysis", lang))

                // Default AI Mode
                SettingsSwitchItem(
                    icon = Icons.Default.CloudQueue,
                    title = Translator.get("Default to Online AI", lang),
                    checked = defaultAiOnline,
                    onCheckedChange = {
                        defaultAiOnline = it
                        sharedPrefs.edit().putBoolean("DEFAULT_AI_ONLINE", it).apply()
                    }
                )

                // Save to Gallery
                SettingsSwitchItem(
                    icon = Icons.Default.PhotoLibrary,
                    title = Translator.get("Save Original Photos", lang),
                    checked = saveOriginalPhotos,
                    onCheckedChange = {
                        saveOriginalPhotos = it
                        sharedPrefs.edit().putBoolean("SAVE_ORIGINAL_PHOTOS", it).apply()
                    }
                )

                Divider()

                // --- 3. ACCOUNT ---
                SettingsSectionTitle(Translator.get("Account", lang))

                SettingsItem(
                    icon = Icons.Default.Person,
                    title = Translator.get("Edit Profile", lang),
                    onClick = { context.startActivity(Intent(context, ProfileActivity::class.java)) }
                )

                SettingsItem(
                    icon = Icons.Default.DeleteForever,
                    title = Translator.get("Delete Account", lang),
                    color = MaterialTheme.colorScheme.error,
                    onClick = { showDeleteDialog = true }
                )

                Divider()

                // --- 4. SUPPORT ---
                SettingsSectionTitle("Support")

                SettingsItem(icon = Icons.Default.Info, title = "App Version", subtitle = "1.0.0 (Beta)", onClick = {})
                SettingsItem(
                    icon = Icons.Default.Policy,
                    title = Translator.get("Privacy Policy", lang),
                    onClick = {
                        context.startActivity(Intent(context, PrivacyActivity::class.java))
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Sign Out
                Button(
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(context, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                        finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Translator.get("Sign Out", lang))
                }
            }

            // Language Dialog
            if (showLangDialog) {
                AlertDialog(
                    onDismissRequest = { showLangDialog = false },
                    title = { Text(Translator.get("Language", lang)) },
                    text = {
                        Column {
                            val languages = mapOf(
                                "English" to "en",
                                "Español" to "es",
                                "Français" to "fr",
                                "Deutsch" to "de",
                                "Ελληνικά" to "el"
                            )
                            languages.forEach { (name, code) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            sharedPrefs.edit().putString("LANGUAGE", code).apply()
                                            applySavedLocale(context)
                                            (context as? ComponentActivity)?.recreate()
                                            showLangDialog = false
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (code == lang),
                                        onClick = null // Handled by Row click
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = name, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showLangDialog = false }) {
                            Text(Translator.get("Cancel", lang))
                        }
                    }
                )
            }

            // Delete Dialog
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text(Translator.get("Delete Account", lang)) },
                    text = { Text(Translator.get("Delete Warning", lang)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                deleteAccount(context)
                                showDeleteDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                        ) { Text("DELETE") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) { Text(Translator.get("Cancel", lang)) }
                    }
                )
            }
        }
    }

    private fun deleteAccount(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()

        if (user != null) {
            db.collection("users").document(user.uid).delete()
                .addOnSuccessListener {
                    user.delete().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Account Deleted", Toast.LENGTH_LONG).show()
                            val intent = Intent(context, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }
    }

    @Composable
    fun SettingsSectionTitle(title: String) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))
    }

    @Composable
    fun SettingsItem(icon: ImageVector, title: String, subtitle: String? = null, color: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
        Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, color = color)
                if (subtitle != null) Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }

    @Composable
    fun SettingsSwitchItem(icon: ImageVector, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }

    // Helpers copied locally for context scope
    private object Translator { fun get(text: String, lang: String): String = com.example.walksafe.Translator.get(text, lang) }
    private fun applySavedLocale(context: Context): String = com.example.walksafe.applySavedLocale(context)
}