# Module feature

Compose画面とUI関連の依存を管理するAndroid Libraryです。
dataを参照し、Cordova APKとnative単独のビルドで同じソースを使います。

MainActivityはKotlinPluginのIntentから処理ID（String）と任意のargs（JsonObject）を受け取り、
Navigationへ一度だけ渡します。対応する処理IDはhome・golf-change.read-holderです。
画面・引数の復元はNavigationと各画面のViewModelのSavedStateHandleが担当し、
Activity側には起動要求JSONの保存キーや独自のonSaveInstanceStateを持ちません。
ゴルフ場番号は、ICODE SLIのブロック1 Byte 0・1をLSBファーストで読むユースケースを提供します。
ホルダー番号は、ブロック1 Byte 2〜ブロック4 Byte 3の14バイトを、
合意した暫定方式として読取順・大文字・区切りなしの28桁16進Stringで返します。
汎用のタグ検出とゴルフ用ユースケースを接続し、前面のゴルフ画面で2項目の番号・失敗を表示します。
UI・ViewModelはRepositoryを直接呼びません。Webへの業務結果返却は未実装です。

# Package dev.example.nativefeature

起動Intentの受け取りと、一つのNavHostへの要求の受け渡しを担当します。

# Package dev.example.nativefeature.launch

Web・Intent境界の要求JSONを型へ変換し、既知の処理IDを型付き画面へ解決します。
サービス共通の起動境界として配置し、業務ユースケースの実行やUIの描画は担当しません。

# Package dev.example.nativefeature.usecase.launch

ResolveFeatureLaunchUseCaseが、未指定の起動要求・JSON形式不正・未対応IDをResultの具体的なエラー型で返します。
要求JSONの解析と画面解決をandThenでつなぎ、成功・失敗時のUI操作はMainActivityへ任せます。

# Package dev.example.nativefeature.err.launch

ResolveFeatureLaunchErrが起動要求の受信・解析・画面選択の失敗を分類します。
取消・JVM Error・予期しない例外を入力エラーへ変換しません。

# Package dev.example.nativefeature.navigation

FeatureNavHost、型付き画面、任意JSON引数の保存・復元を担当します。
FeatureDestinationの型名とシリアル名は、保存済み画面の復元に使う契約として維持します。

# Package dev.example.nativefeature.navigation.golfchange

ゴルフ場釣り銭機サービスの画面登録と、前面表示中の画面状態の購読を担当します。
起動IDのgolf-change.read-holderを、従来と同じ型付きルートへ対応付けます。

# Package dev.example.nativefeature.ui.golfchange

GolfCardReadScreenとGolfCardReadViewModelが、ゴルフ場番号・ホルダー番号の取得結果を表示します。
ViewModelは業務ユースケースを購読し、Repository・Android Tagへ直接依存しません。

# Package dev.example.nativefeature.ui.home

確認用のhome処理IDで開くホーム画面を提供します。

# Package dev.example.nativefeature.ui.common

FeatureRequestLayoutが、画面名・処理ID・argsと各機能の内容を中央に表示する共通レイアウトを提供します。

# Package dev.example.nativefeature.ui.webview

WebMessage双方向通信を確認するComposeのサンプル画面を提供します。

# Package dev.example.nativefeature.usecase.nfc

ゴルフ以外の業務でも利用できる汎用のタグ検出ユースケースを提供します。

# Package dev.example.nativefeature.usecase.golfchange

同じ対象の読取ユースケースを生成するFactory、2項目を直列に取得する業務ユースケース、
ゴルフ場番号・ホルダー番号を取得する個別ユースケースを提供します。
メモリマップ・バイト列の解釈・対象に紐づくRepositoryはdata/repository/golfが管理します。
ユースケースはInt・Stringと具体的な失敗を受け取り、ビット演算は行いません。NFC書き込みは対象外です。

# Package dev.example.nativefeature.err.nfc

汎用タグ検出ユースケース固有のエラー契約を定義します。

# Package dev.example.nativefeature.err.golfchange

ゴルフ場番号・ホルダー番号の読取ユースケース固有のエラー契約を定義します。
具体的な原因は各ユースケースの階層内に保ち、dataの共通契約も実装します。

# Package dev.example.nativefeature.model.golfchange

ユースケースが返すGolfCardReadEventと、画面が使うGolfCardReadUiStateを別ファイルで定義します。
読取の実行やUIの描画は担当しません。

# Package dev.example.nativefeature.logging

StartupによるTimberの初期化と、実際のホストAPKに応じたログ出力の設定を提供します。

# Package dev.example.nativefeature.webview

WebViewの接続・解放、WebMessageの変換、URLと読み込みの制御を提供します。
Compose画面はui.webviewへ置き、通信の管理と表示の責務を分けます。
