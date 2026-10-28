package dev.example.nativedata.repository.golf

import com.github.michaelbull.result.Result
import dev.example.nativedata.err.nfc.NfcReadErr
import dev.example.nativedata.nfc.card.NfcCardReader
import dev.example.nativedata.nfc.profile.NfcTagModel

/** ゴルフの業務項目をSLI用の確定した座標へ対応付け、共通Readerへ読取を委譲する。 */
internal class DefaultGolfCardRepository<E : NfcReadErr>(
    private val nfcReader: NfcCardReader<E>,
) : GolfCardRepository<E> {
    override suspend fun readGolfCourseNum(): Result<ByteArray, E> = read(GolfCardField.COURSE_NUM)

    override suspend fun readGolfCardHolderNum(): Result<ByteArray, E> = read(GolfCardField.HOLDER_NUM)

    /** 型番・座標の指定はdata内部に留め、ユースケースの引数なしAPIを維持する。 */
    private suspend fun read(field: GolfCardField): Result<ByteArray, E> =
        nfcReader.read(expectedModel = NfcTagModel.ICODE_SLI, range = field.range)
}
