package dev.example.nativedata.nfc

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** 業務を持たない検出の購読寿命・追加タグの扱い・具体失敗の生成を検証する。 */
@OptIn(ExperimentalCoroutinesApi::class)
class NfcTagDetectionRepositoryTest {
    private val source = NfcTagDetectionSource()
    private val repository = DefaultNfcTagDetectionRepository(source)

    /** 収集時だけ要求を公開し、キャンセル後は対象を無効化する。 */
    @Test
    fun startsOnCollectionAndInvalidatesTargetOnCancellation() = runTest {
        assertNull(source.requests.value)
        val results = mutableListOf<Result<DetectedNfcTag, DetectionFailure>>()
        val release = CompletableDeferred<Unit>()
        val job = backgroundScope.launch {
            repository.observeTags(DetectionFailures).collect {
                results.add(it)
                release.await()
            }
        }
        runCurrent()
        val request = requireNotNull(source.requests.value)
        assertTrue(request.onTag(FakeNfcTag()))
        runCurrent()
        val target = results.single().unwrap() as DetectedNfcTagHandle
        assertTrue(target.isActive)

        job.cancelAndJoin()
        assertNull(source.requests.value)
        assertFalse(target.isActive)
        assertFalse(request.onTag(FakeNfcTag()))
    }

    /** 再購読では別要求を作り、前回のTagを再生しない。 */
    @Test
    fun neverReplaysAnOldTagToANewSubscriber() = runTest {
        val results = mutableListOf<Result<DetectedNfcTag, DetectionFailure>>()
        val first = backgroundScope.launch { repository.observeTags(DetectionFailures).collect { results.add(it) } }
        runCurrent()
        val oldRequest = requireNotNull(source.requests.value)
        oldRequest.onTag(FakeNfcTag())
        runCurrent()
        first.cancelAndJoin()
        results.clear()

        val second = backgroundScope.launch { repository.observeTags(DetectionFailures).collect { results.add(it) } }
        runCurrent()
        assertNotSame(oldRequest, source.requests.value)
        assertTrue(results.isEmpty())
        second.cancelAndJoin()
    }

    /** 処理中の追加検出をため込まず、完了後の再提示を受け付ける。 */
    @Test
    fun dropsTagsWhileConsumerIsBusy() = runTest {
        val release = CompletableDeferred<Unit>()
        val results = mutableListOf<DetectedNfcTag>()
        val job = backgroundScope.launch {
            repository.observeTags(DetectionFailures).collect {
                results.add(it.unwrap())
                release.await()
            }
        }
        runCurrent()
        val request = requireNotNull(source.requests.value)
        assertTrue(request.onTag(FakeNfcTag()))
        runCurrent()
        assertFalse(request.onTag(FakeNfcTag()))
        release.complete(Unit)
        runCurrent()
        assertEquals(1, results.size)
        assertTrue(request.onTag(FakeNfcTag()))
        runCurrent()
        assertEquals(2, results.size)
        assertFalse((results.first() as DetectedNfcTagHandle).isActive)
        job.cancelAndJoin()
    }

    /** 同時の購読は1つのReaderModeを競合させず、終了したら次へ引き継ぐ。 */
    @Test
    fun serializesConcurrentDetectionRequests() = runTest {
        val first = backgroundScope.launch { repository.observeTags(DetectionFailures).collect {} }
        runCurrent()
        val oldRequest = source.requests.value
        val second = backgroundScope.launch { repository.observeTags(DetectionFailures).collect {} }
        runCurrent()
        assertSame(oldRequest, source.requests.value)

        first.cancelAndJoin()
        runCurrent()
        assertNotSame(oldRequest, source.requests.value)
        second.cancelAndJoin()
        assertNull(source.requests.value)
    }

    /** NFC非搭載・無効・権限拒否の原因を、指定したFactoryで生成する。 */
    @Test
    fun generatesDetectionFailuresWithoutWrappingTheCause() = runTest {
        val results = mutableListOf<Result<DetectedNfcTag, DetectionFailure>>()
        val job = backgroundScope.launch { repository.observeTags(DetectionFailures).collect { results.add(it) } }
        runCurrent()
        val request = requireNotNull(source.requests.value)
        request.signals.send(NfcDetectionSignal.NotSupported)
        runCurrent()
        request.signals.send(NfcDetectionSignal.Disabled)
        runCurrent()
        val cause = SecurityException("denied")
        request.signals.send(NfcDetectionSignal.AccessDenied(cause))
        runCurrent()

        assertEquals(listOf("not supported", "disabled", "denied"), results.map { it.unwrapError().reason })
        assertSame(cause, results.last().unwrapError().cause)
        job.cancelAndJoin()
    }

    /** 読取処理が戻ったら、次の検出や購読終了を待たず対象を無効化する。 */
    @Test
    fun invalidatesTargetImmediatelyAfterConsumerFinishes() = runTest {
        val results = mutableListOf<Result<DetectedNfcTag, DetectionFailure>>()
        var activeDuringRead = false
        val job = backgroundScope.launch {
            repository.observeTags(DetectionFailures).collect {
                results.add(it)
                activeDuringRead = (it.unwrap() as DetectedNfcTagHandle).isActive
            }
        }
        runCurrent()
        val request = requireNotNull(source.requests.value)
        request.onTag(FakeNfcTag())
        runCurrent()
        val target = results.single().unwrap() as DetectedNfcTagHandle
        assertTrue(activeDuringRead)
        assertFalse(target.isActive)
        job.cancelAndJoin()
    }
}

/** Android・featureの型に依存せず、Factoryの呼出結果を観測する。 */
private data class DetectionFailure(val reason: String, val cause: SecurityException? = null) : NfcDetectionFailure

/** 検出失敗の原因をそのまま返す試験用Factory。 */
private object DetectionFailures : NfcDetectionFailureFactory<DetectionFailure> {
    override fun notSupported() = DetectionFailure("not supported")
    override fun disabled() = DetectionFailure("disabled")
    override fun accessDenied(cause: SecurityException) = DetectionFailure("denied", cause)
}
