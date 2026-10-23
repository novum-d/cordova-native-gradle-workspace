# KDocドキュメント生成

## 役割と構成

Dokka 2.2.0（Gradle Plugin v2）で、Kotlinソースに書いたKDocをHTMLへ変換します。KDoc本文を自動で書き足すタスクではありません。処理の目的・流れはソースのKDocに記述し、Gradle設定やJavaScriptなどの説明は各ファイルのコメントに残します。

版とPlugin aliasは[Version Catalog](../native/gradle/libs.versions.toml)、適用と集約は[nativeルート](../native/build.gradle.kts)で管理します。[feature](../native/feature/build.gradle.kts)・[data](../native/data/build.gradle.kts)・[cordova-plugin-dev](../native/cordova-plugin-dev/build.gradle.kts)にもPluginを適用し、Dokka標準の`dokka(project(...))`で文書を集約します。cordova-plugin-devはプラグインの元ソースを文書化し、Cordova側の生成コピーは含めません。

アプリのKotlinは1.9.24のままです。Dokkaの解析エンジンは文書化専用の構成で実行し、アプリの`implementation`へ追加しません。公式リリースが示す最小KGP要件は1.9.0以上です。[Dokka 2.2.0の変更点](https://github.com/Kotlin/dokka/releases/tag/v2.2.0)

採用理由は[依存関係の選定ドキュメント](DEPENDENCY_VERSION_SELECTION.md#kdocのhtml生成にdokka-220を採用)にも記載しています。

## 生成・表示

リポジトリ直下で実行します。

```sh
# feature・dataとプラグインをまとめたHTMLサイトを生成する。
npm run docs:kdoc

# 同じ処理をGradleから直接実行する。
./scripts/with-env.sh ./native/gradlew -p native :dokkaGeneratePublicationHtml

# macOSで生成結果を開く。生成タスク自体はブラウザーを自動起動しない。
open native/build/dokka/html/index.html

# Cordova prepare後、同じモジュールのHTMLを個別に生成する。
npm run docs:kdoc:cordova
```

ルートのタスクには先頭の`:`を付けます。先頭の`:`がないと、各モジュールの同名タスクまで実行するGradleのタスク選択になります。[Dokkaの実行方法](https://kotlinlang.org/docs/dokka-get-started.html)

| 実行・閲覧対象 | タスク／出力先 |
| --- | --- |
| nativeの集約HTML | `:dokkaGeneratePublicationHtml` → `native/build/dokka/html/index.html` |
| featureの個別HTML | `:feature:dokkaGeneratePublicationHtml` → `native/feature/build/dokka/html/index.html` |
| プラグインの個別HTML | `:cordova-plugin-dev:dokkaGeneratePublicationHtml` → `native/cordova-plugin-dev/build/dokka/html/index.html`。native側だけに存在するタスク |
| dataの個別生成 | `:data:dokkaGeneratePublicationHtml`。現在は本体クラスがないためHTMLの出力なし。APIを追加すると`native/data/build/dokka/html/index.html`へ出力 |
| npmの入口 | [package.json](../package.json)の`docs:kdoc`はnative集約、`docs:kdoc:cordova`はprepareとCordovaからの個別生成を実行 |

Cordova側のルートには集約Pluginを追加していません。両ホストはモジュールの`build/`を共有するため、同時実行を避けて順番に生成します。HTMLは`build/`配下の生成物で、Gitには含めません。

## 掲載対象

- Android共通の`main` source setに限定し、`src/main/kotlin`のpublic・protected APIを掲載します。Dokka上の表示名は`androidJvm`です。Debug／Release・単体テスト・Androidテスト・生成ソースは掲載しません。継承元だけにあるメソッドは省き、このモジュールで実装したAPIを表示します。
- [MainActivityのKDoc](../native/feature/src/main/kotlin/dev/example/nativefeature/MainActivity.kt)に、Compose起動確認の目的・画面構築・状態更新の概要を記述しています。非公開のComposableのKDocはソースから読めますが、この公開API一覧には表示されません。
- [KotlinPluginのKDoc](../kotlin-plugin/src/android/dev/example/kotlinplugin/KotlinPlugin.kt)は、cordova-plugin-devから`kotlin-plugin/src/android`をmainのソースとして読み込みます。executeによる起動・成功通知・未知のactionの扱いを記載しています。
- [featureの概要](../native/feature/Module.md)と[dataの概要](../native/data/Module.md)を、各モジュールの説明元として指定しています。dataには業務用の本体クラスがないため、Dokkaはdataの個別ページを生成しません。dataの現状は[集約トップの概要](../native/Module.md)に記載し、APIを追加した後はdataの概要も個別ページに反映されます。
- コメントの有無・品質を自動判定するタスクではありません。コメントの記述ルールは[AGENTS.md](../AGENTS.md)に従います。

設定APIと集約方法は[Dokka Gradle設定](https://kotlinlang.org/docs/dokka-gradle.html)、[source set設定](https://kotlinlang.org/docs/dokka-gradle-configuration-options.html)を参照してください。

## 検証結果（2026-10-22）

以下はDokka導入時の記録です。依存選定用の独自タスクは現在削除しており、`check:android`は整形・静的解析・単体テスト・Android Lintを実行します。

- `npm run docs:kdoc`と`npm run docs:kdoc:cordova`が成功しました。集約HTMLと、Cordovaから生成したfeatureの個別HTMLで同じKDocを確認しました。
- Chrome 155を専用の一時プロファイルで起動し、集約トップ → feature → パッケージ → MainActivity → onCreateのリンクを実際に操作しました。日本語の説明とスタイルの表示も確認しています。[トップの画面](kdoc/overview.png)、[MainActivityの画面](kdoc/main-activity.png)
- 集約HTML全8ページ・内部参照113件を確認し、リンク先・表示リソースの欠落、JavaScriptエラーは0件でした。テストクラス・生成クラスが掲載されていないことも確認しました。[表示確認の結果](kdoc/display-check.json)。確認用ブラウザーとローカルHTTPサーバーは終了しています。
- `npm run check:android`が成功しました。両ホストのdetekt・整形チェック、Debug／Release Android Lint、nativeのfeature／data単体テスト4構成（各2件）、基盤検証を通過しています。
- CordovaアプリのDebug／Release APKビルドと、30構成のモジュール依存解決が成功しました。アプリ・テストの依存にDokkaはなく、Kotlin標準ライブラリは1.9.24を維持しています。
- 当時のbootstrapの12テストが成功し、新規生成したCordova platformでも共有LibraryのGradle設定を読み込めることを確認しました。検証専用コードは削除済みです。
