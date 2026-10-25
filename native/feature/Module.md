# Module feature

Compose画面とUI関連の依存を管理するAndroid Libraryです。
dataを参照し、Cordova APKとnative単独のビルドで同じソースを使います。

MainActivityはKotlinPluginのIntentから処理ID（String）と任意のargs（JsonObject）を受け取り、
Navigationへ一度だけ渡します。対応する処理IDはhome・golf-change.read-holderです。
画面・引数の復元はNavigationと各画面のViewModelのSavedStateHandleが担当し、
Activity側には起動要求JSONの保存キーや独自のonSaveInstanceStateを持ちません。
ゴルフ場ID・ホルダー番号の実際の読み取りと結果返却は未実装で、各画面は画面名と受け取った引数を中央に表示します。

# Package dev.example.nativefeature

起動Intentの受け取りと、一つのNavHostへの要求の受け渡しを担当します。

# Package dev.example.nativefeature.launch

Web・Intent境界の要求JSONを型へ変換し、既知の処理IDを型付き画面へ解決します。

# Package dev.example.nativefeature.navigation

共通NavHost、型付き画面、任意JSON引数の保存・復元を担当します。

# Package dev.example.nativefeature.services.golfchange

ゴルフ場釣り銭機サービスのNavigation、ゴルフ場ID・ホルダー番号読み取り画面とViewModelを提供します。
NFC書き込みは対象外です。本人確認（マイナンバー）は別サービスとして仕様確定後に追加します。

# Package dev.example.nativefeature.home

確認用のhome処理IDで開くホーム画面を提供します。

# Package dev.example.nativefeature.common

機能画面共通の、画面名・処理ID・argsを中央に表示するレイアウトを提供します。
