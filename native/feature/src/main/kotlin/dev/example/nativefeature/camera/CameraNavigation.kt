package dev.example.nativefeature.camera

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.example.nativefeature.navigation.FeatureDestination
import dev.example.nativefeature.navigation.featureArgumentTypes

/** Cameraの画面を共通NavHostへ登録する。 */
internal fun NavGraphBuilder.cameraNavigation(onClose: () -> Unit) {
    composable<FeatureDestination.CameraCapture>(typeMap = featureArgumentTypes) {
        CameraCaptureRoute(onBack = onClose)
    }
}

/** 撮影画面に属するViewModelから、復元可能な処理ID・argsを表示へ渡す。 */
@Composable
private fun CameraCaptureRoute(
    onBack: () -> Unit,
    viewModel: CameraCaptureViewModel = viewModel(),
) {
    CameraCaptureScreen(request = viewModel.request, onBack = onBack)
}
