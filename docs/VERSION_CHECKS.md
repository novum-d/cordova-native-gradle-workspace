# バージョンの追加検証（2026-10-19）

現在のルール維持方針は[静的解析と整形ルール](STATIC_ANALYSIS.md)を参照してください。[追加ルールの試行記録](logs/versions/rule-refresh-2026-10-25/README.md)は採用を見送った案の記録であり、現在の設定には適用していません。

以下は2026-10-19の検証記録です。専用スクリプト・比較用init scriptは削除済みで、通常の品質チェックはdetekt・detekt-formattingを使います。

## ktlint・detekt

| 対象 | 当時の確認版 | 検証結果 |
| --- | --- | --- |
| ktlint Gradle Plugin | 14.2.0 | 当時の一時プロジェクトに適用して実行成功。専用Catalog定義は削除済み |
| ktlint本体 | 1.8.0 | 整形済みコードを受理、故意の末尾空白を検出して失敗、整形後に成功。専用Catalog定義は削除済み |
| detekt Plugin・本体・formatting | 1.23.8 | Catalogの定義は既に最新。通常解析・整形チェックの実行成功 |

版は[ktlint Plugin Portal](https://plugins.gradle.org/plugin/org.jlleitschuh.gradle.ktlint)、[ktlint本体のリリース](https://github.com/ktlint/ktlint/releases/tag/1.8.0)、[detektの安定リリース](https://github.com/detekt/detekt/releases/tag/v1.23.8)で確認しました。detekt 2.0のalpha版は今回の最新安定版には含めません。

```sh
# 現在のnativeソースをdetektで解析・整形チェック
./scripts/with-env.sh ./native/gradlew -p native \
  detekt detektFormatCheck --rerun-tasks --console=plain
```

当時のktlintの検証では、一時GradleプロジェクトへKotlin JVM Plugin 1.9.24とktlint Pluginを適用しました。`native/feature/src`・`native/data/src`のコピーと小さな検証ソースを対象にするため、元の作業ファイルは整形していません。チェック時に不正なファイルが変更されないことも照合し、自動整形とその後のチェックは別のGradle呼び出しで実行しました。ログ中の1回の`BUILD FAILED`は故意の末尾空白を検出した期待どおりの結果です。[ktlint実行ログ](logs/versions/quality-ktlint-latest.log)

通常のnative・Cordovaルートの`ktlintCheck`は引き続きdetektFormatCheckへの互換名です。detekt-formattingに内包されたktlintを1.8.0へ差し替えたわけではありません。今回1.8.0を実行したのは上記の独立した検証です。[detekt実行ログ](logs/versions/quality-detekt-latest.log)

## Compose BOM

Google Mavenの[公開版一覧](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/maven-metadata.xml)で最新安定版2026.09.00を確認し、現在のKotlin 1.9.24／Compose Compiler 1.5.14／compileSdk 35のままビルドを比較しました。

| BOM | 結果 | 証跡 |
| --- | --- | --- |
| 最新版2026.09.00 | `:feature:compileDebugKotlin`で失敗。UI・Runtime・Foundation 1.12.1などのメタデータが2.1.0で、Kotlin 1.9.24では読み取れない。推移依存のstdlibも2.1.20へ更新される | [依存解決・失敗ログ](logs/versions/compose-bom-latest-probe.log) |
| 採用版2025.07.00 | 同じ検証コードでfeature・dataのDebug AARビルド成功 | [成功ログ](logs/versions/compose-bom-selected-probe.log) |

BOM比較用のinit scriptと検証ソースは削除済みです。表・ログは当時の実測を記録しています。2025.07.00と最新版の間にある全BOMを検証したものではなく、2025.07.00が互換性のある最新版の上限という意味ではありません。

現在の採用版は、通常のビルドで確認できます。

```sh
npm run native:build
npm run cordova:build
```

## Serialization JSON

1.6.3を維持する理由を、1.7系で必要となるCompiler Pluginの要件から説明するようREADMEを修正しました。[1.7.0の注意事項](https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.7.0)と、そこから参照される[1.7.0-RCの説明](https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.7.0-RC)では、Compiler Plugin 2.0.0-RC1以降が必要で、古いPluginは同ランタイムを拒否すると説明しています。現在のSerialization Plugin 1.9.24との不整合を避けるための判断です。今回1.7系への変更・ビルド試験は行っていません。
