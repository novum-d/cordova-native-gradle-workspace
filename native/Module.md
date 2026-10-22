# Module Cordova Native Libraries

Cordova Android 14とnative単独のビルドで共有するLibraryのAPIドキュメントです。
Kotlinソースに記述したKDocと、各モジュールの概要をDokkaでHTMLに変換します。

- **feature**: Compose画面とUI関連の依存を管理します。現在はComposeの起動・状態更新を確認するActivityを持ちます。
- **data**: 通信・永続化・日時処理などの依存を管理します。現在は業務用の本体クラスがないため、モジュール概要を掲載します。

依存方向はfeatureからdataです。テスト・ビルド生成ソースは掲載対象に含めません。
