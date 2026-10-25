# Cordova Android 14 依存関係バージョン妥当性検証

初回検証日: 2026-10-16<br>
再選定日: 2026-10-17<br>
実装・KDoc生成の確認日: 2026-10-22

## 結論と選定方針

**Cordovaの対応範囲を最優先し、その基盤で利用できる安定版をできるだけ新しくする。** 前回の「既存版を凍結して維持する」方針から変更する。

本プロジェクトでは、既存のCordova Android 14を維持する前提で、**cordova-android 14.0.1、AGP 8.7.3、Gradle 8.13、Kotlin 1.9.24、JDK 17、compileSdk／targetSdk 35、minSdk 24**を採用している。Compose BOMは**2025.07.00（主要Compose 1.8.3、Material 3 1.3.2）**を使う。

選定の優先順位は次のとおり。

1. 対象`cordova-android`の対応Android APIと公式採用のビルド基盤を守る。
2. Cordova自身が依存するAppCompat、WebKitなどは公式採用版に合わせる。
3. その他のライブラリは、上記のSDK・AGP・Kotlinを維持できる範囲で新しい安定版を選ぶ。直接依存だけでなく推移依存も確認する。
4. 型・APIの移行が必要な更新は、その変更点を明示する。alpha／beta／RC、互換性チェックの無効化、要求版を下げる強制解決は主案に含めない。

**Cordovaが公式に指定する版と、本プロジェクトで選定した追加ライブラリの版は区別する。** CordovaはCompose、Hilt、Retrofit等の全版について公式な対応表を提供していない。以下の追加ライブラリの選定は、公式リリース情報、公開POM／Gradle Module Metadata、AAR要件、Kotlinメタデータと本プロジェクトの実測結果から選定したものであり、Cordova公式の動作保証を意味しない。

この文書は2026-10-23にObsidianのMemoから本プロジェクトの`docs/`へ移動した。採用した構成と、その選定理由・検証記録を記載する。現在の採用版は[Version Catalog](../native/gradle/libs.versions.toml)、実際の依存配置は[依存関係ドキュメント](DEPENDENCIES.md)を管理元とする。

## 検証用workspaceへの反映状況（2026-10-22）

- `feature`・`data`へ依存を配置し、native単独とCordovaホストで共有している。Immutable Collectionsは0.3.8、Datetimeは0.6.2を追加済み。実際の採用版と依存方向は[Version Catalog](../native/gradle/libs.versions.toml)と[配置・解決結果](DEPENDENCIES.md)を管理元とする。
- Compose BOM 2025.07.00でビルドし、Cordova CLIからエミュレーターへインストール・起動、Compose表示・状態更新・Cordovaへの復帰を確認済み。[実行記録](COMPOSE_RUNTIME.md)。案件の既存アプリ環境でもビルド・起動できたとの報告を受けているが、このworkspaceの検証記録とは別の確認である。
- JVM単体テストはJUnit 4.13.2とCoroutines Test 1.8.1、カバレッジはJaCoCo 0.8.14を採用済み。[環境と対象範囲](UNIT_TESTS.md)。
- 通常の品質チェックとGit hookはdetekt 1.23.8とdetekt-formattingを使い、ktlint 0.50.0のルール・整形エンジンを推移依存として利用する。Git hookのHTML表示は、ルール違反時にdetekt標準レポートを開く。[品質チェックの構成](GIT_HOOKS.md)。
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

原文欄はREADMEに記録済みの短い抜粋・公開メタデータと、選定時の確認記録を示す。採用しなかった新しい版への言及は、現在の版を選んだ理由として残す。

