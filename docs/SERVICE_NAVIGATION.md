# サービス単位の起動要求と画面・引数の復元

[ドキュメント一覧](README.md) · [KotlinプラグインのAPI](../kotlin-plugin/README.md)

この文書は、ネイティブ画面を起動する現在の暫定実装と検証記録を説明します。WebView主体の画面遷移・クライアントAPI・ゴルフ場釣り銭機サービスの構成方針は[CLIENT_API_SPEC.md](CLIENT_API_SPEC.md)を参照してください。通常のAPI実行にNavigationを必須とする仕様ではありません。

## 呼び出しの流れ

CordovaのWebから`{id, args?}`を渡し、KotlinPluginが検証した要求を明示的Intentへ格納します。Activityは要求を一度だけNavigationへ渡し、ゴルフ画面のViewModelが`SavedStateHandle`から処理ID・argsを受け取ります。通信サンプルは画面引数を使用しません。

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
| `golf-change.read-holder` | ゴルフ場ID・ホルダー番号 読み取り画面 | GolfCardReadViewModel |
| `home` | 確認用のホーム画面 | 引数を使用しないためなし |
| `sample.webview` | 手書きHTMLの双方向通信サンプル | 保存する画面引数なし。WebView接続は画面の寿命で管理 |

各機能画面は画面名・処理ID・argsを中央に表示し、「戻る」でCordovaへ戻ります。現在の確定仕様はゴルフ場ID・ホルダー番号の読み取りです。この2項目は読み取りによって取得する値で、Webからの必須引数にはしていません。[SLI判定・座標読取とユースケース](NFC_READING.md)を追加しています。ゴルフ画面は汎用のタグ検出ユースケースを使い、同じカードから2項目を取得して表示します。UI・ViewModelからRepositoryを直接呼びません。Webへの業務結果返却は未接続です。

NFC書き込みは対象外です。`nfc.read`・`nfc.write`・`camera.capture`は受け付けず、サービス名と処理を表すIDを使います。本人確認（マイナンバー）は別サービスとして追加する想定ですが、処理ID・画面・引数が未確定のため、今回の受付や画面には追加していません。

MainActivityは`@AndroidEntryPoint`、GolfCardReadViewModelは`@HiltViewModel`で、画面内の`hiltViewModel()`から生成します。Navigationのエントリに属する`SavedStateHandle`をHiltが提供します。[Hiltのホスト設定](HILT.md)を参照してください。

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

サンプルWebには、ゴルフ場釣り銭機サービス・確認用ホーム・[WebView通信サンプル](WEBVIEW_BRIDGE_SAMPLE.md)を呼ぶボタンを置いています。未知のIDや不正な要求はエラーコールバックへ返し、Activityを起動しません。ネイティブ画面内で仮の処理IDを生成する操作はありません。

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

`native/feature/src/main/kotlin/dev/example/nativefeature/`を、表示・遷移・ユースケース・エラー・モデルの役割で整理しています。各役割の配下を、業務・サービスごとに分けます。

| 配置先 | 責務 |
| --- | --- |
| [MainActivity.kt](../native/feature/src/main/kotlin/dev/example/nativefeature/MainActivity.kt) | Intentの受け取り・検証、一時的なコマンドの受け渡し |
| [launch/](../native/feature/src/main/kotlin/dev/example/nativefeature/launch/) | 起動要求のJSON変換と処理IDの解決 |
| [navigation/](../native/feature/src/main/kotlin/dev/example/nativefeature/navigation/) | 共通NavHost、型付きルート、JSON用NavType |
| [navigation/golfchange/](../native/feature/src/main/kotlin/dev/example/nativefeature/navigation/golfchange/) | ゴルフ場釣り銭機サービスの画面登録・前面表示中の状態購読 |
| [ui/golfchange/](../native/feature/src/main/kotlin/dev/example/nativefeature/ui/golfchange/) | ゴルフの読取画面とViewModel |
| [ui/home/](../native/feature/src/main/kotlin/dev/example/nativefeature/ui/home/) | 確認用ホームの表示 |
| [ui/webview/](../native/feature/src/main/kotlin/dev/example/nativefeature/ui/webview/) | WebView通信のサンプル画面 |
| [webview/](../native/feature/src/main/kotlin/dev/example/nativefeature/webview/) | 検証用WebView・WebMessageの解析・接続と読み込み制御 |
| [ui/common/](../native/feature/src/main/kotlin/dev/example/nativefeature/ui/common/) | 画面名・処理ID・argsを中央に表示する共通レイアウト |
| [usecase/](../native/feature/src/main/kotlin/dev/example/nativefeature/usecase/) | 汎用NFC検出とゴルフの読取処理・ユースケース生成Factory |
| [err/](../native/feature/src/main/kotlin/dev/example/nativefeature/err/) | 発生元を示すユースケース固有エラー |
| [model/golfchange/](../native/feature/src/main/kotlin/dev/example/nativefeature/model/golfchange/) | 読取結果イベントと画面状態 |

NavHostとNavControllerは一つです。`golfChangeNavigation()`でサービス単位に画面を登録し、ViewModelは各NavBackStackEntryに属します。Activity共有のViewModelで引数を使い回しません。

