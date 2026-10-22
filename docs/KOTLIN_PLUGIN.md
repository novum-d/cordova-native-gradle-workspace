# Kotlinプラグインの構成と確認（2026-10-22）

## 構成

`CordovaComposeApp`は動作確認用のCordovaアプリ本体、[kotlin-plugin](../kotlin-plugin/README.md)はWebからKotlin機能を呼ぶ入口です。

```text
CordovaComposeApp/www/js/index.js
  → window.KotlinPlugin.openFeature
  → kotlin-plugin/www/kotlin-plugin.js の cordova/exec
  → KotlinPlugin.execute("openFeature", ...)
  → UIスレッドで startActivity(Intent(activity, MainActivity::class.java))
  → native/feature の MainActivity
```

プラグイン内は`plugin.xml`・`package.json`・`src/`・`www/`・READMEだけで、Gradleファイルはありません。Kotlin有効化はアプリの`config.xml`、feature・dataへの依存は既存の`native/gradle/build-extras.gradle`が管理します。

featureのActivityはmain Manifestへ移し、Debug／Releaseの両方でプラグインから起動できるようにしました。`exported=false`のため外部アプリからは起動できず、ランチャーはCordovaのActivityを維持しています。成功コールバックは起動要求の完了で、画面を閉じた結果は返しません。

## Android Studioで編集する

1. `native/`を開き、Gradle JDKを17、GradleをWrapperに設定してSyncします。
2. `cordova-plugin-dev`のmainソースから`dev.example.kotlinplugin.KotlinPlugin`を開きます。
3. 実体は`kotlin-plugin/src/android/dev/example/kotlinplugin/KotlinPlugin.kt`です。元ファイルを編集します。

[cordova-plugin-dev/build.gradle.kts](../native/cordova-plugin-dev/build.gradle.kts)は、プラグインの元ソースをsource setに直接登録します。`CordovaPlugin`と`CallbackContext`は`compileOnly(libs.cordova.framework)`、画面の型は`implementation(project(":feature"))`で解決します。

cordova-plugin-devはnative専用です。Cordova側のSettingsへは追加せず、アプリはplugin.xmlで導入されたソースを直接コンパイルします。Cordova APIの実体は既存のCordovaLibが提供し、編集用のframework依存はAPKに追加しません。[ソース登録・依存範囲の照合結果](kotlin-plugin/source-model.json)

## セットアップ・変更反映

```sh
# 初回。npm依存・platform・プラグインを復元し、最後にKTをリンク導入する。
./scripts/bootstrap.sh

# 通常のKT編集・アプリ側Web変更後。prepare・ビルド・インストール・起動を含む。
npm run cordova:run

# platformを手動で再作成した場合、リンク導入を復元する。
npm run cordova:plugin

# プラグインのJS・plugin.xml・導入対象ファイルを変更した場合だけ再導入する。
npm run cordova:plugin -- --reinstall
```

`setup-kotlin-plugin.sh`は正常なリンクを維持し、コピーで復元された場合だけ対象プラグインをリンク導入し直します。`--nosave`を使い、依存宣言は変更しません。アプリのpackage.jsonではローカルプラグインを`devDependencies`と`cordova.plugins`に保存し、Cordovaによる復元とそろえています。

静的解析・自動整形・ステージ内容の整形確認にも`kotlin-plugin/src/**/*.kt`を追加しました。Cordovaが生成する導入先は重複検査しません。`npm run native:build`で編集用モジュールもコンパイルし、`npm run docs:kdoc`でプラグインのKDocも集約します。

## 確認結果

- Android API 35エミュレータで`npm run cordova:run -- --target=emulator-5554`を実行しました。Webのボタンからプラグインを経由してCompose画面へ遷移し、カウンタが0から1へ更新され、戻る操作でWebへ復帰して成功メッセージが表示されました。クラッシュログはありません。[Web画面](kotlin-plugin/web.png)、[Compose画面](kotlin-plugin/compose.png)
- 確認用エミュレータは終了し、ADBの接続一覧が空であることを確認しました。
- CordovaのDebug／Release APK、nativeのcordova-plugin-devのDebug／Release AARをビルドしました。両APKでActivityの`exported=false`と、プラグインクラスが1回だけ定義されることも照合しました。[APK照合結果](kotlin-plugin/artifact-check.json)。cordova-plugin-devを含む静的解析・整形確認・単体テスト・両variantのAndroid Lintが成功しました。
- 集約KDoc生成タスクが成功し、KotlinPluginとexecuteのHTMLが生成されました。
- 両方のnode_modules・Cordova platform・pluginsがない日本語・空白入りの一時workspaceで、bootstrapだけでプラグインの復元・リンク導入・Cordova側のKotlinコンパイルが成功しました。Node・JDK・SDKと取得済みGradle・キャッシュは既存環境を利用しています。package.json・lockfileの変更がないこと、セットアップの再実行、明示的な再導入も確認しました。
- 一時Gitリポジトリで、作業ファイルが整形済みでも未整形のプラグインがステージされている場合は、再ステージしない2回の試行を両方拒否しました。作業ファイルとindexは保持され、整形済み内容を再ステージすると成功しました。[一時workspaceの結果](kotlin-plugin/workspace-check.json)

Android Studio向けのソース登録とCordova APIの依存解決はGradleの設定と実コンパイルで確認しています。IDE画面での補完・参照移動の確認は、この記録には含めていません。
