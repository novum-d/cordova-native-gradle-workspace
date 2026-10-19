# 通常のモジュール依存と日時API（2026-10-19）

Version Catalogで想定していたライブラリを、通常の`feature`・`data`へ追加しました。`-PverifyCatalog=true`を付けなくても、以下の依存を利用できます。版はすべて[`libs.versions.toml`](../native/gradle/libs.versions.toml)で管理します。

## 配置

| 配置先 | 依存・設定 |
| --- | --- |
| feature | Core KTX、Compose BOM・UI・Foundation・Layout・Material3・Preview、Activity Compose、Lifecycle Runtime KTX／Compose・ViewModel Compose、WebKit、dataプロジェクト |
| featureのDebugのみ | Compose UI Tooling。Releaseへは追加しない |
| data | DataStore Preferences・Core、Serialization JSON、WorkManager、Retrofit、OkHttp・Logging Interceptor |
| 両方 | Coroutines Core・Android、Immutable Collections、Kotlinx Datetime、kotlin-result、Hilt Android、Timber |
| 両方の単体テスト | JUnit 4 |
| 両方のAndroidテスト | AndroidX Test Core KTX・Ext JUnit KTX |
| 両方のコード生成 | Hilt Plugin・KSP Pluginと`ksp(libs.hilt.compiler)` |
| dataのコード生成 | Kotlin Serialization Plugin |

通常依存は`implementation`、Toolingは`debugImplementation`、テストは`testImplementation`・`androidTestImplementation`で宣言しています。detekt・ktlint・JGitなどの開発ツールはAndroidの依存へ追加していません。

featureのCompose Compilerを常時有効にしました。AGP・Kotlinは従来のnative／Cordova基盤を使い、Hilt・KSP・Serializationは利用するモジュールのCatalog aliasで適用します。Hilt用にCordovaの生成GradleやApplicationを書き換える処理は追加していません。

## Kotlinx Datetime

`org.jetbrains.kotlinx:kotlinx-datetime:0.6.2`を両モジュールへ追加しました。使用するaliasは`libs.kotlinx.datetime`です。

同版の[JVM POM](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-datetime-jvm/0.6.2/kotlinx-datetime-jvm-0.6.2.pom)は`kotlin-stdlib:1.9.21`を参照し、`Instant`のメタデータは`mv=[1,9,0]`です。このworkspaceのKotlin 1.9.24で実コンパイルを確認しました。[0.7.0でのInstant／Clockの移動](https://github.com/Kotlin/kotlinx-datetime/releases/tag/v0.7.0)を伴うKotlin 2系への移行を避けており、ライブラリ全体の最新版という意味ではありません。

```kotlin
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

val instant = Instant.parse("2026-10-19T00:00:00Z")
val local = instant.toLocalDateTime(TimeZone.of("Asia/Tokyo"))
```

## 依存解決・コンパイルの確認

```sh
# 通常の実依存。Debug／Release・単体テスト・Androidテスト・KSPを解決
npm run verify:dependencies

# 同じ検査をCordovaホストから実行
./scripts/with-env.sh ./native/gradlew -p CordovaComposeApp/platforms/android verifyModuleDependencies

# 検証ソースによる実コンパイル
npm run verify:compile
```

`verifyModuleDependencies`は依存の解決をタスク実行時に行い、各構成の実際の外部Artifact座標を`build/reports/catalog/module-dependencies.json`へ出力します。Catalog全項目を一括解決する`verifyCatalogResolution`とは別に、モジュールへ設定したクラスパスを確認するためのタスクです。

native・Cordovaそれぞれ30構成で解決に成功し、両者の座標一覧も一致しました。結果は[native JSON](dependencies-resolution-native.json)・[Cordova JSON](dependencies-resolution-cordova.json)、ログは[native](dependencies-resolution-native.log)・[Cordova／APKビルド](dependencies-build.log)です。テスト依存は解決を確認したもので、実機でAndroidテストを実行したという意味ではありません。

通常の依存だけを使った検証ソースで、次を確認しています。[nativeログ](dependencies-native-compile.log)、[Cordovaログ](dependencies-cordova-compile.log)

- Compose、永続化・通信系のAPI参照と不変コレクションの操作
- 日時の解析、タイムゾーン変換、epoch millisecondsとの変換
- `@Serializable`な日時レコードのSerializer生成とJSON化
- `@Inject` RepositoryのFactory、`@HiltViewModel`のModule・Factory生成とコンパイル

生成する検証用ソースとassetsは`verifyCatalog=true`時だけ追加します。通常APKの前に両モジュールをcleanし、検証用assetsがAPKに残っていないことも確認しました。CordovaのApplicationでのHilt起動・実機での注入は未検証です。

Catalog全項目のライブラリ・Pluginを別構成で解決する従来の検証も成功しました。[ログ](dependencies-catalog.log)

## ビルド・品質・ライセンス

通常のDebug／Release APKビルドと、native・Cordovaのdetekt／整形チェック／Android Lintに成功しました。[ビルドログ](dependencies-build.log)、[チェックログ](dependencies-checks.log)、[Lint件数](dependencies-lint-results.json)

ライセンス台帳はDebug 115件・Release 112件へ更新しました。Debug専用のTooling 3件をvariant単位で照合します。ISC・BSD・MPL・CC-BYの原文と必要な通知・帰属、OkHttpのPublic Suffix Listのルール原文もAPKに含めています。両APKの通知が管理元と一致することを確認しました。[APK照合](license-apk-check.json)、[ライセンス確認の根拠](LICENSE_COMPLIANCE.md)

ライセンスの25テストとCordova hookの4テストが成功しました。ライセンス検証は未承認依存だけでなく、Debug専用依存のRelease混入、Debug側からの欠落、レビュー済み追加原文の改変も拒否します。[25テストのログ](dependencies-license-tests.log)
