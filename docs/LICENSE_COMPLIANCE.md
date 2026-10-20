# ライセンス遵守の確認と維持

各ライセンスの対応、対応判断、根拠となる条文の抜粋と原文リンクは[ライセンス対応表](LICENSE_RESPONSE_MATRIX.md)を参照してください。

2026-10-18に、通常のCordova Androidアプリのdebug/release構成と、Git管理されたソース配布物を確認しました。

2026-10-19にImmutable Collections 0.3.8を追加し、この時点の外部実行時依存は45件です。追加したJVM ArtifactはApache-2.0で、上流のLICENSE全文が保存済みの`Apache-2.0.txt`とバイト単位で一致することを確認しました。ソースのJetBrains著作権表示を`attributions.txt`へ追加し、出典・ハッシュを`runtime.json`へ記録しています。既存44件のArtifactハッシュは変わっていません。再ビルドした両APKの通知が管理元と一致し、45件すべての座標を含むことを確認しました。[当時の追加内容](IMMUTABLE_COLLECTIONS.md)

## 通常依存を追加した後の確認（2026-10-19）

Kotlinx Datetimeと想定する全モジュール依存を追加し、現在の通常APKはDebug 115件・Release 112件、両者の和集合115件です。Compose Tooling関連の3件はDebug専用で、台帳の各componentに`variants: ["debug"]`を記録しています。検証はvariantごとの版・ハッシュ・件数を照合し、Debug専用依存がReleaseへ混入しても失敗します。

追加・更新した依存の公式POM、ソースJARのヘッダー、AAR／JARと内部JARのLICENSE・NOTICEを確認しました。Apache-2.0のほか、以下を原文・出典とともに`native/third-party-licenses/`へ保存し、同梱通知へ反映しています。

- `kotlin-result`：ISCのLICENSEをJARから保存。
- DataStore内の再配置Protobuf：BSD-3-Clause。1.1.7のリリースコミットで元のProtobuf 4.28.2を確認し、同版のLICENSEを保存。
- OkHttpのPublic Suffix List：MPL-2.0。上流NOTICE・MPL全文と、上流タグとJARで一致する圧縮テーブルを展開したルール原文を同梱。例外ルールを`!`で表示し、ルール自体は変更していません。
- JSR-305の`javax.annotation.concurrent`内の4注釈：ソースが示すCC-BY-2.5、Brian Goetzの帰属、原文リンクを保持。CC公式リポジトリのHTML原本を保存し、本文をテキスト化して同梱。
- Jakarta InjectとCoroutines 1.8.1：選択された版のNOTICEへ更新。`javax.inject:1`はPOMにライセンス宣言がないため、同版ソースのApache-2.0ヘッダーを根拠として記録。

追加文書は`runtime.json`の`licenseDocuments`にSHA-256と出典を記録しています。コピーを変更して生成し直しても、レビュー済みハッシュと一致しなければ検証は失敗します。現在の[APK照合結果](license-apk-check.json)、[依存配置と検証](DEPENDENCIES.md)も参照してください。

## 初回確認結果と対応

- Cordova Android 14.0.1とHello Worldテンプレート7.0.0のLICENSE・NOTICEが、管理対象のソースとAPKへの同梱物に不足していました。原文を`native/third-party-licenses/`に保存し、ルートの`LICENSE`で適用範囲を説明し、`NOTICE`とAPK内の`assets/www/licenses/THIRD_PARTY_NOTICES.txt`へ反映しました。
- 解決済みの外部実行時依存は44件です。debug/releaseの座標・Artifactハッシュは同じでした。各版の公式Maven POMのライセンス宣言はApache-2.0です。Guava ListenableFuture 1.0は親POM `guava-parent:26.0-android`から宣言を継承します。座標、Artifact SHA-256、POM URL・SHA-256、宣言元を`native/third-party-licenses/runtime.json`に保存しました。
- POMだけでは不足する追加条件も確認しました。Kotlin 1.9.24標準ライブラリのJVM数学関数にはBoost由来のコードがあります。Boost全文、GWT・Guava由来の帰属表示、KotlinのCOPYRIGHT・NOTICE、Coroutines 1.6.4のNOTICEを収録しました。根拠は版を固定した`native/third-party-licenses/kotlin-licensing-README.md`と`upstream-sources.json`です。KotlinのNOTICE原文はCompiler distributionについての表記を含みますが、改変せず帰属の資料として保持しています。
- 外部AAR/JARとその内部JARのライセンス文書も確認しました。WebKitの`META-INF/androidx/webkit/webkit/LICENSE.txt`を原文保存しました。
- 標準テンプレートの既存ヘッダーを保持しています。ライセンス画面とトップ画面のリンクは設けず、ライセンス全文と必要なNOTICEを読めるテキストとしてAPKに同梱します。
- Gradle Wrapperのスクリプトの著作権表示と、JAR内の`META-INF/LICENSE`を保持しています。独自に書かれたworkspaceコードへの新しいライセンス付与は行っていません。
- APKの確認時、既存の通常ビルドはCompose依存がない状態でCompose Compilerが有効になっており、`feature`の通常Activityソースのコンパイルに失敗しました。Composeを使う`verifyCatalog=true`の検証時だけCompilerを有効にする設定へ修正しました。通常APKへの依存追加はありません。

