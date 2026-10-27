package dev.example.nativefeature.services.golfchange

import dev.example.nativefeature.nfc.ObserveNfcTagsErr
import dev.example.nativefeature.nfc.ObserveNfcTagsUseCase
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
            val course = readers.courseNumber(target)()
            if (course.isErr) {
                GolfCardReadEvent.CourseReadFailed(course.error)
            } else {
                val holder = readers.holderNumber(target)()
                if (holder.isErr) {
                    GolfCardReadEvent.HolderReadFailed(holder.error)
                } else {
                    GolfCardReadEvent.ReadCompleted(course.value, holder.value)
                }
            }
        }
    }
}

/** UIへ通知する読取結果。元のユースケースの具体エラーを変換せず保持する。 */
sealed interface GolfCardReadEvent {
    data class DetectionFailed(val failure: ObserveNfcTagsErr) : GolfCardReadEvent
    data class CourseReadFailed(val failure: ReadGolfCourseNumberErr.NfcRead) : GolfCardReadEvent
    data class HolderReadFailed(val failure: ReadGolfHolderNumberErr.NfcRead) : GolfCardReadEvent
    data class ReadCompleted(val courseNumber: Int, val holderNumber: String) : GolfCardReadEvent
}
