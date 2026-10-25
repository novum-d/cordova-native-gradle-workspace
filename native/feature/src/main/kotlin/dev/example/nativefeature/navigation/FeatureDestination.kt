package dev.example.nativefeature.navigation

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Navigationの文字列パスを直接扱わず、画面とその引数を型で表す。 */
@Serializable
sealed interface FeatureDestination {
    /** 引数なしで表示するホーム画面。 */
    @Serializable
    data object Home : FeatureDestination

    /** 業務サービスと区別して開く、WebMessage双方向通信の検証画面。 */
    @Serializable
    data object WebViewSample : FeatureDestination

    /**
     * ゴルフ場ID・ホルダー番号の読み取り画面へ渡す処理IDと任意のJSONオブジェクト。
     *
     * [id]は呼び出し元が指定する処理ID。[args]は省略時にnull、空オブジェクト指定時に{}を保持する。
     * argsの項目は未定のためJsonObjectで受け、文字列・配列などを誤って渡せないようにする。
     */
    @Serializable
    data class GolfHolderRead(
        val id: String,
        val args: JsonObject? = null,
    ) : FeatureDestination
}
