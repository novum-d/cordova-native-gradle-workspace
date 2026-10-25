# Module feature

Compose画面とUI関連の依存を管理するAndroid Libraryです。
dataを参照し、Cordova APKとnative単独のビルドで同じソースを使います。

MainActivityはKotlinPluginのIntentから処理ID（String）と任意のargs（JsonObject）を受け取り、
Navigationへ一度だけ渡します。対応する処理IDはhome・nfc.read・nfc.write・camera.captureです。
画面・引数の復元はNavigationと各画面のViewModelのSavedStateHandleが担当し、
Activity側には起動要求JSONの保存キーや独自のonSaveInstanceStateを持ちません。
NFC・Cameraの機器操作は未実装で、各画面は画面名と受け取った引数を中央に表示します。

# Package dev.example.nativefeature

起動Intentの受け取りと、一つのNavHostへの要求の受け渡しを担当します。

# Package dev.example.nativefeature.launch

Web・Intent境界の要求JSONを型へ変換し、既知の処理IDを型付き画面へ解決します。

# Package dev.example.nativefeature.navigation

共通NavHost、型付き画面、任意JSON引数の保存・復元を担当します。

# Package dev.example.nativefeature.nfc

NFC読み取り・書き込みの画面登録、各画面のViewModel、最小限の表示を担当します。

# Package dev.example.nativefeature.camera

Camera撮影の画面登録、ViewModel、最小限の表示を担当します。

# Package dev.example.nativefeature.home

確認用のhome処理IDで開くホーム画面を提供します。

# Package dev.example.nativefeature.common

機能画面共通の、画面名・処理ID・argsを中央に表示するレイアウトを提供します。
