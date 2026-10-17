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

@Composable
internal fun compileComposeCallback(callback: () -> Unit) {
    SideEffect(callback)
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
