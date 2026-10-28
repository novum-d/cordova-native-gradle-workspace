package dev.example.nativefeature.testing.golfchange

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.example.nativedata.err.nfc.NfcReadErr
import dev.example.nativedata.err.nfc.NfcReadErrFactory
import dev.example.nativedata.nfc.detection.DetectedNfcTag
import dev.example.nativedata.repository.golf.GolfCardRepository

/** 実NFCはdataのテストへ任せ、featureでは業務値・処理順序・具体エラーを観測する。 */
internal class TestGolfCardRepository<E : NfcReadErr>(
    private val target: DetectedNfcTag,
    private val errFactory: NfcReadErrFactory<E>,
    private val state: TestCardRepositoryFactory,
) : GolfCardRepository<E> {
    override suspend fun readGolfCourseNum(): Result<Int, E> = read("course", state.courseNums[target] ?: 0)

    override suspend fun readGolfCardHolderNum(): Result<String, E> =
        read("holder", state.holderNums[target] ?: "0".repeat(28))

    /** 元の原因をFactoryへ渡し、ユースケース固有型を包み直さず返す。 */
    private suspend fun <T> read(
        field: String,
        value: T,
    ): Result<T, E> {
        state.calls.add(target to field)
        state.beforeRead()
        val cause = state.err
        return when {
            state.unsupported -> Err(errFactory.unsupportedTag())
            cause != null && (!state.failHolderOnly || field == "holder") -> Err(errFactory.communication(cause))
            else -> Ok(value)
        }
    }
}
