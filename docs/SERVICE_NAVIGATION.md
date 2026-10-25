# サービス単位の起動要求と画面・引数の復元

[ドキュメント一覧](README.md) · [KotlinプラグインのAPI](../kotlin-plugin/README.md)

## 呼び出しの流れ

CordovaのWebから`{id, args?}`を渡し、KotlinPluginが検証した要求を明示的Intentへ格納します。Activityは要求を一度だけNavigationへ渡し、各画面のViewModelが`SavedStateHandle`から処理ID・argsを受け取ります。

```text
Web: openFeature({id, args?}, success, error)
  → KotlinPlugin: FeatureLaunchRequestへ変換・検証
  → 固定のMainActivityへのIntentに要求JSONを格納
  → MainActivity: 処理IDを型付き画面へ解決し、Navigationへ渡す
  → GolfChangeNavigation: ゴルフ場釣り銭機サービスの読み取り画面を表示
  → 各画面のViewModel: SavedStateHandle.toRoute()で引数を受け取る
```

| 処理ID | 表示する画面 | ViewModel |
| --- | --- | --- |
| `golf-change.read-holder` | ゴルフ場ID・ホルダー番号 読み取り画面 | GolfHolderReadViewModel |
| `home` | 確認用のホーム画面 | 引数を使用しないためなし |

各機能画面は画面名・処理ID・argsを中央に表示し、「戻る」でCordovaへ戻ります。現在の確定仕様はゴルフ場ID・ホルダー番号の読み取りです。この2項目は読み取りによって取得する値で、Webからの必須引数にはしていません。実際の読み取り、データ形式の解析、結果のWeb返却、権限要求は未実装です。

NFC書き込みは対象外です。`nfc.read`・`nfc.write`・`camera.capture`は受け付けず、サービス名と処理を表すIDを使います。本人確認（マイナンバー）は別サービスとして追加する想定ですが、処理ID・画面・引数が未確定のため、今回の受付や画面には追加していません。

## JSからの利用

```js
// deviceready以降に呼ぶ。成功は起動要求の完了であり、機器処理の結果ではない。
window.KotlinPlugin.openFeature(
    { id: 'golf-change.read-holder', args: { source: 'web' } },
    function () { console.log('Screen opened'); },
    function (error) { console.error(error); }
);

// args省略・空オブジェクトの指定も可能。
window.KotlinPlugin.openFeature({ id: 'golf-change.read-holder' }, onSuccess, onError);
window.KotlinPlugin.openFeature({ id: 'golf-change.read-holder', args: {} }, onSuccess, onError);
```

サンプルWebには、ゴルフ場釣り銭機サービスと確認用ホームを呼ぶボタンを置いています。未知のIDや不正な要求はエラーコールバックへ返し、Activityを起動しません。ネイティブ画面内で仮の処理IDを生成する操作はありません。

## 起動要求と型安全性

[FeatureLaunchRequest](../native/feature/src/main/kotlin/dev/example/nativefeature/launch/FeatureLaunchRequest.kt)がJS・Intent境界の型です。

```kotlin
/** Webから渡す処理IDと、任意のJSONオブジェクト引数。 */
@Serializable
data class FeatureLaunchRequest(
    val id: String,
    val args: JsonObject? = null,
)
```

`id`は必須のStringです。`args`は省略・nullを「未指定」、`{}`を空オブジェクトとして区別します。配列・文字列などは拒否します。内部項目は未指定なので`JsonObject`で受け取り、そのフィールドまでは型保証しません。仕様が決まった段階で各機能専用のSerializableな型へ変換できます。

[KotlinPlugin](../kotlin-plugin/src/android/dev/example/kotlinplugin/KotlinPlugin.kt)は`MainActivity.EXTRA_REQUEST`（`dev.example.nativefeature.LAUNCH_REQUEST`）に要求全体のJSON文字列を格納します。JSから遷移先クラス名やIntent自体は受け取りません。MainActivityは非公開（`exported=false`）のままです。

`toDestination()`で[FeatureDestination](../native/feature/src/main/kotlin/dev/example/nativefeature/navigation/FeatureDestination.kt)の型へ変換します。`GolfHolderRead`がサービスの読み取り画面を表す型付きルートです。

## フォルダと責務

`native/feature/src/main/kotlin/dev/example/nativefeature/`をサービスと共通処理で整理しています。

