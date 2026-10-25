package dev.example.nativefeature.nfc

import androidx.compose.runtime.Composable
import dev.example.nativefeature.common.FeatureRequestScreen
import dev.example.nativefeature.navigation.FeatureDestination

/** NFC読み取りの画面名とViewModelが受け取った処理ID・argsを表示する。機器操作は行わない。 */
@Composable
internal fun NfcReadScreen(
    request: FeatureDestination.NfcRead,
    onBack: () -> Unit,
) {
    FeatureRequestScreen(
        title = "NFC読み取り画面",
        id = request.id,
        args = request.args,
        onBack = onBack,
    )
}
