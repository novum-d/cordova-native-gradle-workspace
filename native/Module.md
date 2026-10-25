# Module Cordova Native Libraries

Cordova Android 14とnative単独のビルドで使うLibrary・CordovaプラグインのAPIドキュメントです。
Kotlinソースに記述したKDocと、各モジュールの概要をDokkaでHTMLに変換します。

- **feature**: Compose画面とUI関連の依存を管理します。確認用ホーム・ゴルフ場IDとホルダー番号の読み取り画面、サービス別Navigation、SavedStateHandleから引数を受け取るViewModelを持ちます。
- **data**: 通信・永続化・日時処理などの依存を管理します。現在は業務用の本体クラスがないため、モジュール概要を掲載します。
- **cordova-plugin-dev**: `kotlin-plugin/src/android`の元ソースを編集・コンパイル・文書化します。APKへ組み込むモジュールではありません。

依存方向はcordova-plugin-devからfeature、featureからdataです。テスト・ビルド生成ソースは掲載対象に含めません。
