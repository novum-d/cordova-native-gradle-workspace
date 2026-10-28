package dev.example.nativedata.golf

import com.github.michaelbull.result.Result
import dev.example.nativedata.nfc.failure.NfcReadFailure

/** 生成時に固定した対象から業務項目の生バイトを取得する。型番・座標は公開しない。 */
interface GolfCardRepository<out E : NfcReadFailure> {
    /** ゴルフ場番号の2バイトを格納順で返す。値の解釈は呼出側が行う。 */
    suspend fun readGolfCourseNum(): Result<ByteArray, E>

    /** ホルダー番号の14バイトを返す。文字コード・パディングは推定しない。 */
    suspend fun readGolfCardHolderNum(): Result<ByteArray, E>
}
