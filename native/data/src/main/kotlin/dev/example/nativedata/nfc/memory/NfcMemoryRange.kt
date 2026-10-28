package dev.example.nativedata.nfc.memory

/** 0始まりの物理ブロック番号と、そのブロック内のバイト位置。 */
data class NfcMemoryPosition(val blockNum: Int, val byteOffset: Int)

/** 終了位置を含む連続メモリ範囲。容量やブロック幅は型番Profileから受け取る。 */
data class NfcMemoryRange(val start: NfcMemoryPosition, val endInclusive: NfcMemoryPosition) {
    /** 両端・順序をProfileのユーザー領域と照合し、不正な座標の乗算を避ける。 */
    internal fun isWithin(memory: NfcBlockMemory): Boolean =
        start.blockNum in 0 until memory.blockCount && endInclusive.blockNum in 0 until memory.blockCount &&
            start.byteOffset in 0 until memory.bytesPerBlock &&
            endInclusive.byteOffset in 0 until memory.bytesPerBlock &&
            start.blockNum <= endInclusive.blockNum &&
            (start.blockNum != endInclusive.blockNum || start.byteOffset <= endInclusive.byteOffset)

    /** 検査済みの範囲をProfileのブロック幅でバイト数へ換算する。終了バイト自身も含める。 */
    internal fun byteCount(memory: NfcBlockMemory): Int {
        require(isWithin(memory)) { "The memory range must be within the selected profile" }
        return (endInclusive.blockNum - start.blockNum) * memory.bytesPerBlock +
            endInclusive.byteOffset - start.byteOffset + 1
    }
}

/** 型番のユーザー領域の形状。ByteArrayに収まらない構成や0幅を内部設定の不備として拒否する。 */
internal data class NfcBlockMemory(val blockCount: Int, val bytesPerBlock: Int) {
    init {
        require(blockCount > 0 && bytesPerBlock > 0) { "Memory dimensions must be positive" }
        require(blockCount.toLong() * bytesPerBlock <= Int.MAX_VALUE) { "Memory exceeds the ByteArray size limit" }
    }
}
