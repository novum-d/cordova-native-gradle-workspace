package dev.example.kotlinplugin

import android.content.Intent
import dev.example.nativefeature.MainActivity
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaPlugin
import org.json.JSONArray

/** Webからの起動要求を受け付け、featureの画面へ処理を渡すCordovaプラグイン。 */
class KotlinPlugin : CordovaPlugin() {
    /**
     * openFeatureを受けたときだけ、UIスレッドで明示的Intentを使ってMainActivityを起動する。
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

        val activity = cordova.activity
        activity.runOnUiThread {
            try {
                activity.startActivity(Intent(activity, MainActivity::class.java))
                callbackContext.success()
            } catch (error: RuntimeException) {
                callbackContext.error("${error.javaClass.simpleName}: ${error.message}")
            }
        }
        return true
    }
}
