package dev.example.nativefeature

import dev.example.nativefeature.launch.FeatureLaunchRequest
import dev.example.nativefeature.navigation.FeatureDestination
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/** JS・IntentのJSON境界と、処理IDによる型付き画面の振り分けを確認する。 */
class FeatureLaunchRequestTest {
    /** 通信サンプルの要求を業務サービスの画面と区別して解決する。 */
    @Test
    fun sampleIdSelectsWebView() {
        val request = FeatureLaunchRequest.fromJson("""{"id":"sample.webview"}""")

        assertEquals(FeatureDestination.WebViewSample, request.toDestination())
    }

    /** homeの処理IDをホーム画面へ解決する。 */
    @Test
    fun homeIdSelectsHome() {
        val request = FeatureLaunchRequest.fromJson("""{"id":"home"}""")

        assertEquals(FeatureDestination.Home, request.toDestination())
    }

    /** サービスIDから読み取り画面を選び、省略されたargsはnullのまま渡す。 */
    @Test
    fun golfServiceIdSelectsHolderReadWithoutArgs() {
        val request = FeatureLaunchRequest.fromJson("""{"id":"golf-change.read-holder"}""")

        assertNull(request.args)
        assertEquals(FeatureDestination.GolfHolderRead(id = "golf-change.read-holder"), request.toDestination())
    }

    /** 旧来の機器名IDと対象外のNFC書き込みは、業務サービスの別名として受け付けない。 */
    @Test
    fun hardwareIdsAreUnsupported() {
        listOf("nfc.read", "nfc.write", "camera.capture").forEach { id ->
            assertThrows(IllegalArgumentException::class.java) {
                FeatureLaunchRequest(id = id).toDestination()
            }
        }
    }

    /** サービスの起動引数は省略・空・構造化JSONを区別し、読み取り対象の値を要求しない。 */
    @Test
    fun golfServicePreservesOptionalArgs() {
        val arguments = listOf(null, JsonObject(emptyMap()), buildJsonObject { put("source", "Web / ? #") })
        arguments.forEach { args ->
            val request = FeatureLaunchRequest.fromJson(FeatureLaunchRequest("golf-change.read-holder", args).toJson())

            assertEquals(
                FeatureDestination.GolfHolderRead(id = "golf-change.read-holder", args = args),
                request.toDestination(),
            )
        }
    }

    /** 空オブジェクトを省略と区別して、Intent保存と画面への変換の両方で保持する。 */
    @Test
    fun emptyArgsSurviveTheIntentBoundary() {
        val request = FeatureLaunchRequest.fromJson("""{"id":"golf-change.read-holder","args":{}}""")
        val restored = FeatureLaunchRequest.fromJson(request.toJson())

        assertEquals(request, restored)
        assertEquals(
            FeatureDestination.GolfHolderRead(id = "golf-change.read-holder", args = JsonObject(emptyMap())),
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
        val request = FeatureLaunchRequest(id = "golf-change.read-holder", args = args)
        val restored = FeatureLaunchRequest.fromJson(request.toJson())

        assertEquals(
            FeatureDestination.GolfHolderRead(id = "golf-change.read-holder", args = args),
            restored.toDestination(),
        )
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
        FeatureLaunchRequest.fromJson("""{"id":"golf-change.read-holder","args":[]}""")
    }
}
