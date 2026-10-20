# 利用ライセンスの対応・判断根拠

確認日: 2026-10-20

通常のCordova Android APK（Debug／Release）に含めるライブラリ・内部コード・データと、Cordova由来のソースを対象にした対応判断の記録です。実行時依存はDebug 115件、Release 112件で、対象版・適用ライセンス・出典・ハッシュは[管理台帳](../native/third-party-licenses/runtime.json)に記録しています。

表の「対応判断」は、原文を今回の利用・配布方法に当てはめた判断です。「実装済み」は文書の同梱等を実装している意味で、自動チェックだけで全条項への適合を判定した意味ではありません。原文欄は判断箇所の短い抜粋で、条項全体は隣のリンクから確認できます。

## 対象と適用範囲

| ライセンス | 今回の対象 | 適用判断の根拠・保存した原文 |
| --- | --- | --- |
| Apache-2.0 | Cordova Android 14.0.1、Hello Worldテンプレート7.0.0、AndroidX／Compose、Kotlin、Coroutines、Datetime、Immutable Collections、Hilt、Retrofit、OkHttp本体など | 各版のPOM・ソースヘッダー・配布物のLICENSE／NOTICEを台帳に記録。[共通本文](../native/third-party-licenses/Apache-2.0.txt)、[Cordovaの出典](../native/third-party-licenses/cordova-sources.json)、[その他の出典](../native/third-party-licenses/upstream-sources.json) |
| ISC | `com.michael-bull.kotlin-result:kotlin-result-jvm:2.0.1` | 同版JARの`META-INF/LICENSE`。[保存原文](../native/third-party-licenses/kotlin-result-LICENSE.txt)、[上流の配布JAR](https://repo.maven.apache.org/maven2/com/michael-bull/kotlin-result/kotlin-result-jvm/2.0.1/kotlin-result-jvm-2.0.1.jar) |
| BSD-3-Clause | `androidx.datastore:datastore-preferences-external-protobuf:1.1.7`に再配置されたProtobuf | [DataStoreの再配置設定](https://android.googlesource.com/platform/frameworks/support/+/420da5f291449f11801e378c6789a328a06ec6cb/datastore/datastore-preferences-external-protobuf/build.gradle)と[同時点の版定義](https://android.googlesource.com/platform/frameworks/support/+/420da5f291449f11801e378c6789a328a06ec6cb/gradle/libs.versions.toml)からProtobuf 4.28.2を確認。[保存原文](../native/third-party-licenses/protobuf-LICENSE.txt) |
| Boost Software License 1.0（BSL-1.0） | Kotlin標準ライブラリ1.9.24の一部数学関数 | [Kotlin 1.9.24の第三者コード一覧](https://github.com/JetBrains/kotlin/blob/v1.9.24/license/README.md)が`MathJVM.kt`のBoost由来を記載。[保存原文](../native/third-party-licenses/kotlin-boost-LICENSE.txt)、[帰属表示](../native/third-party-licenses/attributions.txt) |
| MPL-2.0 | OkHttp 4.12.0に含まれるPublic Suffix List | 配布JAR内の[NOTICEの保存原文](../native/third-party-licenses/okhttp-public-suffix-NOTICE.txt)が適用を明記。[同版の圧縮テーブル](https://github.com/square/okhttp/blob/parent-4.12.0/okhttp/src/main/resources/okhttp3/internal/publicsuffix/publicsuffixes.gz)、[MPL本文](../native/third-party-licenses/MPL-2.0.txt)、[展開したルール原文](../native/third-party-licenses/okhttp-public-suffix-list.txt) |
| CC-BY-2.5 | JSR-305 3.0.2の`GuardedBy`、`Immutable`、`NotThreadSafe`、`ThreadSafe` | [同版ソースJAR](https://repo.maven.apache.org/maven2/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2-sources.jar)内の`javax/annotation/concurrent/`配下の4ファイルに適用表示あり。[著作者・帰属表示](../native/third-party-licenses/runtime-attributions.txt)、[保存本文](../native/third-party-licenses/CC-BY-2.5.txt) |

ライブラリ全体のPOMにApache-2.0と書かれていても、内部の第三者コード・データには別の条件があるため、上の6種類を扱います。MPLはOkHttp全体、CC-BYはJSR-305全体に一律適用するという判断ではありません。

## 対応・対応判断・原文の対応表

「実装済み」は保存文書と生成処理で確認できる対応、「運用確認」は配布方法や改変内容などを人が確認する対応です。

| ライセンス・項目 | 今回の対応 | 対応判断・理由 | 判断の根拠となる原文（抜粋） | 原文が記載されているリンク・条項 |
| --- | --- | --- | --- | --- |
| Apache-2.0：本文の提供 | 共通の`Apache-2.0.txt`と、上流独自の内容を含むCordova等の文書を同梱テキストへ収録 | **実装済み。** 配布先へ本文のコピーを渡す。共通本文の重複はまとめ、各ライブラリとの対応は台帳・同梱一覧で示す | “a copy of this License” | [第4条(a)](https://www.apache.org/licenses/LICENSE-2.0) |
| Apache-2.0：NOTICE | Cordova、Kotlin、Coroutines、Jakarta Inject等の必要な帰属表示を集約し、ルート`NOTICE`とAPK用テキストに収録 | **実装済み。** 上流のNOTICEがある対象は、その対象に関係する帰属表示を読める形で残す方針 | “attribution notices contained within such NOTICE file” | [第4条(d)](https://www.apache.org/licenses/LICENSE-2.0) |
| Apache-2.0：改変表示 | 上流由来のファイルを改変して配布する際に、変更した旨を当該ファイルへ表示する | **運用確認。** ライセンス本文の同梱とは別の条件。生成コードやテンプレートへの修正も、該当ファイルの変更表示を確認する | “You changed the files” | [第4条(b)](https://www.apache.org/licenses/LICENSE-2.0) |
| Apache-2.0：ソース中の表示 | Cordovaテンプレート等の既存ヘッダーを保持。第三者の著作権・帰属情報も保管 | **実装と運用確認。** ソース形式の派生物を配布する場合に、対象に関係する著作権・特許・商標・帰属表示を保持する | “copyright, patent, trademark, and attribution notices” | [第4条(c)](https://www.apache.org/licenses/LICENSE-2.0) |
| ISC：著作権・許諾文 | Michael Bullの著作権表示を含む`kotlin-result-LICENSE.txt`を収録 | **実装済み。** ISCという名前やURLだけで済ませず、著作権表示と許諾文を保持する | “copyright notice and this permission notice appear in all copies” | [ISC License・許諾段落](https://opensource.org/license/isc) |
| BSD-3-Clause：配布物の通知 | Googleの著作権表示・条件・免責文を含む`protobuf-LICENSE.txt`を収録 | **実装済み。** バイナリ配布では配布物に添付する資料等へこれらを再掲する。ソースを配布する場合も同じ情報を保持する | “copyright notice, this list of conditions and the following disclaimer” | [Protobuf 4.28.2 LICENSE・第1／第2条件](https://github.com/protocolbuffers/protobuf/blob/v28.2/LICENSE) |
| BSD-3-Clause：推薦・宣伝 | Googleや貢献者の名前を、無許可で自社製品の推薦・宣伝に使用しない | **運用確認。** 著作権者名の記載と、推薦・宣伝への利用を区別する。通知テキストの照合では宣伝物までは検査しない | “without specific prior written permission” | [同LICENSE・第3条件](https://github.com/protocolbuffers/protobuf/blob/v28.2/LICENSE) |
| BSL-1.0：本文と配布形態 | `kotlin-boost-LICENSE.txt`を著作権・帰属表示とともに収録 | **実装済み。** 本文には機械実行形式だけで配布する場合の例外があるが、今回のAPKでは例外だけに依存せず本文も同梱する。ソース配布を追加した場合も条件を確認する | “must be included in all copies of the Software” ／ “solely in the form of machine-executable object code” | [Boost Software License 1.0・第2段落](https://www.boost.org/LICENSE_1_0.txt) |
| MPL-2.0：対象ソースの提供 | OkHttp同版のテーブルから展開したルール原文を、MPL本文とともに同梱。出典・版・ハッシュを記録 | **同梱実装済み／提供形式は配布前確認。** MPL本文だけでは対象ソースを提供したことにならない。展開データが変更に適したSource Code Formとして十分かを確認し、不足する場合は同版の生成前データも提供する | “must also be made available in Source Code Form” | [第3.1条・第3.2条(a)](https://www.mozilla.org/en-US/MPL/2.0/#responsibilities) |
| MPL-2.0：入手方法の通知 | 同梱するルール原文に版・出典・MPL適用を記載し、上流NOTICEのソースURLも保持 | **実装あり／配布方法は運用確認。** 配布先が対象版のソースを合理的な方法で取得できるよう案内する。上流の最新版URLだけに依存せず、今回同梱した対象版との対応を示す | “You must inform recipients” | [第3.2条(a)](https://www.mozilla.org/en-US/MPL/2.0/#responsibilities) |
| MPL-2.0：通知の保持 | 上流NOTICEとMPL本文を保持。ルール自体は変更せず、展開・例外ルールの`!`表記を説明 | **実装と運用確認。** 配布する対象ソースのライセンス通知等の内容を保持する。形式変換を理由に必要な通知を削除しない | “may not remove or alter the substance of any license notices” | [第3.4条](https://www.mozilla.org/en-US/MPL/2.0/#responsibilities) |
| CC-BY-2.5：ライセンスの案内 | `CC-BY-2.5.txt`と原文URLを同梱 | **実装済み。** 第4条(a)の本文コピーまたはURIの提供を、両方を収録する方法で対応する | “include a copy of” ／ “the Uniform Resource Identifier for, this License” | [第4条(a)](https://creativecommons.org/licenses/by/2.5/legalcode.en) |
| CC-BY-2.5：帰属表示 | `runtime-attributions.txt`に4注釈の名称、Brian Goetzの著作権表示、ライセンスURL、公式サイトを記載し同梱 | **同梱実装済み／表示方法は運用確認。** 提供されている著作者名・著作権表示・作品名・関連URIを保持する。媒体に応じた合理性と、他の同等の著作者表示との扱いも確認する | “keep intact all copyright notices” ／ “the name of the Original Author” | [第4条(b)](https://creativecommons.org/licenses/by/2.5/legalcode.en) |
| CC-BY-2.5：派生物の表示 | 現在は対象の4注釈のソースを改変していない旨を帰属表示へ記録。改変時に再確認 | **条件付き・運用確認。** 派生物を作る場合は、元の作品を利用したことが分かるクレジットを加える。既存の未改変という説明をそのまま使わない | “Derivative Work” | [第4条(b)](https://creativecommons.org/licenses/by/2.5/legalcode.en) |

上記は今回の対応を判断した条項です。免責文や特許・商標等を含むライセンス全文は省略せず保存・同梱し、抜粋だけを配布する運用にはしません。

## 保存・生成・APKへの配置

| 段階 | 場所・処理 | 確認すること |
| --- | --- | --- |
| 管理元 | [`native/third-party-licenses/`](../native/third-party-licenses/) | 原文、著作権・帰属表示、出典、対象版、ハッシュを管理する |
| 生成 | [`scripts/licenses.sh`](../scripts/licenses.sh)の`generate` | ライブラリ一覧・原文・帰属表示からルート[`NOTICE`](../NOTICE)と[APK用テキスト](../CordovaComposeApp/www/licenses/THIRD_PARTY_NOTICES.txt)を生成する |
| Cordova prepare | `www/licenses/` → `platforms/android/app/src/main/assets/www/licenses/` | 生成済みテキストをAndroidのassetsへコピーする |
| APK | `assets/www/licenses/THIRD_PARTY_NOTICES.txt` | ライセンス本文・対象一覧・帰属表示・対象データが配布物に入る |
| ビルド前の検査 | [`native/gradle/licenses.gradle`](../native/gradle/licenses.gradle) → `licenses.sh check` | 解決済みの実行時依存と台帳、版・ハッシュ、生成テキスト、prepare済みassetsの一致を確認する |

現在はアプリ内のライセンス画面を設けず、テキストをAPKへ同梱しています。この格納先は本プロジェクトの実装上の選択で、ライセンスが指定したディレクトリではありません。表中のMPLのソース入手方法やCC-BYの帰属表示については、実際の配布先・表示手段でも条件を満たすかを配布前に確認し、必要に応じて画面や配布ページにも案内を追加します。APK内にファイルが存在することだけで、全条件への対応完了とは判断しません。

## 自動検査の範囲と更新時の扱い

自動検査は、レビュー済みの文書・依存関係と、今回のビルド内容が一致するかを確認します。新しいライセンスの法的な解釈、宣伝物の表現、アプリ利用規約が第三者ソースの権利を制限していないか、ソース入手方法・帰属表示の利用者にとっての分かりやすさは自動判定しません。後二者の根拠は[MPL第3.2条(b)](https://www.mozilla.org/en-US/MPL/2.0/#responsibilities)と[CC-BY第4条(a)・(b)](https://creativecommons.org/licenses/by/2.5/legalcode.en)です。

依存ライブラリを追加・更新した場合は、推移的依存と内部の第三者コード・データも確認し、この表と台帳・保存原文を更新します。その後、以下を順番に実行します。

```sh
npm run licenses:generate
npm run cordova:prepare
npm run licenses:verify
```

最終的な配布物は別途ビルドし、APK内の通知テキストを生成元と照合します。このドキュメント作成時にはAPKの再ビルドは行っていません。過去のビルド・APK照合結果は[ライセンス遵守の記録](LICENSE_COMPLIANCE.md)と[APK照合結果](license-apk-check.json)を参照してください。

開発ツール（Cordova CLI、Gradle、detekt、JGit等）やテスト専用依存は、通常APKの対応表には含めません。これらの実体を配布する場合、製品フレーバーを追加する場合、画像・フォント・手動配置のJavaScript等を追加する場合は、その配布対象について別途確認します。独自のworkspaceコードに新しいライセンスを付与する文書でもありません。
