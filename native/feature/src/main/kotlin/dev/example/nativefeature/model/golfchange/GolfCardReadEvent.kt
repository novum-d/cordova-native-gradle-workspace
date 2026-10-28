package dev.example.nativefeature.model.golfchange

import dev.example.nativefeature.err.golfchange.ReadGolfCourseNumErr
import dev.example.nativefeature.err.golfchange.ReadGolfHolderNumErr
import dev.example.nativefeature.err.nfc.ObserveNfcTagsErr

/** UIへ通知する読取結果。元のユースケースの具体エラーを変換せず保持する。 */
sealed interface GolfCardReadEvent {
    data class DetectionFailed(val err: ObserveNfcTagsErr) : GolfCardReadEvent
    data class CourseReadFailed(val err: ReadGolfCourseNumErr.NfcRead) : GolfCardReadEvent
    data class HolderReadFailed(val err: ReadGolfHolderNumErr.NfcRead) : GolfCardReadEvent
    data class ReadCompleted(val courseNum: Int, val holderNum: String) : GolfCardReadEvent
}
