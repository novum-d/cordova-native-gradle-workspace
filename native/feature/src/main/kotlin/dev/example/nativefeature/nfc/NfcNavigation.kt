package dev.example.nativefeature.nfc

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.example.nativefeature.navigation.FeatureDestination
import dev.example.nativefeature.navigation.featureArgumentTypes

/** NFC読み取り・書き込みの画面を共通NavHostへ登録する。 */
internal fun NavGraphBuilder.nfcNavigation(onClose: () -> Unit) {
    composable<FeatureDestination.NfcRead>(typeMap = featureArgumentTypes) {
        NfcReadRoute(onBack = onClose)
    }
    composable<FeatureDestination.NfcWrite>(typeMap = featureArgumentTypes) {
        NfcWriteRoute(onBack = onClose)
    }
}

/** 現在の画面に属するViewModelを取得し、SavedStateHandle由来の引数を表示へ渡す。 */
@Composable
private fun NfcReadRoute(
    onBack: () -> Unit,
    viewModel: NfcReadViewModel = viewModel(),
) {
    NfcReadScreen(request = viewModel.request, onBack = onBack)
}

/** 書き込み画面専用のViewModelを使い、読み取り画面の状態と分離する。 */
@Composable
private fun NfcWriteRoute(
    onBack: () -> Unit,
    viewModel: NfcWriteViewModel = viewModel(),
) {
    NfcWriteScreen(request = viewModel.request, onBack = onBack)
}
