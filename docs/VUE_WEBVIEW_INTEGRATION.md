# Vueの画面切り替えとAndroid WebViewへの配布方針

[ドキュメント一覧](README.md) · [起動・サービス仕様](CLIENT_API_SPEC.md) · [WebMessageのセキュリティ条件](WEBVIEW_BRIDGE_REVIEW.md)

更新日: 2026-10-25。WebMessageを使うWebView案を前提に、Vueのルーティング、既存フロントエンドの再利用、ビルド成果物の取り込み方針をまとめます。Vueとビルド連携は未実装です。前段の[手書きHTMLの通信サンプル](WEBVIEW_BRIDGE_SAMPLE.md)は実装済みです。

## 2つのWebViewの役割

| 配置 | 表示・実行するもの | Kotlinとの接続 |
| --- | --- | --- |
| 起動元のCordova WebView | 既存のVueフロントエンド | 既存CordovaPluginを呼び、IntentでfeatureのMainActivityを起動する |
| featureのMainActivity内の標準WebView | サービス画面用にビルドしたVue | NativeBridge.postMessageとJavaScriptReplyProxy.postMessageで通信する |

別アプリから起動する場合も、受信・検証した要求を起動先のサービス画面へ渡す方針です。外部向けの入口・許可する呼び出し元・結果返却契約は[起動仕様](CLIENT_API_SPEC.md#activityの起動要件)に従って別途確定します。

2つのWebViewは別のJavaScript実行環境です。Vueインスタンス、Router、Store、イベントリスナー、実行中のJS、ログイン状態がそのまま移動することはありません。共有するのは画面・業務ロジックのソースで、起動に必要なデータは定義した要求として渡します。

## index.html内の画面切り替え

`index.html`をVue SPAの入口にし、Vue Routerで表示するコンポーネントを切り替えます。Kotlin側で画面ごとにHTMLを読み直す構成にはしません。

起動先WebViewでは、ハッシュルーティングを基本にします。

```text
https://appassets.androidplatform.net/assets/index.html#/golf/read
https://appassets.androidplatform.net/assets/index.html#/golf/result
```

上記パスは画面構成の例です。現在の確定業務はゴルフ場ID・ホルダー番号の読み取りで、結果画面の個別要件は未確定です。

Vue Routerの`router.push()`と`RouterView`で表示を切り替え、初期のサービス画面選択には`router.replace()`を使う方針です。ハッシュ以外の読み込み先は同じindex.htmlのままなので、通常の画面切り替えではWebViewを再生成・reloadせず、ブリッジを接続したまま使えます。[Vue Routerの履歴モード](https://router.vuejs.org/guide/essentials/history-mode.html)

Vue Router 4以降なら`createWebHashHistory()`、Vue Router 3なら`mode: 'hash'`に相当します。実際のVue・Routerの版に合わせて設定し、このためだけに既存の版を更新しません。

HTML5 History方式は、直接アクセス・reload時にURLをindex.htmlへ解決する仕組みが必要になります。今回の固定assets配信では、ハッシュ方式を使ってその追加実装を抑えます。

### Kotlinから初期画面を選ぶ流れ

1. ActivityがIntentの処理ID・argsと呼び出し元を検証する。
2. 固定のindex.htmlを読み込み、Vueとブリッジの受信ハンドラーを初期化する。
3. JSがREADYを送り、KotlinがそのページのReplyProxyを取得する。
4. Kotlinが検証済みの起動要求をJSONメッセージでJSへ渡す。
5. Vueが処理IDを既知のルート名へ対応付け、`router.replace()`でサービス画面を表示する。

例えば、起動要求を伝えるメッセージは次のような形を想定します。`START_SERVICE`は設計用の仮称で、実装済みのメッセージ種別ではありません。

```json
{
  "type": "START_SERVICE",
  "payload": {
    "id": "golf-change.read-holder",
    "args": {}
  }
}
```

`type`はブリッジ上のメッセージの種類、`payload.id`は業務処理の識別子です。取得するゴルフ場ID・ホルダー番号とは区別します。

READY前の起動要求は送信待ちとして扱い、接続後に渡します。Activity再利用時も同じ検証を行い、有効な接続があればメッセージで要求を渡します。多重起動・取消・要求の相関管理はAPI契約で確定します。

ルートとサービスIDはアプリ内の対応表で結び付けます。呼び出し元から任意のURL・JSコード・Vueコンポーネント名を指定させません。業務引数や読み取り結果をURLへ埋め込まず、JSON通信とVueの状態で扱います。

Web側の戻る操作はVue Routerで管理します。Androidの戻る操作は、サービス画面内の履歴があれば戻り、起動先の初期画面ならActivityを閉じる方針です。取消・結果返却との関係は、呼び出し契約に含めます。

## CordovaのVueフロントエンドを持ってくる方法

既存Vueプロジェクトのソースを再利用し、そのプロジェクトのビルド成果物をAndroidへ同梱します。実行中のCordovaからJSを取り出したり、.vueファイルを直接WebViewへ渡したりする仕組みは想定しません。

```text
既存Vueのソース・依存固定ファイル
  → 既存のビルドツールでWebView向けにビルド
  → dist等のHTML・JS・CSS・画像・遅延ロード用chunk
  → featureのAndroid assetsへ組み込む
  → WebViewAssetLoaderで配信
```

一般的な成果物の例です。出力名は既存のビルド定義に合わせます。

```text
dist/
├── index.html
└── assets/
    ├── app-<hash>.js
    ├── app-<hash>.css
    └── <画面別chunk・画像等>
```

index.htmlだけでなく、参照するファイルを相対関係ごと一式で取り込みます。Vue・Routerも同梱し、画面表示のためにCDN・開発サーバーからスクリプトを取得する構成にはしません。

既存がViteなら、本番ビルドと相対base（例: `base: './'`）を使う構成を検討します。既存がVue CLI等なら、そのツールの同等設定に合わせます。Viteへ移行すること自体は今回の前提にしません。[Viteの本番ビルドとbase設定](https://vite.dev/guide/build.html)

### Androidへの取り込み先

Vue導入時は、生成物を`feature/build/`内の専用ディレクトリへ同期し、そのディレクトリをfeatureのassets source setへ登録するビルド連携を推奨します。管理対象外のassetsを削除せず、同期先はVue生成物の専用領域に限定します。

```text
Vueのdist/
  → native/feature/build/generated/vueAssets/
      ├── index.html
      └── assets/...
  → APKへassetsとしてパッケージ
```

AssetLoaderの`/assets/`をAPKのassetsに対応付けた場合、入口URLは従来案どおり`https://appassets.androidplatform.net/assets/index.html`です。上の構成ならJSは`/assets/assets/app-<hash>.js`のようなURLになります。[WebViewAssetLoaderによる配信](https://developer.android.com/develop/ui/views/layout/webapps/load-local-content)

前段の単体通信サンプルは、手書きの`feature/src/main/assets/index.html`で検証できます。Vueへ移行するときは、この入口をビルド成果物へ置き換えます。同じassetsパスに手書きHTMLと生成HTMLを重複配置しません。HTML・JSを単一ファイルへ集約する制約も、Vue導入時には成果物一式を配布する方針へ変更します。

起動元のCordova向け成果物は、既存の管理方法に従ってCordovaComposeApp/wwwへ配置し、Cordova prepareでplatformへ反映します。feature向けの配布物とは出力先を分けます。platforms配下や、実行中のCordova WebViewからファイルを抽出する工程にはしません。[Cordova prepare](https://cordova.apache.org/docs/en/latest/reference/cordova-cli/#cordova-prepare-command)

Vue生成物の同期・assets登録はfeature側で完結させ、native単独・Cordovaホストの両方から同じ定義を使う方針です。具体的なタスク・scriptsは、Vueソースとビルド環境を確認した後に追加します。

## 共通のVueコードと通信アダプター

画面からは共通のクライアントAPIを呼び、実行環境ごとの差を通信アダプターへ集約します。

| ビルド先 | 再利用する部分 | 環境に合わせる部分 |
| --- | --- | --- |
| Cordova | Vueコンポーネント・画面ロジック・APIの型 | Cordovaの初期化と既存PluginによるActivity起動 |
| featureの標準WebView | 必要なサービス画面・画面ロジック・APIの型 | NativeBridgeによる要求送信とReplyProxyからの結果・イベント受信 |

既存JSが`cordova.exec`・CordovaPlugin・devicereadyへ直接依存している箇所は、標準WebView向けにそのまま実行できません。使用箇所を洗い出し、クライアントAPIのアダプターへ切り出します。cordova.jsをコピーするだけで標準WebViewがCordovaの実行環境になるものではありません。

同じVueソースから配布先ごとに必要なエントリーとアダプターを選ぶ方針です。ビルド名・ソース共有方法・共通パッケージの境界は、実際のフロントエンド構成に合わせます。業務APIの正式なメソッド名・メッセージ契約は引き続き未確定です。

## セキュリティ・状態管理の適用

- 初期HTMLのscheme・host・実効port・pathを限定し、Vue Router用のfragmentとファイルの読み込み先を区別する。Vueの遷移のために外部ページの読み込み許可を広げない。
- JS・CSS・画像・遅延chunkは、同梱した成果物の配信用パスだけを必要に応じて許可する。API通信用のoriginとブリッジの注入先originも区別する。
- 本番用にコンパイルしたVueを使い、外部データをVueテンプレートとしてコンパイルしない。文字列表示には補間等を使い、外部データの`v-html`表示は避ける。
- CSPは成果物のJS・CSSに合わせる。ビルド済みのJSを使うためにunsafe-eval等を許可する前提にはしない。
- Routerによる画面切り替えではWebView・ブリッジを作り直さない。画面固有のイベント購読はVueコンポーネントの寿命で解除する。実際のreload・破棄ではProxyと接続状態を初期化する。
- バックエンドAPIのbase URL・CORS・Cookie等を配布先ごとに確認する。相対`/api`や開発用proxyの設定が、APK内でも同じサーバーを指すと仮定しない。
- 2つのWebView間で認証・Cookie・Storeが引き継がれると仮定しない。共有する必要がある状態は受け渡し契約を決める。認証情報・個人情報をURLへ渡さない。
- フロントエンドへ埋め込む設定はアプリから読めるものとして扱い、秘密鍵・秘密のAPIキー等を含めない。[Viteの環境変数](https://vite.dev/guide/env-and-mode.html)

他のorigin検証・メッセージ検証・ログ出力・ファイルアクセス・SSL・ライフサイクルの条件は[セキュリティレビュー](WEBVIEW_BRIDGE_REVIEW.md)を適用します。

## 現状と次に確認する情報

Web担当へ共有する具体的な質問と確認順序は[WEB_TEAM_QUESTIONS.md](WEB_TEAM_QUESTIONS.md)にまとめています。

現在のリポジトリにはVueソース、Vue Router、Vite等のビルド定義がありません。Cordovaのwwwは検証用の手書きHTML・JSです。実際のフロントエンドの配置先、Vueの版、ビルド定義、依存するCordovaPluginの一覧を確認する必要があります。

既存Vueの再利用範囲、2つの配布物の生成方法、認証の受け渡し、サービス結果の正式な契約を確定してから実装します。Vueの取り込み・assets同期・ルーティングは未実装です。WebMessageサンプルでは現在の固定ページだけを許可しているため、Vue導入時には同梱chunk・ハッシュルートの許可とCSPを成果物に合わせて更新します。
