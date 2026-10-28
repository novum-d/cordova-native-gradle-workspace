package dev.example.nativefeature.err.launch

/** 起動要求の受信・JSON解析・画面解決のどこで失敗したかを表す契約。 */
sealed interface ResolveFeatureLaunchErr {
    val cause: IllegalArgumentException? get() = null

    /** Intentから受け取る要求の不備。生の要求JSONはエラー型へ保存しない。 */
    sealed interface Request : ResolveFeatureLaunchErr {
        /** 起動要求のExtraが指定されていない。 */
        data object Missing : Request

        /** JSONの構文・必須項目・値の型が起動要求の形式に合わない。 */
        data class InvalidJson(override val cause: IllegalArgumentException) : Request
    }

    /** 解析した要求からの画面選択に関する失敗。 */
    sealed interface Destination : ResolveFeatureLaunchErr {
        /** サポートする画面に対応しない処理ID。別の画面へフォールバックしない。 */
        data class UnsupportedId(val id: String, override val cause: IllegalArgumentException) : Destination
    }
}
