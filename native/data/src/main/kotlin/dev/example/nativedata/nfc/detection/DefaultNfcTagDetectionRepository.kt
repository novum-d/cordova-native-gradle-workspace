package dev.example.nativedata.nfc.detection

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.example.nativedata.err.nfc.NfcDetectionErr
import dev.example.nativedata.err.nfc.NfcDetectionErrFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/** 共通の検出状態から、呼出元が指定した具体エラーを直接生成する。 */
class DefaultNfcTagDetectionRepository @Inject constructor(
    private val source: NfcTagDetectionSource,
) : NfcTagDetectionRepository {
    override fun <E : NfcDetectionErr> observeTags(
        errFactory: NfcDetectionErrFactory<E>,
    ): Flow<Result<DetectedNfcTag, E>> = flow {
        source.observe().collect { signal ->
            emit(
                when (signal) {
                    is NfcDetectionSignal.TagFound -> Ok(signal.tag)
                    NfcDetectionSignal.NotSupported -> Err(errFactory.notSupported())
                    NfcDetectionSignal.Disabled -> Err(errFactory.disabled())
                    is NfcDetectionSignal.AccessDenied -> Err(errFactory.accessDenied(signal.cause))
                },
            )
        }
    }
}
