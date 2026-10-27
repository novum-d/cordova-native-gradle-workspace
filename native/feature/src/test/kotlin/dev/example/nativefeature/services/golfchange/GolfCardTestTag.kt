package dev.example.nativefeature.services.golfchange

import dev.example.nativedata.nfc.NfcTagAccess
import dev.example.nativedata.nfc.NfcVConnection

/** ユースケースが指定する実座標を、業務カードのメモリと照合するための模擬SLIタグ。 */
internal class GolfCardTestTag : NfcTagAccess {
    override val uid = byteArrayOf(1, 2, 3, 4, 0, 1, 4, 0xE0.toByte())
    override var technologies = setOf("android.nfc.tech.NfcV")
    val memory = ByteArray(112)
    val requestedBlocks = mutableListOf<Int>()
    var closeCount = 0
    var failure: Throwable? = null
    var response: ByteArray? = null

    override fun openNfcV(): NfcVConnection = Connection()

    /** 初期値と応答の変更で、数値解釈・文字列解釈と失敗型の伝播を確認する。 */
    private inner class Connection : NfcVConnection {
        override fun connect() = Unit

        override fun transceive(request: ByteArray): ByteArray {
            val block = request.last().toInt() and 0xFF
            requestedBlocks.add(block)
            failure?.let { throw it }
            return response ?: (byteArrayOf(0) + memory.copyOfRange(block * 4, block * 4 + 4))
        }

        override fun close() {
            closeCount++
        }
    }
}
