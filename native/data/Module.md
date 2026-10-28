# Module data

汎用NFCタグ検出・読取・通信・永続化・日時処理などを扱うAndroid Libraryです。
featureから参照され、画面側への依存は持ちません。

# Package dev.example.nativedata.constant.binary

カード・通信方式に依存しないバイト幅・符号補正マスク・ニブル幅・16進表現の定数をまとめます。
業務項目のバイト位置や、プロトコルのフラグ・コマンド番号はここへ混ぜません。

# Package dev.example.nativedata.nfc.detection

業務に依存しないタグ検出の契約・実装、対象参照、ReaderModeの制御を提供します。
検出通知の処理が終わると対象参照を無効化し、最後のタグを保存しません。
runCatching直後のalsoで対象・要求を解放し、失敗・取消はその後で再送出します。

# Package dev.example.nativedata.nfc.profile

型番・通信方式・メモリ構成をまとめたProfileと、型番判定器の契約・共通Resolverを提供します。
モデル・Profileはdata内部へ閉じ、ユースケースから通信方式を受け取りません。
ResolverはHiltのSetで登録された判定器を呼び、一意のProfileを選びます。

# Package dev.example.nativedata.nfc.profile.icodesli

ICODE SLIのProfileと、UID・Android技術による型番判定器を提供します。現在登録する判定器はSLIだけです。

# Package dev.example.nativedata.nfc.card

対象に紐づく共通読取契約NfcCardReaderと、そのDefault実装・生成Factoryを提供します。
対象の有効期間・排他・Profile識別・モデル照合を管理し、範囲読取はNfcMemoryDataSourceへ委譲します。
業務データを扱うRepositoryと区別した命名で、Readerはdata内部だけで利用します。

# Package dev.example.nativedata.nfc.memory

座標・範囲・メモリ構成、Android NFC APIとの接続、NfcMemoryDataSourceの範囲読取を提供します。
Profileの通信方式・ブロック幅で要求範囲のByteArrayを返し、値の解釈・文字列化は業務Repositoryへ任せます。
FeliCa・MIFAREなどの読取と、書き込み・認証は実装していません。

# Package dev.example.nativedata.nfc.protocol

ISO 15693のUID長・プレフィックス、Read Single Blockの要求フラグ・コマンド・応答形式を定義します。
現在の標準コマンドで指定できる最大ブロック番号も、汎用のバイトマスクとは別の定数で表します。

# Package dev.example.nativedata.err.nfc

検出・読取失敗の共通interface・Factoryと、I/O例外の分類処理を提供します。
feature固有の具体エラーを逆依存なしに生成し、取消や予期しない実装例外は伝播します。

# Package dev.example.nativedata.di.nfc

検出Repository・業務Factoryのbindingと、型番判定器のSet登録をまとめます。

# Package dev.example.nativedata.repository.golf

検出した対象と呼出元のエラーFactoryに紐づく、引数なしの業務Repositoryを提供します。
物理座標を内部で解決し、GolfCardDecoderで取得バイト列を業務値へ変換します。
ゴルフ場番号は0..65535のInt、ホルダー番号は大文字・区切りなしの28桁16進Stringで返します。
型番・座標・格納順・Byteの符号補正は公開APIへ持ち出しません。

# Package dev.example.nativedata.repository.card

業務カードごとのRepositoryを生成する公開契約CardRepositoryFactoryと、そのDefault実装を提供します。
現在のcreateGolfCardは共通のNfcCardReaderFactoryを使って対象・エラー文脈を固定し、
別ファイルのDefaultGolfCardRepositoryへ組み込みます。IC型番から業務の種類を推測しません。
