# 型番判定器の登録・追加の検証

[ドキュメント一覧](../../README.md) · [追加手順](../../NFC_TAG_IDENTIFICATION.md)

2026-10-28に、製品固有の条件を共通Resolverから分離し、HiltのSet登録で判定器を追加する構成を検証しました。

| ファイル | 内容 |
| --- | --- |
| [results.json](results.json) | 各ホスト・variantのテスト件数と品質・ビルドの結果 |
| [native-check.log](native-check.log) | nativeの両variantのテスト、開発用APK、data・featureとdev-appのLint、detekt・整形・KDoc生成 |
| [cordova-check.log](cordova-check.log) | CordovaのDebug／Release APK、両variantのテスト、appのLintとcheck |
| [final-style.log](final-style.log) | 最後のKDoc文言調整後のdetekt・整形確認 |

## 結果と範囲

- data 72件・feature 46件を各ホストのDebug／Releaseで実行しました。118種類・計472実行で、失敗・エラー・スキップ0件です。
- 新しい試験用の判定器を登録するだけで、共通Resolver・Repository・Readerを変更せず識別結果へ反映されることを確認しました。
- 必要な技術の事前確認、技術の不一致、未一致、異なるProfile・同じProfileを返す複数一致、登録順の入替、非同期待機、例外・取消伝播を検証しました。
- 共通のUID長制限をなくし、ISO 15693の送信に必要な8バイトの検査は通信経路に残しています。試験用の識別条件・UIDは新たな実製品の仕様ではありません。
- 本番登録はICODE SLIだけで、従来のSLI判定・座標読取・具体エラー・接続解放を維持しています。他のRF・型番の実識別や読取、実タグでの通信成功は未確認です。今回はエミュレーターを起動していません。

## 再実行

両ホストのdata・featureはbuild出力を共有します。並行実行せず、ホスト切替時に共有モジュールをcleanします。

```sh
./scripts/with-env.sh ./native/gradlew -p native \
  :data:clean :feature:clean \
  :data:testDebugUnitTest :data:testReleaseUnitTest \
  :feature:testDebugUnitTest :feature:testReleaseUnitTest :dev-app:assembleDebug \
  :data:lintDebug :data:lintRelease :feature:lintDebug :feature:lintRelease :dev-app:lintDebug \
  detekt detektFormatCheck dokkaGeneratePublicationHtml --console=plain

./scripts/with-env.sh ./native/gradlew -p CordovaComposeApp/platforms/android \
  :data:clean :feature:clean :app:clean :app:assembleDebug :app:assembleRelease \
  :data:testDebugUnitTest :data:testReleaseUnitTest \
  :feature:testDebugUnitTest :feature:testReleaseUnitTest \
  :app:lintDebug :app:lintRelease :check --console=plain
```
