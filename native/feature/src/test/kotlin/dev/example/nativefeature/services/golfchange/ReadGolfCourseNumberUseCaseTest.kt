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

/** 実メモリ座標とLSBファーストの符号なし16bit値を、共通Readerを含む経路で検証する。 */
class ReadGolfCourseNumberUseCaseTest {
    private val useCase = ReadGolfCourseNumberUseCase(IcodeSliMemoryReader())

    /** Byte 0が01・Byte 1が02なら、513を返す。 */
    @Test
    fun interpretsCourseNumberAsLittleEndian() = runTest {
        val tag = GolfCardTestTag()
        tag.memory[4] = 1
        tag.memory[5] = 2
        tag.memory[6] = 99

        assertEquals(513, useCase(tag).unwrap())
        assertEquals(listOf(1), tag.requestedBlocks)
        assertEquals(1, tag.closeCount)
    }

    /** 0・符号境界・最大値を含め、0..65535のIntとして扱う。 */
    @Test
    fun preservesUnsignedBoundaries() = runTest {
        val values = listOf(0, 1, 255, 256, 32767, 32768, 65535)
        for (expected in values) {
            val tag = GolfCardTestTag()
            tag.memory[4] = (expected and 0xFF).toByte()
            tag.memory[5] = (expected ushr 8).toByte()

            assertEquals(expected, useCase(tag).unwrap())
        }
    }

    /** 共通Readerの失敗は、このユースケースの具体型として直接生成される。 */
    @Test
    fun retainsUseCaseOriginAndSharedCommunicationContract() = runTest {
        val cause = IOException("read course")
        val tag = GolfCardTestTag().apply { failure = cause }
        val error: ReadGolfCourseNumberErr = useCase(tag).unwrapError()

        assertTrue(error is ReadGolfCourseNumberErr.NfcRead.Communication)
        assertTrue(error is NfcCommunication)
        assertSame(cause, (error as NfcCommunication).cause)
    }

    /** 未対応タグも、ユースケース固有の契約と共通契約の両方で処理できる。 */
    @Test
    fun retainsUseCaseOriginForUnsupportedTags() = runTest {
        val tag = GolfCardTestTag().apply { technologies = setOf("android.nfc.tech.NfcF") }
        val error = useCase(tag).unwrapError()

        assertSame(ReadGolfCourseNumberErr.NfcRead.UnsupportedTag, error)
        assertTrue(error is NfcUnsupportedTag)
        assertTrue(tag.requestedBlocks.isEmpty())
    }

    /** 不正応答を数値0と誤認せず、原因を階層型で返す。 */
    @Test
    fun doesNotDecodeIncompleteResponse() = runTest {
        val tag = GolfCardTestTag().apply { response = byteArrayOf(0, 1) }

        assertTrue(useCase(tag).unwrapError() is ReadGolfCourseNumberErr.NfcRead.ResponseFormat)
        assertEquals(1, tag.closeCount)
    }
}
