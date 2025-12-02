package com.example.walksafe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.recaptcha.Recaptcha
import com.google.android.gms.recaptcha.RecaptchaAction
import com.google.android.gms.recaptcha.RecaptchaHandle

class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private val RECAPTCHA_SITE_KEY = BuildConfig.RECAPTCHA_KEY

    // --- IMPORTANT: PASTE YOUR WEB CLIENT ID HERE ---
    private val WEB_CLIENT_ID = BuildConfig.WEB_CLIENT_ID


    // --- DEBUG SWITCH: Set to TRUE to skip Recaptcha and test Login ---
    private val DEBUG_BYPASS_RECAPTCHA = true

    private var recaptchaHandle: RecaptchaHandle? = null
    private var startupError: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            auth = FirebaseAuth.getInstance()
            // Only init recaptcha if we are NOT in debug mode
            if (!DEBUG_BYPASS_RECAPTCHA) {
                if (RECAPTCHA_SITE_KEY.isEmpty()) throw Exception("Recaptcha Key is empty.")
                initializeRecaptcha()
            }
        } catch (e: Exception) {
            Log.e("WalkSafe", "Startup Crash", e)
            startupError = e.message
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (startupError != null) {
                        ErrorScreen(startupError!!)
                    } else {
                        LoginScreen(
                            onLogin = { email, pass -> handleLogin(email, pass) },
                            onGoToSignUp = { startActivity(Intent(this, SignUpActivity::class.java)) },
                            onGoogleSignIn = { idToken -> firebaseAuthWithGoogle(idToken) },
                            googleSignInClientIntent = getGoogleSignInIntent()
                        )
                    }
                }
            }
        }
    }

    private fun initializeRecaptcha() {
        Recaptcha.getClient(this)
            .init(RECAPTCHA_SITE_KEY)
            .addOnSuccessListener { handle -> recaptchaHandle = handle }
            .addOnFailureListener { Log.e("Recaptcha", "Init failed: ${it.message}") }
    }

    private fun handleLogin(email: String, pass: String) {
        // --- FIXED: ADDED VALIDATION TO PREVENT CRASH ---
        if (email.isBlank() || pass.isBlank()) {
            showToast("Please enter both email and password")
            return
        }

        if (DEBUG_BYPASS_RECAPTCHA) {
            // DIRECTLY LOGIN - NO CHECK
            performFirebaseAuth(email, pass)
            return
        }

        // Standard flow
        val handle = recaptchaHandle
        if (handle == null) {
            initializeRecaptcha()
            Toast.makeText(this, "Security check initializing...", Toast.LENGTH_SHORT).show()
            return
        }

        Recaptcha.getClient(this)
            .execute(handle, RecaptchaAction("login"))
            .addOnSuccessListener { tokenResult ->
                if (!tokenResult.tokenResult.isNullOrEmpty()) {
                    performFirebaseAuth(email, pass)
                } else {
                    showToast("Security Check Failed.")
                }
            }
            .addOnFailureListener { showToast("Security Error: ${it.message}") }
    }

    private fun performFirebaseAuth(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    navigateToHome()
                } else {
                    // DETAILED ERROR LOGGING
                    Log.e("Auth", "Email Login Failed", task.exception)
                    showToast("Login Failed: ${task.exception?.message}")
                }
            }
    }

    private fun getGoogleSignInIntent(): android.content.Intent {
        // Must use requestIdToken with the WEB client ID
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(this, gso).signInIntent
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navigateToHome()
                } else {
                    Log.e("Auth", "Google Auth Failed", task.exception)
                    showToast("Auth Failed: ${task.exception?.message}")
                }
            }
    }

    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}

@Composable
fun ErrorScreen(errorMsg: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(64.dp))
        Text(errorMsg, color = Color.Red, textAlign = TextAlign.Center)
    }
}

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onGoToSignUp: () -> Unit,
    onGoogleSignIn: (String) -> Unit,
    googleSignInClientIntent: android.content.Intent
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                // If this step succeeds, Google Sign-In on the device worked.
                // The next step is sending the token to Firebase.
                account.idToken?.let { onGoogleSignIn(it) }
            } catch (e: ApiException) {
                Log.e("Auth", "Google API Error Code: ${e.statusCode}", e)
                Toast.makeText(context, "Google Sign-In Error: ${e.statusCode}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("WALKSAFE TO SCHOOL", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { onLogin(email, password) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Log In")
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = { googleLauncher.launch(googleSignInClientIntent) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Sign in with Google")
        }
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onGoToSignUp) {
            Text("Don't have an account? Sign Up")
        }
    }
}