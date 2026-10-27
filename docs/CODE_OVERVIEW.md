# コード・設定の役割と読み方

手書きで管理するコード・設定を、[コメント記述ルール](../AGENTS.md)に従って確認しています。各ファイルでは処理のまとまりごとに目的・判断理由を説明し、既存のKDocで概要が分かる箇所は説明を重複させません。JSONや生成・配布元のファイルについては、この文書で補足します。

以下は現在の暫定実装の説明です。WebViewのUI・画面遷移と、Kotlinブリッジによるサービス実行の構成方針は[CLIENT_API_SPEC.md](CLIENT_API_SPEC.md)にまとめています。

## セットアップからビルドまで

1. [bootstrap.sh](../scripts/bootstrap.sh)がルートとアプリ側のnpm依存を復元し、Catalog指定のGradleとWrapperを用意します。不完全なCordova platformは退避して再生成し、正常なplatformはprepareします。最後に[setup-kotlin-plugin.sh](../scripts/setup-kotlin-plugin.sh)でプラグインをリンク導入します。
2. [with-env.sh](../scripts/with-env.sh)がJDK・SDK・Gradleの環境を子プロセスへ渡します。[cordova.sh](../scripts/cordova.sh)はその環境で、ルートに導入したCLIをアプリディレクトリから実行します。[catalog-version.sh](../scripts/catalog-version.sh)は各シェルが必要とする版をTOMLから取り出します。
3. [config.xml](../CordovaComposeApp/config.xml)のhookから[copy-gradle-extras.js](../CordovaComposeApp/hooks/copy-gradle-extras.js)を呼び、生成されたGradleファイルへ共有設定の参照を登録します。管理マーカー外の既存設定は保持します。
4. [settings-extras.gradle](../native/gradle/settings-extras.gradle)がSettings評価時にCatalog・feature・dataとルートのHilt・KSP依存を登録し、[build-extras.gradle](../native/gradle/build-extras.gradle)がapp依存・HiltとKSPの適用・共通Applicationのsource set・実効版の照合・Lint設定・Git hookの品質タスクを追加します。native単独では[settings.gradle.kts](../native/settings.gradle.kts)と[build.gradle.kts](../native/build.gradle.kts)が入口です。

通常の起動は`npm run cordova:run`で行います。`cordova.sh`から`run android --emulator`を呼び、CLIがprepare・ビルド・インストール・起動を実行します。変更のたびにplatform・pluginを削除する処理や、事前のprepareは不要です。変更監視は行わないため、変更後に再実行します。

[kotlin-plugin](../kotlin-plugin/README.md)がWebからKotlin機能への入口です。`www/kotlin-plugin.js`の`cordova/exec`を受けて`KotlinPlugin.execute`が処理ID・任意argsをJSONとしてIntentへ格納し、MainActivityが処理IDから型付き画面を選びます。プラグイン内にGradle設定は置かず、native専用の[cordova-plugin-dev](../native/cordova-plugin-dev/build.gradle.kts)が元のKTソースを編集・コンパイル・文書化します。Cordova側はplugin.xmlからソースを導入し、cordova-plugin-devには依存しません。

## コミット時の検査

[setup-git-hooks.sh](../scripts/setup-git-hooks.sh)は、各cloneのhookからGradleの`:preCommit`を呼ぶ入口を設置します。既存の独自hook・シンボリックリンク・`core.hooksPath`は変更しません。

[git-hooks.gradle.kts](../native/gradle/git-hooks.gradle.kts)の処理は次の順序です。

1. ステージされたKotlinの有無を調べ、nativeのステージ済みソースをindexから別ディレクトリへ取り出します。作業ツリーの整形前ハッシュも保存します。
2. Kotlinのステージがあればnativeの本体・テストを整形します。変更が発生したらコミットを止め、利用者が差分を確認して再ステージできるようにします。
3. 作業ツリーの残存整形違反と静的解析の指摘を確認した後、取り出したindexの内容にも整形チェックを行います。作業ツリーだけが整っていても、古いindexのままの再試行は通しません。
4. ルール違反時だけ、該当フェーズのdetekt標準HTMLを検査の最後に開きます。設定・実行エラーや表示失敗が解析結果を置き換えないようにします。

