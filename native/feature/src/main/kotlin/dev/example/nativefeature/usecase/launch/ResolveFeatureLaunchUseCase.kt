package dev.example.nativefeature.usecase.launch

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toResultOr
import dev.example.nativefeature.err.launch.ResolveFeatureLaunchErr
import dev.example.nativefeature.launch.FeatureLaunchRequest
import dev.example.nativefeature.navigation.FeatureDestination
import javax.inject.Inject

/** 全サービス共通の起動境界。解析・画面解決をResultでつなぎ、UIの副作用は実行しない。 */
class ResolveFeatureLaunchUseCase @Inject constructor() {
    /** 未指定・JSON形式不正・未対応IDを区別し、途中で失敗した要求の後続処理を止める。 */
    operator fun invoke(requestJson: String?): Result<FeatureDestination, ResolveFeatureLaunchErr> =
        requestJson.toResultOr { ResolveFeatureLaunchErr.Request.Missing }
            .andThen(::parseRequest)
            .andThen(::resolveDestination)

    /** 入力形式の例外だけを分類し、取消・JVM Error・予期しない実装例外は伝播する。 */
    private fun parseRequest(requestJson: String): Result<FeatureLaunchRequest, ResolveFeatureLaunchErr.Request> =
        runCatching { FeatureLaunchRequest.fromJson(requestJson) }.mapError { cause ->
            if (cause !is IllegalArgumentException) throw cause
            ResolveFeatureLaunchErr.Request.InvalidJson(cause)
        }

    /** 画面選択は既存のID定義へ委譲し、未対応IDの原因を保持する。 */
    private fun resolveDestination(request: FeatureLaunchRequest): Result<FeatureDestination, ResolveFeatureLaunchErr> =
        runCatching { request.toDestination() }.mapError { cause ->
            if (cause !is IllegalArgumentException) throw cause
            ResolveFeatureLaunchErr.Destination.UnsupportedId(request.id, cause)
        }
}
