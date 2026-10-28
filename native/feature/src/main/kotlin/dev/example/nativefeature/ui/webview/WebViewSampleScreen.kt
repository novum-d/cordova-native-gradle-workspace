package dev.example.nativefeature.ui.webview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.example.nativefeature.webview.WebViewBridge

/** 通信用の仮UI。WebViewは再コンポジションで再生成せず、画面の離脱時に破棄する。 */
@Composable
internal fun WebViewSampleScreen(onClose: () -> Unit) {
    val bridge = remember { WebViewBridge() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(bridge, lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            bridge.setForeground(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        }
        lifecycle.addObserver(observer)
        bridge.setForeground(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        onDispose {
            lifecycle.removeObserver(observer)
            bridge.close()
        }
    }
    val state = bridge.state
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("WebView双方向通信（検証）")
        Text("接続状態: ${state.status}")
        Text(state.user?.let { "ユーザーID: ${it.id} / 名前: ${it.name}" } ?: "ユーザー情報: 未受信")
        Text("NFCイベント送信回数: ${state.sentEvents}")
        Button(onClick = bridge::sendNfc, enabled = state.connected && state.foreground) {
            Text("NFCイベント送信")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = bridge::reload) { Text("再読み込み") }
            Button(onClick = onClose) { Text("戻る") }
        }
        AndroidView(
            factory = bridge::create,
            modifier = Modifier.fillMaxWidth().weight(1f),
            onRelease = { bridge.close() },
        )
    }
}
