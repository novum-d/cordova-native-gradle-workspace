package dev.example.nativefeature.dev

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.example.nativefeature.MainActivity
import java.io.IOException

/**
 * 開発用の要求JSONを選び、既存MainActivityのIntent受信・検証経路を動かす。
 * サンプル名をExtraのsampleに指定すれば、ランチャーで選択せず対象画面を直接開ける。
 */
class DevLauncherActivity : ComponentActivity() {
    private var status by mutableStateOf("サンプルを選んで既存MainActivityを起動します。")

    /** 初回のコマンド指定だけを実行し、再生成で以前の要求を再送しない。 */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize().safeDrawingPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Feature Dev — Intentサンプル")
                    Text("Cordovaを使わずfeatureの画面を起動します。")
                    IntentSample.entries.forEach { sample ->
                        Button(onClick = { openSample(sample) }, modifier = Modifier.fillMaxWidth()) {
                            Text(sample.title)
                        }
                    }
                    Text(status)
                    Text("不正要求ではMainActivityが終了し、この画面へ戻ります。")
                }
            }
        }
        if (savedInstanceState == null) acceptSample(intent)
    }

    /** ランチャーが再利用された場合も、同じ固定サンプルの選択経路を使う。 */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptSample(intent)
    }

    /** サンプル名以外の外部Extraは転送せず、未知の名前はランチャー内で拒否する。 */
    private fun acceptSample(intent: Intent) {
        val key = intent.getStringExtra(EXTRA_SAMPLE) ?: return
        val sample = IntentSample.fromKey(key)
        if (sample == null) {
            status = "指定したサンプルはありません。画面から選択してください。"
        } else {
            openSample(sample)
        }
    }

    /** 不正要求もそのまま渡し、feature側の本物の検証処理で拒否されることを確認する。 */
    private fun openSample(sample: IntentSample) {
        val json = try {
            assets.open("requests/${sample.key}.json").bufferedReader().use { it.readText() }
        } catch (_: IOException) {
            status = "サンプルを読み込めませんでした。assetsを確認してください。"
            return
        }
        status = "送信: ${sample.key}\n$json"
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_REQUEST, json)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
    }

    /** adbやAndroid Studioで既知のサンプルを指定するための開発専用キー。 */
    companion object {
        const val EXTRA_SAMPLE = "sample"
    }
}
