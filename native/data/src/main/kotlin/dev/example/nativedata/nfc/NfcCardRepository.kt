package dev.example.nativedata.nfc

import com.github.michaelbull.result.Result

/** 業務に依存しない、対象タグへ紐づく生バイト読取の契約。data層内部だけで利用する。 */
internal interface NfcCardRepository<out E : NfcReadFailure> {
    /**
     * 業務配置が対応するモデルと範囲を指定する。実タグのProfileは実装側で識別する。
     * モデルの一致を確認してから通信し、別製品へ同じ座標を流用しない。
     */
    suspend fun read(
        expectedModel: NfcTagModel,
        range: NfcMemoryRange,
    ): Result<ByteArray, E>
}
