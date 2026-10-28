package dev.example.nativefeature.webview

import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.runCatching
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

/** ページ識別子付きの受信メッセージ。識別子は再読み込み前の要求を除外し、認証には使わない。 */
internal sealed interface WebMessage {
    val session: String

    /** JavaScriptのイベント登録完了を通知する。 */
    data class Ready(override val session: String) : WebMessage

    /** 検証APIで取得したユーザーの表示情報を渡す。業務上の本人確認結果としては扱わない。 */
    data class UserFetched(
        override val session: String,
        val id: Int,
        val name: String,
    ) : WebMessage
}

/** UIから独立して、方向・サイズ・型を制限したJSONの解析と送信メッセージ生成を行う。 */
internal object WebMessageCodec {
    private const val MAX_MESSAGE_LENGTH = 16_384
    private const val MAX_DEPTH = 8
    private const val MAX_NAME_LENGTH = 256

    /** 不正・未知・逆方向のメッセージはnullにし、受信データを例外やログへ持ち出さない。 */
    fun decode(raw: String): WebMessage? {
        if (raw.length > MAX_MESSAGE_LENGTH || !hasSafeDepth(raw)) return null
        val root = runCatching { Json.parseToJsonElement(raw) }.getOrElse { cause ->
            if (cause !is SerializationException) throw cause
            null
        } as? JsonObject ?: return null
        val session = root.string("session")?.takeIf { it.length == 36 } ?: return null
        val payload = root["payload"] as? JsonObject ?: return null
        return when (root.string("type")) {
            "READY" -> if (payload.isEmpty()) WebMessage.Ready(session) else null
            "USER_FETCHED" -> decodeUser(session, payload)
            else -> null
        }
    }

    /** 固定の疑似タグと接続確認だけを送信し、JavaScriptコードを文字列連結しない。 */
    fun encode(
        type: String,
        session: String,
    ): String {
        require(type == "NFC_DETECTED" || type == "CONNECTED")
        return buildJsonObject {
            put("type", type)
            put("session", session)
            put("payload", buildJsonObject { if (type == "NFC_DETECTED") put("tagId", "ABC123") })
        }.toString()
    }

    /** 数値文字列や表示を乱す制御文字を拒否し、画面・ログへ渡す値を小さく保つ。 */
    private fun decodeUser(
        session: String,
        payload: JsonObject,
    ): WebMessage.UserFetched? {
        val idValue = payload["id"] as? JsonPrimitive ?: return null
        val id = idValue.takeUnless { it.isString }?.intOrNull?.takeIf { it > 0 } ?: return null
        val name = payload.string("name")
            ?.takeIf { it.isNotBlank() && it.length <= MAX_NAME_LENGTH && it.none(Char::isISOControl) }
            ?: return null
        return WebMessage.UserFetched(session, id, name)
    }

    /** JSONの文字列とプリミティブを区別し、暗黙の型変換を避ける。 */
    private fun JsonObject.string(key: String): String? = (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content

    /** JSON解析器へ渡す前に、文字列内の括弧を除いた深さを制限する。 */
    private fun hasSafeDepth(raw: String): Boolean {
        var depth = 0
        var quoted = false
        var escaped = false
        for (character in raw) {
            if (quoted) {
                when {
                    escaped -> escaped = false
                    character == '\\' -> escaped = true
                    character == '"' -> quoted = false
                }
                continue
            }
            when (character) {
                '"' -> quoted = true
                '{', '[' -> if (++depth > MAX_DEPTH) return false
                '}', ']' -> depth--
            }
        }
        return true
    }
}
