#!/bin/sh
# CLI・Android依存・Gradleを復元し、Cordova platformを新規作成またはprepareする初回セットアップ。
set -eu

workspace_dir=$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd -P)
cd "$workspace_dir"

# インストール前に、CLIを管理するルートとAndroid本体を管理するアプリ側の宣言を確かめる。
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

# production環境でも開発依存のCordova CLIを省略せず、両方のlockfileどおりに導入する。
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
# Cordovaが汎用エラーに置き換える前に、Android実装を読み込めない原因をNodeから表示する。
node -e 'const api = require(process.argv[1]); if (typeof api.createPlatform !== "function") throw new Error("Invalid Cordova Android API")' \
  "$workspace_dir/CordovaComposeApp/node_modules/cordova-android"

# Catalog指定のGradleを取得し、配布元のSHA-256と一致する場合だけ展開する。
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

# 準備済みのJDK・Android SDK環境で、Catalogと同じ版のWrapperを復元する。
./scripts/with-env.sh ".tools/gradle-$gradle_version/bin/gradle" -p native wrapper --console=plain

# Cordova Androidの生成物を確認し、不完全なplatformだけを復旧対象にする。
android_dir="$workspace_dir/CordovaComposeApp/platforms/android"
# 必須ファイルが欠けたplatformは退避する。正常なplatformは再作成せず既存内容を保つ。
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
# platform未作成なら追加し、既存ならApi.jsを読み込めることを確かめてhookとassetsを復元する。
if [ ! -d "$android_dir" ]; then
  ./scripts/cordova.sh platform add android@14.0.1 --save
else
  node -e 'require(process.argv[1])' "$android_dir/cordova/Api.js"
  ./scripts/cordova.sh prepare android
fi
