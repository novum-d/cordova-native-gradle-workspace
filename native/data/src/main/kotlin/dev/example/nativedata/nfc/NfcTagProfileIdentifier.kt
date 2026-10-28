package dev.example.nativedata.nfc

/** 型番・RFごとの識別を追加するための契約。共通部へUID書式や製品固有の条件を持ち込まない。 */
internal interface NfcTagProfileIdentifier {
    /** この判定器を呼ぶために、実タグが備えている必要があるAndroid技術。 */
    val requiredTechnologies: Set<String>

    /**
     * 一致するProfile、または対象外を示すnullを返す。例外・キャンセルを対象外へ隠さない。
     * UID以外の技術情報も参照できるようTagAccessを受け取る。識別通信を追加する場合は、
     * 判定器自身が接続を確実に閉じてから戻り、次の判定や読取へ接続を持ち越さない。
     * 受け取ったTagを判定器のフィールドへ保持しない。
     */
    suspend fun identify(tag: NfcTagAccess): NfcTagProfile?
}
