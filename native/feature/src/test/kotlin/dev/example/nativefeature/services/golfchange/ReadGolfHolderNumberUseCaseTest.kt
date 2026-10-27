package dev.example.nativefeature.services.golfchange

import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import dev.example.nativedata.nfc.IcodeSliMemoryReader
import dev.example.nativedata.nfc.NfcCommunication
import dev.example.nativedata.nfc.NfcUnsupportedTag
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** ホルダーの固定領域と、文字コード・パディングを推定しない28桁表現を検証する。 */
class ReadGolfHolderNumberUseCaseTest {
    private val useCase = ReadGolfHolderNumberUseCase(IcodeSliMemoryReader())

    /** 領域外の値を混ぜず、ブロック1の後半から4の末尾までを読取順で表現する。 */
    @Test
    fun readsExactlyFourteenHolderBytesAsUppercaseHex() = runTest {
        val tag = GolfCardTestTag()
        tag.memory.fill(0xFF.toByte())
        ByteArray(14) { it.toByte() }.copyInto(tag.memory, destinationOffset = 6)

        val holder = useCase(tag).unwrap()

        assertEquals("000102030405060708090A0B0C0D", holder)
        assertEquals(28, holder.length)
        assertEquals(listOf(1, 2, 3, 4), tag.requestedBlocks)
        assertEquals(1, tag.closeCount)
    }

    /** 先頭・末尾の0x00を削除せず、全バイト0でも28桁を維持する。 */
    @Test
    fun preservesZeroBytesAndFixedWidth() = runTest {
        assertEquals("0".repeat(28), useCase(GolfCardTestTag()).unwrap())
    }

    /** 符号付きByteの0x80や0xFFを、余分な符号・桁なしの2桁として表す。 */
    @Test
    fun preservesHighBitBytes() = runTest {
        val tag = GolfCardTestTag()
        tag.memory.fill(0xFF.toByte(), fromIndex = 6, toIndex = 20)
        tag.memory[6] = 0x80.toByte()

        assertEquals("80" + "FF".repeat(13), useCase(tag).unwrap())
    }

    /** ASCIIらしい値や空白もテキストへ復号せず、読み取ったバイトをそのまま残す。 */
    @Test
    fun doesNotDecodeOrTrimTextAndPadding() = runTest {
        val tag = GolfCardTestTag()
        byteArrayOf(0x30, 0x31, 0x20, 0).copyInto(tag.memory, destinationOffset = 6)

        assertEquals("30312000" + "00".repeat(10), useCase(tag).unwrap())
    }

    /** 同じI/O原因でもユースケースごとの具体型を保ち、共通契約として扱える。 */
    @Test
    fun retainsHolderOriginWithoutWrappingSharedCommunicationFailure() = runTest {
        val cause = IOException("read holder")
        val holderTag = GolfCardTestTag().apply { failure = cause }
        val holderError: ReadGolfHolderNumberErr = useCase(holderTag).unwrapError()
        val courseTag = GolfCardTestTag().apply { failure = cause }
        val courseError = ReadGolfCourseNumberUseCase(IcodeSliMemoryReader())(courseTag).unwrapError()

        assertTrue(holderError is ReadGolfHolderNumberErr.NfcRead.Communication)
        assertTrue(courseError is ReadGolfCourseNumberErr.NfcRead.Communication)
        assertTrue(holderError is NfcCommunication)
        assertTrue(courseError is NfcCommunication)
        assertSame(cause, (holderError as NfcCommunication).cause)
        assertSame(cause, (courseError as NfcCommunication).cause)
    }

    /** 未対応タグをホルダー番号の空文字や0として扱わない。 */
    @Test
    fun rejectsUnsupportedTagsWithHolderSpecificFailure() = runTest {
        val tag = GolfCardTestTag().apply { technologies = setOf("android.nfc.tech.MifareClassic") }
        val error = useCase(tag).unwrapError()

        assertSame(ReadGolfHolderNumberErr.NfcRead.UnsupportedTag, error)
        assertTrue(error is NfcUnsupportedTag)
        assertTrue(tag.requestedBlocks.isEmpty())
        assertEquals(0, tag.closeCount)
    }

    /** 不完全な応答をパディングしたり、一部だけのホルダー文字列を返したりしない。 */
    @Test
    fun doesNotProduceStringFromIncompleteResponse() = runTest {
        val tag = GolfCardTestTag().apply { response = byteArrayOf(0, 1) }

        assertTrue(useCase(tag).unwrapError() is ReadGolfHolderNumberErr.NfcRead.ResponseFormat)
        assertEquals(1, tag.closeCount)
    }
}
