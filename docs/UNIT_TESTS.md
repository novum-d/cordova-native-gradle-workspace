# 単体テスト環境

## 対象と現状

`native/feature`・`native/data`のローカルJVM単体テストを、JDK 17で実行します。Android SDKはコンパイルに必要ですが、実機・エミュレータの起動は不要です。native単独ビルドとCordovaホストから、同じテストソースを実行できます。

導入当初の4テストは環境の動作確認です。その後、画面引数・WebMessage・Timberに加え、[NFCのタグ分類・範囲読取・業務ユースケース](NFC_READING.md)のテストを追加しています。NFCテストは模擬接続で送信フレーム・応答・解放を確認します。汎用検出の購読・取消・再購読・追加タグ破棄・古い参照の拒否と、同じカードからの2項目取得・具体エラーの保持も検証します。型番Profileに応じた範囲・長さ・応答検査・切り出しと、実タグの技術・モデルの不一致拒否も検証します。業務に依存しない範囲読取と、同じタグを指す複数の共通Repositoryの排他・エラー文脈の独立性も検証します。型番判定器の登録追加・技術別の振分け・複数一致の拒否・識別の非同期待機も確認します。実タグでの通信成功とは区別します。

| 項目 | 設定 |
| --- | --- |
| テストフレームワーク | JUnit 4.13.2（既存構成を使用） |
| Coroutinesのテスト支援 | `kotlinx-coroutines-test:1.8.1`。本体の`coroutines`版を参照 |
| Main Dispatcher | featureの`MainDispatcherRule`でテスト用Dispatcherに差し替え、終了時に復元 |
| カバレッジ | JaCoCo 0.8.14。AGP 8.7.3の`enableUnitTestCoverage`を各build typeで有効化 |
| DI | [Hiltの設定](HILT.md)はアプリ起動で使用。通常の単体テストではコンストラクタに依存を渡してインスタンスを作る |
| Android API・UI | 現在のJVMテストでは使用しない。Robolectric、Compose UIテスト、実機テストの追加は今回の対象外 |

