# WebMessageによるWebView双方向通信サンプルの設計レビュー

[ドキュメント一覧](README.md) · [全体の起動・サービス仕様](CLIENT_API_SPEC.md)

確認日: 2026-10-25。提示された実装案を、現在のリポジトリと公式API仕様に照らしてレビューしました。以下は実装前のレビュー記録です。採用した対策・実装・検証結果は[通信サンプル](WEBVIEW_BRIDGE_SAMPLE.md)を参照してください。

レビュー後、この通信方式と対策を実装方針として[全体仕様](CLIENT_API_SPEC.md)へ反映しました。Vue導入時の単一index.htmlでの画面切り替え、生成JSの配布、CSP・配信パスの扱いは[Vue連携方針](VUE_WEBVIEW_INTEGRATION.md)を参照してください。以下にはレビュー時点の記録を保持しています。後続のサンプル実装では、ページ識別子・CONNECTED受付確認も追加しました。

## 結論

`addWebMessageListener()`・`JavaScriptReplyProxy.postMessage()`・`WebViewAssetLoader`の組み合わせは、今回の文字列JSONによる双方向通信の検証に適しています。許可originの限定、メインフレーム限定、READYの握手、対応機能の確認、古いReplyProxyの破棄も妥当です。

主な不足は、WebViewが読み込めるページ・通信先の制限、XSSを防ぐ表示方法、メッセージの内容・量の検証、ページ切り替え時の状態管理です。APIの選定だけで安全性を保証できるものではありません。現時点ではコードが提示されていないため、以下は対策の記載不足と実装時のリスクであり、既存コードに脆弱性が存在するという判定ではありません。

## 優先して追記する条件

### 1. 外部データをHTMLとして解釈させない

APIレスポンス、ユーザー名、タグID、エラー文字列の表示には`textContent`を使います。レスポンスを整形表示する場合も`JSON.stringify()`の結果を`pre`等の`textContent`へ設定します。`innerHTML`への代入やHTML文字列への埋め込みは避けます。

