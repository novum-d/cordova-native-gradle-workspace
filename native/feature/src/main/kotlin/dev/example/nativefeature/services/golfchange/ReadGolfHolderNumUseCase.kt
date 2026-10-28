package dev.example.nativefeature.services.golfchange

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import dev.example.nativedata.repository.golf.GolfCardRepository

/** ホルダー領域を読み、書式が確定するまで元バイトを失わない16進文字列で返す。 */
class ReadGolfHolderNumUseCase(private val repository: GolfCardRepository<ReadGolfHolderNumErr.NfcRead>) {
    /** ブロック1 Byte 2〜ブロック4 Byte 3の14バイトを、読取順・大文字・区切りなし28桁で返す。 */
    suspend operator fun invoke(): Result<String, ReadGolfHolderNumErr.NfcRead> =
        repository.readGolfCardHolderNum().map { bytes ->
            buildString(bytes.size * 2) {
                for (byte in bytes) {
                    val value = byte.toInt() and 0xFF
                    append(HEX_DIGITS[value ushr 4])
                    append(HEX_DIGITS[value and 0x0F])
                }
            }
        }

    /** Localeや文字コードに依存せず、各バイトの先頭0も保持するための桁文字。 */
    private companion object {
        const val HEX_DIGITS = "0123456789ABCDEF"
    }
}
