# バージョンの追加検証（2026-10-19）

## ktlint・detekt

| 対象 | 最新安定版 | 検証結果 |
| --- | --- | --- |
| ktlint Gradle Plugin | 14.2.0 | Catalogの定義は既に最新。実際に適用して実行成功 |
| ktlint本体 | 1.8.0 | Catalogへ`ktlint-engine`として追加。整形済みコードを受理、故意の末尾空白を検出して失敗、整形後に成功 |
| detekt Plugin・本体・formatting | 1.23.8 | Catalogの定義は既に最新。通常解析・整形チェックの実行成功 |

版は[ktlint Plugin Portal](https://plugins.gradle.org/plugin/org.jlleitschuh.gradle.ktlint)、[ktlint本体のリリース](https://github.com/ktlint/ktlint/releases/tag/1.8.0)、[detektの安定リリース](https://github.com/detekt/detekt/releases/tag/v1.23.8)で確認しました。detekt 2.0のalpha版は今回の最新安定版には含めません。

```sh
# ktlint Plugin 14.2.0＋本体1.8.0を実行
sh verification/verify-ktlint.sh

# 現在のnativeソースをdetektで解析・整形チェック
./scripts/with-env.sh ./native/gradlew -p native \
  detekt detektFormatCheck --rerun-tasks --console=plain
```

ktlintの検証は、一時GradleプロジェクトへKotlin JVM Plugin 1.9.24とktlint Pluginを適用します。`native/feature/src`・`native/data/src`のコピーと小さな検証ソースを対象にするため、元の作業ファイルは整形しません。チェック時に不正なファイルが変更されないことも照合し、自動整形とその後のチェックは別のGradle呼び出しで実行します。ログ中の1回の`BUILD FAILED`は故意の末尾空白を検出した期待どおりの結果です。[ktlint実行ログ](quality-ktlint-latest.log)

通常のnative・Cordovaルートの`ktlintCheck`は引き続きdetektFormatCheckへの互換名です。detekt-formattingに内包されたktlintを1.8.0へ差し替えたわけではありません。今回1.8.0を実行したのは上記の独立した検証です。[detekt実行ログ](quality-detekt-latest.log)

## Compose BOM

Google Mavenの[公開版一覧](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/maven-metadata.xml)で最新安定版2026.09.00を確認し、現在のKotlin 1.9.24／Compose Compiler 1.5.14／compileSdk 35のままビルドを比較しました。

| BOM | 結果 | 証跡 |
| --- | --- | --- |
| 最新版2026.09.00 | `:feature:compileDebugKotlin`で失敗。UI・Runtime・Foundation 1.12.1などのメタデータが2.1.0で、Kotlin 1.9.24では読み取れない。推移依存のstdlibも2.1.20へ更新される | [依存解決・失敗ログ](compose-bom-latest-probe.log) |
| 採用版2025.07.00 | 同じ検証コードでfeature・dataのDebug AARビルド成功 | [成功ログ](compose-bom-selected-probe.log) |

```sh
# 一時的に最新BOMを追加し、選択された依存版とコンパイル失敗を確認
./scripts/with-env.sh ./native/gradlew -p native -PverifyCatalog=true \
  -PcomposeBomProbe=2026.09.00 -I ../verification/compose-bom-probe.init.gradle \
  :feature:assembleDebug :data:assembleDebug --console=plain

# 比較用のinitスクリプトを使わず、採用版でビルド
./scripts/with-env.sh ./native/gradlew -p native -PverifyCatalog=true \
  :feature:assembleDebug :data:assembleDebug --console=plain
```

比較用initスクリプトはCatalogを書き換えず、`-PverifyCatalog=true`による検証用依存へBOMを追加します。最新版を使えないことは今回の実測で確認しました。以前の記録には最新版での失敗を示すログがなかったため、過去に同じ比較を行ったと断定はしません。また、2025.07.00と最新版の間にある全BOMを検証したものではなく、2025.07.00が互換性のある最新版の上限という意味ではありません。

## Serialization JSON

1.6.3を維持する理由を、1.7系で必要となるCompiler Pluginの要件から説明するようREADMEを修正しました。[1.7.0の注意事項](https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.7.0)と、そこから参照される[1.7.0-RCの説明](https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.7.0-RC)では、Compiler Plugin 2.0.0-RC1以降が必要で、古いPluginは同ランタイムを拒否すると説明しています。現在のSerialization Plugin 1.9.24との不整合を避けるための判断です。今回1.7系への変更・ビルド試験は行っていません。
