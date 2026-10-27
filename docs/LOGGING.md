# Timberのセットアップ

Timber 5.0.1を共通bundleの`api`で公開し、feature・data・Cordovaプラグインから`Timber.d`／`Timber.w`／`Timber.e`を呼び出せるようにしています。

## 初期化

[TimberInitializer](../native/feature/src/main/kotlin/dev/example/nativefeature/logging/TimberInitializer.kt)を[featureのManifest](../native/feature/src/main/AndroidManifest.xml)へ登録し、AndroidX Startup 1.2.0からアプリ起動時に実行します。Cordovaと開発用`dev-app`の両方へManifestが統合されます。Timber自体はApplicationでの初期化を必要とせず、[HiltのApplication](HILT.md)と独立して動作します。

[TimberLogging](../native/feature/src/main/kotlin/dev/example/nativefeature/logging/TimberLogging.kt)が、実際のホストアプリの`ApplicationInfo.FLAG_DEBUGGABLE`でTreeの登録を判断します。Library側のBuildConfigは使いません。

| ホストの状態 | この初期化の動作 |
| --- | --- |
| Debuggable・DebugTree未登録 | DebugTreeを1つ登録 |
| Debuggable・DebugTree登録済み | 既存のDebugTreeを利用 |
| Debuggableではない | Treeを追加しない |

ホストが登録済みのTreeは解除・置換しません。Releaseではこのセットアップによる出力先を追加しませんが、ホストが独自Treeを登録した場合はその設定に従います。Treeの管理はプロセスごとで、別プロセス向けの自動初期化は今回の対象にしていません。

AndroidX Startupの別のInitializer内からログが必要な場合は、そのInitializerの`dependencies()`に`TimberInitializer`を指定して順序を明示します。通常のActivity・ViewModel・Repositoryでは、初期化を繰り返し呼ぶ必要はありません。

## 呼び出しと確認

```kotlin
// 呼び出し元のクラス名をタグとして、書式引数付きで記録する。
Timber.d("Read completed: bytes=%d", bytes.size)

// 例外はメッセージへ文字列化せず、第1引数として渡す。
Timber.e(error, "NFC read failed")
```

Debugでは起動時に、タグ`NativeLogging`で`Timber logging initialized`を出力します。Android StudioのLogcat、または次のコマンドで確認できます。

```sh
# 起動時の初期化ログと、不正な画面起動要求のログを確認する。
adb logcat 'NativeLogging:D' 'FeatureLaunch:W' '*:S'
```

MainActivityの起動要求エラーとWebViewサンプルも共通のTimberを使用します。WebViewサンプルのDebug限定データ出力・メッセージ拒否ログの件数制限は維持しています。

## 検証

[単体テスト](../native/feature/src/test/kotlin/dev/example/nativefeature/logging/TimberLoggingTest.kt)で重複登録防止、既存Treeの再利用・保持、ReleaseでTreeを追加しないことを確認します。

2026-10-27にDebug／Releaseで各4件のテストが成功しました。nativeのdetekt・整形チェック、featureのDebug／Release Android Lint、開発用APKビルド、CordovaのDebug／Release APKビルド・Android Lint・detekt・整形チェックも成功しています。

API 35エミュレーターで開発用Debug・Cordova Debug・Cordova Releaseを起動しました。各APKにInitializerのmetadataが1件だけあること、同一プロセスでActivityを2回起動してもDebugの初期化ログは1件、Releaseは0件であることを確認しています。Releaseは検証用にDebugキーで署名したAPKを使用し、Debuggable属性が無効であることも確認しました。クラッシュはなく、検証後にエミュレーターを終了しました。[確認結果・ログ](logs/logging/README.md)。

## 参照

- [Timber 5.0.1](https://github.com/JakeWharton/timber/tree/5.0.1)：Treeの登録とログの呼び出し。
- [AndroidX Startup](https://developer.android.com/topic/libraries/app-startup)：Manifestからの初期化とInitializer間の依存。
- [Startup 1.2.0](https://developer.android.com/jetpack/androidx/releases/startup#1.2.0)：採用版のリリース情報。
