package dev.example.nativefeature.services.golfchange

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import dev.example.nativefeature.navigation.FeatureDestination
import dev.example.nativefeature.navigation.featureArgumentTypes

/** ゴルフ場釣り銭機サービスの読み取り要求を復元する。ゴルフ場ID・ホルダー番号の取得は未実装。 */
internal class GolfHolderReadViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    /** Navigationと共通の型対応表で復元した、この画面専用の型付き引数。 */
    val request = savedStateHandle.toRoute<FeatureDestination.GolfHolderRead>(typeMap = featureArgumentTypes)
}
