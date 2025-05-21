package com.fuseforge.chromatone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fuseforge.chromatone.ui.theme.ChromaToneTheme
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
        enableEdgeToEdge()
        setContent {
            ChromaToneTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(mainViewModel, Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun NoiseGrid(
    selected: NoiseType,
    isPlaying: Boolean,
    onSelect: (NoiseType) -> Unit,
    modifier: Modifier = Modifier
) {
    val noiseTypes = NoiseType.values()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        val isLandscape = maxWidth > maxHeight
        val spacing = 12.dp
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            for (row in 0..1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    for (col in 0..2) {
                        val index = row * 3 + col
                        if (index < noiseTypes.size) {
                            val noise = noiseTypes[index]
                            val isSelected = noise == selected
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(if (isLandscape) 2f else 1.5f)
                                    .shadow(
                                        elevation = if (isSelected) 4.dp else 1.dp,
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) Color.Black else Color.Transparent,
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { onSelect(noise) },
                                color = noise.color,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Box(Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = noise.purpose,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.Black.copy(alpha = 0.87f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    if (isSelected && isPlaying) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = "Playing",
                                            tint = Color.Black,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val selectedNoise by viewModel.selectedNoise.observeAsState(NoiseType.White)
    val isPlaying by viewModel.isPlaying.observeAsState(false)
    val timerMinutes by viewModel.timerMinutes.observeAsState(null)
    val remainingSeconds by viewModel.remainingSeconds.observeAsState(null)
    val context = LocalContext.current

    var showInfoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.setAppContext(context)
    }

    Box(
        modifier = modifier
            .background(Color.White)
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isPlaying) 96.dp else 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Grid of noise types
            NoiseGrid(
                selected = selectedNoise,
                isPlaying = isPlaying,
                onSelect = { viewModel.selectNoise(it) },
                modifier = Modifier.weight(1f)
            )
            // Play/Pause button with countdown next to it
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                IconButton(
                    onClick = { viewModel.toggleNoise() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (isPlaying) {
                    val timerLabel = when {
                        timerMinutes == null || timerMinutes == 0 -> "∞"
                        remainingSeconds != null -> {
                            val hrs = remainingSeconds!! / 3600
                            val mins = (remainingSeconds!! % 3600) / 60
                            val secs = remainingSeconds!! % 60
                            String.format("%02d:%02d:%02d", hrs, mins, secs)
                        }
                        else -> "∞"
                    }
                    Text(
                        text = timerLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Black.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }

        // Timer slider (if playing)
        AnimatedVisibility(
            visible = isPlaying,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            var sliderPosition by remember { mutableStateOf((timerMinutes ?: 0) / 15f) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Slider(
                    value = sliderPosition,
                    onValueChange = {
                        sliderPosition = it
                        val minutes = (it * 15).toInt().coerceAtMost(480)
                        viewModel.setTimer(if (minutes == 0) null else minutes)
                    },
                    valueRange = 0f..32f,
                    steps = 31,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Info button (top right)
        IconButton(
            onClick = { showInfoDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Info",
                tint = Color.Black.copy(alpha = 0.7f)
            )
        }

        // Info dialog
        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = { Text("About ChromaTone") },
                text = {
                    Text(
                        "ChromaTone\n\nA privacy-first, minimal noise utility. No data collection. No tracking. No network calls.\n\nVersion 1.0.0",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

// Assign a distinct color to each noise type
val NoiseType.color: Color
    get() = when (this) {
        NoiseType.White -> Color(0xFFF5F5F5)
        NoiseType.Pink -> Color(0xFFFFC1E3)
        NoiseType.Brown -> Color(0xFFD7CCC8)
        NoiseType.Green -> Color(0xFFC8E6C9)
        NoiseType.Blue -> Color(0xFFBBDEFB)
        NoiseType.Violet -> Color(0xFFE1BEE7)
    }

// Data model for noise types
enum class NoiseType(val displayName: String, val purpose: String) {
    White("White Noise", "FOCUS"),
    Pink("Pink Noise", "RELAX"),
    Brown("Brown Noise", "SLEEP"),
    Blue("Blue Noise", "REST"),
    Green("Green Noise", "CREATE"),
    Violet("Violet Noise", "STUDY")
}

// ViewModel for main screen
class MainViewModel : ViewModel() {
    private val _selectedNoise = MutableLiveData(NoiseType.White)
    val selectedNoise: LiveData<NoiseType> = _selectedNoise
    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> = _isPlaying
    private val _timerMinutes = MutableLiveData<Int?>(null)
    val timerMinutes: LiveData<Int?> = _timerMinutes
    private val _remainingSeconds = MutableLiveData<Int?>(null)
    val remainingSeconds: LiveData<Int?> = _remainingSeconds
    private var timerJob: Job? = null
    private var appContext: Context? = null

    fun setAppContext(context: Context) {
        appContext = context.applicationContext
    }

    fun selectNoise(type: NoiseType) {
        _selectedNoise.value = type
        if (_isPlaying.value == true) {
            playNoise()
        }
    }

    fun setTimer(minutes: Int?) {
        _timerMinutes.value = minutes
        if (minutes == null || minutes == 0) {
            // Timer off (∞)
            timerJob?.cancel()
            timerJob = null
            _remainingSeconds.value = null
        } else {
            _remainingSeconds.value = minutes * 60
            if (_isPlaying.value == true) {
                // If playing, restart countdown immediately
                startTimer(fromSeconds = minutes * 60)
            }
        }
    }

    // Overload startTimer to allow starting from a specific value
    fun startTimer(fromSeconds: Int? = null) {
        val totalSeconds = fromSeconds ?: _timerMinutes.value?.times(60) ?: return
        timerJob?.cancel()
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            var seconds = totalSeconds
            _remainingSeconds.value = seconds
            while (seconds > 0 && _isPlaying.value == true) {
                delay(1000)
                seconds--
                _remainingSeconds.value = seconds
            }
            if (seconds == 0) {
                stopNoise()
                setTimer(null)
            }
        }
    }

    fun playNoise() {
        val context = appContext ?: return
        val type = _selectedNoise.value ?: NoiseType.White
        val intent = Intent(context, NoiseForegroundService::class.java).apply {
            putExtra(NoiseForegroundService.EXTRA_NOISE_TYPE, type.name)
            action = NoiseForegroundService.ACTION_PLAY
        }
        context.startService(intent)
        _isPlaying.value = true
        if (_timerMinutes.value != null) {
            startTimer()
        }
    }

    fun stopNoise() {
        val context = appContext ?: return
        val intent = Intent(context, NoiseForegroundService::class.java).apply {
            action = NoiseForegroundService.ACTION_STOP
        }
        context.startService(intent)
        _isPlaying.value = false
    }
    
    fun toggleNoise() {
        if (_isPlaying.value == true) stopNoise() else playNoise()
    }
    
    override fun onCleared() {
        super.onCleared()
        stopNoise()
        timerJob?.cancel()
    }
}