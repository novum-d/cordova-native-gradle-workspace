package dev.example.nativefeature.nfc

import dev.example.nativedata.err.nfc.NfcDetectionAccessDenied
import dev.example.nativedata.err.nfc.NfcDetectionErr
import dev.example.nativedata.err.nfc.NfcDetectionErrFactory
import dev.example.nativedata.err.nfc.NfcDisabled
import dev.example.nativedata.err.nfc.NfcNotSupported

/** 汎用タグ検出ユースケースの失敗契約。読取の失敗と発生元を区別する。 */
sealed interface ObserveNfcTagsErr {
    /** NFCの検出開始に関する失敗。 */
    sealed interface NfcDetection : ObserveNfcTagsErr, NfcDetectionErr {
        /** NFC非搭載端末。 */
        data object NotSupported : NfcDetection, NfcNotSupported

        /** NFCが端末設定で無効。 */
        data object Disabled : NfcDetection, NfcDisabled

        /** OSによる検出開始の拒否。 */
        data class AccessDenied(override val cause: SecurityException) : NfcDetection, NfcDetectionAccessDenied

        /** 下位層で、このユースケースの具体的な失敗を直接生成する。 */
        companion object : NfcDetectionErrFactory<NfcDetection> {
            override fun notSupported() = NotSupported
            override fun disabled() = Disabled
            override fun accessDenied(cause: SecurityException) = AccessDenied(cause)
        }
    }
}
