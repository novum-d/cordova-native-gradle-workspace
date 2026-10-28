package dev.example.nativedata.nfc.memory

import android.nfc.Tag
import android.nfc.tech.NfcV
import java.io.Closeable

/** 検出タグの識別情報とNFC-V接続の取得口。Android APIと模擬通信を同じDataSourceへ渡す。 */
interface NfcTagAccess {
    val uid: ByteArray
    val technologies: Set<String>

    /** まだconnectしていない接続を返す。NFC-V非対応の場合はnull。 */
    fun openNfcV(): NfcVConnection?
}

/** connectからcloseまでをDataSourceが所有する、1回の読み取り用接続。 */
interface NfcVConnection : Closeable {
    fun connect()
    fun transceive(request: ByteArray): ByteArray
}

/** NfcTagAccessの標準実装としてReaderCallbackのTagを包む。接続・通信はDataSourceのIOで行う。 */
class DefaultNfcTagAccess(private val tag: Tag) : NfcTagAccess {
    override val uid: ByteArray get() = tag.id.copyOf()
    override val technologies: Set<String> get() = tag.techList.toSet()

    override fun openNfcV(): NfcVConnection? = NfcV.get(tag)?.let(::DefaultNfcVConnection)
}

/** NfcVConnectionの標準実装。Androidの例外と応答を転送し、分類・切り出しはDataSourceへ任せる。 */
private class DefaultNfcVConnection(private val connection: NfcV) : NfcVConnection {
    override fun connect() = connection.connect()
    override fun transceive(request: ByteArray): ByteArray = connection.transceive(request)
    override fun close() = connection.close()
}
