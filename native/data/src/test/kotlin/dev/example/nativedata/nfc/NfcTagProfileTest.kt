package dev.example.nativedata.nfc

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** SLIと同じNFC-V・製品種別を持つSLIX系を区別し、未対応タグを誤って読み取らない。 */
class NfcTagProfileTest {
    private val resolver = testNfcProfileResolver()

    /** 選択結果1つにモデル・通信手順・メモリ構成がそろい、呼出側でISO方式を組み合わせない。 */
    @Test
    fun resolvesTheSharedIcodeSliProfileFromTagMetadata() = runTest {
        val tag = FakeNfcTag()
        val profile = requireNotNull(resolver.identify(tag))

        assertSame(IcodeSliTagProfile.profile, profile)
        assertEquals(NfcTagModel.ICODE_SLI, profile.model)
        assertEquals(NfcReadProtocol.ISO_15693, profile.protocol)
        assertEquals("android.nfc.tech.NfcV", profile.protocol.androidTechnology)
        assertEquals(NfcBlockMemory(blockCount = 28, bytesPerBlock = 4), profile.memory)
    }

    /** UIDは表示用の逆順ではなく、UID0からの配列で判定する。 */
    @Test
    fun acceptsIcodeSliWithSerialNumberBitsPreserved() = runTest {
        for (serial in listOf(0x00, 0x07, 0x20, 0xC7)) {
            val tag = FakeNfcTag()
            tag.uid[4] = serial.toByte()
            assertEquals(IcodeSliTagProfile.profile, resolver.identify(tag))
        }
    }

    /** メーカー・製品種別01が一致しても、SLIXとSLIX2と予約値は読み取り対象外。 */
    @Test
    fun rejectsSlixAndSlix2AndReservedTypeBits() = runTest {
        for (type in listOf(0x10, 0x08, 0x18)) {
            val tag = FakeNfcTag()
            tag.uid[4] = type.toByte()
            assertEquals(null, resolver.identify(tag))
        }
    }

    /** 他メーカー・SLI-S・SLI-L・不正プレフィックスをSLIと扱わない。 */
    @Test
    fun rejectsOtherManufacturersAndProductTypes() = runTest {
        for ((index, value) in listOf(7 to 0xE1, 6 to 0x02, 5 to 0x02, 5 to 0x03)) {
            val tag = FakeNfcTag()
            tag.uid[index] = value.toByte()
            assertEquals(null, resolver.identify(tag))
        }
    }

    /** UIDの短縮・余分なデータ・表示順反転を補正せず拒否する。 */
    @Test
    fun rejectsMalformedOrReversedUids() = runTest {
        val tag = FakeNfcTag()
        val invalid = listOf(
            byteArrayOf(),
            tag.uid.take(7).toByteArray(),
            tag.uid + 0.toByte(),
            tag.uid.reversedArray(),
        )
        for (uid in invalid) {
            assertEquals(null, resolver.identify(FakeNfcTag(uid = uid, technologies = tag.technologies)))
        }
    }

    /** 他技術だけのタグには、UIDが似ていてもNFC-Vコマンドを送らない。 */
    @Test
    fun rejectsFelicaAndMifareAndIsoDepAndUnknownTechnologies() = runTest {
        val uid = FakeNfcTag().uid
        for (technology in listOf("NfcF", "MifareClassic", "MifareUltralight", "NfcA", "IsoDep", "Unknown")) {
            val tag = FakeNfcTag(uid = uid, technologies = setOf("android.nfc.tech.$technology"))
            assertEquals(null, resolver.identify(tag))
        }
    }
}
