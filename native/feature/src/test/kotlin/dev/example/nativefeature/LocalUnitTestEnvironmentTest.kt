package dev.example.nativefeature

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Smoke tests for the test environment, rather than the Activity or Compose UI. */
@OptIn(ExperimentalCoroutinesApi::class)
class LocalUnitTestEnvironmentTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun mainDispatcherUsesTheTestScheduler() = runTest {
        val state = MutableStateFlow("idle")
        launch(Dispatchers.Main) {
            state.value = "loading"
            delay(1_000)
            state.value = "ready"
        }

        assertEquals("idle", state.value)
        runCurrent()
        assertEquals("loading", state.value)
        advanceUntilIdle()
        assertEquals("ready", state.value)
        assertEquals(1_000L, testScheduler.currentTime)
    }

    @Test
    fun eachTestStartsWithAnEmptyScheduler() = runTest {
        assertEquals(0L, testScheduler.currentTime)
        assertEquals(mainDispatcherRule.dispatcher.scheduler, testScheduler)
    }
}
