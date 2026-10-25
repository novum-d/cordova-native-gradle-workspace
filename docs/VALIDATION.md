# 検証結果（2026-10-17）

2026-10-19に、Cordova CLIによるエミュレーター実行とComposeの表示・操作も確認しました。[実行記録](COMPOSE_RUNTIME.md)を参照してください。以下は初回からの検証記録です。依存選定用の独自タスク・コンパイル用fixture・Tooling API・hookなどの検証専用コードは削除済みで、結果表・ログは当時の証跡として保持しています。

`~/repos/cordova-native-gradle-workspace`で、Cordova Android 14の基盤を維持したビルドを確認しました。

2026-10-19のGradle非推奨APIの修正、Debug／ReleaseのAndroid Lint、継続確認用コマンドは[追加の品質チェック結果](ANDROID_CHECKS.md)を参照してください。

## 結果

| 検証 | 結果 | 証跡 |
| --- | --- | --- |
| nativeのGradle設定読み込み・Wrapper生成 | 成功、Gradle 8.13 | [native-wrapper.log](logs/build-integration/native-wrapper.log) |
| nativeのAndroid V2モデル取得 | 成功、`:data`・`:feature`、それぞれDebug/Releaseの2variant | [native-tooling-sync.log](logs/build-integration/native-tooling-sync.log) |
| nativeの通常AARビルド | 成功、feature/dataともAndroid Library | [native-structure.log](logs/build-integration/native-structure.log) |
| Kotlin/Composeの実コンパイル | 成功、両Libraryで検証コードをコンパイル。Compose Compiler 1.5.14とBOM 2025.07.00を利用 | [native-verification.log](logs/build-integration/native-verification.log) |
| 全CatalogライブラリのArtifact解決 | 成功、推移依存を含め129 Artifact | [resolved-catalog-libraries.txt](logs/dependencies/resolved-catalog-libraries.txt) |
| 全Catalog PluginのArtifact解決 | 成功、推移依存を含め155 Artifact。未使用Pluginは適用しない | [resolved-catalog-plugins.txt](logs/dependencies/resolved-catalog-plugins.txt) |
| Cordova prepare | 成功、after_prepareで共有設定を復元 | [platform-regeneration.log](logs/regeneration/platform-regeneration.log) |
| Cordova側でも検証コードのコンパイル・APK生成 | 成功。AGP/Kotlinの既存クラスパスでLibraryをコンパイル | [cordova-probe-build.log](logs/build-integration/cordova-probe-build.log) |
| feature/data両方のassetsマージ | 成功、検証APKに両方のファイルを格納 | [cordova-probe-artifact-check.json](logs/build-integration/cordova-probe-artifact-check.json) |
| platform削除・14.0.1再追加 | 成功。主要ソース・設定・標準MainActivityの12ファイルのSHA-256一致 | [regeneration-check.json](logs/regeneration/regeneration-check.json)、[sources-before-regeneration.json](logs/regeneration/sources-before-regeneration.json) |
| 再生成後の通常Cordova Debugビルド | 成功、検証用プロパティを付けずにAPK生成 | [cordova-build.log](logs/build-integration/cordova-build.log) |
| 再生成後のReleaseビルド | 成功、署名前APKを生成。既定設定でのassembleReleaseでありR8有効化の検証ではない | [cordova-release-and-structure.log](logs/build-integration/cordova-release-and-structure.log) |
| APKホスト・基盤・依存方向の検証 | 成功。APKホストは`:app`だけ。JDK 17 / Gradle 8.13 / AGP 8.7.3 / Kotlin 1.9.24 / JVM 11 | [native-final-check.log](logs/build-integration/native-final-check.log)、[cordova-release-and-structure.log](logs/build-integration/cordova-release-and-structure.log) |
| CordovaのAndroid V2モデル取得 | 成功、app/CordovaLib/data/featureの4モジュール | [cordova-tooling-sync.log](logs/build-integration/cordova-tooling-sync.log) |
| 通常APKのManifest・検証コード除外 | 成功。minSdk 24、targetSdk 35、Launcherは既存Cordova MainActivityだけ。検証クラス・assetsは不在 | [final-artifact-check.json](logs/build-integration/final-artifact-check.json) |
| Cordova既定依存の維持 | AppCompat 1.7.0、WebKit 1.12.1、Kotlin stdlib 1.9.24 | [resolved-host-runtime.txt](logs/dependencies/resolved-host-runtime.txt) |
| Debug APKの署名検証 | 成功 | [apk-signature.log](logs/build-integration/apk-signature.log) |
| hookのNode.jsテスト | 4件成功。冪等性、既存設定保持、不足ファイル、破損マーカー、非Android対象を確認 | [hook-tests.log](logs/regeneration/hook-tests.log) |

