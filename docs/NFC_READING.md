# ICODE SLIの判定・座標読み取り

[ドキュメント一覧](README.md) · [サービス仕様](CLIENT_API_SPEC.md)

## 実装の範囲

`~/repos/NFCApp`のNFC-V Single Block READと、応答検査・接続解放を参考に実装しています。読み取る製品はICODE SLIだけです。FeliCa、MIFARE、SLIXなどの読み取りや、書き込み・認証は追加していません。

| 配置 | 責務 |
| --- | --- |
| [data/nfc](../native/data/src/main/kotlin/dev/example/nativedata/nfc/) | Android Tagとの接続、SLI判定、範囲検査、READ・応答検査、連続ByteArrayの取得、共通の失敗契約 |
| [GolfCardField](../native/feature/src/main/kotlin/dev/example/nativefeature/services/golfchange/GolfCardField.kt) | 業務項目と固定座標の対応 |
| [ReadGolfCourseNumberUseCase](../native/feature/src/main/kotlin/dev/example/nativefeature/services/golfchange/ReadGolfCourseNumberUseCase.kt) | ゴルフ場番号の取得と符号なし数値への解釈 |
| [ReadGolfHolderNumberUseCase](../native/feature/src/main/kotlin/dev/example/nativefeature/services/golfchange/ReadGolfHolderNumberUseCase.kt) | ホルダー番号の14バイト取得と、28桁の16進文字列への表現 |

今回の入口は、検出済みのタグを渡す`suspend operator fun invoke`です。Reader Modeの開始・停止、画面の状態管理、Webへの業務結果返却は接続していません。既存のゴルフ画面は引数表示のままです。NFCの通常権限と、NFCを必須としないuses-featureをfeatureのManifestへ追加しています。

## タグ判定

[classifyNfcTag](../native/data/src/main/kotlin/dev/example/nativedata/nfc/NfcTagType.kt)は、通信せずに次を確認し、`ICODE_SLI`または`OTHER`を返します。

- 技術一覧に`android.nfc.tech.NfcV`がある。
- UIDが8バイトで、UID7＝E0、UID6＝04、UID5＝01。
- UID4のbit 4・bit 3がどちらも0。

