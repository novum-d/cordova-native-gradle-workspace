package dev.example.nativefeature.services.golfchange

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import dev.example.nativedata.golf.GolfCardRepository

/** サービス導入先のゴルフ場番号を読み取る。接続・SLI判定・座標の解決はRepositoryへ任せる。 */
class ReadGolfCourseNumberUseCase(private val repository: GolfCardRepository<ReadGolfCourseNumberErr.NfcRead>) {
    /** ブロック1のByte 0・1を、LSBファーストの符号なし16bit値（0..65535）として返す。 */
    suspend operator fun invoke(): Result<Int, ReadGolfCourseNumberErr.NfcRead> =
        repository.readGolfCourseNum().map { bytes ->
            (bytes[0].toInt() and 0xFF) or ((bytes[1].toInt() and 0xFF) shl 8)
        }
}
