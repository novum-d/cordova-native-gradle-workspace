package dev.example.nativefeature.nfc

import dev.example.nativedata.nfc.detection.NfcDetectionAccessDenied
import dev.example.nativedata.nfc.detection.NfcDetectionFailure
import dev.example.nativedata.nfc.detection.NfcDetectionFailureFactory
import dev.example.nativedata.nfc.detection.NfcDisabled
import dev.example.nativedata.nfc.detection.NfcNotSupported

/** 汎用タグ検出ユースケースの失敗契約。読取の失敗と発生元を区別する。 */
sealed interface ObserveNfcTagsErr {
    /** NFCの検出開始に関する失敗。 */
    sealed interface NfcDetection : ObserveNfcTagsErr, NfcDetectionFailure {
        /** NFC非搭載端末。 */
        data object NotSupported : NfcDetection, NfcNotSupported

        /** NFCが端末設定で無効。 */
        data object Disabled : NfcDetection, NfcDisabled

        /** OSによる検出開始の拒否。 */
        data class AccessDenied(override val cause: SecurityException) : NfcDetection, NfcDetectionAccessDenied

        /** 下位層で、このユースケースの具体的な失敗を直接生成する。 */
        companion object : NfcDetectionFailureFactory<NfcDetection> {
            override fun notSupported() = NotSupported
            override fun disabled() = Disabled
            override fun accessDenied(cause: SecurityException) = AccessDenied(cause)
        }
    }
}
