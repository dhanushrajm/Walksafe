package com.example.walksafe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Added missing import
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.recaptcha.Recaptcha
import com.google.android.gms.recaptcha.RecaptchaAction
import com.google.android.gms.recaptcha.RecaptchaHandle
import java.util.Locale

class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private val RECAPTCHA_SITE_KEY = BuildConfig.RECAPTCHA_KEY
    private val WEB_CLIENT_ID = BuildConfig.WEB_CLIENT_ID
    private val DEBUG_BYPASS_RECAPTCHA = false
    private var recaptchaHandle: RecaptchaHandle? = null
    private var startupError: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val langCode = applySavedLocale(this)

        // --- FLOW CONTROL ---
        val sharedPrefs = getSharedPreferences("WalkSafePrefs", Context.MODE_PRIVATE)

        // 1. Check if Terms Accepted (If not, start Intro/Terms flow)
        val termsAccepted = sharedPrefs.getBoolean("TERMS_ACCEPTED", false)
        if (!termsAccepted) {
            // Note: IntroActivity will check if it's the FIRST RUN.
            // If it is, it shows Intro -> Terms -> Login.
            // If it's NOT (maybe user cleared data or crashed), it goes Intro -> Terms.
            // We launch Intro here to start the chain.
            startActivity(Intent(this, IntroActivity::class.java))
            finish()
            return
        }

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            auth = FirebaseAuth.getInstance()

            // 2. Auto Login (Only if terms accepted)
            if (auth.currentUser != null) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return
            }

            if (!DEBUG_BYPASS_RECAPTCHA) {
                if (RECAPTCHA_SITE_KEY.isNotEmpty()) initializeRecaptcha()
            }
        } catch (e: Exception) {
            Log.e("WalkSafe", "Startup Logic Error", e)
            startupError = e.message
        }

        setContent {
            var isDarkTheme by remember { mutableStateOf(sharedPrefs.getBoolean("DARK_MODE", false)) }

            MaterialTheme(colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (startupError != null) {
                        ErrorScreen(startupError!!)
                    } else {
                        LoginScreen(
                            onLogin = { email, pass -> handleLogin(email, pass) },
                            onGoToSignUp = { startActivity(Intent(this, SignUpActivity::class.java)) },
                            onGoogleSignIn = { idToken -> firebaseAuthWithGoogle(idToken) },
                            googleSignInClientIntent = getGoogleSignInIntent(),
                            isDarkTheme = isDarkTheme,
                            onThemeChanged = { isDark ->
                                isDarkTheme = isDark
                                sharedPrefs.edit().putBoolean("DARK_MODE", isDark).apply()
                            },
                            currentLang = langCode
                        )
                    }
                }
            }
        }
    }

    private fun initializeRecaptcha() {
        Recaptcha.getClient(this).init(RECAPTCHA_SITE_KEY).addOnSuccessListener { recaptchaHandle = it }
    }
    private fun handleLogin(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) { Toast.makeText(this, "Enter details", Toast.LENGTH_SHORT).show(); return }
        if (DEBUG_BYPASS_RECAPTCHA) { performFirebaseAuth(email, pass); return }
        val handle = recaptchaHandle
        if (handle == null) {
            Toast.makeText(this, "Recaptcha init... try again", Toast.LENGTH_SHORT).show()
            initializeRecaptcha()
            return
        }
        try {
            Recaptcha.getClient(this).execute(handle, RecaptchaAction("login"))
                .addOnSuccessListener { performFirebaseAuth(email, pass) }
                .addOnFailureListener {
                    Log.e("Recaptcha", "Fail: ${it.message}")
                    Toast.makeText(this, "Security Check Failed", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) { Log.e("Recaptcha", "Crash", e) }
    }
    private fun performFirebaseAuth(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass).addOnSuccessListener { navigateToHome() }.addOnFailureListener { Toast.makeText(this, "Login Failed: ${it.message}", Toast.LENGTH_LONG).show() }
    }
    private fun getGoogleSignInIntent() = GoogleSignIn.getClient(this, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(WEB_CLIENT_ID).requestEmail().build()).signInIntent
    private fun firebaseAuthWithGoogle(id: String) { auth.signInWithCredential(GoogleAuthProvider.getCredential(id, null)).addOnSuccessListener { navigateToHome() }.addOnFailureListener { Toast.makeText(this, "Google Auth Failed: ${it.message}", Toast.LENGTH_LONG).show() } }
    private fun navigateToHome() { startActivity(Intent(this, MainActivity::class.java)); finish() }
}

@Composable
fun ErrorScreen(errorMsg: String) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Error: $errorMsg", color = Color.Red)
    }
}

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onGoToSignUp: () -> Unit,
    onGoogleSignIn: (String) -> Unit,
    googleSignInClientIntent: android.content.Intent,
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    currentLang: String
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showLanguageMenu by remember { mutableStateOf(false) }
    val languages = mapOf("English" to "en", "Spanish" to "es", "French" to "fr", "German" to "de", "Greek" to "el")
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try { task.getResult(ApiException::class.java).idToken?.let { onGoogleSignIn(it) } } catch (e: ApiException) {}
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Box {
                IconButton(onClick = { showLanguageMenu = true }) { Icon(Icons.Default.Translate, "Lang") }
                DropdownMenu(expanded = showLanguageMenu, onDismissRequest = { showLanguageMenu = false }) {
                    languages.forEach { (name, code) ->
                        DropdownMenuItem(text = { Text(name) }, onClick = {
                            val prefs = context.getSharedPreferences("WalkSafePrefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("LANGUAGE", code).apply()
                            applySavedLocale(context)
                            (context as? ComponentActivity)?.recreate()
                        })
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = isDarkTheme, onCheckedChange = onThemeChanged)
        }
        Spacer(Modifier.height(48.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(Translator.get("WALKSAFE TO SCHOOL", currentLang), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            Text(Translator.get("Welcome Back", currentLang), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(Translator.get("Email", currentLang)) }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Email, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text(Translator.get("Password", currentLang)) }, modifier = Modifier.fillMaxWidth(), visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null) } }, leadingIcon = { Icon(Icons.Default.Lock, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done))
            Spacer(Modifier.height(24.dp))
            Button(onClick = { onLogin(email, password) }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text(Translator.get("Log In", currentLang)) }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { googleLauncher.launch(googleSignInClientIntent) }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text(Translator.get("Sign in with Google", currentLang)) }
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = onGoToSignUp) { Text(Translator.get("Don't have an account? Sign Up", currentLang)) }
            Spacer(Modifier.height(8.dp))
            Text(text = Translator.get("TermsLink", currentLang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline, modifier = Modifier.clickable { context.startActivity(Intent(context, TermsActivity::class.java)) })
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(Translator.get("Protected by reCAPTCHA Enterprise", currentLang), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}