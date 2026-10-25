# 画面起動・切り替え・復元のUI階層

[検証概要](../README.md)

API 35でandroid layoutを使って取得したJSONです。各画面の処理IDとargsはViewModelのSavedStateHandleから取得して表示しています。

| ファイル | 内容 |
| --- | --- |
| [camera-capture-back.json](camera-capture-back.json) | Camera撮影：画面の「戻る」でCordovaへ復帰した状態。 |
| [camera-capture-direct.json](camera-capture-direct.json) | Camera撮影：Webボタンから直接起動し、args={}をViewModelで受け取った状態。 |
| [camera-capture-empty.json](camera-capture-empty.json) | Camera撮影：空のargs={}を省略と区別して受け取った状態。 |
| [camera-capture-initial-home.json](camera-capture-initial-home.json) | Camera撮影：初回Intentをhomeで発行した、復元確認前の状態。 |
| [camera-capture-latest-request.json](camera-capture-latest-request.json) | Camera撮影：新Intentで受け取った最新画面・引数。 |
| [camera-capture-omitted.json](camera-capture-omitted.json) | Camera撮影：args省略をViewModelでnullとして受け取った状態。 |
| [camera-capture-portrait-restored.json](camera-capture-portrait-restored.json) | Camera撮影：元の回転設定に戻した後も最新画面・引数が保持される状態。 |
| [camera-capture-process-restored.json](camera-capture-process-restored.json) | Camera撮影：プロセス終了・再起動後、初回homeではなく最新画面・引数が復元された状態。 |
| [camera-capture-restored-back.json](camera-capture-restored-back.json) | Camera撮影：プロセス再生成後の画面からシステムバックでCordovaへ復帰した状態。 |
| [camera-capture-rotation-restored.json](camera-capture-rotation-restored.json) | Camera撮影：画面回転後に最新画面・引数をNavigationとSavedStateHandleから復元した状態。 |
| [camera-capture-structured.json](camera-capture-structured.json) | Camera撮影：日本語・URI予約文字・ネストを含むargsを受け取った状態。 |
| [direct-home.json](direct-home.json) | home処理IDによる確認用ホームの直接起動。 |
| [invalid-requests.json](invalid-requests.json) | 不正要求を拒否し、直前のCamera画面・argsが変わらない状態。 |
| [nfc-read-back.json](nfc-read-back.json) | NFC読み取り：画面の「戻る」でCordovaへ復帰した状態。 |
| [nfc-read-direct.json](nfc-read-direct.json) | NFC読み取り：Webボタンから直接起動し、args={}をViewModelで受け取った状態。 |
| [nfc-read-empty.json](nfc-read-empty.json) | NFC読み取り：空のargs={}を省略と区別して受け取った状態。 |
| [nfc-read-initial-home.json](nfc-read-initial-home.json) | NFC読み取り：初回Intentをhomeで発行した、復元確認前の状態。 |
| [nfc-read-latest-request.json](nfc-read-latest-request.json) | NFC読み取り：新Intentで受け取った最新画面・引数。 |
| [nfc-read-omitted.json](nfc-read-omitted.json) | NFC読み取り：args省略をViewModelでnullとして受け取った状態。 |
| [nfc-read-portrait-restored.json](nfc-read-portrait-restored.json) | NFC読み取り：元の回転設定に戻した後も最新画面・引数が保持される状態。 |
| [nfc-read-process-restored.json](nfc-read-process-restored.json) | NFC読み取り：プロセス終了・再起動後、初回homeではなく最新画面・引数が復元された状態。 |
| [nfc-read-restored-back.json](nfc-read-restored-back.json) | NFC読み取り：プロセス再生成後の画面からシステムバックでCordovaへ復帰した状態。 |
| [nfc-read-rotation-restored.json](nfc-read-rotation-restored.json) | NFC読み取り：画面回転後に最新画面・引数をNavigationとSavedStateHandleから復元した状態。 |
| [nfc-read-structured.json](nfc-read-structured.json) | NFC読み取り：日本語・URI予約文字・ネストを含むargsを受け取った状態。 |
| [nfc-write-back.json](nfc-write-back.json) | NFC書き込み：画面の「戻る」でCordovaへ復帰した状態。 |
| [nfc-write-direct.json](nfc-write-direct.json) | NFC書き込み：Webボタンから直接起動し、args={}をViewModelで受け取った状態。 |
| [nfc-write-empty.json](nfc-write-empty.json) | NFC書き込み：空のargs={}を省略と区別して受け取った状態。 |
| [nfc-write-initial-home.json](nfc-write-initial-home.json) | NFC書き込み：初回Intentをhomeで発行した、復元確認前の状態。 |
| [nfc-write-latest-request.json](nfc-write-latest-request.json) | NFC書き込み：新Intentで受け取った最新画面・引数。 |
| [nfc-write-omitted.json](nfc-write-omitted.json) | NFC書き込み：args省略をViewModelでnullとして受け取った状態。 |
| [nfc-write-portrait-restored.json](nfc-write-portrait-restored.json) | NFC書き込み：元の回転設定に戻した後も最新画面・引数が保持される状態。 |
| [nfc-write-process-restored.json](nfc-write-process-restored.json) | NFC書き込み：プロセス終了・再起動後、初回homeではなく最新画面・引数が復元された状態。 |
| [nfc-write-restored-back.json](nfc-write-restored-back.json) | NFC書き込み：プロセス再生成後の画面からシステムバックでCordovaへ復帰した状態。 |
| [nfc-write-rotation-restored.json](nfc-write-rotation-restored.json) | NFC書き込み：画面回転後に最新画面・引数をNavigationとSavedStateHandleから復元した状態。 |
| [nfc-write-structured.json](nfc-write-structured.json) | NFC書き込み：日本語・URI予約文字・ネストを含むargsを受け取った状態。 |
| [repeated-identical-request.json](repeated-identical-request.json) | 同じCamera処理ID・argsの再要求を受信した画面。 |
| [stack-cleared-back.json](stack-cleared-back.json) | 複数の要求で画面を切り替えた後、システムバック一度でCordovaへ戻った状態。 |
| [web-initial.json](web-initial.json) | APK導入・起動直後のWeb画面。 |
| [web-ready.json](web-ready.json) | 3つの機能とホームを開くWebボタン。 |
