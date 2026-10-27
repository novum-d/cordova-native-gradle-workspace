package dev.example.nativedata.nfc

import java.io.IOException

/** ユースケース固有の具体エラーを、共通の性質で処理するための契約。 */
interface NfcReadFailure {
    val cause: Throwable? get() = null
}

/** 検出購読の終了などによって、読取対象としての有効期間が終わった参照。 */
interface NfcTargetExpired : NfcReadFailure

/** 今回の読取対象でないタグ。FeliCa・MIFARE・SLIXなどの読取処理は実行しない。 */
interface NfcUnsupportedTag : NfcReadFailure

/** ユーザー領域外・逆転など、送信前に拒否した座標指定。 */
interface NfcInvalidRange : NfcReadFailure {
    val range: NfcMemoryRange
}

/** Androidがタグ離脱として報告した失敗。単なる無応答から離脱と断定しない。 */
interface NfcTagLost : NfcReadFailure {
    override val cause: IOException
}

/** 接続・送受信・切断時のI/O失敗。タグ離脱は先に分離する。 */
interface NfcCommunication : NfcReadFailure {
    override val cause: IOException
}

/** 空・長さ不正など、正常データとして利用できない応答。 */
interface NfcResponseFormat : NfcReadFailure {
    val description: String
}

/** タグがError flagと共に返したISO 15693のエラーコード。 */
interface NfcCommandRejected : NfcReadFailure {
    val code: Int
}

/** Androidに拒否されたタグへのアクセス。 */
interface NfcAccessDenied : NfcReadFailure {
    override val cause: SecurityException
}

/**
 * 下位層からユースケース固有の具体型を直接生成する。既成の共通エラーを包み直さない。
 * featureが実装するFactoryをdataへ渡すことで、dataからfeatureへの逆依存を避ける。
 */
interface NfcReadFailureFactory<out E : NfcReadFailure> {
    fun targetExpired(): E
    fun unsupportedTag(): E
    fun invalidRange(range: NfcMemoryRange): E
    fun tagLost(cause: IOException): E
    fun communication(cause: IOException): E
    fun responseFormat(description: String): E
    fun commandRejected(code: Int): E
    fun accessDenied(cause: SecurityException): E
}