## ビルド時の確認

生成・検証はPOSIXシェルの[`scripts/licenses.sh`](../scripts/licenses.sh)で行います。JSONの解析には`jq`、SHA-256の計算には`shasum`または`sha256sum`を使用します。ライセンス処理自体にはNode.jsを使用しません。`jq`がない環境では、インストールしてから以下を実行してください。

```sh
npm run licenses:generate  # 管理された原文と台帳からNOTICE・同梱テキストを生成
npm run licenses:check     # 原文コピー、Cordovaの版・通知、生成物を照合
npm run licenses:verify    # Gradleでdebug/release依存を解決し台帳と照合
npm run test:licenses
npm run test:hooks
npm run cordova:build
```

`native/gradle/licenses.gradle`を共有app設定から適用し、`:app:preBuild`が`verifyThirdPartyLicenses`を必ず実行します。新しい座標、同じ座標で異なるArtifact、削除された依存、座標のないローカルArtifact、Cordovaの版・LICENSE・NOTICEの変更、追加のCordovaプラグイン、未更新の同梱テキストがあれば失敗します。生成platformの配布用assetsも原文と照合し、prepare漏れを検出します。台帳の再生成だけで自動承認する処理はありません。

`exportThirdPartyInventory`の出力は、生成platformの`app/build/reports/licenses/runtime-artifacts.json`です。ローカルキャッシュの絶対パスはこのGit管理外レポートだけに出力し、同梱テキスト・管理台帳には含めません。`platforms/android`を作り直しても既存hook経由で検証を復元します。

## 依存関係を変える場合

1. `:app:exportThirdPartyInventory`で新しい通常構成の実行時依存を取得します。
2. **推移的依存も含め**、使用版の公式POM、上流LICENSE・NOTICE、AAR/JARと内部JAR、必要なソースの帰属を確認します。POMの単一ライセンス宣言だけで完了としません。
3. 必要な原文を`native/third-party-licenses/`へ保存し、出典と台帳を更新します。ライセンスが変わる場合、同梱テキスト生成処理の対象文書も更新します。
4. `licenses:generate`、`licenses:verify`、ビルドを行い、配布するAPK内の`assets/www/licenses/`を照合します。

## 対象範囲

Catalogの未使用ライブラリ、JUnitなどのテスト依存、`verifyCatalog=true`で追加する検証用ソース、Cordova CLI・Gradle・detekt・JGitなどの開発ツールは、通常のAPKの台帳に含めません。検証用APKや、`node_modules`・`.tools`・キャッシュを含む配布には別途その全依存の確認が必要です。これらは通常のGitソース配布にも含まれません。

この検証は現在のdebug/release構成を対象にしています。製品フレーバー、追加のローカルAAR/JAR、手動配置のJavaScript、画像・フォント、別の配布形態を追加する場合は確認範囲も拡張してください。Android SDK本体や端末のWebViewを、このアプリが同梱するライブラリとして扱ってはいません。

アプリの`package.json`の`license: Apache-2.0`は生成元テンプレートの情報として保持しています。リポジトリ全体や今後追加する独自コードの公開ライセンスを決めるものではありません。

## 実行確認

2026-10-19にライセンス処理とテストをシェルへ移行しました。通知テキストは既存の生成物とバイト単位で一致しています。シェル版の22テストで依存の追加・削除、Artifact名・ハッシュ、原文・コピー、Cordovaの版・プラグイン、生成物・prepare済みassetsの不整合と壊れたJSONの拒否を確認しました。空白・日本語を含むパスと呼び出し元からの相対パスも検証しています。既存Cordova hookの4テストと、Gradleからの`verifyThirdPartyLicenses`も成功しました。

移行後の`:app:assembleDebug :app:assembleRelease`も成功し、両APK内の通知テキストが管理元とバイト単位で一致することを確認しました。

以下は初回確認時の記録です。

- `licenses:check`、ライセンス検証の7テスト、既存Cordova hookの4テストに成功しました。
- nativeの`:check`（既存の静的解析・整形検査）と`:verifyNativeIntegration`に成功しました。
- Cordova prepare後、`:app:assembleDebug :app:assembleRelease`に成功し、ビルド前のライセンス検証が実行されることを確認しました。
- 通常の`./scripts/cordova.sh build android`も成功し、`after_prepare`・`before_compile`の復元処理とライセンス検証を通過しました。
- debug/release APK内のトップHTML・通知テキストが管理元とバイト単位で一致し、44件の座標とBoost全文を含むこと、ライセンス画面のHTMLとリンクがないことを確認しました。現在のパスとSHA-256は[APK照合結果](license-apk-check.json)に更新しています。release APKは未署名です。
- 実機・エミュレーターでの画面操作は今回の検証には含めていません。

根拠: [Apache License 2.0 第4条](https://www.apache.org/licenses/LICENSE-2.0.html)、[Cordova Android 14.0.1 LICENSE](https://github.com/apache/cordova-android/blob/14.0.1/LICENSE)、[Kotlin 1.9.24の第三者コード一覧](https://github.com/JetBrains/kotlin/blob/v1.9.24/license/README.md)。
