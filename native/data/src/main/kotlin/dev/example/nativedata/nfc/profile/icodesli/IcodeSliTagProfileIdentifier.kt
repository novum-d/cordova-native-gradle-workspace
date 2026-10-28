package dev.example.nativedata.nfc.profile.icodesli

import dev.example.nativedata.nfc.memory.NfcTagAccess
import dev.example.nativedata.nfc.profile.NfcTagProfile
import dev.example.nativedata.nfc.profile.NfcTagProfileIdentifier
import dev.example.nativedata.nfc.protocol.Iso15693Constants
import javax.inject.Inject

/**
 * NFC-VのUID0からの配列で、SLIとSLIX・SLIX2等を区別する。実際の通信は行わない。
 * UIDによる製品分類であり、真正性の認証ではない。
 * [NXP SLIX2 §9.2.1 Table 8](https://www.nxp.com/docs/en/data-sheet/SL2S2602.pdf)を参照。
 */
internal class IcodeSliTagProfileIdentifier @Inject constructor() : NfcTagProfileIdentifier {
    override val requiredTechnologies = setOf(IcodeSliTagProfile.profile.protocol.androidTechnology)

    /** SLI固有の長さ・メーカー・製品種別・派生型ビットを検査する。 */
    override suspend fun identify(tag: NfcTagAccess): NfcTagProfile? {
        val uid = tag.uid.copyOf()
        return when {
            !tag.technologies.containsAll(requiredTechnologies) -> null
            uid.size != Iso15693Constants.UID_BYTE_COUNT -> null
            !uid.copyOfRange(UID_HEADER_START_OFFSET, Iso15693Constants.UID_BYTE_COUNT)
                .contentEquals(expectedUidHeader) -> null
            uid[UID_VARIANT_OFFSET].toInt() and UID_VARIANT_MASK != SLI_VARIANT_BITS -> null
            else -> IcodeSliTagProfile.profile
        }
    }

    /** 既知のSLIを識別するUIDの配置・製品条件。共通Resolverや汎用バイト定数へ混ぜない。 */
    private companion object {
        const val UID_HEADER_START_OFFSET = 5
        const val PRODUCT_CODE: Byte = 0x01
        const val NXP_MANUFACTURER_CODE: Byte = 0x04
        const val UID_VARIANT_OFFSET = 4
        const val UID_VARIANT_MASK = 0x18
        const val SLI_VARIANT_BITS = 0x00

        // 比較専用として非公開に保ち、呼出元へ可変の配列を渡さない。
        val expectedUidHeader = byteArrayOf(PRODUCT_CODE, NXP_MANUFACTURER_CODE, Iso15693Constants.UID_PREFIX.toByte())
    }
}
