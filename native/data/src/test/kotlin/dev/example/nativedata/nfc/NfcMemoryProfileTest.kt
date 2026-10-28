package dev.example.nativedata.nfc

import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/** 形状を変えた試験用Profileで、SLIの4バイト定数に依存しないことを検証する。実製品の追加ではない。 */
class NfcMemoryProfileTest {
    private val reader = NfcMemoryReader(testNfcProfileResolver())
    private val profile = IcodeSliTagProfile.profile.copy(memory = NfcBlockMemory(blockCount = 2, bytesPerBlock = 8))

    /** Profileの幅で応答を検査し、ブロックをまたぐ範囲の先頭・末尾を切り出す。 */
    @Test
    fun usesProfileBlockWidthForResponseLengthAndSlicing() = runTest {
        val tag = FakeNfcTag().apply {
            respond = { block -> byteArrayOf(0) + ByteArray(8) { (block * 8 + it).toByte() } }
        }
        val range = NfcMemoryRange(NfcMemoryPosition(0, 6), NfcMemoryPosition(1, 1))
        val result = reader.read(tag, profile, range, TestNfcFailures)

        assertArrayEquals(byteArrayOf(6, 7, 8, 9), result.unwrap())
        assertEquals(listOf(0, 1), tag.requests.map { it.last().toInt() })
        assertEquals(1, tag.closeCount)
    }

    /** SLI本来の容量内でも、選択したProfileの領域外なら接続前に拒否する。 */
    @Test
    fun usesProfileCapacityInsteadOfFixedSliLimits() = runTest {
        val tag = FakeNfcTag()
        val position = NfcMemoryPosition(2, 0)
        val range = NfcMemoryRange(position, position)

        assertEquals(FailureKind.RANGE, reader.read(tag, profile, range, TestNfcFailures).unwrapError().kind)
        assertEquals(0, tag.openCount)
    }

    /** 4バイトの応答を固定で受理せず、選択Profileの幅と不一致なら拒否する。 */
    @Test
    fun rejectsResponsesThatDoNotMatchProfileBlockWidth() = runTest {
        val tag = FakeNfcTag()
        val position = NfcMemoryPosition(0, 0)
        val range = NfcMemoryRange(position, position)

        assertEquals(FailureKind.RESPONSE_FORMAT, reader.read(tag, profile, range, TestNfcFailures).unwrapError().kind)
        assertEquals(1, tag.closeCount)
    }

    /** Profileを持っていても、実タグが要求するAndroid技術を持たなければ通信しない。 */
    @Test
    fun checksActualTechnologyBeforeOpeningConnection() = runTest {
        val tag = FakeNfcTag(technologies = setOf("android.nfc.tech.NfcA"))
        val position = NfcMemoryPosition(0, 0)
        val range = NfcMemoryRange(position, position)

        assertEquals(FailureKind.UNSUPPORTED, reader.read(tag, profile, range, TestNfcFailures).unwrapError().kind)
        assertEquals(0, tag.openCount)
    }

    /** 同じNFC-Vでも、SLIX等の別モデルにSLIのProfileで読み取りを行わない。 */
    @Test
    fun checksActualModelEvenWhenNfcVIsPresent() = runTest {
        val tag = FakeNfcTag().apply { uid[4] = 0x10 }
        val position = NfcMemoryPosition(0, 0)
        val range = NfcMemoryRange(position, position)

        assertEquals(FailureKind.UNSUPPORTED, reader.read(tag, profile, range, TestNfcFailures).unwrapError().kind)
        assertEquals(0, tag.openCount)
    }

    /** 未実装の拡張READへ暗黙に切り替えず、1バイトのブロック番号を超える要求を拒否する。 */
    @Test
    fun rejectsBlockNumbersThatDoNotFitTheImplementedCommand() = runTest {
        val extended = profile.copy(memory = NfcBlockMemory(blockCount = 257, bytesPerBlock = 4))
        val tag = FakeNfcTag()
        val position = NfcMemoryPosition(256, 0)
        val range = NfcMemoryRange(position, position)

        assertEquals(FailureKind.RANGE, reader.read(tag, extended, range, TestNfcFailures).unwrapError().kind)
        assertEquals(0, tag.openCount)
    }
}
