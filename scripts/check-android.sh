#!/bin/sh
# Cordova連携を復元し、品質・単体テスト・Android Lintを順に検証する。アプリ起動は行わない。
set -eu

workspace_dir=$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd -P)
cd "$workspace_dir"

# prepareでSettingsへのCatalog登録、app依存、Web assetsを最新の管理元へそろえる。
./scripts/cordova.sh prepare android

# nativeでは各モジュールのcheck・単体テストと両variantのLint、開発ホストのDebug Lintを確認する。
./scripts/with-env.sh ./native/gradlew -p native \
  check :feature:lintDebug :feature:lintRelease :data:lintDebug :data:lintRelease \
  :cordova-plugin-dev:lintDebug :cordova-plugin-dev:lintRelease \
  :dev-app:lintDebug \
  --warning-mode all --console=plain

# 両ホストは同じLibraryのbuildディレクトリを使うため、並行せずnative完了後に実行する。
./scripts/with-env.sh ./native/gradlew -p CordovaComposeApp/platforms/android \
  :check :app:lintDebug :app:lintRelease \
  --warning-mode all --console=plain
