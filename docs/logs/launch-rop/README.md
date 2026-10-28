# 起動要求のROP・try/catch置換の確認

確認日: 2026-10-28

[起動要求とエラー型の仕様](../../SERVICE_NAVIGATION.md)を参照してください。

## ビルドと静的検査

| 対象 | 結果 |
| --- | --- |
| native開発APK・プラグイン編集用AAR | Debugビルド成功 |
| Cordova APK | Debug／Releaseビルド成功 |
| featureの単体テスト | 両ホストのDebug／Releaseで各54件成功。起動要求の具体エラーと引数保持を確認する8件を追加 |
| dataの単体テスト | 両ホストのDebug／Releaseで各73件成功。下流の実装例外・JVM Error・取消でも、タグと要求を解放して同じ原因を再送出するテストを追加 |
| Android Lint | nativeのfeature・data・プラグイン編集用モジュールの両variant、開発APKのDebug、Cordova APKの両variantが成功 |
| detekt・整形チェック | 成功。既存のルール・閾値は変更なし |
| KDoc | nativeのHTML集約生成が成功 |

手書きのKotlinにtry・catch・finallyが残っていないことを検索で確認しました。検出・購読・テストの後始末はrunCatching直後のalsoで行い、その後のgetOrElseで元の失敗を再送出します。例外伝播を検査するテストのkotlin.runCatchingは維持しています。

## API 35エミュレーター

| 操作 | 確認結果 |
| --- | --- |
| 開発APKから構造化args付きのゴルフ要求を送る | 処理ID・日本語や予約文字を含むargsを表示し、NFC非搭載の案内を表示 |
| 未対応IDの要求を送る | UnsupportedIdをログへ記録し、ネイティブ画面を終了して開発ランチャーへ戻る |
| argsが配列の要求を送る | InvalidJsonをログへ記録し、ネイティブ画面を終了して開発ランチャーへ戻る |
| WebView通信画面を開いて疑似NFCイベントを送る | READY受付・接続完了、native送信回数1、Web側CustomEvent発火回数1を確認 |
| Cordovaのゴルフ読取ボタンを押す | cordova/exec・プラグイン・MainActivityを経由してネイティブ画面を表示。空のargsを保持 |
| Cordovaへ戻る | 成功コールバックの表示`Opened golf-change.read-holder.`を確認 |

確認中のAndroidRuntimeにFATAL EXCEPTIONはありませんでした。確認後にエミュレーターを終了し、adb接続一覧が空になったことを確認しています。
