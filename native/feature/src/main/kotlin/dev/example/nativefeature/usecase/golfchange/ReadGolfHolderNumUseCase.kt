package dev.example.nativefeature.usecase.golfchange

import com.github.michaelbull.result.Result
import dev.example.nativedata.repository.golf.GolfCardRepository
import dev.example.nativefeature.err.golfchange.ReadGolfHolderNumErr

/** ホルダー番号を取得する。読取範囲・バイト列の解釈はRepositoryへ任せる。 */
class ReadGolfHolderNumUseCase(private val repository: GolfCardRepository<ReadGolfHolderNumErr.NfcRead>) {
    /** ホルダー番号を、大文字・区切りなし28桁の16進Stringで返す。 */
    suspend operator fun invoke(): Result<String, ReadGolfHolderNumErr.NfcRead> = repository.readGolfCardHolderNum()
}
