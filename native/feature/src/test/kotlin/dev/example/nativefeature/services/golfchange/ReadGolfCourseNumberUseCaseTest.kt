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

/** 物理座標に依存せず、Repositoryのバイト列をLSBファーストの数値として解釈する。 */
class ReadGolfCourseNumberUseCaseTest {
    private val repositories = GolfCardTestRepositories()
    private val target = TestDetectedTag()

    /** 01 02を258ではなく513と解釈する。 */
    @Test
    fun interpretsCourseNumberAsLittleEndian() = runTest {
        repositories.courseBytes[target] = byteArrayOf(1, 2)
        val useCase = GolfCardReadUseCaseFactory(repositories).courseNumber(target)

        assertEquals(513, useCase().unwrap())
        assertEquals(listOf(target to "course"), repositories.calls)
    }

    /** 符号境界と最大値を含め、0..65535のIntとして扱う。 */
    @Test
    fun preservesUnsignedBoundaries() = runTest {
        for (expected in listOf(0, 1, 255, 256, 32767, 32768, 65535)) {
            repositories.courseBytes[target] = byteArrayOf(expected.toByte(), (expected ushr 8).toByte())
            val useCase = GolfCardReadUseCaseFactory(repositories).courseNumber(target)
            assertEquals(expected, useCase().unwrap())
        }
    }

    /** 元の原因とユースケース固有型・共通契約を保持する。 */
    @Test
    fun retainsUseCaseOriginWithoutWrapping() = runTest {
        val cause = IOException("read course")
        repositories.failure = cause
        val useCase = GolfCardReadUseCaseFactory(repositories).courseNumber(target)
        val error = useCase().unwrapError()

        assertTrue(error is ReadGolfCourseNumberErr.NfcRead.Communication)
        assertTrue(error is NfcCommunication)
        assertSame(cause, (error as NfcCommunication).cause)
    }

    /** 未対応タグを数値0に置き換えない。 */
    @Test
    fun retainsUnsupportedTagFailure() = runTest {
        repositories.unsupported = true
        val useCase = GolfCardReadUseCaseFactory(repositories).courseNumber(target)
        assertSame(ReadGolfCourseNumberErr.NfcRead.UnsupportedTag, useCase().unwrapError())
    }
}
