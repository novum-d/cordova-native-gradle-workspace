#!/bin/sh
set -eu

source_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd -P)
test_root=$(mktemp -d "${TMPDIR:-/tmp}/cordova-native-hook.XXXXXX")
trap 'rm -rf "$test_root"' EXIT
trap 'exit 1' HUP INT TERM

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

fixture() {
  case_dir="$test_root/$1"
  fixture_dir="$case_dir/日本語 workspace"
  app_dir="$fixture_dir/CordovaComposeApp"
  for relative in native/gradle/libs.versions.toml native/gradle/settings-extras.gradle \
    native/gradle/build-extras.gradle native/feature/build.gradle.kts native/data/build.gradle.kts \
    native/gradle/git-hooks.gradle.kts native/gradle/licenses.gradle scripts/licenses.js config/detekt/detekt.yml config/detekt/formatting.yml \
    config/detekt/autocorrect.yml verification/catalog-verification.gradle \
    CordovaComposeApp/platforms/android/settings.gradle CordovaComposeApp/platforms/android/app/build-extras.gradle; do
    mkdir -p "$(dirname -- "$fixture_dir/$relative")"
    printf '// existing plugin integration\n' >"$fixture_dir/$relative"
  done
  mkdir -p "$app_dir/node_modules/cordova-android"
  printf '{"version":"14.0.1"}\n' >"$app_dir/node_modules/cordova-android/package.json"
  printf 'rootProject.name = "ExistingApp"\n' >"$app_dir/platforms/android/cdv-gradle-name.gradle"
  settings_file="$app_dir/platforms/android/settings.gradle"
  name_file="$app_dir/platforms/android/cdv-gradle-name.gradle"
  extras_file="$app_dir/platforms/android/app/build-extras.gradle"
}

run_hook() {
  # CordovaのJavaScript関数を実行する部分だけNode.jsを使う。
  node - "$source_dir/CordovaComposeApp/hooks/copy-gradle-extras.js" "$app_dir" "${1:-android}" \
    >"$case_dir/hook.log" 2>&1 <<'JS'
const hook = require(process.argv[2]);
hook({ hook: 'test', opts: { projectRoot: process.argv[3], platforms: [process.argv[4]] } });
JS
}

expect_hook_success() {
  if ! run_hook "${1:-android}"; then
    cat "$case_dir/hook.log" >&2
    fail 'Cordova hook should succeed'
  fi
}

expect_hook_failure() {
  if run_hook; then
    fail 'Cordova hook should fail'
  fi
  grep -q "$1" "$case_dir/hook.log" || fail 'missing expected Cordova hook error'
}

fixture repeat
cp "$settings_file" "$case_dir/settings-before"
expect_hook_success
cp "$name_file" "$case_dir/name-after-first-run"
cp "$extras_file" "$case_dir/extras-after-first-run"
expect_hook_success
expect_hook_success
cmp -s "$name_file" "$case_dir/name-after-first-run" || fail 'repeated hook changed the name file'
cmp -s "$extras_file" "$case_dir/extras-after-first-run" || fail 'repeated hook changed extras'
cmp -s "$settings_file" "$case_dir/settings-before" || fail 'hook changed generated settings'
grep -q ExistingApp "$name_file" || fail 'existing root project name was removed'
grep -q 'existing plugin integration' "$extras_file" || fail 'existing plugin settings were removed'
[ "$(grep -c 'BEGIN CORDOVA_NATIVE_GRADLE' "$extras_file")" -eq 1 ] || fail 'integration marker was duplicated'
echo 'PASS: preserves existing plugin settings and is byte-identical after repeated runs'

fixture missing
cp "$name_file" "$case_dir/name-before"
rm "$fixture_dir/native/data/build.gradle.kts"
expect_hook_failure 'Required integration file missing'
cmp -s "$name_file" "$case_dir/name-before" || fail 'missing-module failure changed generated files'
echo 'PASS: fails before modifying generated files when a native module is missing'

fixture damaged
printf '// BEGIN CORDOVA_NATIVE_GRADLE\n// existing custom plugin' >"$extras_file"
cp "$extras_file" "$case_dir/extras-before"
expect_hook_failure 'Malformed integration markers'
cmp -s "$extras_file" "$case_dir/extras-before" || fail 'damaged-marker failure changed existing content'
echo 'PASS: rejects damaged markers instead of erasing unrelated Gradle content'

case_dir="$test_root/non-android"
mkdir -p "$case_dir"
app_dir="$case_dir/not-an-android-project"
expect_hook_success ios
[ ! -e "$app_dir" ] || fail 'non-Android prepare created project files'
echo 'PASS: does not touch Android for a non-Android prepare'

echo '4 tests passed.'
