package dev.example.nativedata.repository.golf

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import dev.example.nativedata.err.nfc.NfcReadErr
import dev.example.nativedata.nfc.card.NfcCardReader
import dev.example.nativedata.nfc.profile.NfcTagModel

/** 固定座標の読取を共通Readerへ委譲し、成功したバイト列を業務値へ変換する。 */
internal class DefaultGolfCardRepository<E : NfcReadErr>(
    private val nfcReader: NfcCardReader<E>,
) : GolfCardRepository<E> {
    override suspend fun readGolfCourseNum(): Result<Int, E> =
        read(GolfCardField.COURSE_NUM).map(GolfCardDecoder::decodeCourseNum)

    override suspend fun readGolfCardHolderNum(): Result<String, E> =
        read(GolfCardField.HOLDER_NUM).map(GolfCardDecoder::decodeHolderNum)

    /** 型番・座標の指定はdata内部に留め、ユースケースの引数なしAPIを維持する。 */
    private suspend fun read(field: GolfCardField): Result<ByteArray, E> =
        nfcReader.read(expectedModel = NfcTagModel.ICODE_SLI, range = field.range)
}
