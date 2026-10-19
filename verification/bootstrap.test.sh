#!/bin/sh
set -eu

workspace_dir=$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd -P)
test_root=$(mktemp -d "${TMPDIR:-/tmp}/cordova-bootstrap.XXXXXX")
trap 'rm -rf "$test_root"' EXIT
trap 'exit 1' HUP INT TERM

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

fixture_dir="$test_root/日本語 workspace"
mkdir -p "$fixture_dir/scripts" "$fixture_dir/native/gradle" "$fixture_dir/CordovaComposeApp"
for script in bootstrap.sh cordova.sh with-env.sh catalog-version.sh; do
  cp "$workspace_dir/scripts/$script" "$fixture_dir/scripts/$script"
done
cp "$workspace_dir/package.json" "$workspace_dir/package-lock.json" "$fixture_dir/"
cp "$workspace_dir/CordovaComposeApp/package.json" "$workspace_dir/CordovaComposeApp/package-lock.json" \
  "$fixture_dir/CordovaComposeApp/"
cp "$workspace_dir/native/gradle/libs.versions.toml" "$fixture_dir/native/gradle/"
gradle_version=$("$workspace_dir/scripts/catalog-version.sh" gradle)
mkdir -p "$fixture_dir/.tools/gradle-$gradle_version/bin"
# Stop after both real npm installs, before platform creation or any Gradle work.
cat >"$fixture_dir/.tools/gradle-$gradle_version/bin/gradle" <<'SH'
#!/bin/sh
exit 73
SH
chmod +x "$fixture_dir/.tools/gradle-$gradle_version/bin/gradle"

bootstrap_status=0
(
  cd "$test_root"
  NODE_ENV=production npm_config_omit=dev npm_config_bin_links=false \
    "$fixture_dir/scripts/bootstrap.sh"
) >"$test_root/bootstrap.log" 2>&1 || bootstrap_status=$?
if [ "$bootstrap_status" -ne 73 ]; then
  cat "$test_root/bootstrap.log" >&2
  fail "bootstrap did not reach the Gradle boundary (exit $bootstrap_status)"
fi
[ -f "$fixture_dir/node_modules/cordova/bin/cordova" ] || fail 'real Cordova CLI was not installed'
[ ! -e "$fixture_dir/node_modules/.bin/cordova" ] || fail 'bin-links=false did not reproduce the missing .bin command'
[ -f "$fixture_dir/CordovaComposeApp/node_modules/cordova-android/package.json" ] \
  || fail 'Cordova Android dependency was not installed'
echo 'PASS: bootstrap installs Cordova CLI with NODE_ENV=production, omit=dev and bin-links=false'

expected_version=$(node -p 'require(process.argv[1]).devDependencies.cordova' "$workspace_dir/package.json")
actual_version=$(cd "$test_root" && "$fixture_dir/scripts/cordova.sh" --version)
[ "$actual_version" = "$expected_version" ] || fail "unexpected CLI version: $actual_version"
echo 'PASS: wrapper invokes the installed CLI without .bin links, from outside a workspace with spaces and Unicode'

mv "$fixture_dir/node_modules/cordova" "$test_root/installed-cordova"
if "$fixture_dir/scripts/cordova.sh" --version >"$test_root/missing.log" 2>&1; then
  fail 'missing CLI unexpectedly succeeded'
fi
grep -Fq 'Cordova CLI is missing:' "$test_root/missing.log" || fail 'missing CLI path was not explained'
grep -Fq 'npm ci --include=dev' "$test_root/missing.log" || fail 'recovery command was not explained'
echo 'PASS: a missing CLI reports its path and the recovery command'
echo '3 tests passed.'
