package com.example.walksafe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val langCode = applySavedLocale(this)
        val sharedPrefs = getSharedPreferences("WalkSafePrefs", Context.MODE_PRIVATE)
        val isDarkTheme = sharedPrefs.getBoolean("DARK_MODE", false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {
            MaterialTheme(colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SignUpScreen(
                        onSignUpSubmitted = { fname, lname, school, email, pass -> registerUser(fname, lname, school, email, pass) },
                        onBackToLogin = { finish() },
                        lang = langCode
                    )
                }
            }
        }
    }

    private fun registerUser(fname: String, lname: String, school: String, email: String, pass: String) {
        auth.createUserWithEmailAndPassword(email, pass).addOnSuccessListener { authResult ->
            val userId = authResult.user?.uid
            if (userId != null) {
                val userMap = hashMapOf("firstName" to fname, "lastName" to lname, "school" to school, "email" to email, "uid" to userId, "createdAt" to System.currentTimeMillis())
                db.collection("users").document(userId).set(userMap)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Created!", Toast.LENGTH_LONG).show()
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent); finish()
                    }
            }
        }
    }
}

@Composable
fun SignUpScreen(
    onSignUpSubmitted: (String, String, String, String, String) -> Unit,
    onBackToLogin: () -> Unit,
    lang: String
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var schoolName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // --- Added: Visibility States ---
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(Translator.get("Create Account", lang), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 24.dp))

        OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text(Translator.get("First Name", lang)) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text(Translator.get("Last Name", lang)) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = schoolName, onValueChange = { schoolName = it }, label = { Text(Translator.get("School / University", lang)) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(Translator.get("Email Address", lang)) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))

        // --- Password with Toggle ---
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(Translator.get("Password", lang)) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)
        )

        Spacer(Modifier.height(8.dp))

        // --- Confirm Password with Toggle ---
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text(Translator.get("Confirm Password", lang)) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (confirmVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { confirmVisible = !confirmVisible }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
        )

        Spacer(Modifier.height(24.dp))
        Button(onClick = { if (password == confirmPassword) onSignUpSubmitted(firstName, lastName, schoolName, email, password) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text(Translator.get("Register", lang))
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onBackToLogin) { Text(Translator.get("Already have an account? Log In", lang)) }
    }
}