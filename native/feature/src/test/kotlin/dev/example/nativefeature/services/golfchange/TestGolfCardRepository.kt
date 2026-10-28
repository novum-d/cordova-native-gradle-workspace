package dev.example.nativefeature.services.golfchange

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.example.nativedata.golf.GolfCardRepository
import dev.example.nativedata.nfc.detection.DetectedNfcTag
import dev.example.nativedata.nfc.failure.NfcReadFailure
import dev.example.nativedata.nfc.failure.NfcReadFailureFactory

/** 実NFCはdataのテストへ任せ、featureでは業務値・処理順序・具体エラーを観測する。 */
internal class TestGolfCardRepository<E : NfcReadFailure>(
    private val target: DetectedNfcTag,
    private val failures: NfcReadFailureFactory<E>,
    private val state: TestCardRepositoryFactory,
) : GolfCardRepository<E> {
    override suspend fun readGolfCourseNum(): Result<ByteArray, E> =
        read("course", state.courseBytes[target] ?: ByteArray(2))

    override suspend fun readGolfCardHolderNum(): Result<ByteArray, E> =
        read("holder", state.holderBytes[target] ?: ByteArray(14))

    /** 元の原因をFactoryへ渡し、ユースケース固有型を包み直さず返す。 */
    private suspend fun read(
        field: String,
        bytes: ByteArray,
    ): Result<ByteArray, E> {
        state.calls.add(target to field)
        state.beforeRead()
        val cause = state.failure
        return when {
            state.unsupported -> Err(failures.unsupportedTag())
            cause != null && (!state.failHolderOnly || field == "holder") -> Err(failures.communication(cause))
            else -> Ok(bytes.copyOf())
        }
    }
}
