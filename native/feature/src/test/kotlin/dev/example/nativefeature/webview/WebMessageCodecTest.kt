package dev.example.nativefeature.webview

import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 外部入力の型・深さ・サイズ・方向を制限する受信契約を確認する。 */
class WebMessageCodecTest {
    private val session = "00000000-0000-0000-0000-000000000001"

    /** READYと表示用ユーザーの正しい入力を受け取れる。 */
    @Test
    fun acceptsSupportedMessages() {
        assertEquals(WebMessage.Ready(session), decode("READY", "{}"))
        assertEquals(WebMessage.UserFetched(session, 1, "Leanne Graham"), decode("USER_FETCHED", user()))
    }

    /** 未知・逆方向・欠損・不正なJSONを受信状態へ渡さない。 */
    @Test
    fun rejectsInvalidEnvelopes() {
        listOf(
            "{",
            "[]",
            "null",
            "{}",
            """{"type":"READY","payload":{}}""",
            """{"type":"READY","session":1,"payload":{}}""",
        ).forEach { assertNull(WebMessageCodec.decode(it)) }
        assertNull(decode("UNKNOWN", "{}"))
        assertNull(decode("NFC_DETECTED", "{}"))
        assertNull(decode("READY", "[]"))
        assertNull(decode("READY", """{"unexpected":true}"""))
    }

    /** 型の暗黙変換・範囲外のID・過剰な名前・ログを改行させる値を拒否する。 */
    @Test
    fun rejectsInvalidUsers() {
        listOf("\"1\"", "1.5", "0", "-1", "2147483648", "true", "null").forEach {
            assertNull(decode("USER_FETCHED", user(id = it)))
        }
        listOf(
            "\"\"",
            "\" \"",
            "1",
            "null",
            """"a\nb"""",
            JsonPrimitive("a".repeat(257)).toString(),
        ).forEach { assertNull(decode("USER_FETCHED", user(name = it))) }
    }

    /** 小さくても深すぎるJSONや巨大なメッセージを解析器の前で拒否する。 */
    @Test
    fun boundsInputComplexity() {
        assertNull(decode("READY", "[".repeat(2_000) + "]".repeat(2_000)))
        assertNull(decode("USER_FETCHED", user(name = JsonPrimitive("a".repeat(20_000)).toString())))
        val bracketsInString = """[[[\"{{{"""
        assertEquals(
            WebMessage.UserFetched(session, 1, bracketsInString),
            decode("USER_FETCHED", user(name = JsonPrimitive(bracketsInString).toString())),
        )
    }

    /** 送信JSONは型とページ識別子を含み、コード実行用の文字列にはしない。 */
    @Test
    fun encodesNativeEvents() {
        val event = WebMessageCodec.encode("NFC_DETECTED", session)
        assertTrue(event.contains(""""tagId":"ABC123""""))
        assertTrue(event.contains(""""session":"$session""""))
        assertTrue(WebMessageCodec.encode("CONNECTED", session).contains(""""payload":{}"""))
    }

    /** 試験ごとのペイロードを共通のエンベロープに包む。 */
    private fun decode(
        type: String,
        payload: String,
    ): WebMessage? = WebMessageCodec.decode("""{"type":"$type","session":"$session","payload":$payload}""")

    /** 数値と文字列の型違いも入力できるテスト用JSONを組み立てる。 */
    private fun user(
        id: String = "1",
        name: String = "\"Leanne Graham\"",
    ): String = """{"id":$id,"name":$name}"""
}
