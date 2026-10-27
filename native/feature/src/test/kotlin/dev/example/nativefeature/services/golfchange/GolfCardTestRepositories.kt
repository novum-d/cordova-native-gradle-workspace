package dev.example.nativefeature.services.golfchange

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.example.nativedata.golf.GolfCardRepository
import dev.example.nativedata.golf.GolfCardRepositoryFactory
import dev.example.nativedata.nfc.DetectedNfcTag
import dev.example.nativedata.nfc.NfcReadFailure
import dev.example.nativedata.nfc.NfcReadFailureFactory
import java.io.IOException

/** 型番や物理メモリを持たず、ユースケースが受け取れる情報を限定した模擬対象。 */
internal class TestDetectedTag : DetectedNfcTag

/** 対象ごとの返却値と呼出履歴で、別カードの値を混ぜないことを検証する。 */
internal class GolfCardTestRepositories : GolfCardRepositoryFactory {
    val courseBytes = mutableMapOf<DetectedNfcTag, ByteArray>()
    val holderBytes = mutableMapOf<DetectedNfcTag, ByteArray>()
    val calls = mutableListOf<Pair<DetectedNfcTag, String>>()
    var failure: IOException? = null
    var failHolderOnly = false
    var unsupported = false
    var beforeRead: suspend () -> Unit = {}

    override fun <E : NfcReadFailure> create(
        target: DetectedNfcTag,
        failures: NfcReadFailureFactory<E>,
    ): GolfCardRepository<E> = object : GolfCardRepository<E> {
        override suspend fun readGolfCourseNum(): Result<ByteArray, E> =
            read("course", courseBytes[target] ?: ByteArray(2))
        override suspend fun readGolfCardHolderNum(): Result<ByteArray, E> =
            read("holder", holderBytes[target] ?: ByteArray(14))

        /** 実NFCの挙動はdataで検証し、featureでは値解釈・順序・具体エラーを観測する。 */
        private suspend fun read(
            field: String,
            bytes: ByteArray,
        ): Result<ByteArray, E> {
            calls.add(target to field)
            beforeRead()
            val cause = failure
            return when {
                unsupported -> Err(failures.unsupportedTag())
                cause != null && (!failHolderOnly || field == "holder") -> Err(failures.communication(cause))
                else -> Ok(bytes.copyOf())
            }
        }
    }
}
