package dev.example.nativefeature

import dev.example.nativefeature.navigation.FeatureDestination
import dev.example.nativefeature.navigation.JsonObjectNavType
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** ゴルフ場釣り銭機サービスの画面引数について、省略・JSON保存・不正な入力の境界を確認する。 */
class FeatureDestinationTest {
    /** argsを省略した入力を受理し、保存時も不要なargsを追加しない。 */
    @Test
    fun omittedArgsRemainOptional() {
        val request = Json.decodeFromString<FeatureDestination.GolfHolderRead>("""{"id":"read"}""")

        assertEquals("read", request.id)
        assertNull(request.args)
        assertEquals("""{"id":"read"}""", Json.encodeToString(FeatureDestination.GolfHolderRead.serializer(), request))
    }

    /** 空オブジェクトをnullと区別し、保存・復元後にも保持する。 */
    @Test
    fun emptyArgsRemainAnObject() {
        val request = FeatureDestination.GolfHolderRead(id = "read", args = JsonObject(emptyMap()))
        val saved = Json.encodeToString(FeatureDestination.GolfHolderRead.serializer(), request)

        assertEquals(request, Json.decodeFromString<FeatureDestination.GolfHolderRead>(saved))
        assertEquals("{}", JsonObjectNavType.parseValue("{}")?.toString())
    }

    /** 日本語・URI予約文字・数値・ネストしたJSONを欠落や二重デコードなしで保持する。 */
    @Test
    fun structuredArgsPreserveValuesAndReservedCharacters() {
        val args = buildJsonObject {
            put("text", "日本語 / ? & = # %2F +")
            put("count", 2)
            put("options", buildJsonObject { put("enabled", true) })
        }
        val request = FeatureDestination.GolfHolderRead(id = "処理 / ? # %2F", args = args)
        val saved = Json.encodeToString(FeatureDestination.GolfHolderRead.serializer(), request)

        assertEquals(request, Json.decodeFromString<FeatureDestination.GolfHolderRead>(saved))
        assertEquals(args, JsonObjectNavType.parseValue(args.toString()))
        assertNull(JsonObjectNavType.parseValue("null"))
    }

    /** 処理IDがない入力を拒否し、必須項目の欠落を画面へ持ち込まない。 */
    @Test(expected = SerializationException::class)
    fun missingIdIsRejected() {
        Json.decodeFromString<FeatureDestination.GolfHolderRead>("""{"args":{}}""")
    }

    /** argsへの配列指定を拒否し、要求したJSONオブジェクトの型を守る。 */
    @Test(expected = SerializationException::class)
    fun nonObjectArgsAreRejected() {
        JsonObjectNavType.parseValue("[]")
    }
}
