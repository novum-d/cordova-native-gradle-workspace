package dev.example.nativefeature.usecase.launch

import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import dev.example.nativefeature.err.launch.ResolveFeatureLaunchErr
import dev.example.nativefeature.navigation.FeatureDestination
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Activityを使わず、起動境界の成功値と発生箇所が分かる具体的な失敗型を確認する。 */
class ResolveFeatureLaunchUseCaseTest {
    private val resolve = ResolveFeatureLaunchUseCase()

    /** Extraの未指定を、JSON文字列の解析失敗と区別する。 */
    @Test
    fun reportsMissingRequest() {
        assertEquals(ResolveFeatureLaunchErr.Request.Missing, resolve(null).unwrapError())
    }

    /** 壊れたJSONを入力由来の失敗として返し、解析器の原因を保持する。 */
    @Test
    fun reportsMalformedJson() {
        val err = resolve("{").unwrapError() as ResolveFeatureLaunchErr.Request.InvalidJson

        assertTrue(err.cause is SerializationException)
    }

    /** 必須項目・型・未知の項目を、画面選択前に拒否する。 */
    @Test
    fun rejectsInvalidRequestShape() {
        listOf(
            """{"args":{}}""",
            """{"id":123}""",
            """{"id":"home","args":[]}""",
            """{"id":"home","args":"invalid"}""",
            """{"id":"home","unexpected":true}""",
        ).forEach { json ->
            assertTrue(resolve(json).unwrapError() is ResolveFeatureLaunchErr.Request.InvalidJson)
        }
    }

    /** JSONとして有効でも、起動要求オブジェクトでない値は受け付けない。 */
    @Test
    fun rejectsNonObjectJson() {
        listOf("null", "[]", "42", "\"home\"").forEach { json ->
            assertTrue(resolve(json).unwrapError() is ResolveFeatureLaunchErr.Request.InvalidJson)
        }
    }

    /** 既知の共通画面を正常な型付きルートとして返す。 */
    @Test
    fun resolvesCommonScreens() {
        assertEquals(FeatureDestination.Home, resolve("""{"id":"home"}""").unwrap())
        assertEquals(FeatureDestination.WebViewSample, resolve("""{"id":"sample.webview"}""").unwrap())
    }

    /** サービスIDと、引数の未指定・空オブジェクトの違いを維持する。 */
    @Test
    fun preservesOptionalServiceArgs() {
        assertEquals(
            FeatureDestination.GolfHolderRead(id = "golf-change.read-holder"),
            resolve("""{"id":"golf-change.read-holder"}""").unwrap(),
        )
        assertEquals(
            FeatureDestination.GolfHolderRead(id = "golf-change.read-holder", args = JsonObject(emptyMap())),
            resolve("""{"id":"golf-change.read-holder","args":{}}""").unwrap(),
        )
    }

    /** 正常な構造化引数を、ROPの各段階で欠落・文字列化せず画面へ渡す。 */
    @Test
    fun preservesStructuredArgs() {
        val args = buildJsonObject {
            put("text", "日本語 / ? & = # %2F +")
            put("options", buildJsonObject { put("enabled", true) })
        }
        val json = """{"id":"golf-change.read-holder","args":$args}"""

        assertEquals(
            FeatureDestination.GolfHolderRead(id = "golf-change.read-holder", args = args),
            resolve(json).unwrap(),
        )
    }

    /** 未対応IDはJSON形式不正と区別し、ホーム等へ暗黙に遷移させない。 */
    @Test
    fun reportsUnsupportedId() {
        listOf("unknown", "nfc.read", "nfc.write", "camera.capture").forEach { id ->
            val err = resolve("""{"id":"$id"}""").unwrapError() as ResolveFeatureLaunchErr.Destination.UnsupportedId

            assertEquals(id, err.id)
            assertNotNull(err.cause)
        }
    }
}
