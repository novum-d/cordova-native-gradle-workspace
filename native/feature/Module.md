# Module feature

Compose画面とUI関連の依存を管理するAndroid Libraryです。
dataを参照し、Cordova APKとnative単独のビルドで同じソースを使います。

現在のMainActivityはkotlin-pluginから起動するComposeの動作確認用です。起動時に画面を構築し、ボタンを押すとクリック回数が更新されます。
Webからの要求はプラグインが受け付けます。業務画面・NFC処理は実装していません。

# Package dev.example.nativefeature

Composeの起動と状態更新を検証するActivityを提供します。
