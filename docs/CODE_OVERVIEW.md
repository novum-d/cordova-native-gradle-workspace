# コード・設定の役割と読み方

手書きで管理するコード・設定を、[コメント記述ルール](../AGENTS.md)に従って確認しています。各ファイルでは処理のまとまりごとに目的・判断理由を説明し、既存のKDocで概要が分かる箇所は説明を重複させません。JSONや生成・配布元のファイルについては、この文書で補足します。

## セットアップからビルドまで

1. [bootstrap.sh](../scripts/bootstrap.sh)がルートとアプリ側のnpm依存を復元し、Catalog指定のGradleとWrapperを用意します。不完全なCordova platformは退避して再生成し、正常なplatformはprepareします。
2. [with-env.sh](../scripts/with-env.sh)がJDK・SDK・Gradleの環境を子プロセスへ渡します。[cordova.sh](../scripts/cordova.sh)はその環境で、ルートに導入したCLIをアプリディレクトリから実行します。[catalog-version.sh](../scripts/catalog-version.sh)は各シェルが必要とする版をTOMLから取り出します。
3. [config.xml](../CordovaComposeApp/config.xml)のhookから[copy-gradle-extras.js](../CordovaComposeApp/hooks/copy-gradle-extras.js)を呼び、生成されたGradleファイルへ共有設定の参照を登録します。管理マーカー外の既存設定は保持します。
4. [settings-extras.gradle](../native/gradle/settings-extras.gradle)がSettings評価時にCatalog・feature・dataを登録し、[build-extras.gradle](../native/gradle/build-extras.gradle)がapp依存・実効版の照合・Lint設定・ルート検証を追加します。native単独では[settings.gradle.kts](../native/settings.gradle.kts)と[build.gradle.kts](../native/build.gradle.kts)が入口です。

## コミット時の検査

[setup-git-hooks.sh](../scripts/setup-git-hooks.sh)は、各cloneのhookからGradleの`:preCommit`を呼ぶ入口を設置します。既存の独自hook・シンボリックリンク・`core.hooksPath`は変更しません。

[git-hooks.gradle.kts](../native/gradle/git-hooks.gradle.kts)の処理は次の順序です。

1. ステージされたKotlinの有無を調べ、nativeのステージ済みソースをindexから別ディレクトリへ取り出します。作業ツリーの整形前ハッシュも保存します。
2. Kotlinのステージがあればnativeの本体・テストを整形します。変更が発生したらコミットを止め、利用者が差分を確認して再ステージできるようにします。
3. 作業ツリーの残存整形違反と静的解析の指摘を確認した後、取り出したindexの内容にも整形チェックを行います。作業ツリーだけが整っていても、古いindexのままの再試行は通しません。
4. ルール違反時だけ、該当フェーズのdetekt標準HTMLを検査の最後に開きます。設定・実行エラーや表示失敗が解析結果を置き換えないようにします。

Git indexを自動更新する処理はありません。通常の`check`・`ktlintCheck`・`detektFormatCheck`は読み取り専用です。詳細は[Git hookの仕様](GIT_HOOKS.md)を参照してください。

## Library・テスト・検証コード

| 管理元 | 役割 |
| --- | --- |
| [featureの設定](../native/feature/build.gradle.kts)・[dataの設定](../native/data/build.gradle.kts) | Android Libraryの依存・JVM target・コード生成・単体テスト・Dokkaを設定。依存方向はfeature → data |
| [MainActivity](../native/feature/src/main/kotlin/dev/example/nativefeature/MainActivity.kt) | Composeの起動・状態更新を確認する画面。既存KDocに処理の概要を記載 |
| feature・dataの`src/main/AndroidManifest.xml`とfeatureの`src/debug/AndroidManifest.xml` | Libraryの最小Manifestと、Debugでだけ外部起動できる検証Activityの登録 |
| 両モジュールの`src/test/kotlin/` | Mainの差し替え、仮想時間、Flow、DatetimeのJSON化を確認する環境テスト。各クラス・テストのKDocを参照 |
| [catalog-verification.gradle](../verification/catalog-verification.gradle) | 宣言済み依存・実際の構成別依存・実効ビルド基盤を検証。`verifyCatalog=true`ではコンパイル用fixtureを生成ディレクトリへ取り込む |
| [featureのfixture](../verification/fixtures/feature.kt)・[dataのfixture](../verification/fixtures/data.kt) | Compose・不変コレクション・日時・Serialization・Hilt/KSPなどのAPI参照と生成コードを実コンパイル |
| [compose-bom-probe.init.gradle](../verification/compose-bom-probe.init.gradle) | 明示的な`-I`指定時だけ比較用BOMを追加し、コンパイル前に解決版を記録 |
| [ToolingSync.java](../verification/ToolingSync.java)・[tooling-sync.sh](../scripts/tooling-sync.sh) | IDEのSyncと同じTooling APIでAndroidモデルを取得 |
| [check-android.sh](../scripts/check-android.sh) | native・Cordovaの品質チェック、単体テスト、Android Lint、基盤検証を順に実行 |
| `verification/*.test.sh` | Cordova hookの冪等性、Git hookの設置と終了コード、初回セットアップと破損復旧を一時環境で検証 |
| [verify-gradle-git-hooks.py](../verification/verify-gradle-git-hooks.py)・[git-hooks-fixture.gradle.kts](../verification/git-hooks-fixture.gradle.kts) | 作業ツリーとindexが異なる状態、違反・実行エラー・HTML表示を実際のGradleタスクで検証 |
| [verify-ktlint.sh](../verification/verify-ktlint.sh) | 通常プロジェクトの互換名とは別に、本物のktlint Plugin・エンジンで検出と整形を確認 |
| [libs.versions.toml](../native/gradle/libs.versions.toml)・[gradle.properties](../native/gradle.properties)・[detekt設定](../native/gradle/config/detekt/) | 採用版、Gradle実行環境、静的解析・整形ルールの管理元 |

