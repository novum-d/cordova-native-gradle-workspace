package dev.example.nativedata.golf

import com.github.michaelbull.result.Result
import dev.example.nativedata.nfc.NfcCardRepository
import dev.example.nativedata.nfc.NfcReadFailure
import dev.example.nativedata.nfc.NfcTagModel

/** ゴルフの業務項目をSLI用の確定した座標へ対応付け、共通Repositoryへ読取を委譲する。 */
internal class DefaultGolfCardRepository<E : NfcReadFailure>(
    private val nfcRepository: NfcCardRepository<E>,
) : GolfCardRepository<E> {
    override suspend fun readGolfCourseNum(): Result<ByteArray, E> = read(GolfCardField.COURSE_NUMBER)

    override suspend fun readGolfCardHolderNum(): Result<ByteArray, E> = read(GolfCardField.HOLDER_NUMBER)

    /** 型番・座標の指定はdata内部に留め、ユースケースの引数なしAPIを維持する。 */
    private suspend fun read(field: GolfCardField): Result<ByteArray, E> =
        nfcRepository.read(expectedModel = NfcTagModel.ICODE_SLI, range = field.range)
}
