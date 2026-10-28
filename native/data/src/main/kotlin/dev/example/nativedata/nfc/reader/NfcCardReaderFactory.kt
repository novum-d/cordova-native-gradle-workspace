package dev.example.nativedata.nfc.reader

import dev.example.nativedata.nfc.detection.DetectedNfcTag
import dev.example.nativedata.nfc.detection.DetectedNfcTagHandle
import dev.example.nativedata.nfc.failure.NfcReadFailure
import dev.example.nativedata.nfc.failure.NfcReadFailureFactory
import dev.example.nativedata.nfc.memory.NfcMemoryDataSource
import dev.example.nativedata.nfc.profile.NfcTagProfileResolver
import javax.inject.Inject

/** 実行時の検出対象・エラーFactoryと、DIの通信依存を組み合わせる。独自Sessionは作らない。 */
internal class NfcCardReaderFactory @Inject constructor(
    private val memoryDataSource: NfcMemoryDataSource,
    private val profileResolver: NfcTagProfileResolver,
) {
    /** Profileの識別は読取時に行い、識別失敗も指定されたユースケースの具体型で返す。 */
    fun <E : NfcReadFailure> create(
        target: DetectedNfcTag,
        failures: NfcReadFailureFactory<E>,
    ): NfcCardReader<E> {
        require(target is DetectedNfcTagHandle) { "The target must be issued by the NFC detection repository" }
        return DefaultNfcCardReader(
            target = target,
            failures = failures,
            memoryDataSource = memoryDataSource,
            profileResolver = profileResolver,
        )
    }
}
