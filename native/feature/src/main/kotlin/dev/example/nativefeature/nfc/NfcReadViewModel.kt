package dev.example.nativefeature.nfc

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import dev.example.nativefeature.navigation.FeatureDestination
import dev.example.nativefeature.navigation.featureArgumentTypes

/** NFC読み取り画面の処理ID・argsをSavedStateHandleから復元する。実際の機器操作は未実装。 */
internal class NfcReadViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    /** Navigationと共通の型対応表で復元した、この画面専用の型付き引数。 */
    val request = savedStateHandle.toRoute<FeatureDestination.NfcRead>(typeMap = featureArgumentTypes)
}
