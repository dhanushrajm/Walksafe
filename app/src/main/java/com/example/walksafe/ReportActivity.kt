package com.example.walksafe

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.*
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

// --------------------------- Data Models ---------------------------
data class WalkSafeReport(
    val id: String = UUID.randomUUID().toString(),
    val imageUri: Uri,
    val analysis: String,
    val lat: Double,
    val lng: Double,
    val timestamp: Long = System.currentTimeMillis()
)

data class PrivacyStats(val faces: Int = 0, val plates: Int = 0)

// --------------------------- Activity ---------------------------
class ReportActivity : ComponentActivity() {

    // --- AZURE CREDENTIALS ---
    private val AZURE_CV_ENDPOINT = "https://YOUR_CV_RESOURCE.cognitiveservices.azure.com/"
    private val AZURE_CV_KEY = "YOUR_CV_KEY"
    private val AZURE_OAI_ENDPOINT = "https://YOUR_OAI_RESOURCE.openai.azure.com/"
    private val AZURE_OAI_KEY = "YOUR_OAI_KEY"
    private val AZURE_DEPLOYMENT_NAME = "gpt-4"
    private val API_VERSION = "2024-02-15-preview"

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    // Offline Interpreter
    private var sidewalkInterpreter: Interpreter? = null

    // Settings
    private var useMetricUnits = true
    private var saveToGallery = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            sidewalkInterpreter = Interpreter(loadModelFile("sidewalk_model.tflite"))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val langCode = applySavedLocale(this)
        val sharedPrefs = getSharedPreferences("WalkSafePrefs", Context.MODE_PRIVATE)
        val isDarkTheme = sharedPrefs.getBoolean("DARK_MODE", false)

        // Load settings
        useMetricUnits = sharedPrefs.getBoolean("USE_METRIC", true)
        saveToGallery = sharedPrefs.getBoolean("SAVE_ORIGINAL_PHOTOS", false)
        val defaultAiOnline = sharedPrefs.getBoolean("DEFAULT_AI_ONLINE", true)