本人確認など新しいサービスを追加するときは、`navigation/<サービス名>/`に画面登録、`ui/<サービス名>/`に画面・ViewModelを置き、サービス名を含む処理IDと型付きルートを登録します。サービス固有のユースケース・エラー・モデルは`usecase/<サービス名>/`・`err/<サービス名>/`・`model/<サービス名>/`へまとめます。汎用タグ検出は`usecase/nfc/`・`err/nfc/`で管理します。NFC・Cameraは各サービスから利用する機器処理として扱い、共有できる処理は実装する段階で共通化します。

共通のUI部品は`ui/common/`へ置きます。`FeatureRequestLayout`は表示用のレイアウトなので、ユースケースや起動処理と同じcommonフォルダへ混在させません。`launch/FeatureLaunchRequest.kt`は全サービス共通の起動境界として、要求JSONの変換・検証と型付きルートの選択をまとめます。Activityからは`usecase/launch/ResolveFeatureLaunchUseCase`を経由してこの処理を呼び、Resultの成功値をNavigationへ渡します。

共通NavHostは`FeatureNavHost`、共通レイアウトは`FeatureRequestLayout`と呼びます。ゴルフ画面は2項目の取得を表す`GolfCardReadScreen`・`GolfCardReadViewModel`とし、読取結果の`GolfCardReadEvent`・画面状態の`GolfCardReadUiState`は個別ファイルへ分けています。起動ID・Intentのキー・JSONの形式・型付きルートのシリアル名は維持しています。

テストは`launch/`・`navigation/`・`usecase/golfchange/`など、本体の責務に対応する配置です。Main差し替え用のRuleと共用の模擬Repository等は`src/test/.../testing/`へまとめています。

## 起動要求のROPとUI境界

[ResolveFeatureLaunchUseCase](../native/feature/src/main/kotlin/dev/example/nativefeature/usecase/launch/ResolveFeatureLaunchUseCase.kt)は`Result<FeatureDestination, ResolveFeatureLaunchErr>`を返します。要求の存在確認・JSON解析・画面解決を`toResultOr`・`andThen`で接続し、失敗した段階で後続処理を止めます。

| エラー型 | 発生箇所 |
| --- | --- |
| `ResolveFeatureLaunchErr.Request.Missing` | Intentの要求Extraが未指定 |
| `ResolveFeatureLaunchErr.Request.InvalidJson` | JSON構文、必須項目、値の型が不正 |
| `ResolveFeatureLaunchErr.Destination.UnsupportedId` | 解析済みの処理IDに対応する画面がない |

`MainActivity.acceptRequest()`は`Result<Unit, ResolveFeatureLaunchErr>`を返します。`onSuccess`で一時的な遷移コマンドを設定し、`onFailure`で原因をログへ記録してActivityを終了します。初回起動では`isErr`を確認して画面生成を止め、再要求では同じ成功・失敗処理を再利用します。Booleanを返す必然性はなく、以前は続行可否へ情報を縮約していました。Cordovaの`execute()`のBooleanは、actionを処理したかを通知するSDK契約として扱います。

`kotlin-result.runCatching`はThrowableを捕捉するため、JSON入力のIllegalArgumentExceptionなど、想定した例外だけを失敗値として扱います。取消・JVM Error・想定外の実装例外は伝播します。プラグインの起動、開発用assets読込、WebView送信・JSON／URL解析、ReaderModeの権限拒否もkotlin-resultの関数で扱います。

検出対象・検出要求・ReaderModeの後始末は、`runCatching`の直後の`also`で成功・失敗の両方に対して実行し、その後の`getOrElse`で元の失敗を再送出します。後始末を、例外を再送出する`mapError`・`onFailure`より後ろへ置きません。ReaderModeは権限拒否の通知・取消待機も外側の`runCatching`で囲み、待機中にキャンセルされても解除処理へ進む構成です。

```kotlin
runCatching {
    observe()
}.also {
    release()
}.getOrElse { throw it }
```

この書き方ではブロックから外側の関数を抜ける非局所returnを使いません。処理を早期終了する場合は`return@runCatching`などの局所returnでResultへ戻し、後始末を通過させます。通信接続のread／closeが両方失敗する場合の優先順位・suppressedの扱いは、既存の接続管理処理で維持しています。

[ResolveFeatureLaunchUseCaseTest](../native/feature/src/test/kotlin/dev/example/nativefeature/usecase/launch/ResolveFeatureLaunchUseCaseTest.kt)で、未指定・不正形式・未対応IDと、既知の画面・省略／空／構造化argsの保持を確認します。

両ホストのビルド・テスト・静的検査と、エミュレーターでの正常起動・不正要求・WebView通信を確認しました。[確認記録](logs/launch-rop/README.md)を参照してください。

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

[FeatureLaunchRequestTest](../native/feature/src/test/kotlin/dev/example/nativefeature/launch/FeatureLaunchRequestTest.kt)の10件でサービスID分岐・旧IDの拒否・JSON境界を、[FeatureDestinationTest](../native/feature/src/test/kotlin/dev/example/nativefeature/navigation/FeatureDestinationTest.kt)の5件で引数の保存形式を確認します。

以前の機器別構成での実機経路の記録は[機能別Navigation・ViewModelの検証](logs/intent-routing/viewmodel-navigation/README.md)にまとめています。以前のActivity独自保存による記録は[初期のIntent分岐](logs/intent-routing/README.md)、ホーム内サンプル遷移は[導入初期](logs/nfc-navigation/README.md)として区別します。

旧構成の検証ログは当時の履歴として保持します。サービス単位への変更後の確認は[ゴルフ場釣り銭機サービスの検証](logs/intent-routing/golf-service/README.md)に記録しています。
