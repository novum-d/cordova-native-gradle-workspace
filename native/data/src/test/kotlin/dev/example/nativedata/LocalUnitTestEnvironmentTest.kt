package dev.example.nativedata

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/** Coroutinesの仮想時間と、テストソースへのSerialization Plugin適用を確認するJVM環境テスト。 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocalUnitTestEnvironmentTest {
    /** Flow内のdelayを仮想時間で進め、受信値の順番と経過時間を確認する。 */
    @Test
    fun flowDelaysUseVirtualTime() = runTest {
        val states = flow {
            emit("loading")
            delay(5_000)
            emit("ready")
        }.toList()

        assertEquals(listOf("loading", "ready"), states)
        assertEquals(5_000L, testScheduler.currentTime)
    }

    /** Datetimeを含むテスト用データのSerializer生成と、JSONの往復変換を確認する。 */
    @Test
    fun testSourcesGenerateASerializerForDatetimeRecords() {
        val record = StoredRecord("example", Instant.parse("2026-10-20T00:00:00Z"))

        val json = Json.encodeToString(record)
        val restored = Json.decodeFromString<StoredRecord>(json)

        assertEquals("""{"name":"example","updatedAt":"2026-10-20T00:00:00Z"}""", json)
        assertEquals(record, restored)
    }

    /** テストソースにもSerializerが生成されることを確かめるための最小データ。 */
    @Serializable
    private data class StoredRecord(val name: String, val updatedAt: Instant)
}
