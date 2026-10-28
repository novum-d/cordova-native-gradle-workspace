package dev.example.nativedata.repository.golf

import org.junit.Assert.assertEquals
import org.junit.Test

/** 格納形式の解釈をdata側で検証し、ユースケースのテストへバイト演算を持ち込まない。 */
class GolfCardDecoderTest {
    /** 01 02をLSBファーストで513と解釈する。 */
    @Test
    fun decodesCourseNumAsLittleEndian() {
        assertEquals(513, GolfCardDecoder.decodeCourseNum(byteArrayOf(1, 2)))
    }

    /** Byteの符号境界をまたいでも、0..65535の範囲を維持する。 */
    @Test
    fun preservesUnsignedBoundaries() {
        val cases = listOf(
            byteArrayOf(0, 0) to 0,
            byteArrayOf(1, 0) to 1,
            byteArrayOf(0xFF.toByte(), 0) to 255,
            byteArrayOf(0, 1) to 256,
            byteArrayOf(0xFF.toByte(), 0x7F) to 32767,
            byteArrayOf(0, 0x80.toByte()) to 32768,
            byteArrayOf(0xFF.toByte(), 0xFF.toByte()) to 65535,
        )
        for ((bytes, expected) in cases) {
            assertEquals(expected, GolfCardDecoder.decodeCourseNum(bytes))
        }
    }

    /** ホルダー番号の読取順・大文字・固定桁数を維持する。 */
    @Test
    fun preservesByteOrderAndUppercaseHex() {
        assertEquals("000102030405060708090A0B0C0D", GolfCardDecoder.decodeHolderNum(ByteArray(14) { it.toByte() }))
    }

    /** 全バイト0でも、先頭・末尾の0を削らず28桁を維持する。 */
    @Test
    fun preservesZeroBytesAndFixedWidth() {
        assertEquals("0".repeat(28), GolfCardDecoder.decodeHolderNum(ByteArray(14)))
    }

    /** 符号付きByteも、符号文字を含めず2桁で表す。 */
    @Test
    fun preservesHighBitBytes() {
        val bytes = byteArrayOf(0x80.toByte()) + ByteArray(13) { 0xFF.toByte() }

        assertEquals("80" + "FF".repeat(13), GolfCardDecoder.decodeHolderNum(bytes))
    }

    /** テキストに見える値・空白・末尾0にも、未確定の文字コードやパディング規則を適用しない。 */
    @Test
    fun doesNotDecodeOrTrimTextAndPadding() {
        val bytes = byteArrayOf(0x30, 0x31, 0x20, 0) + ByteArray(10)

        assertEquals("30312000" + "00".repeat(10), GolfCardDecoder.decodeHolderNum(bytes))
    }
}
