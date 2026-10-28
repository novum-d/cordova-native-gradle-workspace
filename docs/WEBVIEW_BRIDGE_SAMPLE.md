# WebViewとKotlinの双方向通信サンプル

[ドキュメント一覧](README.md) · [全体仕様](CLIENT_API_SPEC.md) · [Vue連携方針](VUE_WEBVIEW_INTEGRATION.md) · [検証証跡](logs/webview-bridge/README.md)

更新日: 2026-10-25。Web側の回答に依存しない、固定HTMLによる通信検証を実装しました。既存のfeatureのMainActivityに型付きルートを追加しています。新しいActivity・モジュール・依存ライブラリは追加していません。WebKit 1.12.1、AGP・Kotlin・Composeの版、静的解析ルール、dataは変更していません。

## 起動・操作

```sh
npm run cordova:plugin -- --reinstall
npm run cordova:run
```

1. Cordova画面の「WebView通信を検証する」を押す。要求は`{id: 'sample.webview'}`。
2. ComposeとWebの接続状態が接続済みになったことを確認する。
3. Webの「API実行」でユーザーIDと名前を取得し、Web・Compose・DebugのLogcatを確認する。
4. Composeの「NFCイベント送信」で、WebにタグID `ABC123`とCustomEvent発火回数が表示されることを確認する。
5. 「再読み込み」を押す。表示結果・回数は初期化され、READY後に通信を再確立する。
6. 「戻る」またはAndroidの戻る操作でCordovaへ戻る。

既存の`home`と`golf-change.read-holder`も利用できます。`sample.webview`は検証用IDで、業務サービスIDではありません。`openFeature`のコールバックは従来どおりActivity起動要求の完了だけを返します。

## 通信フローと契約

```text
JS: ハンドラー登録 → READY → Kotlin: ReplyProxy保持 → CONNECTED → JS: 接続済み
JS: API実行 → fetch → USER_FETCHED → Kotlin: JSON検証 → Compose・Debug Logcat
Kotlin: ボタン → NFC_DETECTED → ReplyProxy.postMessage → JS: NativeBridge.onmessage
  → CustomEvent("native:nfcDetected") → 別のaddEventListener → タグID・発火回数表示
```

サンプルWebViewでは`addWebMessageListener`と`JavaScriptReplyProxy.postMessage`を使います。Cordovaの通信APIは起動元でのみ使い、サンプルHTMLへは導入していません。

全メッセージは`{type, payload, session}`です。元の`type`・`payload`に、古いページの結果を除外するための`session`を追加しています。これは秘密情報や呼び出し元認証ではありません。

| 方向 | type | payload | 処理 |
| --- | --- | --- | --- |
| JS → Kotlin | READY | `{}` | 現在のページのReplyProxyを保持 |
| Kotlin → JS | CONNECTED | `{}` | READYの受付確認。Web側も実際の接続成立を表示 |
| JS → Kotlin | USER_FETCHED | `{id: 1, name: "Leanne Graham"}` | Composeに表示、Debug時だけ値をLogcatへ出力 |
| Kotlin → JS | NFC_DETECTED | `{tagId: "ABC123"}` | 疑似イベントを通知。機器操作は行わない |

ローカル文書は`https://appassets.androidplatform.net/assets/index.html?session=<UUID>`です。配信元・パスは指定の固定HTMLのまま、再読み込みのたびにクエリのページ識別子を更新します。業務引数や認証情報はURLへ入れません。

## ファイルと責務

| 変更・追加ファイル | 内容 |
| --- | --- |
| [WebViewSampleScreen.kt](../native/feature/src/main/kotlin/dev/example/nativefeature/ui/webview/WebViewSampleScreen.kt) | Composeの仮UI、AndroidView、Lifecycleとの接続・解放 |
| [WebViewBridge.kt](../native/feature/src/main/kotlin/dev/example/nativefeature/webview/WebViewBridge.kt) | WebView生成、対応API確認、リスナー、受信検証、ReplyProxy・状態・寿命の管理 |
| [WebMessage.kt](../native/feature/src/main/kotlin/dev/example/nativefeature/webview/WebMessage.kt) | UIから独立したJSON解析とネイティブ送信JSON生成 |
| [WebViewPolicy.kt](../native/feature/src/main/kotlin/dev/example/nativefeature/webview/WebViewPolicy.kt) | HTTPS・host・実効port・path・ページ識別子の許可判定 |
| [SampleWebViewClient.kt](../native/feature/src/main/kotlin/dev/example/nativefeature/webview/SampleWebViewClient.kt) | WebSettings、AssetLoader配信、ページ遷移・通信先制限、読み込み・TLS・レンダラーエラー処理 |
| [index.html](../native/feature/src/main/assets/index.html) | 手書きHTML・JS、公開API呼び出し、返信受信、CustomEvent、CSP |
| [FeatureDestination.kt](../native/feature/src/main/kotlin/dev/example/nativefeature/navigation/FeatureDestination.kt)・[FeatureNavHost.kt](../native/feature/src/main/kotlin/dev/example/nativefeature/navigation/FeatureNavHost.kt)・[FeatureLaunchRequest.kt](../native/feature/src/main/kotlin/dev/example/nativefeature/launch/FeatureLaunchRequest.kt) | 既存のMainActivityからsample.webviewを開く型付きルート |
| [Cordovaのindex.html](../CordovaComposeApp/www/index.html)・[index.js](../CordovaComposeApp/www/js/index.js) | 起動ボタンと要求の追加 |
| [プラグインJS](../kotlin-plugin/www/kotlin-plugin.js) | 検証用処理IDのJSDoc更新 |
| [WebMessageCodecTest.kt](../native/feature/src/test/kotlin/dev/example/nativefeature/webview/WebMessageCodecTest.kt)・[WebViewPolicyTest.kt](../native/feature/src/test/kotlin/dev/example/nativefeature/webview/WebViewPolicyTest.kt)・[FeatureLaunchRequestTest.kt](../native/feature/src/test/kotlin/dev/example/nativefeature/launch/FeatureLaunchRequestTest.kt) | メッセージの境界・URL偽装・処理IDの契約テスト |

