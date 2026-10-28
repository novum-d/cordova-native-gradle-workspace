package dev.example.nativefeature.services.golfchange

import dev.example.nativedata.nfc.detection.DetectedNfcTag
import dev.example.nativedata.repository.card.CardRepositoryFactory
import javax.inject.Inject

/** 業務カードの共通Factoryへ同じ対象と各エラー文脈を渡し、引数なしの読取処理を組み立てる。 */
class GolfCardReadUseCaseFactory @Inject constructor(private val repositories: CardRepositoryFactory) {
    fun courseNum(target: DetectedNfcTag): ReadGolfCourseNumUseCase = ReadGolfCourseNumUseCase(
        repositories.createGolfCard(target, ReadGolfCourseNumErr.NfcRead),
    )

    fun holderNum(target: DetectedNfcTag): ReadGolfHolderNumUseCase = ReadGolfHolderNumUseCase(
        repositories.createGolfCard(target, ReadGolfHolderNumErr.NfcRead),
    )
}
