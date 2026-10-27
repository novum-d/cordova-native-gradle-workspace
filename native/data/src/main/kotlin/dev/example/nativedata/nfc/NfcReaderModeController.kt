package dev.example.nativedata.nfc

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.os.Build
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activityの前面表示と汎用の検出要求がそろった間だけReaderModeを有効化する。
 * Activityはこのホスト制御だけを呼び、Repositoryや業務の読み取りには関与しない。
 */
@ActivityScoped
class NfcReaderModeController @Inject constructor(private val source: NfcTagDetectionSource) {
    private var observation: Job? = null

    /** onResumeから呼び出し、要求の開始・終了とNFC設定の変更を監視する。 */
    fun resume(activity: Activity) {
        if (observation != null) return
        observation = CoroutineScope(Dispatchers.Main.immediate).launch {
            source.requests.collectLatest { request ->
                if (request != null) observeRequest(activity, request)
            }
        }
    }

    /** onPauseから呼び出し、ReaderMode・設定変更の監視を解除する。 */
    fun pause() {
        observation?.cancel()
        observation = null
    }

    /** NFC無効時にも要求を維持し、設定で有効へ戻したら検出を再開する。 */
    private suspend fun observeRequest(
        activity: Activity,
        request: NfcDetectionRequest,
    ) {
        val adapter = NfcAdapter.getDefaultAdapter(activity)
        if (adapter == null) {
            request.signals.send(NfcDetectionSignal.NotSupported)
            awaitCancellation()
        }
        val changes = Channel<Unit>(Channel.CONFLATED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                changes.trySend(Unit)
            }
        }
        var registered = false
        try {
            registerReceiver(activity, receiver)
            registered = true
            changes.trySend(Unit)
            changes.receiveAsFlow().collect {
                if (adapter.isEnabled) {
                    enableReader(activity, adapter, request)
                } else {
                    adapter.disableReaderMode(activity)
                    request.signals.send(NfcDetectionSignal.Disabled)
                }
            }
        } catch (cause: SecurityException) {
            request.signals.send(NfcDetectionSignal.AccessDenied(cause))
            awaitCancellation()
        } finally {
            if (registered) activity.unregisterReceiver(receiver)
            changes.close()
            // 権限拒否時にも、先に発生した失敗やキャンセルを置き換えない。
            try {
                adapter.disableReaderMode(activity)
            } catch (_: SecurityException) {
                // 開始が拒否された場合、終了も拒否されることがある。
            }
        }
    }

    /** 生メモリを読むためNDEF検査を省き、未対応RFも検出後に読取側で判定する。 */
    private fun enableReader(
        activity: Activity,
        adapter: NfcAdapter,
        request: NfcDetectionRequest,
    ) {
        val flags = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        adapter.enableReaderMode(activity, { tag -> request.onTag(AndroidNfcTagAccess(tag)) }, flags, null)
    }

    /** 新しいAndroidではReceiverの公開範囲を明示し、OSのNFC設定変更だけを受ける。 */
    @Suppress("DEPRECATION")
    private fun registerReceiver(
        activity: Activity,
        receiver: BroadcastReceiver,
    ) {
        val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            activity.registerReceiver(receiver, filter)
        }
    }
}
