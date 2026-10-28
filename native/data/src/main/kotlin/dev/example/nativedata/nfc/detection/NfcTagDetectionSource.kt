package dev.example.nativedata.nfc.detection

import dev.example.nativedata.nfc.memory.NfcTagAccess
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 画面からの検出要求とAndroidホストを仲介する。最後のTagやActivityは保持しない。
 * 同時要求は直列化し、読取中に受け取れない追加タグをキューへ蓄積しない。
 */
@Singleton
class NfcTagDetectionSource @Inject constructor() {
    private val observationMutex = Mutex()
    private val pendingRequest = MutableStateFlow<NfcDetectionRequest?>(null)
    internal val requests = pendingRequest.asStateFlow()

    /** 終了・キャンセル時に要求を解除し、次の購読へ古い検出結果を渡さない。 */
    internal fun observe(): Flow<NfcDetectionSignal> = flow {
        observationMutex.withLock {
            val request = NfcDetectionRequest()
            pendingRequest.value = request
            try {
                for (signal in request.signals) {
                    try {
                        emit(signal)
                    } finally {
                        // 下流の読取処理が戻ったら、その操作の対象参照を失効させる。
                        (signal as? NfcDetectionSignal.TagFound)?.tag?.invalidate()
                    }
                }
            } finally {
                pendingRequest.value = null
                request.signals.cancel()
            }
        }
    }
}

/** Androidホストが通知する、業務・ユースケースの型に依存しない検出状態。 */
internal sealed interface NfcDetectionSignal {
    data class TagFound(val tag: DetectedNfcTagHandle) : NfcDetectionSignal
    data object NotSupported : NfcDetectionSignal
    data object Disabled : NfcDetectionSignal
    data class AccessDenied(val cause: SecurityException) : NfcDetectionSignal
}

/** 1つの購読に対応する受渡口。Tagの再生・永続化は行わない。 */
internal class NfcDetectionRequest {
    val signals = Channel<NfcDetectionSignal>(Channel.RENDEZVOUS)

    /** Androidのコールバックをブロックせず、受信待ちのときだけ対象を渡す。 */
    fun onTag(access: NfcTagAccess): Boolean =
        signals.trySend(NfcDetectionSignal.TagFound(DetectedNfcTagHandle(access))).isSuccess
}
