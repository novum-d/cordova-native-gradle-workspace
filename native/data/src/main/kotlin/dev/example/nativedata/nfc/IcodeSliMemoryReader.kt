package dev.example.nativedata.nfc

import android.nfc.TagLostException
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.runCatching
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

/**
 * ICODE SLIの必要ブロックだけを読み、指定された連続バイト列を返す。値の解釈や2バイト分割は行わない。
 * 1呼び出しごとに接続し、成功・失敗・キャンセルのいずれでも、取得した接続を一度だけ閉じる。
 */
class IcodeSliMemoryReader @Inject constructor() {
    /**
     * 0始まり・終了を含む範囲を読む。AndroidのTagはAndroidNfcTagAccessで包んで渡す。
     * ブロッキングI/OはDispatchers.IOで行い、キャンセルは結果型へ変換せず伝播する。
     */
    suspend fun <E : NfcReadFailure> read(
        tag: NfcTagAccess,
        range: NfcMemoryRange,
        failures: NfcReadFailureFactory<E>,
    ): Result<ByteArray, E> = withContext(Dispatchers.IO) {
        if (!range.isWithinIcodeSli()) {
            return@withContext Err(failures.invalidRange(range))
        }
        nfcCall(failures) {
            val uid = tag.uid.copyOf()
            if (classifyNfcTag(uid, tag.technologies) == NfcTagType.ICODE_SLI) uid else null
        }.andThen { uid ->
            if (uid == null) {
                Err(failures.unsupportedTag())
            } else {
                nfcCall(failures) { tag.openNfcV() }.andThen { connection ->
                    if (connection == null) {
                        Err(failures.unsupportedTag())
                    } else {
                        useConnection(connection, failures) {
                            readConnected(connection, uid, range, failures)
                        }
                    }
                }
            }
        }
    }

    /** UID指定のSingle Block READを順に送る。必要部分だけを結果へコピーし、途中失敗は返却しない。 */
    private suspend fun <E : NfcReadFailure> readConnected(
        connection: NfcVConnection,
        uid: ByteArray,
        range: NfcMemoryRange,
        failures: NfcReadFailureFactory<E>,
    ): Result<ByteArray, E> {
        val context = coroutineContext
        return binding {
            context.ensureActive()
            nfcCall(failures) { connection.connect() }.bind()
            val memory = ByteArray(range.byteCount)
            var written = 0
            for (block in range.start.blockNumber..range.endInclusive.blockNumber) {
                context.ensureActive()
                // 22=高速・Addressed・Optionなし、20=Read Single Block。UIDは送信順のまま使用する。
                // NFCAppの非Addressed READ（02 20 block）から対象指定を追加。CRCはAndroid側で付加される。
                val request = byteArrayOf(0x22, 0x20) + uid + block.toByte()
                val response = nfcCall(failures) { connection.transceive(request) }.bind()
                val bytes = readBlockData(response, failures).bind()
                val from = if (block == range.start.blockNumber) range.start.byteOffset else 0
                val until = if (block == range.endInclusive.blockNumber) range.endInclusive.byteOffset + 1 else 4
                bytes.copyInto(memory, destinationOffset = written, startIndex = from, endIndex = until)
                written += until - from
            }
            memory
        }
    }
}

/** Optionなし応答のFlagsを検査し、4バイトのブロックデータだけを返す。 */
private fun <E : NfcReadFailure> readBlockData(
    response: ByteArray,
    failures: NfcReadFailureFactory<E>,
): Result<ByteArray, E> = when {
    response.isEmpty() -> Err(failures.responseFormat("Empty Read Single Block response"))
    response[0].toInt() and 0x01 != 0 ->
        if (response.size == 2) {
            Err(failures.commandRejected(response[1].toInt() and 0xFF))
        } else {
            Err(failures.responseFormat("Expected response flags and one error code byte"))
        }
    response.size != 5 -> Err(failures.responseFormat("Expected response flags and four data bytes"))
    else -> Ok(response.copyOfRange(1, 5))
}

/** I/O例外だけを呼び出し元の具体型へする。キャンセル・JVM Error・予期しない実装例外は伝播する。 */
private inline fun <T, E : NfcReadFailure> nfcCall(
    failures: NfcReadFailureFactory<E>,
    operation: () -> T,
): Result<T, E> = runCatching(operation).mapError { cause -> nfcFailure(cause, failures) }

/** TagLostはIOExceptionより先に分類し、プログラムの不具合を通信エラーへ隠さない。 */
private fun <E : NfcReadFailure> nfcFailure(
    cause: Throwable,
    failures: NfcReadFailureFactory<E>,
): E = when (cause) {
    is CancellationException -> throw cause
    is TagLostException -> failures.tagLost(cause)
    is IOException -> failures.communication(cause)
    is SecurityException -> failures.accessDenied(cause)
    else -> throw cause
}

/** connect途中の失敗でもcloseし、closeの失敗で先に発生した読取エラーやキャンセルを上書きしない。 */
private suspend fun <T, E : NfcReadFailure> useConnection(
    connection: NfcVConnection,
    failures: NfcReadFailureFactory<E>,
    operation: suspend () -> Result<T, E>,
): Result<T, E> {
    val attempt = runCatching { operation() }
    val closeAttempt = runCatching { connection.close() }
    val read = attempt.getOrElse { cause ->
        closeAttempt.onFailure { closeCause -> if (cause !== closeCause) cause.addSuppressed(closeCause) }
        throw cause
    }
    read.onFailure { failure ->
        closeAttempt.onFailure { closeCause ->
            failure.cause?.let { cause -> if (cause !== closeCause) cause.addSuppressed(closeCause) }
        }
    }
    return read.andThen { value ->
        closeAttempt.mapError { cause -> nfcFailure(cause, failures) }.map { value }
    }
}
