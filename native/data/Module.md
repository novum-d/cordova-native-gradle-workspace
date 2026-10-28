# Module data

汎用NFCタグ検出・読取・通信・永続化・日時処理などを扱うAndroid Libraryです。
featureから参照され、画面側への依存は持ちません。

# Package dev.example.nativedata.nfc

ICODE SLIの型番Profile選択、Profileの通信方式・メモリ構成を使う範囲読取と、Android NFC APIとの接続を提供します。
型番識別は共通Resolverと個別判定器へ分け、HiltのSet登録で追加できます。現在の登録はSLIだけです。
モデル・Profile・共通読取Repository・汎用Readerはdata内部へ閉じ、ユースケースから通信方式を受け取りません。
対象の有効期間・排他・Profile識別はDefaultNfcCardRepositoryで共通管理し、生成役のFactoryとファイルを分けます。
値の業務上の意味・文字列化はfeatureのユースケースへ任せ、要求範囲のByteArrayを返します。
失敗の共通interfaceとFactoryによって、feature固有の具体エラーを逆依存なしに生成します。
FeliCa・MIFAREなどの読取と、書き込み・認証は実装していません。

# Package dev.example.nativedata.golf

検出した対象と呼出元のエラーFactoryに紐づく、引数なしの業務Repositoryを提供します。
物理座標を内部で解決して2バイト・14バイトを返し、値の解釈はfeatureへ任せます。

# Package dev.example.nativedata.card

業務カードごとのRepositoryを生成する公開契約CardRepositoryFactoryと、そのDefault実装を提供します。
現在のcreateGolfCardは共通のNfcCardRepositoryFactoryを使って対象・エラー文脈を固定し、
別ファイルのDefaultGolfCardRepositoryへ組み込みます。IC型番から業務の種類を推測しません。