| 配置先 | 責務 |
| --- | --- |
| [MainActivity.kt](../native/feature/src/main/kotlin/dev/example/nativefeature/MainActivity.kt) | Intentの受け取り・検証、一時的なコマンドの受け渡し |
| [launch/](../native/feature/src/main/kotlin/dev/example/nativefeature/launch/) | 起動要求のJSON変換と処理IDの解決 |
| [navigation/](../native/feature/src/main/kotlin/dev/example/nativefeature/navigation/) | 共通NavHost、型付きルート、JSON用NavType |
| [services/golfchange/](../native/feature/src/main/kotlin/dev/example/nativefeature/services/golfchange/) | ゴルフ場釣り銭機サービスのNavigation・ViewModel・画面 |
| [home/](../native/feature/src/main/kotlin/dev/example/nativefeature/home/) | 確認用ホームの表示 |
| [common/](../native/feature/src/main/kotlin/dev/example/nativefeature/common/) | 画面名・処理ID・argsを中央に表示する共通レイアウト |

NavHostとNavControllerは一つです。`golfChangeNavigation()`でサービス単位に画面を登録し、ViewModelは各NavBackStackEntryに属します。Activity共有のViewModelで引数を使い回しません。

本人確認など新しいサービスを追加するときは、`services/`配下に専用のNavigation・ViewModel・画面をまとめ、サービス名を含む処理IDと型付きルートを登録します。NFC・Cameraは各サービスから利用する機器処理として扱い、共有できる処理は実装する段階で共通化します。

[JsonObjectNavType](../native/feature/src/main/kotlin/dev/example/nativefeature/navigation/JsonObjectNavType.kt)と共通の`featureArgumentTypes`を、画面登録と`SavedStateHandle.toRoute()`の両方で使います。BundleにはJSON文字列を保存し、ルート出力時だけURIエンコードします。二重デコードを避け、日本語・予約文字・`%2F`を保持します。

## 状態復元と新しい要求

- `EXTRA_REQUEST`はWebから届いた要求の受信キーです。独自の`STATE_REQUEST`とActivityの`onSaveInstanceState`は削除しました。
- 初回（`savedInstanceState == null`）はIntentを検証し、一時的な起動コマンドとしてNavigationへ渡します。Navigationが受け取った後はコマンドを破棄し、要求JSONを重ねて保存しません。
- 再生成時は初回Intentを再実行せず、`rememberNavController()`が保存した最新の画面・引数を復元します。各ViewModelはその画面の`SavedStateHandle`から引数を復元します。
- `FLAG_ACTIVITY_SINGLE_TOP`による再利用時は`onNewIntent()`でIntentを更新し、同じ検証を行います。NavHostを作り直さず、現在の履歴を要求先の画面一つへ置き換えます。
- 同じ処理ID・同じargsの再要求も別のコマンドとして受け付け、画面のViewModelを作り直します。単なる回転・プロセス再生成では起動コマンドを発行しません。
- 各機能画面の「戻る」・システムバックはActivityを閉じます。以前の要求先や確認用ホームを途中に表示しません。
- 直接Activityへ不正な初回Intent・新Intentが届いた場合は終了します。

読み取りなどの実処理を今後追加するときは、引数の復元だけで自動再実行しないよう、実行開始・結果・再試行を別途設計します。

## 採用ライブラリ

`navigation-3`スキルの型安全ナビゲーション手順を参照し、既存のKotlin 1.9.24／SDK 35と互換性のあるNavigation Compose 2.8.9を使います。Serialization JSONは1.6.3、Lifecycle ViewModel Compose／SavedStateは既存と同じ2.8.7です。

- [Navigationの型安全APIとSavedStateHandle](https://developer.android.com/guide/navigation/design/type-safety)
- [ActivityのonNewIntent](https://developer.android.com/reference/android/app/Activity#onNewIntent(android.content.Intent))

## 検証

```sh
# ネイティブ・プラグインのビルド、Debug／Release単体テストと静的解析。
./scripts/with-env.sh ./native/gradlew -p native \
  :feature:assembleDebug :cordova-plugin-dev:assembleDebug \
  :feature:testDebugUnitTest :feature:testReleaseUnitTest detekt detektFormatCheck

# JS変更を再導入してCordova APKをビルド。
npm run cordova:plugin -- --reinstall
npm run cordova:build

# 両ホストの品質・単体テスト・Android Lint。
npm run check:android
```

[FeatureLaunchRequestTest](../native/feature/src/test/kotlin/dev/example/nativefeature/FeatureLaunchRequestTest.kt)の10件でサービスID分岐・旧IDの拒否・JSON境界を、[FeatureDestinationTest](../native/feature/src/test/kotlin/dev/example/nativefeature/FeatureDestinationTest.kt)の5件で引数の保存形式を確認します。

以前の機器別構成での実機経路の記録は[機能別Navigation・ViewModelの検証](logs/intent-routing/viewmodel-navigation/README.md)にまとめています。以前のActivity独自保存による記録は[初期のIntent分岐](logs/intent-routing/README.md)、ホーム内サンプル遷移は[導入初期](logs/nfc-navigation/README.md)として区別します。

旧構成の検証ログは当時の履歴として保持します。サービス単位への変更後の確認は[ゴルフ場釣り銭機サービスの検証](logs/intent-routing/golf-service/README.md)に記録しています。
