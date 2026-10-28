package dev.example.nativefeature.services.golfchange

import dev.example.nativedata.card.CardRepositoryFactory
import dev.example.nativedata.nfc.DetectedNfcTag
import javax.inject.Inject

/** 業務カードの共通Factoryへ同じ対象と各エラー文脈を渡し、引数なしの読取処理を組み立てる。 */
class GolfCardReadUseCaseFactory @Inject constructor(private val repositories: CardRepositoryFactory) {
    fun courseNumber(target: DetectedNfcTag): ReadGolfCourseNumberUseCase = ReadGolfCourseNumberUseCase(
        repositories.createGolfCard(target, ReadGolfCourseNumberErr.NfcRead),
    )

    fun holderNumber(target: DetectedNfcTag): ReadGolfHolderNumberUseCase = ReadGolfHolderNumberUseCase(
        repositories.createGolfCard(target, ReadGolfHolderNumberErr.NfcRead),
    )
}
