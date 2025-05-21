package com.fuseforge.chromatone

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var viewModel: MainViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = MainViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has white noise selected`() {
        assertEquals(NoiseType.White, viewModel.selectedNoise.value)
    }

    @Test
    fun `selecting noise updates selected noise type`() {
        viewModel.selectNoise(NoiseType.Pink)
        assertEquals(NoiseType.Pink, viewModel.selectedNoise.value)
    }

    @Test
    fun `toggle noise changes playing state`() {
        assertFalse(viewModel.isPlaying.value ?: false)
        viewModel.toggleNoise()
        assertTrue(viewModel.isPlaying.value ?: false)
        viewModel.toggleNoise()
        assertFalse(viewModel.isPlaying.value ?: false)
    }

    @Test
    fun `setting timer updates timer minutes and seconds`() {
        assertNull(viewModel.timerMinutes.value)
        assertNull(viewModel.remainingSeconds.value)
        
        val minutes = 30
        viewModel.setTimer(minutes)
        assertEquals(minutes, viewModel.timerMinutes.value)
        assertEquals(minutes * 60, viewModel.remainingSeconds.value)
    }
}
