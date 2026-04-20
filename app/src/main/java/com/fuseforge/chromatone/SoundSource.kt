package com.fuseforge.chromatone

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

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
        NoiseType.White -> Color(0xFFE0E0E0)
        NoiseType.Pink -> Color(0xFFFF8AB4)
        NoiseType.Brown -> Color(0xFFD4A574)
        NoiseType.Green -> Color(0xFF81C784)
        NoiseType.Blue -> Color(0xFF64B5F6)
        NoiseType.Violet -> Color(0xFFCE93D8)
    }

val NoiseType.icon: ImageVector
    get() = Icons.Filled.GraphicEq

sealed interface SoundSource {
    val key: String
    val displayName: String
    val color: Color
    val icon: ImageVector
}

data class GeneratedNoise(val noiseType: NoiseType) : SoundSource {
    override val key: String get() = "gen_${noiseType.name}"
    override val displayName: String get() = noiseType.displayName
    override val color: Color get() = noiseType.color
    override val icon: ImageVector get() = noiseType.icon
}

data class AmbientSound(
    override val key: String,
    override val displayName: String,
    override val color: Color,
    override val icon: ImageVector,
    val rawResId: Int
) : SoundSource

val ALL_SOUND_SOURCES: List<SoundSource> by lazy {
    NoiseType.values().map { GeneratedNoise(it) } + listOf(
        AmbientSound("rain", "Rain", Color(0xFF5C9CE6), Icons.Filled.WaterDrop, R.raw.rain),
        AmbientSound("rain_on_window", "Window Rain", Color(0xFF4DD0E1), Icons.Filled.Window, R.raw.rain_on_window),
        AmbientSound("thunders", "Thunder", Color(0xFF90A4AE), Icons.Filled.Bolt, R.raw.thunders),
        AmbientSound("ocean", "Ocean", Color(0xFF4DB6AC), Icons.Filled.Waves, R.raw.ocean),
        AmbientSound("creek", "Creek", Color(0xFF66BB6A), Icons.Filled.Water, R.raw.creek),
        AmbientSound("fire", "Fire", Color(0xFFFFB74D), Icons.Filled.LocalFireDepartment, R.raw.fire),
        AmbientSound("crickets", "Crickets", Color(0xFF9CCC65), Icons.Filled.Grass, R.raw.crickets),
        AmbientSound("toads", "Toads", Color(0xFFAED581), Icons.Filled.Eco, R.raw.toads),
        AmbientSound("whale1", "Whale 1", Color(0xFF448AFF), Icons.Filled.SetMeal, R.raw.whale1),
        AmbientSound("whale2", "Whale 2", Color(0xFF536DFE), Icons.Filled.SetMeal, R.raw.whale2),
        AmbientSound("fan", "Fan", Color(0xFF78909C), Icons.Filled.Cyclone, R.raw.fan),
        AmbientSound("air_conditioner", "AC", Color(0xFF90A4AE), Icons.Filled.AcUnit, R.raw.air_conditioner),
        AmbientSound("airplane", "Airplane", Color(0xFFB0BEC5), Icons.Filled.Flight, R.raw.airplane_main),
        AmbientSound("cat_purring", "Cat Purring", Color(0xFFFFCC80), Icons.Filled.Pets, R.raw.cat_purring),
        AmbientSound("brainwave_5hz", "5Hz Brainwave", Color(0xFF9575CD), Icons.Filled.Psychology, R.raw.brainwave_5hz),
        AmbientSound("lf_hum", "LF Hum", Color(0xFF8D6E63), Icons.Filled.SurroundSound, R.raw.lf_hum),
        AmbientSound("lf_hum_2", "LF Hum 2", Color(0xFFA1887F), Icons.Filled.SurroundSound, R.raw.lf_hum_2),
        AmbientSound("melody", "Melody", Color(0xFFF06292), Icons.Filled.MusicNote, R.raw.melody),
        AmbientSound("purring2", "Purring 2", Color(0xFFFF8A65), Icons.Filled.Pets, R.raw.purring2),
    )
}
