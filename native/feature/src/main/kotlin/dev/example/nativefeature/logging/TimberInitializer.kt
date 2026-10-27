package dev.example.nativefeature.logging

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.startup.Initializer
import timber.log.Timber

/** Manifestに登録し、Cordova・開発用APKのActivityが起動する前に共通ログを初期化する。 */
class TimberInitializer : Initializer<Unit> {
    /** Libraryのビルド種別ではなく、実際のホストアプリのDebuggableフラグでTree登録を判断する。 */
    override fun create(context: Context) {
        val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        TimberLogging.initialize(debuggable)
        if (debuggable) Timber.tag("NativeLogging").d("Timber logging initialized")
    }

    /** この初期化は他のInitializerに依存しない。 */
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
