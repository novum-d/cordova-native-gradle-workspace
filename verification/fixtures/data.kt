// Compile-only validation fixture; never part of a normal build.
package dev.example.verification.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.work.WorkManager
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import timber.log.Timber

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
