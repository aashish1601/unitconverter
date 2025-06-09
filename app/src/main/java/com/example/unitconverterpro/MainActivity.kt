package com.example.unitconverterpro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unitconverterpro.ui.theme.UnitConverterProTheme
import kotlinx.coroutines.delay
import java.util.Locale

class MainActivity : ComponentActivity() {

    // Load native library
    companion object {
        init {
            try {
                System.loadLibrary("unitconverter")
                Log.d("MainActivity", "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e("MainActivity", "Failed to load native library: ${e.message}")
            }
        }
    }

    // Native function declarations
    external fun convertLength(value: Double, fromUnit: String, toUnit: String): Double
    external fun convertTemperature(value: Double, fromUnit: String, toUnit: String): Double
    external fun convertWeight(value: Double, fromUnit: String, toUnit: String): Double

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UnitConverterProTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UnitConverterScreen()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun UnitConverterScreen() {
        // State variables
        var selectedTabIndex by remember { mutableIntStateOf(0) }
        var inputValue by remember { mutableStateOf("") }
        var fromUnit by remember { mutableStateOf("") }
        var toUnit by remember { mutableStateOf("") }
        var result by remember { mutableStateOf("") }
        var fromUnitExpanded by remember { mutableStateOf(false) }
        var toUnitExpanded by remember { mutableStateOf(false) }
        var isConverting by remember { mutableStateOf(false) }
        var showResult by remember { mutableStateOf(false) }
        var isListening by remember { mutableStateOf(false) }
        var hasAudioPermission by remember { mutableStateOf(false) }

        // Dark mode state
        val systemInDarkTheme = isSystemInDarkTheme()
        var isDarkMode by remember { mutableStateOf(systemInDarkTheme) }

        val context = LocalContext.current

        // Check audio permission on start
        LaunchedEffect(Unit) {
            hasAudioPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        }

        // Permission launcher
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasAudioPermission = isGranted
        }

        // Speech recognition launcher
        val speechLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            isListening = false
            if (result.resultCode == RESULT_OK) {
                val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                spokenText?.firstOrNull()?.let { text ->
                    // Extract numbers from spoken text
                    val numberRegex = Regex("""(\d+(?:\.\d+)?)""")
                    val matchResult = numberRegex.find(text)
                    matchResult?.let { match ->
                        inputValue = match.value
                        Log.d("MainActivity", "Voice input captured: $text -> ${match.value}")
                    }
                }
            }
        }

        val categories = listOf("Length", "Temperature", "Weight")
        val units = mapOf(
            "Length" to listOf("Meter", "Kilometer", "Inch", "Mile"),
            "Temperature" to listOf("Celsius", "Fahrenheit", "Kelvin"),
            "Weight" to listOf("Kilogram", "Pound", "Gram")
        )

        // Dynamic color scheme based on dark mode
        val primaryColor = if (isDarkMode) Color(0xFFBB86FC) else Color(0xFF6200EE)
        val secondaryColor = if (isDarkMode) Color(0xFF03DAC6) else Color(0xFF03DAC6)
        val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
        val onSurfaceColor = if (isDarkMode) Color.White else Color.Black

