# 通常のモジュール依存と日時API（2026-10-19）

Version Catalogで想定していたライブラリを、通常の`feature`・`data`へ追加しました。以下の依存を通常のビルドで利用できます。版はすべて[`libs.versions.toml`](../native/gradle/libs.versions.toml)で管理します。

選定方針、各バージョンを採用・見送りとした理由、公式資料へのリンクは[依存関係バージョン妥当性検証](DEPENDENCY_VERSION_SELECTION.md)にまとめています。

2026-10-22にCordovaプラグインの編集用モジュール`cordova-plugin-dev`を追加しました。`libs.cordova.framework`（`org.apache.cordova:framework:14.0.1`）は同モジュールの`compileOnly`です。Cordova APIをAndroid Studioで解決するための依存で、アプリへCordovaを二重に組み込みません。APKでは既存のCordovaLibが実装を提供します。[プラグイン構成](../kotlin-plugin/README.md)

## 配置

| 配置先 | 依存・設定 |
| --- | --- |
| feature | Core KTX、Compose BOM・UI・Foundation・Layout・Material3・Preview、Activity Compose、Lifecycle Runtime KTX／Compose・ViewModel Compose、WebKit、dataプロジェクト |
| featureのDebugのみ | Compose UI Tooling。Releaseへは追加しない |
| data | DataStore Preferences・Core、Serialization JSON、WorkManager、Retrofit、OkHttp・Logging Interceptor |
| 両方のapi | `libs.bundles.native.common`。Coroutines Core・Android、Immutable Collections、Kotlinx Datetime、kotlin-result、Hilt Android、Timber |
| 両方の単体テスト | JUnit 4、Coroutines Test（本体と同じ1.8.1） |
| 両方のAndroidテスト | AndroidX Test Core KTX・Ext JUnit KTX |
| 両方のコード生成 | Hilt Plugin・KSP Pluginと`ksp(libs.hilt.compiler)` |
| dataのコード生成 | Kotlin Serialization Plugin |

共通の型・非同期処理・ログ・DIは、両モジュールの`api(libs.bundles.native.common)`で利用側へ公開します。共通依存の一覧はVersion Catalogの`[bundles].native-common`で管理します。`feature`に依存する`cordova-plugin-dev`やCordovaのappからも、この7依存を直接参照できます。`feature`から`data`へのプロジェクト依存は`implementation`のままです。

モジュール固有の依存は`implementation`、Toolingは`debugImplementation`、テストは`testImplementation`・`androidTestImplementation`で宣言しています。Hilt Compilerは各モジュールの`ksp`、Hilt・KSP Pluginは各モジュールの`plugins`で設定します。`api`はライブラリを公開する設定であり、PluginやProcessorの適用を利用側へ引き継ぐものではありません。detekt・ktlint・JGitなどの開発ツールはAndroidの依存へ追加していません。

2026-10-20に[単体テスト環境](UNIT_TESTS.md)を整備しました。両モジュールの`src/test/`に環境確認テストを追加し、JUnit 4・Coroutines TestとAGPのJaCoCo連携で実行・レポート生成を行います。Coroutines Testは`testImplementation`で、JaCoCoはビルド時ツールです。

featureのCompose Compilerを常時有効にしました。AGP・Kotlinは従来のnative／Cordova基盤を使い、Hilt・KSP・Serializationは利用するモジュールのCatalog aliasで適用します。Hilt用にCordovaの生成GradleやApplicationを書き換える処理は追加していません。

## Kotlinx Datetime

`org.jetbrains.kotlinx:kotlinx-datetime:0.6.2`は共通bundleを通して両モジュールの`api`へ追加しています。個別のaliasは`libs.kotlinx.datetime`です。

