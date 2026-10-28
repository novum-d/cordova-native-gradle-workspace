package dev.example.nativedata.nfc.profile

import dev.example.nativedata.nfc.memory.NfcBlockMemory

/** 型番Profileで識別するICのモデル。読取可否とは分けて扱い、ユースケースの引数には公開しない。 */
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