        val gradientBrush = if (isDarkMode) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF2D1B69),
                    Color(0xFF11998E)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF667eea),
                    Color(0xFF764ba2)
                )
            )
        }

        // Animation states
        val swapRotation by animateFloatAsState(
            targetValue = if (isConverting) 360f else 0f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            label = "swap_rotation"
        )

        val buttonScale by animateFloatAsState(
            targetValue = if (isConverting) 0.95f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "button_scale"
        )

        val resultAlpha by animateFloatAsState(
            targetValue = if (showResult) 1f else 0f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            label = "result_alpha"
        )

        val micAlpha by animateFloatAsState(
            targetValue = if (isListening) 0.7f else 1f,
            animationSpec = tween(durationMillis = 300),
            label = "mic_alpha"
        )

        val micScale by animateFloatAsState(
            targetValue = if (isListening) 1.2f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "mic_scale"
        )

        val resultScale by animateFloatAsState(
            targetValue = if (showResult) 1f else 0.8f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "result_scale"
        )

        // Reset units when category changes
        LaunchedEffect(selectedTabIndex) {
            val currentUnits = units[categories[selectedTabIndex]] ?: emptyList()
            fromUnit = if (currentUnits.isNotEmpty()) currentUnits[0] else ""
            toUnit = if (currentUnits.size > 1) currentUnits[1] else currentUnits.getOrElse(0) { "" }
            result = ""
            showResult = false
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
        ) {
            // Dark mode toggle button
            FloatingActionButton(
                onClick = { isDarkMode = !isDarkMode },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 60.dp, end = 16.dp),
                containerColor = primaryColor,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle dark mode"
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Title Card with animation
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(1000, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(1000))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp,end = 80.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = surfaceColor.copy(alpha = 0.95f)
                        )
                    ) {
                        Text(
                            text = "Unit Converter Pro",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        )
                    }
                }

                // Main Content Card with animation
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(1000, delayMillis = 200, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(1000, delayMillis = 200))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = surfaceColor.copy(alpha = 0.95f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Tab Row for categories
                            TabRow(
                                selectedTabIndex = selectedTabIndex,
                                containerColor = Color.Transparent,
                                indicator = { tabPositions ->
                                    if (selectedTabIndex < tabPositions.size) {
                                        TabRowDefaults.SecondaryIndicator(
                                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                            color = primaryColor
                                        )
                                    }
                                }
                            ) {
                                categories.forEachIndexed { index, category ->
                                    Tab(
                                        selected = selectedTabIndex == index,
                                        onClick = { selectedTabIndex = index },
                                        text = {
                                            Text(
                                                category,
                                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedTabIndex == index) primaryColor else onSurfaceColor.copy(alpha = 0.6f)
                                            )
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Input field with voice input
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = inputValue,
                                    onValueChange = { inputValue = it },
                                    label = { Text("Enter value", color = primaryColor) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryColor,
                                        unfocusedBorderColor = onSurfaceColor.copy(alpha = 0.5f),
                                        focusedTextColor = onSurfaceColor,
                                        unfocusedTextColor = onSurfaceColor
                                    )
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Voice input button
                                IconButton(
                                    onClick = {
                                        if (hasAudioPermission) {
                                            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                                                isListening = true
                                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a number")
                                                }
                                                speechLauncher.launch(intent)
                                            }
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(
                                            if (isListening) Color.Red.copy(alpha = 0.1f) else primaryColor.copy(alpha = 0.1f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .scale(micScale)
                                        .graphicsLayer(alpha = micAlpha)
                                ) {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = "Voice input",
                                        tint = if (isListening) Color.Red else primaryColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // From Unit Dropdown
                            ExposedDropdownMenuBox(
                                expanded = fromUnitExpanded,
                                onExpandedChange = { fromUnitExpanded = !fromUnitExpanded }
                            ) {
                                OutlinedTextField(
                                    value = fromUnit,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("From Unit", color = primaryColor) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromUnitExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryColor,
                                        unfocusedBorderColor = onSurfaceColor.copy(alpha = 0.5f),
                                        focusedTextColor = onSurfaceColor,
                                        unfocusedTextColor = onSurfaceColor
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = fromUnitExpanded,
                                    onDismissRequest = { fromUnitExpanded = false },
                                    modifier = Modifier.background(surfaceColor)
                                ) {
                                    units[categories[selectedTabIndex]]?.forEach { unit ->
                                        DropdownMenuItem(
                                            text = { Text(unit, color = onSurfaceColor) },
                                            onClick = {
                                                fromUnit = unit
                                                fromUnitExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Swap Button with rotation animation
                            IconButton(
                                onClick = {
                                    val temp = fromUnit
                                    fromUnit = toUnit
                                    toUnit = temp
                                },
                                modifier = Modifier
                                    .background(
                                        secondaryColor.copy(alpha = 0.1f),
                                        RoundedCornerShape(50)
                                    )
                                    .size(48.dp)
                                    .rotate(swapRotation)
                            ) {
                                Icon(
                                    Icons.Default.SwapVert,
                                    contentDescription = "Swap units",
                                    tint = primaryColor
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // To Unit Dropdown
                            ExposedDropdownMenuBox(
                                expanded = toUnitExpanded,
                                onExpandedChange = { toUnitExpanded = !toUnitExpanded }
                            ) {
                                OutlinedTextField(
                                    value = toUnit,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("To Unit", color = primaryColor) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toUnitExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryColor,
                                        unfocusedBorderColor = onSurfaceColor.copy(alpha = 0.5f),
                                        focusedTextColor = onSurfaceColor,
                                        unfocusedTextColor = onSurfaceColor
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = toUnitExpanded,
                                    onDismissRequest = { toUnitExpanded = false },
                                    modifier = Modifier.background(surfaceColor)
                                ) {
                                    units[categories[selectedTabIndex]]?.forEach { unit ->
                                        DropdownMenuItem(
                                            text = { Text(unit, color = onSurfaceColor) },
                                            onClick = {
                                                toUnit = unit
                                                toUnitExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Conversion logic with LaunchedEffect
                            LaunchedEffect(isConverting) {
                                if (isConverting) {
                                    delay(800) // Animation duration
                                    val value = inputValue.toDoubleOrNull()
                                    if (value != null && fromUnit.isNotEmpty() && toUnit.isNotEmpty()) {
                                        try {
                                            val convertedValue = when (categories[selectedTabIndex]) {
                                                "Length" -> convertLength(value, fromUnit, toUnit)
                                                "Temperature" -> convertTemperature(value, fromUnit, toUnit)
                                                "Weight" -> convertWeight(value, fromUnit, toUnit)
                                                else -> 0.0
                                            }
                                            result = "${String.format(Locale.getDefault(), "%.4f", value)} $fromUnit = ${String.format(Locale.getDefault(), "%.4f", convertedValue)} $toUnit"
                                            Log.d("MainActivity", "Conversion successful: $result")
                                        } catch (e: Exception) {
                                            result = "Conversion error occurred"
                                            Log.e("MainActivity", "Conversion error: ${e.message}")
                                        }
                                    }
                                    isConverting = false
                                    showResult = true
                                }
                            }

                            // Convert Button with animations
                            Button(
                                onClick = {
                                    val value = inputValue.toDoubleOrNull()
                                    if (value != null && fromUnit.isNotEmpty() && toUnit.isNotEmpty()) {
                                        isConverting = true
                                        showResult = false
                                    } else {
                                        result = "Please enter a valid number"
                                        showResult = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .scale(buttonScale),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor
                                ),
                                enabled = !isConverting
                            ) {
                                if (isConverting) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Converting...",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Text(
                                        "Convert",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Result Card with animations
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(4.dp, RoundedCornerShape(12.dp))
                                    .graphicsLayer(
                                        alpha = resultAlpha,
                                        scaleX = resultScale,
                                        scaleY = resultScale
                                    ),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (result.isNotEmpty() && !result.contains("Please") && !result.contains("error"))
                                        secondaryColor.copy(alpha = 0.1f) else onSurfaceColor.copy(alpha = 0.1f)
                                )
                            ) {
                                Text(
                                    text = if (result.isEmpty()) "Result will appear here" else result,
                                    fontSize = 16.sp,
                                    fontWeight = if (result.isNotEmpty() && !result.contains("Please") && !result.contains("error"))
                                        FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    color = if (result.contains("error") || result.contains("Please"))
                                        Color.Red else onSurfaceColor,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}