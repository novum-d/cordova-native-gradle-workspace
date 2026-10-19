// Only copied under build/generated when -PverifyCatalog=true. No screen or Activity is implemented.
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

@Composable
internal fun compileComposeCallback(callback: () -> Unit) {
    SideEffect(callback)
}

@Composable
internal fun compileImmutableItems(items: ImmutableList<String>) {
    SideEffect { check(items.isNotEmpty()) }
}

internal fun compilePersistentUpdate(): PersistentList<String> = persistentListOf("before").add("after")

internal fun compileDisplayDate(): LocalDateTime =
    Instant.parse("2026-10-19T00:00:00Z").toLocalDateTime(TimeZone.of("Asia/Tokyo"))

@HiltViewModel
public class VerificationViewModel @Inject constructor(
    private val repository: VerificationRepository,
) : ViewModel() {
    public fun epochMillis(): Long = repository.timestamp().toEpochMilliseconds()
}

internal fun compileApiReferences(
    activity: ComponentActivity,
    owner: LifecycleOwner,
    model: ViewModel,
    assetLoader: WebViewAssetLoader,
    modifier: Modifier,
    padding: PaddingValues,
    colors: ColorScheme,
): List<Any> = listOf(activity, owner, model, assetLoader, modifier, padding, colors)
