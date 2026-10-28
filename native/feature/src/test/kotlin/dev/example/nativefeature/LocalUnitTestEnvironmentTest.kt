package dev.example.nativefeature

import dev.example.nativefeature.testing.MainDispatcherRule
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

/** Mainの差し替え・仮想時間・テスト間の分離が機能することを確認するJVM環境テスト。 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocalUnitTestEnvironmentTest {
    /** 各テストの開始・終了でMainを差し替え、スケジューラを共有する。 */
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Main上の待機を実時間なしで進め、idle→loading→readyの順に状態が変わることを確認する。 */
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

    /** 他のテストで進めた仮想時刻が残らず、新しいスケジューラから開始することを確認する。 */
    @Test
    fun eachTestStartsWithAnEmptyScheduler() = runTest {
        assertEquals(0L, testScheduler.currentTime)
        assertEquals(mainDispatcherRule.dispatcher.scheduler, testScheduler)
    }
}
