package dev.example.nativedata.nfc.memory

import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import dev.example.nativedata.nfc.profile.icodesli.IcodeSliTagProfile
import dev.example.nativedata.nfc.testing.ErrKind
import dev.example.nativedata.nfc.testing.FakeNfcTag
import dev.example.nativedata.nfc.testing.TestNfcErrFactory
import dev.example.nativedata.nfc.testing.testNfcProfileResolver
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/** 必要な物理ブロックだけを読み、開始・終了バイトを含む連続領域をそのまま取得する。 */
class NfcMemoryDataSourceTest {
    private val dataSource = NfcMemoryDataSource(testNfcProfileResolver())
    private val profile = IcodeSliTagProfile.profile

    /** 同一ブロックの一部だけを取り出し、UID付きREADフレームを確認する。 */
    @Test
    fun readsOnlyRequestedBytesInOneBlock() = runTest {
        val tag = FakeNfcTag()
        val range = NfcMemoryRange(NfcMemoryPosition(1, 1), NfcMemoryPosition(1, 2))

        val result = dataSource.read(tag = tag, profile = profile, range = range, errFactory = TestNfcErrFactory)

        assertArrayEquals(byteArrayOf(5, 6), result.unwrap())
        assertArrayEquals(byteArrayOf(0x22, 0x20) + tag.uid + 1.toByte(), tag.requests.single())
        assertEquals(1, tag.connectCount)
        assertEquals(1, tag.closeCount)
    }

    /** ブロック境界の前後だけをつなぎ、不要な前後ブロックや2バイト分割を追加しない。 */
    @Test
    fun readsAcrossBlockBoundaryAndSlicesBothEnds() = runTest {
        val tag = FakeNfcTag()
        val range = NfcMemoryRange(NfcMemoryPosition(1, 2), NfcMemoryPosition(2, 1))

        val result = dataSource.read(tag = tag, profile = profile, range = range, errFactory = TestNfcErrFactory)

        assertArrayEquals(byteArrayOf(6, 7, 8, 9), result.unwrap())
        assertEquals(listOf(1, 2), tag.requests.map { it.last().toInt() })
        assertEquals(1, tag.closeCount)
    }

    /** ゴルフ場番号の2バイトを読み、ブロック1以外を取得しない。 */
    @Test
    fun readsCourseNumCoordinates() = runTest {
        val tag = FakeNfcTag()
        val range = NfcMemoryRange(NfcMemoryPosition(1, 0), NfcMemoryPosition(1, 1))

        assertArrayEquals(byteArrayOf(4, 5), dataSource.read(tag, profile, range, TestNfcErrFactory).unwrap())
        assertEquals(listOf(1), tag.requests.map { it.last().toInt() })
    }

    /** ホルダー領域はブロック1の後半から4の末尾まで、14バイトのまま返す。 */
    @Test
    fun readsAllFourHolderBlocksInOrder() = runTest {
        val tag = FakeNfcTag()
        val range = NfcMemoryRange(NfcMemoryPosition(1, 2), NfcMemoryPosition(4, 3))

        assertArrayEquals(
            ByteArray(14) { (it + 6).toByte() },
            dataSource.read(tag, profile, range, TestNfcErrFactory).unwrap(),
        )
        assertEquals(listOf(1, 2, 3, 4), tag.requests.map { it.last().toInt() })
        assertEquals(1, tag.connectCount)
        assertEquals(1, tag.closeCount)
    }

    /** ユーザー領域の先頭・末尾の単一バイトも取得できる。 */
    @Test
    fun includesFirstAndLastMemoryBytes() = runTest {
        for ((block, offset, expected) in listOf(Triple(0, 0, 0), Triple(27, 3, 111))) {
            val tag = FakeNfcTag()
            val position = NfcMemoryPosition(block, offset)
            val result = dataSource.read(tag, profile, NfcMemoryRange(position, position), TestNfcErrFactory)
            assertArrayEquals(byteArrayOf(expected.toByte()), result.unwrap())
            assertEquals(1, tag.requests.size)
        }
    }

    /** 符号付きByteの上位ビットを数値変換や並べ替えで変えない。 */
    @Test
    fun preservesHighBitBytes() = runTest {
        val tag = FakeNfcTag().apply { respond = { byteArrayOf(0, 0x80.toByte(), 0xFF.toByte(), 0, 1) } }
        val range = NfcMemoryRange(NfcMemoryPosition(2, 0), NfcMemoryPosition(2, 3))

        val actual = dataSource.read(tag, profile, range, TestNfcErrFactory).unwrap()
        assertArrayEquals(byteArrayOf(0x80.toByte(), 0xFF.toByte(), 0, 1), actual)
    }

    /** 負数・バイト位置超過・SLI領域超過・逆転・整数上限の入力を、接続前に拒否する。 */
    @Test
    fun rejectsInvalidCoordinatesWithoutOpeningConnection() = runTest {
        val start = NfcMemoryPosition(1, 0)
        val invalid = listOf(
            NfcMemoryRange(NfcMemoryPosition(-1, 0), start),
            NfcMemoryRange(start, NfcMemoryPosition(28, 0)),
            NfcMemoryRange(NfcMemoryPosition(1, -1), start),
            NfcMemoryRange(start, NfcMemoryPosition(1, 4)),
            NfcMemoryRange(NfcMemoryPosition(2, 0), NfcMemoryPosition(1, 3)),
            NfcMemoryRange(NfcMemoryPosition(1, 2), NfcMemoryPosition(1, 1)),
            NfcMemoryRange(start, NfcMemoryPosition(Int.MAX_VALUE, Int.MAX_VALUE)),
        )
        for (range in invalid) {
            val tag = FakeNfcTag()
            assertEquals(range, dataSource.read(tag, profile, range, TestNfcErrFactory).unwrapError().range)
            assertEquals(0, tag.openCount)
        }
    }

    /** 非対応タグには接続自体を行わず、後続の読取を止める。 */
    @Test
    fun rejectsUnsupportedTagBeforeOpeningConnection() = runTest {
        val tag = FakeNfcTag(technologies = setOf("android.nfc.tech.NfcF"))
        val range = NfcMemoryRange(NfcMemoryPosition(1, 0), NfcMemoryPosition(1, 1))

        assertEquals(ErrKind.UNSUPPORTED, dataSource.read(tag, profile, range, TestNfcErrFactory).unwrapError().kind)
        assertEquals(0, tag.openCount)
    }
}