配列はUID0からの送信順です。表示用のUID文字列へ変換・反転してから渡しません。メーカーと製品種別だけではSLIXやSLIX2も一致するため、ISO 15693の1始まりのbit 37・36で区別します。[SLIX2データシート §9.2.1 Table 8](https://www.nxp.com/docs/en/data-sheet/SL2S2602.pdf#page=12)の区別を参照しています。UIDによる製品分類であり、真正性の認証ではありません。

`OTHER`は未対応という判断です。FeliCaやMIFAREの種類を細かく識別したり、各規格のコマンドで試し読みしたりしません。

## 座標指定

位置は`NfcMemoryPosition(blockNumber, byteOffset)`、範囲は`NfcMemoryRange(start, endInclusive)`で指定します。開始・終了とも0始まりで、終了バイトを含みます。

ICODE SLIのユーザー領域はブロック0〜27・各4バイトです。負数、28以上、バイト位置4以上、開始と終了の逆転を通信前に拒否します。ブロック番号を1バイトに格納できても、SLIの領域内とは判断しません。[SLIデータシート §3.2](https://www.nxp.com/docs/en/data-sheet/058031.pdf#page=7)

例えば`(1, 2)`〜`(2, 1)`なら、ブロック1・2を読み、各ブロックのデータから4バイトを返します。タグ全体や管理領域は読みません。2次元配列や2バイト単位の項目へ分割せず、要求順の連続ByteArrayとして返します。

## 通信・解放・取消

`IcodeSliMemoryReader`はDispatchers.IOで実行し、1回の呼び出しごとに接続・切断します。複数ユースケース間で接続や読取済みブロックをキャッシュしません。

フレームは`22 20 <UID0〜7> <block>`です。高速・Addressed・OptionなしのRead Single Blockで対象UIDを指定します。NFCAppの`02 20 <block>`から、対象指定を追加した点が異なります。CRCはAndroidに任せ、Single Block READを必要ブロックへ順番に送ります。[Android NfcV](https://developer.android.com/reference/android/nfc/tech/NfcV)

正常応答はFlags＋4バイトの5バイトです。Error flagがある場合はFlags＋エラーコードの2バイトを要求し、コードを符号なし値で保持します。空・短縮・過剰な応答は成功扱いせず、途中までのデータも返しません。

connect途中の失敗も含め、取得した接続を一度だけcloseします。読取とcloseの両方がI/Oで失敗した場合は読取側を返し、close原因は元の例外のsuppressedへ残します。読取成功後のclose失敗もエラーです。CancellationExceptionとJVMのErrorは読取Errへ変換しません。

キャンセルはブロック間でも検査します。ただし、実行中のブロッキングtransceiveを、この実装が別スレッドから即時closeして中断する仕組みは追加していません。実行中I/Oが戻った後に解放・キャンセル伝播します。画面終了時の即時中断は、Reader Mode・セッション管理を接続する際の確認事項です。

## 業務データ

| 項目 | 範囲（終了を含む） | 戻り値・解釈 |
| --- | --- | --- |
| ゴルフ場番号 | ブロック1 Byte 0〜1 | Int、LSBファースト、0..65535 |
| ホルダー番号 | ブロック1 Byte 2〜ブロック4 Byte 3 | String、読み取り順・大文字・区切りなしの16進文字列28桁 |

ゴルフ場番号はByte 0が下位です。`01 02`は513、`FF FF`は65535になります。ホルダー番号は、文字コード・パディングが「任意の書式」としか定義されていないため、合意した暫定方式として16進文字列を返します。例えば先頭の`30 31 20 00`は`30312000`です。ASCIIへの復号、空白・0x00の削除、2バイト単位への分割は行いません。先頭・末尾の0や上位ビットも保持し、常に28桁で返します。

両ユースケースはHiltのコンストラクタ注入に対応しています。検出済みのAndroid Tagは`AndroidNfcTagAccess(tag)`で包み、コルーチンから各インスタンスを関数として呼び出せます。

```kotlin
// readCourseNumber・readHolderNumberは、それぞれHiltから注入したユースケース。
val access = AndroidNfcTagAccess(tag)
val courseResult: Result<Int, ReadGolfCourseNumberErr> = readCourseNumber(access)
val holderResult: Result<String, ReadGolfHolderNumberErr> = readHolderNumber(access)
```

それぞれ別の接続で読み取ります。2項目の一括取得や、複数項目を1つの結果としてWebへ返す処理は含めません。

## エラーの型

ゴルフ場番号の例は`ReadGolfCourseNumberErr.NfcRead.Communication`です。ホルダー番号では`ReadGolfHolderNumberErr.NfcRead.Communication`です。外側がユースケース、次がデータ処理、最後が原因です。`Err`は外側だけに付けます。

`NfcReadFailureFactory`をReaderへ渡し、各ユースケースの具体型を失敗箇所で直接生成します。dataからfeatureへ依存せず、共通の具体エラーからユースケース型へ包み直しません。同じ値を`NfcCommunication`・`NfcTagLost`などの共通interfaceとしても処理できます。

範囲不正、対象外タグ、タグ離脱、I/O、応答形式不正、タグのエラーコード、Androidのアクセス拒否を区別します。無応答だけでタグ離脱と断定しません。元の例外はcauseで保持し、予期しないプログラム例外は通信エラーへ隠さず伝播します。

## 検証

[検証ログ・件数](logs/nfc-reading/README.md)を参照してください。

- JVMの模擬接続で、SLI・SLIX・SLIX2・他技術、UID形式、座標境界、必要なREADだけの送信、応答形式、途中失敗、解放、キャンセルを確認します。
- ユースケースでは実際の共通Readerと模擬タグを組み合わせ、固定座標・LSBファースト・符号境界・ホルダーの28桁表現と0x00／空白／上位ビットの保持・ユースケース固有のエラーを確認します。
- 実タグを接続できる端末は現在ありません。実ICODE SLIでの読取成功、端末から得るUIDの送信順、離脱・再提示は未確認です。エミュレーターでNFC通信成功を検証したとは扱いません。

```sh
npm run test:unit
npm run check:android
```
