# ICODE SLI読取の検証記録

[ログ一覧](../README.md) · [実装・制約](../../NFC_READING.md)

2026-10-27時点の記録です。タグ分類・座標読取と、ゴルフ場番号・ホルダー番号の両ユースケースを検証しています。ホルダー番号は合意した暫定方式として、14バイトを読み取り順・大文字・区切りなしの28桁16進Stringで返します。

| ファイル | 内容 |
| --- | --- |
| [results.json](results.json) | native・Cordovaの各variantのテスト件数と確認範囲 |
| [native-check.log](native-check.log) | data・featureのDebug／Release単体テスト、静的解析・整形、Android Lint、開発用APK、KDoc生成 |
| [cordova-check.log](cordova-check.log) | CordovaのDebug／Release APK・Android Lint、data・featureの両variant単体テスト、root check |

各ホストでdataは各variant 29件、featureは各variant 42件（うちホルダー番号7件）が成功しました。両ホスト・Debug／Release合計284件で、失敗・エラーは0件です。

SLI以外の除外、UID付きREADフレーム、必要ブロックだけの取得、範囲・応答不正、途中失敗、接続解放・キャンセル、LSBファースト数値、ホルダー番号の28桁表現・0x00／空白／上位ビットの保持、エラー型の発生元を確認しました。NFC通信は模擬接続であり、実端末・実タグでの成功確認ではありません。

両GradleルートはLibraryのbuild出力を共有します。切替時にクラス出力の衝突があったため、この記録のビルドではdata・featureをcleanし、Cordova側はappもcleanして実行しました。
