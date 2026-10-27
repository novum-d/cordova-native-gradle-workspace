package dev.example.nativedata.nfc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Profileに渡す形状の境界、終了を含む長さ、整数オーバーフローを検証する。 */
class NfcMemoryRangeTest {
    /** ブロック幅の違いを反映し、末尾バイトを含む長さへ換算する。 */
    @Test
    fun calculatesInclusiveLengthUsingSelectedBlockWidth() {
        val range = NfcMemoryRange(NfcMemoryPosition(1, 2), NfcMemoryPosition(2, 1))

        assertEquals(4, range.byteCount(NfcBlockMemory(blockCount = 3, bytesPerBlock = 4)))
        assertEquals(8, range.byteCount(NfcBlockMemory(blockCount = 3, bytesPerBlock = 8)))
        assertFalse(range.isWithin(NfcBlockMemory(blockCount = 2, bytesPerBlock = 8)))
    }

    /** 最大バイト数でも途中の乗算をオーバーフローさせず、単一バイト・全範囲を扱う。 */
    @Test
    fun handlesLargeValidGeometryWithoutIntegerOverflow() {
        val memory = NfcBlockMemory(blockCount = 1, bytesPerBlock = Int.MAX_VALUE)
        val end = NfcMemoryPosition(0, Int.MAX_VALUE - 1)
        val whole = NfcMemoryRange(NfcMemoryPosition(0, 0), end)

        assertTrue(whole.isWithin(memory))
        assertEquals(Int.MAX_VALUE, whole.byteCount(memory))
        assertEquals(1, NfcMemoryRange(end, end).byteCount(memory))
        assertFalse(NfcMemoryRange(end, NfcMemoryPosition(Int.MAX_VALUE, 0)).isWithin(memory))
    }

    /** 容量がByteArrayのInt上限を超える設定は、通信処理へ到達させない。 */
    @Test(expected = IllegalArgumentException::class)
    fun rejectsOverflowingGeometry() {
        NfcBlockMemory(blockCount = Int.MAX_VALUE, bytesPerBlock = 2)
    }

    /** 存在しないブロック幅を内部設定の不備として検出する。 */
    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroBlockWidth() {
        NfcBlockMemory(blockCount = 28, bytesPerBlock = 0)
    }

    /** ブロック数0の領域を通常の読取対象として登録しない。 */
    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroBlockCount() {
        NfcBlockMemory(blockCount = 0, bytesPerBlock = 4)
    }
}
