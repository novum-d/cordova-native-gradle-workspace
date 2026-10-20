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

/** Verifies coroutine virtual time and the test source set's serialization plugin. */
@OptIn(ExperimentalCoroutinesApi::class)
class LocalUnitTestEnvironmentTest {
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

    @Test
    fun testSourcesGenerateASerializerForDatetimeRecords() {
        val record = StoredRecord("example", Instant.parse("2026-10-20T00:00:00Z"))

        val json = Json.encodeToString(record)
        val restored = Json.decodeFromString<StoredRecord>(json)

        assertEquals("""{"name":"example","updatedAt":"2026-10-20T00:00:00Z"}""", json)
        assertEquals(record, restored)
    }

    @Serializable
    private data class StoredRecord(val name: String, val updatedAt: Instant)
}
