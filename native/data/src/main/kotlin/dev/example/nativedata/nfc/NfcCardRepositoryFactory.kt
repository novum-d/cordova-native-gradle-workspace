package dev.example.nativedata.nfc

import javax.inject.Inject

/** 実行時の検出対象・エラーFactoryと、DIの通信依存を組み合わせる。独自Sessionは作らない。 */
internal class NfcCardRepositoryFactory @Inject constructor(
    private val reader: NfcMemoryReader,
    private val profileResolver: NfcTagProfileResolver,
) {
    /** Profileの識別は読取時に行い、識別失敗も指定されたユースケースの具体型で返す。 */
    fun <E : NfcReadFailure> create(
        target: DetectedNfcTag,
        failures: NfcReadFailureFactory<E>,
    ): NfcCardRepository<E> {
        require(target is DetectedNfcTagHandle) { "The target must be issued by the NFC detection repository" }
        return DefaultNfcCardRepository(
            target = target,
            failures = failures,
            reader = reader,
            profileResolver = profileResolver,
        )
    }
}
