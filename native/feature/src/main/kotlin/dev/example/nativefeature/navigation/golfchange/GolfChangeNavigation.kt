package dev.example.nativefeature.navigation.golfchange

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.example.nativefeature.navigation.FeatureDestination
import dev.example.nativefeature.navigation.featureArgumentTypes
import dev.example.nativefeature.ui.golfchange.GolfCardReadScreen
import dev.example.nativefeature.ui.golfchange.GolfCardReadViewModel

/** ゴルフ場釣り銭機サービスの画面を登録し、サービスごとに画面構成と遷移を管理する。 */
internal fun NavGraphBuilder.golfChangeNavigation(onClose: () -> Unit) {
    composable<FeatureDestination.GolfHolderRead>(typeMap = featureArgumentTypes) {
        GolfCardReadRoute(onBack = onClose)
    }
}

/** 前面の画面だけが検出を購読し、引数と読取状態を表示へ渡す。 */
@Composable
private fun GolfCardReadRoute(
    onBack: () -> Unit,
    viewModel: GolfCardReadViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle(minActiveState = Lifecycle.State.RESUMED)
    GolfCardReadScreen(request = viewModel.request, state = state, onBack = onBack)
}
