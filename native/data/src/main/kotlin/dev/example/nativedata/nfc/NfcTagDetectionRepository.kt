package dev.example.nativedata.nfc

import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow

/** 業務用途に依存せず、検出対象または検出開始の失敗を通知する。 */
interface NfcTagDetectionRepository {
    /**
     * 収集中だけ検出を要求し、複数要求は直列化する。対象の読取は下流のmap・collect内で完了させる。
     * 通知処理が戻ると対象は失効するため、firstで持ち出したりbufferで処理を分離したりしない。
     */
    fun <E : NfcDetectionFailure> observeTags(failures: NfcDetectionFailureFactory<E>): Flow<Result<DetectedNfcTag, E>>
}
