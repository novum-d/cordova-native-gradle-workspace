package dev.example.nativedata.golf

import dev.example.nativedata.nfc.memory.NfcMemoryPosition
import dev.example.nativedata.nfc.memory.NfcMemoryRange

/** SLIで発行された業務カードの固定配置。ICの容量・通信方式はNFC側のProfileが管理する。 */
internal enum class GolfCardField(val range: NfcMemoryRange) {
    COURSE_NUMBER(NfcMemoryRange(NfcMemoryPosition(1, 0), NfcMemoryPosition(1, 1))),
    HOLDER_NUMBER(NfcMemoryRange(NfcMemoryPosition(1, 2), NfcMemoryPosition(4, 3))),
}
