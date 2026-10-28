package dev.example.nativedata.repository.card

import dev.example.nativedata.err.nfc.NfcReadErr
import dev.example.nativedata.err.nfc.NfcReadErrFactory
import dev.example.nativedata.nfc.detection.DetectedNfcTag
import dev.example.nativedata.repository.golf.GolfCardRepository

/** 業務カードごとのRepositoryを生成する入口。IC型番から業務の種類を推測しない。 */
interface CardRepositoryFactory {
    /** 検出対象とユースケースのエラー文脈に紐づく、ゴルフ用の読取窓口を生成する。 */
    fun <E : NfcReadErr> createGolfCard(
        target: DetectedNfcTag,
        errFactory: NfcReadErrFactory<E>,
    ): GolfCardRepository<E>
}
