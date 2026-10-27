package dev.example.nativedata.nfc

import org.junit.Assert.assertEquals
import org.junit.Test

/** SLIと同じNFC-V・製品種別を持つSLIX系を区別し、未対応タグを誤って読み取らない。 */
class NfcTagTypeTest {
    /** UIDは表示用の逆順ではなく、UID0からの配列で判定する。 */
    @Test
    fun acceptsIcodeSliWithSerialNumberBitsPreserved() {
        for (serial in listOf(0x00, 0x07, 0x20, 0xC7)) {
            val tag = FakeNfcTag()
            tag.uid[4] = serial.toByte()
            assertEquals(NfcTagType.ICODE_SLI, classifyNfcTag(tag.uid, tag.technologies))
        }
    }

    /** メーカー・製品種別01が一致しても、SLIXとSLIX2と予約値は読み取り対象外。 */
    @Test
    fun rejectsSlixAndSlix2AndReservedTypeBits() {
        for (type in listOf(0x10, 0x08, 0x18)) {
            val tag = FakeNfcTag()
            tag.uid[4] = type.toByte()
            assertEquals(NfcTagType.OTHER, classifyNfcTag(tag.uid, tag.technologies))
        }
    }

    /** 他メーカー・SLI-S・SLI-L・不正プレフィックスをSLIと扱わない。 */
    @Test
    fun rejectsOtherManufacturersAndProductTypes() {
        for ((index, value) in listOf(7 to 0xE1, 6 to 0x02, 5 to 0x02, 5 to 0x03)) {
            val tag = FakeNfcTag()
            tag.uid[index] = value.toByte()
            assertEquals(NfcTagType.OTHER, classifyNfcTag(tag.uid, tag.technologies))
        }
    }

    /** UIDの短縮・余分なデータ・表示順反転を補正せず拒否する。 */
    @Test
    fun rejectsMalformedOrReversedUids() {
        val tag = FakeNfcTag()
        val invalid = listOf(
            byteArrayOf(),
            tag.uid.take(7).toByteArray(),
            tag.uid + 0.toByte(),
            tag.uid.reversedArray(),
        )
        for (uid in invalid) {
            assertEquals(NfcTagType.OTHER, classifyNfcTag(uid, tag.technologies))
        }
    }

    /** 他技術だけのタグには、UIDが似ていてもNFC-Vコマンドを送らない。 */
    @Test
    fun rejectsFelicaAndMifareAndIsoDepAndUnknownTechnologies() {
        val uid = FakeNfcTag().uid
        for (technology in listOf("NfcF", "MifareClassic", "MifareUltralight", "NfcA", "IsoDep", "Unknown")) {
            assertEquals(NfcTagType.OTHER, classifyNfcTag(uid, setOf("android.nfc.tech.$technology")))
        }
    }
}