## JSONの説明

### ルートのpackage.json

[ルートのpackage.json](../package.json)はworkspaceのnpmコマンドとCordova CLIを管理します。`name`・`version`・`description`はworkspaceの情報、`private`はnpm公開を防ぐ設定、`devDependencies.cordova`はCLIの固定版です。APKのアプリIDやSDKの管理元は`config.xml`です。

`scripts`の各コマンドは次の処理を実行します。

| コマンド | 目的 |
| --- | --- |
| `cordova:prepare` | Cordovaの生成設定とWeb assetsを管理元から復元 |
| `cordova:build` | CLIからAndroidアプリをビルド |
| `native:build` | native単独でfeature・dataのDebug AARをビルド |
| `verify:catalog` | CatalogのライブラリとPluginマーカーを別構成で解決 |
| `verify:dependencies` | 実際の本体・テスト・KSPの構成別依存を解決 |
| `verify:compile` | 検証用fixtureを取り込み、ライブラリのAPI・コード生成を実コンパイル |
| `test:hooks` | Cordova hookの4ケースを検証 |
| `test:bootstrap` | 初回構築・復旧・宣言不足の12ケースを検証 |
| `test:git-hooks` | Git hookセットアップの5ケースを検証 |
| `test:unit` | nativeのfeature・dataでDebug／Releaseの単体テストを実行 |
| `test:unit:cordova` | prepare後、Cordova側から同じ単体テストを実行 |
| `test:unit:coverage` | 両LibraryのDebugカバレッジを生成 |
| `docs:kdoc` | feature・dataのKDoc HTMLをnativeルートで集約 |
| `docs:kdoc:cordova` | prepare後、Cordova側から各LibraryのKDoc HTMLを個別生成 |
| `check:android` | 両ホストの品質チェック・Android Lint・構成検証を順に実行 |
| `verify:structure` | モジュール境界・JVM/SDK・Pluginの実効版を確認 |
| `verify:sync` | native単独のAndroid Tooling APIモデルを取得 |

### アプリ側のpackage.json

[CordovaComposeApp/package.json](../CordovaComposeApp/package.json)はアプリ側のnpm依存を管理します。`dependencies.cordova-android`はAndroid実装の固定版、`cordova.platforms`は利用するplatformの宣言です。名称・説明・作者などはテンプレート由来のメタデータで、`main`はnpmパッケージの入口です。Web画面の開始ファイルは`config.xml`の`content`が指定します。

アプリ側の`scripts.test`はテンプレートの未設定メッセージを出して失敗するコマンドです。このworkspaceの単体テストはルートの`test:unit`を使います。

## 直接コメントを追加しないファイル

- `build/`、Cordovaの`platforms/`・`plugins/`、KSPの生成ソース、検証ログ・レポート・スクリーンショットは生成物です。生成元のスクリプト・設定へ説明を付けます。
- `package-lock.json`はnpmが作成する依存固定情報、`native/gradlew`・`gradlew.bat`・Wrapper JAR・`gradle-wrapper.properties`はWrapperの生成物です。Wrapperの版はCatalogと`native/build.gradle.kts`から管理します。
- `CordovaComposeApp/www/`とアプリ側の`.gitignore`は配布元のテンプレートを保持します。`index.html`は起動画面とJS/CSS読込、`www/js/index.js`は`deviceready`後の表示切替、`www/css/index.css`はレイアウト・イベント状態・配色を担います。
- Markdown本文や既存のモジュール概要は、それ自体を説明として扱います。画像・JARなどのバイナリへコメントは埋め込みません。
