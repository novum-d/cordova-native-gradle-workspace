# Module data

汎用NFCタグ検出・読取・通信・永続化・日時処理などを扱うAndroid Libraryです。
featureから参照され、画面側への依存は持ちません。

# Package dev.example.nativedata.nfc

ICODE SLIの型番Profile選択、Profileの通信方式・メモリ構成を使う範囲読取と、Android NFC APIとの接続を提供します。
モデル・Profile・汎用Readerはdata内部へ閉じ、ユースケースから通信方式を受け取りません。
値の業務上の意味・文字列化はfeatureのユースケースへ任せ、要求範囲のByteArrayを返します。
失敗の共通interfaceとFactoryによって、feature固有の具体エラーを逆依存なしに生成します。
FeliCa・MIFAREなどの読取と、書き込み・認証は実装していません。

# Package dev.example.nativedata.golf

検出した対象と呼出元のエラーFactoryに紐づく、引数なしの業務Repositoryを提供します。
物理座標を内部で解決して2バイト・14バイトを返し、値の解釈はfeatureへ任せます。
