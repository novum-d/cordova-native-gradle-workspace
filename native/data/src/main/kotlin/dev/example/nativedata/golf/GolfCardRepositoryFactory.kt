package dev.example.nativedata.golf

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import dev.example.nativedata.nfc.DetectedNfcTag
import dev.example.nativedata.nfc.DetectedNfcTagHandle
import dev.example.nativedata.nfc.NfcMemoryPosition
import dev.example.nativedata.nfc.NfcMemoryRange
import dev.example.nativedata.nfc.NfcMemoryReader
import dev.example.nativedata.nfc.NfcReadFailure
import dev.example.nativedata.nfc.NfcReadFailureFactory
import dev.example.nativedata.nfc.NfcTagModel
import dev.example.nativedata.nfc.identifyNfcTagProfile
import dev.example.nativedata.nfc.nfcCall
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/** 対象とエラー文脈を生成時に固定する。Repository自体をSingletonにはしない。 */
interface GolfCardRepositoryFactory {
    fun <E : NfcReadFailure> create(
        target: DetectedNfcTag,
        failures: NfcReadFailureFactory<E>,
    ): GolfCardRepository<E>
}

/** Androidの検出対象と通信依存を組み合わせ、短命なRepositoryを生成する。 */
internal class DefaultGolfCardRepositoryFactory @Inject constructor(
    private val reader: NfcMemoryReader,
) : GolfCardRepositoryFactory {
    override fun <E : NfcReadFailure> create(
        target: DetectedNfcTag,
        failures: NfcReadFailureFactory<E>,
    ): GolfCardRepository<E> {
        require(target is DetectedNfcTagHandle) { "The target must be issued by the NFC detection repository" }
        return DefaultGolfCardRepository(target, failures, reader)
    }
}

/** 現在はSLIの確定した経路のみを扱い、未対応製品のレイアウトを推測しない。 */
private class DefaultGolfCardRepository<E : NfcReadFailure>(
    private val target: DetectedNfcTagHandle,
    private val failures: NfcReadFailureFactory<E>,
    private val reader: NfcMemoryReader,
) : GolfCardRepository<E> {
    override suspend fun readGolfCourseNum(): Result<ByteArray, E> = read(GolfCardField.COURSE_NUMBER)

    override suspend fun readGolfCardHolderNum(): Result<ByteArray, E> = read(GolfCardField.HOLDER_NUMBER)

    /** エラー文脈の異なるRepositoryでも、同じタグへの接続を競合させない。 */
    private suspend fun read(field: GolfCardField): Result<ByteArray, E> = target.readMutex.withLock {
        if (target.isActive) {
            nfcCall(failures) {
                identifyNfcTagProfile(target.access.uid, target.access.technologies)
            }.andThen { profile ->
                if (profile == null) {
                    Err(failures.unsupportedTag())
                } else {
                    reader.read(
                        tag = target.access,
                        profile = profile,
                        range = field.rangeFor(profile.model),
                        failures = failures,
                    )
                }
            }
        } else {
            Err(failures.targetExpired())
        }
    }
}

/** SLI業務カードの固定配置。0始まりで、範囲は末尾バイトも含む。 */
private enum class GolfCardField(private val icodeSliRange: NfcMemoryRange) {
    COURSE_NUMBER(NfcMemoryRange(NfcMemoryPosition(1, 0), NfcMemoryPosition(1, 1))),
    HOLDER_NUMBER(NfcMemoryRange(NfcMemoryPosition(1, 2), NfcMemoryPosition(4, 3))),
    ;

    /** ICのメモリ形状とは別に、発行書式が確定したモデルの業務配置を選ぶ。 */
    fun rangeFor(model: NfcTagModel): NfcMemoryRange = when (model) {
        NfcTagModel.ICODE_SLI -> icodeSliRange
    }
}