| 項目 | 今回の版 | 提示候補からの判断 | 判断理由となった原文・メタデータ（抜粋） | 原文のリンク |
| --- | --- | --- | --- | --- |
| Compose BOM | **2025.07.00** | 更新。UI／Foundation／Runtime 1.8.3、Material 3 1.3.2へ揃える。1.9系へ進むBOMはKotlin 2.0等の要件が上がるため主案から外す | 最新版での実測: `The binary version of its metadata is 2.1.0, expected version is 1.9.0.`<br>採用版での実測: `:feature:compileDebugKotlin`、`BUILD SUCCESSFUL` | [BOM POM](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2025.07.00/compose-bom-2025.07.00.pom)、[Compose UI](https://developer.android.com/jetpack/androidx/releases/compose-ui)、[最新版の失敗ログ](logs/versions/compose-bom-latest-probe.log)、[採用版の成功ログ](logs/versions/compose-bom-selected-probe.log)、[2025.07.00 POM](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2025.07.00/compose-bom-2025.07.00.pom) |
| Core KTX | **1.16.0** | 更新。公開AARはcompileSdk 35、AGP 8.6.0以上、Kotlinメタデータ1.8.0。1.17.0でKGP 2.0以上が必須になる | 1.17.0: “requires use of Kotlin Gradle Plugin 2.0.0 or newer.” | [Core](https://developer.android.com/jetpack/androidx/releases/core#1.17.0)、[Core 1.17.0の変更点](https://developer.android.com/jetpack/androidx/releases/core?hl=en#1.17.0) |
| Activity Compose | **1.10.1** | 更新。公開AARのminCompileSdk 35。1.11.0は36／AGP 8.9.1以上となり14の基盤を超える | 1.11.0: “Activity is now compiled with API 36.”<br>同版のAAR: `minCompileSdk=36`、`minAndroidGradlePluginVersion=8.9.1` | [Activity](https://developer.android.com/jetpack/androidx/releases/activity)、[Activity 1.11.0の変更点](https://developer.android.com/jetpack/androidx/releases/activity?hl=en#1.11.0)、[1.11.0 AAR](https://dl.google.com/dl/android/maven2/androidx/activity/activity/1.11.0/activity-1.11.0.aar) |
| Lifecycle | **2.8.7** | 維持。Compose 1.8.3側もこの世代を利用する。2.9系はKotlin 2.0世代となるため主案から外す | 2.8.6: “Lifecycle Runtime Compose now depends on Compose Runtime `1.7.1`”<br>2.8.7 Runtime KTXのPOM: `<artifactId>kotlin-stdlib</artifactId>` … `<version>1.8.22</version>` | [Lifecycle](https://developer.android.com/jetpack/androidx/releases/lifecycle)、[Lifecycle 2.8.6の依存更新](https://developer.android.com/jetpack/androidx/releases/lifecycle?hl=en#2.8.6)、[Runtime KTX 2.8.7 POM](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime-ktx-android/2.8.7/lifecycle-runtime-ktx-android-2.8.7.pom) |
| DataStore | **1.1.7** | 更新。1.1系の修正を取り込む。1.2.0はKotlin stdlib 2.0.21／Coroutines 1.9.0へ進むため主案から外す | 1.1.7: “Fixed missing Proguard rules issue”<br>1.2.0のPOM: `<artifactId>kotlin-stdlib</artifactId>` … `<version>2.0.21</version>` | [DataStore](https://developer.android.com/jetpack/androidx/releases/datastore)、[DataStore 1.1.7の修正](https://developer.android.com/jetpack/androidx/releases/datastore?hl=en#1.1.7)、[Core JVM 1.2.0 POM](https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-core-jvm/1.2.0/datastore-core-jvm-1.2.0.pom) |
| WorkManager | **2.10.5** | 更新。2.10系の安定修正版で、公開AARのminCompileSdk 35、minSdk 21。2.11.0はKotlinメタデータ2.1.0／Coroutines 1.9.0となるため主案から外す | `work-runtime:2.10.5`のAAR: `minCompileSdk=35`<br>`work-runtime:2.11.0`の`androidx.work.CoroutineWorker.class`: `mv=[2,1,0]` | [WorkManager](https://developer.android.com/jetpack/androidx/releases/work#2.10.5)、[Runtime 2.10.5 AAR](https://dl.google.com/dl/android/maven2/androidx/work/work-runtime/2.10.5/work-runtime-2.10.5.aar)、[Runtime 2.11.0 AAR](https://dl.google.com/dl/android/maven2/androidx/work/work-runtime/2.11.0/work-runtime-2.11.0.aar) |
| Coroutines | **1.8.1** | 維持。1.9.0はstdlib 2.0.0に依存する。`core`／`android`／`test`を同じ版へ揃える | 1.9.0: “Kotlin was updated to 2.0” | [1.9.0 POM](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core-jvm/1.9.0/kotlinx-coroutines-core-jvm-1.9.0.pom)、[Coroutines 1.9.0の変更点](https://github.com/Kotlin/kotlinx.coroutines/releases/tag/1.9.0) |
| Immutable Collections | **0.3.8** | Kotlin 1.9.24で利用・実コンパイルを確認。0.4.0のKotlin 2.1.20要件を避ける | 0.4.0: “Updated Kotlin to version 2.1.20 and core dependencies”<br>0.3.8のJVM POM: `<version>1.9.21</version>`（kotlin-stdlib）、PersistentListの`mv=[1,9,0]` | [0.4.0変更点](https://github.com/Kotlin/kotlinx.collections.immutable/releases/tag/v0.4.0)、[0.4.0の変更点](https://github.com/Kotlin/kotlinx.collections.immutable/releases/tag/v0.4.0)、[0.3.8 JVM POM](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-collections-immutable-jvm/0.3.8/kotlinx-collections-immutable-jvm-0.3.8.pom)、[追加内容と検証](IMMUTABLE_COLLECTIONS.md) |
| Datetime | **0.6.2** | Kotlin 1.9.24で実コンパイルを確認。0.7系の`kotlin.time.Instant`への移行を避ける | JVM POM: `<artifactId>kotlin-stdlib</artifactId>` … `<version>1.9.21</version>`<br>Instantの`mv=[1,9,0]`<br>0.7.0: “Remove `kotlinx.datetime.Instant` and `kotlinx.datetime.Clock` in favor of `kotlin.time.Instant`” | [0.7.0変更点](https://github.com/Kotlin/kotlinx-datetime/releases/tag/v0.7.0)、[0.6.2 JVM POM](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-datetime-jvm/0.6.2/kotlinx-datetime-jvm-0.6.2.pom)、[0.7.0のAPI変更](https://github.com/Kotlin/kotlinx-datetime/releases/tag/v0.7.0)、[追加内容](DEPENDENCIES.md) |
| Serialization runtime | **1.6.3** | 維持。1.7系が求めるSerialization Compiler Plugin 2.0.0-RC1以降と、現在のPlugin 1.9.24の不整合を避ける。Serialization Gradle PluginはKotlinと同じ1.9.24にする | 1.7.0: “it may not be possible to use it with the Kotlin 1.9.x compiler plugin.”<br>同版が参照する1.7.0-RC: “it requires a compiler plugin with a version at least of 2.0.0-RC1.” | [Serializationリリース](https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.7.0)、[Serialization 1.7.0の注意事項](https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.7.0)、[1.7.0-RCのCompiler Plugin要件](https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.7.0-RC) |
| Retrofit | **2.11.0** | 維持。2.12.0の公開JARはKotlinメタデータ2.1.0。Java中心のライブラリでもArtifact内のKotlin部分を確認する | 2.12.0 JAR内の`retrofit2.KotlinExtensions.class`: `kotlin.Metadata(` … `mv=[2,1,0]` | [2.12.0 JAR](https://repo.maven.apache.org/maven2/com/squareup/retrofit2/retrofit/2.12.0/retrofit-2.12.0.jar)、[Retrofit 2.12.0 JAR](https://repo.maven.apache.org/maven2/com/squareup/retrofit2/retrofit/2.12.0/retrofit-2.12.0.jar) |
| OkHttp | **4.12.0** | 維持。4系の安定版を採用し、Kotlin 2.xへ移行する5系は外す。Logging Interceptorも同版へ揃える | Logging InterceptorのPOM: `<artifactId>okhttp</artifactId>` … `<version>4.12.0</version>`、`<artifactId>kotlin-stdlib-jdk8</artifactId>` … `<version>1.8.21</version>` | [OkHttp変更履歴](https://square.github.io/okhttp/changelogs/changelog/)、[Logging Interceptor 4.12.0 POM](https://repo.maven.apache.org/maven2/com/squareup/okhttp3/logging-interceptor/4.12.0/logging-interceptor-4.12.0.pom) |
| Hilt / Dagger | **2.52** | 更新。本体・Compiler・Gradle Pluginを統一する。2.53の本体POMは既にstdlib 2.0.0-Beta5、2.53.1は2.0.21へ進むため、Pluginの版だけで判断しない | `hilt-android:2.52`のPOM: `<artifactId>kotlin-stdlib</artifactId>` … `<version>1.9.24</version>`<br>`hilt-android:2.53`のPOM: `<artifactId>kotlin-stdlib</artifactId>` … `<version>2.0.0-Beta5</version>` | [2.52 POM](https://repo.maven.apache.org/maven2/com/google/dagger/hilt-android/2.52/hilt-android-2.52.pom)、[2.53 POM](https://repo.maven.apache.org/maven2/com/google/dagger/hilt-android/2.53/hilt-android-2.53.pom)、[Hilt Android 2.52 POM](https://repo.maven.apache.org/maven2/com/google/dagger/hilt-android/2.52/hilt-android-2.52.pom) |
| KSP Plugin | **1.9.24-1.0.20** | Kotlin Compiler 1.9.24用を採用。feature・dataに適用し、Hilt CompilerをKSPで実行する | “KSP 1.0.20 for kotlin compiler version 1.9.24” | [KSPリリース](https://github.com/google/ksp/releases/tag/1.9.24-1.0.20)、[KSP 1.9.24-1.0.20の公開情報](https://github.com/google/ksp/releases/tag/1.9.24-1.0.20) |
| Timber | **5.0.1** | 維持。公開済み安定版 | 公開版: “5.0.1”<br>修正内容: “Fix TimberArgCount lint check false positive on some calls to `String.format`.” | [Timberリリース](https://github.com/JakeWharton/timber/releases/tag/5.0.1)、[Timber 5.0.1の変更点](https://github.com/JakeWharton/timber/releases/tag/5.0.1) |
| kotlin-result | **2.0.1** | 更新。ただし2.0でinline value class化されたためAPI移行が必要。公開JARはメタデータ1.9.0／stdlib 1.9.22。2.0.2はstdlib・メタデータとも2.2系 | 2.0.0: “The base `Result` class is now modelled as an inline value class.”<br>2.0.1 JVMのPOM: `<artifactId>kotlin-stdlib</artifactId>` … `<version>1.9.22</version>` | [移行説明](https://github.com/michaelbull/kotlin-result/releases/tag/2.0.0)、[2.0.1 POM](https://repo.maven.apache.org/maven2/com/michael-bull/kotlin-result/kotlin-result-jvm/2.0.1/kotlin-result-jvm-2.0.1.pom)、[2.0.0の移行説明](https://github.com/michaelbull/kotlin-result/releases/tag/2.0.0)、[2.0.1 JVM POM](https://repo.maven.apache.org/maven2/com/michael-bull/kotlin-result/kotlin-result-jvm/2.0.1/kotlin-result-jvm-2.0.1.pom) |
| JUnit 4 | **4.13.2** | 維持。JUnit 4系の安定版 | 公式リリース名: “JUnit 4.13.2” | [JUnit 4リリース](https://github.com/junit-team/junit4/releases/tag/r4.13.2)、[JUnit 4.13.2の公開情報](https://github.com/junit-team/junit4/releases/tag/r4.13.2) |
| AndroidX Test Core | **1.7.0** | 更新。公開AARはKotlinメタデータ1.9.0、stdlib 1.9.21／Coroutines 1.8.1。minSdk 21で対象端末の範囲内 | Core KTXの`androidx.test.core.app.ActivityScenarioKt.class`: `mv=[1,9,0]`<br>Ext JUnit KTXの`androidx.test.ext.junit.rules.ActivityScenarioRuleKt.class`: `mv=[1,9,0]` | [Testリリース](https://developer.android.com/jetpack/androidx/releases/test)、[Core KTX 1.7.0 AAR](https://dl.google.com/dl/android/maven2/androidx/test/core-ktx/1.7.0/core-ktx-1.7.0.aar)、[Ext JUnit KTX 1.3.0 AAR](https://dl.google.com/dl/android/maven2/androidx/test/ext/junit-ktx/1.3.0/junit-ktx-1.3.0.aar) |
| AndroidX Test ext.junit | **1.3.0** | 更新。Core 1.7.0と同世代。`androidx.test.ext:junit`を指定する | Core KTXの`androidx.test.core.app.ActivityScenarioKt.class`: `mv=[1,9,0]`<br>Ext JUnit KTXの`androidx.test.ext.junit.rules.ActivityScenarioRuleKt.class`: `mv=[1,9,0]` | [Testリリース](https://developer.android.com/jetpack/androidx/releases/test)、[Core KTX 1.7.0 AAR](https://dl.google.com/dl/android/maven2/androidx/test/core-ktx/1.7.0/core-ktx-1.7.0.aar)、[Ext JUnit KTX 1.3.0 AAR](https://dl.google.com/dl/android/maven2/androidx/test/ext/junit-ktx/1.3.0/junit-ktx-1.3.0.aar) |
| detekt | **1.23.8** | 安定1系の修正版を採用済み。専用構成のCompilerを使う。アプリのKotlin 1.9.24への全構成一括強制はしない。通常解析・整形チェックの実行を確認済み | 公式リリース: “v1.23.8”、`Latest`<br>実測: `> Task :detekt`、`> Task :detektFormatCheck`、`BUILD SUCCESSFUL` | [detekt互換表](https://detekt.dev/docs/introduction/compatibility/)、[detekt 1.23.8](https://github.com/detekt/detekt/releases/tag/v1.23.8)、[実行ログ](logs/versions/quality-detekt-latest.log)、[品質タスクの検証](GIT_HOOKS.md) |
| Dokka Gradle Plugin | **2.2.0** | KDoc HTML生成用。KGP 1.9.0以上に対応するDGP v2を使い、アプリ側はKotlin 1.9.24を維持する | 公式の最小KGP要件: `KGP 1.9.0+`<br>実測結果はKDoc生成の記録を参照 | [リリース](https://github.com/Kotlin/dokka/releases/tag/v2.2.0)、[検証記録](KDOC.md)、[2.2.0リリース](https://github.com/Kotlin/dokka/releases/tag/v2.2.0)、[KDoc生成](KDOC.md) |

ここでいう「できるだけ新しい」は、上記の基盤・依存世代・安定版という条件で確認し、採用した版を指す。任意の依存強制や前方互換まで含めた、動作可能な絶対上限を証明したものではない。

## 組み合わせで確認する点

### Compose BOMは2025.07.00へ揃える

前回のBOM 2024.06.00ではRuntime 1.6.8とLifecycle Compose 2.8.7が要求するRuntime 1.7.1にずれがあった。今回はBOM側を更新し、主要Composeを1.8.3へ揃える。

公開Artifactを確認したところ、UI／Foundation 1.8.3のminCompileSdkは35、Kotlinメタデータは1.9.0。Runtime 1.8.3と推移依存のCollection 1.5.0もメタデータ1.9.0である。したがって、BOMを1.7系までに限定する必要はないと判断する。これはArtifactに基づく判断であり、検証用workspaceではCompiler 1.5.14によるコンパイルとエミュレーターでのCompose表示を確認済みだが、案件固有の画面・機能の試験は別途必要である。

通常の`platform(...)`でBOMを導入し、各Compose Artifactの個別バージョン指定を外す。`enforcedPlatform`で古い版へ戻したり、Kotlinメタデータ検査を無効化したりして通す方針は取らない。

根拠: [UI 1.8.3 AAR](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-android/1.8.3/ui-android-1.8.3.aar)、[Foundation 1.8.3 AAR](https://dl.google.com/dl/android/maven2/androidx/compose/foundation/foundation-android/1.8.3/foundation-android-1.8.3.aar)、[Runtime 1.8.3 POM](https://dl.google.com/dl/android/maven2/androidx/compose/runtime/runtime-android/1.8.3/runtime-android-1.8.3.pom)。

### Hilt 2.52のコード生成にはKSPを採用する

KSP 1.9.24-1.0.20を`feature`・`data`の両モジュールへ適用済み。Hiltの本体・Compiler・Gradle Pluginを2.52にそろえ、`ksp(libs.hilt.compiler)`でコード生成を行う。

検証コードでRepositoryのFactoryとHilt ViewModelのModule／Factory生成・コンパイルを確認している。[配置・検証記録](DEPENDENCIES.md)。

### kotlin-result 2.0.1にはソース移行が伴う

2.0では`Result`がinline value classになり、`Ok`／`Err`を型として扱うコードなどを変更する必要がある。本プロジェクトでは2.0.1を採用している。バージョン番号だけを書き換えて完了とはしない。

### 解析ツールのCompilerとアプリのKotlinは分ける

detekt 1.23.8はKotlin 2.0.21でビルドされているが、専用の`detekt`構成内で実行するCompilerまでアプリの1.9.24へ揃える必要はない。`configurations.all`等でそのCompiler依存を書き換えない。アプリ／テストのclasspathと、解析ツールのclasspathをそれぞれ確認する。

整形には`detekt-formatting:1.23.8`が推移依存として利用するktlint 0.50.0を使う。`ktlintCheck`は読み取り専用の`detektFormatCheck`への互換名である。[品質タスク](GIT_HOOKS.md)。

## 採用したVersion Catalogの設定

以下は[現在のVersion Catalog](../native/gradle/libs.versions.toml)からの抜粋。全ライブラリのaliasは管理元を参照する。Gradle Wrapper、SDK、Cordova自身の版は対応する設定ファイルでも管理する。

```toml
[versions]
# Cordova Android 14とそろえる基盤。SDK・JVM targetも両ホストから同じ値を参照する。
cordova-android = "14.0.1"
gradle = "8.13"
agp = "8.7.3"
kotlin = "1.9.24"
compose-compiler = "1.5.14"
build-tools = "35.0.0"
compile-sdk = "35"
target-sdk = "35"
min-sdk = "24"
jvm-target = "11"
# UI・通信・永続化などの採用版。互換性の選定理由はREADMEの表で管理する。
compose-bom = "2025.07.00"
core = "1.16.0"
activity = "1.10.1"
lifecycle = "2.8.7"
webkit = "1.12.1"
datastore = "1.1.7"
serialization = "1.6.3"
coroutines = "1.8.1"
collections-immutable = "0.3.8"
datetime = "0.6.2"
work = "2.10.5"
retrofit = "2.11.0"
okhttp = "4.12.0"
result = "2.0.1"
hilt = "2.52"
ksp = "1.9.24-1.0.20"
timber = "5.0.1"
# テスト・品質チェック・文書化・Git連携のツール。アプリの通常依存とは用途を分ける。
junit = "4.13.2"
jacoco = "0.8.14"
test-core = "1.7.0"
test-junit = "1.3.0"
detekt = "1.23.8"
dokka = "2.2.0"
twitter-compose-rules = "0.0.26"
jgit = "7.8.0.202609011348-r"

# コード生成・品質チェック・文書化に使うPlugin。
[plugins]
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
detekt = { id = "io.gitlab.arturbosch.detekt", version.ref = "detekt" }
dokka = { id = "org.jetbrains.dokka", version.ref = "dokka" }

# 採用した通常依存とテスト依存の一部。
[libraries]
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
compose-foundation-layout = { module = "androidx.compose.foundation:foundation-layout" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
androidx-test-ext-junit-ktx = { module = "androidx.test.ext:junit-ktx", version.ref = "test-junit" }
```

Composeの各Artifactには個別版を付けず、BOMでそろえる。HiltのProcessorは両モジュールでKSPへ登録する。

共通のDatetime・kotlin-result・Immutable Collections・Coroutines Core／Android・Hilt Android・Timberは、Catalogの`native-common` bundleにまとめ、feature・dataの`api`で公開する。利用側への公開範囲と設定は[依存の配置](DEPENDENCIES.md)を参照する。

## 採用構成の確認手順

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
- Hilt Module／`@Inject`対象を変更した後も、KSPの増分ビルドが成功する。
- minSdk側とAPI 35側の対象端末で、WebViewとネイティブ画面の遷移、カメラ、権限、バックグラウンド処理を確認する。

上記は採用構成を対象プロジェクトで確認する手順であり、すべての案件固有機能を本workspaceで検証済みという意味ではない。実行済みの範囲は各検証記録を参照する。
