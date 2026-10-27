package dev.example.nativedata.nfc

/** 今回読み取り可能なICODE SLIと、それ以外のタグ。未確定の他製品へ処理を分岐させない。 */
enum class NfcTagType { ICODE_SLI, OTHER }

/**
 * AndroidのNFC-V UID（UID0からの送信順）を使ってSLIを判定する。通信やメモリREADは行わない。
 * E0・NXP 04・製品種別01に加え、UID4のbit 4/3が0であることを確認してSLIX・SLIX2を除外する。
 * UIDの製品分類であり、タグの真正性を保証する認証ではない。
 *
 * [NXP SLIX2 §9.2.1 Table 8](https://www.nxp.com/docs/en/data-sheet/SL2S2602.pdf)を参照。
 */
fun classifyNfcTag(
    uid: ByteArray,
    technologies: Set<String>,
): NfcTagType = when {
    "android.nfc.tech.NfcV" !in technologies -> NfcTagType.OTHER
    uid.size != 8 -> NfcTagType.OTHER
    !uid.copyOfRange(5, 8).contentEquals(byteArrayOf(0x01, 0x04, 0xE0.toByte())) -> NfcTagType.OTHER
    uid[4].toInt() and 0x18 != 0 -> NfcTagType.OTHER
    else -> NfcTagType.ICODE_SLI
}
