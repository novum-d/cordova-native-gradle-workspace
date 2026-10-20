#!/bin/sh
set -eu

workspace_dir=$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd -P)
cd "$workspace_dir"

cordova_version=$(node -p 'const p = require("./package.json"); p.devDependencies?.cordova ?? p.dependencies?.cordova ?? ""')
[ -n "$cordova_version" ] || {
  echo "Cordova CLI is not declared in: $workspace_dir/package.json" >&2
  echo 'Restore the workspace package.json (not CordovaComposeApp/package.json), then retry.' >&2
  exit 1
}
cordova_android_version=$(node -p 'const p = require("./CordovaComposeApp/package.json"); p.dependencies?.["cordova-android"] ?? p.devDependencies?.["cordova-android"] ?? ""')
[ -n "$cordova_android_version" ] || {
  echo "Cordova Android is not declared in: $workspace_dir/CordovaComposeApp/package.json" >&2
  echo 'Restore the application package.json and package-lock.json, then retry.' >&2
  exit 1
}

# Cordova CLI is a development dependency, including when NODE_ENV=production.
npm ci --include=dev --no-audit --no-fund
[ -f node_modules/cordova/bin/cordova ] || {
  echo "Cordova CLI was not installed in: $workspace_dir/node_modules/cordova" >&2
  echo "Declared version: $cordova_version" >&2
  echo "npm install location: $(npm root)" >&2
  echo "npm dry-run: $(npm config get dry-run)" >&2
  echo 'Check the npm ci output above. With dry-run=true, no files are installed.' >&2
  exit 1
}
(cd CordovaComposeApp && npm ci --include=dev --no-audit --no-fund)
# Report the original Node error before Cordova replaces it with a generic API error.
node -e 'const api = require(process.argv[1]); if (typeof api.createPlatform !== "function") throw new Error("Invalid Cordova Android API")' \
  "$workspace_dir/CordovaComposeApp/node_modules/cordova-android"

# Gradle
gradle_version=$(./scripts/catalog-version.sh gradle)
if [ ! -x ".tools/gradle-$gradle_version/bin/gradle" ]; then
  mkdir -p .tools
  curl -fL --retry 2 "https://services.gradle.org/distributions/gradle-$gradle_version-bin.zip" \
    -o ".tools/gradle-$gradle_version-bin.zip"
  expected_sha=$(curl -fsSL "https://services.gradle.org/distributions/gradle-$gradle_version-bin.zip.sha256")
  actual_sha=$(shasum -a 256 ".tools/gradle-$gradle_version-bin.zip" | cut -d ' ' -f 1)
  [ "$actual_sha" = "$expected_sha" ] || {
    echo 'Gradle checksum mismatch' >&2
    exit 1
  }
  unzip -q ".tools/gradle-$gradle_version-bin.zip" -d .tools
fi

# Sdk（platforms/android-35, build-tools/35.0.0）
./scripts/with-env.sh ".tools/gradle-$gradle_version/bin/gradle" -p native wrapper --console=plain

# Cordova
android_dir="$workspace_dir/CordovaComposeApp/platforms/android"
if [ -e "$android_dir" ] || [ -L "$android_dir" ]; then
  if [ ! -f "$android_dir/cordova/Api.js" ] || [ ! -f "$android_dir/settings.gradle" ] \
    || [ ! -f "$android_dir/app/build.gradle" ] || [ ! -f "$android_dir/CordovaLib/build.gradle" ]; then
    backup_root="$workspace_dir/.tools/cordova-platform-backups"
    mkdir -p "$backup_root"
    backup_dir=$(mktemp -d "$backup_root/android.XXXXXX")
    mv "$android_dir" "$backup_dir/android"
    echo "Incomplete Android platform saved to: $backup_dir/android"
    echo 'Regenerating the Android platform with Cordova CLI.'
  fi
fi
if [ ! -d "$android_dir" ]; then
  ./scripts/cordova.sh platform add android@14.0.1 --save
else
  node -e 'require(process.argv[1])' "$android_dir/cordova/Api.js"
  ./scripts/cordova.sh prepare android
fi
