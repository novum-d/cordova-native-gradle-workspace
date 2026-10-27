# Module data

NFCの読取・通信・永続化・日時処理などを扱うAndroid Libraryです。
featureから参照され、画面側への依存は持ちません。

# Package dev.example.nativedata.nfc

ICODE SLIの識別、ブロック・バイト位置の範囲読取と、Android NFC APIとの接続を提供します。
値の業務上の意味・文字列化はfeatureのユースケースへ任せ、要求範囲のByteArrayを返します。
失敗の共通interfaceとFactoryによって、feature固有の具体エラーを逆依存なしに生成します。
FeliCa・MIFAREなどの読取と、書き込み・認証は実装していません。
