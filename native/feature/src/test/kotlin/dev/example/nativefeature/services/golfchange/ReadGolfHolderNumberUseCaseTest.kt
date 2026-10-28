package dev.example.nativefeature.services.golfchange

import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import dev.example.nativedata.nfc.NfcCommunication
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** 文字コード・パディングを推定せず、Repositoryの14バイトを28桁の16進表現へ変換する。 */
class ReadGolfHolderNumberUseCaseTest {
    private val repositories = TestCardRepositoryFactory()
    private val target = TestDetectedTag()

    /** 順序・大文字・固定桁数を維持する。 */
    @Test
    fun preservesByteOrderAndUppercaseHex() = runTest {
        repositories.holderBytes[target] = ByteArray(14) { it.toByte() }
        val useCase = GolfCardReadUseCaseFactory(repositories).holderNumber(target)
        assertEquals("000102030405060708090A0B0C0D", useCase().unwrap())
        assertEquals(listOf(target to "holder"), repositories.calls)
    }

    /** 全バイト0でも、先頭・末尾の0を削らず28桁を維持する。 */
    @Test
    fun preservesZeroBytesAndFixedWidth() = runTest {
        val useCase = GolfCardReadUseCaseFactory(repositories).holderNumber(target)
        assertEquals("0".repeat(28), useCase().unwrap())
    }

    /** 符号付きByteも2桁で表す。 */
    @Test
    fun preservesHighBitBytes() = runTest {
        repositories.holderBytes[target] = ByteArray(14) { if (it == 0) 0x80.toByte() else 0xFF.toByte() }
        val useCase = GolfCardReadUseCaseFactory(repositories).holderNumber(target)
        assertEquals("80" + "FF".repeat(13), useCase().unwrap())
    }

    /** テキストに見える値や空白・末尾0をそのまま残す。 */
    @Test
    fun doesNotDecodeOrTrimTextAndPadding() = runTest {
        repositories.holderBytes[target] = byteArrayOf(0x30, 0x31, 0x20, 0) + ByteArray(10)
        val useCase = GolfCardReadUseCaseFactory(repositories).holderNumber(target)
        assertEquals("30312000" + "00".repeat(10), useCase().unwrap())
    }

    /** 同じ原因でも、呼出元ごとの具体型を直接生成する。 */
    @Test
    fun retainsDistinctUseCaseOriginsForTheSameCause() = runTest {
        val cause = IOException("read holder")
        repositories.failure = cause
        val factory = GolfCardReadUseCaseFactory(repositories)
        val holder = factory.holderNumber(target)().unwrapError()
        val course = factory.courseNumber(target)().unwrapError()

        assertTrue(holder is ReadGolfHolderNumberErr.NfcRead.Communication)
        assertTrue(course is ReadGolfCourseNumberErr.NfcRead.Communication)
        assertSame(cause, (holder as NfcCommunication).cause)
        assertSame(cause, (course as NfcCommunication).cause)
    }
}
