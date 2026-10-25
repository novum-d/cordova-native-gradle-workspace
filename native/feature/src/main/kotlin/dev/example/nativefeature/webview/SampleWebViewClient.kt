package dev.example.nativefeature.webview

import android.annotation.SuppressLint
import android.net.http.SslError
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import java.io.ByteArrayInputStream

/** 固定の検証HTMLに必要なJavaScriptだけを許可し、ローカルファイル・混在コンテンツを拒否する。 */
@SuppressLint("SetJavaScriptEnabled")
internal fun configureSampleWebView(view: WebView) {
    view.settings.apply {
        javaScriptEnabled = true
        allowFileAccess = false
        allowContentAccess = false
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        javaScriptCanOpenWindowsAutomatically = false
        setSupportMultipleWindows(false)
        setGeolocationEnabled(false)
    }
}

/** 固定ページをassetsから配信し、メイン文書とサブリソースの許可を個別に確認する。 */
internal class SampleWebViewClient(
    private val bridge: WebViewBridge,
    private val assetLoader: WebViewAssetLoader,
) : WebViewClient() {
    /** iframe・外部サイト・別ローカルページへの遷移を開かず、現在の検証ページだけを許可する。 */
    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean = !request.isForMainFrame || request.method != "GET" ||
        !WebViewPolicy.isPage(request.url.toString(), bridge.session)

    /** ワーカースレッドで資源を判定する。未解決のローカルURLをネットワークへ流さない。 */
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        val url = request.url.toString()
        if (request.method == "GET" && WebViewPolicy.isPage(url, bridge.session)) {
            return assetLoader.shouldInterceptRequest(request.url) ?: blockedResponse()
        }
        if (!request.isForMainFrame && request.method == "GET" && WebViewPolicy.isApi(url)) return null
        return blockedResponse()
    }

    /** 読み込み完了では初期化せず、READYが先に到着しても接続を壊さない。 */
    override fun onPageStarted(
        view: WebView,
        url: String,
        favicon: android.graphics.Bitmap?,
    ) = bridge.pageStarted(url)

    /** サブリソースの通信失敗とメイン文書の失敗を区別する。 */
    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
        if (request.isForMainFrame) bridge.pageFailed(request.url.toString())
    }

    /** HTTPエラー文書では接続済み表示を残さない。 */
    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse,
    ) {
        if (request.isForMainFrame) bridge.pageFailed(request.url.toString())
    }

    /** TLS検証エラーは例外なく中止し、証明書チェックを迂回しない。 */
    override fun onReceivedSslError(
        view: WebView,
        handler: SslErrorHandler,
        error: SslError,
    ) {
        handler.cancel()
        bridge.pageFailed(error.url)
    }

    /** レンダラー終了後のWebViewを再利用せず、画面を閉じて開き直せる状態にする。 */
    override fun onRenderProcessGone(
        view: WebView,
        detail: RenderProcessGoneDetail,
    ): Boolean {
        bridge.close("WebViewが終了しました。画面を開き直してください")
        return true
    }

    /** 拒否したURLや受信データをエラー本文へ反映しない。 */
    private fun blockedResponse(): WebResourceResponse = WebResourceResponse(
        "text/plain",
        "UTF-8",
        403,
        "Forbidden",
        emptyMap(),
        ByteArrayInputStream("Blocked".toByteArray(Charsets.UTF_8)),
    )
}
