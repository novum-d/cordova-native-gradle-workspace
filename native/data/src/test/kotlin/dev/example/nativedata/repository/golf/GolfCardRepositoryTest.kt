package dev.example.nativedata.repository.golf

import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import dev.example.nativedata.nfc.detection.DetectedNfcTagHandle
import dev.example.nativedata.nfc.memory.NfcTagAccess
import dev.example.nativedata.nfc.testing.ErrKind
import dev.example.nativedata.nfc.testing.FakeNfcTag
import dev.example.nativedata.nfc.testing.TestNfcErrFactory
import dev.example.nativedata.nfc.testing.testNfcCardReaderFactory
import dev.example.nativedata.repository.card.DefaultCardRepositoryFactory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** 本物のRepositoryから模擬通信まで通し、物理配置・対象固定・期限切れの拒否を検証する。 */
class GolfCardRepositoryTest {
    private val repositories = DefaultCardRepositoryFactory(testNfcCardReaderFactory())

    /** 業務座標はdata側で選び、範囲外や隣の項目を混ぜない。 */
    @Test
    fun readsOnlyTheConfiguredBusinessRanges() = runTest {
        val tag = FakeNfcTag()
        val repository = repositories.createGolfCard(DetectedNfcTagHandle(tag), TestNfcErrFactory)

        assertArrayEquals(byteArrayOf(4, 5), repository.readGolfCourseNum().unwrap())
        assertArrayEquals(ByteArray(14) { (it + 6).toByte() }, repository.readGolfCardHolderNum().unwrap())
        assertEquals(listOf(1, 1, 2, 3, 4), tag.requests.map { it.last().toInt() })
        assertEquals(2, tag.closeCount)
    }

    /** 別タグのRepositoryを作っても、すでに作った対象を差し替えない。 */
    @Test
    fun keepsEachRepositoryBoundToItsOriginalTarget() = runTest {
        val firstTag = FakeNfcTag()
        val secondTag = FakeNfcTag().apply { respond = { byteArrayOf(0, 20, 21, 22, 23) } }
        val first = repositories.createGolfCard(DetectedNfcTagHandle(firstTag), TestNfcErrFactory)
        val second = repositories.createGolfCard(DetectedNfcTagHandle(secondTag), TestNfcErrFactory)

        assertArrayEquals(byteArrayOf(20, 21), second.readGolfCourseNum().unwrap())
        assertArrayEquals(byteArrayOf(4, 5), first.readGolfCourseNum().unwrap())
        assertEquals(1, firstTag.openCount)
        assertEquals(1, secondTag.openCount)
    }

    /** 無効化した参照は接続前に拒否し、物理的なタグ離脱とは区別する。 */
    @Test
    fun rejectsExpiredTargetsWithoutOpeningAConnection() = runTest {
        val tag = FakeNfcTag()
        val target = DetectedNfcTagHandle(tag)
        val repository = repositories.createGolfCard(target, TestNfcErrFactory)
        target.invalidate()

        assertEquals(ErrKind.EXPIRED, repository.readGolfCourseNum().unwrapError().kind)
        assertFalse(target.isActive)
        assertEquals(0, tag.openCount)
    }

    /** 未対応のRF・型番には、SLIの座標で試し読みしない。 */
    @Test
    fun rejectsUnsupportedTagsBeforeConnecting() = runTest {
        val tag = FakeNfcTag(technologies = setOf("android.nfc.tech.NfcF"))
        val repository = repositories.createGolfCard(DetectedNfcTagHandle(tag), TestNfcErrFactory)

        assertEquals(ErrKind.UNSUPPORTED, repository.readGolfCardHolderNum().unwrapError().kind)
        assertEquals(0, tag.openCount)
    }

    /** 不完全な応答は数値・文字列の解釈へ渡さない。 */
    @Test
    fun rejectsIncompleteDataAndClosesConnection() = runTest {
        val tag = FakeNfcTag().apply { respond = { byteArrayOf(0, 1) } }
        val repository = repositories.createGolfCard(DetectedNfcTagHandle(tag), TestNfcErrFactory)

        assertEquals(ErrKind.RESPONSE_FORMAT, repository.readGolfCardHolderNum().unwrapError().kind)
        assertEquals(1, tag.closeCount)
    }

    /** 元のI/O原因を包み直さず、Factoryが生成した失敗へ残す。 */
    @Test
    fun preservesCommunicationCause() = runTest {
        val cause = IOException("connect")
        val tag = FakeNfcTag().apply { connectErr = cause }
        val repository = repositories.createGolfCard(DetectedNfcTagHandle(tag), TestNfcErrFactory)
        val err = repository.readGolfCourseNum().unwrapError()

        assertEquals(ErrKind.COMMUNICATION, err.kind)
        assertTrue(cause === err.cause)
        assertEquals(1, tag.closeCount)
    }

    /** Profile選択でメタデータ取得が拒否されても、呼出元のアクセス拒否型と原因を返す。 */
    @Test
    fun preservesAccessDenialDuringProfileSelection() = runTest {
        val cause = SecurityException("metadata denied")
        val delegate = FakeNfcTag()
        val tag = object : NfcTagAccess by delegate {
            override val technologies: Set<String> get() = throw cause
        }
        val repository = repositories.createGolfCard(DetectedNfcTagHandle(tag), TestNfcErrFactory)
        val err = repository.readGolfCourseNum().unwrapError()

        assertEquals(ErrKind.ACCESS_DENIED, err.kind)
        assertTrue(err.cause === cause)
        assertEquals(0, delegate.openCount)
    }

    /** 識別処理の実装上の不備を、対象外タグや通信失敗へ隠さず伝播する。 */
    @Test
    fun propagatesUnexpectedMetadataErrs() = runTest {
        val cause = IllegalStateException("invalid metadata implementation")
        val tag = object : NfcTagAccess by FakeNfcTag() {
            override val uid: ByteArray get() = throw cause
        }
        val repository = repositories.createGolfCard(DetectedNfcTagHandle(tag), TestNfcErrFactory)
        val thrown = kotlin.runCatching { repository.readGolfCardHolderNum() }.exceptionOrNull()

        assertTrue(thrown is IllegalStateException)
        // IO境界のスタックトレース復元による複製も含め、元の原因が残ることを確認する。
        assertTrue(generateSequence(thrown) { it.cause }.any { it === cause })
    }
}
