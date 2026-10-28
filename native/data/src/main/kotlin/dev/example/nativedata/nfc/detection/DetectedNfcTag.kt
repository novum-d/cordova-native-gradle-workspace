package dev.example.nativedata.nfc.detection

import dev.example.nativedata.nfc.memory.NfcTagAccess
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.atomic.AtomicBoolean

/** 検出通知の処理中だけ有効な対象参照。処理完了・購読終了後には再利用しない。 */
interface DetectedNfcTag

/** 同じ対象への読取を直列化する。接続・認証状態を保存するSessionではない。 */
internal class DetectedNfcTagHandle(val access: NfcTagAccess) : DetectedNfcTag {
    val readMutex = Mutex()
    private val active = AtomicBoolean(true)
    val isActive: Boolean get() = active.get()

    /** 終了した対象への新しい読み取りを拒否する。実行中I/Oの中断とは別に扱う。 */
    fun invalidate() {
        active.set(false)
    }
}
