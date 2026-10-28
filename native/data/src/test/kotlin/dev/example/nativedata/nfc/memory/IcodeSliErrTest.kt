package dev.example.nativedata.nfc.memory

import com.github.michaelbull.result.unwrapError
import dev.example.nativedata.nfc.profile.icodesli.IcodeSliTagProfile
import dev.example.nativedata.nfc.testing.ErrKind
import dev.example.nativedata.nfc.testing.FakeNfcTag
import dev.example.nativedata.nfc.testing.TestNfcErrFactory
import dev.example.nativedata.nfc.testing.testNfcProfileResolver
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CancellationException

/** 不正応答・I/O・取消時に部分結果を返さず、接続を解放して原因を保持する。 */
class IcodeSliErrTest {
    private val dataSource = NfcMemoryDataSource(testNfcProfileResolver())
    private val profile = IcodeSliTagProfile.profile
    private val range = NfcMemoryRange(NfcMemoryPosition(1, 2), NfcMemoryPosition(4, 3))

    /** Flagsのない空応答と、データ不足・過剰の正常応答を拒否する。 */
    @Test
    fun rejectsEmptyShortAndOversizedResponses() = runTest {
        for (response in listOf(byteArrayOf(), byteArrayOf(0), ByteArray(4), ByteArray(6))) {
            val tag = FakeNfcTag().apply { respond = { response } }
            assertEquals(
                ErrKind.RESPONSE_FORMAT,
                dataSource.read(tag, profile, range, TestNfcErrFactory).unwrapError().kind,
            )
            assertEquals(1, tag.requests.size)
            assertEquals(1, tag.closeCount)
        }
    }

    /** Error flagがある応答では、符号なしのエラーコードを保持する。 */
    @Test
    fun returnsTagRejectionCodeInsteadOfMemoryBytes() = runTest {
        val tag = FakeNfcTag().apply { respond = { byteArrayOf(1, 0xA3.toByte()) } }
        val err = dataSource.read(tag, profile, range, TestNfcErrFactory).unwrapError()

        assertEquals(ErrKind.REJECTED, err.kind)
        assertEquals(0xA3, err.code)
        assertEquals(1, tag.closeCount)
    }

    /** エラーコード欠落や余分なバイトを正常なエラー応答と推測しない。 */
    @Test
    fun rejectsMalformedErrorResponses() = runTest {
        for (response in listOf(byteArrayOf(1), byteArrayOf(1, 2, 3))) {
            val tag = FakeNfcTag().apply { respond = { response } }
            assertEquals(
                ErrKind.RESPONSE_FORMAT,
                dataSource.read(tag, profile, range, TestNfcErrFactory).unwrapError().kind,
            )
            assertEquals(1, tag.closeCount)
        }
    }

    /** connect途中で失敗しても、取得した接続のcloseを実行する。 */
    @Test
    fun closesConnectionWhenConnectFails() = runTest {
        val cause = IOException("connect")
        val tag = FakeNfcTag().apply { connectErr = cause }
        val err = dataSource.read(tag, profile, range, TestNfcErrFactory).unwrapError()

        assertEquals(ErrKind.COMMUNICATION, err.kind)
        assertSame(cause, err.cause)
        assertEquals(0, tag.requests.size)
        assertEquals(1, tag.closeCount)
    }

    /** 途中の通信失敗後は次のブロックへ進まず、元のIOExceptionを維持する。 */
    @Test
    fun stopsAtFirstReadErr() = runTest {
        val cause = IOException("block 2")
        val tag = FakeNfcTag().apply {
            respond = { block -> if (block == 2) throw cause else ByteArray(5) }
        }
        val err = dataSource.read(tag, profile, range, TestNfcErrFactory).unwrapError()

        assertSame(cause, err.cause)
        assertEquals(listOf(1, 2), tag.requests.map { it.last().toInt() })
        assertEquals(1, tag.closeCount)
    }

    /** 読取成功後のclose失敗も、完了した成功として返さない。 */
    @Test
    fun reportsCloseErrAfterSuccessfulRead() = runTest {
        val cause = IOException("close")
        val tag = FakeNfcTag().apply { closeErr = cause }

        assertSame(cause, dataSource.read(tag, profile, range, TestNfcErrFactory).unwrapError().cause)
        assertEquals(1, tag.closeCount)
    }

