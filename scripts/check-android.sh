#!/bin/sh
set -eu

workspace_dir=$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd -P)
cd "$workspace_dir"

# Restore generated Cordova integration and assets before the checks.
./scripts/cordova.sh prepare android

./scripts/with-env.sh ./native/gradlew -p native \
  check :feature:lintDebug :feature:lintRelease :data:lintDebug :data:lintRelease \
  verifyNativeIntegration --warning-mode all --console=plain

# Run sequentially: the native modules share build directories with this host.
./scripts/with-env.sh ./native/gradlew -p CordovaComposeApp/platforms/android \
  :check :app:lintDebug :app:lintRelease verifyNativeIntegration \
  --warning-mode all --console=plain
