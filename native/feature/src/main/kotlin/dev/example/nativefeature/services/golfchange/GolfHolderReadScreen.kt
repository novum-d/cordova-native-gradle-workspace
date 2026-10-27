package dev.example.nativefeature.services.golfchange

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.example.nativedata.nfc.NfcAccessDenied
import dev.example.nativedata.nfc.NfcCommandRejected
import dev.example.nativedata.nfc.NfcCommunication
import dev.example.nativedata.nfc.NfcInvalidRange
import dev.example.nativedata.nfc.NfcReadFailure
import dev.example.nativedata.nfc.NfcResponseFormat
import dev.example.nativedata.nfc.NfcTagLost
import dev.example.nativedata.nfc.NfcTargetExpired
import dev.example.nativedata.nfc.NfcUnsupportedTag
import dev.example.nativefeature.common.FeatureRequestScreen
import dev.example.nativefeature.navigation.FeatureDestination
import dev.example.nativefeature.nfc.ObserveNfcTagsErr

/** 検出待ち・取得した番号・失敗時の案内を表示する。業務処理やRepositoryを直接呼ばない。 */
@Composable
internal fun GolfHolderReadScreen(
    request: FeatureDestination.GolfHolderRead,
    state: GolfHolderReadState,
    onBack: () -> Unit,
) {
    FeatureRequestScreen(
        title = "ゴルフ場ID・ホルダー番号 読み取り画面",
        id = request.id,
        args = request.args,
        onBack = onBack,
    ) {
        Text(text = state.message())
    }
}

/** 型付きの状態を、NFC実装の詳細を含めない画面向けの案内にする。 */
private fun GolfHolderReadState.message(): String = when (this) {
    GolfHolderReadState.Waiting -> "カードをかざしてください"
    is GolfHolderReadState.ResultReceived -> when (val result = event) {
        is GolfCardReadEvent.ReadCompleted -> "ゴルフ場番号: ${result.courseNumber}\nホルダー番号: ${result.holderNumber}"
        is GolfCardReadEvent.DetectionFailed -> when (result.failure) {
            ObserveNfcTagsErr.NfcDetection.NotSupported -> "この端末はNFCに対応していません"
            ObserveNfcTagsErr.NfcDetection.Disabled -> "端末設定でNFCを有効にして、カードをかざしてください"
            is ObserveNfcTagsErr.NfcDetection.AccessDenied -> "NFCの検出を開始できませんでした"
        }
        is GolfCardReadEvent.CourseReadFailed -> "ゴルフ場番号: ${readFailureMessage(result.failure)}"
        is GolfCardReadEvent.HolderReadFailed -> "ホルダー番号: ${readFailureMessage(result.failure)}"
    }
}

/** 発生元の具体型を保ったまま、共通の性質で案内を選ぶ。カード番号自体はログへ出さない。 */
private fun readFailureMessage(failure: NfcReadFailure): String = when (failure) {
    is NfcTargetExpired -> "読み取りが終了しています。もう一度かざしてください"
    is NfcUnsupportedTag -> "対応していないカードです"
    is NfcTagLost -> "カードが離れました。もう一度かざしてください"
    is NfcCommunication -> "通信できませんでした。もう一度かざしてください"
    is NfcAccessDenied -> "カードにアクセスできませんでした。もう一度かざしてください"
    is NfcCommandRejected -> "カードから読み取りを拒否されました"
    is NfcResponseFormat -> "カードの応答を読み取れませんでした"
    is NfcInvalidRange -> "読み取り範囲を確認してください"
    else -> "読み取りに失敗しました"
}