    /** 読取失敗とclose失敗が重なった場合、先の失敗を返しclose原因をsuppressedへ残す。 */
    @Test
    fun preservesOriginalReadErrWhenCloseAlsoFails() = runTest {
        val readCause = IOException("read")
        val closeCause = IOException("close")
        val tag = FakeNfcTag().apply {
            respond = { throw readCause }
            closeErr = closeCause
        }

        assertSame(readCause, dataSource.read(tag, profile, range, TestNfcErrFactory).unwrapError().cause)
        assertSame(closeCause, readCause.suppressed.single())
        assertEquals(1, tag.closeCount)
    }

    /** 型付きの応答不正も、後続のclose失敗で通信エラーへ置き換えない。 */
    @Test
    fun preservesResponseErrWhenCloseAlsoFails() = runTest {
        val tag = FakeNfcTag().apply {
            respond = { byteArrayOf() }
            closeErr = IOException("close")
        }

        assertEquals(
            ErrKind.RESPONSE_FORMAT,
            dataSource.read(tag, profile, range, TestNfcErrFactory).unwrapError().kind,
        )
    }

    /** 先に返す読取失敗がある場合、close側の予期しない失敗でも置き換えない。 */
    @Test
    fun preservesReadErrWhenCloseThrowsUnexpectedException() = runTest {
        val cause = IOException("read")
        val closeCause = IllegalStateException("close")
        val tag = FakeNfcTag().apply {
            respond = { throw cause }
            closeErr = closeCause
        }

        assertSame(cause, dataSource.read(tag, profile, range, TestNfcErrFactory).unwrapError().cause)
        assertSame(closeCause, cause.suppressed.single())
    }

    /** キャンセルをErrへ取り込まず、close後に原因を保って伝播する。 */
    @Test
    fun propagatesCancellationAndClosesConnection() = runTest {
        val cause = CancellationException("cancel")
        val tag = FakeNfcTag().apply { respond = { throw cause } }
        val thrown = kotlin.runCatching { dataSource.read(tag, profile, range, TestNfcErrFactory) }.exceptionOrNull()

        assertTrue(thrown is CancellationException)
        // コルーチンのスタックトレース復元で例外が複製されても、元の原因を保持していること。
        assertTrue(generateSequence(thrown) { it.cause }.any { it === cause })
        assertEquals(1, tag.closeCount)
    }

    /** JVMのErrorを通信失敗へ変換しない。 */
    @Test
    fun propagatesFatalErrorAndClosesConnection() = runTest {
        val cause = AssertionError("fatal")
        val tag = FakeNfcTag().apply { respond = { throw cause } }
        val thrown = kotlin.runCatching { dataSource.read(tag, profile, range, TestNfcErrFactory) }.exceptionOrNull()

        assertTrue(thrown is AssertionError)
        // コルーチンのスタックトレース復元で例外が複製されても、元の原因を保持していること。
        assertTrue(generateSequence(thrown) { it.cause }.any { it === cause })
        assertEquals(1, tag.closeCount)
    }

    /** Androidのアクセス拒否をI/Oと区別する。 */
    @Test
    fun reportsAccessDenialAndClosesConnection() = runTest {
        val cause = SecurityException("stale handle")
        val tag = FakeNfcTag().apply { connectErr = cause }

        assertEquals(
            ErrKind.ACCESS_DENIED,
            dataSource.read(tag, profile, range, TestNfcErrFactory).unwrapError().kind,
        )
        assertEquals(1, tag.closeCount)
    }

    /** 現在のテストコルーチンを親に持つ読取Jobの取消を検知し、残りのREADと部分結果を返さない。 */
    @Test
    fun stopsBeforeNextBlockWhenJobIsCancelled() = runTest {
        val readingJob = Job(currentCoroutineContext()[Job])
        val tag = FakeNfcTag().apply {
            respond = {
                readingJob.cancel()
                ByteArray(5)
            }
        }
        val thrown = kotlin.runCatching {
            withContext(readingJob) { dataSource.read(tag, profile, range, TestNfcErrFactory) }
        }.exceptionOrNull()

        assertTrue(thrown is CancellationException)
        assertEquals(listOf(1), tag.requests.map { it.last().toInt() })
        assertEquals(1, tag.closeCount)
    }

    /** メタデータがNFC-Vでも接続が取得できなければ対象外として終える。 */
    @Test
    fun reportsUnavailableNfcVConnection() = runTest {
        val tag = FakeNfcTag().apply { connectionAvailable = false }

        assertEquals(ErrKind.UNSUPPORTED, dataSource.read(tag, profile, range, TestNfcErrFactory).unwrapError().kind)
        assertEquals(0, tag.closeCount)
    }
}
