// Compile-only validation fixture; never part of a normal build.
package dev.example.verification.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.work.WorkManager
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.serialization.json.Json
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import timber.log.Timber
import javax.inject.Inject
import kotlinx.serialization.Serializable

internal fun compileApiReferences(
    store: DataStore<Preferences>,
    work: WorkManager,
    scope: CoroutineScope,
    json: Json,
    client: OkHttpClient,
    interceptor: HttpLoggingInterceptor,
    retrofit: Retrofit,
    tree: Timber.Tree,
): Result<List<Any>, String> = Ok(listOf(store, work, scope, json, client, interceptor, retrofit, tree))

internal fun compilePersistentMap(): PersistentMap<String, Int> = persistentMapOf("before" to 1).put("after", 2)

internal fun compilePersistentSet(): PersistentSet<String> = persistentHashSetOf("before").add("after")

internal fun compileTimestamp(): Instant = Instant.fromEpochMilliseconds(0)

internal fun compileStoredDate(): LocalDate = LocalDate.parse("2026-10-19")

@Serializable
internal data class VerificationRecord(val occurredAt: Instant)

internal fun compileGeneratedSerializer(): String =
    Json.encodeToString(VerificationRecord.serializer(), VerificationRecord(compileTimestamp()))

public class VerificationRepository @Inject constructor() {
    public fun timestamp(): Instant = Instant.fromEpochMilliseconds(0)
}
