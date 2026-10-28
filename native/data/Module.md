# Module data

汎用NFCタグ検出・読取・通信・永続化・日時処理などを扱うAndroid Libraryです。
featureから参照され、画面側への依存は持ちません。

# Package dev.example.nativedata.nfc.detection

業務に依存しないタグ検出の契約・実装、対象参照、ReaderModeの制御と検出失敗の契約を提供します。
検出通知の処理が終わると対象参照を無効化し、最後のタグを保存しません。

# Package dev.example.nativedata.nfc.profile

型番・通信方式・メモリ構成をまとめたProfileと、型番判定器の契約・共通Resolverを提供します。
モデル・Profileはdata内部へ閉じ、ユースケースから通信方式を受け取りません。
ResolverはHiltのSetで登録された判定器を呼び、一意のProfileを選びます。

# Package dev.example.nativedata.nfc.profile.icodesli

ICODE SLIのProfileと、UID・Android技術による型番判定器を提供します。現在登録する判定器はSLIだけです。

# Package dev.example.nativedata.nfc.reader

対象に紐づく共通読取契約NfcCardReaderと、そのDefault実装・生成Factoryを提供します。
対象の有効期間・排他・Profile識別・モデル照合を管理し、範囲読取はNfcMemoryDataSourceへ委譲します。
業務データを扱うRepositoryと区別した命名で、Readerはdata内部だけで利用します。

# Package dev.example.nativedata.nfc.memory

座標・範囲・メモリ構成、Android NFC APIとの接続、NfcMemoryDataSourceの範囲読取を提供します。
Profileの通信方式・ブロック幅で要求範囲のByteArrayを返し、値の業務上の意味・文字列化はfeatureへ任せます。
FeliCa・MIFAREなどの読取と、書き込み・認証は実装していません。

# Package dev.example.nativedata.nfc.failure

読取失敗の共通interface・Factoryと、I/O例外の分類処理を提供します。
feature固有の具体エラーを逆依存なしに生成し、取消や予期しない実装例外は伝播します。

# Package dev.example.nativedata.nfc.di

検出Repository・業務Factoryのbindingと、型番判定器のSet登録をまとめます。

# Package dev.example.nativedata.golf

検出した対象と呼出元のエラーFactoryに紐づく、引数なしの業務Repositoryを提供します。
物理座標を内部で解決して2バイト・14バイトを返し、値の解釈はfeatureへ任せます。

# Package dev.example.nativedata.card

業務カードごとのRepositoryを生成する公開契約CardRepositoryFactoryと、そのDefault実装を提供します。
現在のcreateGolfCardは共通のNfcCardReaderFactoryを使って対象・エラー文脈を固定し、
別ファイルのDefaultGolfCardRepositoryへ組み込みます。IC型番から業務の種類を推測しません。
