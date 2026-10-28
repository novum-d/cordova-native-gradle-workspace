package dev.example.nativedata.nfc.card

import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import dev.example.nativedata.err.nfc.NfcReadErrFactory
import dev.example.nativedata.nfc.detection.DetectedNfcTagHandle
import dev.example.nativedata.nfc.memory.NfcMemoryPosition
import dev.example.nativedata.nfc.memory.NfcMemoryRange
import dev.example.nativedata.nfc.profile.NfcTagModel
import dev.example.nativedata.nfc.testing.ErrKind
import dev.example.nativedata.nfc.testing.FakeNfcTag
import dev.example.nativedata.nfc.testing.TestNfcErr
import dev.example.nativedata.nfc.testing.TestNfcErrFactory
import dev.example.nativedata.nfc.testing.testNfcCardReaderFactory
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
class NfcCardReaderTest {
    private val readers = testNfcCardReaderFactory()

    /** ゴルフの固定項目以外も、必要なブロックとバイトだけ取得する。 */
    @Test
    fun readsAnArbitraryRangeWithoutBusinessFieldKnowledge() = runTest {
        val tag = FakeNfcTag()
        val cardReader = readers.create(DetectedNfcTagHandle(tag), TestNfcErrFactory)
        val range = NfcMemoryRange(NfcMemoryPosition(5, 1), NfcMemoryPosition(6, 0))

        assertArrayEquals(byteArrayOf(21, 22, 23, 24), cardReader.read(NfcTagModel.ICODE_SLI, range).unwrap())
        assertEquals(listOf(5, 6), tag.requests.map { it.last().toInt() })
        assertEquals(1, tag.closeCount)
    }

    /** 業務Repositoryを経由しない呼び出しでも、Profileの容量を越える範囲は通信前に拒否する。 */
    @Test
    fun validatesRangesBeforeOpeningConnection() = runTest {
        val tag = FakeNfcTag()
        val cardReader = readers.create(DetectedNfcTagHandle(tag), TestNfcErrFactory)
        val position = NfcMemoryPosition(28, 0)
        val range = NfcMemoryRange(position, position)
        val err = cardReader.read(NfcTagModel.ICODE_SLI, range).unwrapError()

        assertEquals(ErrKind.RANGE, err.kind)
        assertEquals(range, err.range)
        assertEquals(0, tag.openCount)
    }

    /** 同じ対象でも毎回別Readerを作り、各呼出元の具体的な失敗をそのまま返す。 */
    @Test
    fun createsIndependentRepositoriesWithTheirOwnErrFactories() = runTest {
        val target = DetectedNfcTagHandle(FakeNfcTag())
        val firstErr = TestNfcErr(ErrKind.EXPIRED)
        val secondErr = TestNfcErr(ErrKind.EXPIRED)
        val firstFactory = object : NfcReadErrFactory<TestNfcErr> by TestNfcErrFactory {
            override fun targetExpired() = firstErr
        }
        val secondFactory = object : NfcReadErrFactory<TestNfcErr> by TestNfcErrFactory {
            override fun targetExpired() = secondErr
        }
        val first = readers.create(target, firstFactory)
        val second = readers.create(target, secondFactory)
        target.invalidate()
        val position = NfcMemoryPosition(1, 0)
        val range = NfcMemoryRange(position, position)

        assertNotSame(first, second)
        assertSame(firstErr, first.read(NfcTagModel.ICODE_SLI, range).unwrapError())
        assertSame(secondErr, second.read(NfcTagModel.ICODE_SLI, range).unwrapError())
    }

    /** 別々の共通Readerも、同じ検出対象のMutexで接続を直列化する。 */
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
        val first = readers.create(target, TestNfcErrFactory)
        val second = readers.create(target, TestNfcErrFactory)
        val position = NfcMemoryPosition(1, 0)
        val range = NfcMemoryRange(position, position)
        val firstRead = async { first.read(NfcTagModel.ICODE_SLI, range) }
        runCatching {
            withContext(Dispatchers.IO) { assertTrue(started.await(2, TimeUnit.SECONDS)) }
            assertTrue(target.readMutex.isLocked)
            val secondRead = async { second.read(NfcTagModel.ICODE_SLI, range) }
            runCurrent()
            assertEquals(1, tag.openCount)
            release.countDown()

            assertArrayEquals(byteArrayOf(4), firstRead.await().unwrap())
            assertArrayEquals(byteArrayOf(4), secondRead.await().unwrap())
            assertEquals(2, tag.closeCount)
        }.also {
            release.countDown()
        }.getOrElse { throw it }
    }
}
