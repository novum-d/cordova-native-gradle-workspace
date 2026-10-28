package dev.example.nativefeature.testing.golfchange

import dev.example.nativedata.err.nfc.NfcReadErr
import dev.example.nativedata.err.nfc.NfcReadErrFactory
import dev.example.nativedata.nfc.detection.DetectedNfcTag
import dev.example.nativedata.repository.card.CardRepositoryFactory
import dev.example.nativedata.repository.golf.GolfCardRepository
import java.io.IOException

/** 対象ごとの返却値と履歴を設定し、模擬Repositoryの生成だけを担当する。 */
internal class TestCardRepositoryFactory : CardRepositoryFactory {
    val courseNums = mutableMapOf<DetectedNfcTag, Int>()
    val holderNums = mutableMapOf<DetectedNfcTag, String>()
    val calls = mutableListOf<Pair<DetectedNfcTag, String>>()
    var err: IOException? = null
    var failHolderOnly = false
    var unsupported = false
    var beforeRead: suspend () -> Unit = {}

    override fun <E : NfcReadErr> createGolfCard(
        target: DetectedNfcTag,
        errFactory: NfcReadErrFactory<E>,
    ): GolfCardRepository<E> = TestGolfCardRepository(target, errFactory, this)
}
