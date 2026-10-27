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

# Package dev.example.nativefeature.navigation

共通NavHost、型付き画面、任意JSON引数の保存・復元を担当します。

# Package dev.example.nativefeature.services.golfchange

ゴルフ場釣り銭機サービスのNavigation、ゴルフ場ID・ホルダー番号読み取り画面とViewModel、
ゴルフ場番号・ホルダー番号の読み取りユースケース・ユースケース固有のエラー契約を提供します。
メモリマップと対象に紐づくRepositoryはdata/golfが管理します。
NFC書き込みは対象外です。本人確認（マイナンバー）は別サービスとして仕様確定後に追加します。

# Package dev.example.nativefeature.home

確認用のhome処理IDで開くホーム画面を提供します。

# Package dev.example.nativefeature.common

機能画面共通の、画面名・処理ID・argsを中央に表示するレイアウトを提供します。

# Package dev.example.nativefeature.nfc

ゴルフ以外の業務でも利用できる、汎用のタグ検出ユースケースと検出エラー契約を提供します。
