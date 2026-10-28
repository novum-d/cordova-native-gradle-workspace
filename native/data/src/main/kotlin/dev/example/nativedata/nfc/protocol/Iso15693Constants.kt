package dev.example.nativedata.nfc.protocol

/** 現在のISO 15693のUIDと、Addressed Read Single Blockで使う通信形式。 */
internal object Iso15693Constants {
    /** ISO 15693で扱うUIDのバイト数。製品ごとのUID判定条件とは分けて管理する。 */
    const val UID_BYTE_COUNT = 8

    /** 送信順のUID配列の末尾にある、ISO 15693のUIDの固定プレフィックス。 */
    const val UID_PREFIX = 0xE0

    /** 高速データレート・Addressedを指定し、Optionを指定しない要求フラグ。 */
    const val READ_REQUEST_FLAGS: Byte = 0x22

    /** 拡張形式ではないRead Single Blockのコマンド番号。 */
    const val READ_SINGLE_BLOCK_COMMAND: Byte = 0x20

    /** 現在実装する標準コマンドの1バイトのブロック番号で指定できる最大値。 */
    const val MAX_SINGLE_BLOCK_NUM = 0xFF

    /** 応答先頭にあるFlagsの位置と長さ。 */
    const val RESPONSE_FLAGS_OFFSET = 0
    const val RESPONSE_FLAGS_BYTE_COUNT = 1

    /** 応答Flagsで、タグがコマンドの失敗を返したことを示すビット。 */
    const val RESPONSE_ERROR_FLAG_MASK = 0x01

    /** Error flag付き応答のエラーコード位置と、Flagsを含む全体のバイト数。 */
    const val ERROR_CODE_OFFSET = 1
    const val ERROR_RESPONSE_BYTE_COUNT = 2
}
