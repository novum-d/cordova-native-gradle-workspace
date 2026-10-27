package dev.example.nativedata.nfc

import java.io.IOException

/** Readerが呼び出し元のFactoryで具体エラーを作ることを観測する、テスト用の分類。 */
internal enum class FailureKind {
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
internal data class TestNfcFailure(
    val kind: FailureKind,
    override val cause: Throwable? = null,
    val range: NfcMemoryRange? = null,
    val code: Int? = null,
) : NfcReadFailure

/** 本番ユースケースに依存せず、Readerの失敗分類と元の原因を検査するFactory。 */
internal object TestNfcFailures : NfcReadFailureFactory<TestNfcFailure> {
    override fun targetExpired() = TestNfcFailure(FailureKind.EXPIRED)
    override fun unsupportedTag() = TestNfcFailure(FailureKind.UNSUPPORTED)
    override fun invalidRange(range: NfcMemoryRange) = TestNfcFailure(FailureKind.RANGE, range = range)
    override fun tagLost(cause: IOException) = TestNfcFailure(FailureKind.TAG_LOST, cause)
    override fun communication(cause: IOException) = TestNfcFailure(FailureKind.COMMUNICATION, cause)
    override fun responseFormat(description: String) = TestNfcFailure(FailureKind.RESPONSE_FORMAT)
    override fun commandRejected(code: Int) = TestNfcFailure(FailureKind.REJECTED, code = code)
    override fun accessDenied(cause: SecurityException) = TestNfcFailure(FailureKind.ACCESS_DENIED, cause)
}
