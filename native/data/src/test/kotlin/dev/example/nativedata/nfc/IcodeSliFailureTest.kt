package dev.example.nativedata.nfc

import com.github.michaelbull.result.unwrapError
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CancellationException

/** 不正応答・I/O・取消時に部分結果を返さず、接続を解放して原因を保持する。 */
class IcodeSliFailureTest {
    private val reader = NfcMemoryReader()
    private val profile = NfcTagProfiles.icodeSli
    private val range = NfcMemoryRange(NfcMemoryPosition(1, 2), NfcMemoryPosition(4, 3))

    /** Flagsのない空応答と、データ不足・過剰の正常応答を拒否する。 */
    @Test
    fun rejectsEmptyShortAndOversizedResponses() = runTest {
        for (response in listOf(byteArrayOf(), byteArrayOf(0), ByteArray(4), ByteArray(6))) {
            val tag = FakeNfcTag().apply { respond = { response } }
            assertEquals(
                FailureKind.RESPONSE_FORMAT,
                reader.read(tag, profile, range, TestNfcFailures).unwrapError().kind,
            )
            assertEquals(1, tag.requests.size)
            assertEquals(1, tag.closeCount)
        }
    }

    /** Error flagがある応答では、符号なしのエラーコードを保持する。 */
    @Test
    fun returnsTagRejectionCodeInsteadOfMemoryBytes() = runTest {
        val tag = FakeNfcTag().apply { respond = { byteArrayOf(1, 0xA3.toByte()) } }
        val failure = reader.read(tag, profile, range, TestNfcFailures).unwrapError()

        assertEquals(FailureKind.REJECTED, failure.kind)
        assertEquals(0xA3, failure.code)
        assertEquals(1, tag.closeCount)
    }

    /** エラーコード欠落や余分なバイトを正常なエラー応答と推測しない。 */
    @Test
    fun rejectsMalformedErrorResponses() = runTest {
        for (response in listOf(byteArrayOf(1), byteArrayOf(1, 2, 3))) {
            val tag = FakeNfcTag().apply { respond = { response } }
            assertEquals(
                FailureKind.RESPONSE_FORMAT,
                reader.read(tag, profile, range, TestNfcFailures).unwrapError().kind,
            )
            assertEquals(1, tag.closeCount)
        }
    }

    /** connect途中で失敗しても、取得した接続のcloseを実行する。 */
    @Test
    fun closesConnectionWhenConnectFails() = runTest {
        val cause = IOException("connect")
        val tag = FakeNfcTag().apply { connectFailure = cause }
        val failure = reader.read(tag, profile, range, TestNfcFailures).unwrapError()

        assertEquals(FailureKind.COMMUNICATION, failure.kind)
        assertSame(cause, failure.cause)
        assertEquals(0, tag.requests.size)
        assertEquals(1, tag.closeCount)
    }

    /** 途中の通信失敗後は次のブロックへ進まず、元のIOExceptionを維持する。 */
    @Test
    fun stopsAtFirstReadFailure() = runTest {
        val cause = IOException("block 2")
        val tag = FakeNfcTag().apply {
            respond = { block -> if (block == 2) throw cause else ByteArray(5) }
        }
        val failure = reader.read(tag, profile, range, TestNfcFailures).unwrapError()

        assertSame(cause, failure.cause)
        assertEquals(listOf(1, 2), tag.requests.map { it.last().toInt() })
        assertEquals(1, tag.closeCount)
    }

    /** 読取成功後のclose失敗も、完了した成功として返さない。 */
    @Test
    fun reportsCloseFailureAfterSuccessfulRead() = runTest {
        val cause = IOException("close")
        val tag = FakeNfcTag().apply { closeFailure = cause }

        assertSame(cause, reader.read(tag, profile, range, TestNfcFailures).unwrapError().cause)
        assertEquals(1, tag.closeCount)
    }