版は[`libs.versions.toml`](../native/gradle/libs.versions.toml)、テスト依存とカバレッジ設定は各モジュールの`build.gradle.kts`に置きます。MockK等のモックライブラリは追加せず、必要な依存は手書きのFakeで差し替える方針です。通常のJVM単体テストにHiltのAndroidテスト用依存は不要です。[Hiltの単体テストの説明](https://developer.android.com/training/dependency-injection/hilt-testing)

## サービス画面引数のテスト

featureの[FeatureDestinationTest](../native/feature/src/test/kotlin/dev/example/nativefeature/FeatureDestinationTest.kt)に5件の契約テストを追加しています。args省略・空オブジェクト・日本語と予約文字を含む構造化JSONの保持、必須idの欠落、argsへの配列指定を確認します。既存の環境確認テストとは別のテストクラスです。[FeatureLaunchRequestTest](../native/feature/src/test/kotlin/dev/example/nativefeature/FeatureLaunchRequestTest.kt)には、処理IDの分岐とJSON境界の10件を追加しています。サービスIDによる読み取り画面への分岐、旧機器名IDの拒否、省略・空・構造化argsを確認します。ViewModelの`SavedStateHandle.toRoute()`はAndroidのBundleを使うため、実際のWeb→Plugin→Intent→Navigation→ViewModelの経路と復元をエミュレーターで確認します。[仕様と検証](SERVICE_NAVIGATION.md)

## 配置

| ファイル | 目的 |
| --- | --- |
| [featureのMainDispatcherRule](../native/feature/src/test/kotlin/dev/example/nativefeature/MainDispatcherRule.kt) | `Dispatchers.Main`の差し替え・復元。将来のViewModelテストでも使用可能 |
| [featureの環境確認テスト](../native/feature/src/test/kotlin/dev/example/nativefeature/LocalUnitTestEnvironmentTest.kt) | Main上の処理と`runTest`がスケジューラを共有すること、仮想時間で状態が変わること、テスト間で時刻がリセットされることを確認 |
| [dataの環境確認テスト](../native/data/src/test/kotlin/dev/example/nativedata/LocalUnitTestEnvironmentTest.kt) | Flowの待機を仮想時間で処理できること、テストソースにもSerialization Pluginが適用され、Datetimeを含むレコードをJSON化・復元できることを確認 |

業務ロジックを追加するときは、対象モジュールの`src/test/kotlin/`に`対象クラス名Test.kt`を置き、正常系・失敗系の振る舞いを確認します。Activity、Composeレイアウト、DI設定の動作をこの環境確認テストへ混ぜません。

## 実行方法

リポジトリ直下から実行します。初回の依存・SDK等の準備はREADMEの手順に従ってください。

```sh
# feature・dataのDebug／Release単体テストを実行する
npm run test:unit

# Cordova prepare後、Cordovaホストから同じ4構成を実行する
npm run test:unit:cordova

# Debugのテストを必要に応じて実行し、両モジュールのカバレッジを生成する
npm run test:unit:coverage

# 特定のモジュール／テストだけを実行する
./scripts/with-env.sh ./native/gradlew -p native :feature:testDebugUnitTest \
  --tests 'dev.example.nativefeature.LocalUnitTestEnvironmentTest'
```

テスト失敗はGradleとnpmの非ゼロ終了として扱います。テスト結果を無視する設定はしていません。各モジュールの通常のGradle `check`からも単体テストが実行されます。Gitのpre-commitは既存の整形・detektを担当し、今回、単体テスト実行をpre-commitへ追加してはいません。

nativeとCordovaはモジュールの`build/`ディレクトリを共有するため、両ホストのGradleコマンドは順番に実行します。直前の実行結果が再利用される場合があり、強制的に再実行したいときはGradleに`--rerun-tasks`を渡します。

## レポート

`<module>`は`feature`または`data`、`<Variant>`は`Debug`または`Release`です。

| 内容 | 出力先 |
| --- | --- |
| テスト結果HTML | `native/<module>/build/reports/tests/test<Variant>UnitTest/index.html` |
| JUnit XML | `native/<module>/build/test-results/test<Variant>UnitTest/TEST-*.xml` |
| DebugカバレッジHTML | `native/<module>/build/reports/coverage/test/debug/index.html` |
| DebugカバレッジXML | `native/<module>/build/reports/coverage/test/debug/report.xml` |

Releaseのカバレッジは、各モジュールの`createReleaseUnitTestCoverageReport`を実行します。

カバレッジは本体コードを対象にします。環境確認テストが成功しても業務ロジックをカバーしたことにはならず、現在のfeatureではActivityを実行しないため未カバーになり、NFCのReader・ユースケースはJVMテストで検証する処理が計測対象になります。現段階でカバレッジ率の下限は設定していません。

Coroutines Testはテスト専用、JaCoCoはテスト実行・レポート用です。

## 実行確認（2026-10-20）

- native・Cordovaそれぞれで、feature／dataのDebug／Release、計4構成を実行。各構成2件、各ホスト計8件のテストが成功しました。Cordova側では`--rerun-tasks`を指定して実際の再実行を確認しました。
- 一時ディレクトリの失敗用テストをinit scriptでdataのテストソースへ追加し、アサーション失敗がJUnit XMLに記録され、`npm run test:unit`が非ゼロ終了することを確認しました。失敗用テストは削除し、通常構成のテストを再実行しています。
- 両モジュールのDebugカバレッジHTML／XMLを生成しました。
- nativeの`detektFormat check`が成功し、整形・静的解析、単体テストと既定のDebug Android Lintを通過しました。

## 設定の根拠

- [Androidのローカル単体テスト](https://developer.android.com/training/testing/local-tests)：JVMテストの配置とAndroid APIの制約。
- [Coroutines Test 1.8.1のREADME](https://github.com/Kotlin/kotlinx.coroutines/blob/1.8.1/kotlinx-coroutines-test/README.md)：`runTest`、仮想時間、Main Dispatcherの差し替え。
- [AGP 8.7のBuildType API](https://developer.android.com/reference/tools/gradle-api/8.7/com/android/build/api/dsl/BuildType)：`enableUnitTestCoverage`がJaCoCo Pluginによる収集を有効化する。
- [AGP 8.7のTestCoverage API](https://developer.android.com/reference/tools/gradle-api/8.7/com/android/build/api/dsl/TestCoverage)：`jacocoVersion`による使用版の指定。
