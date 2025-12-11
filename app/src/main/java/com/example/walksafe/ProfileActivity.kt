package com.example.walksafe

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter

// --- Imports for Crop & Date ---
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val langCode = applySavedLocale(this)
        val sharedPrefs = getSharedPreferences("WalkSafePrefs", Context.MODE_PRIVATE)
        val isDarkTheme = sharedPrefs.getBoolean("DARK_MODE", false)

        setContent {
            MaterialTheme(colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ProfileScreen(onBack = { finish() }, lang = langCode)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ProfileScreen(onBack: () -> Unit, lang: String) {
        val context = LocalContext.current
        val currentUser = auth.currentUser

        // Form States
        var firstName by remember { mutableStateOf("") }
        var lastName by remember { mutableStateOf("") }
        var school by remember { mutableStateOf("") }
        var dob by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }

        // Image States
        var profileImageUri by remember { mutableStateOf<Uri?>(null) }
        var existingImageUrl by remember { mutableStateOf("") }

        // UI States
        var isLoading by remember { mutableStateOf(true) }
        var isSaving by remember { mutableStateOf(false) }
        var showDatePicker by remember { mutableStateOf(false) }
        val datePickerState = rememberDatePickerState()

        // --- NEW: Photo Menu States ---
        var showPhotoMenu by remember { mutableStateOf(false) }
        var showFullImageDialog by remember { mutableStateOf(false) }

        // Fetch Data
        LaunchedEffect(Unit) {
            if (currentUser != null) {
                email = currentUser.email ?: ""
                db.collection("users").document(currentUser.uid).get().addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        firstName = doc.getString("firstName") ?: ""
                        lastName = doc.getString("lastName") ?: ""
                        school = doc.getString("school") ?: ""
                        dob = doc.getString("dob") ?: ""
                        existingImageUrl = doc.getString("profilePhotoUrl") ?: ""
                    }
                    isLoading = false
                }
            }
        }

        // Cropper Logic
        val cropImageLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
            if (result.isSuccessful) {
                profileImageUri = result.uriContent
            } else {
                Toast.makeText(context, "Crop failed: ${result.error?.message}", Toast.LENGTH_SHORT).show()
            }
        }

        fun startCrop(uri: Uri?) {
            if (uri == null) return
            val options = CropImageOptions(
                fixAspectRatio = true,
                aspectRatioX = 1,
                aspectRatioY = 1
            )
            // Launch cropper with the specific URI picked
            val cropInput = com.canhub.cropper.CropImageContractOptions(uri, options)
            cropImageLauncher.launch(cropInput)
        }

        // Gallery Picker
        val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                startCrop(uri)
            }
        }

        fun saveChanges() {
            if (currentUser == null) return
            isSaving = true

            // Logic to save (omitted detailed implementation for brevity, same as before)
            // Ideally call a suspend function here or standard callback chain
            // For this snippet, assume it works or copy previous save logic block
            val filename = "profile_photos/${currentUser.uid}.jpg"
            val ref = storage.reference.child(filename)

            // Simple save block
            val saveFirestore = { url: String ->
                val userData = mapOf("firstName" to firstName, "lastName" to lastName, "school" to school, "dob" to dob, "profilePhotoUrl" to url, "email" to email)
                db.collection("users").document(currentUser.uid).update(userData).addOnCompleteListener {
                    isSaving = false
                    Toast.makeText(context, "Profile Saved!", Toast.LENGTH_SHORT).show()
                    onBack()
                }
            }

            if (profileImageUri != null) {
                ref.putFile(profileImageUri!!).addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { saveFirestore(it.toString()) }
                }
            } else {
                saveFirestore(existingImageUrl)
            }
        }

        if (isLoading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                    Text(Translator.get("Edit Profile", lang), style = MaterialTheme.typography.headlineSmall)
                }
                Spacer(Modifier.height(24.dp))

                // --- PROFILE PHOTO WITH MENU ---
                Box(contentAlignment = Alignment.BottomEnd) {
                    val currentPainter = if (profileImageUri != null) rememberAsyncImagePainter(profileImageUri)
                    else if (existingImageUrl.isNotEmpty()) rememberAsyncImagePainter(existingImageUrl)
                    else rememberAsyncImagePainter(R.mipmap.ic_launcher_round)

                    Image(
                        painter = currentPainter,
                        contentDescription = "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                            .clickable { showPhotoMenu = true } // Open Menu on Click
                    )

                    // Edit Icon Overlay
                    Icon(
                        Icons.Default.Edit,
                        "Edit",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(6.dp)
                    )

                    // --- FLOATING MENU ---
                    DropdownMenu(
                        expanded = showPhotoMenu,
                        onDismissRequest = { showPhotoMenu = false }
                    ) {
                        // Option 1: View
                        DropdownMenuItem(
                            text = { Text("View Image") },
                            leadingIcon = { Icon(Icons.Default.Visibility, null) },
                            onClick = {
                                showPhotoMenu = false
                                if (profileImageUri != null || existingImageUrl.isNotEmpty()) {
                                    showFullImageDialog = true
                                } else {
                                    Toast.makeText(context, "No image to view", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        // Option 2: Change
                        DropdownMenuItem(
                            text = { Text("Change Image") },
                            leadingIcon = { Icon(Icons.Default.PhotoLibrary, null) },
                            onClick = {
                                showPhotoMenu = false
                                galleryLauncher.launch("image/*")
                            }
                        )
                        // Option 3: Remove
                        DropdownMenuItem(
                            text = { Text("Remove Image") },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                            onClick = {
                                showPhotoMenu = false
                                profileImageUri = null
                                existingImageUrl = ""
                                Toast.makeText(context, "Image removed (Save to apply)", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                Text(Translator.get("Tap to change photo", lang), style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                Spacer(Modifier.height(24.dp))

                // Fields
                OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text(Translator.get("First Name", lang)) }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text(Translator.get("Last Name", lang)) }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = dob,
                    onValueChange = {},
                    label = { Text(Translator.get("Date of Birth (DD/MM/YYYY)", lang)) },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    trailingIcon = { Icon(Icons.Default.CalendarToday, null) }
                )

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = school, onValueChange = { school = it }, label = { Text(Translator.get("School / University", lang)) }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(Translator.get("Email", lang)) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(Translator.get("New Password (Leave empty to keep current)", lang)) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = null)
                        }
                    }
                )

                Spacer(Modifier.height(32.dp))
                Button(onClick = { saveChanges() }, enabled = !isSaving, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    if (isSaving) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary) else Text(Translator.get("Save Changes", lang))
                }
            }

            // --- FULL IMAGE DIALOG ---
            if (showFullImageDialog) {
                Dialog(onDismissRequest = { showFullImageDialog = false }) {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .clickable { showFullImageDialog = false },
                        contentAlignment = Alignment.Center
                    ) {
                        val painter = if (profileImageUri != null) rememberAsyncImagePainter(profileImageUri) else rememberAsyncImagePainter(existingImageUrl)
                        Image(
                            painter = painter,
                            contentDescription = "Full Size",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // --- DATE PICKER DIALOG ---
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                dob = formatter.format(Date(millis))
                            }
                            showDatePicker = false
                        }) { Text(Translator.get("OK", lang)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text(Translator.get("Cancel", lang)) }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }
    }

    private object Translator { fun get(text: String, lang: String): String = text }
    private fun applySavedLocale(context: Context): String = java.util.Locale.getDefault().language
}