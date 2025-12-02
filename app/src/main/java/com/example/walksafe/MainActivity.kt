package com.example.walksafe

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // Security Check: If user is not logged in, send them to LoginActivity
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(
                        currentUser = currentUser,
                        onSignOut = {
                            auth.signOut()
                            val intent = Intent(this, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(currentUser: com.google.firebase.auth.FirebaseUser, onSignOut: () -> Unit) {
    // State to hold the name. Default to "User" initially.
    var userName by remember { mutableStateOf("User") }

    // Fetch detailed profile from Firestore when the screen loads
    LaunchedEffect(currentUser.uid) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // CASE 1: User signed up with Email/Password (Data is in Firestore)
                    val firstName = document.getString("firstName")
                    if (!firstName.isNullOrEmpty()) {
                        userName = firstName
                    }
                } else {
                    // CASE 2: User signed in with Google (Data is in Auth Profile)
                    val googleName = currentUser.displayName
                    if (!googleName.isNullOrEmpty()) {
                        // Use the first part of their name (e.g., "John" from "John Doe")
                        userName = googleName.split(" ").firstOrNull() ?: googleName
                    }
                }
            }
            .addOnFailureListener {
                // If fetch fails, keep default "User" or try Auth display name
                userName = currentUser.displayName ?: "User"
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WalkSafe") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // --- UPDATED TEXT ---
            Text(
                text = "Welcome, $userName!",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "You are securely logged in.",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { /* TODO: Implement Start Walk */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Start Safe Walk")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign Out")
            }
        }
    }
}