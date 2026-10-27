package dev.example.nativedata.nfc

/** 読取対応が確定しているICのモデル。ユースケースの引数には公開しない。 */
internal enum class NfcTagModel { ICODE_SLI, }

/** モデルのProfileから選ぶ通信手順と、実タグに必要なAndroid技術の対応。 */
internal enum class NfcReadProtocol(val androidTechnology: String) {
    ISO_15693("android.nfc.tech.NfcV"),
}

/** ICの型番に紐づく仕様。発行サービスごとの業務データ配置や接続状態は持たない。 */
internal data class NfcTagProfile(
    val model: NfcTagModel,
    val protocol: NfcReadProtocol,
    val memory: NfcBlockMemory,
)

/**
 * 公開仕様と共有済みTagInfoの112バイト構成を対応付ける。
 * 転記のSLI2ICS20と資料のSL2 ICS20は照合待ちのため、正式品番の確定値としては保存しない。
 */
internal object NfcTagProfiles {
    val icodeSli = NfcTagProfile(
        model = NfcTagModel.ICODE_SLI,
        protocol = NfcReadProtocol.ISO_15693,
        memory = NfcBlockMemory(blockCount = 28, bytesPerBlock = 4),
    )
}

/**
 * AndroidのUID0からの配列と技術一覧からProfileを選ぶ。実際の通信は行わない。
 * E0・NXP 04・製品種別01とUID4のbit 4/3を確認し、SLIX・SLIX2等を除外する。
 * UIDによる製品分類であり、タグの真正性を認証する処理ではない。
 *
 * [NXP SLIX2 §9.2.1 Table 8](https://www.nxp.com/docs/en/data-sheet/SL2S2602.pdf)を参照。
 */
internal fun identifyNfcTagProfile(
    uid: ByteArray,
    technologies: Set<String>,
): NfcTagProfile? = when {
    NfcTagProfiles.icodeSli.protocol.androidTechnology !in technologies -> null
    uid.size != 8 -> null
    !uid.copyOfRange(5, 8).contentEquals(byteArrayOf(0x01, 0x04, 0xE0.toByte())) -> null
    uid[4].toInt() and 0x18 != 0 -> null
    else -> NfcTagProfiles.icodeSli
}