Git indexを自動更新する処理はありません。通常の`check`・`ktlintCheck`・`detektFormatCheck`は読み取り専用です。詳細は[Git hookの仕様](GIT_HOOKS.md)を参照してください。

## Library・テスト・品質設定

| 管理元 | 役割 |
| --- | --- |
| [featureの設定](../native/feature/build.gradle.kts)・[dataの設定](../native/data/build.gradle.kts) | Android Libraryの依存・JVM target・コード生成・単体テスト・Dokkaを設定。依存方向はfeature → data |
| [NativeApplication](../native/host/src/main/kotlin/dev/example/nativehost/NativeApplication.kt) | 両APKで直接コンパイルするHiltルート。詳細は[Hiltの構成](HILT.md)を参照 |
| [dataのNFC処理](../native/data/src/main/kotlin/dev/example/nativedata/nfc/) | ICODE SLIのProfile選択・Profileからの通信方式とメモリ構成の解決・必要なブロックの読取・切り出し・解放。詳細は[NFC読取](NFC_READING.md)を参照 |
| [dataの業務Repository](../native/data/src/main/kotlin/dev/example/nativedata/golf/) | 検出対象とエラー文脈を固定し、業務項目の物理範囲を解決して生バイトを返す |
| [汎用タグ検出ユースケース](../native/feature/src/main/kotlin/dev/example/nativefeature/nfc/ObserveNfcTagsUseCase.kt) | ゴルフ以外にも再利用する検出の入口。UIからRepositoryを直接呼ばない |
| [ゴルフ場番号ユースケース](../native/feature/src/main/kotlin/dev/example/nativefeature/services/golfchange/ReadGolfCourseNumberUseCase.kt) | Repositoryから取得した2バイトのLSBファーストInt変換。失敗はユースケース固有の階層型で返す |
| [ホルダー番号ユースケース](../native/feature/src/main/kotlin/dev/example/nativefeature/services/golfchange/ReadGolfHolderNumberUseCase.kt) | 固定領域の14バイトを読取順・大文字・区切りなし28桁の16進Stringで返す |
| [MainActivity](../native/feature/src/main/kotlin/dev/example/nativefeature/MainActivity.kt) | 起動Intentを検証してNavigationへ渡すActivity。要求JSONを重複保存せず、画面とargsの復元はNavigation・ViewModelが担当。詳細は[画面構成](SERVICE_NAVIGATION.md)を参照 |
| feature・dataの`src/main/AndroidManifest.xml` | LibraryのManifest。featureのActivityは両variantで同一アプリから起動し、外部には公開しない |
| 両モジュールの`src/test/kotlin/` | Mainの差し替え、仮想時間、Flow、DatetimeのJSON化と、起動要求・WebMessage・URL許可の契約テスト。各クラス・テストのKDocを参照 |
| [check-android.sh](../scripts/check-android.sh) | native・Cordovaの品質チェック、単体テスト、Android Lintを順に実行 |
| [libs.versions.toml](../native/gradle/libs.versions.toml)・[gradle.properties](../native/gradle.properties)・[detekt設定](../native/gradle/config/detekt/) | 採用版、Gradle実行環境、静的解析・整形ルールの管理元 |

## 開発専用APK

[dev-app](../native/dev-app/build.gradle.kts)はnativeのSettingsにだけ登録するDebug APKです。ランチャーから固定の要求JSONをIntentでfeatureへ渡します。JSONごとの用途、source set、コマンドは[開発手順](FEATURE_DEVELOPMENT.md)を参照してください。ルートpackage.jsonの`dev:build`・`dev:install`もこのホストを対象にします。

## WebView通信サンプル

[WebViewSampleScreen.kt](../native/feature/src/main/kotlin/dev/example/nativefeature/webview/WebViewSampleScreen.kt)がComposeの検証UIを持ち、[WebViewBridge.kt](../native/feature/src/main/kotlin/dev/example/nativefeature/webview/WebViewBridge.kt)が一つのWebViewの接続と解放を管理します。[WebMessage.kt](../native/feature/src/main/kotlin/dev/example/nativefeature/webview/WebMessage.kt)でJSONを解析し、[WebViewPolicy.kt](../native/feature/src/main/kotlin/dev/example/nativefeature/webview/WebViewPolicy.kt)と[SampleWebViewClient.kt](../native/feature/src/main/kotlin/dev/example/nativefeature/webview/SampleWebViewClient.kt)で配信先と通信先を絞ります。[index.html](../native/feature/src/main/assets/index.html)は固定のHTMLとJavaScript・CSPを持ちます。通信の流れと制約は[サンプル文書](WEBVIEW_BRIDGE_SAMPLE.md)を参照してください。

