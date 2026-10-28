package dev.example.nativedata.card

import dev.example.nativedata.golf.GolfCardRepository
import dev.example.nativedata.nfc.detection.DetectedNfcTag
import dev.example.nativedata.nfc.failure.NfcReadFailure
import dev.example.nativedata.nfc.failure.NfcReadFailureFactory

/** 業務カードごとのRepositoryを生成する入口。IC型番から業務の種類を推測しない。 */
interface CardRepositoryFactory {
    /** 検出対象とユースケースのエラー文脈に紐づく、ゴルフ用の読取窓口を生成する。 */
    fun <E : NfcReadFailure> createGolfCard(
        target: DetectedNfcTag,
        failures: NfcReadFailureFactory<E>,
    ): GolfCardRepository<E>
}