同版の[JVM POM](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-datetime-jvm/0.6.2/kotlinx-datetime-jvm-0.6.2.pom)は`kotlin-stdlib:1.9.21`を参照し、`Instant`のメタデータは`mv=[1,9,0]`です。このworkspaceのKotlin 1.9.24で実コンパイルを確認しました。[0.7.0でのInstant／Clockの移動](https://github.com/Kotlin/kotlinx-datetime/releases/tag/v0.7.0)を伴うKotlin 2系への移行を避けており、ライブラリ全体の最新版という意味ではありません。

```kotlin
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

val instant = Instant.parse("2026-10-19T00:00:00Z")
val local = instant.toLocalDateTime(TimeZone.of("Asia/Tokyo"))
```

## 現在の依存解決・コンパイルの確認

独自のCatalog検証タスクとコンパイル用fixtureは削除しました。通常のビルドで依存解決と本体コードのコンパイルを行い、依存一覧はGradle標準の`dependencies`で確認します。

```sh
# feature・dataの本体をビルドする。
npm run native:build

# 個別の構成の依存を確認する。releaseやテスト構成も--configurationで指定できる。
./scripts/with-env.sh ./native/gradlew -p native :feature:dependencies --configuration debugRuntimeClasspath
./scripts/with-env.sh ./native/gradlew -p native :data:dependencies --configuration debugRuntimeClasspath

# Cordova側から同じモジュールの依存を確認する。
./scripts/with-env.sh ./native/gradlew -p CordovaComposeApp/platforms/android \
  :feature:dependencies --configuration debugRuntimeClasspath
```

### 共通依存のapi公開確認（2026-10-23）

- nativeの3モジュールと、Cordovaのapp・feature・dataについて、Debug／Releaseのコンパイル依存に共通7依存が含まれることを確認しました。実行時の依存座標・解決版は変更前と一致しています。
- `cordova-plugin-dev`へ一時ソースだけを追加し、依存宣言の追加なしで日時・Result・不変コレクション・Flow・Main Dispatcher・Timber・Hiltの型をコンパイルできることを確認しました。検証ソースは管理対象へ追加せず、通常のソースで成果物を再生成しています。
- nativeのDebug／Release AAR、単体テスト、detekt・整形チェック、各モジュールのDebug／Release Android Lint、CordovaアプリのDebug／Release APKとAndroid Lintが成功しました。

## 依存選定時の検証記録

以下は独自タスク・検証ソースを削除する前の記録です。現在のビルドでは、これらの検証ソースやassetsを生成・追加しません。

native・Cordovaそれぞれ30構成で解決に成功し、両者の座標一覧も一致しました。結果は[native JSON](dependencies-resolution-native.json)・[Cordova JSON](dependencies-resolution-cordova.json)、ログは[native](dependencies-resolution-native.log)・[Cordova／APKビルド](dependencies-build.log)です。テスト依存は解決を確認したもので、実機でAndroidテストを実行したという意味ではありません。

通常の依存だけを使った検証ソースで、次を確認しています。[nativeログ](dependencies-native-compile.log)、[Cordovaログ](dependencies-cordova-compile.log)

- Compose、永続化・通信系のAPI参照と不変コレクションの操作
- 日時の解析、タイムゾーン変換、epoch millisecondsとの変換
- `@Serializable`な日時レコードのSerializer生成とJSON化
- `@Inject` RepositoryのFactory、`@HiltViewModel`のModule・Factory生成とコンパイル

当時の検証用ソースとassetsは専用プロパティを指定した実行時だけ追加していました。通常APKの前に両モジュールをcleanし、検証用assetsがAPKに残っていないことも確認しました。CordovaのApplicationでのHilt起動・実機での注入は未検証です。

Catalog全項目のライブラリ・Pluginを別構成で解決する従来の検証も成功しました。[ログ](dependencies-catalog.log)

## ビルド・品質

通常のDebug／Release APKビルドと、native・Cordovaのdetekt／整形チェック／Android Lintに成功しました。[ビルドログ](dependencies-build.log)、[チェックログ](dependencies-checks.log)、[Lint件数](dependencies-lint-results.json)

Cordova hookの4テストが成功しました。
