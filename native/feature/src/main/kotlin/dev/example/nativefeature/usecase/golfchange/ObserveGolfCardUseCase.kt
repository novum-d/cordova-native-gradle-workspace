package dev.example.nativefeature.usecase.golfchange

import dev.example.nativefeature.model.golfchange.GolfCardReadEvent
import dev.example.nativefeature.usecase.nfc.ObserveNfcTagsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** 同じ検出対象からゴルフ場番号・ホルダー番号を直列に取得する。UIにRepositoryを公開しない。 */
class ObserveGolfCardUseCase @Inject constructor(
    private val observeNfcTags: ObserveNfcTagsUseCase,
    private val readers: GolfCardReadUseCaseFactory,
) {
    /** 最初の失敗で今回の読取を終え、次の検出を待つ。別タグの番号を組み合わせない。 */
    operator fun invoke(): Flow<GolfCardReadEvent> = observeNfcTags().map { detection ->
        if (detection.isErr) {
            GolfCardReadEvent.DetectionFailed(detection.error)
        } else {
            val target = detection.value
            val course = readers.courseNum(target)()
            if (course.isErr) {
                GolfCardReadEvent.CourseReadFailed(course.error)
            } else {
                val holder = readers.holderNum(target)()
                if (holder.isErr) {
                    GolfCardReadEvent.HolderReadFailed(holder.error)
                } else {
                    GolfCardReadEvent.ReadCompleted(course.value, holder.value)
                }
            }
        }
    }
}
