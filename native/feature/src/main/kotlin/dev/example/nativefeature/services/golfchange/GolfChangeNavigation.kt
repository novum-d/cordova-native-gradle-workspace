package dev.example.nativefeature.services.golfchange

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.example.nativefeature.navigation.FeatureDestination
import dev.example.nativefeature.navigation.featureArgumentTypes

/** ゴルフ場釣り銭機サービスの画面を登録し、サービスごとに画面構成と遷移を管理する。 */
internal fun NavGraphBuilder.golfChangeNavigation(onClose: () -> Unit) {
    composable<FeatureDestination.GolfHolderRead>(typeMap = featureArgumentTypes) {
        GolfHolderReadRoute(onBack = onClose)
    }
}

/** 読み取り画面に属するViewModelから、復元可能な処理ID・argsを表示へ渡す。 */
@Composable
private fun GolfHolderReadRoute(
    onBack: () -> Unit,
    viewModel: GolfHolderReadViewModel = viewModel(),
) {
    GolfHolderReadScreen(request = viewModel.request, onBack = onBack)
}
