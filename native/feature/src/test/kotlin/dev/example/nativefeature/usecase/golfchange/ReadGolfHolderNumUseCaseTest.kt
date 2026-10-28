package dev.example.nativefeature.usecase.golfchange

import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import dev.example.nativedata.err.nfc.NfcCommunication
import dev.example.nativefeature.err.golfchange.ReadGolfCourseNumErr
import dev.example.nativefeature.err.golfchange.ReadGolfHolderNumErr
import dev.example.nativefeature.testing.golfchange.TestCardRepositoryFactory
import dev.example.nativefeature.testing.golfchange.TestDetectedTag
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** Repositoryが返す文字列と、呼出元を区別できる具体的な失敗を保持する。 */
class ReadGolfHolderNumUseCaseTest {
    private val repositories = TestCardRepositoryFactory()
    private val target = TestDetectedTag()

    /** 解釈済みのホルダー番号を、そのまま返して再変換しない。 */
    @Test
    fun returnsHolderNumFromRepository() = runTest {
        repositories.holderNums[target] = "000102030405060708090A0B0C0D"
        val useCase = GolfCardReadUseCaseFactory(repositories).holderNum(target)
        assertEquals("000102030405060708090A0B0C0D", useCase().unwrap())
        assertEquals(listOf(target to "holder"), repositories.calls)
    }

    /** 同じ原因でも、呼出元ごとの具体型を直接生成する。 */
    @Test
    fun retainsDistinctUseCaseOriginsForTheSameCause() = runTest {
        val cause = IOException("read holder")
        repositories.err = cause
        val factory = GolfCardReadUseCaseFactory(repositories)
        val holder = factory.holderNum(target)().unwrapError()
        val course = factory.courseNum(target)().unwrapError()

        assertTrue(holder is ReadGolfHolderNumErr.NfcRead.Communication)
        assertTrue(course is ReadGolfCourseNumErr.NfcRead.Communication)
        assertSame(cause, (holder as NfcCommunication).cause)
        assertSame(cause, (course as NfcCommunication).cause)
    }
}
