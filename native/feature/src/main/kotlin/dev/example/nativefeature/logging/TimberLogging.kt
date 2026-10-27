package dev.example.nativefeature.logging

import timber.log.Timber

/** ホストがDebuggableな場合にDebugTreeを登録し、既存のTreeと重複初期化を考慮する。 */
internal object TimberLogging {
    /** ReleaseではTreeを追加せず、ホストが登録したTreeはどちらの構成でも保持する。 */
    @Synchronized
    fun initialize(debuggable: Boolean) {
        if (debuggable && Timber.forest().none { it is Timber.DebugTree }) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
