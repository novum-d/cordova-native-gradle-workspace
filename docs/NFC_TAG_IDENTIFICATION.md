# 型番識別の判定器を追加する

[ドキュメント一覧](README.md) · [Repository・Factoryの構成](NFC_READER_DESIGN.md)

更新日: 2026-10-28。共通のResolverと型番固有の判定器を分離しています。現在の本番登録はICODE SLIだけです。他のRF・型番を実機確認済み、または読取対応済みとする変更ではありません。

## 現在の分担

| ファイル | 責務 |
| --- | --- |
| [NfcTagProfile.kt](../native/data/src/main/kotlin/dev/example/nativedata/nfc/profile/NfcTagProfile.kt) | モデル・通信方式・メモリ構成の共通の型。製品固有の判定条件は置かない |
| [NfcTagProfileIdentifier.kt](../native/data/src/main/kotlin/dev/example/nativedata/nfc/profile/NfcTagProfileIdentifier.kt) | 必要なAndroid技術と、型番識別の契約 |
| [NfcTagProfileResolver.kt](../native/data/src/main/kotlin/dev/example/nativedata/nfc/profile/NfcTagProfileResolver.kt) | 登録済みの判定器を呼び、一意のProfileを選ぶ |
| [IcodeSliTagProfile.kt](../native/data/src/main/kotlin/dev/example/nativedata/nfc/profile/icodesli/IcodeSliTagProfile.kt) | SLI固有の28ブロック×4バイト等の仕様 |
| [IcodeSliTagProfileIdentifier.kt](../native/data/src/main/kotlin/dev/example/nativedata/nfc/profile/icodesli/IcodeSliTagProfileIdentifier.kt) | SLIのUID長・メーカー・製品種別・派生型ビットの判定 |
| [NfcTagIdentificationModule.kt](../native/data/src/main/kotlin/dev/example/nativedata/nfc/di/NfcTagIdentificationModule.kt) | HiltのSetへ判定器を登録する |

ReaderとDataSourceは同じ登録内容のResolverを使います。旧`identifyNfcTagProfile(uid, technologies)`へ製品ごとのwhen分岐を追加する構成から、`profileResolver.identify(tag)`へ置き換えています。

## 共通Resolverの動作

1. 実タグのAndroid技術一覧を取得する。
2. `requiredTechnologies`をすべて満たす判定器だけを呼び出す。
3. 判定器が返したProfileについて、必要な通信方式の技術が実タグに存在することを確認する。
4. 一致した判定器が1つだけの場合、そのProfileを返す。

未一致・複数一致はnullとなり、読取側でユースケース固有のUnsupportedTagへします。複数一致は同じProfileを返す場合も含み、登録順を優先順位にしません。例外・キャンセルを不一致へ変換せず、呼出元のnfcCallへ伝えます。

共通部には「UIDは8バイト」「メーカーはNXP」といった製品の条件を置きません。ISO 15693の送信に必要な8バイトUIDの検査は、DataSourceのISO 15693経路にも残しています。

## 型番を追加するときの手順

1. **モデルとProfileを追加する。** `NfcTagModel`へ識別子を追加し、モデル固有のファイルで、根拠を確認した通信方式とメモリ構成を定義する。業務フィールドの配置はProfileに入れない。
2. **判定器を追加する。** `NfcTagProfileIdentifier`を実装し、必要なAndroid技術と識別条件を書く。UIDだけで分からない型番は、技術固有の情報や識別コマンドを使い、確定できなければnullを返す。
3. **Hiltへ登録する。** `@Binds`・`@IntoSet`で登録する。既存Moduleへ登録を追加しても、別のModuleから登録してもよく、Resolverの判定アルゴリズムは変更しない。
4. **判定テストを追加する。** 正常例に加え、似た型番・技術違い・欠落情報・重複した条件を検証する。

以下は新しい判定器を登録する際のテンプレートで、説明用の型名です。

```kotlin
/** 新しい判定器を、共通Resolverが利用するSetへ追加する。 */
@Binds
@IntoSet
abstract fun bindNewIdentifier(
    implementation: NewTagProfileIdentifier,
): NfcTagProfileIdentifier
```

HiltのSet注入により、Resolverが個別の判定器を直接生成する必要はありません。KotlinのSet注入ではワイルドカードを避けるため、注入箇所に`@JvmSuppressWildcards`を指定しています。[Dagger公式のMultibindings](https://dagger.dev/dev-guide/multibindings.html)

## 非同期の識別と読取対応の区別

識別契約はUID・技術一覧の引数だけに固定せず、`suspend fun identify(tag: NfcTagAccess)`としています。現在のSLI判定はメタデータだけを使い、追加の通信はしません。

識別コマンドが必要な場合は、技術固有の取得口をNfcTagAccessへ追加できます。判定器は取得した接続を確実に閉じ、Tagや接続をフィールドへ保存しないでください。共通ReaderはタグのMutex内・IOで識別し、DataSourceもIOで再照合します。識別結果のキャッシュは作っていないため、判定器は繰り返し呼ばれ得ます。

**型番を識別できることと、そのタグを読み取れることは別です。** 新しい通信方式では`NfcReadProtocol`等の型定義・接続API・DataSourceの対応も必要です。既存のISO 15693・均一ブロック以外の領域、ページ・セクタ・認証は対象仕様を確認してから追加します。共通Resolverがモデル固有の識別コマンドを決めることはありません。

## 検証

SLIの既存判定を維持し、追加の試験用判定器で、登録追加・技術条件・重複一致・非同期取得・失敗と取消の伝播・ReaderからDataSourceまでの登録利用を検証します。試験用のUIDやProfileは実製品の仕様・新しい対応製品を示しません。

[ビルド・テストの検証記録](logs/nfc-identification/README.md)を参照してください。
