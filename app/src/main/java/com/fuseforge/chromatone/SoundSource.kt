package com.fuseforge.chromatone

import androidx.compose.ui.graphics.Color

enum class NoiseType(val displayName: String) {
    White("White Noise"),
    Pink("Pink Noise"),
    Brown("Brown Noise"),
    Blue("Blue Noise"),
    Green("Green Noise"),
    Violet("Violet Noise")
}

val NoiseType.color: Color
    get() = when (this) {
        NoiseType.White -> Color(0xFFF5F5F5)
        NoiseType.Pink -> Color(0xFFFFC1E3)
        NoiseType.Brown -> Color(0xFFD7CCC8)
        NoiseType.Green -> Color(0xFFC8E6C9)
        NoiseType.Blue -> Color(0xFFBBDEFB)
        NoiseType.Violet -> Color(0xFFE1BEE7)
    }

sealed interface SoundSource {
    val key: String
    val displayName: String
    val color: Color
}

data class GeneratedNoise(val noiseType: NoiseType) : SoundSource {
    override val key: String get() = "gen_${noiseType.name}"
    override val displayName: String get() = noiseType.displayName
    override val color: Color get() = noiseType.color
}

data class AmbientSound(
    override val key: String,
    override val displayName: String,
    override val color: Color,
    val rawResId: Int
) : SoundSource

val ALL_SOUND_SOURCES: List<SoundSource> by lazy {
    NoiseType.values().map { GeneratedNoise(it) } + listOf(
        AmbientSound("rain", "Rain", Color(0xFF90CAF9), R.raw.rain),
        AmbientSound("rain_on_window", "Window Rain", Color(0xFF80DEEA), R.raw.rain_on_window),
        AmbientSound("thunders", "Thunder", Color(0xFFB0BEC5), R.raw.thunders),
        AmbientSound("ocean", "Ocean", Color(0xFF80CBC4), R.raw.ocean),
        AmbientSound("creek", "Creek", Color(0xFFA5D6A7), R.raw.creek),
        AmbientSound("fire", "Fire", Color(0xFFFFCC80), R.raw.fire),
        AmbientSound("crickets", "Crickets", Color(0xFFC5E1A5), R.raw.crickets),
        AmbientSound("toads", "Toads", Color(0xFFDCEDC8), R.raw.toads),
        AmbientSound("whale1", "Whale 1", Color(0xFF82B1FF), R.raw.whale1),
        AmbientSound("whale2", "Whale 2", Color(0xFF8C9EFF), R.raw.whale2),
        AmbientSound("fan", "Fan", Color(0xFFCFD8DC), R.raw.fan),
        AmbientSound("air_conditioner", "AC", Color(0xFFE0E0E0), R.raw.air_conditioner),
        AmbientSound("airplane", "Airplane", Color(0xFFEEEEEE), R.raw.airplane_main),
        AmbientSound("cat_purring", "Cat Purring", Color(0xFFFFE0B2), R.raw.cat_purring),
        AmbientSound("brainwave_5hz", "5Hz Brainwave", Color(0xFFB39DDB), R.raw.brainwave_5hz),
        AmbientSound("lf_hum", "LF Hum", Color(0xFFBCAAA4), R.raw.lf_hum),
        AmbientSound("lf_hum_2", "LF Hum 2", Color(0xFFA1887F), R.raw.lf_hum_2),
        AmbientSound("melody", "Melody", Color(0xFFF8BBD0), R.raw.melody),
        AmbientSound("purring2", "Purring 2", Color(0xFFFFCCBC), R.raw.purring2),
    )
}
