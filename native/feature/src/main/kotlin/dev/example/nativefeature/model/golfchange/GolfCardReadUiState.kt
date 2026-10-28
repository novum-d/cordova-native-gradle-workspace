package dev.example.nativefeature.model.golfchange

/** 検出待ちと、発生元が分かる業務の結果を表す画面状態。 */
internal sealed interface GolfCardReadUiState {
    data object Waiting : GolfCardReadUiState
    data class ResultReceived(val event: GolfCardReadEvent) : GolfCardReadUiState
}
