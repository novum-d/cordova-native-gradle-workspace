package dev.example.nativedata.nfc.card

import dev.example.nativedata.err.nfc.NfcReadErr
import dev.example.nativedata.err.nfc.NfcReadErrFactory
import dev.example.nativedata.nfc.detection.DetectedNfcTag
import dev.example.nativedata.nfc.detection.DetectedNfcTagHandle
import dev.example.nativedata.nfc.memory.NfcMemoryDataSource
import dev.example.nativedata.nfc.profile.NfcTagProfileResolver
import javax.inject.Inject

/** 実行時の検出対象・エラーFactoryと、DIの通信依存を組み合わせる。独自Sessionは作らない。 */
internal class NfcCardReaderFactory @Inject constructor(
    private val memoryDataSource: NfcMemoryDataSource,
    private val profileResolver: NfcTagProfileResolver,
) {
    /** Profileの識別は読取時に行い、識別失敗も指定されたユースケースの具体型で返す。 */
    fun <E : NfcReadErr> create(
        target: DetectedNfcTag,
        errFactory: NfcReadErrFactory<E>,
    ): NfcCardReader<E> {
        require(target is DetectedNfcTagHandle) { "The target must be issued by the NFC detection repository" }
        return DefaultNfcCardReader(
            target = target,
            errFactory = errFactory,
            memoryDataSource = memoryDataSource,
            profileResolver = profileResolver,
        )
    }
}
