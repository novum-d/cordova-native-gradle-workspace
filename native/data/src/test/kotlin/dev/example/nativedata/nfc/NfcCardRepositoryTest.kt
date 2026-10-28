package dev.example.nativedata.nfc

import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** 業務カードに依存しない範囲読取と、対象・エラー文脈・排他の保持を検証する。 */
@OptIn(ExperimentalCoroutinesApi::class)
class NfcCardRepositoryTest {
    private val repositories = testNfcCardRepositoryFactory()

    /** ゴルフの固定項目以外も、必要なブロックとバイトだけ取得する。 */
    @Test
    fun readsAnArbitraryRangeWithoutBusinessFieldKnowledge() = runTest {
        val tag = FakeNfcTag()
        val repository = repositories.create(DetectedNfcTagHandle(tag), TestNfcFailures)
        val range = NfcMemoryRange(NfcMemoryPosition(5, 1), NfcMemoryPosition(6, 0))

        assertArrayEquals(byteArrayOf(21, 22, 23, 24), repository.read(NfcTagModel.ICODE_SLI, range).unwrap())
        assertEquals(listOf(5, 6), tag.requests.map { it.last().toInt() })
        assertEquals(1, tag.closeCount)
    }

    /** 業務Repositoryを経由しない呼び出しでも、Profileの容量を越える範囲は通信前に拒否する。 */
    @Test
    fun validatesRangesBeforeOpeningConnection() = runTest {
        val tag = FakeNfcTag()
        val repository = repositories.create(DetectedNfcTagHandle(tag), TestNfcFailures)
        val position = NfcMemoryPosition(28, 0)
        val range = NfcMemoryRange(position, position)
        val failure = repository.read(NfcTagModel.ICODE_SLI, range).unwrapError()

        assertEquals(FailureKind.RANGE, failure.kind)
        assertEquals(range, failure.range)
        assertEquals(0, tag.openCount)
    }

    /** 同じ対象でも毎回別Repositoryを作り、各呼出元の具体的な失敗をそのまま返す。 */
    @Test
    fun createsIndependentRepositoriesWithTheirOwnFailureFactories() = runTest {
        val target = DetectedNfcTagHandle(FakeNfcTag())
        val firstFailure = TestNfcFailure(FailureKind.EXPIRED)
        val secondFailure = TestNfcFailure(FailureKind.EXPIRED)
        val firstFactory = object : NfcReadFailureFactory<TestNfcFailure> by TestNfcFailures {
            override fun targetExpired() = firstFailure
        }
        val secondFactory = object : NfcReadFailureFactory<TestNfcFailure> by TestNfcFailures {
            override fun targetExpired() = secondFailure
        }
        val first = repositories.create(target, firstFactory)
        val second = repositories.create(target, secondFactory)
        target.invalidate()
        val position = NfcMemoryPosition(1, 0)
        val range = NfcMemoryRange(position, position)

        assertNotSame(first, second)
        assertSame(firstFailure, first.read(NfcTagModel.ICODE_SLI, range).unwrapError())
        assertSame(secondFailure, second.read(NfcTagModel.ICODE_SLI, range).unwrapError())
    }

    /** 別々の共通Repositoryも、同じ検出対象のMutexで接続を直列化する。 */
    @Test
    fun serializesDifferentRepositoriesBoundToTheSameTag() = runTest {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val tag = FakeNfcTag().apply {
            respond = { block ->
                started.countDown()
                check(release.await(2, TimeUnit.SECONDS)) { "The test must release the blocking NFC call" }
                byteArrayOf(0) + ByteArray(4) { (block * 4 + it).toByte() }
            }
        }
        val target = DetectedNfcTagHandle(tag)
        val first = repositories.create(target, TestNfcFailures)
        val second = repositories.create(target, TestNfcFailures)
        val position = NfcMemoryPosition(1, 0)
        val range = NfcMemoryRange(position, position)
        val firstRead = async { first.read(NfcTagModel.ICODE_SLI, range) }
        try {
            withContext(Dispatchers.IO) { assertTrue(started.await(2, TimeUnit.SECONDS)) }
            assertTrue(target.readMutex.isLocked)
            val secondRead = async { second.read(NfcTagModel.ICODE_SLI, range) }
            runCurrent()
            assertEquals(1, tag.openCount)
            release.countDown()

            assertArrayEquals(byteArrayOf(4), firstRead.await().unwrap())
            assertArrayEquals(byteArrayOf(4), secondRead.await().unwrap())
            assertEquals(2, tag.closeCount)
        } finally {
            release.countDown()
        }
    }
}
