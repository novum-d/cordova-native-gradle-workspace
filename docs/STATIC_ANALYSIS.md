# 静的解析と整形ルールの維持方針

[ドキュメント一覧](README.md) · [Git hookの実装](GIT_HOOKS.md) · [プロジェクトルール](../AGENTS.md#静的解析整形ルールの変更方針)

## 変更方針

既存のdetekt・ktlintのルールを尊重し、有効・無効、閾値、例外、自動整形の方針を維持します。ツールやルールライブラリの更新を、追加ルールの導入や厳格化の理由にはしません。

追加したい項目がある場合は、必要性・検出例・既存コードへの影響・コミット時の停止や自動整形への影響を提示し、相談して合意した内容だけを適用します。更新先のデフォルトルールが増える場合も、同じ方針で確認します。

## 現在の採用設定

2026-10-25に試行したルール追加・厳格化とComposeルール依存の移行を取り消し、試行前の設定へ復元しました。

| 対象 | 設定 |
| --- | --- |
| detekt・detekt-formatting | 1.23.8 |
| Composeルール | `com.twitter.compose.rules:detekt:0.0.26`、`TwitterCompose` ruleset |
| ktlint | detekt-formatting内蔵の0.50.0。`ktlintCheck`は`detektFormatCheck`への互換名 |
| 設定検証 | `validation=true`、`warningsAsErrors=false` |
| 複雑度 | `CognitiveComplexMethod`は無効。既存の引数数・関数長などの閾値を維持 |
| 未使用private宣言 | `UnusedPrivateMember`は無効 |
| コルーチン | `GlobalCoroutineUsage`の明示的な追加は取り消し、従来の標準設定を維持 |
| Compose | 既存の`RememberMissing`・`UnstableCollections`などを維持。無効だったModifier関連ルールも無効のまま |
| 整形 | 試行前の設定を維持。追加した標準50ルールと呼び出し側末尾カンマの設定は取り消し |
| 失敗条件 | 従来からの`build.maxIssues=0`を維持 |

複数引数の関数宣言の改行、宣言側末尾カンマ、行長120などは試行前からの設定です。これらを含め、もともとの整形方針を独断で緩める変更も行っていません。

管理元は[Catalog](../native/gradle/libs.versions.toml)、[解析設定](../native/gradle/config/detekt/detekt.yml)、[整形設定](../native/gradle/config/detekt/formatting.yml)、[共有品質タスク](../native/gradle/git-hooks.gradle.kts)です。

## 相談前の候補の扱い

`GlobalCoroutineUsage`、複雑度や未使用private宣言の検査、Modifier関連の追加ルールなどは、現時点では追加していません。型解決が必要なルールも、設定を有効にするだけで検査可能と扱わず、必要な構成変更を含めて相談します。

[追加ルールの試行記録](logs/versions/rule-refresh-2026-10-25/README.md)は採用を見送った案の履歴です。現在の採用構成や検証結果とは区別してください。

## 確認コマンド

通常のチェックは読み取り専用です。

```sh
# nativeの解析・整形確認。
./scripts/with-env.sh ./native/gradlew -p native detekt detektFormatCheck

# Cordova側でも共通設定を確認する。
./scripts/with-env.sh ./native/gradlew -p CordovaComposeApp/platforms/android detekt ktlintCheck
```

preCommitの処理順序・対象・自動整形後の差分確認と再ステージ・ステージ内容の検査も従来どおりです。詳細は[Git hookの仕様](GIT_HOOKS.md)を参照してください。
