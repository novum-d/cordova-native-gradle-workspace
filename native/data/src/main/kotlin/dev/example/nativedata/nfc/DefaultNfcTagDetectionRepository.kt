package dev.example.nativedata.nfc

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/** 共通の検出状態から、呼出元が指定した具体エラーを直接生成する。 */
class DefaultNfcTagDetectionRepository @Inject constructor(
    private val source: NfcTagDetectionSource,
) : NfcTagDetectionRepository {
    override fun <E : NfcDetectionFailure> observeTags(
        failures: NfcDetectionFailureFactory<E>,
    ): Flow<Result<DetectedNfcTag, E>> = flow {
        source.observe().collect { signal ->
            emit(
                when (signal) {
                    is NfcDetectionSignal.TagFound -> Ok(signal.tag)
                    NfcDetectionSignal.NotSupported -> Err(failures.notSupported())
                    NfcDetectionSignal.Disabled -> Err(failures.disabled())
                    is NfcDetectionSignal.AccessDenied -> Err(failures.accessDenied(signal.cause))
                },
            )
        }
    }
}
