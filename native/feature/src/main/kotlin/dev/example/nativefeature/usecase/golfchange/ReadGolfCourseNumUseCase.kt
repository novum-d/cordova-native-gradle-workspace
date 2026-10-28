package dev.example.nativefeature.usecase.golfchange

import com.github.michaelbull.result.Result
import dev.example.nativedata.repository.golf.GolfCardRepository
import dev.example.nativefeature.err.golfchange.ReadGolfCourseNumErr

/** サービス導入先のゴルフ場番号を取得する。通信・座標・バイト列の解釈はRepositoryへ任せる。 */
class ReadGolfCourseNumUseCase(private val repository: GolfCardRepository<ReadGolfCourseNumErr.NfcRead>) {
    /** 対象カードのゴルフ場番号を、0..65535のIntとして返す。 */
    suspend operator fun invoke(): Result<Int, ReadGolfCourseNumErr.NfcRead> = repository.readGolfCourseNum()
}
