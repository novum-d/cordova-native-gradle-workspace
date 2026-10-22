# Cordova Android 14 / Kotlin Library Gradle統合検証

`CordovaComposeApp`を唯一のAPKホストとして、同じ`native/feature`・`native/data`をCordovaからもnative単独からもビルドするworkspaceです。2026-10-17に新規作成しました。既存の業務プロジェクトは提供されていないため、Cordova標準テンプレートを検証ホストにしています。

画面、Activity、CordovaPlugin、Application、DI初期化、WebView通信、カメラ・NFC機能は新規実装していません。`cordova-plugin-compose`やnativeの`MainActivity`も、既存ファイルがないため作成していません。Cordova標準のJava版`MainActivity`は維持しています。

## 開発ルール

コメント・KDocの記述方針は[プロジェクトルール](AGENTS.md)を参照してください。手書きのコード・設定には処理のまとまりごとの概要説明を必須とし、KDocで概要が分かる場合は通常コメントの重複を求めません。JSONの説明方法と生成ファイルの扱いも同文書に定めています。

## 構成と依存方向

```text
cordova-native-gradle-workspace/
├── package.json / package-lock.json        Cordova CLI 13.0.0を固定
├── CordovaComposeApp/
│   ├── config.xml                         Kotlin有効化、SDK設定、hook登録
│   ├── package.json / package-lock.json    cordova-android 14.0.1を固定
│   ├── hooks/copy-gradle-extras.js          生成物に管理ブロックを復元
│   ├── www/                               Cordova標準テンプレート
│   └── platforms/android/                 再生成可能、Git管理外
│       ├── settings.gradle                Cordovaがビルド時に再生成
│       ├── cdv-gradle-name.gradle          共有settingsスクリプトへの参照
│       ├── app/build-extras.gradle         共有appスクリプトへの参照
│       ├── app/                           唯一のcom.android.application
│       └── CordovaLib/                     Cordova既存Library
├── native/
│   ├── settings.gradle.kts / build.gradle.kts / gradle.properties
│   ├── gradlew / gradlew.bat / gradle/wrapper/
│   ├── gradle/libs.versions.toml           共有Catalogの管理元
│   ├── gradle/settings-extras.gradle       CordovaへCatalog・モジュールを登録
│   ├── gradle/build-extras.gradle          app依存と基盤版の照合
│   ├── feature/build.gradle.kts / src/main/AndroidManifest.xml
│   └── data/build.gradle.kts / src/main/AndroidManifest.xml
├── scripts/                               環境設定・再現用コマンド
├── verification/                          検証専用コード・hookテスト
└── docs/                                  実行ログ・検証レポート
```

```text
Cordova :app ──implementation──> :CordovaLib
             ├implementation──> :feature ──implementation──> :data
             └implementation──> :data
```

`feature`・`data`はどちらも`com.android.library`です。`native/app`、LibraryのtargetSdk、追加Launcherはありません。ソースはコピーしません。通常の`feature`には確認用の`MainActivity.kt`があります。`data`には通常のKotlinソースがないため、コンパイルタスクは`NO-SOURCE`となります。

## 固定する基盤

| 項目 | 版・設定 |
| --- | --- |
| cordova-android | 14.0.1 |
| AGP / Gradle | 8.7.3 / 8.13 |
| Kotlin / Compose Compiler | 1.9.24 / 1.5.14 |
| JDK | 17（実行環境17.0.2） |
| Java source・target / Kotlin JVM target | 11 / 11 |
| compileSdk / minSdk | 35 / 24 |
| targetSdk | Cordova appのみ35 |
| Build Tools | 35.0.0 |
| WebKit | 1.12.1 |
| Cordova内部AppCompat | 1.7.0を維持。Catalogに新設しない |

GradleはCatalogの`gradle`をWrapper生成・実行環境の選択・実行版照合で参照します。標準のWrapper propertiesにも起動に必要な版が記録されます。Cordovaの生成処理が使う既定値は維持し、共有Catalogと異なればビルド時にエラーにします。