Android StudioのUI上でSyncボタンを押す操作は未実施です。Gradle Tooling APIを通じてAndroid V2モデルを取得し、ライブラリとしてインポートできるGradle構成まで確認しています。実際にIDEを開く場合はWrapperとJDK 17を選択してください。

## 生成物

- [Debug APK](../artifacts/CordovaComposeApp-debug.apk): 3,442,820 bytes。Debug署名済み。
- [Release APK](../artifacts/CordovaComposeApp-release-unsigned.apk): 2,714,940 bytes。未署名。
- Cordovaの標準出力先: `CordovaComposeApp/platforms/android/app/build/outputs/apk/`
- Library AARの出力先: `native/feature/build/outputs/aar/`、`native/data/build/outputs/aar/`

SHA-256と実際のサイズは[最終Artifact検査](logs/build-integration/final-artifact-check.json)に保存しています。最終APKにprobe用クラス・assetsは含まれません。

## 現在の主な確認コマンド

workspaceルートから実行します。初回のCatalog全項目検証・fixtureの注入は廃止し、現在は本体のビルドと標準タスクを使います。

```sh
# nativeのLibraryとCordovaアプリを通常の依存でビルドする。
npm run native:build
npm run cordova:build
./scripts/with-env.sh ./native/gradlew -p CordovaComposeApp/platforms/android \
  :app:assembleRelease --console=plain --no-watch-fs

# 本体・テストの品質とKDocを確認する。
npm run check:android
npm run test:unit
npm run docs:kdoc
```

## 今回適用していないもの・未検証

- Hilt・KSPはCatalogへの定義とArtifact解決だけ。Application/Activity/Processor/DI生成を変更していません。
- ktlint Pluginは通常ビルドには適用していません。後続の[追加検証](VERSION_CHECKS.md)でPlugin 14.2.0・本体1.8.0を一時プロジェクトへ適用し、チェック・整形を実行しました。detektは適用済みです。TvAppを参照したKotlin DSL品質タスクと28ケースの実行検証は[Git hook検証](GIT_HOOKS.md)を参照してください。
- JUnit/AndroidX TestはCatalogへの定義と解決だけ。JUnit・Instrumentationのテストコードと実行環境は追加していません。Cordova hook検証の4件はシェルテストで、対象のJavaScript関数を呼ぶ部分だけNode.jsを使います。
- 既存業務コード・既存CordovaPluginは提供されていません。このworkspaceに追加Pluginはなく、実案件固有のMainActivity・Pluginとの統合は未検証です。[Plugin一覧](logs/build-integration/cordova-plugins.log)
- 端末・エミュレーターへのインストールや起動、画面遷移、カメラ・NFC、WebView通信の実行試験は今回の対象外です。
- 署名付きRelease配布、R8有効化、CI導入、DI導入は未対応です。
- kotlin-resultの旧1系から2.0.1へ移す既存ソースがある場合、API移行は別途必要です。このworkspaceでは実API参照のコンパイルだけを検証しました。

Cordova公式の基盤を優先したため、KGP 1.9.24に対するJetBrainsの完全サポート表の範囲とは一致しません。実際のビルドでは上記の組み合わせで成功しています。SDK XML世代差・Gradle 9非互換の非推奨API・Cordova既存Javaコードの警告は一部ログに残ります。これらを消すためのAGP/Kotlin/Gradle更新や互換性チェック無効化は行っていません。

## Android CLIとskillsの準備

先行作業として、Android CLIを公式配布の`1.0.16625370`へ更新し、提供される全34種類のスキルを`~/.codex/skills/`へ取り込みました（配布データには重複したIDがあるため36エントリー）。未取り込みIDがないことを確認しています。

Nix管理の既存CLIは自己更新できないため残し、`~/.local/bin/android`に公式版を導入しました。`~/.zprofile`で同ディレクトリを優先するようにしています。SDK 35とBuild Tools 35.0.0も導入済みです。

## 追加: TvAppのGit hookをGradleタスク化

[Git hookのKotlin DSL実装](GIT_HOOKS.md)で参照元・仕様・変更ファイル・実行方法を記録しています。TvAppのshellや既存JARは持ち込まず、公式detekt PluginとJGitのAPIを利用します。

- detekt/formatting 1.23.8、Twitter Compose rules 0.0.26、JGit 7.8.0.202609011348-rをbuild-timeで管理。
- native・Cordova両方で`check`・`preCommit`を実行可能。
- 一時Git repositoryの28ケースが成功し、全ケースでindex不変を確認。自動整形後の再試行、ステージ済み内容の整形検査、最後のHTML生成・自動表示も含みます。[結果](logs/git-hooks/cases/results.json)
- [nativeの品質タスク・ビルド](logs/git-hooks/git-hooks-native.log)、[Cordovaの品質タスク](logs/git-hooks/git-hooks-cordova.log)、[Cordovaビルド](logs/git-hooks/git-hooks-cordova-build.log)
- Hilt・KSP、Cordovaの基盤版、APKホストの構成は維持。
