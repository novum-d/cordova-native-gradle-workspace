# Cordova Android 14 依存関係バージョン妥当性検証

初回検証日: 2026-10-16<br>
再選定日: 2026-10-17<br>
実装・KDoc生成の確認日: 2026-10-22

## 結論と選定方針

**Cordovaの対応範囲を最優先し、その基盤で利用できる安定版をできるだけ新しくする。** 前回の「既存版を凍結して維持する」方針から変更する。

本メモの主案は、既存のCordova Android 14を維持する前提で、**cordova-android 14.0.1、AGP 8.7.3、Gradle 8.13、Kotlin 1.9.24、JDK 17、compileSdk／targetSdk 35、minSdk 24**を基準とする。Compose BOMは**2025.07.00（主要Compose 1.8.3、Material 3 1.3.2）**へ更新する。

選定の優先順位は次のとおり。

1. 対象`cordova-android`の対応Android APIと公式採用のビルド基盤を守る。
2. Cordova自身が依存するAppCompat、WebKitなどは公式採用版に合わせる。
3. その他のライブラリは、上記のSDK・AGP・Kotlinを維持できる範囲で新しい安定版を選ぶ。直接依存だけでなく推移依存も確認する。
4. 型・APIの移行が必要な更新は、その変更点を明示する。alpha／beta／RC、互換性チェックの無効化、要求版を下げる強制解決は主案に含めない。

**Cordovaが公式に指定する版と、本メモが追加ライブラリについて選んだ互換候補は区別する。** CordovaはCompose、Hilt、Retrofit等の全版について公式な対応表を提供していない。以下の追加ライブラリの選定は、公式リリース情報、公開POM／Gradle Module Metadata、AAR要件、Kotlinメタデータから判断した案であり、Cordova公式の動作保証を意味しない。

この文書は2026-10-23にObsidianのMemoから本プロジェクトの`docs/`へ移動した。以下は選定時点の候補と検証記録であり、未採用・削除済みの候補も含む。現在の採用版は[Version Catalog](../native/gradle/libs.versions.toml)、実際の依存配置は[依存関係ドキュメント](DEPENDENCIES.md)を管理元とする。

## 検証用workspaceへの反映状況（2026-10-22）

- `feature`・`data`へ依存を配置し、native単独とCordovaホストで共有している。Immutable Collectionsは0.3.8、Datetimeは0.6.2を追加済み。実際の採用版と依存方向は[Version Catalog](../native/gradle/libs.versions.toml)と[配置・解決結果](DEPENDENCIES.md)を管理元とする。
- Compose BOM 2025.07.00でビルドし、Cordova CLIからエミュレーターへインストール・起動、Compose表示・状態更新・Cordovaへの復帰を確認済み。[実行記録](COMPOSE_RUNTIME.md)。案件の既存アプリ環境でもビルド・起動できたとの報告を受けているが、このworkspaceの検証記録とは別の確認である。
- JVM単体テストはJUnit 4.13.2とCoroutines Test 1.8.1、カバレッジはJaCoCo 0.8.14を採用済み。[環境と対象範囲](UNIT_TESTS.md)。下表のMockK・Truth・Turbineは選定時の候補で、現在のworkspaceには追加していない。
- ktlintはPlugin 14.2.0／エンジン1.8.0を当時の独立した検証構成で確認したが、その構成は削除済み。通常の品質チェックとGit hookはdetekt 1.23.8とdetekt-formattingを使い、ktlint 0.50.0のルール・整形エンジンを推移依存として利用する。Git hookのHTML表示は、ルール違反時にdetekt標準レポートを開く。[品質チェックの構成](GIT_HOOKS.md)。
- 手書きコード・設定には処理のまとまりごとの概要説明を付ける。KDocで概要を把握できれば通常コメントの重複は不要。JSONはREADME等で説明し、生成ファイルは対象外とする。[記述ルール](../AGENTS.md)。

