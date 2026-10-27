package dev.example.nativefeature.services.golfchange

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.example.nativefeature.navigation.FeatureDestination
import dev.example.nativefeature.navigation.featureArgumentTypes
import javax.inject.Inject

/** Hiltから受け取るSavedStateHandleで読み取り要求を復元する。NFC検出・読取ユースケースへの接続は未実装。 */
@HiltViewModel
internal class GolfHolderReadViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {
    /** Navigationと共通の型対応表で復元した、この画面専用の型付き引数。 */
    val request = savedStateHandle.toRoute<FeatureDestination.GolfHolderRead>(typeMap = featureArgumentTypes)
}
