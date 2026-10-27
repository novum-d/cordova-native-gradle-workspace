package dev.example.nativedata.nfc

/** 実際の接続を使わず、送信フレーム・接続回数・例外時の解放を観測するタグ。 */
internal class FakeNfcTag(
    override val uid: ByteArray = byteArrayOf(1, 2, 3, 4, 0, 1, 4, 0xE0.toByte()),
    override val technologies: Set<String> = setOf("android.nfc.tech.NfcV"),
) : NfcTagAccess {
    var openCount = 0
    var connectCount = 0
    var closeCount = 0
    val requests = mutableListOf<ByteArray>()
    var connectFailure: Throwable? = null
    var closeFailure: Throwable? = null
    var connectionAvailable = true
    var respond: (Int) -> ByteArray = { block ->
        byteArrayOf(0) + ByteArray(4) { offset -> (block * 4 + offset).toByte() }
    }

    override fun openNfcV(): NfcVConnection? {
        openCount++
        return if (connectionAvailable) Connection() else null
    }

    /** 通信開始前の失敗と、取得済み接続のcloseを別々に観測する。 */
    private inner class Connection : NfcVConnection {
        override fun connect() {
            connectCount++
            connectFailure?.let { throw it }
        }

        override fun transceive(request: ByteArray): ByteArray {
            requests.add(request.copyOf())
            return respond(request.last().toInt() and 0xFF)
        }

        override fun close() {
            closeCount++
            closeFailure?.let { throw it }
        }
    }
}
