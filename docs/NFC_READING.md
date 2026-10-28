# ICODE SLIの判定・座標読み取り

[ドキュメント一覧](README.md) · [サービス仕様](CLIENT_API_SPEC.md)

汎用タグ検出・引数なしの業務Repository・画面への接続を実装しています。[タグ検出と業務Repositoryの構成](NFC_READER_DESIGN.md)に責務と寿命を整理しています。読取対応製品はICODE SLIだけです。

## 実装の範囲

`~/repos/NFCApp`のNFC-V Single Block READと、応答検査・接続解放を参考に実装しています。読み取る製品はICODE SLIだけです。FeliCa、MIFARE、SLIXなどの読み取りや、書き込み・認証は追加していません。

| 配置 | 責務 |
| --- | --- |
| [data/nfc](../native/data/src/main/kotlin/dev/example/nativedata/nfc/) | Android Tagとの接続、SLI判定、範囲検査、READ・応答検査、連続ByteArrayの取得 |
| [data/err/nfc](../native/data/src/main/kotlin/dev/example/nativedata/err/nfc/) | 検出・読取の共通エラー契約と生成・分類処理 |
| [data/repository/card](../native/data/src/main/kotlin/dev/example/nativedata/repository/card/) | 業務Repositoryの生成契約と、共通Factoryを使う生成実装 |
| [data/repository/golf](../native/data/src/main/kotlin/dev/example/nativedata/repository/golf/) | 対象タグに紐づくRepository、固定座標の対応と、業務値への変換 |
| [feature/usecase/nfc](../native/feature/src/main/kotlin/dev/example/nativefeature/usecase/nfc/) | 汎用タグ検出ユースケース |
| [feature/err](../native/feature/src/main/kotlin/dev/example/nativefeature/err/) | 汎用検出・ゴルフ読取のユースケース固有エラー |
| [ObserveGolfCardUseCase](../native/feature/src/main/kotlin/dev/example/nativefeature/usecase/golfchange/ObserveGolfCardUseCase.kt) | 同じ対象から2項目を取得し、画面へ結果を通知 |
| [ReadGolfCourseNumUseCase](../native/feature/src/main/kotlin/dev/example/nativefeature/usecase/golfchange/ReadGolfCourseNumUseCase.kt) | Repositoryからゴルフ場番号をIntで取得 |
| [ReadGolfHolderNumUseCase](../native/feature/src/main/kotlin/dev/example/nativefeature/usecase/golfchange/ReadGolfHolderNumUseCase.kt) | Repositoryからホルダー番号をStringで取得 |

画面→ViewModel→`ObserveGolfCardUseCase`→汎用検出ユースケース→検出Repositoryの順で呼び出します。検出後はFactoryで同じ対象に紐づく読取ユースケースを作り、引数なしの`suspend operator fun invoke()`で2項目を順番に取得します。固定座標・型番判定はdataへ隠します。ReaderModeはActivityの前面表示と購読に合わせて制御し、画面に番号・失敗の案内を表示します。Webへの業務結果返却は未実装です。NFCは通常権限で、ハードウェアは必須にしていません。

## 型番Profileとタグ判定

[NfcTagProfileResolver](../native/data/src/main/kotlin/dev/example/nativedata/nfc/profile/NfcTagProfileResolver.kt)が登録済みの判定器を呼び、一意のProfileを選びます。現在登録している[IcodeSliTagProfileIdentifier](../native/data/src/main/kotlin/dev/example/nativedata/nfc/profile/icodesli/IcodeSliTagProfileIdentifier.kt)は、通信せずに次を確認します。

- 選択するProfileの通信方式が必要とする技術`android.nfc.tech.NfcV`がある。
- UIDが8バイトで、UID7＝E0、UID6＝04、UID5＝01。
- UID4のbit 4・bit 3がどちらも0。

