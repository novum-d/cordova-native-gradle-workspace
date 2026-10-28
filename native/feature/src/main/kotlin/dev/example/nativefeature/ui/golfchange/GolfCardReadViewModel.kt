package dev.example.nativefeature.ui.golfchange

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.example.nativefeature.model.golfchange.GolfCardReadEvent
import dev.example.nativefeature.model.golfchange.GolfCardReadUiState
import dev.example.nativefeature.navigation.FeatureDestination
import dev.example.nativefeature.navigation.featureArgumentTypes
import dev.example.nativefeature.usecase.golfchange.ObserveGolfCardUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** 起動要求を復元し、業務ユースケースの通知を画面状態へ反映する。Repositoryには直接依存しない。 */
@HiltViewModel
internal class GolfCardReadViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeGolfCard: ObserveGolfCardUseCase,
) : ViewModel() {
    /** Navigationと共通の型対応表で復元した、この画面専用の型付き引数。 */
    val request = savedStateHandle.toRoute<FeatureDestination.GolfHolderRead>(typeMap = featureArgumentTypes)

    /** 画面が購読している間だけ読み取り、再表示時には過去のタグ結果を再生しない。 */
    val state = observeGolfCard().map<GolfCardReadEvent, GolfCardReadUiState> { GolfCardReadUiState.ResultReceived(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 0, replayExpirationMillis = 0),
            initialValue = GolfCardReadUiState.Waiting,
        )
}
