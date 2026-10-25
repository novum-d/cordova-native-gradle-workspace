# サービス起動・引数・復元のUI階層

[検証概要](../README.md)

API 35でandroid layoutを使って取得したJSONです。

| ファイル | 内容 |
| --- | --- |
| [direct-home.json](direct-home.json) | 確認用home IDの画面。 |
| [golf-holder-read-back.json](golf-holder-read-back.json) | 読み取り画面の「戻る」でCordovaへ復帰。 |
| [golf-holder-read-direct.json](golf-holder-read-direct.json) | Webからの直接起動。args={}をSavedStateHandle経由で表示。 |
| [golf-holder-read-empty.json](golf-holder-read-empty.json) | args={}を省略と区別して受け取った状態。 |
| [golf-holder-read-initial-home.json](golf-holder-read-initial-home.json) | 最新要求の復元を確認する前の、初回home起動。 |
| [golf-holder-read-latest-request.json](golf-holder-read-latest-request.json) | 新Intentから受け取った最新のサービス要求。 |
| [golf-holder-read-omitted.json](golf-holder-read-omitted.json) | 省略したargsを未指定として受け取った状態。 |
| [golf-holder-read-portrait-restored.json](golf-holder-read-portrait-restored.json) | 元の回転設定に戻した後の最新画面・args。 |
| [golf-holder-read-process-restored.json](golf-holder-read-process-restored.json) | プロセス終了・再起動後、最新要求をNavigation・ViewModelから復元。 |
| [golf-holder-read-restored-back.json](golf-holder-read-restored-back.json) | 再生成後の画面からCordovaへ復帰。 |
| [golf-holder-read-rotation-restored.json](golf-holder-read-rotation-restored.json) | 回転後の最新サービス画面・argsの復元。 |
| [golf-holder-read-structured.json](golf-holder-read-structured.json) | 日本語・予約文字・ネストを含むargsの保持。 |
| [invalid-requests.json](invalid-requests.json) | 旧3機器名IDと不正入力の拒否後も保持された画面・args。 |
| [repeated-identical-request.json](repeated-identical-request.json) | 同じサービスID・argsの再受信。 |
| [stack-cleared-back.json](stack-cleared-back.json) | 複数の要求を受けた後、システムバック一度でCordovaへ復帰。 |
| [web-ready.json](web-ready.json) | サービス起動ボタンと確認用ホーム。 |
