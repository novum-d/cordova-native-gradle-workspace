package dev.example.nativedata.nfc.memory

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
import dev.example.nativedata.constant.binary.BinaryConstants
import dev.example.nativedata.err.nfc.NfcReadErr
import dev.example.nativedata.err.nfc.NfcReadErrFactory
import dev.example.nativedata.err.nfc.nfcCall
import dev.example.nativedata.err.nfc.nfcErr
import dev.example.nativedata.nfc.profile.NfcReadProtocol
import dev.example.nativedata.nfc.profile.NfcTagProfile
import dev.example.nativedata.nfc.profile.NfcTagProfileResolver
import dev.example.nativedata.nfc.protocol.Iso15693Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Readerが選んだ型番Profileから通信手順・ブロック幅を決め、要求範囲の生バイトを返す。
 * 現在の通信実装はISO 15693のAddressed Single Block READのみ。
 * 1呼び出しごとに接続し、成功・失敗・キャンセルのいずれでも、取得した接続を一度だけ閉じる。
 */
internal class NfcMemoryDataSource @Inject constructor(
    private val profileResolver: NfcTagProfileResolver,
) {
    /**
     * 0始まり・終了を含む範囲を読む。Profileと実タグのモデル・Android技術の整合も確認する。
     * ブロッキングI/OはDispatchers.IOで行い、キャンセルは結果型へ変換せず伝播する。
     */
    suspend fun <E : NfcReadErr> read(
        tag: NfcTagAccess,
        profile: NfcTagProfile,
        range: NfcMemoryRange,
        errFactory: NfcReadErrFactory<E>,
    ): Result<ByteArray, E> = withContext(Dispatchers.IO) {
        if (!range.isWithin(profile.memory) || range.endInclusive.blockNum > Iso15693Constants.MAX_SINGLE_BLOCK_NUM) {
            return@withContext Err(errFactory.invalidRange(range))
        }
        nfcCall(errFactory) {
            val uid = tag.uid.copyOf()
            val actual = profileResolver.identify(tag)
            if (actual?.model == profile.model && actual.protocol == profile.protocol) uid else null
        }.andThen { uid ->
            if (uid == null) {
                Err(errFactory.unsupportedTag())
            } else {
                when (profile.protocol) {
                    NfcReadProtocol.ISO_15693 -> readIso15693(tag, uid, profile.memory, range, errFactory)
                }
            }
        }
    }

    /** 接続APIとコマンドの組合せを通信方式の分岐内に閉じ、接続の解放を一度だけ行う。 */
    private suspend fun <E : NfcReadErr> readIso15693(
        tag: NfcTagAccess,
        uid: ByteArray,
        memory: NfcBlockMemory,
        range: NfcMemoryRange,
        errFactory: NfcReadErrFactory<E>,
    ): Result<ByteArray, E> {
        // UIDの長さは共通Resolverではなく、ISO 15693の送信形式として検査する。
        if (uid.size != Iso15693Constants.UID_BYTE_COUNT) return Err(errFactory.unsupportedTag())
        return nfcCall(errFactory) { tag.openNfcV() }.andThen { connection ->
            if (connection == null) {
                Err(errFactory.unsupportedTag())
            } else {
                useConnection(connection, errFactory) {
                    readConnected(connection, uid, memory, range, errFactory)
                }
            }
        }
    }

    /**
     * UID指定のSingle Block READを順に送り、必要部分だけを返す。途中までのデータは返さない。
     * bindingの外で現在のコルーチンのContextを取得し、ブロック間の取消確認に使う。
     */
    private suspend fun <E : NfcReadErr> readConnected(
        connection: NfcVConnection,
        uid: ByteArray,
        geometry: NfcBlockMemory,
        range: NfcMemoryRange,
        errFactory: NfcReadErrFactory<E>,
    ): Result<ByteArray, E> {
        val context = currentCoroutineContext()
        return binding {
            context.ensureActive()
            nfcCall(errFactory) { connection.connect() }.bind()
            val memory = ByteArray(range.byteCount(geometry))
            var written = 0
            for (block in range.start.blockNum..range.endInclusive.blockNum) {
                context.ensureActive()
                // UIDは送信順のまま使用する。CRCはAndroid側で付加される。
                val request = byteArrayOf(
                    Iso15693Constants.READ_REQUEST_FLAGS,
                    Iso15693Constants.READ_SINGLE_BLOCK_COMMAND,
                ) + uid + block.toByte()
                val response = nfcCall(errFactory) { connection.transceive(request) }.bind()
                val bytes = readBlockData(response, geometry.bytesPerBlock, errFactory).bind()
                val from = if (block == range.start.blockNum) range.start.byteOffset else 0
                val until = if (block == range.endInclusive.blockNum) {
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
private fun <E : NfcReadErr> readBlockData(
    response: ByteArray,
    bytesPerBlock: Int,
    errFactory: NfcReadErrFactory<E>,
): Result<ByteArray, E> = when {
    response.isEmpty() -> Err(errFactory.responseFormat("Empty Read Single Block response"))
    response[Iso15693Constants.RESPONSE_FLAGS_OFFSET].toInt() and Iso15693Constants.RESPONSE_ERROR_FLAG_MASK != 0 ->
        if (response.size == Iso15693Constants.ERROR_RESPONSE_BYTE_COUNT) {
            Err(
                errFactory.commandRejected(
                    response[Iso15693Constants.ERROR_CODE_OFFSET].toInt() and BinaryConstants.UNSIGNED_BYTE_MASK,
                ),
            )
        } else {
            Err(errFactory.responseFormat("Expected response flags and one error code byte"))
        }
    response.size != bytesPerBlock + Iso15693Constants.RESPONSE_FLAGS_BYTE_COUNT ->
        Err(errFactory.responseFormat("Expected response flags and $bytesPerBlock data bytes"))
    else -> Ok(response.copyOfRange(Iso15693Constants.RESPONSE_FLAGS_BYTE_COUNT, response.size))
}

/** connect途中の失敗でもcloseし、closeの失敗で先に発生した読取エラーやキャンセルを上書きしない。 */
private suspend fun <T, E : NfcReadErr> useConnection(
    connection: NfcVConnection,
    errFactory: NfcReadErrFactory<E>,
    operation: suspend () -> Result<T, E>,
): Result<T, E> {
    val attempt = runCatching { operation() }
    val closeAttempt = runCatching { connection.close() }
    val read = attempt.getOrElse { cause ->
        closeAttempt.onFailure { closeCause -> if (cause !== closeCause) cause.addSuppressed(closeCause) }
        throw cause
    }
    read.onFailure { err ->
        closeAttempt.onFailure { closeCause ->
            err.cause?.let { cause -> if (cause !== closeCause) cause.addSuppressed(closeCause) }
        }
    }
    return read.andThen { value ->
        closeAttempt.mapError { cause -> nfcErr(cause, errFactory) }.map { value }
    }
}