配列はUID0からの送信順です。表示用のUID文字列へ変換・反転してから渡しません。メーカーと製品種別だけではSLIXやSLIX2も一致するため、ISO 15693の1始まりのbit 37・36で区別します。[SLIX2データシート §9.2.1 Table 8](https://www.nxp.com/docs/en/data-sheet/SL2S2602.pdf#page=12)の区別を参照しています。UIDによる製品分類であり、真正性の認証ではありません。

Profileが得られない、または複数の判定器が一致した場合は未対応という判断です。追加手順は[型番識別の判定器を追加する](NFC_TAG_IDENTIFICATION.md)を参照してください。FeliCaやMIFAREの種類を細かく識別したり、各規格のコマンドで試し読みしたりしません。

## 座標指定

位置は`NfcMemoryPosition(blockNum, byteOffset)`、範囲は`NfcMemoryRange(start, endInclusive)`で指定します。開始・終了とも0始まりで、終了バイトを含みます。

SLIのProfileにユーザー領域を28ブロック・各4バイトと定義します。範囲検査と長さ計算はこの形状を受け取り、座標の型へSLIの値を固定しません。ICODE SLIのユーザー領域はブロック0〜27です。負数、28以上、バイト位置4以上、開始と終了の逆転を通信前に拒否します。ブロック番号を1バイトに格納できても、SLIの領域内とは判断しません。[SLIデータシート §3.2](https://www.nxp.com/docs/en/data-sheet/058031.pdf#page=7)

例えば`(1, 2)`〜`(2, 1)`なら、ブロック1・2を読み、各ブロックのデータから4バイトを返します。タグ全体や管理領域は読みません。2次元配列や2バイト単位の項目へ分割せず、要求順の連続ByteArrayとして返します。

## 通信・解放・取消

`DefaultGolfCardRepository`は項目の固定範囲を共通の`NfcCardReader`へ渡します。共通実装がタグの有効期間・排他・Profileと業務配置の対応モデルを確認します。内部の`NfcMemoryDataSource`は、共通Readerが選択したProfileで通信方式を決め、Dispatchers.IOで実行し、1回の呼び出しごとに接続・切断します。複数ユースケース間で接続や読取済みブロックをキャッシュしません。

フレームは`22 20 <UID0〜7> <block>`です。高速・Addressed・OptionなしのRead Single Blockで対象UIDを指定します。NFCAppの`02 20 <block>`から、対象指定を追加した点が異なります。CRCはAndroidに任せ、Single Block READを必要ブロックへ順番に送ります。[Android NfcV](https://developer.android.com/reference/android/nfc/tech/NfcV)

正常応答はProfileのブロック幅＋Flagsの長さを要求し、SLIでは4＋1の5バイトです。Error flagがある場合はFlags＋エラーコードの2バイトを要求し、コードを符号なし値で保持します。空・短縮・過剰な応答は成功扱いせず、途中までのデータも返しません。

connect途中の失敗も含め、取得した接続を一度だけcloseします。読取とcloseの両方がI/Oで失敗した場合は読取側を返し、close原因は元の例外のsuppressedへ残します。読取成功後のclose失敗もエラーです。CancellationExceptionとJVMのErrorは読取Errへ変換しません。

キャンセルはブロック間でも検査します。Scopeが持つContextとの取り違えを避けるため、`currentCoroutineContext()`で現在実行中のコルーチンのContextを取得し、`ensureActive()`で取消を確認します。ただし、実行中のブロッキングtransceiveを、この実装が別スレッドから即時closeして中断する仕組みは追加していません。実行中I/Oが戻った後に解放・キャンセル伝播します。画面終了時は検出要求とReaderModeを解除しますが、実行中I/Oの即時中断は保証しません。終了した対象への新しい読み取りはTargetExpiredで拒否します。

## 業務データ

| 項目 | 範囲（終了を含む） | 戻り値・解釈 |
| --- | --- | --- |
| ゴルフ場番号 | ブロック1 Byte 0〜1 | Int、LSBファースト、0..65535 |
| ホルダー番号 | ブロック1 Byte 2〜ブロック4 Byte 3 | String、読み取り順・大文字・区切りなしの16進文字列28桁 |

ゴルフ場番号はByte 0が下位です。`01 02`は513、`FF FF`は65535になります。ホルダー番号は、文字コード・パディングが「任意の書式」としか定義されていないため、合意した暫定方式として16進文字列を返します。例えば先頭の`30 31 20 00`は`30312000`です。ASCIIへの復号、空白・0x00の削除、2バイト単位への分割は行いません。先頭・末尾の0や上位ビットも保持し、常に28桁で返します。

格納形式の解釈はdataの[GolfCardDecoder](../native/data/src/main/kotlin/dev/example/nativedata/repository/golf/GolfCardDecoder.kt)へ集約しています。`DefaultGolfCardRepository`が読取成功時だけ変換し、公開APIは`Result<Int, E>`・`Result<String, E>`を返します。ユースケースは業務値を取得するだけで、ByteArray・ビット演算・バイト順を扱いません。失敗の具体型・原因はそのまま伝播します。

### バイト演算と固有仕様の定数

| 配置 | 定義する内容 |
| --- | --- |
| [BinaryConstants](../native/data/src/main/kotlin/dev/example/nativedata/constant/binary/BinaryConstants.kt) | 汎用のバイト幅・符号補正マスク・ニブル幅・16進文字。`0xFF`は`UNSIGNED_BYTE_MASK`、`ushr 4`の幅は`BITS_PER_NIBBLE`として表す |
| [Iso15693Constants](../native/data/src/main/kotlin/dev/example/nativedata/nfc/protocol/Iso15693Constants.kt) | ISO 15693のUID、要求フラグ・コマンド番号、応答の位置・長さ、標準READの最大ブロック番号 |
| [GolfCardDecoder](../native/data/src/main/kotlin/dev/example/nativedata/repository/golf/GolfCardDecoder.kt)内のprivate定数 | ゴルフ場番号の下位・上位バイトの配置 |
| [IcodeSliTagProfileIdentifier](../native/data/src/main/kotlin/dev/example/nativedata/nfc/profile/icodesli/IcodeSliTagProfileIdentifier.kt)・[IcodeSliTagProfile](../native/data/src/main/kotlin/dev/example/nativedata/nfc/profile/icodesli/IcodeSliTagProfile.kt)内のprivate定数 | SLI識別の製品コード・派生型ビット・UID配置と、ユーザー領域の28ブロック×4バイト |

同じ数値でも意味が異なるものは別の定数で表します。例えば`0xFF`は、符号補正では`UNSIGNED_BYTE_MASK`、標準READの範囲検査では`MAX_SINGLE_BLOCK_NUM`です。SLIのブロック幅4バイトと、汎用のニブル幅4bitも共有しません。

両ユースケースはHiltのコンストラクタ注入に対応しています。検出済みのAndroid Tagは`DefaultNfcTagAccess(tag)`で包み、コルーチンから各インスタンスを関数として呼び出せます。`NfcTagAccess`の標準実装として扱い、内部のNFC-V接続も`DefaultNfcVConnection`へ委譲します。

```kotlin
// 汎用検出で得た同じtargetから、業務側のFactoryで組み立てる。
val readCourseNum = readers.courseNum(target)
val readHolderNum = readers.holderNum(target)
val courseResult: Result<Int, ReadGolfCourseNumErr> = readCourseNum()
val holderResult: Result<String, ReadGolfHolderNumErr> = readHolderNum()
```

それぞれ別の接続で読み取ります。画面の業務ユースケースはゴルフ場番号の失敗時にはホルダーを読まず、両方成功した場合だけ完了を通知します。検出参照は検出Flowのmap・collect内で使い切り、処理が戻ると失効します。複数項目を1つの結果としてWebへ返す処理は含めません。

## エラーの型

ゴルフ場番号の例は`ReadGolfCourseNumErr.NfcRead.Communication`です。ホルダー番号では`ReadGolfHolderNumErr.NfcRead.Communication`です。外側がユースケース、次がデータ処理、最後が原因です。ユースケースの階層では`Err`は外側だけに付けます。独立した共通契約の最上位型は`NfcReadErr`・`NfcDetectionErr`とし、原因の共通interfaceである`NfcTagLost`などには`Err`を付けません。

`NfcReadErrFactory`をReader・DataSourceへ渡し、各ユースケースの具体型を失敗箇所で直接生成します。dataからfeatureへ依存せず、共通の具体エラーからユースケース型へ包み直しません。同じ値を`NfcCommunication`・`NfcTagLost`などの共通interfaceとしても処理できます。

対象参照の期限切れ、範囲不正、対象外タグ、タグ離脱、I/O、応答形式不正、タグのエラーコード、Androidのアクセス拒否を区別します。無応答だけでタグ離脱と断定しません。元の例外はcauseで保持し、予期しないプログラム例外は通信エラーへ隠さず伝播します。

## 検証

[読取導入時の記録](logs/nfc-reading/README.md)と、[汎用検出・Repository接続の記録](logs/nfc-detection/README.md)、[型番Profile導入の記録](logs/nfc-profile/README.md)を参照してください。

- JVMの模擬接続で、SLI・SLIX・SLIX2・他技術、UID形式、座標境界、必要なREADだけの送信、応答形式、途中失敗、解放、キャンセルを確認します。
- dataでは実際の業務Repositoryと共通Reader・模擬タグで固定座標・応答検査・解放を確認します。GolfCardDecoderのテストでLSBファースト・符号境界・ホルダーの28桁表現と0x00／空白／上位ビットを確認します。featureでは解釈済みの業務値・読取順序・ユースケース固有のエラーを確認します。
- 汎用検出の購読・キャンセル・再購読・追加タグの破棄・対象参照の失効と、同じカードの2項目取得・失敗後の再検出を検証します。
- 試験用に形状を変えたProfileで、範囲・長さ・応答検査・切り出しがProfileを参照することも検証します。試験用の形状は実製品の仕様・新しい対応製品ではありません。
- 実タグを接続できる端末は現在ありません。実ICODE SLIでの読取成功、端末から得るUIDの送信順、離脱・再提示は未確認です。エミュレーターでNFC通信成功を検証したとは扱いません。

```sh
npm run test:unit
npm run check:android
```
