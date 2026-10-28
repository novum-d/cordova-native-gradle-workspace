package dev.example.nativedata.nfc.detection

/** 検出開始に失敗した理由。通信・読取の失敗とは区別する。 */
interface NfcDetectionFailure

/** NFC非搭載端末。 */
interface NfcNotSupported : NfcDetectionFailure

/** 端末設定でNFCが無効。 */
interface NfcDisabled : NfcDetectionFailure

/** OSから検出の開始を拒否された状態。 */
interface NfcDetectionAccessDenied : NfcDetectionFailure {
    val cause: SecurityException
}

/** data層から呼出元固有の検出失敗を直接生成する。 */
interface NfcDetectionFailureFactory<out E : NfcDetectionFailure> {
    fun notSupported(): E
    fun disabled(): E
    fun accessDenied(cause: SecurityException): E
}
