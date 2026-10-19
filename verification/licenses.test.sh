#!/bin/sh
set -eu

workspace_dir=$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd -P)
command -v jq >/dev/null 2>&1 || { echo 'jq is required for license tests.' >&2; exit 1; }
test_root=$(mktemp -d "${TMPDIR:-/tmp}/license-tests.XXXXXX")
trap 'rm -rf "$test_root"' EXIT
trap 'exit 1' HUP INT TERM
passed=0

fail() {
  echo "FAIL: $case_name: $*" >&2
  exit 1
}

fixture() {
  case_name=$1
  case_dir="$test_root/$case_name"
  fixture_dir="$case_dir/日本語 workspace"
  app_dir="$fixture_dir/CordovaComposeApp"
  mkdir -p "$fixture_dir/scripts" "$fixture_dir/native" "$app_dir/www/licenses"
  cp "$workspace_dir/scripts/licenses.sh" "$fixture_dir/scripts/licenses.sh"
  cp -R "$workspace_dir/native/third-party-licenses" "$fixture_dir/native/third-party-licenses"
  cp "$workspace_dir/NOTICE" "$fixture_dir/NOTICE"
  cp "$workspace_dir/CordovaComposeApp/package.json" "$app_dir/package.json"
  cp "$workspace_dir/CordovaComposeApp/package-lock.json" "$app_dir/package-lock.json"
  cp "$workspace_dir/CordovaComposeApp/www/index.html" "$app_dir/www/index.html"
  cp "$workspace_dir/CordovaComposeApp/www/licenses/THIRD_PARTY_NOTICES.txt" "$app_dir/www/licenses/THIRD_PARTY_NOTICES.txt"
  jq -r '.[] | .path' "$fixture_dir/native/third-party-licenses/cordova-sources.json" >"$case_dir/source-paths"
  while IFS= read -r source_path; do
    mkdir -p "$fixture_dir/$source_path"
    for source_file in package.json LICENSE NOTICE; do
      cp "$workspace_dir/$source_path/$source_file" "$fixture_dir/$source_path/$source_file"
    done
  done <"$case_dir/source-paths"
  runtime_file="$fixture_dir/runtime inventory.json"
  jq '. as $manifest | [.variants[] as $variant | {key: $variant, value:
    [$manifest.components[] | select((.variants // $manifest.variants) | index($variant))
    | {coordinate, sha256, path: ("/cache/日本語 artifacts/" + .artifact)}]}] | from_entries' \
    "$fixture_dir/native/third-party-licenses/runtime.json" >"$runtime_file"
  assets_dir="$fixture_dir/prepared www"
  mkdir -p "$assets_dir/licenses"
  cp "$app_dir/www/index.html" "$assets_dir/index.html"
  cp "$app_dir/www/licenses/THIRD_PARTY_NOTICES.txt" "$assets_dir/licenses/THIRD_PARTY_NOTICES.txt"
}

edit_json() {
  jq "$1" "$2" >"$case_dir/edited.json"
  mv "$case_dir/edited.json" "$2"
}

run_license() {
  sh "$fixture_dir/scripts/licenses.sh" "$@"
}

pass() {
  passed=$((passed + 1))
  echo "PASS: $case_name"
}

expect_failure() {
  expected=$1
  shift
  cp "$fixture_dir/NOTICE" "$case_dir/notice-before"
  cp "$app_dir/www/licenses/THIRD_PARTY_NOTICES.txt" "$case_dir/bundled-before"
  if run_license "$@" >"$case_dir/result.log" 2>&1; then
    fail 'license verification unexpectedly succeeded'
  fi
  if ! grep -Fq "$expected" "$case_dir/result.log"; then
    cat "$case_dir/result.log" >&2
    fail "missing expected error: $expected"
  fi
  cmp -s "$case_dir/notice-before" "$fixture_dir/NOTICE" || fail 'check modified NOTICE'
  cmp -s "$case_dir/bundled-before" "$app_dir/www/licenses/THIRD_PARTY_NOTICES.txt" || fail 'check modified bundled text'
  pass
}

fixture generate-byte-identical-and-check-from-another-directory
# Compare the shell-generated files with the existing complete notices, not a second renderer.
run_license generate
cmp -s "$workspace_dir/NOTICE" "$fixture_dir/NOTICE" || fail 'generated NOTICE differs'
cmp -s "$workspace_dir/CordovaComposeApp/www/licenses/THIRD_PARTY_NOTICES.txt" \
  "$app_dir/www/licenses/THIRD_PARTY_NOTICES.txt" || fail 'generated bundled text differs'
(
  cd "$fixture_dir"
  run_license check 'runtime inventory.json' 'prepared www'
)
pass

fixture rejects-new-transitive-dependency
edit_json '.release += [{coordinate: "unreviewed:library:2.0", sha256: "new", path: "/cache/new.jar"}]' "$runtime_file"
expect_failure 'License review required: release: unreviewed:library:2.0' check "$runtime_file"

fixture rejects-changed-artifact-hash
edit_json '.debug[0].sha256 = "changed"' "$runtime_file"
expect_failure 'License review required: debug:' check "$runtime_file"

fixture rejects-changed-artifact-name
edit_json '.debug[0].path = "/cache/renamed.jar"' "$runtime_file"
expect_failure 'License review required: debug:' check "$runtime_file"

