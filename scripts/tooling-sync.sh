#!/bin/sh
set -eu
workspace_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
# The matching model client JAR is downloaded by AGP during the first build.
agp_version=$(node "$workspace_dir/scripts/catalog-version.cjs" agp)
gradle_version=$(node "$workspace_dir/scripts/catalog-version.cjs" gradle)
model_jar=$(find "${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1/com.android.tools.build/builder-model/$agp_version" -name '*.jar' -type f | head -1)
mkdir -p "$workspace_dir/.tools/tooling-sync"
"$workspace_dir/scripts/with-env.sh" javac -cp "$workspace_dir/.tools/gradle-$gradle_version/lib/*:$model_jar" \
    -d "$workspace_dir/.tools/tooling-sync" "$workspace_dir/verification/ToolingSync.java"
"$workspace_dir/scripts/with-env.sh" java \
    -cp "$workspace_dir/.tools/tooling-sync:$workspace_dir/.tools/gradle-$gradle_version/lib/*:$model_jar" \
    ToolingSync "$workspace_dir/${1:-native}" "$workspace_dir/.tools/gradle-$gradle_version"
