package dev.example.nativefeature.launch

import dev.example.nativefeature.navigation.FeatureDestination
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * KotlinPluginからIntentへ渡す、処理IDと任意のJSONオブジェクトを持つ起動要求。
 *
 * JSONとの変換をこの型に集約し、Activityは[toDestination]で処理IDに対応する画面を選ぶ。
 * argsの省略と空オブジェクトを区別し、未対応IDやオブジェクト以外のargsは受け付けない。
 */
@Serializable
data class FeatureLaunchRequest(
    val id: String,
    val args: JsonObject? = null,
) {
    /** プラグインからIntentへ格納するJSONを生成する。 */
    fun toJson(): String = Json.encodeToString(serializer(), this)

    /** 処理IDを既知の型付き画面へ解決する。未対応IDで別の画面を開かない。 */
    fun toDestination(): FeatureDestination = when (id) {
        HOME -> FeatureDestination.Home
        GOLF_HOLDER_READ -> FeatureDestination.GolfHolderRead(id = id, args = args)
        else -> throw IllegalArgumentException("Unsupported feature process ID: $id")
    }

    /** JSとIntentの境界で使う処理IDと、型付きJSONへの変換を公開する。 */
    companion object {
        const val HOME = "home"
        const val GOLF_HOLDER_READ = "golf-change.read-holder"

        /** 必須idの欠落やargsの型違いを検出し、起動要求を復元する。 */
        fun fromJson(value: String): FeatureLaunchRequest = Json.decodeFromString<FeatureLaunchRequest>(value)
    }
}