## JSONの説明

### ルートのpackage.json

[ルートのpackage.json](../package.json)はworkspaceのnpmコマンドとCordova CLIを管理します。`name`・`version`・`description`はworkspaceの情報、`private`はnpm公開を防ぐ設定、`devDependencies.cordova`はCLIの固定版です。APKのアプリIDやSDKの管理元は`config.xml`です。

`scripts`の各コマンドは次の処理を実行します。

| コマンド | 目的 |
| --- | --- |
| `cordova:run` | prepare・ビルド後、エミュレータへインストールして起動。追加引数は`npm run cordova:run -- --target=<対象>`で渡す |
| `cordova:plugin` | ローカルプラグインをリンク導入。JS・plugin.xml変更時は`-- --reinstall`で再導入 |
| `cordova:prepare` | Cordovaの生成設定とWeb assetsを管理元から復元。ビルド・起動は行わない |
| `cordova:build` | prepareを含めてAndroid APKをビルド。起動は行わない |
| `dev:build` | dev-appのDebug APKだけをビルド。Cordovaのprepareは不要 |
| `dev:install` | dev-appのDebug APKをビルドして接続端末へインストール |
| `native:build` | native単独でfeature・data・cordova-plugin-devのDebug AARをビルド |
| `test:unit` | nativeのfeature・dataでDebug／Releaseの単体テストを実行 |
| `test:unit:cordova` | prepare後、Cordova側から同じ単体テストを実行 |
| `test:unit:coverage` | 両LibraryのDebugカバレッジを生成 |
| `docs:kdoc` | feature・data・プラグインのKDoc HTMLをnativeルートで集約 |
| `docs:kdoc:cordova` | prepare後、Cordova側から各LibraryのKDoc HTMLを個別生成 |
| `check:android` | 両ホストの品質チェック、native単体テスト、Android Lintを順に実行。起動は行わない |

### アプリ側のpackage.json

[CordovaComposeApp/package.json](../CordovaComposeApp/package.json)はアプリ側のnpm依存を管理します。`dependencies.cordova-android`はAndroid実装の固定版、`cordova.platforms`は利用するplatformの宣言です。名称・説明・作者などはテンプレート由来のメタデータで、`main`はnpmパッケージの入口です。Web画面の開始ファイルは`config.xml`の`content`が指定します。

アプリ側の`scripts.test`はテンプレートの未設定メッセージを出して失敗するコマンドです。このworkspaceの単体テストはルートの`test:unit`を使います。

`devDependencies.kotlin-plugin`は`file:../kotlin-plugin`のローカル依存、`cordova.plugins.kotlin-plugin`はCordovaのプラグイン導入宣言です。プラグイン側の[package.json](../kotlin-plugin/package.json)は名前・版・対応platform・導入ファイルを管理します。各項目の役割は[プラグインの説明](../kotlin-plugin/README.md)を参照してください。

## 直接コメントを追加しないファイル

- `build/`、Cordovaの`platforms/`・`plugins/`、KSPの生成ソース、検証ログ・レポート・スクリーンショットは生成物です。生成元のスクリプト・設定へ説明を付けます。
- `package-lock.json`はnpmが作成する依存固定情報、`native/gradlew`・`gradlew.bat`・Wrapper JAR・`gradle-wrapper.properties`はWrapperの生成物です。Wrapperの版はCatalogと`native/build.gradle.kts`から管理します。
- アプリ側の`.gitignore`は配布元のテンプレートを保持します。`CordovaComposeApp/www/`は標準テンプレートを基に、プラグインを呼ぶボタンとコールバックの表示を追加しています。変更したHTML・JS・CSSの処理にはコメントを記載します。
- Markdown本文や既存のモジュール概要は、それ自体を説明として扱います。画像・JARなどのバイナリへコメントは埋め込みません。
