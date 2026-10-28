package dev.example.nativedata.err.nfc

/** 検出開始に失敗した理由。通信・読取の失敗とは区別する。 */
interface NfcDetectionErr

/** NFC非搭載端末。 */
interface NfcNotSupported : NfcDetectionErr

/** 端末設定でNFCが無効。 */
interface NfcDisabled : NfcDetectionErr

/** OSから検出の開始を拒否された状態。 */
interface NfcDetectionAccessDenied : NfcDetectionErr {
    val cause: SecurityException
}

/** data層から呼出元固有の検出失敗を直接生成する。 */
interface NfcDetectionErrFactory<out E : NfcDetectionErr> {
    fun notSupported(): E
    fun disabled(): E
    fun accessDenied(cause: SecurityException): E
}
