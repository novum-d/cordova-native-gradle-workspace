# Hiltのセットアップ

[ドキュメント一覧](README.md) · [依存配置](DEPENDENCIES.md) · [確認結果](logs/hilt/README.md)

Hilt 2.52・KSP 1.9.24-1.0.20を使い、Cordovaアプリと開発用APKの両方で同じfeatureを動かします。Kotlin 1.9.24は維持します。Navigation連携には`androidx.hilt:hilt-navigation-compose:1.2.0`を使います。

## Applicationとホストの関係

[NativeApplication](../native/host/src/main/kotlin/dev/example/nativehost/NativeApplication.kt)に`@HiltAndroidApp`を付け、両APKのApplicationとして登録します。`native/host`はGradleモジュールではなく、Applicationの元ソースを共有するディレクトリです。

HiltのApplicationは、注入先やModuleが見えるアプリモジュールでコンパイルする必要があります。そのため、このソースを[dev-app](../native/dev-app/build.gradle.kts)とCordovaのappの`main` source setへ直接登録し、それぞれのAPKでHiltのルートを生成します。featureやdataのLibrary内にはApplicationを置きません。

| ホスト | Applicationの登録 | Hilt・KSPとソースの登録 |
| --- | --- | --- |
| 開発用Debug APK | [Debug Manifest](../native/dev-app/src/debug/AndroidManifest.xml) | [dev-app/build.gradle.kts](../native/dev-app/build.gradle.kts) |
| Cordova Debug／Release | [config.xml](../CordovaComposeApp/config.xml)の`edit-config`をprepareが生成Manifestへ反映 | [settings-extras.gradle](../native/gradle/settings-extras.gradle)・[build-extras.gradle](../native/gradle/build-extras.gradle) |

プラグインの`kotlin-plugin`にはGradle設定を置きません。Cordova標準のランチャーActivityとKotlinPluginはHiltの注入先にしていません。Hiltを使うfeatureのActivityを、これまでどおり明示的Intentで起動します。

既存の別アプリへ移植するときは、そのアプリ固有のApplication処理も必要です。既存Applicationをそのまま置き換えず、ホスト側のApplicationへ`@HiltAndroidApp`を統合してください。1つのAPKに登録するApplicationは1つです。

## 画面・ViewModelへの注入

[featureのMainActivity](../native/feature/src/main/kotlin/dev/example/nativefeature/MainActivity.kt)を`@AndroidEntryPoint`にしました。[GolfHolderReadViewModel](../native/feature/src/main/kotlin/dev/example/nativefeature/services/golfchange/GolfHolderReadViewModel.kt)は`@HiltViewModel`と`@Inject constructor`を使い、Hiltから`SavedStateHandle`を受け取ります。

[GolfChangeNavigation](../native/feature/src/main/kotlin/dev/example/nativefeature/services/golfchange/GolfChangeNavigation.kt)では`hiltViewModel()`で取得します。Navigationの画面エントリに属するViewModelとなり、従来の`SavedStateHandle.toRoute()`で処理IDとargsを復元します。引数をHilt Moduleで別途提供する必要はありません。

[汎用タグ検出と業務Repository](NFC_READER_DESIGN.md)を追加しました。検出Repositoryと`CardRepositoryFactory`はdataの`NfcRepositoryModule`で`@Binds`します。業務生成の実装は`DefaultCardRepositoryFactory`で、内部の`NfcCardRepositoryFactory`は`@Inject constructor`で生成します。Factoryと読取Repositoryの実装は別ファイルです。検出要求の仲介は`@Singleton`、AndroidのReaderMode制御は`@ActivityScoped`です。MainActivityはメソッド注入で制御役を受け取ります。型番識別は`NfcTagIdentificationModule`で`@IntoSet`登録した判定器を`NfcTagProfileResolver`へ注入します。ResolverはSingletonですが、Tagや識別結果を保持しません。タグに紐づくRepository・引数なしの読取ユースケースは、検出ごとにFactoryで生成します。対象TagをSingletonへ保存せず、専用Session・独自Hilt Component・独自KSP生成は追加しません。

## Gradleとコード生成

feature・data・各APKでHiltとKSPを適用し、各モジュールの`ksp(libs.hilt.compiler)`へCompilerを登録します。Hilt本体の`api`公開だけでは、利用側のコード生成は有効になりません。

Pluginの版は[Version Catalog](../native/gradle/libs.versions.toml)で管理します。nativeはルートの版付きaliasから、CordovaはSettingsで登録したルートのbuildscript依存から読み込み、各モジュールでは版なしのPlugin IDで適用します。Serializationは従来どおりCatalog aliasで適用します。

AGPとHiltを同じルートのクラスパスで解決することで、Hiltが必要とするJavaPoet 1.13.0を利用します。AGPだけを親、Hiltを子に置くと、親にある1.10.0が優先され、`ClassName.canonicalName()`が見つからず`hiltAggregateDeps`で失敗しました。アプリ依存へのJavaPoet追加や、全構成への版の強制はしていません。

CordovaのSettingsでは、Catalogが利用可能になるクラスパス解決直前にPlugin依存を登録します。生成されたroot build.gradleを手編集する必要はありません。生成物を復元するには、workspaceルートで実行します。

```sh
./scripts/cordova.sh prepare android
npm run dev:build
./scripts/with-env.sh ./native/gradlew -p CordovaComposeApp/platforms/android \
  :app:assembleDebug :app:assembleRelease
```

Timberは[AndroidX Startupによる初期化](LOGGING.md)を継続します。HiltのApplicationへログの初期化を重複追加する必要はありません。

## 確認した範囲

2026-10-27に、開発用Debug・Cordova Debug／ReleaseのAPKビルド、featureのDebug／Release単体テスト、detekt・整形チェック、Android Lintを実行しました。API 35エミュレーターでは3種類のAPKすべてでHilt ViewModelによる画面表示と回転後の引数復元を確認し、クラッシュはありませんでした。TimberのDebug／Releaseの方針も維持しています。確認後にエミュレーターを終了しました。[検証記録](logs/hilt/README.md)を参照してください。業務RepositoryやNFC処理の注入を確認したものではありません。

## 一次資料

- [Hilt Gradle設定](https://dagger.dev/hilt/gradle-setup.html)：Plugin・KSPとコード生成。
- [Hilt Application](https://dagger.dev/hilt/application.html)：Applicationモジュールから注入対象が見える必要があること。
- [HiltとJetpack](https://developer.android.com/training/dependency-injection/hilt-jetpack)：ViewModel・SavedStateHandle・Navigationとの連携。
- [AndroidX Hiltのリリース](https://developer.android.com/jetpack/androidx/releases/hilt#1.2.0)：採用したNavigation連携の版。
