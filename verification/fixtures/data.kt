// -PverifyCatalog=trueのときだけbuild/generatedへコピーし、dataの依存APIとコード生成を実コンパイルする。
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

/** 通信・永続化・バックグラウンド処理などの型を同時に参照し、依存の解決と型の互換性を確認する。 */
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

/** 不変Mapの生成・更新APIを現在のKotlinでコンパイルできることを確認する。 */
internal fun compilePersistentMap(): PersistentMap<String, Int> = persistentMapOf("before" to 1).put("after", 2)

/** 不変Setの生成・追加APIを実コンパイルする。 */
internal fun compilePersistentSet(): PersistentSet<String> = persistentHashSetOf("before").add("after")

/** DatetimeのInstant生成APIを参照し、標準ライブラリとの世代差を検出する。 */
internal fun compileTimestamp(): Instant = Instant.fromEpochMilliseconds(0)

/** Datetimeの日付解析APIを参照し、保存用日付型が利用できることを確認する。 */
internal fun compileStoredDate(): LocalDate = LocalDate.parse("2026-10-19")

/** Datetimeを含むデータからSerializerを生成するための検証用の型。 */
@Serializable
internal data class VerificationRecord(val occurredAt: Instant)

/** Pluginが生成したserializer()を呼び出し、runtimeとCompiler Pluginの組み合わせを確認する。 */
internal fun compileGeneratedSerializer(): String =
    Json.encodeToString(VerificationRecord.serializer(), VerificationRecord(compileTimestamp()))

/** Hilt/KSPのコンストラクタ注入と、featureからdataへの型参照を確認する検証用Repository。 */
public class VerificationRepository @Inject constructor() {
    /** ViewModel側から日時APIを呼び出すコンパイル確認用に、固定のInstantを返す。 */
    public fun timestamp(): Instant = Instant.fromEpochMilliseconds(0)
}
