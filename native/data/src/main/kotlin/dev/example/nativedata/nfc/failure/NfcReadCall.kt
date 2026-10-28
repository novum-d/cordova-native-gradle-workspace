package dev.example.nativedata.nfc.failure

import android.nfc.TagLostException
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import java.io.IOException
import java.util.concurrent.CancellationException

/** I/O例外だけを呼び出し元の具体型へする。キャンセル・JVM Error・予期しない実装例外は伝播する。 */
internal inline fun <T, E : NfcReadFailure> nfcCall(
    failures: NfcReadFailureFactory<E>,
    operation: () -> T,
): Result<T, E> = runCatching(operation).mapError { cause -> nfcFailure(cause, failures) }

/** TagLostはIOExceptionより先に分類し、プログラムの不具合を通信エラーへ隠さない。 */
internal fun <E : NfcReadFailure> nfcFailure(
    cause: Throwable,
    failures: NfcReadFailureFactory<E>,
): E = when (cause) {
    is CancellationException -> throw cause
    is TagLostException -> failures.tagLost(cause)
    is IOException -> failures.communication(cause)
    is SecurityException -> failures.accessDenied(cause)
    else -> throw cause
}
