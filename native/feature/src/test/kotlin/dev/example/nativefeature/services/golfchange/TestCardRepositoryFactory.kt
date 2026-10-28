package dev.example.nativefeature.services.golfchange

import dev.example.nativedata.card.CardRepositoryFactory
import dev.example.nativedata.golf.GolfCardRepository
import dev.example.nativedata.nfc.detection.DetectedNfcTag
import dev.example.nativedata.nfc.failure.NfcReadFailure
import dev.example.nativedata.nfc.failure.NfcReadFailureFactory
import java.io.IOException

/** 対象ごとの返却値と履歴を設定し、模擬Repositoryの生成だけを担当する。 */
internal class TestCardRepositoryFactory : CardRepositoryFactory {
    val courseBytes = mutableMapOf<DetectedNfcTag, ByteArray>()
    val holderBytes = mutableMapOf<DetectedNfcTag, ByteArray>()
    val calls = mutableListOf<Pair<DetectedNfcTag, String>>()
    var failure: IOException? = null
    var failHolderOnly = false
    var unsupported = false
    var beforeRead: suspend () -> Unit = {}

    override fun <E : NfcReadFailure> createGolfCard(
        target: DetectedNfcTag,
        failures: NfcReadFailureFactory<E>,
    ): GolfCardRepository<E> = TestGolfCardRepository(target, failures, this)
}
