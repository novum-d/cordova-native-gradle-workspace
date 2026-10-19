#!/bin/sh
set -eu

fail() {
  echo "$*" >&2
  exit 1
}

mode=${1:-}
case "$mode" in
  generate|check) ;;
  *) fail 'Usage: sh scripts/licenses.sh generate|check [runtime-artifacts.json] [prepared-www]' ;;
esac
[ "$#" -le 3 ] || fail 'Too many arguments'
command -v jq >/dev/null 2>&1 || fail 'jq is required for license generation and verification; install jq and retry.'

workspace_dir=$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd -P)
licenses_dir="$workspace_dir/native/third-party-licenses"
manifest="$licenses_dir/runtime.json"
temporary_dir=$(mktemp -d "${TMPDIR:-/tmp}/workspace-licenses.XXXXXX")
trap 'rm -rf "$temporary_dir"' EXIT
trap 'exit 1' HUP INT TERM

# Keep input arguments relative to the caller; do not change the working directory.
runtime_file=${2:-}
assets_dir=${3:-}

render_documents() {
  first=true
  for document in "$@"; do
    if [ "$first" = false ]; then printf '\n'; fi
    first=false
    printf '%s\n' "$document"
    # Match the original trimEnd() while preserving internal whitespace and UTF-8.
    jq -Rrs 'sub("[\\s\uFEFF]+$"; "")' "$licenses_dir/$document"
  done
}

render_documents cordova-template-NOTICE.txt cordova-android-NOTICE.txt \
  kotlin-NOTICE.txt coroutines-1.8.1-NOTICE.txt jakarta-inject-NOTICE.md \
  okhttp-public-suffix-NOTICE.txt >"$temporary_dir/NOTICE"
jq -jr '.components | map(
  "\(.coordinate) — \(.license)"
  + (if .supplementalLicenses then "; includes " + (.supplementalLicenses | join(", ")) else "" end)
  + "\nLicense evidence: \(.licenseEvidence // .licensePom)\n"
) | join("\n")' "$manifest" >"$temporary_dir/components"
render_documents Apache-2.0.txt cordova-android-LICENSE.txt webkit-LICENSE.txt \
  kotlin-COPYRIGHT.txt attributions.txt kotlin-boost-LICENSE.txt \
  kotlin-gwt-LICENSE.txt kotlin-guava-LICENSE.txt >"$temporary_dir/licenses"
jq -r '.licenseDocuments[]? | select(.bundled) | .file' "$manifest" >"$temporary_dir/license-documents"
while IFS= read -r document; do
  printf '\n' >>"$temporary_dir/licenses"
  render_documents "$document" >>"$temporary_dir/licenses"
done <"$temporary_dir/license-documents"
{
  printf 'Third-party software notices\n\n'
  printf 'Apache Cordova Android 14.0.1 / Cordova Hello World template 7.0.0: Apache-2.0\n\n'
  cat "$temporary_dir/components"
  printf '\n'
  cat "$temporary_dir/NOTICE"
  printf '\n'
  cat "$temporary_dir/licenses"
} >"$temporary_dir/THIRD_PARTY_NOTICES.txt"

bundled_text="$workspace_dir/CordovaComposeApp/www/licenses/THIRD_PARTY_NOTICES.txt"
if [ "$mode" = generate ]; then
  mkdir -p "$(dirname -- "$bundled_text")"
  cp "$temporary_dir/NOTICE" "$workspace_dir/NOTICE"
  cp "$temporary_dir/THIRD_PARTY_NOTICES.txt" "$bundled_text"
  echo 'Generated NOTICE and bundled license text'
  exit 0
fi

sha256() {
  if command -v shasum >/dev/null 2>&1; then
    hash_output=$(shasum -a 256 <"$1")
  elif command -v sha256sum >/dev/null 2>&1; then
    hash_output=$(sha256sum <"$1")
  else
    fail 'shasum or sha256sum is required for license verification.'
  fi
  printf '%s\n' "${hash_output%% *}"
}

jq -c '.licenseDocuments[]?' "$manifest" >"$temporary_dir/managed-documents"
while IFS= read -r document; do
  file=$(printf '%s' "$document" | jq -er '.file')
  expected_hash=$(printf '%s' "$document" | jq -er '.sha256')
  actual_hash=$(sha256 "$licenses_dir/$file")
  [ "$actual_hash" = "$expected_hash" ] || fail "Bundled upstream license differs: $file"
done <"$temporary_dir/managed-documents"

