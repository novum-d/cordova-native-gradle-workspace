# Timber初期化の検証記録

[ログ一覧](../README.md) · [セットアップと使い方](../../LOGGING.md)

2026-10-27にAPI 35エミュレーターで確認した記録です。各APKを起動し、同一プロセス内でActivityを再起動して初期化ログの重複がないことを確認しました。

| ファイル | 内容 |
| --- | --- |
| [results.json](results.json) | Manifestの登録数、Debuggable属性、ログ件数、テスト・ビルドの結果、エミュレーター終了確認 |
| [dev-debug.log](dev-debug.log) | 開発用DebugのNativeLoggingタグ。初期化1件 |
| [cordova-debug.log](cordova-debug.log) | Cordova DebugのNativeLoggingタグ。初期化1件 |
| [cordova-release.log](cordova-release.log) | Cordova ReleaseのNativeLoggingタグ。初期化ログなし |

Release APKは検証専用にDebugキーで署名したもので、Debuggable属性は無効です。APKや署名ファイルはこの記録に含めていません。
