package dev.example.nativefeature.navigation

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.reflect.typeOf

/** NavigationとSavedStateHandleで同じJSON引数の保存形式を使うための型対応表。 */
internal val featureArgumentTypes = mapOf(typeOf<JsonObject?>() to JsonObjectNavType)

/**
 * 任意のJSONオブジェクトをNavigationの型付き引数として保存・復元する。
 *
 * BundleにはJSON文字列を保存し、ルートへ含めるときだけURIエンコードする。
 * NavigationがURIをデコードした値をparseValueへ渡すため、二重デコードは行わない。
 */
internal object JsonObjectNavType : NavType<JsonObject?>(isNullableAllowed = true) {
    /** 画面の復元用BundleへJSONを格納し、省略されたargsはnullのまま保存する。 */
    override fun put(
        bundle: Bundle,
        key: String,
        value: JsonObject?,
    ) {
        bundle.putString(key, value?.toString())
    }

    /** 保存したJSONを復元する。オブジェクト以外のJSONは受け付けない。 */
    override fun get(
        bundle: Bundle,
        key: String,
    ): JsonObject? = bundle.getString(key)?.let { Json.decodeFromString<JsonObject>(it) }

    /** Navigationから渡されたJSON文字列を型付きのオブジェクトへ戻す。 */
    override fun parseValue(value: String): JsonObject? =
        if (value == "null") null else Json.decodeFromString<JsonObject>(value)

    /** JSON内の予約文字や日本語がルート構造を変えないようエンコードする。 */
    override fun serializeAsValue(value: JsonObject?): String = value?.let { Uri.encode(it.toString()) } ?: "null"
}