README、仕様・コード概要・サービス起動・プラグイン・Vue連携・事前質問・レビュー・ログ一覧も、実装済み範囲を追記しています。

WebMessageサンプル追加時にはMainActivityの実装・Manifest・Gradleを変更していません。この文書の検証APKは既存のCordovaComposeApp/platforms/android/appであり、native/appはありません。後続で追加した[開発専用dev-app](FEATURE_DEVELOPMENT.md)からも同じ画面を起動できます。既存のホストにINTERNET権限、featureにWebKit依存が設定済みです。

## 接続とライフサイクル

- 生成はAndroidViewのfactoryで一度だけ行い、再コンポジションからloadUrlを呼びません。
- 対応APIは`WEB_MESSAGE_LISTENER`で確認します。非対応時はUIへ表示し、送信を無効にします。別方式へのフォールバックはありません。
- HTML読み込み前にリスナーを登録します。メインフレーム・許可origin・文字列・現在のsessionを検証します。
- READY前はNFCボタンを無効にします。受信結果は、現在のREADYで保持したReplyProxyからのメッセージだけを受け付けます。
- ネイティブの再読み込み、JavaScriptのlocation.reloadのどちらも古いProxyを解放し、新しいsessionを使います。onPageFinishedでは接続状態を初期化しません。
- Activityの再生成・プロセス終了後はルートをNavigationから復元し、新しいWebView・接続を作ります。サンプルの取得結果・回数は復元せず、実NFC処理の再実行もありません。
- ON_RESUME／ON_PAUSEをWebViewへ伝え、バックグラウンド中はネイティブ送信を無効にします。全WebViewに影響するpauseTimersは使いません。
- 画面の破棄時はリスナー・Proxyを解放し、WebViewをdestroyします。破棄後の送信を拒否します。レンダラーが終了した場合は画面を閉じて開き直します。

## 入力・表示・通信の制限

- ブリッジ許可originは`https://appassets.androidplatform.net`だけ。画面は現在のsessionを持つ固定index.htmlだけを許可します。
- 外部サブリソースは指定の検証APIへのGETだけを許可します。WebのfetchはCookieなし、リダイレクト禁止、10秒タイムアウトです。
- 不要なfile／contentアクセス、混在コンテンツ、位置情報、ポップアップを無効にします。TLSエラーでproceedしません。
- 文字列表示はtextContent・Compose Textを使います。CSPはインラインJSのSHA-256を指定し、未知のscript・frame・object・フォーム送信等を禁止します。
- 受信は最大16,384 UTF-16コード単位、JSONの深さ8、毎秒30メッセージまで。ユーザーIDは正のInt、名前は非空・256文字以下・制御文字なし。未知type・逆方向type・不正JSON・バイナリを拒否します。
- 不正入力は本文をログへ出さず、ページあたり最大3件の拒否ログに制限します。ユーザーの値は公開サンプルの検証用途でDebugにだけ出し、Releaseでは出しません。
- 許可origin内で実行を許したコード自体が侵害された場合を、sessionでは防げません。Vue導入時も、読み込むコード・CSP・同梱資源を管理する必要があります。

### HTMLを編集するとき

インラインJSを変更したら、CSPのハッシュも更新します。元の改行を含めたscriptタグ内のバイト列が対象です。

```sh
python3 - <<'PY'
from pathlib import Path
import base64
import hashlib
import re
p = Path('native/feature/src/main/assets/index.html')
html = p.read_text()
script = re.search(r'<script>(.*?)</script>', html, re.S).group(1)
digest = base64.b64encode(hashlib.sha256(script.encode()).digest()).decode()
p.write_text(re.sub(r"sha256-[A-Za-z0-9+/=]+", 'sha256-' + digest, html))
PY
```

## ビルド・検証

```sh
npm run native:build
npm run test:unit
npm run check:android
npm run cordova:build
npm run cordova:build -- --release
npm run docs:kdoc
```

実行結果とエミュレーター確認の詳細は[証跡一覧](logs/webview-bridge/README.md)へまとめています。ReleaseのCordovaビルド成果物はAABです。

## 残っている範囲

既存Vueのソース・ビルド・認証方式が未提供のため、Vueの導入、ルーター、成果物同期、実サービスのクライアントAPIは未実装です。[Web側への質問](WEB_TEAM_QUESTIONS.md)を先に確認します。現在の許可パス・CSPをそのままVueのchunkやルートに流用しません。

ゴルフ場ID・ホルダー番号の実際の読み取り、カメラ、NFC書き込み、本人確認、業務結果の起動元への返却、外部アプリ向け公開入口は実装していません。MainActivityはexported=falseを維持しています。

エミュレーター以外の実機、WebMessage非対応の古いWebView、レンダラー終了時の復旧表示は別途確認が必要です。公開テストAPIの可用性・CORS・端末の証明書環境にも依存します。API本文のサイズ確認は取得後に行うため、任意の外部サーバーから巨大な応答を受ける用途には展開しません。
