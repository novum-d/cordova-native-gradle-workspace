package dev.example.nativefeature.logging

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import timber.log.Timber

/** Debug／Releaseの登録方針、重複登録防止、ホストのログ設定との共存を確認する。 */
class TimberLoggingTest {
    private var originalTrees = emptyList<Timber.Tree>()

    /** グローバルなTree設定を退避し、このテスト専用の状態にする。 */
    @Before
    fun saveTrees() {
        originalTrees = Timber.forest()
        Timber.uprootAll()
    }

    /** 成功・失敗にかかわらず、テスト開始前のTree設定へ戻す。 */
    @After
    fun restoreTrees() {
        Timber.uprootAll()
        Timber.plant(*originalTrees.toTypedArray())
    }

    /** ホストのTreeを保持し、繰り返し初期化してもDebugTreeを一つだけ登録する。 */
    @Test
    fun debugInitializationIsIdempotentAndPreservesHostTree() {
        val hostTree = HostTree()
        Timber.plant(hostTree)

        TimberLogging.initialize(debuggable = true)
        val initializedTrees = Timber.forest()
        TimberLogging.initialize(debuggable = true)

        assertEquals(initializedTrees, Timber.forest())
        assertTrue(Timber.forest().contains(hostTree))
        assertEquals(1, Timber.forest().count { it is Timber.DebugTree })
    }

    /** ホストがすでにDebugTreeを登録していた場合は、そのインスタンスを再利用する。 */
    @Test
    fun existingDebugTreeIsReused() {
        val debugTree = Timber.DebugTree()
        Timber.plant(debugTree)

        TimberLogging.initialize(debuggable = true)

        assertEquals(1, Timber.treeCount)
        assertSame(debugTree, Timber.forest().single())
    }

    /** Releaseの初期化ではログ出力用のTreeを追加しない。 */
    @Test
    fun releaseDoesNotRegisterAnyTree() {
        TimberLogging.initialize(debuggable = false)

        assertTrue(Timber.forest().isEmpty())
    }

    /** Releaseでも、ホストが明示したログ設定を解除・置換しない。 */
    @Test
    fun releasePreservesHostConfiguration() {
        val hostTree = HostTree()
        Timber.plant(hostTree)

        TimberLogging.initialize(debuggable = false)

        assertSame(hostTree, Timber.forest().single())
    }

    /** AndroidのLogを呼ばずに、ホストが独自Treeを持つ状態を再現する。 */
    private class HostTree : Timber.Tree() {
        /** この検証では登録状態だけを確認するため、ログを外部へ出力しない。 */
        override fun log(
            priority: Int,
            tag: String?,
            message: String,
            t: Throwable?,
        ) = Unit
    }
}