    /** 読取失敗とclose失敗が重なった場合、先の失敗を返しclose原因をsuppressedへ残す。 */
    @Test
    fun preservesOriginalReadFailureWhenCloseAlsoFails() = runTest {
        val readCause = IOException("read")
        val closeCause = IOException("close")
        val tag = FakeNfcTag().apply {
            respond = { throw readCause }
            closeFailure = closeCause
        }

        assertSame(readCause, reader.read(tag, profile, range, TestNfcFailures).unwrapError().cause)
        assertSame(closeCause, readCause.suppressed.single())
        assertEquals(1, tag.closeCount)
    }

    /** 型付きの応答不正も、後続のclose失敗で通信エラーへ置き換えない。 */
    @Test
    fun preservesResponseFailureWhenCloseAlsoFails() = runTest {
        val tag = FakeNfcTag().apply {
            respond = { byteArrayOf() }
            closeFailure = IOException("close")
        }

        assertEquals(FailureKind.RESPONSE_FORMAT, reader.read(tag, profile, range, TestNfcFailures).unwrapError().kind)
    }

    /** 先に返す読取失敗がある場合、close側の予期しない失敗でも置き換えない。 */
    @Test
    fun preservesReadFailureWhenCloseThrowsUnexpectedException() = runTest {
        val cause = IOException("read")
        val closeCause = IllegalStateException("close")
        val tag = FakeNfcTag().apply {
            respond = { throw cause }
            closeFailure = closeCause
        }

        assertSame(cause, reader.read(tag, profile, range, TestNfcFailures).unwrapError().cause)
        assertSame(closeCause, cause.suppressed.single())
    }

    /** キャンセルをErrへ取り込まず、close後に原因を保って伝播する。 */
    @Test
    fun propagatesCancellationAndClosesConnection() = runTest {
        val cause = CancellationException("cancel")
        val tag = FakeNfcTag().apply { respond = { throw cause } }
        val thrown = kotlin.runCatching { reader.read(tag, profile, range, TestNfcFailures) }.exceptionOrNull()

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
        val thrown = kotlin.runCatching { reader.read(tag, profile, range, TestNfcFailures) }.exceptionOrNull()

        assertTrue(thrown is AssertionError)
        // コルーチンのスタックトレース復元で例外が複製されても、元の原因を保持していること。
        assertTrue(generateSequence(thrown) { it.cause }.any { it === cause })
        assertEquals(1, tag.closeCount)
    }

    /** Androidのアクセス拒否をI/Oと区別する。 */
    @Test
    fun reportsAccessDenialAndClosesConnection() = runTest {
        val cause = SecurityException("stale handle")
        val tag = FakeNfcTag().apply { connectFailure = cause }

        assertEquals(FailureKind.ACCESS_DENIED, reader.read(tag, profile, range, TestNfcFailures).unwrapError().kind)
        assertEquals(1, tag.closeCount)
    }

    /** 親からの取消をブロック間で検知し、残りのREADと部分結果の返却を行わない。 */
    @Test
    fun stopsBeforeNextBlockWhenJobIsCancelled() = runTest {
        val readingJob = Job(coroutineContext[Job])
        val tag = FakeNfcTag().apply {
            respond = {
                readingJob.cancel()
                ByteArray(5)
            }
        }
        val thrown = kotlin.runCatching {
            withContext(readingJob) { reader.read(tag, profile, range, TestNfcFailures) }
        }.exceptionOrNull()

        assertTrue(thrown is CancellationException)
        assertEquals(listOf(1), tag.requests.map { it.last().toInt() })
        assertEquals(1, tag.closeCount)
    }

    /** メタデータがNFC-Vでも接続が取得できなければ対象外として終える。 */
    @Test
    fun reportsUnavailableNfcVConnection() = runTest {
        val tag = FakeNfcTag().apply { connectionAvailable = false }

        assertEquals(FailureKind.UNSUPPORTED, reader.read(tag, profile, range, TestNfcFailures).unwrapError().kind)
        assertEquals(0, tag.closeCount)
    }
}
