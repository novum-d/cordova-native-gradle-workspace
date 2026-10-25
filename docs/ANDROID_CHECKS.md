# Gradle・Android Lintの確認結果（2026-10-19）

後続の[Immutable Collections追加](IMMUTABLE_COLLECTIONS.md)でも全チェックと通常APKビルドを再実行しました。Lint件数は下表と同じで、全6構成でエラー0です。[追加後のログ](logs/immutable-collections/immutable-checks.log)、[追加後の件数](logs/immutable-collections/immutable-lint-results.json)

日時API・通常依存の追加後にも再実行しました。現在の件数・依存配置は[追加記録](DEPENDENCIES.md)を参照してください。

## 修正した記述

| 対象 | 修正内容 |
| --- | --- |
| feature・dataのKotlinコンパイラ設定 | `android.kotlinOptions`から型付きの`KotlinJvmCompile.compilerOptions.jvmTarget`へ移行。CatalogのProviderから設定し、Debug／ReleaseともJVM 11であることを検証 |
| CordovaアプリのLint設定 | `abortOnError=true`、`checkDependencies=true`を設定。Cordovaの後続設定で上書きされないよう、共有スクリプトの`androidComponents.finalizeDsl`で適用し、最終値も検証 |

置き換えの根拠は[Kotlinの型付きcompilerOptions](https://kotlinlang.org/docs/whatsnew18.html#exposing-kotlin-compiler-options-as-gradle-lazy-properties)、[AGPのDSL確定処理](https://developer.android.com/build/extend-agp)です。Kotlin 1.9.24でも利用できるAPIで実ビルドを確認しています。

## 再実行

```sh
npm run check:android
```

[`scripts/check-android.sh`](../scripts/check-android.sh)がCordova prepareを実行した後、native・Cordovaを順番に検証します。両方から同じnativeモジュールのビルドディレクトリを利用するため、並列には実行しません。

- detekt・読み取り専用の整形チェック
- nativeのfeature・dataでDebug／Releaseの単体テスト
- feature・data・CordovaアプリのDebug／Release Android Lint。アプリ側ではCordovaLibを含む依存先もチェック
- native専用のcordova-plugin-devもDebug／Release Android Lintで確認。以下の過去の件数表には含まれない
- `--warning-mode all`によるGradle非推奨警告の表示

アプリのインストール・起動は行いません。エミュレータで起動確認する場合は、別途`npm run cordova:run`を実行します。

実行ログは[android-gradle-check.log](logs/android-checks/android-gradle-check.log)です。Lintのエラーを無視する設定、baseline、警告の一括抑制は追加していません。警告はビルドを止めず、エラーで失敗させます。

## 実測結果

| 対象 | Debug | Release |
| --- | --- | --- |
| native feature | エラー0・警告0 | エラー0・警告0 |
| native data | エラー0・警告42 | エラー0・警告28 |
| Cordova app（依存先を含む） | エラー0・警告61 | エラー0・警告47 |

件数はXMLレポートの生の指摘数で、同じCatalog項目への重複を含みます。アプリの件数には依存ライブラリの指摘も含まれ、上記を合算するものではありません。内訳と相対パスは[android-lint-results.json](logs/android-checks/android-lint-results.json)に保存しています。

HTMLレポートの出力先は、各variantの`debug`または`release`に応じて以下になります。

- `native/feature/build/reports/lint-results-<variant>.html`
- `native/data/build/reports/lint-results-<variant>.html`
- `CordovaComposeApp/platforms/android/app/build/reports/lint-results-<variant>.html`

Lintの失敗も実行確認しました。一時initスクリプトで既存の`UnusedResources`警告だけをエラーへ変更し、`:app:lintDebug`が失敗することを確認しています。[期待どおり失敗したログ](logs/android-checks/android-lint-error-gate.log)の後、通常設定で再実行して成功しています。この一時的な重大度変更は通常のビルド設定には含めていません。

追加で、Catalog全体のライブラリ129 Artifact・Plugin 155 Artifactの解決と、型付きcompilerOptionsを使ったKotlin／Composeの検証ビルドも成功しました。[コンパイルログ](logs/android-checks/compiler-options-probe.log)

検証用ソースを除いた通常のDebug／Release APKの[ビルド](logs/android-checks/android-normal-build.log)も成功しました。検証用assetsの除外、Cordova hookの4テスト、Git hookセットアップの5テストも確認しています。

## 残っている警告

- **AGP 8.7.3内部のGradle非推奨API**：`isCrunchPngs`・`isUseProguard`・`isWearAppUnbundled`のBoolean getter。管理スクリプトで定義したものではなく、AGP内部の実装です。
- **Cordova 14.0.1生成Gradleの旧Groovy記法**：`namespace`・`buildConfig`・`abortOnError`の空白区切り代入。共有設定でLintの最終値は修正していますが、生成元の旧記法による警告は残ります。AGP／Cordovaの更新は別途互換性検証が必要です。
- **依存更新の案内**：native dataの指摘は`AndroidGradlePluginVersion`と`GradleDependency`です。現在のCordova／Kotlin／SDK基盤を維持するため固定している版で、採用理由は[README](../README.md#採用バージョンと変更理由)に記録しています。
- **CordovaのJava・標準画像**：既定Localeの利用、リソース名による参照、SSLエラー処理、アイコンの形状・サイズ・重複、スプラッシュ画像などです。`UnusedResources`は動的に参照する`config.xml`への指摘です。SSLの指摘箇所は`FLAG_DEBUGGABLE`の場合に`handler.proceed()`を呼び、それ以外は親クラスへ委譲するCordova本体の実装でした。これらの警告を抑制したり、生成ソース・依存パッケージを直接書き換えたりはしていません。
- **SDK XMLの世代差**：現在のAGPが理解するXML形式と、インストール済みSDKメタデータの世代差による警告です。今回のLint・ビルドは完了しています。

管理するスクリプト由来の`Project.exec`・`Task.project`・`getDependencyProject()`の非推奨警告は、修正後の対象タスクの実行では出ていません。ビルド全体のConfiguration Cache対応やGradle 9対応を保証する変更ではありません。
