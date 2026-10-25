# Immutable Collectionsの追加（2026-10-19）

以下は追加時の記録です。その後の日時API・全モジュール依存の追加は[依存配置と検証](DEPENDENCIES.md)を参照してください。

`kotlinx.collections.immutable:0.3.8`をVersion Catalogへ登録し、通常の`feature`・`data`両モジュールへ`implementation(libs.kotlinx.collections.immutable)`で追加しました。native単独とCordovaの両方で利用できます。

## バージョンの判断

現在のKotlinは1.9.24です。0.3.8の[JVM POM](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-collections-immutable-jvm/0.3.8/kotlinx-collections-immutable-jvm-0.3.8.pom)はstdlib 1.9.21を参照し、`PersistentList`のKotlinメタデータは`mv=[1,9,0]`でした。[0.4.0の変更点](https://github.com/Kotlin/kotlinx.collections.immutable/releases/tag/v0.4.0)はKotlin 2.1.20への更新を記載しています。このworkspaceではKotlin 1.9.24で実コンパイルできた0.3.8を採用しています。ライブラリ全体の最新版を採用したという意味ではありません。

検証用ソースでComposeの`ImmutableList`引数と、`PersistentList.add`・`PersistentMap.put`・`PersistentSet.add`を実コンパイルしました。

## 使い方

```kotlin
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class UiState(val items: ImmutableList<String>)

val before = persistentListOf("first")
val after = before.add("second")
```

`add`や`put`は更新後の新しいコレクションを返すため、戻り値を使います。上の例では`before`の内容は変わりません。通常の`List`は読み取り専用インターフェースであり、別の参照から変更される可能性があります。Composeへ渡す一覧には`ImmutableList`・`ImmutableSet`・`ImmutableMap`を使えます。

要素自体の不変性は別途必要です。`val`や`ImmutableList`だけで、要素に含まれる可変オブジェクトまで不変になるわけではありません。

## detekt・ktlint

[`native/gradle/config/detekt/detekt.yml`](../native/gradle/config/detekt/detekt.yml)で以下を設定しました。native・Cordovaで共通の設定です。

| ルール・設定 | 対応と範囲 |
| --- | --- |
| `style.DataClassShouldBeImmutable` | 有効化。data classの`var`プロパティを検出する。`val`の参照先まで不変と保証するルールではない |
| `TwitterCompose.UnstableCollections` | 有効化。Composable引数の通常の`List`・`Set`・`Map`を検出し、不変コレクションなどの利用を促す |
| `TwitterCompose.MutableParams` | 既存の有効設定を維持。Composableの`MutableList`などの引数を検出する |
| `build.maxIssues` | 1から0へ変更。上記を含め、違反1件でも品質チェック・preCommitを失敗させる |

独立したktlintとdetekt-formattingには、不変コレクションの利用を強制する標準ルールを追加していません。整形は既存の設定を維持し、この検査はdetektへまとめています。すべての`var`や可変コレクションを禁止する設定にもしていません。状態管理や局所的なbuilderでは可変性が必要になるためです。

`VarCouldBeVal`はdetektのデフォルトで有効ですが、型解決を必要とします。現在のルート品質タスクは解析用classpathを渡さないため、この追加によって検査できるようになったとは扱いません。

根拠: [DataClassShouldBeImmutable](https://detekt.dev/docs/1.23.8/rules/style/#dataclassshouldbeimmutable)、[VarCouldBeVal](https://detekt.dev/docs/1.23.8/rules/style/#varcouldbeval)、[Composeの不安定なコレクション](https://twitter.github.io/compose-rules/rules/#avoid-using-unstable-collections)、[ktlint標準ルール](https://ktlint.github.io/ktlint/latest/rules/standard/)。

## 検証結果

- Kotlin 1.9.24／Compose Compiler 1.5.14での実コンパイル成功。[ログ](logs/immutable-collections/immutable-compile.log)
- 検証用ソースをcleanした通常Debug／Release APKのビルド成功。[ログ](logs/immutable-collections/immutable-normal-build.log)
- `npm run check:android`とCatalogの依存解決に成功。Android Lintは全6構成でエラー0。[ログ](logs/immutable-collections/immutable-checks.log)、[件数](logs/immutable-collections/immutable-lint-results.json)
- Git hookの28ケースに成功し、全ケースでindex不変。新たにdata classの`var`検出／`val`許可、Composableの`List`・`Set`・`Map`・`MutableList`検出、不変コレクション許可を確認。[結果](logs/git-hooks/cases/results.json)
- 独立したktlintのチェック・故意の整形違反の拒否・整形後の成功を確認。
