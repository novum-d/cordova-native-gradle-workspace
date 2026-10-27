package dev.example.nativefeature.services.golfchange

import dev.example.nativedata.golf.GolfCardRepositoryFactory
import dev.example.nativedata.nfc.DetectedNfcTag
import javax.inject.Inject

/** 同じ対象と各ユースケースのエラー文脈から、引数なしの読取処理を組み立てる。 */
class GolfCardReadUseCaseFactory @Inject constructor(private val repositories: GolfCardRepositoryFactory) {
    fun courseNumber(target: DetectedNfcTag): ReadGolfCourseNumberUseCase = ReadGolfCourseNumberUseCase(
        repositories.create(target, ReadGolfCourseNumberErr.NfcRead),
    )

    fun holderNumber(target: DetectedNfcTag): ReadGolfHolderNumberUseCase = ReadGolfHolderNumberUseCase(
        repositories.create(target, ReadGolfHolderNumberErr.NfcRead),
    )
}
