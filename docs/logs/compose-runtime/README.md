# Compose画面のエミュレーター実行の記録

[ログ一覧へ戻る](../README.md) · [検証内容・実行方法](../../COMPOSE_RUNTIME.md)

保存済みの検証記録です。採用構成や実行手順の現状は、上の解説文書を参照してください。

| ファイル | 内容 |
| --- | --- |
| [compose-runtime-build.log](compose-runtime-build.log) | 実行確認に使用するCordova Debug APKのビルド。 |
| [compose-runtime-checks.log](compose-runtime-checks.log) | アプリLintとRelease Manifest処理。 |
| [compose-runtime-target-sdk.log](compose-runtime-target-sdk.log) | SDK 35明示後のAPK再ビルド。 |
| [result.json](result.json) | CLI実行・画面表示・状態更新・復帰・終了をまとめた確認結果。 |
| [cordova-run.log](cordova-run.log) | Cordova CLIによるビルド・インストール・起動。 |
| [emulator.log](emulator.log) | 検証用エミュレーターの起動・実行ログ。 |
| [crash.log](crash.log) | 検証中のcrashバッファ。 |
| [shutdown.log](shutdown.log) | 検証後のエミュレーター終了。 |
| [devices-after.txt](devices-after.txt) | エミュレーター終了後のADB接続一覧。 |
| [manifest-check.json](manifest-check.json) | プラグイン導入前のDebug専用Activity登録とランチャーの照合。 |
| [compose-start.txt](compose-start.txt) | ADBによるCompose Activityの明示起動結果。 |
| [compose-activity.txt](compose-activity.txt) | Compose画面表示中のActivity状態。 |
| [cordova-activity.txt](cordova-activity.txt) | Cordova画面表示中のActivity状態。 |
| [cordova-ready.png](cordova-ready.png) | Device is Readyを表示したCordova画面。 |
| [compose-before.png](compose-before.png) | カウンタ操作前のCompose画面。 |
| [compose-after.png](compose-after.png) | カウンタ操作後のCompose画面。 |
| [cordova-ui.xml](cordova-ui.xml) | 起動後のCordova画面のUI階層。 |
| [cordova-return-ui.xml](cordova-return-ui.xml) | 戻る操作後のCordova画面のUI階層。 |
| [compose-ui-before.xml](compose-ui-before.xml) | 操作前のCompose画面のUI階層。 |
| [compose-ui-after.xml](compose-ui-after.xml) | 操作後のCompose画面のUI階層。 |
