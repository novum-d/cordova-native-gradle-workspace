# Cordova Android 14 / Kotlin Library Gradle統合検証

`CordovaComposeApp`を唯一のAPKホストとして、同じ`native/feature`・`native/data`をCordovaからもnative単独からもビルドするworkspaceです。2026-10-17に新規作成しました。既存の業務プロジェクトは提供されていないため、Cordova標準テンプレートを検証ホストにしています。

画面、Activity、CordovaPlugin、Application、DI初期化、WebView通信、カメラ・NFC機能は新規実装していません。`cordova-plugin-compose`やnativeの`MainActivity`も、既存ファイルがないため作成していません。Cordova標準のJava版`MainActivity`は維持しています。

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
│   ├── third-party-licenses/               ライセンス原文・出典・依存台帳
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

`feature`・`data`はどちらも`com.android.library`です。`native/app`、LibraryのtargetSdk、追加Launcherはありません。ソースはコピーしません。通常ビルドでは業務ソースがないため、LibraryのKotlinコンパイルタスクは`NO-SOURCE`となります。

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
| Compose BOM | 2025.07.00 | 2024.06.00から更新。主要UI・Runtime・Foundationは1.8.3、Material3は1.3.2。Compiler 1.5.14で実コンパイル確認 | BOMのPOM: `<artifactId>runtime</artifactId>` … `<version>1.8.3</version>`、`<artifactId>material3</artifactId>` … `<version>1.3.2</version>`<br>実測ログ: `> Task :feature:compileDebugKotlin`、`BUILD SUCCESSFUL` | [2025.07.00 POM](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2025.07.00/compose-bom-2025.07.00.pom)、[実コンパイルのログ](docs/native-verification.log) |
| Core KTX | 1.16.0 | compileSdk 35／AGP 8.7.3内で更新。1.17以降のKGP 2.0要件を避ける | 1.17.0: “requires use of Kotlin Gradle Plugin 2.0.0 or newer.” | [Core 1.17.0の変更点](https://developer.android.com/jetpack/androidx/releases/core?hl=en#1.17.0) |
| Activity Compose | 1.10.1 | compileSdk 35内で更新。1.11のSDK 36／AGP 8.9.1要件を避ける | 1.11.0: “Activity is now compiled with API 36.”<br>同版のAAR: `minCompileSdk=36`、`minAndroidGradlePluginVersion=8.9.1` | [Activity 1.11.0の変更点](https://developer.android.com/jetpack/androidx/releases/activity?hl=en#1.11.0)、[1.11.0 AAR](https://dl.google.com/dl/android/maven2/androidx/activity/activity/1.11.0/activity-1.11.0.aar) |
| Lifecycle Runtime KTX・Runtime Compose・ViewModel Compose | 2.8.7 | Kotlin 1.9系を維持。BOM側を更新してCompose Runtimeとの世代差を解消 | 2.8.6: “Lifecycle Runtime Compose now depends on Compose Runtime `1.7.1`”<br>2.8.7 Runtime KTXのPOM: `<artifactId>kotlin-stdlib</artifactId>` … `<version>1.8.22</version>` | [Lifecycle 2.8.6の依存更新](https://developer.android.com/jetpack/androidx/releases/lifecycle?hl=en#2.8.6)、[Runtime KTX 2.8.7 POM](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime-ktx-android/2.8.7/lifecycle-runtime-ktx-android-2.8.7.pom) |
| WebKit | 1.12.1 | Cordova指定版を維持 | Cordova 14.0.1の既定値: `"ANDROIDX_WEBKIT_VERSION": "1.12.1"` | [Cordova 14.0.1の既定値](https://github.com/apache/cordova-android/blob/14.0.1/framework/cdv-gradle-config-defaults.json) |
| DataStore Preferences・Core | 1.1.7 | 1.1系の修正を反映。1.2系のstdlib 2.0への移行を避ける | 1.1.7: “Fixed missing Proguard rules issue”<br>1.2.0のPOM: `<artifactId>kotlin-stdlib</artifactId>` … `<version>2.0.21</version>` | [DataStore 1.1.7の修正](https://developer.android.com/jetpack/androidx/releases/datastore?hl=en#1.1.7)、[Core JVM 1.2.0 POM](https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-core-jvm/1.2.0/datastore-core-jvm-1.2.0.pom) |
| WorkManager Runtime KTX | 2.10.5 | SDK 35内の修正版。2.11系のKotlin 2.1メタデータを避ける | `work-runtime:2.10.5`のAAR: `minCompileSdk=35`<br>`work-runtime:2.11.0`の`androidx.work.CoroutineWorker.class`: `mv=[2,1,0]` | [Runtime 2.10.5 AAR](https://dl.google.com/dl/android/maven2/androidx/work/work-runtime/2.10.5/work-runtime-2.10.5.aar)、[Runtime 2.11.0 AAR](https://dl.google.com/dl/android/maven2/androidx/work/work-runtime/2.11.0/work-runtime-2.11.0.aar) |
| Coroutines Core・Android | 1.8.1 | 1.9でのKotlin 2.0への更新を避ける | 1.9.0: “Kotlin was updated to 2.0” | [Coroutines 1.9.0の変更点](https://github.com/Kotlin/kotlinx.coroutines/releases/tag/1.9.0) |
| Serialization JSON | 1.6.3 | Kotlin 1.9系列を維持。Serialization Pluginは1.9.24で別定義 | 1.6.3: “uses Kotlin 1.9.22 as default.” | [Serialization 1.6.3の変更点](https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.6.3) |
| Retrofit | 2.11.0 | 2.12のKotlin 2.1メタデータを避ける | 2.12.0 JAR内の`retrofit2.KotlinExtensions.class`: `kotlin.Metadata(` … `mv=[2,1,0]` | [Retrofit 2.12.0 JAR](https://repo.maven.apache.org/maven2/com/squareup/retrofit2/retrofit/2.12.0/retrofit-2.12.0.jar) |
| OkHttp・Logging Interceptor | 4.12.0 | Kotlin 1.9基盤で同版に統一 | Logging InterceptorのPOM: `<artifactId>okhttp</artifactId>` … `<version>4.12.0</version>`、`<artifactId>kotlin-stdlib-jdk8</artifactId>` … `<version>1.8.21</version>` | [Logging Interceptor 4.12.0 POM](https://repo.maven.apache.org/maven2/com/squareup/okhttp3/logging-interceptor/4.12.0/logging-interceptor-4.12.0.pom) |
| kotlin-result | 2.0.1 | Kotlin 1.9対応の2系へ更新。既存1系から移す場合はinline value class化に伴うAPI修正が必要 | 2.0.0: “The base `Result` class is now modelled as an inline value class.”<br>2.0.1 JVMのPOM: `<artifactId>kotlin-stdlib</artifactId>` … `<version>1.9.22</version>` | [2.0.0の移行説明](https://github.com/michaelbull/kotlin-result/releases/tag/2.0.0)、[2.0.1 JVM POM](https://repo.maven.apache.org/maven2/com/michael-bull/kotlin-result/kotlin-result-jvm/2.0.1/kotlin-result-jvm-2.0.1.pom) |
| Hilt Android・Compiler・Plugin | 2.52 | stdlib 1.9.24世代へ更新。2.53の本体は2.0 Beta系へ進む。定義・解決確認のみ | `hilt-android:2.52`のPOM: `<artifactId>kotlin-stdlib</artifactId>` … `<version>1.9.24</version>`<br>`hilt-android:2.53`のPOM: `<artifactId>kotlin-stdlib</artifactId>` … `<version>2.0.0-Beta5</version>` | [Hilt Android 2.52 POM](https://repo.maven.apache.org/maven2/com/google/dagger/hilt-android/2.52/hilt-android-2.52.pom)、[2.53 POM](https://repo.maven.apache.org/maven2/com/google/dagger/hilt-android/2.53/hilt-android-2.53.pom) |
| KSP Plugin | 1.9.24-1.0.20 | Kotlin Compiler 1.9.24用。定義・解決確認のみ | “KSP 1.0.20 for kotlin compiler version 1.9.24” | [KSP 1.9.24-1.0.20の公開情報](https://github.com/google/ksp/releases/tag/1.9.24-1.0.20) |
| Timber | 5.0.1 | 公開安定版を維持 | 公開版: “5.0.1”<br>修正内容: “Fix TimberArgCount lint check false positive on some calls to `String.format`.” | [Timber 5.0.1の変更点](https://github.com/JakeWharton/timber/releases/tag/5.0.1) |
| JUnit 4 | 4.13.2 | 維持 | 公式リリース名: “JUnit 4.13.2” | [JUnit 4.13.2の公開情報](https://github.com/junit-team/junit4/releases/tag/r4.13.2) |
| AndroidX Test Core KTX / Ext JUnit KTX | 1.7.0 / 1.3.0 | Kotlinメタデータ1.9世代へ更新。Artifact解決確認済み | Core KTXの`androidx.test.core.app.ActivityScenarioKt.class`: `mv=[1,9,0]`<br>Ext JUnit KTXの`androidx.test.ext.junit.rules.ActivityScenarioRuleKt.class`: `mv=[1,9,0]` | [Core KTX 1.7.0 AAR](https://dl.google.com/dl/android/maven2/androidx/test/core-ktx/1.7.0/core-ktx-1.7.0.aar)、[Ext JUnit KTX 1.3.0 AAR](https://dl.google.com/dl/android/maven2/androidx/test/ext/junit-ktx/1.3.0/junit-ktx-1.3.0.aar) |
| ktlint Gradle Plugin | 14.2.0 | 選定時点の安定版のArtifact解決確認。Pluginは適用しない | Plugin Portal: “Version 14.2.0”<br>解決ログ: `org.jlleitschuh.gradle:ktlint-gradle:14.2.0` | [Plugin Portal 14.2.0](https://plugins.gradle.org/plugin/org.jlleitschuh.gradle.ktlint/14.2.0)、[解決ログ](docs/resolved-catalog-plugins.txt) |
| detekt Gradle Plugin | 1.23.8 | 適用済み。TvAppの設定を参照してKotlin DSLの品質タスクを実装 | 公式設定例: `id("io.gitlab.arturbosch.detekt") version "1.23.8"`<br>workspaceの適用処理: `apply<DetektPlugin>()` | [detekt 1.23.8の設定例](https://detekt.dev/docs/1.23.8/gettingstarted/gradle/)、[適用コード](native/gradle/git-hooks.gradle.kts)、[品質タスクの検証](docs/GIT_HOOKS.md) |

Composeライブラリには個別版を付けず、BOMを利用する構成です。Catalogに全項目を定義していますが、通常のLibraryには未使用依存を追加していません。

AppCompat、Truth、MockK、Turbine、coroutines-test、MockWebServer、KAPTのaliasや新規依存宣言はありません。Cordova由来の既存AppCompatは削除していません。

Hilt・KSPは適用せず、Hilt CompilerをProcessorとして有効化していません。`verifyCatalogResolution`・`verifyCatalogPlugins`では別構成でArtifactを解決するだけです。DIの実行・コード生成・Hilt/KSP Plugin併用時の動作保証は今回の検証に含みません。ktlint Pluginは未適用です。detektは追加タスクで適用し、読み取り専用checkとpreCommitを実行できます。ktlintCheckはdetektFormatCheckへの互換名です。詳細は[Git hookのKotlin DSL実装](docs/GIT_HOOKS.md)を参照してください。JUnitテストコードは追加していません。

選定根拠: [Cordova公式対応表](https://cordova.apache.org/docs/en/latest/guide/platforms/android/index.html)、[14.0.1既定値](https://github.com/apache/cordova-android/blob/14.0.1/framework/cdv-gradle-config-defaults.json)、[Compose BOM POM](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2025.07.00/compose-bom-2025.07.00.pom)、[Core](https://developer.android.com/jetpack/androidx/releases/core)、[Activity](https://developer.android.com/jetpack/androidx/releases/activity)、[DataStore](https://developer.android.com/jetpack/androidx/releases/datastore)、[WorkManager](https://developer.android.com/jetpack/androidx/releases/work)、[Hilt 2.52 POM](https://repo.maven.apache.org/maven2/com/google/dagger/hilt-android/2.52/hilt-android-2.52.pom)、[kotlin-result移行説明](https://github.com/michaelbull/kotlin-result/releases/tag/2.0.0)。最終的な解決座標は[ライブラリ](docs/resolved-catalog-libraries.txt)・[プラグイン](docs/resolved-catalog-plugins.txt)を参照してください。

## Catalog共有・プラグイン競合対策・hook

nativeはGradle標準の`gradle/libs.versions.toml`自動読込を使います。Cordovaは同じTOMLをSettingsスコープで`libs`として登録します。生成側の別Catalogや同名登録があれば上書きせず明示的に失敗します。

シェルからは`./scripts/catalog-version.sh gradle`などで同じCatalogの版を取得します。`[versions]`内の`name = "value"`形式をsh・awkで読み取ります。hookのテストもシェルで実行し、CordovaのJavaScript関数を呼ぶ部分だけNode.jsを使います。

nativeルートだけがAGP・KotlinをCatalogの版付きaliasで宣言します。共有LibraryはversionなしのPlugin IDを使います。Cordova側では既存`buildscript`クラスパスをそのまま使うため、既知でない版のPluginが既にクラスパスにあるという競合を避けられます。Hilt・KSP等の未使用Pluginを`apply false`で先行ロードすることもしていません。

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
npm run test:git-hooks

# Catalog全Artifactの解決と、検証用Kotlinコードのコンパイル。
npm run verify:catalog
npm run verify:compile
```

`-PverifyCatalog=true`の検証ビルドだけが`verification/fixtures/*.kt`を`build/generated`へコピーし、Catalog依存とassetsを注入します。画面・Activity・DIは実装しません。Compose CompilerとKotlin APIの互換性、両Libraryのassetsマージを確認するための仕組みです。通常の配布APKには含めません。検証後に通常ビルドへ戻す場合は生成物をcleanしてください。

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
| native/gradle/git-hooks.gradle.kts・config/detekt/配下 | TvAppを参照したdetekt・整形・preCommitの実装と設定 |
| verification/git-hooks-fixture.gradle.kts・verify-gradle-git-hooks.py | 一時Git repositoryでの品質ゲート・index保持の実行検証 |
| native/feature・dataのbuild.gradle.kts | Library化、Java/Kotlin 11、SDK、feature→dataの設定 |
| native/feature・dataのManifest | Activityを追加しない空のLibrary Manifest |
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

実測結果、APK、コマンド、残存事項は[検証レポート](docs/VALIDATION.md)に記載しています。追加したdetekt・preCommit環境、TvAppとの差異、15ケースの実行検証は[Git hookのKotlin DSL実装](docs/GIT_HOOKS.md)を参照してください。

## ライセンス

適用範囲は[LICENSE](LICENSE)、帰属表示は[NOTICE](NOTICE)を参照してください。ライセンス全文と必要な帰属表示は、APK内の`assets/www/licenses/THIRD_PARTY_NOTICES.txt`に同梱します。アプリ内のライセンス画面は設けていません。

ライセンス原文・出典と通常APKの実行時依存44件の[台帳](native/third-party-licenses/runtime.json)を、`native/third-party-licenses/`で管理します。Cordovaのビルド前に依存の版・ハッシュと通知の整合性を検証します。更新時の手順、確認根拠、対象範囲は[ライセンス遵守の記録](docs/LICENSE_COMPLIANCE.md)を参照してください。`npm run licenses:verify`で単独実行できます。

| 用途 | 置き場所 |
| --- | --- |
| ライセンス原文・著作権表示の保管 | [native/third-party-licenses/](native/third-party-licenses/) |
| 実行時依存の版・ライセンス・ハッシュの台帳 | [runtime.json](native/third-party-licenses/runtime.json) |
| 原文の出典・照合情報 | [upstream-sources.json](native/third-party-licenses/upstream-sources.json)、[cordova-sources.json](native/third-party-licenses/cordova-sources.json) |
| ソース配布時の適用範囲・帰属表示 | ルートの[LICENSE](LICENSE)、[NOTICE](NOTICE) |
| APKに取り込む通知テキストの生成先 | [CordovaComposeApp/www/licenses/THIRD_PARTY_NOTICES.txt](CordovaComposeApp/www/licenses/THIRD_PARTY_NOTICES.txt) |
| APK内の格納先 | `assets/www/licenses/THIRD_PARTY_NOTICES.txt` |

原文・台帳を更新した場合は、`npm run licenses:generate`で`NOTICE`とAPK同梱用テキストを再生成し、生成結果もGitで管理します。
