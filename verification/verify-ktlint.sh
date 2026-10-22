#!/bin/sh
# 独立した一時Gradleプロジェクトで本物のktlintを動かし、検出・非変更・自動修正を確認する。
set -eu

# 通常プロジェクトのktlintCheckはdetektの互換名なので、ktlint Pluginとエンジン自体は別に適用する。
workspace_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd -P)
fixture_dir=$(mktemp -d "${TMPDIR:-/tmp}/workspace-ktlint.XXXXXX")
trap 'rm -rf "$fixture_dir"' EXIT
trap 'exit 1' HUP INT TERM

plugin_version=$("$workspace_dir/scripts/catalog-version.sh" ktlint)
engine_version=$("$workspace_dir/scripts/catalog-version.sh" ktlint-engine)
kotlin_version=$("$workspace_dir/scripts/catalog-version.sh" kotlin)

# 一時プロジェクトとコピーした入力だけを使い、通常ソース・依存設定は変更しない。
cat >"$fixture_dir/settings.gradle" <<'GRADLE'
pluginManagement { repositories { gradlePluginPortal(); mavenCentral() } }
rootProject.name = 'ktlint-execution-probe'
GRADLE
cat >"$fixture_dir/build.gradle" <<GRADLE
plugins {
    id 'org.jetbrains.kotlin.jvm' version '$kotlin_version'
    id 'org.jlleitschuh.gradle.ktlint' version '$plugin_version'
}
repositories { mavenCentral() }
ktlint {
    version = '$engine_version'
    filter { exclude '**/*.gradle.kts' }
}
GRADLE
mkdir -p "$fixture_dir/src/main/kotlin"
for module in feature data; do
  cp -R "$workspace_dir/native/$module/src" "$fixture_dir/src/main/kotlin/$module"
done
cat >"$fixture_dir/src/main/kotlin/Answer.kt" <<'KOTLIN'
package fixture

class Answer {
    fun value(): Int = 42
}
KOTLIN

# 実際のWrapperと共有環境を使い、呼び出し元のJDKや作業ディレクトリに依存させない。
run_gradle() {
  "$workspace_dir/scripts/with-env.sh" "$workspace_dir/native/gradlew" \
    -p "$fixture_dir" --console=plain --no-watch-fs "$@"
}

echo "ktlint Gradle Plugin $plugin_version / ktlint engine $engine_version / Kotlin $kotlin_version"
run_gradle ktlintCheck

# 故意の末尾空白を拒否することと、checkが入力を書き換えないことを確かめてからformatを実行する。
printf '\n    \n' >>"$fixture_dir/src/main/kotlin/Answer.kt"
cp "$fixture_dir/src/main/kotlin/Answer.kt" "$fixture_dir/malformed-before.kt"
if run_gradle ktlintCheck >"$fixture_dir/rejected.log" 2>&1; then
  cat "$fixture_dir/rejected.log"
  echo 'FAIL: ktlint accepted deliberately malformed Kotlin.' >&2
  exit 1
fi
cat "$fixture_dir/rejected.log"
if ! grep -Fq 'Trailing space(s)' "$fixture_dir/rejected.log"; then
  echo 'FAIL: ktlint did not report the expected formatting violation.' >&2
  exit 1
fi
cmp "$fixture_dir/malformed-before.kt" "$fixture_dir/src/main/kotlin/Answer.kt"
run_gradle ktlintFormat
run_gradle ktlintCheck
echo 'PASS: clean Kotlin accepted, malformed Kotlin rejected, formatted Kotlin accepted.'
