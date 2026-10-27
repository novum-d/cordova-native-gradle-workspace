# Hiltの検証記録

[ログ一覧](../README.md) · [セットアップ](../../HILT.md)

2026-10-27に、Hilt 2.52とKSPによるコード生成・ビルド、API 35エミュレーターでの画面表示を確認しました。

| ファイル | 内容 |
| --- | --- |
| [results.json](results.json) | 3種類のAPKでのApplication・Hilt ViewModel・引数復元、テスト件数、ビルドとエミュレーター終了確認 |
| [ui-results.json](ui-results.json) | Android CLIのUI取得結果から抽出したアプリの表示文字と画面サイズ。縦→横→縦の9件 |
| [dev-debug.log](dev-debug.log) | 開発用Debugの起動ログ。Timberの初期化1件 |
| [cordova-debug.log](cordova-debug.log) | Cordova Debugの起動ログ。Timberの初期化1件 |
| [cordova-release.log](cordova-release.log) | Cordova Releaseの起動ログ。Debugログなし |
| [native-check.log](native-check.log) | 開発用APK・featureの両variant単体テスト・静的解析・整形・Android Lint |
| [native-final-check.log](native-final-check.log) | KDoc更新後の静的解析・整形・開発用APK・featureの単体テスト |
| [cordova-check.log](cordova-check.log) | CordovaのDebug／Release APK・Android Lint・root check |
| [native-classpath.log](native-classpath.log)・[cordova-classpath.log](cordova-classpath.log) | 両ルートでJavaPoet 1.13.0を解決することの確認 |
| [prepare.log](prepare.log) | config.xmlのhookによる共有設定の復元 |

開発用APKでは`golf-structured-args`を指定し、日本語・予約文字・ネストを含むJSONを画面で確認しました。CordovaではWebの「ゴルフ場ID・ホルダー番号を読み取る」を押し、実際の`cordova/exec`→KotlinPlugin→feature経路から空のargsを受け取ります。HiltのViewModelから引数を表示でき、画面回転後も内容は一致しました。9件のUI結果はJSONへ復元して元の引数と比較しています。

Releaseは検証用にDebugキーで署名したAPKですが、Debuggable属性は無効です。APK・署名ファイル・一時検証スクリプトは管理対象へ追加していません。検証後にエミュレーターを終了し、接続端末がないことを確認しました。NFCや業務Repositoryの注入は、この検証には含めていません。
