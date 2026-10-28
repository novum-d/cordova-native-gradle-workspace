package dev.example.nativedata.repository.golf

import com.github.michaelbull.result.Result
import dev.example.nativedata.err.nfc.NfcReadErr

/** 生成時に固定した対象から業務値を取得する。型番・座標・バイト列の解釈は公開しない。 */
interface GolfCardRepository<out E : NfcReadErr> {
    /** ゴルフ場番号を0..65535のIntとして返す。 */
    suspend fun readGolfCourseNum(): Result<Int, E>

    /** ホルダー番号を大文字・区切りなしの28桁16進Stringで返す。文字コード・パディングは推定しない。 */
    suspend fun readGolfCardHolderNum(): Result<String, E>
}
