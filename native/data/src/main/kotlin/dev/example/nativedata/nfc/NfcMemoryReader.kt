package dev.example.nativedata.nfc

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
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

/**
 * Repositoryが選んだ型番Profileから通信手順・ブロック幅を決め、要求範囲の生バイトを返す。
 * 現在の通信実装はISO 15693のAddressed Single Block READのみ。
 * 1呼び出しごとに接続し、成功・失敗・キャンセルのいずれでも、取得した接続を一度だけ閉じる。
 */
internal class NfcMemoryReader @Inject constructor(
    private val profileResolver: NfcTagProfileResolver,
) {
    /**
     * 0始まり・終了を含む範囲を読む。Profileと実タグのモデル・Android技術の整合も確認する。
     * ブロッキングI/OはDispatchers.IOで行い、キャンセルは結果型へ変換せず伝播する。
     */
    suspend fun <E : NfcReadFailure> read(
        tag: NfcTagAccess,
        profile: NfcTagProfile,
        range: NfcMemoryRange,
        failures: NfcReadFailureFactory<E>,
    ): Result<ByteArray, E> = withContext(Dispatchers.IO) {
        if (!range.isWithin(profile.memory) || range.endInclusive.blockNumber > 0xFF) {
            return@withContext Err(failures.invalidRange(range))
        }
        nfcCall(failures) {
            val uid = tag.uid.copyOf()
            val actual = profileResolver.identify(tag)
            if (actual?.model == profile.model && actual.protocol == profile.protocol) uid else null
        }.andThen { uid ->
            if (uid == null) {
                Err(failures.unsupportedTag())
            } else {
                when (profile.protocol) {
                    NfcReadProtocol.ISO_15693 -> readIso15693(tag, uid, profile.memory, range, failures)
                }
            }
        }
    }

    /** 接続APIとコマンドの組合せを通信方式の分岐内に閉じ、接続の解放を一度だけ行う。 */
    private suspend fun <E : NfcReadFailure> readIso15693(
        tag: NfcTagAccess,
        uid: ByteArray,
        memory: NfcBlockMemory,
        range: NfcMemoryRange,
        failures: NfcReadFailureFactory<E>,
    ): Result<ByteArray, E> {
        // UIDの長さは共通Resolverではなく、ISO 15693の送信形式として検査する。
        if (uid.size != 8) return Err(failures.unsupportedTag())
        return nfcCall(failures) { tag.openNfcV() }.andThen { connection ->
            if (connection == null) {
                Err(failures.unsupportedTag())
            } else {
                useConnection(connection, failures) {
                    readConnected(connection, uid, memory, range, failures)
                }
            }
        }
    }

    /** UID指定のSingle Block READを順に送る。必要部分だけを結果へコピーし、途中失敗は返却しない。 */
    private suspend fun <E : NfcReadFailure> readConnected(
        connection: NfcVConnection,
        uid: ByteArray,
        geometry: NfcBlockMemory,
        range: NfcMemoryRange,
        failures: NfcReadFailureFactory<E>,
    ): Result<ByteArray, E> {
        val context = coroutineContext
        return binding {
            context.ensureActive()
            nfcCall(failures) { connection.connect() }.bind()
            val memory = ByteArray(range.byteCount(geometry))
            var written = 0
            for (block in range.start.blockNumber..range.endInclusive.blockNumber) {
                context.ensureActive()
                // 22=高速・Addressed・Optionなし、20=Read Single Block。UIDは送信順のまま使用する。
                // NFCAppの非Addressed READ（02 20 block）から対象指定を追加。CRCはAndroid側で付加される。
                val request = byteArrayOf(0x22, 0x20) + uid + block.toByte()
                val response = nfcCall(failures) { connection.transceive(request) }.bind()
                val bytes = readBlockData(response, geometry.bytesPerBlock, failures).bind()
                val from = if (block == range.start.blockNumber) range.start.byteOffset else 0
                val until = if (block == range.endInclusive.blockNumber) {
                    range.endInclusive.byteOffset + 1
                } else {
                    geometry.bytesPerBlock
                }
                bytes.copyInto(memory, destinationOffset = written, startIndex = from, endIndex = until)
                written += until - from
            }
            memory
        }
    }
}

/** Optionなし応答のFlagsとProfile由来のブロック幅を検査し、データだけを返す。 */
private fun <E : NfcReadFailure> readBlockData(
    response: ByteArray,
    bytesPerBlock: Int,
    failures: NfcReadFailureFactory<E>,
): Result<ByteArray, E> = when {
    response.isEmpty() -> Err(failures.responseFormat("Empty Read Single Block response"))
    response[0].toInt() and 0x01 != 0 ->
        if (response.size == 2) {
            Err(failures.commandRejected(response[1].toInt() and 0xFF))
        } else {
            Err(failures.responseFormat("Expected response flags and one error code byte"))
        }
    response.size != bytesPerBlock + 1 ->
        Err(failures.responseFormat("Expected response flags and $bytesPerBlock data bytes"))
    else -> Ok(response.copyOfRange(1, response.size))
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
