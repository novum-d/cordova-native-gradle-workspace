package dev.example.nativedata.nfc

/** 0始まりの物理ブロック番号と、そのブロック内のバイト位置。 */
data class NfcMemoryPosition(val blockNumber: Int, val byteOffset: Int)

/** 終了位置を含む連続メモリ範囲。製品ごとの領域検査はReaderが通信前に行う。 */
data class NfcMemoryRange(val start: NfcMemoryPosition, val endInclusive: NfcMemoryPosition) {
    /** 検証済み範囲で取得するバイト数。終了バイト自身も含める。 */
    internal val byteCount: Int
        get() = (endInclusive.blockNumber - start.blockNumber) * 4 + endInclusive.byteOffset - start.byteOffset + 1

    /** ICODE SLIのユーザー領域（28ブロック×4バイト）内で、開始・終了が逆転していないか。 */
    internal fun isWithinIcodeSli(): Boolean = start.blockNumber in 0..27 && endInclusive.blockNumber in 0..27 &&
        start.byteOffset in 0..3 && endInclusive.byteOffset in 0..3 &&
        start.blockNumber * 4 + start.byteOffset <= endInclusive.blockNumber * 4 + endInclusive.byteOffset
}
