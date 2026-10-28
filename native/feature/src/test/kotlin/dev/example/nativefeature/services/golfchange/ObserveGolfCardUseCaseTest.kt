package dev.example.nativefeature.services.golfchange

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.example.nativedata.nfc.DetectedNfcTag
import dev.example.nativedata.nfc.NfcDetectionFailure
import dev.example.nativedata.nfc.NfcDetectionFailureFactory
import dev.example.nativedata.nfc.NfcTagDetectionRepository
import dev.example.nativefeature.nfc.ObserveNfcTagsErr
import dev.example.nativefeature.nfc.ObserveNfcTagsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** 汎用検出から業務ユースケースまで通し、同じ対象・処理順序・発生元の保持を検証する。 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveGolfCardUseCaseTest {
    private val detection = TestTagDetectionRepository()
    private val repositories = TestCardRepositoryFactory()
    private val useCase = ObserveGolfCardUseCase(
        ObserveNfcTagsUseCase(detection),
        GolfCardReadUseCaseFactory(repositories),
    )

    /** 2枚を順に検出しても、各組の番号は同じカードから取得する。 */
    @Test
    fun readsBothFieldsFromTheSameTargetForEveryDetection() = runTest {
        val first = TestDetectedTag()
        val second = TestDetectedTag()
        repositories.courseBytes[first] = byteArrayOf(1, 2)
        repositories.courseBytes[second] = byteArrayOf(3, 0)
        repositories.holderBytes[first] = ByteArray(14) { 1 }
        repositories.holderBytes[second] = ByteArray(14) { 2 }
        val results = async { useCase().take(2).toList() }

        detection.events.send(TestDetection.TagFound(first))
        detection.events.send(TestDetection.TagFound(second))

        assertEquals(
            listOf(
                GolfCardReadEvent.ReadCompleted(513, "01".repeat(14)),
                GolfCardReadEvent.ReadCompleted(3, "02".repeat(14)),
            ),
            results.await(),
        )
        assertEquals(
            listOf(first to "course", first to "holder", second to "course", second to "holder"),
            repositories.calls,
        )
    }

    /** 検出失敗時は読取を開始せず、汎用検出ユースケースの発生元を保持する。 */
    @Test
    fun preservesDetectionFailureAndDoesNotStartReading() = runTest {
        val result = async { useCase().first() }
        detection.events.send(TestDetection.Disabled)

        assertEquals(GolfCardReadEvent.DetectionFailed(ObserveNfcTagsErr.NfcDetection.Disabled), result.await())
        assertTrue(repositories.calls.isEmpty())
    }

    /** ゴルフ場番号で失敗したらホルダーは読まず、元の具体エラーと原因を保持する。 */
    @Test
    fun stopsBeforeHolderWhenCourseReadFails() = runTest {
        val target = TestDetectedTag()
        val cause = IOException("course")
        repositories.failure = cause
        val result = async { useCase().first() }
        detection.events.send(TestDetection.TagFound(target))
        val event = result.await() as GolfCardReadEvent.CourseReadFailed

        assertTrue(event.failure is ReadGolfCourseNumberErr.NfcRead.Communication)
        assertSame(cause, event.failure.cause)
        assertEquals(listOf(target to "course"), repositories.calls)
    }

    /** ホルダーの失敗をゴルフ場番号の失敗へ変換しない。部分成功を完了扱いしない。 */
    @Test
    fun preservesHolderFailureWithoutReturningPartialSuccess() = runTest {
        val cause = IOException("holder")
        repositories.failure = cause
        repositories.failHolderOnly = true
        val result = async { useCase().first() }
        detection.events.send(TestDetection.TagFound(TestDetectedTag()))
        val event = result.await() as GolfCardReadEvent.HolderReadFailed

        assertTrue(event.failure is ReadGolfHolderNumberErr.NfcRead.Communication)
        assertSame(cause, event.failure.cause)
        assertEquals(listOf("course", "holder"), repositories.calls.map { it.second })
    }

    /** 失敗しても検出購読を終了せず、次のカードで成功できる。 */
    @Test
    fun keepsObservingAfterAReadFailure() = runTest {
        repositories.unsupported = true
        val events = mutableListOf<GolfCardReadEvent>()
        val job = backgroundScope.launch { useCase().collect { events.add(it) } }
        detection.events.send(TestDetection.TagFound(TestDetectedTag()))
        runCurrent()
        repositories.unsupported = false
        detection.events.send(TestDetection.TagFound(TestDetectedTag()))
        runCurrent()

        assertTrue(events.first() is GolfCardReadEvent.CourseReadFailed)
        assertTrue(events.last() is GolfCardReadEvent.ReadCompleted)
        job.cancelAndJoin()
    }

    /** 画面の購読終了を模したキャンセルが、読取と検出まで届く。 */
    @Test
    fun propagatesCancellationInsteadOfTurningItIntoAReadFailure() = runTest {
        repositories.beforeRead = { awaitCancellation() }
        val results = mutableListOf<GolfCardReadEvent>()
        val job = backgroundScope.launch { useCase().collect { results.add(it) } }
        detection.events.send(TestDetection.TagFound(TestDetectedTag()))
        runCurrent()
        job.cancelAndJoin()

        assertEquals(1, detection.finishedCollections)
        assertTrue(results.isEmpty())
        assertEquals(listOf("course"), repositories.calls.map { it.second })
    }

    /** NFC非搭載・検出拒否も、汎用の検出ユースケースで具体型を生成する。 */
    @Test
    fun exposesGenericDetectionFailuresWithOriginalCause() = runTest {
        val results = async { ObserveNfcTagsUseCase(detection)().take(2).toList() }
        val cause = SecurityException("reader mode denied")
        detection.events.send(TestDetection.NotSupported)
        detection.events.send(TestDetection.Denied(cause))
        val failures = results.await().map { it.error }

        assertSame(ObserveNfcTagsErr.NfcDetection.NotSupported, failures.first())
        assertSame(cause, (failures.last() as ObserveNfcTagsErr.NfcDetection.AccessDenied).cause)
    }
}

/** Androidを使わず、検出と利用可否の順序を制御する。 */
private sealed interface TestDetection {
    data class TagFound(val target: DetectedNfcTag) : TestDetection
    data object Disabled : TestDetection
    data object NotSupported : TestDetection
    data class Denied(val cause: SecurityException) : TestDetection
}

/** 購読終了も観測できる、業務に依存しない模擬Repository。 */
private class TestTagDetectionRepository : NfcTagDetectionRepository {
    val events = Channel<TestDetection>(Channel.RENDEZVOUS)
    var finishedCollections = 0

    override fun <E : NfcDetectionFailure> observeTags(
        failures: NfcDetectionFailureFactory<E>,
    ): Flow<Result<DetectedNfcTag, E>> = flow {
        try {
            for (event in events) {
                emit(
                    when (event) {
                        is TestDetection.TagFound -> Ok(event.target)
                        TestDetection.Disabled -> Err(failures.disabled())
                        TestDetection.NotSupported -> Err(failures.notSupported())
                        is TestDetection.Denied -> Err(failures.accessDenied(event.cause))
                    },
                )
            }
        } finally {
            finishedCollections++
        }
    }
}
