package dev.example.nativefeature.webview

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.SystemClock
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.runCatching
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.util.UUID

/** Composeに公開する検証状態。WebView・ReplyProxyは画面状態や保存状態へ含めない。 */
internal data class WebViewBridgeState(
    val status: String = "ページを読み込み中",
    val connected: Boolean = false,
    val foreground: Boolean = false,
    val user: WebMessage.UserFetched? = null,
    val sentEvents: Int = 0,
)

/**
 * 一つのWebViewの接続・ページ識別子・受信検証・送信・破棄をメインスレッドで管理する。
 * 再読み込みで古いReplyProxyを破棄し、READYを受けた現在のページにだけ返信する。
 */
internal class WebViewBridge {
    var state by mutableStateOf(WebViewBridgeState())
        private set

    @Volatile
    var session: String = UUID.randomUUID().toString()
        private set

    private var webView: WebView? = null
    private var replyProxy: JavaScriptReplyProxy? = null
    private var supported = false
    private var disposed = false
    private var expectedStart = false
    private var debugLogging = false
    private var rateWindow = 0L
    private var messageCount = 0
    private var rejectionCount = 0

    /** 設定・URL制限・受信リスナーの順で準備し、最後に固定HTMLを読み込む。 */
    fun create(context: Context): WebView {
        check(webView == null && !disposed)
        debugLogging = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (debugLogging) WebView.setWebContentsDebuggingEnabled(true)
        val view = WebView(context)
        webView = view
        configureSampleWebView(view)
        supported = WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
        view.webViewClient = SampleWebViewClient(this, assetLoader)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            WebViewCompat.addWebMessageListener(view, WebViewPolicy.BRIDGE_NAME, setOf(WebViewPolicy.ORIGIN)) {
                    source, message, origin, isMainFrame, proxy ->
                if (source === webView && isMainFrame && WebViewPolicy.isOrigin(origin.toString())) {
                    receive(message, proxy)
                }
            }
        }
        reload()
        return view
    }

    /** ページ識別子を更新し、同一URLの再表示でも新しい文書と接続を作る。 */
    fun reload() {
        if (disposed) return
        replyProxy = null
        session = UUID.randomUUID().toString()
        rejectionCount = 0
        messageCount = 0
        rateWindow = SystemClock.elapsedRealtime()
        state = WebViewBridgeState(
            status = if (supported) "READY待機中" else "このWebViewはWEB_MESSAGE_LISTENERに対応していません",
            foreground = state.foreground,
        )
        expectedStart = true
        webView?.loadUrl("${WebViewPolicy.PAGE_URL}?session=$session")
    }

    /** JavaScriptやWebView自身の再読み込みにも新しい識別子を発行し、古い非同期処理を切る。 */
    fun pageStarted(url: String) {
        if (disposed || !WebViewPolicy.isPage(url, session)) return
        if (expectedStart) {
            expectedStart = false
        } else {
            webView?.stopLoading()
            reload()
        }
    }

    /** 現在のメイン文書の失敗だけを接続エラーへ反映する。API失敗はHTML側が表示する。 */
    fun pageFailed(url: String) {
        if (WebViewPolicy.isPage(url, session)) disconnect("ページの読み込みに失敗しました")
    }

    /** READYと同じページの接続でのみ疑似イベントを送信する。実NFCは利用しない。 */
    fun sendNfc() {
        if (!state.foreground || !state.connected || disposed) return
        if (send("NFC_DETECTED")) state = state.copy(sentEvents = state.sentEvents + 1)
    }

    /** Activityの状態とWebViewの描画ライフサイクルを合わせ、非表示中の送信を防ぐ。 */
    fun setForeground(foreground: Boolean) {
        if (disposed) return
        state = state.copy(foreground = foreground)
        if (foreground) webView?.onResume() else webView?.onPause()
    }

    /** View・受信リスナー・返信先を解放する。画面破棄とレンダラー終了から重複して呼べる。 */
    fun close(status: String = "接続終了") {
        if (disposed) return
        disposed = true
        disconnect(status)
        webView?.let { view ->
            if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                WebViewCompat.removeWebMessageListener(view, WebViewPolicy.BRIDGE_NAME)
            }
            view.stopLoading()
            (view.parent as? ViewGroup)?.removeView(view)
            view.destroy()
        }
        webView = null
    }

    /** サイズ・型・頻度・ページ識別子を確認し、READY後だけ表示情報を受け入れる。 */
    private fun receive(
        message: WebMessageCompat,
        proxy: JavaScriptReplyProxy,
    ) {
        if (disposed) return
        val now = SystemClock.elapsedRealtime()
        if (now - rateWindow >= MESSAGE_WINDOW_MS) {
            rateWindow = now
            messageCount = 0
        }
        if (++messageCount > MAX_MESSAGES_PER_WINDOW || message.type != WebMessageCompat.TYPE_STRING) {
            reject()
            return
        }
        val decoded = message.data?.let(WebMessageCodec::decode)
        if (decoded == null || decoded.session != session) {
            reject()
            return
        }
        when (decoded) {
            is WebMessage.Ready -> {
                replyProxy = proxy
                state = state.copy(status = "接続済み", connected = true)
                send("CONNECTED")
            }
            is WebMessage.UserFetched -> {
                if (!state.connected || proxy !== replyProxy) {
                    reject()
                    return
                }
                state = state.copy(user = decoded)
                if (debugLogging) Timber.d("Sample user id=%d, name=%s", decoded.id, decoded.name)
            }
        }
    }

    /** ReplyProxyの送信失敗をResultで扱い、取消・JVM Errorは伝播して接続の失敗と混同しない。 */
    private fun send(type: String): Boolean {
        val proxy = replyProxy ?: return false
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return false
        return runCatching {
            proxy.postMessage(WebMessageCodec.encode(type, session))
            true
        }.getOrElse { cause ->
            if (cause is CancellationException || cause !is RuntimeException) throw cause
            disconnect("通信が切断されました。再読み込みしてください")
            false
        }
    }

    /** 接続に紐づく参照だけを破棄する。 */
    private fun disconnect(status: String) {
        replyProxy = null
        state = state.copy(status = status, connected = false)
    }

    /** 攻撃者の入力をそのまま記録せず、ページごとのログ量も制限する。 */
    private fun reject() {
        if (rejectionCount++ < MAX_REJECTION_LOGS) Timber.w("Rejected WebView message")
    }

    private companion object {
        const val MESSAGE_WINDOW_MS = 1_000L
        const val MAX_MESSAGES_PER_WINDOW = 30
        const val MAX_REJECTION_LOGS = 3
    }
}
