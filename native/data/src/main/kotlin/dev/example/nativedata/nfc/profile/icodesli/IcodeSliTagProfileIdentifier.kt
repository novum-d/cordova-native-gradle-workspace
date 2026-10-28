package dev.example.nativedata.nfc.profile.icodesli

import dev.example.nativedata.nfc.memory.NfcTagAccess
import dev.example.nativedata.nfc.profile.NfcTagProfile
import dev.example.nativedata.nfc.profile.NfcTagProfileIdentifier
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
            uid.size != 8 -> null
            !uid.copyOfRange(5, 8).contentEquals(byteArrayOf(0x01, 0x04, 0xE0.toByte())) -> null
            uid[4].toInt() and 0x18 != 0 -> null
            else -> IcodeSliTagProfile.profile
        }
    }
}
