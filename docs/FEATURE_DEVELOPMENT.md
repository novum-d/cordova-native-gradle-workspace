# Cordovaなしでfeatureを開発する

[ドキュメント一覧](README.md) · [起動要求の仕様](SERVICE_NAVIGATION.md) · [WebView通信サンプル](WEBVIEW_BRIDGE_SAMPLE.md)

`native/dev-app`は開発専用のDebug APKです。既存のfeatureのMainActivityへIntentサンプルを渡すだけのホストで、画面・WebView・サービス処理はfeatureの実装を使います。Cordovaのprepare・プラグイン起動は不要です。

## Android Studioで実行する

1. Android Studioで`native/`を開き、Gradle Syncを実行する。JDKは既存どおり17を使う。
2. 実行するアプリを`dev-app`、Build Variantを`debug`にする。
3. 実機またはエミュレーターを選んでRunする。
4. 「Feature Dev」のサンプル一覧から起動する。ブレークポイントは既存のfeatureのMainActivityや各画面・ブリッジに置く。
5. feature画面の戻る操作でサンプル一覧へ戻る。不正要求でも一覧へ戻る。

アプリIDは`dev.example.nativefeature.dev`です。Cordovaホストの`dev.example.cordovanative`と同時にインストールできます。実行にはAndroid SDK等の既存環境が必要ですが、Cordovaのplatformやプラグインを生成する必要はありません。

## コマンドで実行する

リポジトリ直下から実行します。インストール時は端末を接続するか、エミュレーターを起動してください。

```sh
# Debug APKだけをビルドする。
npm run dev:build

# ビルドしてインストールする。アプリ起動は次のadbで行う。
npm run dev:install

# サンプルの選択画面を開く。
adb shell am start -n dev.example.nativefeature.dev/.DevLauncherActivity

# 指定したサンプルでfeatureを直接開く。
adb shell am start -n dev.example.nativefeature.dev/.DevLauncherActivity --es sample webview
adb shell am start -n dev.example.nativefeature.dev/.DevLauncherActivity --es sample golf-structured-args
```

複数端末への操作ではadbの`-s <serial>`などで対象を指定してください。Android StudioのRunでも、起動時のExtraに`sample`というStringを指定する方法が使えます。

DevLauncherActivityの`sample`は固定一覧から選ぶ開発専用キーです。featureの本番受け付けExtraではありません。任意の要求JSON・URI・Activity名を外部から受け取って転送する仕組みは追加していません。

## 用意したIntentサンプル

JSONにはコメントや説明用フィールドを追加せず、この表で用途を説明します。

| sample指定 | JSON | 確認する内容 |
| --- | --- | --- |
| `webview` | [webview.json](../native/dev-app/src/debug/assets/requests/webview.json) | sample.webviewで通信検証画面を開く |
| `golf-holder` | [golf-holder.json](../native/dev-app/src/debug/assets/requests/golf-holder.json) | ゴルフの読み取り画面、argsが空オブジェクト |
| `golf-omitted-args` | [golf-omitted-args.json](../native/dev-app/src/debug/assets/requests/golf-omitted-args.json) | args省略が「未指定」として届く |
| `golf-structured-args` | [golf-structured-args.json](../native/dev-app/src/debug/assets/requests/golf-structured-args.json) | 日本語・記号・入れ子・真偽値を持つargsが届く |
| `home` | [home.json](../native/dev-app/src/debug/assets/requests/home.json) | 確認用ホーム |
| `invalid-id` | [invalid-id.json](../native/dev-app/src/debug/assets/requests/invalid-id.json) | 未対応IDをfeature側が拒否して終了する |
| `invalid-args` | [invalid-args.json](../native/dev-app/src/debug/assets/requests/invalid-args.json) | argsの配列をfeature側が拒否して終了する |

通常のJSON構文は保ち、後ろの2件は起動要求の仕様に意図的に違反させています。ランチャー側で正常な型へ変換せず、そのまま`MainActivity.EXTRA_REQUEST`に格納します。このため、本物のActivityのJSON解析・検証を通せます。

既存サンプルの引数を変える場合はJSONを編集し、ビルド・インストールし直します。新しいサンプルは同じassetsフォルダへ追加し、[IntentSample.kt](../native/dev-app/src/debug/kotlin/dev/example/nativefeature/dev/IntentSample.kt)へキーと表示名を登録します。

開発ホストは`CLEAR_TOP | SINGLE_TOP`を付けて固定のMainActivityを起動します。同じタスクにfeature画面が残っている場合はそれを再利用し、`onNewIntent`による要求の切り替えも確認できます。新規IntentとActivity再生成の扱いは、既存のfeature側の実装に従います。

## フォルダ・ビルドの扱い

| 配置 | 役割 |
| --- | --- |
| [dev-app/build.gradle.kts](../native/dev-app/build.gradle.kts) | 共有Catalogの版を使う開発APK、Compose、feature依存、Release variant無効化 |
| [src/debug/AndroidManifest.xml](../native/dev-app/src/debug/AndroidManifest.xml) | Debugだけのランチャー、ラベル、INTERNET権限 |
| [DevLauncherActivity.kt](../native/dev-app/src/debug/kotlin/dev/example/nativefeature/dev/DevLauncherActivity.kt) | サンプル選択UI、固定assetsの読み込み、既存Activityへの起動要求 |
| [IntentSample.kt](../native/dev-app/src/debug/kotlin/dev/example/nativefeature/dev/IntentSample.kt) | UIとコマンドに共通の固定サンプル一覧 |
| [styles.xml](../native/dev-app/src/debug/res/values/styles.xml) | 明るい開発画面でもステータスバーの文字を読めるようにするテーマ |
| [requests/](../native/dev-app/src/debug/assets/requests/) | Debug専用の要求JSON |

Flavorは追加していません。開発ホストをモジュールで分け、ランチャー・要求JSONは`src/debug`に限定し、dev-appのRelease variantを無効にしています。CordovaのSettingsにはdev-appを登録せず、Cordova側のAPKには開発ランチャーや要求JSONを含めません。

featureはLibraryのまま、MainActivityは`exported=false`を維持します。featureからdev-appへの依存もありません。依存方向は`dev-app → feature → data`です。既存の版や解析ルールを変更せず、開発ホストのKotlinにも同じdetekt・整形チェックを適用します。

Intentモックは起動要求の固定データです。NFCの実機処理やWeb APIの応答をモックへ切り替える機能は追加していません。機器実装を切り替える必要ができた段階で、DIやFlavorの使い方を判断します。

CordovaPluginそのものの起動連携やコールバックを確認するときは、従来のCordovaホストも実行します。HiltやSDK等のホスト初期化が今後必要になった場合は、開発ホストにも必要な初期化を追加します。

## 検証

[開発ホストの検証記録](logs/feature-development/README.md)に、ビルド・UI操作・Intent再受信・起動要求の拒否・Cordova APKへの混入確認をまとめています。`npm run check:android`にdev-appのDebug Lintも含めています。
