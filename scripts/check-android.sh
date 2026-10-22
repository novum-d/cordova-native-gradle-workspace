#!/bin/sh
# Cordova連携を復元してから、両ホストの品質・Android Lint・基盤構成を順に検証する。
set -eu

workspace_dir=$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd -P)
cd "$workspace_dir"

# prepareでSettingsへのCatalog登録、app依存、Web assetsを最新の管理元へそろえる。
./scripts/cordova.sh prepare android

# nativeでは各モジュールのcheck・単体テストも含め、両variantのLintと実効版を確認する。
./scripts/with-env.sh ./native/gradlew -p native \
  check :feature:lintDebug :feature:lintRelease :data:lintDebug :data:lintRelease \
  verifyNativeIntegration --warning-mode all --console=plain

# 両ホストは同じLibraryのbuildディレクトリを使うため、並行せずnative完了後に実行する。
./scripts/with-env.sh ./native/gradlew -p CordovaComposeApp/platforms/android \
  :check :app:lintDebug :app:lintRelease verifyNativeIntegration \
  --warning-mode all --console=plain
