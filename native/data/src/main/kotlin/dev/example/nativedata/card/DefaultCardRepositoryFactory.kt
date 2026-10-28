package dev.example.nativedata.card

import dev.example.nativedata.golf.DefaultGolfCardRepository
import dev.example.nativedata.golf.GolfCardRepository
import dev.example.nativedata.nfc.detection.DetectedNfcTag
import dev.example.nativedata.nfc.failure.NfcReadFailure
import dev.example.nativedata.nfc.failure.NfcReadFailureFactory
import dev.example.nativedata.nfc.reader.NfcCardReaderFactory
import javax.inject.Inject

/** 共通のNFC読取Readerを各業務の実装へ組み込む。生成したRepositoryは保持しない。 */
internal class DefaultCardRepositoryFactory @Inject constructor(
    private val nfcReaders: NfcCardReaderFactory,
) : CardRepositoryFactory {
    override fun <E : NfcReadFailure> createGolfCard(
        target: DetectedNfcTag,
        failures: NfcReadFailureFactory<E>,
    ): GolfCardRepository<E> {
        val nfcReader = nfcReaders.create(target, failures)
        return DefaultGolfCardRepository(nfcReader)
    }
}