## 採用バージョンと変更理由

原文・公開メタデータは2026-10-19に確認しました。原文欄は関連箇所の短い抜粋で、省略箇所は`…`としています。POMは公開された依存定義、AARは`META-INF/com/android/build/gradle/aar-metadata.properties`、クラスの`mv`は`javap -v`で読み取った`kotlin.Metadata`の値です。依存バージョンだけで互換性が保証されるものではなく、採用・維持はこのworkspaceの基盤と実測結果に基づく判断です。

| 項目 | 今回の版 | 提示候補からの判断 | 判断理由となった原文・メタデータ（抜粋） | 原文のリンク |
| --- | --- | --- | --- | --- |
| Compose BOM | 2025.07.00 | 2024.06.00から更新。最新版2026.09.00はKotlin 2.1メタデータでコンパイルに失敗するため採用しない。2025.07.00はKotlin 1.9.24／Compiler 1.5.14でビルド成功を再確認 | 最新版での実測: `The binary version of its metadata is 2.1.0, expected version is 1.9.0.`<br>採用版での実測: `:feature:compileDebugKotlin`、`BUILD SUCCESSFUL` | [最新版の失敗ログ](docs/compose-bom-latest-probe.log)、[採用版の成功ログ](docs/compose-bom-selected-probe.log)、[2025.07.00 POM](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2025.07.00/compose-bom-2025.07.00.pom) |
| Core KTX | 1.16.0 | compileSdk 35／AGP 8.7.3内で更新。1.17以降のKGP 2.0要件を避ける | 1.17.0: “requires use of Kotlin Gradle Plugin 2.0.0 or newer.” | [Core 1.17.0の変更点](https://developer.android.com/jetpack/androidx/releases/core?hl=en#1.17.0) |
| Activity Compose | 1.10.1 | compileSdk 35内で更新。1.11のSDK 36／AGP 8.9.1要件を避ける | 1.11.0: “Activity is now compiled with API 36.”<br>同版のAAR: `minCompileSdk=36`、`minAndroidGradlePluginVersion=8.9.1` | [Activity 1.11.0の変更点](https://developer.android.com/jetpack/androidx/releases/activity?hl=en#1.11.0)、[1.11.0 AAR](https://dl.google.com/dl/android/maven2/androidx/activity/activity/1.11.0/activity-1.11.0.aar) |
| Lifecycle Runtime KTX・Runtime Compose・ViewModel Compose | 2.8.7 | Kotlin 1.9系を維持。BOM側を更新してCompose Runtimeとの世代差を解消 | 2.8.6: “Lifecycle Runtime Compose now depends on Compose Runtime `1.7.1`”<br>2.8.7 Runtime KTXのPOM: `<artifactId>kotlin-stdlib</artifactId>` … `<version>1.8.22</version>` | [Lifecycle 2.8.6の依存更新](https://developer.android.com/jetpack/androidx/releases/lifecycle?hl=en#2.8.6)、[Runtime KTX 2.8.7 POM](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime-ktx-android/2.8.7/lifecycle-runtime-ktx-android-2.8.7.pom) |
| WebKit | 1.12.1 | Cordova指定版を維持 | Cordova 14.0.1の既定値: `"ANDROIDX_WEBKIT_VERSION": "1.12.1"` | [Cordova 14.0.1の既定値](https://github.com/apache/cordova-android/blob/14.0.1/framework/cdv-gradle-config-defaults.json) |
| DataStore Preferences・Core | 1.1.7 | 1.1系の修正を反映。1.2系のstdlib 2.0への移行を避ける | 1.1.7: “Fixed missing Proguard rules issue”<br>1.2.0のPOM: `<artifactId>kotlin-stdlib</artifactId>` … `<version>2.0.21</version>` | [DataStore 1.1.7の修正](https://developer.android.com/jetpack/androidx/releases/datastore?hl=en#1.1.7)、[Core JVM 1.2.0 POM](https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-core-jvm/1.2.0/datastore-core-jvm-1.2.0.pom) |
| WorkManager Runtime KTX | 2.10.5 | SDK 35内の修正版。2.11系のKotlin 2.1メタデータを避ける | `work-runtime:2.10.5`のAAR: `minCompileSdk=35`<br>`work-runtime:2.11.0`の`androidx.work.CoroutineWorker.class`: `mv=[2,1,0]` | [Runtime 2.10.5 AAR](https://dl.google.com/dl/android/maven2/androidx/work/work-runtime/2.10.5/work-runtime-2.10.5.aar)、[Runtime 2.11.0 AAR](https://dl.google.com/dl/android/maven2/androidx/work/work-runtime/2.11.0/work-runtime-2.11.0.aar) |
| Coroutines Core・Android | 1.8.1 | 1.9でのKotlin 2.0への更新を避ける | 1.9.0: “Kotlin was updated to 2.0” | [Coroutines 1.9.0の変更点](https://github.com/Kotlin/kotlinx.coroutines/releases/tag/1.9.0) |
| Kotlinx Datetime | 0.6.2 | Kotlin 1.9.24で実コンパイルを確認。0.7系の`kotlin.time.Instant`への移行を避ける | JVM POM: `<artifactId>kotlin-stdlib</artifactId>` … `<version>1.9.21</version>`<br>Instantの`mv=[1,9,0]`<br>0.7.0: “Remove `kotlinx.datetime.Instant` and `kotlinx.datetime.Clock` in favor of `kotlin.time.Instant`” | [0.6.2 JVM POM](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-datetime-jvm/0.6.2/kotlinx-datetime-jvm-0.6.2.pom)、[0.7.0のAPI変更](https://github.com/Kotlin/kotlinx-datetime/releases/tag/v0.7.0)、[追加内容](docs/DEPENDENCIES.md) |
| Immutable Collections | 0.3.8 | Kotlin 1.9.24で使える0.3系を採用。0.4.0以降へのKotlin 2系移行を避ける。実コンパイル確認済み | 0.4.0: “Updated Kotlin to version 2.1.20 and core dependencies”<br>0.3.8のJVM POM: `<version>1.9.21</version>`（kotlin-stdlib）、PersistentListの`mv=[1,9,0]` | [0.4.0の変更点](https://github.com/Kotlin/kotlinx.collections.immutable/releases/tag/v0.4.0)、[0.3.8 JVM POM](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-collections-immutable-jvm/0.3.8/kotlinx-collections-immutable-jvm-0.3.8.pom)、[追加内容と検証](docs/IMMUTABLE_COLLECTIONS.md) |
| Serialization JSON | 1.6.3 | 1.7系が求めるSerialization Compiler Plugin 2.0.0-RC1以降と、現在のPlugin 1.9.24との不整合を避けるため維持 | 1.7.0: “it may not be possible to use it with the Kotlin 1.9.x compiler plugin.”<br>同版が参照する1.7.0-RC: “it requires a compiler plugin with a version at least of 2.0.0-RC1.” | [Serialization 1.7.0の注意事項](https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.7.0)、[1.7.0-RCのCompiler Plugin要件](https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.7.0-RC) |
| Retrofit | 2.11.0 | 2.12のKotlin 2.1メタデータを避ける | 2.12.0 JAR内の`retrofit2.KotlinExtensions.class`: `kotlin.Metadata(` … `mv=[2,1,0]` | [Retrofit 2.12.0 JAR](https://repo.maven.apache.org/maven2/com/squareup/retrofit2/retrofit/2.12.0/retrofit-2.12.0.jar) |
| OkHttp・Logging Interceptor | 4.12.0 | Kotlin 1.9基盤で同版に統一 | Logging InterceptorのPOM: `<artifactId>okhttp</artifactId>` … `<version>4.12.0</version>`、`<artifactId>kotlin-stdlib-jdk8</artifactId>` … `<version>1.8.21</version>` | [Logging Interceptor 4.12.0 POM](https://repo.maven.apache.org/maven2/com/squareup/okhttp3/logging-interceptor/4.12.0/logging-interceptor-4.12.0.pom) |
| kotlin-result | 2.0.1 | Kotlin 1.9対応の2系へ更新。既存1系から移す場合はinline value class化に伴うAPI修正が必要 | 2.0.0: “The base `Result` class is now modelled as an inline value class.”<br>2.0.1 JVMのPOM: `<artifactId>kotlin-stdlib</artifactId>` … `<version>1.9.22</version>` | [2.0.0の移行説明](https://github.com/michaelbull/kotlin-result/releases/tag/2.0.0)、[2.0.1 JVM POM](https://repo.maven.apache.org/maven2/com/michael-bull/kotlin-result/kotlin-result-jvm/2.0.1/kotlin-result-jvm-2.0.1.pom) |
| Hilt Android・Compiler・Plugin | 2.52 | stdlib 1.9.24世代へ更新。2.53の本体は2.0 Beta系へ進む。両モジュールに適用し、KSPコード生成も確認 | `hilt-android:2.52`のPOM: `<artifactId>kotlin-stdlib</artifactId>` … `<version>1.9.24</version>`<br>`hilt-android:2.53`のPOM: `<artifactId>kotlin-stdlib</artifactId>` … `<version>2.0.0-Beta5</version>` | [Hilt Android 2.52 POM](https://repo.maven.apache.org/maven2/com/google/dagger/hilt-android/2.52/hilt-android-2.52.pom)、[2.53 POM](https://repo.maven.apache.org/maven2/com/google/dagger/hilt-android/2.53/hilt-android-2.53.pom) |
| KSP Plugin | 1.9.24-1.0.20 | Kotlin Compiler 1.9.24用。両モジュールに適用し、KSPコード生成も確認 | “KSP 1.0.20 for kotlin compiler version 1.9.24” | [KSP 1.9.24-1.0.20の公開情報](https://github.com/google/ksp/releases/tag/1.9.24-1.0.20) |
| Timber | 5.0.1 | 公開安定版を維持 | 公開版: “5.0.1”<br>修正内容: “Fix TimberArgCount lint check false positive on some calls to `String.format`.” | [Timber 5.0.1の変更点](https://github.com/JakeWharton/timber/releases/tag/5.0.1) |
| JUnit 4 | 4.13.2 | 維持 | 公式リリース名: “JUnit 4.13.2” | [JUnit 4.13.2の公開情報](https://github.com/junit-team/junit4/releases/tag/r4.13.2) |
| AndroidX Test Core KTX / Ext JUnit KTX | 1.7.0 / 1.3.0 | Kotlinメタデータ1.9世代へ更新。Artifact解決確認済み | Core KTXの`androidx.test.core.app.ActivityScenarioKt.class`: `mv=[1,9,0]`<br>Ext JUnit KTXの`androidx.test.ext.junit.rules.ActivityScenarioRuleKt.class`: `mv=[1,9,0]` | [Core KTX 1.7.0 AAR](https://dl.google.com/dl/android/maven2/androidx/test/core-ktx/1.7.0/core-ktx-1.7.0.aar)、[Ext JUnit KTX 1.3.0 AAR](https://dl.google.com/dl/android/maven2/androidx/test/ext/junit-ktx/1.3.0/junit-ktx-1.3.0.aar) |
| ktlint Gradle Plugin／ktlint本体 | 14.2.0／1.8.0 | どちらも確認時点の最新安定版。独立した検証用Gradleプロジェクトへ適用し、実ソースのコピーのチェック・故意の整形違反の検出・整形後の成功を確認 | Plugin Portal: “Version 14.2.0 (latest)”<br>ktlint本体の公開版: “1.8.0”<br>実測: `PASS: clean Kotlin accepted, malformed Kotlin rejected, formatted Kotlin accepted.` | [Plugin Portal](https://plugins.gradle.org/plugin/org.jlleitschuh.gradle.ktlint)、[ktlint 1.8.0](https://github.com/ktlint/ktlint/releases/tag/1.8.0)、[実行ログ](docs/quality-ktlint-latest.log) |
| detekt Gradle Plugin | 1.23.8 | 確認時点の最新安定版を適用済み。通常解析と整形チェックの実行成功を再確認 | 公式リリース: “v1.23.8”、`Latest`<br>実測: `> Task :detekt`、`> Task :detektFormatCheck`、`BUILD SUCCESSFUL` | [detekt 1.23.8](https://github.com/detekt/detekt/releases/tag/v1.23.8)、[実行ログ](docs/quality-detekt-latest.log)、[品質タスクの検証](docs/GIT_HOOKS.md) |

Composeライブラリには個別版を付けず、BOMを利用します。画面系は`feature`、通信・永続化系は`data`、日時・不変コレクション・Coroutines・Hilt・Result・Timberは両方へ通常依存として追加しています。単体テスト・Androidテスト依存と、Hilt/KSP・Serializationのコード生成も設定済みです。[配置一覧と解決結果](docs/DEPENDENCIES.md)を参照してください。

Immutable Collectionsに合わせて、detektの`DataClassShouldBeImmutable`と`TwitterCompose.UnstableCollections`を有効にし、違反1件で停止する設定にしました。使い方とルールの範囲は[追加内容](docs/IMMUTABLE_COLLECTIONS.md)を参照してください。

最新版との比較方法と結果は[バージョンの追加検証](docs/VERSION_CHECKS.md)を参照してください。Compose BOM 2025.07.00はビルド成功を確認した採用版であり、互換性のある最新版の上限を特定したものではありません。

単体テストにはJUnit 4と`kotlinx-coroutines-test:1.8.1`を使用します。Coroutines本体とテスト支援の版をそろえ、JaCoCo 0.8.14でカバレッジを収集します。AppCompat、Truth、MockK、Turbine、MockWebServer、KAPTのaliasや新規依存宣言はありません。Cordova由来の既存AppCompatは削除していません。

Hilt・KSPをfeature・dataへ適用し、`ksp(libs.hilt.compiler)`を設定しました。dataにはKotlin Serialization Pluginも適用し、検証コードでRepositoryのFactory、Hilt ViewModelのModule／Factory、日時を含むSerializerの生成とコンパイルを確認しています。CordovaのApplicationをHilt化した実機での注入は、この検証には含めていません。ktlint Pluginは検証用の一時プロジェクトだけに適用し、`sh verification/verify-ktlint.sh`で再実行できます。通常のnative・Cordovaプロジェクトではdetektを適用し、読み取り専用checkとpreCommitを実行できます。この2つのルートにあるktlintCheckはdetektFormatCheckへの互換名です。詳細は[Git hookのKotlin DSL実装](docs/GIT_HOOKS.md)を参照してください。両モジュールにJUnitの環境確認テストを追加しており、配置・実行方法は[単体テスト環境](docs/UNIT_TESTS.md)を参照してください。

選定根拠: [Cordova公式対応表](https://cordova.apache.org/docs/en/latest/guide/platforms/android/index.html)、[14.0.1既定値](https://github.com/apache/cordova-android/blob/14.0.1/framework/cdv-gradle-config-defaults.json)、[Compose BOM POM](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2025.07.00/compose-bom-2025.07.00.pom)、[Core](https://developer.android.com/jetpack/androidx/releases/core)、[Activity](https://developer.android.com/jetpack/androidx/releases/activity)、[DataStore](https://developer.android.com/jetpack/androidx/releases/datastore)、[WorkManager](https://developer.android.com/jetpack/androidx/releases/work)、[Hilt 2.52 POM](https://repo.maven.apache.org/maven2/com/google/dagger/hilt-android/2.52/hilt-android-2.52.pom)、[kotlin-result移行説明](https://github.com/michaelbull/kotlin-result/releases/tag/2.0.0)。最終的な解決座標は[ライブラリ](docs/resolved-catalog-libraries.txt)・[プラグイン](docs/resolved-catalog-plugins.txt)を参照してください。

## Catalog共有・プラグイン競合対策・hook

nativeはGradle標準の`gradle/libs.versions.toml`自動読込を使います。Cordovaは同じTOMLをSettingsスコープで`libs`として登録します。生成側の別Catalogや同名登録があれば上書きせず明示的に失敗します。

シェルからは`./scripts/catalog-version.sh gradle`などで同じCatalogの版を取得します。`[versions]`内の`name = "value"`形式をsh・awkで読み取ります。hookのテストもシェルで実行し、CordovaのJavaScript関数を呼ぶ部分だけNode.jsを使います。

nativeルートだけがAGP・KotlinをCatalogの版付きaliasで宣言します。共有LibraryのAGP・KotlinはversionなしのPlugin IDを使います。Cordova側では既存`buildscript`クラスパスをそのまま使うため、既知でない版のPluginが既にクラスパスにあるという競合を避けられます。Hilt・KSP・Serializationは利用するLibraryの`plugins`でCatalogのaliasから宣言します。

Cordova 14.0.1の`ProjectBuilder.prepBuildFiles()`は**build時にsettings.gradleを上書き**します。生成されるsettingsは`cdv-gradle-name.gradle`を読み込むため、このファイルに共有settingsへの相対参照をhookで登録します。Appの`build-extras.gradle`からも共有app設定へ相対参照を登録します。

`after_prepare`でprepareが再作成したファイルへ復元し、`before_compile`でも確認します。後続のビルド前settings再生成では参照が維持されます。`settings-extras.gradle`を置くだけの方式ではありません。既存のrootProject名・Plugin設定・app extrasは残し、専用マーカー内だけを更新します。必要ファイル不足や壊れたマーカーではエラーとし、ローカル絶対パスを生成設定へ埋め込みません。

根拠: [ProjectBuilder 14.0.1](https://github.com/apache/cordova-android/blob/14.0.1/lib/builders/ProjectBuilder.js)、[prepare 14.0.1](https://github.com/apache/cordova-android/blob/14.0.1/lib/prepare.js)、[Cordova hook実行順](https://cordova.apache.org/docs/en/latest/guide/appdev/hooks/)。この拡張点は14.0.1の生成形式に依存するため、Cordova更新時に再確認します。

## 再実行

Node.js、JDK 17、SDK 35とBuild Tools 35.0.0を用意します。このマシンではmiseの17.0.2を利用します。miseを使わない場合は`CORDOVA_JAVA_HOME`を指定してください。環境スクリプトは既存のグローバルGradleやJDK設定を書き換えません。

```sh
cd ~/repos/cordova-native-gradle-workspace
# 新しい環境での初回のみ。依存、Gradle 8.13、Wrapper、Cordova platformを復元する。
./scripts/bootstrap.sh

# 各cloneで初回のみ。git commit時にGradleの:preCommitを実行する。
./scripts/setup-git-hooks.sh

npm run cordova:prepare
npm run native:build
npm run verify:structure
npm run verify:sync
npm run cordova:build
npm run test:hooks
npm run test:bootstrap
npm run test:git-hooks
npm run test:unit
npm run test:unit:coverage
npm run check:android

# Catalog全Artifactの解決と、検証用Kotlinコードのコンパイル。
npm run verify:catalog
npm run verify:dependencies
npm run verify:compile
```

`bootstrap.sh`はルートのCordova CLIを開発用依存も含めてインストールします。`cordova.sh`は`.bin`リンクの有無に依存せず、インストールされたCLIをNode.jsで起動します。CLIだけを復元する場合は、リポジトリ直下で以下を実行してください。

```sh
npm ci --include=dev
./scripts/cordova.sh --version
```

`platforms/android/cordova/Api.js`は、Cordova CLIによるplatform作成時の生成物で、Gitには含めません。`npm ci`だけでは生成されません。ファイルが存在しない場合は`./scripts/bootstrap.sh`を再実行してください。platformが未作成なら新規作成し、フォルダだけが残って主要ファイルが欠けていれば、`.tools/cordova-platform-backups/`へ退避してから再生成します。正常なplatformは置き換えずprepareします。

`Api.js`があるのに`Could not load API`が出る場合は、そこから読み込む`cordova-android`や依存の不足も考えられます。リポジトリ直下で以下を実行してください。

```sh
npm --prefix CordovaComposeApp ci --include=dev
./scripts/cordova.sh prepare android --verbose
```

続く場合は`node -e 'require("./CordovaComposeApp/platforms/android/cordova/Api.js")'`で元の読み込みエラーを確認します。bootstrapも、ルートとアプリそれぞれの依存宣言とAndroid APIの読み込みを検査します。

`build-extras.gradle`で`Catalog named libs doesn't exist`が出る場合は、Settingsでの共有Catalog登録が実行されていません。リポジトリ直下で`./scripts/cordova.sh prepare android`を実行してから、ビルドまたはGradle Syncを再実行してください。`platforms/android/cdv-gradle-name.gradle`に以下の行が復元されます。

```groovy
apply from: new File(settingsDir, "../../../native/gradle/settings-extras.gradle")
```

Cordovaのprepareはこのファイルを再生成するため、手動追記だけでは次回に消えます。復元されない場合は、`CordovaComposeApp/config.xml`のAndroid設定に`<hook type="after_prepare" src="hooks/copy-gradle-extras.js" />`があるか確認してください。`--nohooks=after_prepare`などで復元hookを省略した場合も参照が消えるため、通常のprepareで復元してください。

Git hookでlint・フォーマットルール違反を検出した場合だけ、検査終了後に該当タスクのdetekt標準HTMLを開きます。独自の結果一覧は生成しません。nativeから実行した場合の出力先は`native/build/reports/detekt/<task>.html`です。成功時・ルール違反以外の実行エラー・CIでは自動表示せず、`-PopenPreCommitReport=false`でも無効化できます。[動作と検証結果](docs/GIT_HOOKS.md#最後にhtmlレポートを開く)

`npm run check:android`はCordova prepare後、nativeとCordovaのdetekt・整形チェック、Debug／ReleaseのAndroid Lint、基盤・依存方向の検証を順番に実行します。Lintエラーはビルドを失敗させ、ライブラリの指摘もアプリのレポートに含めます。修正した非推奨APIと残る警告は[Gradle・Android Lintの確認結果](docs/ANDROID_CHECKS.md)を参照してください。

`-PverifyCatalog=true`の検証ビルドだけが`verification/fixtures/*.kt`を`build/generated`へコピーし、検証用assetsを注入します。依存は通常のbuild.gradle.ktsで宣言しており、検証時に追加しません。Compose・日時・Serialization APIとHilt/KSPコード生成の実コンパイル、両Libraryのassetsマージを確認します。通常の配布APKには含めません。検証後に通常ビルドへ戻す場合は生成物をcleanしてください。

```sh
./scripts/with-env.sh ./native/gradlew -p native :feature:clean :data:clean
npm run native:build
npm run cordova:build
```

Android Studioでは`native/`を開き、GradleをWrapper、Gradle JDKを17に設定します。IDE画面上のSync操作は未実施ですが、Tooling APIによるAndroid V2モデル取得は実行しています。

## 変更ファイルの役割

| ファイル | 追加・変更の理由 |
| --- | --- |
| ルートpackage.json・lock | CLI版と検証コマンドを固定 |
| CordovaComposeAppのpackage.json・lock・config.xml | Android 14.0.1、SDK、既存Kotlin Pluginとhooksを管理 |
| CordovaComposeApp/www配下 | CLI生成の標準テンプレート。独自機能は追加しない |
| CordovaComposeApp/hooks/copy-gradle-extras.js | 冪等な参照復元、既存設定の保持、不足ファイル検出 |
| native/gradle/libs.versions.toml | 全ライブラリ・Plugin・基盤版の共有元 |
| native/settings.gradle.kts | RepositoryとLibraryモジュールを宣言、Catalog自動読込 |
| native/build.gradle.kts | native単独時のAGP/KGP、Wrapper版と検証タスク |
| native/gradle.properties | AndroidX、JVMメモリ、Kotlin設定 |
| native/gradlew・gradlew.bat・gradle/wrapper配下 | Gradle 8.13で単独ビルドする標準Wrapper |
| native/gradle/settings-extras.gradle | CordovaのSettingsへ共有Catalogと物理モジュールを登録 |
| native/gradle/build-extras.gradle | 既存appへLibrary依存を追加、Cordova基盤とCatalogを照合、共有Kotlin DSL品質設定を適用 |
| native/gradle/git-hooks.gradle.kts・native/gradle/config/detekt/配下 | TvAppを参照したdetekt・整形・preCommitの実装と設定 |
| verification/git-hooks-fixture.gradle.kts・verify-gradle-git-hooks.py | 一時Git repositoryでの品質ゲート・index保持の実行検証 |
| native/feature・dataのbuild.gradle.kts | Library化、Java/Kotlin 11、SDK、feature→dataの設定 |
| native/feature・dataのManifest | 通常のLibrary Manifestは空。featureのDebug ManifestだけにCompose確認用Activityを登録 |
| scripts/bootstrap.sh | 再現用依存・Gradle取得・platform復元 |
| scripts/setup-git-hooks.sh | Gitのpre-commitからGradleの:preCommitを呼ぶhookを初回登録。既存hookは上書きしない |
| scripts/with-env.sh・cordova.sh | JDK 17とローカルGradleを使う実行環境 |
| scripts/catalog-version.sh | sh・awkで共有Catalogの版を取得し、シェル補助処理へ渡す |
| scripts/tooling-sync.sh・verification/ToolingSync.java | Android V2モデルの取得検証 |
| verification/catalog-verification.gradle | 独立したArtifact解決、基盤・依存方向の検証、任意のコンパイルprobe |
| verification/fixtures/*.kt | Kotlin/Compose APIの検証専用コード。通常ビルドから除外 |
| verification/hook.test.sh | Cordova hookの冪等性、既存設定保持、不足・破損時の挙動をシェルで検証 |
| verification/git-hooks-setup.test.sh | Git hookの登録・既存設定保持・コミット時の呼び出しをシェルで検証 |
| .gitignore | 生成platform、依存、ビルド結果、ローカル設定をGit管理外にする |
| README.md・docs/ | 採用理由、実行方法、実測結果、ログを保存 |

`platforms/android`内の変更はすべて再生成またはhookによる復元です。恒久的な手動パッチやnode_modulesの書き換えはありません。

## 検証結果と未対応事項

Cordova CLIによるエミュレーターへのインストール・起動と、同じAPK内でのCompose表示・状態更新・Cordovaへの復帰も確認しました。確認後のエミュレーター終了を含め、[実行記録とスクリーンショット](docs/COMPOSE_RUNTIME.md)を参照してください。

実測結果、APK、コマンド、残存事項は[検証レポート](docs/VALIDATION.md)に記載しています。追加したdetekt・preCommit環境、TvAppとの差異、28ケースの実行検証は[Git hookのKotlin DSL実装](docs/GIT_HOOKS.md)を参照してください。
