#!/bin/sh
# ToolingSync.javaを使い、IDEのGradle Syncと同じAndroid V2基本モデルを取得する。
set -eu
workspace_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)

agp_version=$("$workspace_dir/scripts/catalog-version.sh" agp)
gradle_version=$("$workspace_dir/scripts/catalog-version.sh" gradle)
# builder-modelは事前のAndroidビルドで解決済みのキャッシュを使う。接続先は引数、既定はnative。
model_jar=$(find "${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1/com.android.tools.build/builder-model/$agp_version" -name '*.jar' -type f | head -1)
mkdir -p "$workspace_dir/.tools/tooling-sync"

# Catalog指定のGradle Tooling APIとAGPのモデルJARで検証プログラムをコンパイルして実行する。
"$workspace_dir/scripts/with-env.sh" javac -cp "$workspace_dir/.tools/gradle-$gradle_version/lib/*:$model_jar" \
  -d "$workspace_dir/.tools/tooling-sync" "$workspace_dir/verification/ToolingSync.java"
"$workspace_dir/scripts/with-env.sh" java \
  -cp "$workspace_dir/.tools/tooling-sync:$workspace_dir/.tools/gradle-$gradle_version/lib/*:$model_jar" \
  ToolingSync "$workspace_dir/${1:-native}" "$workspace_dir/.tools/gradle-$gradle_version"
