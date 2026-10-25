# Cordova CLI・Composeのエミュレーター実行確認（2026-10-19）

この文書はプラグイン導入前の実行記録です。現在は[プラグイン経由の起動](KOTLIN_PLUGIN.md)へ変更し、Activityは通常Manifestに`exported=false`で登録しています。

Android 15（API 35／arm64-v8a）のエミュレーターで、Cordova CLIによるインストール・起動と、同じAPKに含まれるCompose画面の表示・操作を確認しました。終了時にエミュレーターを閉じ、ADBの接続一覧が空であることも確認しています。[結果JSON](logs/compose-runtime/result.json)

`targetSdk`・`compileSdk`はどちらも35です。Cordovaの`config.xml`で明示し、Catalogでも`target-sdk = "35"`・`compile-sdk = "35"`を個別に管理して照合します。実行したDebug APKから`apkanalyzer manifest target-sdk`で35を読み取りました。実行端末のAPIレベルも35です。

## 確認した動作

| 確認項目 | 結果・証跡 |
| --- | --- |
| Cordova CLIでDebug APKをビルド・インストール・起動 | 終了コード0、`INSTALL SUCCESS`・`LAUNCH SUCCESS`。[CLIログ](logs/compose-runtime/cordova-run.log) |
| Cordovaの初期化 | `Device is Ready`をUI階層と画面で確認。[スクリーンショット](logs/compose-runtime/cordova-ready.png)、[UI XML](logs/compose-runtime/cordova-ui.xml) |
| Composeの実表示 | Material3のテキスト・ボタンを確認。[操作前](logs/compose-runtime/compose-before.png)、[起動結果](logs/compose-runtime/compose-start.txt) |
| Composeの状態更新 | ボタンをタップし、`Clicks: 0`から`Clicks: 1`へ変化。[操作後](logs/compose-runtime/compose-after.png)、[UI XML](logs/compose-runtime/compose-ui-after.xml) |
| 戻る操作 | Cordova画面の`Device is Ready`へ復帰。[UI XML](logs/compose-runtime/cordova-return-ui.xml) |
| アプリのクラッシュ | 検証中のcrashバッファにアプリのクラッシュなし。[ログ](logs/compose-runtime/crash.log) |
| エミュレーター終了 | `adb emu kill`後、プロセス終了と接続一覧が空であることを確認。[終了ログ](logs/compose-runtime/shutdown.log)、[接続一覧](logs/compose-runtime/devices-after.txt) |

以前の検証はKotlin／ComposeのコンパイルとAPKビルドまででした。今回は実表示に加え、`remember { mutableIntStateOf(0) }`の更新が画面へ反映されるところまで確認しています。

## 追加した確認用画面

featureの[`MainActivity.kt`](../native/feature/src/main/kotlin/dev/example/nativefeature/MainActivity.kt)を`ComponentActivity`・`setContent`・Material3を使用する画面へ変更しました。当時はDebug用ManifestだけにActivityを登録し、ReleaseのManifestには登録していませんでした。LauncherはCordovaのActivityです。[当時のManifest照合結果](logs/compose-runtime/manifest-check.json)。Debug専用Manifestは現在削除済みです。

Cordova CLIで最初に起動するのはCordovaのWebViewです。その後、ADBで同じAPK内のCompose Activityを明示起動して確認しています。WebViewからComposeを開くJavaScriptブリッジは追加していません。

## 実行方法

今回使用したAVD名は`Cordova_Compose_API_35`、起動時の端末IDは`emulator-5556`でした。別の環境では`adb devices`で端末IDを確認して置き換えてください。

```sh
# AVDを起動し、接続された端末IDを確認
android emulator start Cordova_Compose_API_35 --cold
adb devices

# 起動済み端末IDを指定してCordova CLIから実行
./scripts/cordova.sh run android --debug --emulator --target=emulator-5556 --verbose

# 同じAPKのCompose画面を起動
adb -s emulator-5556 shell am start -W \
  -n dev.example.cordovanative/dev.example.nativefeature.MainActivity

# 確認後はエミュレーターを終了
android emulator stop emulator-5556
adb devices
```

起動済みの端末を使うときは`--target`へ端末IDを渡します。AVD名を渡すと、Cordova Android 14は別のエミュレーターの新規起動を試みます。

表示確認用のSDKイメージとAVDは再実行用にローカルへ残しています。動作確認を行う処理には`finally`でエミュレーターを終了する後処理を設け、成功・失敗にかかわらず終了するようにしました。

追加画面はdetekt・整形チェック・native Lintに合格し、アプリ側のDebug Lintにも成功しています。[Cordovaビルド](logs/compose-runtime/compose-runtime-build.log)、[アプリLint・Release Manifest処理](logs/compose-runtime/compose-runtime-checks.log)
