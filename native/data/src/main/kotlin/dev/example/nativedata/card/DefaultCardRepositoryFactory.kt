package dev.example.nativedata.card

import dev.example.nativedata.golf.DefaultGolfCardRepository
import dev.example.nativedata.golf.GolfCardRepository
import dev.example.nativedata.nfc.DetectedNfcTag
import dev.example.nativedata.nfc.NfcCardRepositoryFactory
import dev.example.nativedata.nfc.NfcReadFailure
import dev.example.nativedata.nfc.NfcReadFailureFactory
import javax.inject.Inject

/** 共通のNFC読取Repositoryを各業務の実装へ組み込む。生成したRepositoryは保持しない。 */
internal class DefaultCardRepositoryFactory @Inject constructor(
    private val nfcRepositories: NfcCardRepositoryFactory,
) : CardRepositoryFactory {
    override fun <E : NfcReadFailure> createGolfCard(
        target: DetectedNfcTag,
        failures: NfcReadFailureFactory<E>,
    ): GolfCardRepository<E> {
        val nfcRepository = nfcRepositories.create(target, failures)
        return DefaultGolfCardRepository(nfcRepository)
    }
}
