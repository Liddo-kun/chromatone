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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun MainScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val selectedNoise by viewModel.selectedNoise.observeAsState(NoiseType.White)
    val isPlaying by viewModel.isPlaying.observeAsState(false)
    val timerMinutes by viewModel.timerMinutes.observeAsState(null)
    val remainingSeconds by viewModel.remainingSeconds.observeAsState(null)
    val context = LocalContext.current
    var showInfoDialog by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableStateOf((timerMinutes ?: 0) / 15f) }

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
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Top bar with app title and info button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ChromaTone",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                // Countdown timer (if active) on the top right, before the info button
                if (isPlaying && remainingSeconds != null) {
                    val hrs = (remainingSeconds ?: 0) / 3600
                    val mins = ((remainingSeconds ?: 0) % 3600) / 60
                    val secs = (remainingSeconds ?: 0) % 60
                    Text(
                        text = String.format("%02d:%02d:%02d", hrs, mins, secs),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                IconButton(
                    onClick = { showInfoDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = Color.Black
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Grid of noise types (with black outline)
            NoiseGrid(
                selected = selectedNoise,
                isPlaying = isPlaying,
                onSelect = { viewModel.selectNoise(it) },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Play/Pause button (centered, above seek bar)
            IconButton(
                onClick = { viewModel.toggleNoise() },
                modifier = Modifier
                    .size(56.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Timer slider (seek bar) always visible, below play/pause
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Slider(
                    value = (remainingSeconds?.div(60f)?.div(15f)) ?: sliderPosition,
                    onValueChange = {
                        sliderPosition = it
                        val minutes = (it * 15).toInt().coerceAtMost(480)
                        viewModel.setTimer(minutes)
                        if (minutes > 0 && isPlaying) {
                            viewModel.startTimer()
                        }
                    },
                    valueRange = 0f..32f,
                    steps = 31
                )
                // Show countdown in HH:MM:SS if timer is set, else show "∞"
                val seconds = remainingSeconds ?: ((sliderPosition * 15).toInt() * 60).takeIf { it > 0 }
                if (seconds != null && seconds > 0) {
                    val hrs = seconds / 3600
                    val mins = (seconds % 3600) / 60
                    val secs = seconds % 60
                    Text(
                        text = String.format("%02d:%02d:%02d", hrs, mins, secs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                } else {
                    Text(
                        text = "∞",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                }
            }
        }
        // Minimal info dialog
        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = { Text("About ChromaTone") },
                text = { Text("ChromaTone is a minimal noise app. No data is collected. For privacy info, see the app listing.") },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

// Minimal NoiseGrid (with black outline for color grid, no shadow or decorative elements)
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
            .padding(horizontal = 8.dp, vertical = 0.dp)
    ) {
        val isLandscape = maxWidth > maxHeight
        val spacing = 8.dp
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
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(if (isLandscape) 2f else 1.5f)
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .background(noise.color)
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) Color.Black else Color.Black.copy(alpha = 0.3f)
                                    )
                                    .clickable { onSelect(noise) },
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = noise.purpose,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Black,
                                        textAlign = TextAlign.Center
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
        _remainingSeconds.value = if (minutes != null) minutes * 60 else null
        if (minutes == null) {
            timerJob?.cancel()
            timerJob = null
        }
    }

    fun startTimer() {
        val totalSeconds = _timerMinutes.value?.times(60) ?: return
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