package dev.example.nativefeature.usecase.golfchange

import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import dev.example.nativedata.err.nfc.NfcCommunication
import dev.example.nativefeature.err.golfchange.ReadGolfCourseNumErr
import dev.example.nativefeature.testing.golfchange.TestCardRepositoryFactory
import dev.example.nativefeature.testing.golfchange.TestDetectedTag
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** 格納形式に依存せず、Repositoryの業務値とユースケース固有の失敗をそのまま返す。 */
class ReadGolfCourseNumUseCaseTest {
    private val repositories = TestCardRepositoryFactory()
    private val target = TestDetectedTag()

    /** Repositoryが解釈済みの番号を返し、変換や補正を重ねない。 */
    @Test
    fun returnsCourseNumFromRepository() = runTest {
        repositories.courseNums[target] = 513
        val useCase = GolfCardReadUseCaseFactory(repositories).courseNum(target)

        assertEquals(513, useCase().unwrap())
        assertEquals(listOf(target to "course"), repositories.calls)
    }

    /** 元の原因とユースケース固有型・共通契約を保持する。 */
    @Test
    fun retainsUseCaseOriginWithoutWrapping() = runTest {
        val cause = IOException("read course")
        repositories.err = cause
        val useCase = GolfCardReadUseCaseFactory(repositories).courseNum(target)
        val error = useCase().unwrapError()

        assertTrue(error is ReadGolfCourseNumErr.NfcRead.Communication)
        assertTrue(error is NfcCommunication)
        assertSame(cause, (error as NfcCommunication).cause)
    }

    /** 未対応タグを数値0に置き換えない。 */
    @Test
    fun retainsUnsupportedTagErr() = runTest {
        repositories.unsupported = true
        val useCase = GolfCardReadUseCaseFactory(repositories).courseNum(target)
        assertSame(ReadGolfCourseNumErr.NfcRead.UnsupportedTag, useCase().unwrapError())
    }
}
