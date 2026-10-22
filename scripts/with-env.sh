#!/bin/sh
# 受け取ったコマンドに、このworkspace用のJDK・SDK・Gradleの環境を設定して実行する。
set -eu

workspace_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)

# 明示されたCordova用JDKを優先し、なければmiseの17.0.2、最後に既存JAVA_HOMEを使う。
if [ -n "${CORDOVA_JAVA_HOME:-}" ]; then
  JAVA_HOME=$CORDOVA_JAVA_HOME
elif command -v mise >/dev/null 2>&1; then
  JAVA_HOME=$(mise where java@17.0.2)
fi
: "${JAVA_HOME:?Set JAVA_HOME or CORDOVA_JAVA_HOME to a JDK 17 installation}"

export JAVA_HOME
export CORDOVA_JAVA_HOME=$JAVA_HOME
# SDKの明示設定を保ち、未指定の場合だけOS標準の配置先を補う。
if [ -z "${ANDROID_HOME:-}" ]; then
  if [ "$(uname -s)" = Darwin ]; then ANDROID_HOME="$HOME/Library/Android/sdk"; else ANDROID_HOME="$HOME/Android/Sdk"; fi
fi

export ANDROID_HOME
export ANDROID_SDK_ROOT=$ANDROID_HOME

# この子プロセスの探索パスだけを変更する。親シェルやグローバル設定は書き換えない。
gradle_version=$("$workspace_dir/scripts/catalog-version.sh" gradle)
export GRADLE_OPTS="${GRADLE_OPTS:-} -Dorg.gradle.vfs.watch=false"

export PATH="$workspace_dir/.tools/gradle-$gradle_version/bin:$workspace_dir/node_modules/.bin:$JAVA_HOME/bin:$PATH"
exec "$@"
