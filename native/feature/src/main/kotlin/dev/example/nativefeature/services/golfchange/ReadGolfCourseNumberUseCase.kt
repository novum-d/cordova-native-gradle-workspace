package dev.example.nativefeature.services.golfchange

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import dev.example.nativedata.nfc.IcodeSliMemoryReader
import dev.example.nativedata.nfc.NfcTagAccess
import javax.inject.Inject

/** サービス導入先のゴルフ場番号を読み取る。接続・SLI判定・生バイト取得はReaderへ任せる。 */
class ReadGolfCourseNumberUseCase @Inject constructor(private val reader: IcodeSliMemoryReader) {
    /** ブロック1のByte 0・1を、LSBファーストの符号なし16bit値（0..65535）として返す。 */
    suspend operator fun invoke(tag: NfcTagAccess): Result<Int, ReadGolfCourseNumberErr> = reader.read(
        tag = tag,
        range = GolfCardField.COURSE_NUMBER.range,
        failures = ReadGolfCourseNumberErr.NfcRead,
    ).map { bytes ->
        (bytes[0].toInt() and 0xFF) or ((bytes[1].toInt() and 0xFF) shl 8)
    }
}
