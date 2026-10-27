package dev.example.nativedata.nfc

import android.nfc.Tag
import android.nfc.tech.NfcV
import java.io.Closeable

/** 検出タグの識別情報とNFC-V接続の取得口。Android APIと模擬通信を同じReaderへ渡す。 */
interface NfcTagAccess {
    val uid: ByteArray
    val technologies: Set<String>

    /** まだconnectしていない接続を返す。NFC-V非対応の場合はnull。 */
    fun openNfcV(): NfcVConnection?
}

/** connectからcloseまでをReaderが所有する、1回の読み取り用接続。 */
interface NfcVConnection : Closeable {
    fun connect()
    fun transceive(request: ByteArray): ByteArray
}

/** ReaderCallbackで得たTagを包むAndroid側の入口。接続・通信はReaderのIOコンテキストで行う。 */
class AndroidNfcTagAccess(private val tag: Tag) : NfcTagAccess {
    override val uid: ByteArray get() = tag.id.copyOf()
    override val technologies: Set<String> get() = tag.techList.toSet()

    override fun openNfcV(): NfcVConnection? = NfcV.get(tag)?.let(::AndroidNfcVConnection)
}

/** Androidの例外と応答バイト列を変えずに転送し、分類・切り出しはReaderへ任せる。 */
private class AndroidNfcVConnection(private val connection: NfcV) : NfcVConnection {
    override fun connect() = connection.connect()
    override fun transceive(request: ByteArray): ByteArray = connection.transceive(request)
    override fun close() = connection.close()
}