### KDocのHTML生成にDokka 2.2.0を採用

Dokkaは**記述済みのKDocをHTMLへ変換するビルド時のツール**として追加する。KDoc本文を自動執筆する機能ではない。公式リリースが示すKGP 1.9.0以上の条件を満たし、現在のKotlin 1.9.24／Gradle 8.13／AGP 8.7.3で生成できることを確認済み。アプリのKotlinや実行時依存をDokkaの解析エンジンへ合わせて更新しない。[Dokka 2.2.0リリース](https://github.com/Kotlin/dokka/releases/tag/v2.2.0)。

```sh
# 検証用workspace直下で、feature・dataの文書を集約する。
npm run docs:kdoc
# Gradleから直接実行する場合。先頭の:でルートの集約タスクを指定する。
./scripts/with-env.sh ./native/gradlew -p native :dokkaGeneratePublicationHtml
# macOSで生成したHTMLを開く。
open native/build/dokka/html/index.html
# Cordova prepare後、両モジュールの個別生成を実行する。
npm run docs:kdoc:cordova
```

対象は`src/main/kotlin`のpublic・protected APIとモジュール概要で、テスト・生成ソース・継承元だけのメソッドは掲載しない。dataは本体クラスがないため現状は個別ページを生成せず、役割を集約トップに記載する。KTS・Gradle・JavaScript・JSONはDokkaのAPI文書化対象に含めない。生成HTMLは`build/`配下としGit管理しない。構成・出力先・表示確認の記録は[KDoc生成ドキュメント](KDOC.md)にまとめる。

生成はnative集約・Cordova個別とも成功。Chrome 155でMainActivity・onCreateへの画面遷移と日本語表示を確認し、全8ページ・内部参照113件でリンク先やリソースの欠落、JavaScriptエラーは0件だった。確認用ブラウザーは終了済み。あわせてDebug／Release APK、静的解析・整形、Android Lint、単体テスト、30構成の依存解決、新規platformを含むbootstrapの12テストも成功した。

## Cordovaに合わせて固定する基盤

[Apache Cordovaの公式対応表](https://cordova.apache.org/docs/en/latest/guide/platforms/android/index.html)と、[14.0.1の既定設定](https://github.com/apache/cordova-android/blob/14.0.1/framework/cdv-gradle-config-defaults.json)を基準とする。

| 項目 | 再選定版・設定 | 選定理由 |
| --- | --- | --- |
| cordova-android | **14.0.1** | 14系の公開済み安定版。14.0.2のnightlyは採用しない。 |
| 対応Android API | **24〜35** | 14系の公式対応範囲。 |
| minSdk / compileSdk / targetSdk | **24 / 35 / 35** | 対応端末を狭めず、14系の上限へ合わせる。 |
| SDK Build Tools | **35.0.0** | 公式指定の35系。36系へ独立更新しない。 |
| AGP | **8.7.3** | Cordova 14の公式採用版。 |
| Gradle Wrapper | **8.13** | Cordova 14の公式採用版。AGP 8.7の最低要件8.9とは区別する。 |
| Kotlin / KGP | **1.9.24** | Cordova 14の公式採用版。1.9.25や2.xへ単独更新しない。 |
| Compose Compiler | **1.5.14** | Kotlin 1.9.24に対応する版。BOMとは別管理。 |
| JDK | **17** | Cordova 14の指定。JDKの17系パッチ更新は可能。 |
| AppCompat | **1.7.0** | Cordovaの既定依存と揃える。 |
| WebKit | **1.12.1** | Cordovaの既定依存と揃える。 |
| Core SplashScreen | **1.0.1** | Cordovaの既定依存と揃える。 |

Node.jsの公式最低要件は`>=20.5.0`。これは推奨するNode.jsの固定版や保守期限を表すものではなく、実プロジェクトのCordova CLI／CI要件と合わせて管理する。

KGP 1.9.24に対してGradle 8.13／AGP 8.7.3は、[JetBrainsの完全サポート範囲](https://kotlinlang.org/docs/gradle-configure-project.html#apply-the-plugin)を超える。ただし今回の優先順位では**Cordova公式の組み合わせを採用**し、この理由でGradleやAGPを下げない。警告・ビルド・テストの確認は残す。

参考: [AGP 8.7の互換表](https://developer.android.com/build/releases/agp-8-7-0-release-notes#compatibility)、[ComposeとKotlinの対応表](https://developer.android.com/jetpack/androidx/releases/compose-kotlin)、[Cordova 14.0.1リリースノート](https://github.com/apache/cordova-android/blob/14.0.1/RELEASENOTES.md)。

## 追加ライブラリの再選定

「維持」は互換条件による選択であり、単に変更を避ける判断ではない。Kotlinライブラリは、アプリ／テストの依存グラフがKotlin 2.xの標準ライブラリやリフレクションへ持ち上がらないことを基本条件とする。これも本メモの選定基準であり、Cordovaが個々の追加ライブラリに規定する公式上限ではない。

| ライブラリ／ツール | 前回版 | 再選定版 | 判断・上限の理由 |
| --- | --- | --- | --- |
| Compose BOM | 2024.06.00 | **2025.07.00** | 更新。UI／Foundation／Runtime 1.8.3、Material 3 1.3.2へ揃える。1.9系へ進むBOMはKotlin 2.0等の要件が上がるため主案から外す。[BOM POM](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2025.07.00/compose-bom-2025.07.00.pom)、[Compose UI](https://developer.android.com/jetpack/androidx/releases/compose-ui) |
| Core KTX | 1.13.1 | **1.16.0** | 更新。公開AARはcompileSdk 35、AGP 8.6.0以上、Kotlinメタデータ1.8.0。1.17.0でKGP 2.0以上が必須になる。[Core](https://developer.android.com/jetpack/androidx/releases/core#1.17.0) |
| Activity Compose | 1.9.3 | **1.10.1** | 更新。公開AARのminCompileSdk 35。1.11.0は36／AGP 8.9.1以上となり14の基盤を超える。[Activity](https://developer.android.com/jetpack/androidx/releases/activity) |
| Lifecycle | 2.8.7 | **2.8.7** | 維持。Compose 1.8.3側もこの世代を利用する。2.9系はKotlin 2.0世代となるため主案から外す。[Lifecycle](https://developer.android.com/jetpack/androidx/releases/lifecycle) |
| DataStore | 1.1.1 | **1.1.7** | 更新。1.1系の修正を取り込む。1.2.0はKotlin stdlib 2.0.21／Coroutines 1.9.0へ進むため主案から外す。[DataStore](https://developer.android.com/jetpack/androidx/releases/datastore) |
| WorkManager | 2.9.1 | **2.10.5** | 更新。2.10系の安定修正版で、公開AARのminCompileSdk 35、minSdk 21。2.11.0はKotlinメタデータ2.1.0／Coroutines 1.9.0となるため主案から外す。[WorkManager](https://developer.android.com/jetpack/androidx/releases/work#2.10.5) |
| Coroutines | 1.8.1 | **1.8.1** | 維持。1.9.0はstdlib 2.0.0に依存する。`core`／`android`／`test`を同じ版へ揃える。[1.9.0 POM](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core-jvm/1.9.0/kotlinx-coroutines-core-jvm-1.9.0.pom) |
| Immutable Collections | 未定義 | **0.3.8** | Kotlin 1.9.24で利用・実コンパイルを確認。0.4.0のKotlin 2.1.20要件を避ける。[0.4.0変更点](https://github.com/Kotlin/kotlinx.collections.immutable/releases/tag/v0.4.0) |
| Datetime | 未定義 | **0.6.2** | Kotlin 1.9.24で実コンパイルを確認。0.7系の`kotlin.time.Instant`への移行を避ける。[0.7.0変更点](https://github.com/Kotlin/kotlinx-datetime/releases/tag/v0.7.0) |
| Serialization runtime | 1.6.3 | **1.6.3** | 維持。1.7系が求めるSerialization Compiler Plugin 2.0.0-RC1以降と、現在のPlugin 1.9.24の不整合を避ける。Serialization Gradle PluginはKotlinと同じ1.9.24にする。[Serializationリリース](https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.7.0) |
| Retrofit | 2.11.0 | **2.11.0** | 維持。2.12.0の公開JARはKotlinメタデータ2.1.0。Java中心のライブラリでもArtifact内のKotlin部分を確認する。[2.12.0 JAR](https://repo.maven.apache.org/maven2/com/squareup/retrofit2/retrofit/2.12.0/retrofit-2.12.0.jar) |
| OkHttp | 4.12.0 | **4.12.0** | 維持。4系の安定版を採用し、Kotlin 2.xへ移行する5系は外す。Interceptor／MockWebServerも同版へ揃える。[OkHttp変更履歴](https://square.github.io/okhttp/changelogs/changelog/) |
| Hilt / Dagger | 2.51.1 | **2.52** | 更新。本体・Compiler・Gradle Plugin・テストを統一する。2.53の本体POMは既にstdlib 2.0.0-Beta5、2.53.1は2.0.21へ進むため、Pluginの版だけで判断しない。[2.52 POM](https://repo.maven.apache.org/maven2/com/google/dagger/hilt-android/2.52/hilt-android-2.52.pom)、[2.53 POM](https://repo.maven.apache.org/maven2/com/google/dagger/hilt-android/2.53/hilt-android-2.53.pom) |
| KSP（利用する場合） | 1.9.24-1.0.20 | **1.9.24-1.0.20** | Kotlin Compiler 1.9.24用を維持する。KAPT利用中なら追加は必須ではない。[KSPリリース](https://github.com/google/ksp/releases/tag/1.9.24-1.0.20) |
| Timber | 5.0.1 | **5.0.1** | 維持。公開済み安定版。[Timberリリース](https://github.com/JakeWharton/timber/releases/tag/5.0.1) |
| kotlin-result | 1.1.18 | **2.0.1** | 更新。ただし2.0でinline value class化されたためAPI移行が必要。公開JARはメタデータ1.9.0／stdlib 1.9.22。2.0.2はstdlib・メタデータとも2.2系。[移行説明](https://github.com/michaelbull/kotlin-result/releases/tag/2.0.0)、[2.0.1 POM](https://repo.maven.apache.org/maven2/com/michael-bull/kotlin-result/kotlin-result-jvm/2.0.1/kotlin-result-jvm-2.0.1.pom) |
| JUnit 4 | 4.13.2 | **4.13.2** | 維持。JUnit 4系の安定版。[JUnit 4リリース](https://github.com/junit-team/junit4/releases/tag/r4.13.2) |
| AndroidX Test Core | 1.6.1 | **1.7.0** | 更新。公開AARはKotlinメタデータ1.9.0、stdlib 1.9.21／Coroutines 1.8.1。minSdk 21で対象端末の範囲内。[Testリリース](https://developer.android.com/jetpack/androidx/releases/test) |
| AndroidX Test ext.junit | 1.2.1 | **1.3.0** | 更新。Core 1.7.0と同世代。`androidx.test.ext:junit`を指定する。[Testリリース](https://developer.android.com/jetpack/androidx/releases/test) |
| MockK | 1.13.12 | **1.13.11** | 整合性を優先して1版戻す。1.13.12以降のstdlib／reflect 2.0.0流入を避ける。1.13.12が必ず動かないという意味ではない。[1.13.11 POM](https://repo.maven.apache.org/maven2/io/mockk/mockk-jvm/1.13.11/mockk-jvm-1.13.11.pom)、[1.13.12 POM](https://repo.maven.apache.org/maven2/io/mockk/mockk-jvm/1.13.12/mockk-jvm-1.13.12.pom) |
| Truth（利用中なら） | 1.4.2 | **1.4.5** | 更新。未使用なら削除可能。削除は利用検索で判断し、バージョン互換性を理由にしない。[Truthリリース](https://github.com/google/truth/releases/tag/v1.4.5) |
| Turbine | 1.1.0 | **1.1.0** | 維持。1.2.0はstdlib 2.0.21／Coroutines 1.9.0に依存する。[1.2.0 POM](https://repo.maven.apache.org/maven2/app/cash/turbine/turbine-jvm/1.2.0/turbine-jvm-1.2.0.pom) |
| ktlint Gradle Plugin | 12.1.1 | **14.2.0** | 更新候補。公開安定版。Gradle Plugin API属性は7.4、Pluginの言語／API版はKotlin 1.8。アプリのKotlinを更新せずに利用する。ktlintエンジンの版とは別管理。[14.2.0リリース](https://github.com/JLLeitschuh/ktlint-gradle/releases/tag/v14.2.0)、[Pluginビルド設定](https://github.com/JLLeitschuh/ktlint-gradle/blob/v14.2.0/plugin/build.gradle.kts) |
| detekt | 1.23.6（候補1.23.7） | **1.23.8** | 安定1系の修正版へ更新候補。専用構成のCompilerを使わせる。アプリのKotlin 1.9.24への全構成一括強制はしない。型解決付きタスクも確認する。[detekt互換表](https://detekt.dev/docs/introduction/compatibility/) |
| Dokka Gradle Plugin | 未定義 | **2.2.0** | KDoc HTML生成用。KGP 1.9.0以上に対応するDGP v2を使い、アプリ側はKotlin 1.9.24を維持する。[リリース](https://github.com/Kotlin/dokka/releases/tag/v2.2.0)、[検証記録](KDOC.md) |

ここでいう「できるだけ新しい」は、上記の基盤・依存世代・安定版という条件で確認できた候補を指す。任意の依存強制や前方互換まで含めた、動作可能な絶対上限を証明したものではない。

## 組み合わせで確認する点

### Compose BOMは2025.07.00へ揃える

前回のBOM 2024.06.00ではRuntime 1.6.8とLifecycle Compose 2.8.7が要求するRuntime 1.7.1にずれがあった。今回はBOM側を更新し、主要Composeを1.8.3へ揃える。

公開Artifactを確認したところ、UI／Foundation 1.8.3のminCompileSdkは35、Kotlinメタデータは1.9.0。Runtime 1.8.3と推移依存のCollection 1.5.0もメタデータ1.9.0である。したがって、BOMを1.7系までに限定する必要はないと判断する。これはArtifactに基づく判断であり、検証用workspaceではCompiler 1.5.14によるコンパイルとエミュレーターでのCompose表示を確認済みだが、案件固有の画面・機能の試験は別途必要である。

通常の`platform(...)`でBOMを導入し、各Compose Artifactの個別バージョン指定を外す。`enforcedPlatform`で古い版へ戻したり、Kotlinメタデータ検査を無効化したりして通す方針は取らない。

根拠: [UI 1.8.3 AAR](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-android/1.8.3/ui-android-1.8.3.aar)、[Foundation 1.8.3 AAR](https://dl.google.com/dl/android/maven2/androidx/compose/foundation/foundation-android/1.8.3/foundation-android-1.8.3.aar)、[Runtime 1.8.3 POM](https://dl.google.com/dl/android/maven2/androidx/compose/runtime/runtime-android/1.8.3/runtime-android-1.8.3.pom)。

### Hiltは2.52へ更新し、Processor方式は既存構成を基準にする

既存がKAPTなら、まずHilt本体・Compiler・Pluginを2.52へ揃えて更新する。KSP利用中ならKSP 1.9.24-1.0.20を維持して検証する。

Hilt 2.52世代のDagger KSPは、現在の公式ドキュメントの安定化基準であるDagger 2.60+／KSP 2.3.9+には達していない。新規のKSP移行は版更新と分け、クリーンビルド、増分ビルド、テストを確認する。[Dagger KSP](https://dagger.dev/dev-guide/ksp.html)

残存ProcessorやData Bindingを確認する前に`kotlin-kapt`を削除しない。Compiler Artifactは既存宣言と解決結果を確認し、`com.google.dagger:hilt-compiler`を使う場合は本体と同じ2.52へ揃える。HiltのInstrumentation／Robolectric Testを使う場合も`hilt-android-testing:2.52`とする。

### kotlin-result 2.0.1にはソース移行が伴う

2.0では`Result`がinline value classになり、`Ok`／`Err`を型として扱うコードなどを変更する必要がある。最新版へ近づける主案は2.0.1とし、API移行ができない期間だけ1系を別途選び直す。バージョン番号だけを書き換えて完了とはしない。

### 解析ツールのCompilerとアプリのKotlinは分ける

detekt 1.23.8はKotlin 2.0.21でビルドされているが、専用の`detekt`構成内で実行するCompilerまでアプリの1.9.24へ揃える必要はない。`configurations.all`等でそのCompiler依存を書き換えない。アプリ／テストのclasspathと、解析ツールのclasspathをそれぞれ確認する。

ktlint Gradle Pluginとktlintエンジンも別物である。14.2.0の公開ソースでのエンジン既定値は1.5.0。既存のエンジン指定・カスタムルール・`.editorconfig`を確認し、実際の使用版を明示する。Plugin更新に伴うルール変更や警告は、`ktlintCheck`で確認する。

## Version Catalogへ反映する版

以下は**選定時点の候補を含む転記用の例**であり、現在のCatalog全体を表すものではない。未採用のMockK・Truth・Turbine、削除済みの独立したktlint Pluginなどを含むため、利用する項目だけを選ぶ。現在の設定は[Version Catalog](../native/gradle/libs.versions.toml)を参照し、Gradle Wrapper、SDK、Cordova自身の版は対応する設定ファイルで別途管理する。

```toml
[versions]
agp = "8.7.3"
kotlin = "1.9.24"
compose-compiler = "1.5.14"
compose-bom = "2025.07.00"
core-ktx = "1.16.0"
activity-compose = "1.10.1"
lifecycle = "2.8.7"
appcompat = "1.7.0"
webkit = "1.12.1"
datastore = "1.1.7"
work = "2.10.5"
coroutines = "1.8.1"
collections-immutable = "0.3.8"
datetime = "0.6.2"
serialization = "1.6.3"
retrofit = "2.11.0"
okhttp = "4.12.0"
hilt = "2.52"
ksp = "1.9.24-1.0.20" # KSPを使うモジュールがある場合のみ
timber = "5.0.1"
kotlin-result = "2.0.1"
junit = "4.13.2"
jacoco = "0.8.14"
androidx-test-core = "1.7.0"
androidx-test-junit = "1.3.0"
mockk = "1.13.11"
truth = "1.4.5" # 利用中の場合のみ
turbine = "1.1.0"
ktlint-plugin = "14.2.0"
detekt = "1.23.8"
dokka = "2.2.0"

[plugins]
# Serialization Pluginの版はruntimeではなくKotlinに合わせる
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
# KAPT利用中なら、残存Processorを確認するまで維持する
kotlin-kapt = { id = "org.jetbrains.kotlin.kapt", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
ktlint = { id = "org.jlleitschuh.gradle.ktlint", version.ref = "ktlint-plugin" }
detekt = { id = "io.gitlab.arturbosch.detekt", version.ref = "detekt" }
dokka = { id = "org.jetbrains.dokka", version.ref = "dokka" }

[libraries]
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
androidx-compose-foundation-layout = { module = "androidx.compose.foundation:foundation-layout" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
hilt-android-testing = { module = "com.google.dagger:hilt-android-testing", version.ref = "hilt" }
androidx-test-junit = { module = "androidx.test.ext:junit", version.ref = "androidx-test-junit" }
```

ライブラリのaliasは既存Catalogへ合わせる。`foundation-layout`はコードから直接使う場合に追加し、個別版は付けない。KAPTとKSPは実際にProcessorを割り当てる方をモジュールで適用する。

## Cordova本体も更新する場合

**14系維持は本メモの作業前提であり、Cordova全体の最新を意味しない。** 本体更新も含めて最新へ近づける場合は、確認時点のnpm安定版**cordova-android 15.1.0**を別の基準にする。[npm公開版情報](https://registry.npmjs.org/cordova-android/15.1.0)

公開npmパッケージ内の`framework/cdv-gradle-config-defaults.json`では、15.1.0はKotlin 2.1.21、AGP 8.10.1、Gradle 8.14.2、SDK 36、AppCompat 1.7.1、WebKit 1.14.0を採用している。minSdkは24、JDKは17のまま。公式ガイドの15.0.x欄の構成とも一致する。

この場合はKotlin 1.9系による制限が変わるため、**14用の上表をそのまま最新版の選定表として流用しない。** Compose Compilerも`org.jetbrains.kotlin.plugin.compose:2.1.21`へ切り替え、利用中のCordovaプラグインと追加ライブラリをKotlin 2.1.21／SDK 36の条件で再選定する。KotlinやAGPだけを14へ先行適用せず、Cordova本体・ビルド基盤・プラグインを一組で更新する。

## 採用確定に必要な検証

対象Androidプロジェクトで、モジュール名を実構成へ合わせて実行する。

```bash
cordova platform ls
./gradlew --version
./gradlew :app:dependencies --configuration debugRuntimeClasspath
./gradlew :app:dependencies --configuration debugUnitTestRuntimeClasspath
./gradlew :app:dependencies --configuration debugAndroidTestRuntimeClasspath
./gradlew :app:dependencyInsight \
  --configuration debugRuntimeClasspath \
  --dependency androidx.compose.runtime
./gradlew :app:dependencyInsight \
  --configuration debugRuntimeClasspath \
  --dependency kotlin-stdlib
./gradlew :app:dependencyInsight \
  --configuration debugUnitTestRuntimeClasspath \
  --dependency kotlin-reflect
./gradlew :app:dependencyInsight \
  --configuration debugRuntimeClasspath \
  --dependency okhttp
./gradlew :app:assembleDebug :app:assembleRelease
./gradlew :app:testDebugUnitTest :app:lintDebug
./gradlew detekt ktlintCheck
```

確認条件は次のとおり。

- 直接依存と推移依存を含め、Cordovaが使うAppCompat／WebKit等の実解決版が公式採用版から変わっていない。
- Composeの主要ArtifactがBOMの1.8.3へ揃い、アプリ／テストのKotlin依存が意図せず2.xへ上がっていない。
- ReleaseビルドでR8、DataStore、Hiltの生成コードが通り、kotlin-resultのAPI移行後のテストが成功する。
- Instrumentation Testがある場合は`./gradlew :app:connectedDebugAndroidTest`を実行する。Runnerを明示する場合は1.7.0、Espressoを使う場合は3.7.0を同世代の候補とする。
- KSPを使う場合はHilt Module／`@Inject`対象を変更した後の増分ビルドも成功する。
- minSdk側とAPI 35側の対象端末で、WebViewとネイティブ画面の遷移、カメラ、権限、バックグラウンド処理を確認する。

以上を満たした構成を実プロジェクトの採用版としてロックする。基盤を超える最新版が必要になった場合は、追加ライブラリの要求を無理に下げず、Cordova本体更新の構成へ進む。
