#!/bin/sh
set -eu
workspace_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$workspace_dir"
npm ci --no-audit --no-fund
(cd CordovaComposeApp && npm ci --no-audit --no-fund)
gradle_version=$(node scripts/catalog-version.cjs gradle)
if [ ! -x ".tools/gradle-$gradle_version/bin/gradle" ]; then
    mkdir -p .tools
    curl -fL --retry 2 "https://services.gradle.org/distributions/gradle-$gradle_version-bin.zip" \
        -o ".tools/gradle-$gradle_version-bin.zip"
    expected_sha=$(curl -fsSL "https://services.gradle.org/distributions/gradle-$gradle_version-bin.zip.sha256")
    actual_sha=$(shasum -a 256 ".tools/gradle-$gradle_version-bin.zip" | cut -d ' ' -f 1)
    [ "$actual_sha" = "$expected_sha" ] || { echo 'Gradle checksum mismatch' >&2; exit 1; }
    unzip -q ".tools/gradle-$gradle_version-bin.zip" -d .tools
fi
# Install SDK packages separately with android sdk install platforms/android-35 and build-tools/35.0.0.
./scripts/with-env.sh ".tools/gradle-$gradle_version/bin/gradle" -p native wrapper --console=plain
if [ ! -d CordovaComposeApp/platforms/android ]; then
    ./scripts/cordova.sh platform add android@14.0.1 --save
else
    ./scripts/cordova.sh prepare android
fi
