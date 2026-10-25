package dev.example.nativefeature.camera

import androidx.compose.runtime.Composable
import dev.example.nativefeature.common.FeatureRequestScreen
import dev.example.nativefeature.navigation.FeatureDestination

/** Camera撮影の画面名とViewModelが受け取った処理ID・argsを表示する。機器操作は行わない。 */
@Composable
internal fun CameraCaptureScreen(
    request: FeatureDestination.CameraCapture,
    onBack: () -> Unit,
) {
    FeatureRequestScreen(
        title = "Camera撮影画面",
        id = request.id,
        args = request.args,
        onBack = onBack,
    )
}
