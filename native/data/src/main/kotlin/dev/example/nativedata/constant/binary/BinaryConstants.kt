package dev.example.nativedata.constant.binary

/** 通信方式・カードの種類に依存しない、バイトと16進表現の基本定義。 */
internal object BinaryConstants {
    /** Kotlin標準のByte幅。複数バイト値の結合で、上位バイトを移動するビット数。 */
    const val BITS_PER_BYTE = Byte.SIZE_BITS

    /** 符号付きByteをIntへ拡張したときに、下位8bitだけを取り出すマスク。 */
    const val UNSIGNED_BYTE_MASK = 0xFF

    /** 16進数1桁に対応するニブルのビット数。 */
    const val BITS_PER_NIBBLE = 4

    /** 16進数の下位1桁を取り出すマスク。 */
    const val LOW_NIBBLE_MASK = 0x0F

    /** 1バイトを欠落なく16進表記するための桁数。 */
    const val HEX_DIGITS_PER_BYTE = BITS_PER_BYTE / BITS_PER_NIBBLE

    /** Locale・文字コードに依存せず、大文字の16進表現を生成するための桁文字。 */
    const val UPPERCASE_HEX_DIGITS = "0123456789ABCDEF"
}
