// -PverifyCatalog=trueでだけ取り込むコンパイル用ソース。Compose・日時・Hiltの連携を確認する。
package dev.example.verification.feature

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ColorScheme
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.webkit.WebViewAssetLoader
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.example.verification.data.VerificationRepository
import javax.inject.Inject

/** Compose CompilerがComposable内のSideEffectを扱えることを確認する。 */
@Composable
internal fun compileComposeCallback(callback: () -> Unit) {
    SideEffect(callback)
}

/** Composeの引数としてImmutableListを扱い、Composeと不変コレクションの型参照を確認する。 */
@Composable
internal fun compileImmutableItems(items: ImmutableList<String>) {
    SideEffect { check(items.isNotEmpty()) }
}

/** PersistentListの更新後の戻り値を利用できることを実コンパイルで確認する。 */
internal fun compilePersistentUpdate(): PersistentList<String> = persistentListOf("before").add("after")

/** Instantを指定タイムゾーンの表示用日時へ変換するAPIを実コンパイルする。 */
internal fun compileDisplayDate(): LocalDateTime =
    Instant.parse("2026-10-19T00:00:00Z").toLocalDateTime(TimeZone.of("Asia/Tokyo"))

/** dataのRepositoryを注入するViewModelから、Hilt/KSPがFactoryを生成できることを確認する。 */
@HiltViewModel
public class VerificationViewModel @Inject constructor(
    private val repository: VerificationRepository,
) : ViewModel() {
    /** 注入されたdataの型とDatetimeの変換APIを同時に参照する。 */
    public fun epochMillis(): Long = repository.timestamp().toEpochMilliseconds()
}

/** UI・Lifecycle・WebKitなどのAPI型を参照し、Catalogの実依存がコンパイルに使えることを確認する。 */
internal fun compileApiReferences(
    activity: ComponentActivity,
    owner: LifecycleOwner,
    model: ViewModel,
    assetLoader: WebViewAssetLoader,
    modifier: Modifier,
    padding: PaddingValues,
    colors: ColorScheme,
): List<Any> = listOf(activity, owner, model, assetLoader, modifier, padding, colors)
