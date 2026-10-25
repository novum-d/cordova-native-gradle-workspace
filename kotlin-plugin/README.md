# kotlin-plugin

CordovaのWeb画面からKotlinの機能へ入るプラグインです。Gradle設定はこのディレクトリに置かず、ホスト側の`native/gradle/`と編集用の`native/cordova-plugin-dev/`で管理します。

## 呼び出しの流れ

`openFeature({id, args?}, success, error)` → `cordova/exec` → `KotlinPlugin.execute` → 起動要求をJSONとしてIntentへ格納 → `MainActivity`が処理IDを型付き画面へ解決、の順で処理します。

```js
// deviceready以降に呼ぶ。成功通知は起動要求の完了であり、画面から戻った結果ではない。
window.KotlinPlugin.openFeature(
    { id: 'nfc.read', args: { source: 'web' } },
    function () { console.log('Kotlin screen opened'); },
    function (error) { console.error(error); }
);
```

`id`は必須のStringで、`nfc.read`はNFC読み取り、`nfc.write`はNFC書き込み、`camera.capture`はCamera撮影画面を直接開きます。確認用の`home`も利用できます。`args`は任意のJSONオブジェクトです。省略と`{}`を区別し、配列・文字列などを受け付けません。未対応IDや引数不正はエラーコールバックへ返し、Activityを起動しません。[型付き引数とIntentの仕様](../docs/NFC_NAVIGATION.md)

未知のactionは`execute`が`false`を返し、CordovaがエラーとしてJSへ通知します。起動時の例外もエラーコールバックへ返します。画面を閉じた結果をWebへ返す処理はありません。

## ファイルの役割

| ファイル | 役割 |
| --- | --- |
| `plugin.xml` | JSの公開名、service名とAndroidクラスの対応、導入するKTソースを宣言 |
| `src/android/dev/example/kotlinplugin/KotlinPlugin.kt` | CordovaPluginを継承し、要求JSONを検証し、固定のActivityへのIntentに格納する |
| `www/kotlin-plugin.js` | `cordova/exec`へ要求オブジェクト・service・action・コールバックを渡す |
| `package.json` | npmで扱う名前・版・CordovaのIDとAndroid対応を宣言。`private`で誤公開を防ぎ、`files`で導入対象を限定 |

## ホスト・編集環境

このworkspaceでは`CordovaComposeApp`へローカルプラグインとして導入します。ホストにKotlinの有効化とfeatureへのGradle依存が必要です。プラグイン単体でfeatureやビルド環境を導入する仕組みは持ちません。

`native/`をAndroid Studioで開いてGradle Syncすると、`cordova-plugin-dev`のmainソースとして元のKTファイルを認識します。`CordovaPlugin`などは編集用モジュールの`compileOnly`、`MainActivity`はfeature依存で解決します。元ファイルを編集し、生成先の`platforms/`は編集しません。

初回はworkspaceルートで`./scripts/bootstrap.sh`を実行します。最後にリンク導入を行うため、KTの編集後は`npm run cordova:run`で変更を反映できます。platformを手動で作り直した場合は`npm run cordova:plugin`でリンク導入を復元します。

プラグインのJS・`plugin.xml`の変更時は、Cordovaによる再導入が必要です。`npm run cordova:plugin -- --reinstall`でこのプラグインだけを再導入してから、`npm run cordova:run`を実行します。アプリ側の`www/`の変更は通常のprepareで反映できます。

仕様の根拠: [Androidプラグインのexecute・UIスレッド](https://cordova.apache.org/docs/en/latest/guide/platforms/android/plugin.html)、[plugin.xml](https://cordova.apache.org/docs/en/latest/plugin_ref/spec.html)、[ローカルプラグインのリンク導入](https://cordova.apache.org/docs/en/latest/reference/cordova-cli/#cordova-plugin-command)。
