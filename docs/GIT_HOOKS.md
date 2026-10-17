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
| Git index | 読み取りのみ。自動stage・commit・resetはしない |
| パス | 文字列集合として扱い、空白・日本語をshellで分割しない |
| レポート | `build/reports/detekt/<task>.html`、`build/reports/git-hooks/preCommit.txt` |

TvAppのshell、既存detektコマンド、手動取得したローカルJARは取り込んでいません。Hook実装に`Exec`、`JavaExec`、`ProcessBuilder`、`git diff`、`java -jar`はありません。

detekt Plugin、detekt-formatting、Twitter Compose rules、JGitは通常のMaven座標からGradleが依存解決します。JARをリポジトリへコピーしたり、既存JARのmainを直接起動したりする構成ではありません。JGit 7.8.0.202609011348-rはGitコマンドを呼ばずにindexを扱うためのbuild-time依存です。これらはAndroidモジュールやAPKの依存には追加しません。

版の管理元は既存の`native/gradle/libs.versions.toml`です。`native/build.gradle.kts`とCordovaの共有`build-extras.gradle`から同じKotlin DSLを適用するため、両方のGradleルートから実行できます。

## タスク

| タスク | 処理 |
| --- | --- |
| `preCommit` | staged判定→自動整形→残存整形違反チェック→通常解析→変更検出 |
| `detektFormat` | 自動整形だけを明示実行。stagedの有無ではスキップしない |
| `detektFormatCheck` | 整形違反を検査し、ファイルは変更しない |
| `detekt` | 通常の静的解析。ファイルは変更しない |
| `ktlintCheck` | TvAppと同じ互換名。detektFormatCheckへ依存 |
| `check` | detektFormatCheckとdetektを実行する読み取り専用の品質ゲート |

`preparePreCommit`、`detektPreCommitFormat`、`detektPreCommitFormatCheck`、`detektPreCommit`はpreCommitの内部段階です。コミット前の確認には最終の`preCommit`を使います。

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

## Gitコミット時の自動実行を初回設定する

各cloneで一度、ワークスペースのルートから実行します。

```sh
./scripts/setup-git-hooks.sh
```

セットアップはGitのhookディレクトリに実行可能な`pre-commit`を作成します。通常は`.git/hooks/pre-commit`です。hookはワークスペースのルートへ移動し、`./scripts/with-env.sh ./native/gradlew -p native :preCommit`を実行します。Gradleが成功すればコミットを続行し、失敗すればコミットを中止します。整形でファイルが変わった場合は、内容を確認して再ステージしてからコミットしてください。JDKなどは手動実行時と同じ環境準備が必要です。

同じhookが登録済みなら再実行できます。異なる既存hookやシンボリックリンクは上書きせず停止します。`core.hooksPath`が設定済みの場合も設定を変更せず停止するため、既存のhook側から上記のコマンドを呼び出してください。セットアップスクリプトはbootstrapからは自動実行しません。

セットアップとGitからの呼び出しは、一時リポジトリを使って`npm run test:git-hooks`で検証できます。

## 解析範囲とTvAppから調整した点

`native/feature/src/**/*.kt`と`native/data/src/**/*.kt`を対象にします。`java`・`kotlin`双方の配置、test、androidTest、その他のsource setを含め、build/generatedは除外します。生成されたCordova platform、Gradle Kotlin DSL自体（`.kts`）、検証用build/generatedコードは対象外です。

TvApp同様に「stagedの`.kt`は実行のトリガー」であり、解析対象は全対象ソースの**working tree**です。staged版そのもののコンパイル・解析ではありません。既存の部分ステージを許容する挙動を維持しています。

変更検出はstagedファイルだけでなく全解析対象へ広げました。全体の自動整形で未ステージの別ファイルが変わった場合も、レビューを要求して失敗します。もとの未ステージ差分が変わらなければ成功します。indexは全ケースで変更しません。

### 自動整形の補正

引き継いだ設定をこの環境で実行すると、`buildUponDefaultConfig=false`かつrule-levelのautoCorrect未指定では、TaskとrulesetのautoCorrectをtrueにしても修正されないことを確認しました。`formatting.yml`で有効な各ruleの`autoCorrect: true`を明示しています。

さらに、修正済みのCorrectableCodeSmellが件数に含まれて整形段階で失敗するため、**自動整形段階だけ**に`autocorrect.yml`の`excludeCorrectable: true`を適用します。その後、autoCorrect=falseかつ補正設定を使わない読み取り専用の整形チェックを挟み、残った整形違反は失敗させます。通常のdetekt設定・maxIssues・ルールは弱めていません。

根拠: [detekt ConfigAware](https://github.com/detekt/detekt/blob/v1.23.8/detekt-api/src/main/kotlin/io/gitlab/arturbosch/detekt/api/ConfigAware.kt)、[設定の読込順](https://github.com/detekt/detekt/blob/v1.23.8/detekt-core/src/main/kotlin/io/gitlab/arturbosch/detekt/core/config/Configurations.kt)、[読み取り専用時のAutoCorrect無効化](https://github.com/detekt/detekt/blob/v1.23.8/detekt-core/src/main/kotlin/io/gitlab/arturbosch/detekt/core/config/DisabledAutoCorrectConfig.kt)。

## 検証結果

一時的なGit repositoryをJGitで作成し、実際のKotlin DSL・detektを実行した9ケースがすべて成功しました。fixtureのGit操作も既存GitコマンドではなくJGit APIで行います。workspaceのindexへテストファイルをstageしていません。

| ケース | 期待する結果 | 実測 |
| --- | --- | --- |
| stagedにKotlinなし | 未ステージの不正整形コードも変更せずスキップ | 成功 |
| 正常なKotlinをstage | 整形チェック・解析を通過 | 成功 |
| 整形が必要なKotlinをstage | 自動修正後、レビュー・再ステージを要求して失敗 | 期待どおり失敗 |
| 既存のstaged/unstaged差分が共存 | 追加整形がなければ成功し、既存差分を維持 | 成功 |
| 静的解析違反 | detektで失敗 | 期待どおり失敗 |
| Kotlinの削除だけをstage | スキップ | 成功 |
| 空白・日本語を含むファイル名 | 正常に扱う | 成功 |
| stagedとは別の未ステージファイルを整形 | その変更も検出して失敗 | 期待どおり失敗 |
| 読み取り専用detektFormatCheckで違反 | 失敗し、元ファイルを変更しない | 期待どおり失敗 |

全9ケースでindexの実行前後SHA-256が一致しました。[結果JSON](git-hook-tests/results.json)、[実行ログ](git-hook-tests.log)

```sh
python3 verification/verify-gradle-git-hooks.py
```

検証は一時repositoryだけで行い、終了時に削除します。Gradle本体の呼び出しはテスト実行用で、Hook実装が外部コマンドへ処理を委譲するものではありません。

workspace自体のnative・Cordovaルートでも`check`・`preCommit`・ビルドを確認しています。実際のLibraryは空なので通常のdetektはNO-SOURCE、preCommitは未stageのためスキップです。実ソースに対する陽性・陰性の動作確認は上記fixtureで行っています。

preCommitは実行時のindexと一時状態に依存するためConfiguration Cache非対応を明示しています。自動整形タスクはキャッシュ復元で処理を省略しません。
