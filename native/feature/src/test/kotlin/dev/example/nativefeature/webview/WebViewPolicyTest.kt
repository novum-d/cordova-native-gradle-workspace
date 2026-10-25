package dev.example.nativefeature.webview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 許可オリジンと許可ページを混同せず、外部遷移・偽装URL・別ページを拒否する。 */
class WebViewPolicyTest {
    private val session = "00000000-0000-0000-0000-000000000001"

    /** 許可元はHTTPSの既定ポートだけに限定する。 */
    @Test
    fun validatesOrigin() {
        assertTrue(WebViewPolicy.isOrigin(WebViewPolicy.ORIGIN))
        assertTrue(WebViewPolicy.isOrigin("${WebViewPolicy.ORIGIN}:443"))
        listOf(
            "http://appassets.androidplatform.net",
            "https://appassets.androidplatform.net:444",
            "https://appassets.androidplatform.net.evil.test",
            "https://appassets.androidplatform.net@evil.test",
            "https://user@appassets.androidplatform.net",
            "${WebViewPolicy.ORIGIN}/assets/index.html",
            "null",
            "[",
        ).forEach { assertFalse(it, WebViewPolicy.isOrigin(it)) }
    }

    /** ページ識別子が異なる再読み込み前のページや、同一オリジンの別資源を拒否する。 */
    @Test
    fun limitsDocumentAndSession() {
        val page = "${WebViewPolicy.PAGE_URL}?session=$session"
        assertTrue(WebViewPolicy.isPage(page, session))
        listOf(
            WebViewPolicy.PAGE_URL,
            "$page&other=1",
            "$page#fragment",
            "$page-old",
            "${WebViewPolicy.ORIGIN}/assets/other.html?session=$session",
            "${WebViewPolicy.ORIGIN}/assets/../assets/index.html?session=$session",
            "file:///android_asset/index.html",
            "content://sample",
            "javascript:alert(1)",
        ).forEach { assertFalse(it, WebViewPolicy.isPage(it, session)) }
    }

    /** API以外のホスト・パス・認証情報・クエリを外部通信に使用しない。 */
    @Test
    fun limitsApiEndpoint() {
        assertTrue(WebViewPolicy.isApi(WebViewPolicy.API_URL))
        listOf(
            "${WebViewPolicy.API_URL}?extra=1",
            "${WebViewPolicy.API_URL}/",
            "${WebViewPolicy.API_URL}#part",
            "https://jsonplaceholder.typicode.com/users/2",
            "http://jsonplaceholder.typicode.com/users/1",
            "https://user@jsonplaceholder.typicode.com/users/1",
            "https://evil.test/users/1",
        ).forEach { assertFalse(it, WebViewPolicy.isApi(it)) }
    }
}