        setContent {
            MaterialTheme(colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ReportWorkflowScreen(langCode, defaultAiOnline)
                }
            }
        }
    }

    private fun loadModelFile(modelName: String): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ReportWorkflowScreen(lang: String, defaultOnline: Boolean) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var showMenu by remember { mutableStateOf(false) }
        var userName by remember { mutableStateOf("User") }
        var userEmail by remember { mutableStateOf(auth.currentUser?.email ?: "") }
        var userPhotoUrl by remember { mutableStateOf("") }

        var useOnlineAi by remember { mutableStateOf(defaultOnline) }

        LaunchedEffect(Unit) {
            auth.currentUser?.let { user ->
                db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        userName = doc.getString("firstName") ?: "User"
                        userPhotoUrl = doc.getString("profilePhotoUrl") ?: ""
                    }
                }
            }
        }

        // Data States
        var currentStep by remember { mutableIntStateOf(1) }
        var currentImageUri by remember { mutableStateOf<Uri?>(null) }
        var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
        var currentAnalysis by remember { mutableStateOf("") }
        var currentLocation by remember { mutableStateOf<LatLng?>(null) }

        // --- FIXED: Map Dialog States ---
        var showMapDialog by remember { mutableStateOf(false) }
        var mapDialogLocation by remember { mutableStateOf(LatLng(0.0, 0.0)) }

        val reportsList = remember { mutableStateListOf<WalkSafeReport>() }

        // UI States
        var isProcessing by remember { mutableStateOf(false) }
        var isUploading by remember { mutableStateOf(false) }
        var isCsvUploading by remember { mutableStateOf(false) }

        // Launchers
        val tempUri = remember { mutableStateOf<Uri?>(null) }
        val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentImageUri = tempUri.value
                currentImageUri?.let { uri ->
                    val inputStream = context.contentResolver.openInputStream(uri)
                    currentBitmap = BitmapFactory.decodeStream(inputStream)
                    currentStep = 2
                }
            }
        }
        val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                currentImageUri = uri
                val inputStream = context.contentResolver.openInputStream(uri)
                currentBitmap = BitmapFactory.decodeStream(inputStream)
                currentStep = 2
            }
        }

        val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                fetchUserLocation(context) { loc ->
                    currentLocation = loc
                    mapDialogLocation = loc // Update dialog default as well
                }
            }
        }

        val resetWorkflow = {
            currentStep = 1; currentImageUri = null; currentBitmap = null; currentAnalysis = ""; currentLocation = null; isProcessing = false; showMapDialog = false
            Toast.makeText(context, "Reset", Toast.LENGTH_SHORT).show()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(Translator.get("New WalkSafe Report", lang)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer, titleContentColor = MaterialTheme.colorScheme.primary),
                    navigationIcon = {
                        Box {
                            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.Menu, "Menu") }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(text = { Text(Translator.get("Settings", lang)) }, leadingIcon = { Icon(Icons.Default.Settings, null) }, onClick = {
                                    showMenu = false
                                    startActivity(Intent(context, SettingsActivity::class.java))
                                })
                            }
                        }
                    },
                    actions = { IconButton(onClick = resetWorkflow) { Icon(Icons.Default.Refresh, "Reset") } }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {

                // --- STEP 1: CAPTURE ---
                if (currentStep >= 1) {
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(Translator.get("Step 1: Capture Sidewalk", lang), style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            if (currentImageUri == null) {
                                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                                    Button(onClick = {
                                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                            val tmp = File.createTempFile("walk", ".jpg", context.getExternalFilesDir(null)); tempUri.value = FileProvider.getUriForFile(context, "${context.packageName}.provider", tmp); takePictureLauncher.launch(tempUri.value!!)
                                        } else ActivityCompat.requestPermissions(context as ComponentActivity, arrayOf(Manifest.permission.CAMERA), 100)
                                    }) { Text(Translator.get("Camera", lang)) }
                                    OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) { Text(Translator.get("Gallery", lang)) }
                                }
                            } else {
                                Image(bitmap = currentBitmap!!.asImageBitmap(), contentDescription = null, modifier = Modifier.height(200.dp).fillMaxWidth(), contentScale = ContentScale.Crop)
                                TextButton(onClick = resetWorkflow) { Text(Translator.get("Retake Photo", lang)) }
                            }
                        }
                    }
                }

                // --- STEP 2: ANALYSIS ---
                if (currentStep >= 2 && currentBitmap != null) {
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(Translator.get("Step 2: AI Analysis", lang), style = MaterialTheme.typography.titleMedium)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column { Text(if(useOnlineAi) "Online (Azure)" else "Offline (DL)", fontWeight = FontWeight.Bold); Text(if(useOnlineAi) "Vision API + GPT-4" else "TFLite Segmentation", style = MaterialTheme.typography.labelSmall) }
                                Switch(checked = useOnlineAi, onCheckedChange = { useOnlineAi = it })
                            }

                            if (currentAnalysis.isEmpty()) {
                                Button(onClick = {
                                    isProcessing = true
                                    scope.launch {
                                        val (safeBitmap, privacyStats) = processPrecisePrivacy(currentBitmap!!)
                                        currentBitmap = safeBitmap

                                        val report = if (useOnlineAi) {
                                            analyzeWithAzureChain(safeBitmap, privacyStats)
                                        } else {
                                            analyzeSidewalkReal(safeBitmap, privacyStats)
                                        }

                                        currentAnalysis = report
                                        isProcessing = false
                                        if (!currentAnalysis.startsWith("Error")) currentStep = 3
                                    }
                                }, enabled = !isProcessing, modifier = Modifier.fillMaxWidth()) {
                                    if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("Analyze Image")
                                }
                            } else {
                                Text(currentAnalysis, modifier = Modifier.background(Color(0xFFEEEEEE)).padding(8.dp).fillMaxWidth())
                            }
                        }
                    }
                }

                // --- STEP 3: LOCATION ---
                if (currentStep >= 3) {
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Step 3: Location", style = MaterialTheme.typography.titleMedium)

                            if (currentLocation == null) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { fetchUserLocation(context) { loc -> currentLocation = loc } }, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.LocationOn, null); Spacer(Modifier.width(8.dp)); Text("Use GPS (Automatic)")
                                    }

                                    OutlinedButton(onClick = {
                                        // Initialize dialog location
                                        fetchUserLocation(context) { loc -> mapDialogLocation = loc; showMapDialog = true }
                                        // Fallback if no permission
                                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                            showMapDialog = true
                                        }
                                    }, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Map, null); Spacer(Modifier.width(8.dp)); Text("Pin on Map (Manual)")
                                    }
                                }
                            } else {
                                // CONFIRMED LOCATION
                                Text("Location Set:", fontWeight = FontWeight.Bold)

                                // Static Map Preview
                                Box(modifier = Modifier.height(150.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)).padding(vertical = 8.dp)) {
                                    val markerState = rememberMarkerState(position = currentLocation!!)
                                    val previewCamState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(currentLocation!!, 16f) }
                                    GoogleMap(
                                        modifier = Modifier.fillMaxSize(),
                                        cameraPositionState = previewCamState,
                                        uiSettings = MapUiSettings(zoomControlsEnabled = false, scrollGesturesEnabled = false, zoomGesturesEnabled = false, rotationGesturesEnabled = false, tiltGesturesEnabled = false)
                                    ) {
                                        Marker(state = markerState)
                                    }
                                }

                                Text("Lat: ${"%.4f".format(currentLocation!!.latitude)}, Lng: ${"%.4f".format(currentLocation!!.longitude)}")
                                Spacer(Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { currentLocation = null }, modifier = Modifier.weight(1f)) { Text("Change") }
                                    Button(onClick = {
                                        isUploading = true // reused for save state
                                        saveReportToCloud(currentBitmap!!, currentLocation!!, currentAnalysis) {
                                            isUploading = false
                                            reportsList.add(WalkSafeReport(imageUri = currentImageUri!!, analysis = currentAnalysis, lat = currentLocation!!.latitude, lng = currentLocation!!.longitude))
                                            Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()

                                            if (saveToGallery) saveImageToGallery(context, currentBitmap!!)

                                            resetWorkflow()
                                        }
                                    }, modifier = Modifier.weight(1f), enabled = !isUploading) { if(isUploading) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text("Save & Add") }
                                }
                            }
                        }
                    }
                }

                // --- STEP 4: SUMMARY ---
                if (reportsList.isNotEmpty()) {
                    Text("Collected Reports: ${reportsList.size}", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 8.dp))
                    Button(onClick = { val csvContent = generateCsvContent(reportsList); downloadCsvLocally(context, csvContent) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text(Translator.get("Download CSV", lang)) }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { isCsvUploading = true; val csvContent = generateCsvContent(reportsList); uploadCsvToFirebase(context, csvContent) { isCsvUploading = false } }, enabled = !isCsvUploading, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)), modifier = Modifier.fillMaxWidth()) { if(isCsvUploading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp)) else { Icon(Icons.Default.CloudUpload, null); Spacer(Modifier.width(8.dp)); Text("Upload CSV to Cloud") } }
                }
            }

            // --- MAP DIALOG ---
            if (showMapDialog) {
                Dialog(
                    onDismissRequest = { showMapDialog = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Pin Location", style = MaterialTheme.typography.titleLarge)
                                IconButton(onClick = { showMapDialog = false }) { Icon(Icons.Default.Close, "Close") }
                            }

                            // Interactive Map
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                val dialogCamState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(mapDialogLocation, 15f) }

                                GoogleMap(
                                    modifier = Modifier.fillMaxSize(),
                                    cameraPositionState = dialogCamState
                                )

                                // Center Target
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Pin",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp).align(Alignment.Center).padding(bottom = 24.dp)
                                )

                                LaunchedEffect(dialogCamState.isMoving) {
                                    if (!dialogCamState.isMoving) {
                                        mapDialogLocation = dialogCamState.position.target
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    currentLocation = mapDialogLocation
                                    showMapDialog = false
                                },
                                modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp)
                            ) {
                                Text("Confirm This Location")
                            }
                        }
                    }
                }
            }
        }
    }

    // --- HELPER FUNCTIONS ---

    private fun generateCsvContent(reports: List<WalkSafeReport>): String {
        val sb = StringBuilder().append("ID,Lat,Lng,Analysis\n")
        reports.forEach { sb.append("${it.id},${it.lat},${it.lng},\"${it.analysis.replace("\n", " | ")}\"\n") }
        return sb.toString()
    }

    private fun uploadCsvToFirebase(context: Context, csvContent: String, onComplete: () -> Unit) {
        val userId = auth.currentUser?.uid ?: "anon"
        val filename = "csv_reports/${userId}_${System.currentTimeMillis()}.csv"
        val ref = storage.reference.child(filename)
        ref.putBytes(csvContent.toByteArray()).addOnSuccessListener { Toast.makeText(context, "Uploaded!", Toast.LENGTH_LONG).show(); onComplete() }.addOnFailureListener { Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_LONG).show(); onComplete() }
    }

    private fun downloadCsvLocally(context: Context, csvContent: String) {
        val filename = "WalkSafe_${System.currentTimeMillis()}.csv"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cv = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, filename); put(MediaStore.MediaColumns.MIME_TYPE, "text/csv"); put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS) }
            context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)?.let { uri ->
                context.contentResolver.openOutputStream(uri)?.use { s -> s.write(csvContent.toByteArray()) }
                Toast.makeText(context, "Saved to Downloads!", Toast.LENGTH_LONG).show()
            }
        } else Toast.makeText(context, "CSV Export requires Android 10+", Toast.LENGTH_LONG).show()
    }

    private fun fetchUserLocation(context: Context, onLocationFound: (LatLng) -> Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            Toast.makeText(context, "Please enable GPS location", Toast.LENGTH_LONG).show()
            context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener { loc ->
                if (loc != null) onLocationFound(LatLng(loc.latitude, loc.longitude))
                else Toast.makeText(context, "GPS weak. Try manual pin.", Toast.LENGTH_SHORT).show()
            }
        } else {
            ActivityCompat.requestPermissions(context as ComponentActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
        }
    }

    private suspend fun processPrecisePrivacy(bitmap: Bitmap): Pair<Bitmap, PrivacyStats> = withContext(Dispatchers.Default) {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint().apply { color = android.graphics.Color.BLACK; style = Paint.Style.FILL }
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        var facesRedacted = 0; var platesRedacted = 0
        try {
            val faces = Tasks.await(FaceDetection.getClient(FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE).build()).process(inputImage))
            for (face in faces) { canvas.drawRect(face.boundingBox, paint); facesRedacted++ }
            val objects = Tasks.await(ObjectDetection.getClient(ObjectDetectorOptions.Builder().setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE).enableMultipleObjects().enableClassification().build()).process(inputImage))
            val vehicleBoxes = objects.filter { it.labels.any { l -> l.text.lowercase() in listOf("car","vehicle","truck","bus") } }.map { it.boundingBox }
            val textBlocks = Tasks.await(TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(inputImage)).textBlocks
            for (block in textBlocks) { for (line in block.lines) {
                if (vehicleBoxes.any { Rect.intersects(it, line.boundingBox!!) } || (line.text.length in 4..12 && line.text.any { it.isDigit() })) {
                    line.boundingBox?.let { it.inset(-5,-5); canvas.drawRect(it, paint); platesRedacted++ }
                }
            }}
        } catch (e: Exception) { e.printStackTrace() }
        Pair(mutableBitmap, PrivacyStats(facesRedacted, platesRedacted))
    }

    private suspend fun analyzeWithAzureChain(bitmap: Bitmap, privacyStats: PrivacyStats): String = withContext(Dispatchers.IO) {
        try {
            val visionUrl = URL("${AZURE_CV_ENDPOINT.trimEnd('/')}/computervision/imageanalysis:analyze?features=caption,denseCaptions&model-version=latest&language=en&api-version=2024-02-01")
            val visionConn = (visionUrl.openConnection() as HttpURLConnection).apply { requestMethod = "POST"; setRequestProperty("Content-Type", "application/octet-stream"); setRequestProperty("Ocp-Apim-Subscription-Key", AZURE_CV_KEY); doOutput = true }
            val baos = ByteArrayOutputStream(); bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos); val imageBytes = baos.toByteArray(); visionConn.outputStream.use { it.write(imageBytes) }
            val description = if (visionConn.responseCode == 200) { val json = JSONObject(visionConn.inputStream.bufferedReader().readText()); json.optJSONObject("captionResult")?.optString("text") ?: "No caption" } else "Vision API Failed: ${visionConn.responseCode}"

            val openAiUrl = URL("${AZURE_OAI_ENDPOINT.trimEnd('/')}/openai/deployments/$AZURE_DEPLOYMENT_NAME/chat/completions?api-version=$API_VERSION")
            val requestJson = JSONObject().put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", "Image Description: $description. Create a sidewalk safety report with headings: AI Notes, Issues, Est. Length/Breadth, Confidence."))).put("max_tokens", 800)
            val aiConn = (openAiUrl.openConnection() as HttpURLConnection).apply { requestMethod = "POST"; setRequestProperty("Content-Type", "application/json"); setRequestProperty("api-key", AZURE_OAI_KEY); doOutput = true }
            aiConn.outputStream.use { it.write(requestJson.toString().toByteArray()) }
            if (aiConn.responseCode == 200) {
                val content = JSONObject(aiConn.inputStream.bufferedReader().readText()).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                "=== Online Report (Azure) ===\n$content\nPrivacy: ${privacyStats.faces} faces redacted."
            } else "Azure OpenAI Failed: ${aiConn.responseCode}"
        } catch (e: Exception) { "Chain Error: ${e.message}" }
    }

    private suspend fun analyzeSidewalkReal(bitmap: Bitmap, privacyStats: PrivacyStats): String = withContext(Dispatchers.Default) {
        if (sidewalkInterpreter == null) return@withContext "Error: Offline AI model missing."
        try {
            val modelSize = 257
            val imageProcessor = ImageProcessor.Builder().add(ResizeOp(modelSize, modelSize, ResizeOp.ResizeMethod.BILINEAR)).add(CastOp(DataType.FLOAT32)).add(NormalizeOp(127.5f, 127.5f)).build()
            var tensorImage = TensorImage(DataType.FLOAT32); tensorImage.load(bitmap); tensorImage = imageProcessor.process(tensorImage)
            val outputBuffer = ByteBuffer.allocateDirect(1 * modelSize * modelSize * 21 * 4).order(ByteOrder.nativeOrder())
            sidewalkInterpreter?.run(tensorImage.buffer, outputBuffer)
            outputBuffer.rewind(); val pixels = FloatArray(modelSize * modelSize * 21); outputBuffer.asFloatBuffer().get(pixels)
            var roadPixels = 0; var indoorPixels = 0; val totalPixels = modelSize * modelSize
            for (i in 0 until totalPixels) { var maxVal = Float.NEGATIVE_INFINITY; var maxIdx = -1; for (c in 0 until 21) { val idx = i * 21 + c; if (pixels[idx] > maxVal) { maxVal = pixels[idx]; maxIdx = c } }; if (maxIdx == 0) roadPixels++; if (maxIdx in listOf(5, 9, 11, 18, 20)) indoorPixels++ }
            val ratio = roadPixels.toFloat() / totalPixels; val indoorRatio = indoorPixels.toFloat() / totalPixels; val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            // Unit conversion
            val unitName = if(useMetricUnits) "meters" else "feet"
            val multiplier = if(useMetricUnits) 1.0 else 3.28

            if (indoorRatio > 0.10f) return@withContext "1. AI Notes: Indoor detected.\n2. Issue: Invalid Env\n3. Confidence: 0%\n4. Privacy: ${privacyStats.faces} faces, ${privacyStats.plates} plates.\n5. Time: $timestamp"
            val estWidth = (if (ratio > 0f) (ratio * 3.0) else 0.0) * multiplier
            val estLen = (if (ratio > 0f) (ratio * 20.0) else 0.0) * multiplier
            val confidence = (ratio * 100).toInt().coerceIn(0, 99)
            val issue = if (ratio > 0.6f) "None" else "Obstruction"
            """
            === WalkSafe AI Report (Offline) ===
            1. AI Notes: Pixel segmentation complete.
            2. Identified Issue: $issue
            3. Est Length: ${String.format("%.1f", estLen)} $unitName
            4. Est Breadth: ${String.format("%.1f", estWidth)} $unitName
            5. AI Confidence: $confidence%
            6. Privacy: ${privacyStats.faces} faces, ${privacyStats.plates} plates
            7. Time: $timestamp
            """.trimIndent()
        } catch (e: Exception) { "Offline Analysis Error: ${e.message}" }
    }

    private fun saveReportToCloud(bitmap: Bitmap, loc: LatLng, analysis: String, onComplete: () -> Unit) {
        val ref = storage.reference.child("walksafe/${UUID.randomUUID()}.jpg")
        val baos = ByteArrayOutputStream(); bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos); val data = baos.toByteArray()
        ref.putBytes(data).addOnSuccessListener { t -> t.storage.downloadUrl.addOnSuccessListener { url ->
            db.collection("reports").add(hashMapOf("lat" to loc.latitude, "lng" to loc.longitude, "url" to url.toString(), "analysis" to analysis, "timestamp" to System.currentTimeMillis())).addOnCompleteListener { onComplete() }
        }}.addOnFailureListener { onComplete() }
    }

    private fun saveImageToGallery(context: Context, bitmap: Bitmap) {
        val filename = "WalkSafe_Original_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            }
            Toast.makeText(context, "Saved to Gallery", Toast.LENGTH_SHORT).show()
        }
    }

    private object Translator { fun get(text: String, lang: String): String = text }
    private fun applySavedLocale(context: Context): String = java.util.Locale.getDefault().language
}