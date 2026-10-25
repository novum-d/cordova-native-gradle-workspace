package dev.example.kotlinplugin

import android.content.Intent
import dev.example.nativefeature.MainActivity
import dev.example.nativefeature.launch.FeatureLaunchRequest
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaPlugin
import org.json.JSONArray

/** Webからの起動要求を受け付け、featureの画面へ処理を渡すCordovaプラグイン。 */
class KotlinPlugin : CordovaPlugin() {
    /**
     * openFeatureのJSON要求を型へ変換し、明示的IntentでMainActivityへ渡す。
     *
     * 成功通知はstartActivityの完了を表し、画面を閉じたときの結果は返さない。
     * 未知のactionはfalseを返し、Cordova標準の不正actionエラーとして扱う。
     */
    override fun execute(
        action: String,
        args: JSONArray,
        callbackContext: CallbackContext,
    ): Boolean {
        if (action != "openFeature") {
            return false
        }

        val payload = args.optJSONObject(0)?.takeIf { args.length() == 1 }
        if (payload == null) {
            callbackContext.error("Expected one request object with id and optional args")
            return true
        }
        val request = try {
            FeatureLaunchRequest.fromJson(payload.toString()).also { it.toDestination() }
        } catch (_: IllegalArgumentException) {
            callbackContext.error("Invalid feature request: unsupported id or invalid args")
            return true
        }
        val activity = cordova.activity
        activity.runOnUiThread {
            try {
                // 受信したIntentや遷移先クラス名は転送せず、検証した要求だけを固定のActivityへ渡す。
                val intent = Intent(activity, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_REQUEST, request.toJson())
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                activity.startActivity(intent)
                callbackContext.success()
            } catch (error: RuntimeException) {
                callbackContext.error("${error.javaClass.simpleName}: ${error.message}")
            }
        }
        return true
    }
}
