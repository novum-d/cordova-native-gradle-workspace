package dev.example.nativefeature.services.golfchange

import dev.example.nativedata.nfc.NfcAccessDenied
import dev.example.nativedata.nfc.NfcCommandRejected
import dev.example.nativedata.nfc.NfcCommunication
import dev.example.nativedata.nfc.NfcInvalidRange
import dev.example.nativedata.nfc.NfcMemoryRange
import dev.example.nativedata.nfc.NfcReadFailure
import dev.example.nativedata.nfc.NfcReadFailureFactory
import dev.example.nativedata.nfc.NfcResponseFormat
import dev.example.nativedata.nfc.NfcTagLost
import dev.example.nativedata.nfc.NfcUnsupportedTag
import java.io.IOException

/** ゴルフ場番号読取の失敗契約。修飾名からユースケース・データ処理・原因を識別する。 */
sealed interface ReadGolfCourseNumberErr {
    /** NFC読み取りで発生した失敗。共通interfaceでも同じ具体値を扱える。 */
    sealed interface NfcRead : ReadGolfCourseNumberErr, NfcReadFailure {
        /** 今回の読取対象でないタグ。 */
        data object UnsupportedTag : NfcRead, NfcUnsupportedTag

        /** 通信前に検出した不正なメモリ範囲。 */
        data class InvalidRange(override val range: NfcMemoryRange) : NfcRead, NfcInvalidRange

        /** Androidが報告したタグ離脱。 */
        data class TagLost(override val cause: IOException) : NfcRead, NfcTagLost

        /** 接続・送受信・切断のI/O失敗。 */
        data class Communication(override val cause: IOException) : NfcRead, NfcCommunication

        /** 空・短縮・過剰などの応答形式不正。 */
        data class ResponseFormat(override val description: String) : NfcRead, NfcResponseFormat

        /** タグが返したエラーコード。 */
        data class CommandRejected(override val code: Int) : NfcRead, NfcCommandRejected

        /** Androidによるタグアクセスの拒否。 */
        data class AccessDenied(override val cause: SecurityException) : NfcRead, NfcAccessDenied

        /** Readerがこのユースケースの具体型を直接生成するためのFactory。 */
        companion object : NfcReadFailureFactory<NfcRead> {
            override fun unsupportedTag() = UnsupportedTag
            override fun invalidRange(range: NfcMemoryRange) = InvalidRange(range)
            override fun tagLost(cause: IOException) = TagLost(cause)
            override fun communication(cause: IOException) = Communication(cause)
            override fun responseFormat(description: String) = ResponseFormat(description)
            override fun commandRejected(code: Int) = CommandRejected(code)
            override fun accessDenied(cause: SecurityException) = AccessDenied(cause)
        }
    }
}
