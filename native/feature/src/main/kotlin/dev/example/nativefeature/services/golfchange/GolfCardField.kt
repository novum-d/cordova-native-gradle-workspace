package dev.example.nativefeature.services.golfchange

import dev.example.nativedata.nfc.NfcMemoryPosition
import dev.example.nativedata.nfc.NfcMemoryRange

/** 業務カードの固定メモリマップ。物理座標は0始まりで、末尾バイトも範囲へ含める。 */
internal enum class GolfCardField(val range: NfcMemoryRange) {
    COURSE_NUMBER(NfcMemoryRange(NfcMemoryPosition(1, 0), NfcMemoryPosition(1, 1))),
    HOLDER_NUMBER(NfcMemoryRange(NfcMemoryPosition(1, 2), NfcMemoryPosition(4, 3))),
}
