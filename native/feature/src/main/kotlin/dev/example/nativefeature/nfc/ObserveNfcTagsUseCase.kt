package dev.example.nativefeature.nfc

import com.github.michaelbull.result.Result
import dev.example.nativedata.nfc.DetectedNfcTag
import dev.example.nativedata.nfc.NfcTagDetectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** ゴルフなどの業務に依存しないタグ検出の入口。型番やAndroidのTagは受け渡さない。 */
class ObserveNfcTagsUseCase @Inject constructor(private val repository: NfcTagDetectionRepository) {
    /** 収集の終了に合わせて検出要求を解除する。過去の検出対象は再生しない。 */
    operator fun invoke(): Flow<Result<DetectedNfcTag, ObserveNfcTagsErr>> =
        repository.observeTags(ObserveNfcTagsErr.NfcDetection)
}
