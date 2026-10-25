package dev.example.nativefeature.camera

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import dev.example.nativefeature.navigation.FeatureDestination
import dev.example.nativefeature.navigation.featureArgumentTypes

/** Camera撮影画面の処理ID・argsをSavedStateHandleから復元する。実際の機器操作は未実装。 */
internal class CameraCaptureViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    /** Navigationと共通の型対応表で復元した、この画面専用の型付き引数。 */
    val request = savedStateHandle.toRoute<FeatureDestination.CameraCapture>(typeMap = featureArgumentTypes)
}
