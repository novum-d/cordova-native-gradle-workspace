package dev.example.nativedata.repository.golf

import dev.example.nativedata.constant.binary.BinaryConstants

/** 固定範囲の読取で取得したバイト列を、ゴルフカードの業務値へ変換する。 */
internal object GolfCardDecoder {
    /** ゴルフ場番号の格納順。取得した2バイトの先頭が下位、次が上位に対応する。 */
    private const val COURSE_NUM_LOW_BYTE_OFFSET = 0
    private const val COURSE_NUM_HIGH_BYTE_OFFSET = 1

    /** 応答検査・範囲切出し済みの2バイトを、LSBファーストの符号なし16bit値として解釈する。 */
    fun decodeCourseNum(bytes: ByteArray): Int {
        val low = bytes[COURSE_NUM_LOW_BYTE_OFFSET].toInt() and BinaryConstants.UNSIGNED_BYTE_MASK
        val high = bytes[COURSE_NUM_HIGH_BYTE_OFFSET].toInt() and BinaryConstants.UNSIGNED_BYTE_MASK
        return low or (high shl BinaryConstants.BITS_PER_BYTE)
    }

    /** 取得した14バイトを読取順・大文字・区切りなしの28桁で表し、0や上位ビットも保持する。 */
    fun decodeHolderNum(bytes: ByteArray): String = buildString(bytes.size * BinaryConstants.HEX_DIGITS_PER_BYTE) {
        for (byte in bytes) {
            val value = byte.toInt() and BinaryConstants.UNSIGNED_BYTE_MASK
            append(BinaryConstants.UPPERCASE_HEX_DIGITS[value ushr BinaryConstants.BITS_PER_NIBBLE])
            append(BinaryConstants.UPPERCASE_HEX_DIGITS[value and BinaryConstants.LOW_NIBBLE_MASK])
        }
    }
}
