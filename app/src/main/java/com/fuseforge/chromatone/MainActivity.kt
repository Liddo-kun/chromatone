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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fuseforge.chromatone.ui.theme.*

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
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Surface
                ) { innerPadding ->
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
            .background(Surface)
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 20.dp, end = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CHROMATONE",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (isPlaying && remainingSeconds != null) {
                    Text(
                        text = formatTime(remainingSeconds ?: 0),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
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
                        tint = TextMuted
                    )
                }
            }

            // Sound grid
            val saved by viewModel.savedVolumesLive.observeAsState(emptyMap())
            SoundGrid(
                sources = sources,
                volumes = volumes,
                savedVolumes = saved,
                onToggle = { viewModel.toggleSound(it) },
                onVolumeChange = { source, volume -> viewModel.setVolume(source, volume) },
                onMoveToTop = { viewModel.moveToTop(it) },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Play/Pause button
            IconButton(
                onClick = { viewModel.togglePlayback() },
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPlaying) Accent.copy(alpha = 0.15f)
                        else SurfaceCard
                    )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isPlaying) Accent else TextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Timer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
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
                    steps = 31,
                    colors = SliderDefaults.colors(
                        thumbColor = Accent,
                        activeTrackColor = Accent,
                        inactiveTrackColor = TextMuted.copy(alpha = 0.3f),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    )
                )
                val seconds = remainingSeconds ?: ((sliderPosition * 15).toInt() * 60).takeIf { it > 0 }
                Text(
                    text = if (seconds != null && seconds > 0) formatTime(seconds) else "\u221E",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                containerColor = SurfaceElevated,
                titleContentColor = TextPrimary,
                textContentColor = TextSecondary,
                title = { Text("About ChromaTone") },
                text = { Text("ChromaTone is a minimal noise app. No data is collected. For privacy info, see the app listing.") },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) {
                        Text("OK", color = Accent)
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
    savedVolumes: Map<String, Float>,
    onToggle: (SoundSource) -> Unit,
    onVolumeChange: (SoundSource, Float) -> Unit,
    onMoveToTop: (SoundSource) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(sources, key = { it.key }) { source ->
            val isActive = volumes.containsKey(source)
            val volume = if (isActive) volumes[source]!! else (savedVolumes[source.key] ?: 0f)
            val tileShape = RoundedCornerShape(12.dp)

            // Active: source color at 20% over card surface; inactive: plain card surface
            val tileBg = if (isActive)
                source.color.copy(alpha = 0.20f).compositeOver(SurfaceCard)
            else
                SurfaceCard

            val borderColor = if (isActive)
                source.color.copy(alpha = 0.5f)
            else
                Color.Transparent

            Box(
                modifier = Modifier
                    .aspectRatio(2.5f)
                    .clip(tileShape)
                    .background(tileBg)
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = tileShape
                    )
                    .combinedClickable(
                        onClick = { onToggle(source) },
                        onLongClick = { onMoveToTop(source) }
                    )
            ) {
                // Background icon filling the tile
                Icon(
                    imageVector = source.icon,
                    contentDescription = null,
                    tint = source.color.copy(alpha = if (isActive) 0.6f else 0.25f),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { scaleX = 1.4f; scaleY = 1.4f }
                        .padding(4.dp)
                )
                // Translucent overlay with controls
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceCard.copy(alpha = 0.45f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = source.displayName.uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isActive) source.color else TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Slider(
                        value = volume,
                        onValueChange = { onVolumeChange(source, it) },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth().height(24.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = if (isActive) source.color else TextMuted,
                            activeTrackColor = if (isActive) source.color else TextMuted,
                            inactiveTrackColor = TextMuted.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    }
}
