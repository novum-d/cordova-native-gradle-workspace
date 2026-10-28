package dev.example.nativedata.repository.card

import dev.example.nativedata.err.nfc.NfcReadErr
import dev.example.nativedata.err.nfc.NfcReadErrFactory
import dev.example.nativedata.nfc.card.NfcCardReaderFactory
import dev.example.nativedata.nfc.detection.DetectedNfcTag
import dev.example.nativedata.repository.golf.DefaultGolfCardRepository
import dev.example.nativedata.repository.golf.GolfCardRepository
import javax.inject.Inject

/** 共通のNFC読取Readerを各業務の実装へ組み込む。生成したRepositoryは保持しない。 */
internal class DefaultCardRepositoryFactory @Inject constructor(
    private val nfcReaders: NfcCardReaderFactory,
) : CardRepositoryFactory {
    override fun <E : NfcReadErr> createGolfCard(
        target: DetectedNfcTag,
        errFactory: NfcReadErrFactory<E>,
    ): GolfCardRepository<E> {
        val nfcReader = nfcReaders.create(target, errFactory)
        return DefaultGolfCardRepository(nfcReader)
    }
}
