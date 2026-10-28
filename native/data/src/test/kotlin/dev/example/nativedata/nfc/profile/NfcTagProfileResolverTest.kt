package dev.example.nativedata.nfc.profile

import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import dev.example.nativedata.nfc.detection.DetectedNfcTagHandle
import dev.example.nativedata.nfc.memory.NfcBlockMemory
import dev.example.nativedata.nfc.memory.NfcMemoryDataSource
import dev.example.nativedata.nfc.memory.NfcMemoryPosition
import dev.example.nativedata.nfc.memory.NfcMemoryRange
import dev.example.nativedata.nfc.memory.NfcTagAccess
import dev.example.nativedata.nfc.profile.icodesli.IcodeSliTagProfile
import dev.example.nativedata.nfc.profile.icodesli.IcodeSliTagProfileIdentifier
import dev.example.nativedata.nfc.reader.NfcCardReaderFactory
import dev.example.nativedata.nfc.testing.FailureKind
import dev.example.nativedata.nfc.testing.FakeNfcTag
import dev.example.nativedata.nfc.testing.TestNfcFailures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CancellationException

/** 登録追加・技術別の振分け・曖昧な識別・取消を、実製品を追加せず試験用の判定器で検証する。 */
@OptIn(ExperimentalCoroutinesApi::class)
class NfcTagProfileResolverTest {
    /** 登録がなければSLIを暗黙に既定値として選ばない。 */
    @Test
    fun returnsNullWhenNoIdentifierIsRegistered() = runTest {
        assertNull(NfcTagProfileResolver(emptySet()).identify(FakeNfcTag()))
    }

    /** 共通部へUID長やプレフィックスを固定せず、追加した判定器の条件を使う。 */
    @Test
    fun addsIdentificationRulesWithoutChangingTheResolver() = runTest {
        val added = TestProfileIdentifier(probe = { tag ->
            if (tag.uid.contentEquals(byteArrayOf(1, 2))) IcodeSliTagProfile.profile else null
        })
        val resolver = NfcTagProfileResolver(setOf(IcodeSliTagProfileIdentifier(), added))

        assertSame(IcodeSliTagProfile.profile, resolver.identify(FakeNfcTag(uid = byteArrayOf(1, 2))))
        assertEquals(1, added.calls)
    }

    /** 必要なAndroid技術がなければ判定器を呼ばず、別の対応する判定器を使う。 */
    @Test
    fun skipsIdentifiersForOtherTechnologies() = runTest {
        val otherRf = TestProfileIdentifier(requiredTechnologies = setOf("android.nfc.tech.NfcF"))
        val resolver = NfcTagProfileResolver(setOf(otherRf, IcodeSliTagProfileIdentifier()))

        assertSame(IcodeSliTagProfile.profile, resolver.identify(FakeNfcTag()))
        assertEquals(0, otherRf.calls)
    }

    /** 複数の技術が必要な判定器では、一部だけの一致で識別を始めない。 */
    @Test
    fun requiresAllDeclaredTechnologies() = runTest {
        val identifier = TestProfileIdentifier(
            requiredTechnologies = setOf("android.nfc.tech.NfcV", "android.nfc.tech.IsoDep"),
        )

        assertNull(NfcTagProfileResolver(setOf(identifier)).identify(FakeNfcTag()))
        assertEquals(0, identifier.calls)
    }

    /** 判定器が誤ったProfileを返しても、実タグにない通信方式を選択しない。 */
    @Test
    fun rejectsProfilesWhoseProtocolDoesNotMatchActualTechnology() = runTest {
        val identifier = TestProfileIdentifier(requiredTechnologies = setOf("android.nfc.tech.NfcA"))
        val tag = FakeNfcTag(technologies = setOf("android.nfc.tech.NfcA"))

        assertNull(NfcTagProfileResolver(setOf(identifier)).identify(tag))
        assertEquals(1, identifier.calls)
    }

    /** 複数の一致は、Setの列挙順を入れ替えても一方へ決めつけない。 */
    @Test
    fun rejectsAmbiguousMatchesIndependentlyOfRegistrationOrder() = runTest {
        val first = TestProfileIdentifier()
        val second = TestProfileIdentifier(probe = {
            IcodeSliTagProfile.profile.copy(memory = NfcBlockMemory(blockCount = 1, bytesPerBlock = 8))
        })

        assertNull(NfcTagProfileResolver(linkedSetOf(first, second)).identify(FakeNfcTag()))
        assertNull(NfcTagProfileResolver(linkedSetOf(second, first)).identify(FakeNfcTag()))
    }

