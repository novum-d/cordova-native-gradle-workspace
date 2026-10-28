package dev.example.nativedata.nfc.testing

import dev.example.nativedata.err.nfc.NfcReadErr
import dev.example.nativedata.err.nfc.NfcReadErrFactory
import dev.example.nativedata.nfc.memory.NfcMemoryRange
import java.io.IOException

/** Reader・DataSourceが呼び出し元のFactoryで具体エラーを作ることを観測する、テスト用の分類。 */
internal enum class ErrKind {
    EXPIRED,
    UNSUPPORTED,
    RANGE,
    TAG_LOST,
    COMMUNICATION,
    RESPONSE_FORMAT,
    REJECTED,
    ACCESS_DENIED,
}

/** 試験で必要な原因・範囲・タグ応答コードを保存する。 */
internal data class TestNfcErr(
    val kind: ErrKind,
    override val cause: Throwable? = null,
    val range: NfcMemoryRange? = null,
    val code: Int? = null,
) : NfcReadErr

/** 本番ユースケースに依存せず、Reader・DataSourceの失敗分類と元の原因を検査するFactory。 */
internal object TestNfcErrFactory : NfcReadErrFactory<TestNfcErr> {
    override fun targetExpired() = TestNfcErr(ErrKind.EXPIRED)
    override fun unsupportedTag() = TestNfcErr(ErrKind.UNSUPPORTED)
    override fun invalidRange(range: NfcMemoryRange) = TestNfcErr(ErrKind.RANGE, range = range)
    override fun tagLost(cause: IOException) = TestNfcErr(ErrKind.TAG_LOST, cause)
    override fun communication(cause: IOException) = TestNfcErr(ErrKind.COMMUNICATION, cause)
    override fun responseFormat(description: String) = TestNfcErr(ErrKind.RESPONSE_FORMAT)
    override fun commandRejected(code: Int) = TestNfcErr(ErrKind.REJECTED, code = code)
    override fun accessDenied(cause: SecurityException) = TestNfcErr(ErrKind.ACCESS_DENIED, cause)
}
