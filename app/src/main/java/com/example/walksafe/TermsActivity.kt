package com.example.walksafe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class TermsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply Language
        val langCode = applySavedLocale(this)
        val sharedPrefs = getSharedPreferences("WalkSafePrefs", Context.MODE_PRIVATE)
        val isDarkTheme = sharedPrefs.getBoolean("DARK_MODE", false)

        setContent {
            MaterialTheme(colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TermsScreen(
                        onBack = { finish() },
                        onAccept = {
                            // Save preference and Go to Login
                            sharedPrefs.edit().putBoolean("TERMS_ACCEPTED", true).apply()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        },
                        lang = langCode
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TermsScreen(onBack: () -> Unit, onAccept: () -> Unit, lang: String) {
        // State for Checkbox
        var isChecked by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(Translator.get("Terms & Conditions", lang)) },
                    navigationIcon = {
                        // Back button behaves normally (finishes activity)
                        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = Translator.get("TermsTitle", lang),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = Translator.get("TermsContent", lang),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Sticky Bottom Bar for Acceptance
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { isChecked = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Translator.get("IAgree", lang),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.clickable { isChecked = !isChecked }
                            )
                        }

                        Button(
                            onClick = onAccept,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            enabled = isChecked // Disabled until checked
                        ) {
                            Text(Translator.get("AcceptContinue", lang))
                        }
                    }
                }
            }
        }
    }
}