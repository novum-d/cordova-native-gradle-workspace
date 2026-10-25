package dev.example.nativefeature.nfc

import androidx.compose.runtime.Composable
import dev.example.nativefeature.common.FeatureRequestScreen
import dev.example.nativefeature.navigation.FeatureDestination

/** NFC書き込みの画面名とViewModelが受け取った処理ID・argsを表示する。機器操作は行わない。 */
@Composable
internal fun NfcWriteScreen(
    request: FeatureDestination.NfcWrite,
    onBack: () -> Unit,
) {
    FeatureRequestScreen(
        title = "NFC書き込み画面",
        id = request.id,
        args = request.args,
        onBack = onBack,
    )
}