jq -e '(((.dependencies // {} | keys) - ["cordova-android"]) | length == 0)
  and ((.cordova.plugins // {}) | length == 0)' \
  "$workspace_dir/CordovaComposeApp/package.json" >/dev/null \
  || fail 'License review required for added application dependencies or Cordova plugins'
plugin_file="$workspace_dir/CordovaComposeApp/plugins/fetch.json"
if [ -f "$plugin_file" ]; then
  jq -e 'length == 0' "$plugin_file" >/dev/null \
    || fail 'License review required for installed Cordova plugins'
fi

# Use temporary JSON-lines files so jq failures cannot be hidden by shell pipelines.
jq -c 'to_entries[]' "$licenses_dir/cordova-sources.json" >"$temporary_dir/sources"
while IFS= read -r source; do
  source_name=$(printf '%s' "$source" | jq -er '.key')
  source_path=$(printf '%s' "$source" | jq -er '.value.path')
  source_version=$(printf '%s' "$source" | jq -er '.value.version')
  jq -e --arg version "$source_version" '.version == $version' \
    "$workspace_dir/$source_path/package.json" >/dev/null \
    || fail "License review required: $source_name version changed"
  if [ "$source_name" = cordova-android ]; then
    jq -e --arg version "$source_version" '.packages["node_modules/cordova-android"].version == $version' \
      "$workspace_dir/CordovaComposeApp/package-lock.json" >/dev/null \
      || fail 'License review required: cordova-android lock version changed'
  fi
  printf '%s' "$source" | jq -c '.value as $source | .value.files | to_entries[]
    | {file: .key, hash: .value, copy: $source.copies[.key]}' >"$temporary_dir/source-files"
  while IFS= read -r source_file; do
    file=$(printf '%s' "$source_file" | jq -er '.file')
    expected_hash=$(printf '%s' "$source_file" | jq -er '.hash')
    copy=$(printf '%s' "$source_file" | jq -er '.copy')
    actual_hash=$(sha256 "$workspace_dir/$source_path/$file")
    [ "$actual_hash" = "$expected_hash" ] || fail "License review required: $source_name/$file changed"
    copy_hash=$(sha256 "$workspace_dir/$copy")
    [ "$copy_hash" = "$expected_hash" ] || fail "Bundled upstream license differs: $copy"
  done <"$temporary_dir/source-files"
done <"$temporary_dir/sources"

cmp -s "$temporary_dir/NOTICE" "$workspace_dir/NOTICE" \
  || fail 'Stale license output: NOTICE; run npm run licenses:generate'
cmp -s "$temporary_dir/THIRD_PARTY_NOTICES.txt" "$bundled_text" \
  || fail 'Stale license output: CordovaComposeApp/www/licenses/THIRD_PARTY_NOTICES.txt; run npm run licenses:generate'

if [ -n "$runtime_file" ]; then
  jq -e --slurpfile reviewed "$manifest" '
    . as $actual | $reviewed[0] as $manifest
    | $manifest.variants[] as $variant
    | ($manifest.components | map(select((.variants // $manifest.variants) | index($variant)))
      | map({key: .coordinate, value: .}) | from_entries) as $entries
    | $actual[$variant] as $artifacts
    | if ($artifacts | type) != "array" then error("Missing runtime inventory: \($variant)")
      elif ($artifacts | length) == 0 then error("Empty runtime inventory: \($variant)")
      else
        ($artifacts[] | . as $artifact | $entries[.coordinate] as $entry
          | if $entry == null or $entry.sha256 != .sha256 or $entry.artifact != (.path | split("/") | last)
            then error("License review required: \($variant): \($artifact.coordinate)")
            else true end),
        (if ([$artifacts[].coordinate] | unique | length) != ($entries | length)
         then error("License inventory is stale: \($variant)") else true end)
      end' "$runtime_file" >/dev/null
fi

if [ -n "$assets_dir" ]; then
  cmp -s "$workspace_dir/CordovaComposeApp/www/index.html" "$assets_dir/index.html" \
    || fail 'Stale prepared license asset: index.html; run npm run cordova:prepare'
  cmp -s "$temporary_dir/THIRD_PARTY_NOTICES.txt" "$assets_dir/licenses/THIRD_PARTY_NOTICES.txt" \
    || fail 'Stale prepared license asset: licenses/THIRD_PARTY_NOTICES.txt; run npm run cordova:prepare'
fi

if [ -n "$runtime_file" ]; then
  echo 'License checks passed for resolved Android runtime artifacts'
else
  echo 'License checks passed (run Gradle verifyThirdPartyLicenses to check runtime dependencies)'
fi