許可されたoriginのページ内でXSSが成立すると、注入されたコードもNativeBridgeへ送信できます。`allowedOriginRules`と`isMainFrame`は、許可ページ内の悪意あるスクリプトを識別する仕組みではありません。[Androidのブリッジのリスク](https://developer.android.com/privacy-and-security/risks/insecure-webview-native-bridges)、[textContentの説明](https://developer.mozilla.org/en-US/docs/Web/API/Node/textContent)

HTMLにはCSPを設け、実行できるスクリプトと`fetch`先を絞る方針を追加します。通信先は検証APIのHTTPS originに限定し、不要なframe・object・フォーム送信・base URL変更も制限します。

HTML内にJavaScriptを置く制約を維持する場合、`script-src 'self'`だけではインラインscriptは実行できません。固定scriptのハッシュをCSPへ指定する方式を使うか、別のassets内JSへ分ける制約変更を合意します。安易に`unsafe-inline`・`unsafe-eval`を許可する前提にはしません。[script-src](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Content-Security-Policy/script-src)、[connect-src](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Content-Security-Policy/connect-src)

### 2. ブリッジの許可originとページの読み込み制限を分ける

ブリッジの許可は`https://appassets.androidplatform.net`だけとし、受信時にもscheme・host・実効portを検証します。URL文字列の前方一致や、メッセージのpayloadにある自己申告originでは判定しません。`isMainFrame == true`もすべての受信で確認します。

`allowedOriginRules`は注入先originの制御です。URLのpathを限定せず、WebViewの外部ページ読み込みそのものを禁止する機能でもありません。[addWebMessageListenerの仕様](https://developer.android.com/reference/androidx/webkit/WebViewCompat#addWebMessageListener(android.webkit.WebView,java.lang.String,java.util.Set,androidx.webkit.WebViewCompat.WebMessageListener))

このサンプルは、トップページを固定の`/assets/index.html`に限定します。外部HTTPSページや`file:`・`content:`・`data:`・`javascript:`・`intent:`等への遷移、新規ウィンドウを受け付けない方針を明記します。外部アプリから渡されたURLをそのまま`loadUrl()`へ渡す拡張もしません。

初回の`loadUrl`、WebView内での遷移、リダイレクト、サブリソースは、それぞれ制御範囲が異なります。`shouldOverrideUrlLoading`だけで全リクエストを制御したつもりにせず、固定URL・CSP・必要に応じたリクエスト制御を組み合わせます。

AssetLoaderで解決できなかった`appassets.androidplatform.net`への要求は、このサンプルではエラー応答にして、想定外のパスをネットワークへ流さない構成を提案します。`shouldInterceptRequest()`が`null`を返す意味を区別してください。[AssetLoaderの仕様](https://developer.android.com/reference/androidx/webkit/WebViewAssetLoader)

### 3. JSONの構文以外も検証する

Kotlin側の受信は文字列メッセージに限定し、JSONオブジェクト、既知のtype、type別のpayloadを検証します。

| 方向 | type | 検証する内容 |
| --- | --- | --- |
| JS → Kotlin | READY | payloadの省略を許可するか、空オブジェクトに統一するかを決める。正しい初期化メッセージだけで接続状態を更新する |
| JS → Kotlin | USER_FETCHED | payloadがオブジェクト、idが整数、nameが文字列であること。長さ・サイズも制限する |
| Kotlin → JS | NFC_DETECTED | payload.tagIdが文字列であること。JS側でも解析・型チェックしてからCustomEventへ渡す |

JSから届いた`NFC_DETECTED`は、Kotlin由来のNFC検出として扱いません。未知type、非文字列メッセージ、不正JSON、過大なメッセージは破棄し、ログ出力もサイズ・頻度を制限します。今回の固定的なデータ量に合う簡単な上限で十分です。

JSから送られる`USER_FETCHED`はページ側が任意に生成できます。実際に指定APIを実行した証明にはならず、本人確認済み・認証済みの根拠として扱えません。同様にCustomEventもJSから生成できるので、今回は表示更新の確認として扱います。

### 4. ファイル・通信・デバッグ設定を明記する

今回のサンプル用WebViewでは、次の条件を提案します。

- `allowFileAccess`・`allowContentAccess`をfalseにする。file URL向けのアクセス緩和設定も有効にしない。
- HTTPSを使い、Mixed Contentを許可しない。SSL証明書エラー時の既定の拒否を維持し、`proceed()`で回避しない。
- CORSの制限は維持する。API通信のためにfile URLの権限拡大や証明書検証の回避をしない。
- `fetch`先を固定する。実データ・認証情報・NFC情報を検証APIへ送らない。
- LogcatへのID・名前の表示は検証用データに限定し、実サービスへ展開するときはデータを絞る。受信JSON全体や不正入力をそのままログへ出さない。
- WebViewのデバッグ公開は開発時に限定し、Releaseで有効にしない。

ホスト全体のネットワーク設定を変更する場合はCordova側への影響も確認します。WebView単体の設定で対応できる条件と区別し、不要な全体設定変更は避けます。[安全なローカルコンテンツの読み込み](https://developer.android.com/develop/ui/views/layout/webapps/load-local-content)、[WebSettings](https://developer.android.com/reference/android/webkit/WebSettings)、[SSLエラー時の扱い](https://developer.android.com/reference/android/webkit/WebViewClient#onReceivedSslError(android.webkit.WebView,android.webkit.SslErrorHandler,android.net.http.SslError))

## ライフサイクル・通信状態の実装上の注意

READYからReplyProxyを取得して保持する案は適切です。ReplyProxyは特定フレームのJSオブジェクトに対応するため、ページ再読み込み後の新しい接続へ自動的に置き換わる前提にしません。[JavaScriptReplyProxy](https://developer.android.com/reference/androidx/webkit/JavaScriptReplyProxy)

- 状態は未接続・接続済み・非対応・破棄済み程度にとどめます。WebViewとProxyは画面の寿命に合わせて管理し、長寿命のシングルトンや保存状態へ保持しません。
- Kotlinからreload・別ページの読み込みを開始する前に、READY状態とProxyを無効化します。ページ開始・エラー・レンダラ終了・破棄も切断の契機にします。
- READYはページのscript実行時に届くため、`onPageFinished`より先に来る場合があります。`onPageFinished`で一律に状態を初期化したり、毎回ブリッジを登録し直したりしない構成にします。
- 古い読み込みの完了処理で新しい接続状態を上書きしないようにします。読み込みとREADYの競合がある場合は、ページの世代を扱う小さな仕組みを検討します。
- WebView生成・リスナー登録・Proxy送信・破棄・Compose状態更新はUIスレッドで行います。AssetLoaderを呼ぶ`shouldInterceptRequest`は別スレッドなので、そこからCompose状態を直接変更しません。
- 再コンポジションではWebViewを再生成・再読み込みせず、AndroidViewの生成・解放に合わせて管理します。破棄時はProxyとコールバック参照を切り、登録したリスナーを解除してWebViewを解放します。
- `WEB_MESSAGE_LISTENER`非対応時は、ComposeとHTML双方でブリッジ不在を扱い、NFC送信を無効にします。StringメッセージだけならArrayBuffer用APIの対応確認は不要です。

上記APIとString版ReplyProxyの要件は、採用中のWebKit 1.12.1の配布ソースでも確認しました。実際の対応は端末のWebView実装にも依存するため、実機・エミュレーターではWebViewのバージョンと機能チェック結果を記録します。

## API通信・UI表示の補足

`fetch()`はHTTP 4xx・5xxでもレスポンスを返すため、`response.ok`を確認してからJSONを解析します。通信・JSON解析・型不正・タイムアウトを表示できるようにし、APIボタンの無効化を必ず解除します。タイムアウトにはAbortController等を使えます。

2026-10-25の確認時点では、指定URLはHTTP 200を返し、`Origin: https://appassets.androidplatform.net`を付けたGETに同じoriginの`Access-Control-Allow-Origin`が返りました。これは確認環境からのHTTP結果で、Android WebView内でのfetch成功を確認したものではありません。第三者APIの可用性・CORS設定は変化し得ます。

「CustomEventが発火した」ことは、受信ハンドラー内で更新するイベント回数や最終受信タグIDで確認できます。READY送信済みとKotlinでの受理は区別し、必要以上のプロトコル追加を避けて状態表示の意味を明確にします。

## 既存プロジェクトとの整合性

| 実装案の記載 | 現在のリポジトリ | 修正案 |
| --- | --- | --- |
| native/appがAPKホスト | nativeにはfeature・data・cordova-plugin-devがあり、APKホストはCordovaComposeApp/platforms/androidの生成app | native/appを新設せず、既存のCordovaホストを使う |
| featureへWebKit依存を追加する | Version CatalogのWebKit 1.12.1とfeatureのimplementationは既に存在する | 既存依存を再利用する。AGP・Kotlin・Composeの変更は不要 |
| INTERNET権限を追加する | 現在のCordovaホストに既に存在する | 生成Manifestを手書き変更せず、重複追加を避ける |
| 既存MainActivityにサンプルを追加する | 現在はIntentのid・argsを検証し、確認用home・golf-change.read-holderの画面を表示する | 既存の起動要求処理と暫定画面との共存方法を明記する |
| Cordovaの通信APIは使用しない | Activityの起動元としてCordovaPluginを使用中 | 禁止範囲はサンプル内のJS↔Kotlin通信とする。既存の起動経路は維持する |

サンプルの通信方式は、前段のCordovaPluginによるActivity起動と両立します。新しいWebViewへCordovaのブリッジを追加する必要はありません。別アプリ向けの入口・公開契約も別の課題として維持し、サンプル起動の便宜でexportedを変更することは提案しません。

全体仕様では通常のUIはWeb側が担当します。今回のCompose上の送信ボタン・受信内容表示は、その方針に対する検証用UIとして位置付けます。また、USER_FETCHED等の検証メッセージと、golf-change.read-holder等の業務処理IDは別の契約として扱います。

## 追加する確認項目

正常系の完了条件に加え、次を確認します。

| ケース | 期待結果 |
| --- | --- |
| 外部URL・別scheme・未許可のassetsページへの遷移 | サンプルWebViewで読み込まれず、通信可能状態にならない |
| 外部origin・サブフレームからの送信 | Kotlin側の状態・表示を更新しない |
| 名前・エラー等にHTMLタグやscriptに似た文字列を含める | 文字列として表示され、実行されない |
| JSON構文は正しいがpayloadの型が違う、過大な入力、方向の違うtype | 破棄され、クラッシュや無制限なログ出力を起こさない |
| READY前、reload直後、破棄後のNFC送信 | 送信されず、新しいREADY後だけ送信できる |
| reloadの連打、回転、ページ読み込み失敗 | 古いページの接続・受信内容を新しい接続として扱わない |
| オフライン・HTTPエラー・不正APIレスポンス | エラーを表示し、再試行可能になる |
| WEB_MESSAGE_LISTENER非対応 | 非対応を表示し、フォールバックなしでクラッシュしない |

この文書のレビュー時点では設計とHTTP応答を確認しました。後続の実装・ビルド・Android WebView内の通信検証は[通信サンプル](WEBVIEW_BRIDGE_SAMPLE.md)に記録します。
