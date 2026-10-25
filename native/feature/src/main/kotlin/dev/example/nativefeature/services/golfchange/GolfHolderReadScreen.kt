package dev.example.nativefeature.services.golfchange

import androidx.compose.runtime.Composable
import dev.example.nativefeature.common.FeatureRequestScreen
import dev.example.nativefeature.navigation.FeatureDestination

/** ゴルフ場ID・ホルダー番号の読み取り画面を表示する。実際の読み取りや結果返却は未実装。 */
@Composable
internal fun GolfHolderReadScreen(
    request: FeatureDestination.GolfHolderRead,
    onBack: () -> Unit,
) {
    FeatureRequestScreen(
        title = "ゴルフ場ID・ホルダー番号 読み取り画面",
        id = request.id,
        args = request.args,
        onBack = onBack,
    )
}
