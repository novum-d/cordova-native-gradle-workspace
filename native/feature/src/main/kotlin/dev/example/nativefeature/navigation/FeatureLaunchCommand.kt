package dev.example.nativefeature.navigation

/** 同じ処理ID・argsの連続要求も区別する、保存対象外の一時的な起動コマンド。 */
internal class FeatureLaunchCommand(val destination: FeatureDestination)
