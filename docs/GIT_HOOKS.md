# TvAppのdetekt hookをKotlin DSLのGradleタスクへ移植

2026-10-17に`~/repos/TvApp`を読み取り、`cordova-native-gradle-workspace`へ実装しました。TvApp本体・そのGit hook・indexは変更していません。

## TvAppで確認した処理

参照元は`.git/hooks/pre-commit`、`scripts/git-hooks/pre-commit`、ルート`build.gradle.kts`、`config/detekt/{detekt,formatting}.yml`、`gradle/libs.versions.toml`です。インストール済みhookと管理元スクリプトは同内容でした。

- stagedに削除以外の`.kt`がなければ解析をスキップ。
- stagedに`.kt`があれば、全対象ソースへ`detektFormat`と`detekt`を順番に実行。
- stagedのKotlinファイルについて、実行前後のworking-tree差分を比較。
- 整形で差分が増えたら失敗し、レビューと再ステージを要求。
- もともとの未ステージ差分が存在するだけなら失敗させない。
- `ktlintCheck`は独立したktlint実行ではなく、`detektFormatCheck`への互換名。
- detektのHTMLレポートだけを有効化。

TvAppのdetektは1.23.7、Twitter Compose rulesは0.0.26でした。AGP 9.2.1・Kotlin 2.2.10等のTvAppのビルド基盤は取り込まず、Cordova側のAGP 8.7.3・Gradle 8.13・Kotlin 1.9.24・JDK 17・JVM target 11を維持しています。detektはworkspaceで選定済みの1.23.8を使います。

## 実装

本体は[`native/gradle/git-hooks.gradle.kts`](../native/gradle/git-hooks.gradle.kts)です。

| 機能 | 実装 |
| --- | --- |
| Gitのstaged判定 | JGit APIのstatus.added/status.changed。deletedは含めない |
| Git repository探索 | FileRepositoryBuilder。workspaceのwork treeを確認 |
| 整形・解析 | 公式detekt Gradle Pluginの型付き`Detekt`タスク |
| 実行順序 | GradleのdependsOnで固定 |
| 変更検出 | Kotlin/Java APIで実行前後のファイルSHA-256を比較 |
| ステージ内容の整形検査 | JGitでindexの内容をbuild/tmpへ取り出し、毎回読み取り専用のDetektタスクで検査 |
| Git index | 読み取りのみ。自動stage・commit・resetはしない |
| パス | 文字列集合として扱い、空白・日本語をshellで分割しない |
| レポート | detekt標準の`build/reports/detekt/<task>.html`と、Git・整形処理の記録`build/reports/git-hooks/preCommit.txt` |

TvAppのshell、既存detektコマンド、手動取得したローカルJARは取り込んでいません。Git判定・整形・解析はJGitとdetektのAPIで行い、`git diff`や`java -jar`は呼びません。HTMLの表示だけは、Gradleの注入サービス`ExecOperations`でOSの起動コマンドを実行します。

detekt Plugin、detekt-formatting、Twitter Compose rules、JGitは通常のMaven座標からGradleが依存解決します。JARをリポジトリへコピーしたり、既存JARのmainを直接起動したりする構成ではありません。JGit 7.8.0.202609011348-rはGitコマンドを呼ばずにindexを扱うためのbuild-time依存です。これらはAndroidモジュールやAPKの依存には追加しません。

版の管理元は既存の`native/gradle/libs.versions.toml`です。`native/build.gradle.kts`とCordovaの共有`build-extras.gradle`から同じKotlin DSLを適用するため、両方のGradleルートから実行できます。

## タスク

| タスク | 処理 |
| --- | --- |
| `preCommit` | staged判定・内容取得→自動整形→変更検出→残存整形違反チェック→通常解析→ステージ内容の整形検査 |
| `showPreCommitReport` | preCommitでルール違反があった場合だけ、該当タスクのdetekt標準HTMLを自動表示 |
| `detektFormat` | 自動整形だけを明示実行。stagedの有無ではスキップしない |
| `detektFormatCheck` | 整形違反を検査し、ファイルは変更しない |
| `detekt` | 通常の静的解析。ファイルは変更しない |
| `ktlintCheck` | TvAppと同じ互換名。detektFormatCheckへ依存 |
| `check` | detektFormatCheckとdetektを実行する読み取り専用の品質ゲート |

`preparePreCommit`、`detektPreCommitFormat`、`checkPreCommitFormattingChanges`、`detektPreCommitFormatCheck`、`detektPreCommit`、`detektPreCommitStagedFormatCheck`はpreCommitの内部段階です。コミット前の確認には最終の`preCommit`を使います。

