package dev.example.nativefeature.webview

import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.runCatching
import java.net.URI
import java.net.URISyntaxException

/** 検証ページとAPIだけを許可する。オリジンが一致しても別ページへブリッジを公開しない。 */
internal object WebViewPolicy {
    const val ORIGIN = "https://appassets.androidplatform.net"
    const val PAGE_URL = "$ORIGIN/assets/index.html"
    const val API_URL = "https://jsonplaceholder.typicode.com/users/1"
    const val BRIDGE_NAME = "NativeBridge"

    /** 許可オリジンをscheme・host・portまで検証する。 */
    fun isOrigin(url: String): Boolean {
        val uri = parse(url) ?: return false
        return isHttpsHost(uri, "appassets.androidplatform.net") &&
            uri.rawPath.orEmpty() in setOf("", "/") && uri.rawQuery == null && uri.rawFragment == null
    }

    /** 現在のページ識別子を持つ固定HTML以外のメインフレーム遷移を禁止する。 */
    fun isPage(
        url: String,
        session: String,
    ): Boolean {
        val uri = parse(url) ?: return false
        return isHttpsHost(uri, "appassets.androidplatform.net") &&
            uri.rawPath == "/assets/index.html" && uri.rawQuery == "session=$session" && uri.rawFragment == null
    }

    /** 外部へのサブリソース要求は検証APIのGETだけに限定する。 */
    fun isApi(url: String): Boolean {
        val uri = parse(url) ?: return false
        return isHttpsHost(uri, "jsonplaceholder.typicode.com") &&
            uri.rawPath == "/users/1" && uri.rawQuery == null && uri.rawFragment == null
    }

    /** ユーザー情報・別ポート・別スキームを使った見かけ上の一致を拒否する。 */
    private fun isHttpsHost(
        uri: URI,
        host: String,
    ): Boolean = uri.scheme.equals("https", ignoreCase = true) && uri.host.equals(host, ignoreCase = true) &&
        uri.port in setOf(-1, 443) && uri.rawUserInfo == null

    /** URL構文の失敗だけを未解決とし、取消・JVM Errorなどを不正URLへ隠さない。 */
    private fun parse(url: String): URI? = runCatching { URI(url) }.getOrElse { cause ->
        if (cause !is URISyntaxException) throw cause
        null
    }
}