    /** 同じProfileを返す重複した判定条件も、登録の曖昧さとして扱う。 */
    @Test
    fun rejectsOverlappingIdentifiersEvenIfProfilesAreEqual() = runTest {
        val resolver = NfcTagProfileResolver(setOf(TestProfileIdentifier(), TestProfileIdentifier()))

        assertNull(resolver.identify(FakeNfcTag()))
    }

    /** 技術が一致しても型番条件が違えば、別の判定器による一致を使う。 */
    @Test
    fun allowsAnotherIdentifierWhenOneReturnsNoMatch() = runTest {
        val noMatch = TestProfileIdentifier(probe = { null })
        val resolver = NfcTagProfileResolver(setOf(noMatch, IcodeSliTagProfileIdentifier()))

        assertSame(IcodeSliTagProfile.profile, resolver.identify(FakeNfcTag()))
        assertEquals(1, noMatch.calls)
    }

    /** 将来、識別情報の非同期取得が必要な場合も共通窓口の署名を変えずに待機できる。 */
    @Test
    fun awaitsSuspendingIdentification() = runTest {
        val identifier = TestProfileIdentifier(probe = {
            delay(10)
            IcodeSliTagProfile.profile
        })

        assertSame(IcodeSliTagProfile.profile, NfcTagProfileResolver(setOf(identifier)).identify(FakeNfcTag()))
        assertEquals(10L, testScheduler.currentTime)
    }

    /** 判定器の失敗を未対応へ隠さず、呼出元のエラー生成処理へ伝える。 */
    @Test
    fun propagatesIdentificationFailures() = runTest {
        val cause = SecurityException("probe denied")
        val identifier = TestProfileIdentifier(probe = { throw cause })
        val resolver = NfcTagProfileResolver(setOf(identifier))
        val thrown = kotlin.runCatching { resolver.identify(FakeNfcTag()) }.exceptionOrNull()

        assertSame(cause, thrown)
    }

    /** キャンセルを不一致として扱わず、後続の判定器も呼び出さない。 */
    @Test
    fun propagatesCancellationWithoutTryingAnotherIdentifier() = runTest {
        val cancellation = CancellationException("probe cancelled")
        val first = TestProfileIdentifier(probe = { throw cancellation })
        val second = TestProfileIdentifier()
        val resolver = NfcTagProfileResolver(linkedSetOf(first, second))
        val thrown = kotlin.runCatching { resolver.identify(FakeNfcTag()) }.exceptionOrNull()

        assertSame(cancellation, thrown)
        assertEquals(0, second.calls)
    }

    /** RepositoryとReaderも追加登録した条件を使い、旧SLI条件へ戻って再判定しない。 */
    @Test
    fun usesTheInjectedIdentifiersThroughoutTheReadPath() = runTest {
        val resolver = NfcTagProfileResolver(setOf(TestProfileIdentifier()))
        val dataSource = NfcMemoryDataSource(resolver)
        val factory = NfcCardReaderFactory(dataSource, resolver)
        val tag = FakeNfcTag(uid = ByteArray(8) { 0x55 })
        val cardReader = factory.create(DetectedNfcTagHandle(tag), TestNfcFailures)
        val position = NfcMemoryPosition(1, 0)
        val result = cardReader.read(NfcTagModel.ICODE_SLI, NfcMemoryRange(position, position))

        assertArrayEquals(byteArrayOf(4), result.unwrap())
        assertEquals(1, tag.closeCount)
    }

    /** 共通ResolverはUID書式を限定しないが、ISO 15693の送信では8バイトを要求する。 */
    @Test
    fun validatesUidLengthInTheProtocolRatherThanTheCommonResolver() = runTest {
        val resolver = NfcTagProfileResolver(setOf(TestProfileIdentifier()))
        val dataSource = NfcMemoryDataSource(resolver)
        val tag = FakeNfcTag(uid = byteArrayOf(1, 2))
        val position = NfcMemoryPosition(1, 0)
        val result = dataSource.read(
            tag,
            IcodeSliTagProfile.profile,
            NfcMemoryRange(position, position),
            TestNfcFailures,
        )

        assertSame(IcodeSliTagProfile.profile, resolver.identify(tag))
        assertEquals(FailureKind.UNSUPPORTED, result.unwrapError().kind)
        assertTrue(tag.requests.isEmpty())
        assertEquals(0, tag.openCount)
    }
}

/** UID・技術・非同期処理を任意に差し替える試験用判定器。実タグの識別対応を増やすものではない。 */
private class TestProfileIdentifier(
    override val requiredTechnologies: Set<String> = setOf("android.nfc.tech.NfcV"),
    private val probe: suspend (NfcTagAccess) -> NfcTagProfile? = { IcodeSliTagProfile.profile },
) : NfcTagProfileIdentifier {
    var calls = 0

    override suspend fun identify(tag: NfcTagAccess): NfcTagProfile? {
        calls++
        return probe(tag)
    }
}
