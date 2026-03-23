package com.fuseforge.chromatone

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PREFS_NAME = "chromatone"
private const val KEY_SOURCE_ORDER = "source_order"
private const val KEY_SAVED_VOLUMES = "saved_volumes"

class MainViewModel : ViewModel() {
    private val _soundVolumes = MutableLiveData<Map<SoundSource, Float>>(emptyMap())
    val soundVolumes: LiveData<Map<SoundSource, Float>> = _soundVolumes
    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> = _isPlaying
    private val _timerMinutes = MutableLiveData<Int?>(null)
    val timerMinutes: LiveData<Int?> = _timerMinutes
    private val _remainingSeconds = MutableLiveData<Int?>(null)
    val remainingSeconds: LiveData<Int?> = _remainingSeconds
    private val _sourceOrder = MutableLiveData(ALL_SOUND_SOURCES.toList())
    val sourceOrder: LiveData<List<SoundSource>> = _sourceOrder

    private var timerJob: Job? = null
    private var appContext: Context? = null
    private val savedVolumes = mutableMapOf<String, Float>()

    fun setAppContext(context: Context) {
        appContext = context.applicationContext
        loadSourceOrder()
        loadSavedVolumes()
    }

    // --- Persistence ---

    private fun prefs() = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun loadSourceOrder() {
        val keys = prefs()?.getString(KEY_SOURCE_ORDER, null) ?: return
        val byKey = ALL_SOUND_SOURCES.associateBy { it.key }
        val ordered = keys.split(",").mapNotNull { byKey[it] }
        val remaining = ALL_SOUND_SOURCES.filter { it !in ordered }
        _sourceOrder.value = ordered + remaining
    }

    private fun saveSourceOrder() {
        val keys = (_sourceOrder.value ?: return).joinToString(",") { it.key }
        prefs()?.edit()?.putString(KEY_SOURCE_ORDER, keys)?.apply()
    }

    private fun loadSavedVolumes() {
        val data = prefs()?.getString(KEY_SAVED_VOLUMES, null) ?: return
        for (entry in data.split(",")) {
            val parts = entry.split("=")
            if (parts.size == 2) {
                savedVolumes[parts[0]] = parts[1].toFloatOrNull() ?: 1f
            }
        }
    }

    private fun saveSavedVolumes() {
        val data = savedVolumes.entries.joinToString(",") { "${it.key}=${it.value}" }
        prefs()?.edit()?.putString(KEY_SAVED_VOLUMES, data)?.apply()
    }

    // --- Sound control ---

    fun toggleSound(source: SoundSource) {
        val current = _soundVolumes.value ?: emptyMap()
        if (current.containsKey(source)) {
            updateVolumes(source, 0f)
        } else {
            updateVolumes(source, savedVolumes[source.key] ?: 1f)
        }
    }

    fun setVolume(source: SoundSource, volume: Float) {
        if (volume > 0f) {
            savedVolumes[source.key] = volume
            saveSavedVolumes()
        }
        updateVolumes(source, volume)
    }

    fun moveToTop(source: SoundSource) {
        val current = _sourceOrder.value ?: return
        _sourceOrder.value = listOf(source) + current.filter { it != source }
        saveSourceOrder()
    }

    private fun updateVolumes(source: SoundSource, volume: Float) {
        val updated = (_soundVolumes.value?.toMutableMap() ?: mutableMapOf()).apply {
            if (volume > 0f) put(source, volume) else remove(source)
        }
        _soundVolumes.value = updated
        NoiseForegroundService.activeNoises = updated
        if (_isPlaying.value == true && source is AmbientSound) {
            val context = appContext ?: return
            val intent = Intent(context, NoiseForegroundService::class.java).apply {
                action = NoiseForegroundService.ACTION_SYNC
            }
            context.startService(intent)
        }
    }

    // --- Timer ---

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
        timerJob = viewModelScope.launch {
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

    // --- Playback ---

    fun playNoise() {
        val context = appContext ?: return
        NoiseForegroundService.activeNoises = _soundVolumes.value ?: emptyMap()
        val intent = Intent(context, NoiseForegroundService::class.java).apply {
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

    fun togglePlayback() {
        if (_isPlaying.value == true) stopNoise() else playNoise()
    }

    override fun onCleared() {
        super.onCleared()
        stopNoise()
        timerJob?.cancel()
    }
}
