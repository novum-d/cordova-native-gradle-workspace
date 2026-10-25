package dev.example.nativefeature

import dev.example.nativefeature.launch.FeatureLaunchRequest
import dev.example.nativefeature.navigation.FeatureDestination
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** JS・IntentのJSON境界と、処理IDによる型付き画面の振り分けを確認する。 */
class FeatureLaunchRequestTest {
    /** homeの処理IDをホーム画面へ解決する。 */
    @Test
    fun homeIdSelectsHome() {
        val request = FeatureLaunchRequest.fromJson("""{"id":"home"}""")

        assertEquals(FeatureDestination.Home, request.toDestination())
    }

    /** args省略のNFC要求を、nullを保持したままNFC画面へ渡す。 */
    @Test
    fun nfcIdSelectsNfcWithoutArgs() {
        val request = FeatureLaunchRequest.fromJson("""{"id":"nfc.read"}""")

        assertNull(request.args)
        assertEquals(FeatureDestination.NfcRead(id = "nfc.read"), request.toDestination())
    }

    /** 書き込み要求を読み取り画面と区別し、省略・空・構造化argsをIntent境界で保持する。 */
    @Test
    fun nfcWriteSelectsWriteWithOptionalArgs() {
        val arguments = listOf(null, JsonObject(emptyMap()), buildJsonObject { put("text", "書き込み %2F") })
        arguments.forEach { args ->
            val request = FeatureLaunchRequest.fromJson(FeatureLaunchRequest("nfc.write", args).toJson())

            assertEquals(FeatureDestination.NfcWrite(id = "nfc.write", args = args), request.toDestination())
        }
    }

    /** 撮影要求をCamera画面へ振り分け、省略・空・構造化argsをIntent境界で保持する。 */
    @Test
    fun cameraCaptureSelectsCameraWithOptionalArgs() {
        val arguments = listOf(null, JsonObject(emptyMap()), buildJsonObject { put("source", "撮影 / ? #") })
        arguments.forEach { args ->
            val request = FeatureLaunchRequest.fromJson(FeatureLaunchRequest("camera.capture", args).toJson())

            assertEquals(FeatureDestination.CameraCapture(id = "camera.capture", args = args), request.toDestination())
        }
    }

    /** 空オブジェクトを省略と区別して、Intent保存と画面への変換の両方で保持する。 */
    @Test
    fun emptyArgsSurviveTheIntentBoundary() {
        val request = FeatureLaunchRequest.fromJson("""{"id":"nfc.read","args":{}}""")
        val restored = FeatureLaunchRequest.fromJson(request.toJson())

        assertEquals(request, restored)
        assertEquals(
            FeatureDestination.NfcRead(id = "nfc.read", args = JsonObject(emptyMap())),
            restored.toDestination(),
        )
    }

    /** 呼び出し元の日本語・予約文字・数値・ネストしたargsを画面へ欠落なく引き継ぐ。 */
    @Test
    fun structuredArgsAreForwardedToTheSelectedScreen() {
        val args = buildJsonObject {
            put("text", "日本語 / ? & = # %2F +")
            put("count", 2)
            put("options", buildJsonObject { put("enabled", true) })
        }
        val request = FeatureLaunchRequest(id = "nfc.read", args = args)
        val restored = FeatureLaunchRequest.fromJson(request.toJson())

        assertEquals(FeatureDestination.NfcRead(id = "nfc.read", args = args), restored.toDestination())
    }

    /** 未対応IDでホームなどへ暗黙にフォールバックしない。 */
    @Test(expected = IllegalArgumentException::class)
    fun unsupportedIdIsRejected() {
        FeatureLaunchRequest.fromJson("""{"id":"unknown"}""").toDestination()
    }

    /** 必須idが欠落したJSONはIntentへ渡す前に拒否する。 */
    @Test(expected = SerializationException::class)
    fun missingIdIsRejected() {
        FeatureLaunchRequest.fromJson("""{"args":{}}""")
    }

    /** JSから数値idが渡されてもStringへ暗黙変換しない。 */
    @Test(expected = SerializationException::class)
    fun numericIdIsRejected() {
        FeatureLaunchRequest.fromJson("""{"id":123}""")
    }

    /** JSONオブジェクト以外のargsは拒否する。 */
    @Test(expected = SerializationException::class)
    fun nonObjectArgsAreRejected() {
        FeatureLaunchRequest.fromJson("""{"id":"nfc.read","args":[]}""")
    }
}