```sh
cd ~/repos/cordova-native-gradle-workspace

# nativeのGradleルートからHook処理を実行
./scripts/with-env.sh ./native/gradlew -p native preCommit

# CordovaのGradleルートから同じHook処理を実行
./scripts/with-env.sh ./native/gradlew -p CordovaComposeApp/platforms/android preCommit

# 書き換えを伴わない確認
./scripts/with-env.sh ./native/gradlew -p native check

# 自動整形のみ
./scripts/with-env.sh ./native/gradlew -p native detektFormat
```

`with-env.sh`はJDK 17等の実行環境を選ぶ既存の補助です。Gradleタスク本体がこのscriptを実行する構成ではありません。JDK 17を設定済みならnativeディレクトリで`./gradlew preCommit`を直接実行できます。

## 最後にHTMLレポートを開く

2026-10-21に、独自の結果一覧の生成を削除し、**lint・フォーマットルール違反があった場合だけ、detektが出力した標準HTMLをそのまま開く**方式に変更しました。独自HTMLの生成・detektレポートの加工は行いません。

| 停止した理由 | 開くdetekt標準HTML |
| --- | --- |
| 自動整形でファイルが変わり、確認・再ステージが必要 | `build/reports/detekt/detektPreCommitFormat.html` |
| 自動整形段階で補正できないルール違反 | `build/reports/detekt/detektPreCommitFormat.html` |
| 自動整形後の読み取り専用検査で残存する整形違反 | `build/reports/detekt/detektPreCommitFormatCheck.html` |
| 通常のlint・静的解析ルール違反 | `build/reports/detekt/detektPreCommit.html` |
| 再ステージ前など、index内の整形違反 | `build/reports/detekt/detektPreCommitStagedFormatCheck.html` |

nativeから実行した場合は`native/build/reports/detekt/`、Cordovaから実行した場合は`CordovaComposeApp/platforms/android/build/reports/detekt/`配下です。

