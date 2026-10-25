package dev.example.nativefeature.dev

/** 固定assetsの起動要求を選ぶ。外部入力から任意のファイル名・要求JSON・遷移先は受け取らない。 */
internal enum class IntentSample(val key: String, val title: String) {
    WebView("webview", "WebView通信"),
    GolfHolder("golf-holder", "ゴルフ: 空のargs"),
    GolfOmittedArgs("golf-omitted-args", "ゴルフ: args省略"),
    GolfStructuredArgs("golf-structured-args", "ゴルフ: 入れ子のargs"),
    Home("home", "ホーム"),
    InvalidId("invalid-id", "不正要求: 未対応ID"),
    InvalidArgs("invalid-args", "不正要求: argsが配列"),
    ;

    /** コマンド指定もUIと同じ有限のサンプルだけから選ぶ。 */
    companion object {
        fun fromKey(key: String): IntentSample? = entries.firstOrNull { it.key == key }
    }
}
