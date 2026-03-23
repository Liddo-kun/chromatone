package com.fuseforge.chromatone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.fuseforge.chromatone.ui.theme.ChromaToneTheme

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

private fun formatTime(totalSeconds: Int): String {
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hrs, mins, secs)
}

@Composable
fun MainScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val volumes by viewModel.soundVolumes.observeAsState(emptyMap())
    val sources by viewModel.sourceOrder.observeAsState(ALL_SOUND_SOURCES)
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
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
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
                if (isPlaying && remainingSeconds != null) {
                    Text(
                        text = formatTime(remainingSeconds ?: 0),
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
            SoundGrid(
                sources = sources,
                volumes = volumes,
                onToggle = { viewModel.toggleSound(it) },
                onVolumeChange = { source, volume -> viewModel.setVolume(source, volume) },
                onMoveToTop = { viewModel.moveToTop(it) },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            IconButton(
                onClick = { viewModel.togglePlayback() },
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
                val seconds = remainingSeconds ?: ((sliderPosition * 15).toInt() * 60).takeIf { it > 0 }
                if (seconds != null && seconds > 0) {
                    Text(
                        text = formatTime(seconds),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                } else {
                    Text(
                        text = "\u221E",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                }
            }
        }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SoundGrid(
    sources: List<SoundSource>,
    volumes: Map<SoundSource, Float>,
    onToggle: (SoundSource) -> Unit,
    onVolumeChange: (SoundSource, Float) -> Unit,
    onMoveToTop: (SoundSource) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(sources, key = { it.key }) { source ->
            val volume = volumes[source] ?: 0f
            val isActive = volume > 0f
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .background(source.color)
                    .border(
                        width = if (isActive) 3.dp else 1.dp,
                        color = if (isActive) Color.Black else Color.Black.copy(alpha = 0.3f)
                    )
                    .combinedClickable(
                        onClick = { onToggle(source) },
                        onLongClick = { onMoveToTop(source) }
                    )
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = source.displayName.uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isActive) Color.Black else Color.Black.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = volume,
                        onValueChange = { onVolumeChange(source, it) },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Black,
                            activeTrackColor = Color.Black,
                            inactiveTrackColor = Color.Black.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    }
}
