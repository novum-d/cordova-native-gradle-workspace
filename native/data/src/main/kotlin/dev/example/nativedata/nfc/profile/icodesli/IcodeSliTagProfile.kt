package dev.example.nativedata.nfc.profile.icodesli

import dev.example.nativedata.nfc.memory.NfcBlockMemory
import dev.example.nativedata.nfc.profile.NfcReadProtocol
import dev.example.nativedata.nfc.profile.NfcTagModel
import dev.example.nativedata.nfc.profile.NfcTagProfile

/**
 * 公開仕様と共有済みTagInfoの112バイト構成に対応する、ICODE SLI固有のProfile。
 * 転記のSLI2ICS20と資料のSL2 ICS20は照合待ちのため、正式品番の確定値としては保存しない。
 */
internal object IcodeSliTagProfile {
    /** SLIユーザー領域固有の構成。同じ数値でも汎用のニブル幅などとは共有しない。 */
    private const val USER_MEMORY_BLOCK_COUNT = 28
    private const val BYTES_PER_BLOCK = 4

    val profile = NfcTagProfile(
        model = NfcTagModel.ICODE_SLI,
        protocol = NfcReadProtocol.ISO_15693,
        memory = NfcBlockMemory(blockCount = USER_MEMORY_BLOCK_COUNT, bytesPerBlock = BYTES_PER_BLOCK),
    )
}
