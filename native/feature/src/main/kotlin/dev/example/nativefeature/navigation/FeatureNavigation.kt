package dev.example.nativefeature.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.example.nativefeature.home.HomeScreen
import dev.example.nativefeature.services.golfchange.golfChangeNavigation
import dev.example.nativefeature.webview.WebViewSampleScreen

/**
 * 一つのNavHostにサービス別の画面を登録し、起動コマンドで表示先を切り替える。
 *
 * NavHostを作り直さず、現在の画面と引数はNavController・SavedStateHandleから復元する。
 * 新しい要求では履歴を置き換え、戻る操作で起動元のCordovaへ戻れるようにする。
 */
@Composable
internal fun FeatureNavigation(
    command: FeatureLaunchCommand?,
    onCommandHandled: (FeatureLaunchCommand) -> Unit,
    onClose: () -> Unit,
) {
    val navController = rememberNavController()
    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = FeatureDestination.Home) {
            composable<FeatureDestination.Home> {
                HomeScreen()
            }
            composable<FeatureDestination.WebViewSample> {
                WebViewSampleScreen(onClose = onClose)
            }
            golfChangeNavigation(onClose = onClose)
        }
    }
    LaunchedEffect(command) {
        command?.let { pending ->
            navController.navigate(pending.destination) {
                popUpTo(navController.graph.id) { inclusive = false }
            }
            onCommandHandled(pending)
        }
    }
}
