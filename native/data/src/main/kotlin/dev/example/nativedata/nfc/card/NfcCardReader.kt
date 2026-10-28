package dev.example.nativedata.nfc.card

import com.github.michaelbull.result.Result
import dev.example.nativedata.err.nfc.NfcReadErr
import dev.example.nativedata.nfc.memory.NfcMemoryRange
import dev.example.nativedata.nfc.profile.NfcTagModel

/** 業務に依存しない、対象タグへ紐づく生バイト読取の契約。data層内部だけで利用する。 */
internal interface NfcCardReader<out E : NfcReadErr> {
    /**
     * 業務配置が対応するモデルと範囲を指定する。実タグのProfileは実装側で識別する。
     * モデルの一致を確認してから通信し、別製品へ同じ座標を流用しない。
     */
    suspend fun read(
        expectedModel: NfcTagModel,
        range: NfcMemoryRange,
    ): Result<ByteArray, E>
}
