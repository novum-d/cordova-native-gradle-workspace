package dev.example.nativefeature

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import dev.example.nativefeature.launch.FeatureLaunchRequest
import dev.example.nativefeature.navigation.FeatureLaunchCommand
import dev.example.nativefeature.navigation.FeatureNavigation
import timber.log.Timber

/**
 * ホストからの起動要求を検証し、Navigationへ一度だけ渡すHiltの注入入口となるActivity。
 *
 * [onCreate]と[onNewIntent]で同じ起動要求を検証し、処理IDに対応する画面を直接開く。
 * 画面・引数の保存はNavigationと各画面のSavedStateHandleへ任せ、要求JSONは重複保存しない。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var pendingCommand by mutableStateOf<FeatureLaunchCommand?>(null)

    /** 初回だけIntentを渡す。再生成時はNavigationの復元を優先し、古いIntentを再実行しない。 */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && !acceptRequest(intent.getStringExtra(EXTRA_REQUEST))) return
        setContent {
            MaterialTheme {
                FeatureNavigation(
                    command = pendingCommand,
                    onCommandHandled = { handled ->
                        if (pendingCommand === handled) pendingCommand = null
                    },
                    onClose = ::finish,
                )
            }
        }
    }

    /** singleTopによる再利用時も、Intentを更新して新しい要求を同じ検証経路へ渡す。 */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptRequest(intent.getStringExtra(EXTRA_REQUEST))
    }

    /** 不正な要求では終了し、正常時は同じ内容の再要求も独立した遷移として受け付ける。 */
    private fun acceptRequest(requestJson: String?): Boolean {
        val resolved = try {
            FeatureLaunchRequest.fromJson(requireNotNull(requestJson)).toDestination()
        } catch (error: IllegalArgumentException) {
            Timber.tag("FeatureLaunch").w(error, "Invalid feature launch request")
            finish()
            return false
        }
        pendingCommand = FeatureLaunchCommand(resolved)
        return true
    }

    /** ホストから処理ID・argsのJSONを受け取るIntentキー。 */
    companion object {
        const val EXTRA_REQUEST = "dev.example.nativefeature.LAUNCH_REQUEST"
    }
}