`preparePreCommit.finalizedBy(showPreCommitReport)`と`mustRunAfter`により、予定された検査が終わってから表示します。detekt 1.23.8の`MaxIssuesReached`が失敗原因に含まれる場合、または自動整形で変更が生じた場合に限定しています。依存解決・設定不備・Git操作・detekt実行そのもののエラーは、ルール違反として扱いません。[detektの失敗原因の保持](https://github.com/detekt/detekt/blob/v1.23.8/detekt-gradle-plugin/src/main/kotlin/io/gitlab/arturbosch/detekt/invoke/DetektInvoker.kt)

準備段階で前回のpreCommit用detekt HTMLと、旧版の独自一覧`build/reports/git-hooks/preCommit.html`を削除します。今回実行していない段階の古いレポートは開きません。通常の`check`や`detektFormatCheck`を単独で実行する場合は、従来どおり自動表示せず、ログのレポートリンクから参照します。

macOSは`open`、Windowsは`rundll32.exe`、デスクトップのあるLinuxは`xdg-open`を使用します。macOSで既定のHTMLアプリが開けない場合はSafariを試します。起動失敗は警告とレポートへのリンクを出し、元のコミット可否の判定は維持します。

以下の場合はブラウザを起動しません。

- 検査が成功した場合
- ステージ済みKotlinがなく、検査自体をスキップする場合
- 設定不備など、ルール違反以外の理由で失敗した場合
- `CI=true`または`CI=1`
- `-PopenPreCommitReport=false`を指定した場合
- Linuxなどでデスクトップ環境を利用できない場合

自動表示だけを無効にするには、`~/.gradle/gradle.properties`または実行するルートの`gradle.properties`に`openPreCommitReport=false`を設定できます。detekt標準HTMLの出力は継続します。成功時にもdetekt自身はHTMLを出力しますが、ブラウザを起動しません。

## Gitコミット時の自動実行を初回設定する

各cloneで一度、ワークスペースのルートから実行します。

```sh
./scripts/setup-git-hooks.sh
```

セットアップはGitのhookディレクトリに実行可能な`pre-commit`を作成します。通常は`.git/hooks/pre-commit`です。hookはワークスペースのルートへ移動し、`./scripts/with-env.sh ./native/gradlew -p native :preCommit`を実行します。Gradleが成功すればコミットを続行し、失敗すればコミットを中止します。整形でファイルが変わった場合は、内容を確認して再ステージしてからコミットしてください。JDKなどは手動実行時と同じ環境準備が必要です。

同じhookが登録済みなら再実行できます。異なる既存hookやシンボリックリンクは上書きせず停止します。`core.hooksPath`が設定済みの場合も設定を変更せず停止するため、既存のhook側から上記のコマンドを呼び出してください。セットアップスクリプトはbootstrapからは自動実行しません。

セットアップとGitからの呼び出しは、一時リポジトリで検証済みです。検証専用コードは削除し、セットアップスクリプトと品質タスク本体を管理しています。

## 解析範囲とTvAppから調整した点

`native/feature/src/**/*.kt`・`native/data/src/**/*.kt`・`kotlin-plugin/src/**/*.kt`を対象にします。`java`・`kotlin`双方の配置、test、androidTest、その他のsource setを含め、build/generatedは除外します。プラグインは管理元だけを検査し、Cordova platform内の導入先を重複して検査しません。Gradle Kotlin DSL自体（`.kts`）、検証用build/generatedコードも対象外です。

TvApp同様に「stagedの`.kt`は実行のトリガー」であり、自動整形・通常の静的解析は全対象ソースの**working tree**に対して行います。これに加えて、解析対象範囲内で追加・変更がstageされた`.kt`は、indexから取り出した内容全体の整形チェックを毎回行います。build/generatedなどの除外条件は作業ツリーと共通です。ステージ内容の静的解析・コンパイルは行いません。

自動整形で内容が変わった場合は、その直後に対象ファイルと差分確認・再ステージの案内を表示して停止します。変更検出はstagedファイルだけでなく全解析対象に対して行うため、未ステージの別ファイルを整形した場合も確認が必要です。

再ステージせず再試行した場合、作業ファイルの整形前後は一致していても、ステージ内容の整形違反を検出して停止します。ステージ内容は`build/tmp/git-hooks/staged/`へ元のパス構造で取り出し、毎回作り直します。`GIT_INDEX_FILE`でコミット用のindexが指定された場合は、そちらを読み取ります。

作業ツリーとステージ内容が異なることだけでは拒否しません。どちらも整形済みなら、部分ステージした状態でも成功します。indexの自動更新や作業内容の退避・復元は行いません。

### 自動整形の補正

引き継いだ設定をこの環境で実行すると、`buildUponDefaultConfig=false`かつrule-levelのautoCorrect未指定では、TaskとrulesetのautoCorrectをtrueにしても修正されないことを確認しました。`formatting.yml`で有効な各ruleの`autoCorrect: true`を明示しています。

さらに、修正済みのCorrectableCodeSmellが件数に含まれて整形段階で失敗するため、**自動整形段階だけ**に`autocorrect.yml`の`excludeCorrectable: true`を適用します。その後、autoCorrect=falseかつ補正設定を使わない読み取り専用の整形チェックを挟み、残った整形違反は失敗させます。通常のdetekt設定・maxIssues・ルールは弱めていません。

根拠: [detekt ConfigAware](https://github.com/detekt/detekt/blob/v1.23.8/detekt-api/src/main/kotlin/io/gitlab/arturbosch/detekt/api/ConfigAware.kt)、[設定の読込順](https://github.com/detekt/detekt/blob/v1.23.8/detekt-core/src/main/kotlin/io/gitlab/arturbosch/detekt/core/config/Configurations.kt)、[読み取り専用時のAutoCorrect無効化](https://github.com/detekt/detekt/blob/v1.23.8/detekt-core/src/main/kotlin/io/gitlab/arturbosch/detekt/core/config/DisabledAutoCorrectConfig.kt)。

## 検証結果

このworkspaceで利用する設定は`native/gradle/config/detekt/`へ集約しています。`detekt.yml`・`formatting.yml`・`autocorrect.yml`をnative・Cordovaで共用し、上記の参照元TvAppのパスとは分けて管理します。

2026-10-21に、一時的なGit repositoryで実際のKotlin DSL・detektを実行した**30ケースがすべて成功**しました。ビルドキャッシュを有効にした状態で、同じリポジトリの再試行・再ステージまで確認しています。fixtureのGit操作も既存GitコマンドではなくJGit APIで行います。workspaceのindexへテストファイルをstageしていません。

| ケース | 期待する結果 | 実測 |
| --- | --- | --- |
| stagedにKotlinなし | 未ステージの不正整形コードも変更せずスキップ | 成功 |
| 正常なKotlinをstage | 整形チェック・解析を通過し、ブラウザは起動しない | 成功 |
| 整形が必要なKotlinをstage | 自動修正後、レビュー・再ステージを要求して失敗 | 期待どおり失敗 |
| 既存のstaged/unstaged差分が共存 | 追加整形がなければ成功し、既存差分を維持 | 成功 |
| 静的解析違反 | detektで失敗 | 期待どおり失敗 |
| Kotlinの削除だけをstage | スキップ | 成功 |
| 空白・日本語を含むファイル名 | 正常に扱う | 成功 |
| stagedとは別の未ステージファイルを整形 | その変更も検出して失敗 | 期待どおり失敗 |
| 読み取り専用detektFormatCheckで違反 | 失敗し、元ファイルを変更しない | 期待どおり失敗 |
| 自動整形後に再ステージせず再試行 | ステージ内容の整形違反で再び失敗 | 期待どおり失敗 |
| 整形済み内容を再ステージして再試行 | ステージ内容も整形チェックを通過 | 成功 |
| 作業ファイルだけ整形済み | ステージ内容の整形違反で失敗 | 期待どおり失敗 |
| 作業ファイルが消失し、ステージ内容に整形違反あり | ステージ内容を検査して失敗 | 期待どおり失敗 |
| 空白・日本語を含むファイル名でステージ内容に整形違反あり | ステージ内容を検査して失敗 | 期待どおり失敗 |
| GIT_INDEX_FILEで指定されたindexに整形違反あり | 指定されたindexを検査し、通常のindexも変更しない | 期待どおり失敗 |
| 補正できないフォーマットルール違反 | 停止し、detektの整形レポートを開く | 期待どおり失敗 |
| detekt実行処理のエラー（古いHTMLが存在） | 停止し、ブラウザは起動しない | 期待どおり失敗 |
| detekt設定YAMLの不備 | 停止し、ブラウザは起動しない | 期待どおり失敗 |
| 解析違反時にブラウザ自動表示を無効化 | detekt標準HTMLを生成し、ブラウザは起動しない | 期待どおり失敗 |
| 解析違反があるCI環境 | detekt標準HTMLを生成し、ブラウザは起動しない | 期待どおり失敗 |
| macOSで既定アプリの起動が失敗 | Safariへのフォールバックで違反レポートを表示し、元の違反停止は維持 | 期待どおり失敗 |
| 解析違反とブラウザ起動失敗が共存 | 解析違反によるコミット停止を維持 | 期待どおり失敗 |
| HTML特殊文字を含むファイル名 | ステージ内容の違反を検出し、detekt標準HTMLを直接開く | 期待どおり失敗 |
| data classの`var`プロパティ | `DataClassShouldBeImmutable`で失敗 | 期待どおり失敗 |
| data classの`val`プロパティ | プロパティの再代入がない定義を許可 | 成功 |
| Composableの`List`引数 | `UnstableCollections`で失敗 | 期待どおり失敗 |
| Composableの`Set`引数 | `UnstableCollections`で失敗 | 期待どおり失敗 |
| Composableの`Map`引数 | `UnstableCollections`で失敗 | 期待どおり失敗 |
| Composableの`MutableList`引数 | 既存の`MutableParams`で失敗 | 期待どおり失敗 |
| Composableの`ImmutableList`・`ImmutableSet`・`ImmutableMap`引数 | 不変コレクションを許可 | 成功 |

全30ケースでindexの実行前後SHA-256が一致しました。旧一覧を事前に置いても削除され、新しい一覧が生成されないこと、違反に応じたdetekt標準HTMLを最後に直接開くこと、成功時とルール違反以外のエラーでは開かないことを確認しています。ブラウザ自動表示の無効化・CIは、実際に解析違反があるケースで検証しています。[結果JSON](logs/git-hooks/cases/results.json)。

当時の自動テストではOSの起動コマンドを差し替え、多数のタブを開かずに、開くファイルの実在と内容・呼び出し回数・実行順を検証します。初回9ケースの[旧実行ログ](logs/git-hooks/git-hook-tests.log)や2026-10-19の[旧表示確認ログ](logs/git-hooks/git-hook-browser-smoke.log)は、変更前の仕様の履歴です。

上の表・結果JSONは削除前の検証記録です。検証用スクリプトとfixtureは現在のリポジトリには含めていません。

workspace自体のnative・Cordovaルートでも品質タスクの読み込みを確認しています。未stageの状態ではpreCommitをスキップします。ステージ済みソースに対する陽性・陰性の動作確認は上記fixtureで行っています。

preCommitは実行時のindexと一時状態に依存するためConfiguration Cache非対応を明示しています。自動整形タスクはキャッシュ復元で処理を省略しません。

Immutable Collectionsの追加に合わせて、`DataClassShouldBeImmutable`と`UnstableCollections`を有効化し、`build.maxIssues`を0にしました。違反1件でも停止します。パスの扱いを検証する特殊ファイル名のfixtureだけは`Filename`を抑制し、製品側の設定は維持しています。ルールの範囲とライブラリの互換性は[追加記録](IMMUTABLE_COLLECTIONS.md)を参照してください。