fixture requires-both-variants
edit_json 'del(.release)' "$runtime_file"
expect_failure 'Missing runtime inventory: release' check "$runtime_file"

fixture rejects-non-array-inventory
edit_json '.release = {}' "$runtime_file"
expect_failure 'Missing runtime inventory: release' check "$runtime_file"

fixture rejects-empty-inventory
edit_json '.debug = []' "$runtime_file"
expect_failure 'Empty runtime inventory: debug' check "$runtime_file"

fixture rejects-removed-dependency
edit_json '.release |= .[1:]' "$runtime_file"
expect_failure 'License inventory is stale: release' check "$runtime_file"

fixture rejects-debug-only-dependency-in-release
jq -r '.components[] | select(.variants == ["debug"]) | .coordinate' \
  "$fixture_dir/native/third-party-licenses/runtime.json" >"$case_dir/debug-only"
debug_only=$(head -n 1 "$case_dir/debug-only")
[ -n "$debug_only" ] || fail 'expected a debug-only tooling dependency'
jq --arg coordinate "$debug_only" '.release += [.debug[] | select(.coordinate == $coordinate)]' \
  "$runtime_file" >"$case_dir/edited.json"
mv "$case_dir/edited.json" "$runtime_file"
expect_failure "License review required: release: $debug_only" check "$runtime_file"

fixture rejects-missing-debug-only-dependency
jq -r '.components[] | select(.variants == ["debug"]) | .coordinate' \
  "$fixture_dir/native/third-party-licenses/runtime.json" >"$case_dir/debug-only"
debug_only=$(head -n 1 "$case_dir/debug-only")
jq --arg coordinate "$debug_only" '.debug |= map(select(.coordinate != $coordinate))' \
  "$runtime_file" >"$case_dir/edited.json"
mv "$case_dir/edited.json" "$runtime_file"
expect_failure 'License inventory is stale: debug' check "$runtime_file"

fixture rejects-stale-prepared-notices
printf 'stale notices\n' >"$assets_dir/licenses/THIRD_PARTY_NOTICES.txt"
expect_failure 'Stale prepared license asset: licenses/THIRD_PARTY_NOTICES.txt' check "$runtime_file" "$assets_dir"

fixture rejects-missing-prepared-notices
rm "$assets_dir/licenses/THIRD_PARTY_NOTICES.txt"
expect_failure 'Stale prepared license asset: licenses/THIRD_PARTY_NOTICES.txt' check "$runtime_file" "$assets_dir"

fixture rejects-stale-prepared-html
printf 'stale html\n' >"$assets_dir/index.html"
expect_failure 'Stale prepared license asset: index.html' check "$runtime_file" "$assets_dir"

fixture rejects-stale-notice
printf 'stale notice\n' >"$fixture_dir/NOTICE"
expect_failure 'Stale license output: NOTICE' check

fixture rejects-stale-bundled-text
printf 'stale text\n' >"$app_dir/www/licenses/THIRD_PARTY_NOTICES.txt"
expect_failure 'Stale license output: CordovaComposeApp/www/licenses/THIRD_PARTY_NOTICES.txt' check

fixture rejects-changed-upstream-notice
printf '\nchanged\n' >>"$app_dir/node_modules/cordova-android/NOTICE"
expect_failure 'License review required: cordova-android/NOTICE changed' check

fixture rejects-changed-license-copy
printf '\nchanged\n' >>"$fixture_dir/native/third-party-licenses/cordova-android-LICENSE.txt"
expect_failure 'Bundled upstream license differs:' check

fixture rejects-changed-reviewed-supplemental-license
printf '\nchanged\n' >>"$fixture_dir/native/third-party-licenses/MPL-2.0.txt"
run_license generate
expect_failure 'Bundled upstream license differs: MPL-2.0.txt' check

fixture rejects-changed-cordova-version
edit_json '.version = "99.0.0"' "$app_dir/node_modules/cordova-android/package.json"
expect_failure 'License review required: cordova-android version changed' check

fixture rejects-changed-template-version
edit_json '.version = "99.0.0"' "$fixture_dir/node_modules/cordova-app-hello-world/package.json"
expect_failure 'License review required: cordova-app-hello-world version changed' check

fixture rejects-changed-lock-version
edit_json '.packages["node_modules/cordova-android"].version = "99.0.0"' "$app_dir/package-lock.json"
expect_failure 'License review required: cordova-android lock version changed' check

fixture rejects-added-application-dependency
edit_json '.dependencies["unreviewed-library"] = "1.0.0"' "$app_dir/package.json"
expect_failure 'License review required for added application dependencies or Cordova plugins' check

fixture rejects-configured-cordova-plugin
edit_json '.cordova.plugins["unreviewed-plugin"] = {}' "$app_dir/package.json"
expect_failure 'License review required for added application dependencies or Cordova plugins' check

fixture rejects-installed-cordova-plugin
mkdir -p "$app_dir/plugins"
printf '{"unreviewed-plugin": {}}\n' >"$app_dir/plugins/fetch.json"
expect_failure 'License review required for installed Cordova plugins' check

fixture rejects-malformed-runtime-json
printf '{broken\n' >"$runtime_file"
expect_failure 'parse error' check "$runtime_file"

echo "$passed tests passed."
