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
fixture_dir=$(CDPATH='' cd -- "$fixture_dir" && pwd -P)
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

api_dir="$fixture_dir/CordovaComposeApp/platforms/android/cordova"
mkdir -p "$api_dir"
printf "module.exports = require('cordova-android');\n" >"$api_dir/Api.js"
mv "$fixture_dir/CordovaComposeApp/node_modules/cordova-android" "$test_root/installed-cordova-android"
if NODE_PATH='' node -e 'require(process.argv[1])' "$api_dir/Api.js" >"$test_root/api.log" 2>&1; then
  fail 'generated API unexpectedly loaded without cordova-android'
fi
grep -Fq "Cannot find module 'cordova-android'" "$test_root/api.log" || fail 'missing backend error was not reproduced'
(
  cd "$fixture_dir/CordovaComposeApp"
  npm ci --include=dev --no-audit --no-fund
) >"$test_root/api-restore.log" 2>&1
NODE_PATH='' node -e 'const api = require(process.argv[1]); if (typeof api.createPlatform !== "function") process.exit(1)' "$api_dir/Api.js" \
  || fail 'npm ci did not restore the generated Android API'
echo 'PASS: reinstalling app dependencies restores Api.js loading without deleting the platform'

# Continue through real platform creation, while replacing only Gradle execution.
cp "$workspace_dir/CordovaComposeApp/config.xml" "$fixture_dir/CordovaComposeApp/"
cp -R "$workspace_dir/CordovaComposeApp/www" "$workspace_dir/CordovaComposeApp/hooks" "$fixture_dir/CordovaComposeApp/"
for relative in native/gradle/settings-extras.gradle native/gradle/build-extras.gradle \
  native/gradle/git-hooks.gradle.kts \
  native/feature/build.gradle.kts native/data/build.gradle.kts \
  native/gradle/config/detekt/detekt.yml native/gradle/config/detekt/formatting.yml \
  native/gradle/config/detekt/autocorrect.yml verification/catalog-verification.gradle; do
  mkdir -p "$(dirname -- "$fixture_dir/$relative")"
  cp "$workspace_dir/$relative" "$fixture_dir/$relative"
done
cat >"$fixture_dir/.tools/gradle-$gradle_version/bin/gradle" <<'SH'
#!/bin/sh
exit 0
SH

run_platform_bootstrap() {
  if ! (cd "$test_root" && "$fixture_dir/scripts/bootstrap.sh") >"$test_root/$1.log" 2>&1; then
    cat "$test_root/$1.log" >&2
    fail "platform bootstrap failed: $1"
  fi
}

android_dir="$fixture_dir/CordovaComposeApp/platforms/android"
mv "$android_dir" "$test_root/api-fixture-platform"
run_platform_bootstrap fresh-platform
[ -f "$android_dir/cordova/Api.js" ] || fail 'fresh bootstrap did not generate Api.js'
grep -Fq CORDOVA_NATIVE_GRADLE "$android_dir/app/build-extras.gradle" || fail 'fresh platform lacks native integration'
NODE_PATH='' node -e 'require(process.argv[1])' "$android_dir/cordova/Api.js" || fail 'fresh platform API cannot load'
echo 'PASS: bootstrap creates a missing Android platform with the real Cordova CLI'

# Configure the real fresh Gradle project, rather than checking marker files alone.
run_android_gradle() {
  "$workspace_dir/scripts/with-env.sh" "$workspace_dir/native/gradlew" \
    -p "$android_dir" help --console=plain >"$test_root/$1.log" 2>&1
}
if ! run_android_gradle fresh-gradle; then
  cat "$test_root/fresh-gradle.log" >&2
  fail 'the freshly generated Android Gradle project cannot load its shared catalog'
fi
echo 'PASS: a fresh platform configures successfully with the real Gradle and shared catalog'

# Cordova rewrites cdv-gradle-name.gradle on prepare. Skipping hooks leaves app
# extras in place but removes the Settings reference that registers libs.
if ! "$fixture_dir/scripts/cordova.sh" prepare android --nohooks=after_prepare >"$test_root/nohooks.log" 2>&1; then
  cat "$test_root/nohooks.log" >&2
  fail 'prepare without hooks failed'
fi
if run_android_gradle missing-catalog; then
  fail 'Gradle unexpectedly accepted app extras without the shared catalog'
fi
if ! grep -Fq "Shared Version Catalog 'libs' was not registered" "$test_root/missing-catalog.log"; then
  cat "$test_root/missing-catalog.log" >&2
  fail 'missing catalog was not explained'
fi
grep -Fq './scripts/cordova.sh prepare android' "$test_root/missing-catalog.log" \
  || fail 'missing catalog error lacks the recovery command'
echo 'PASS: prepare without hooks reproduces the missing catalog and reports its recovery command'

if ! "$fixture_dir/scripts/cordova.sh" prepare android >"$test_root/restore-hooks.log" 2>&1; then
  cat "$test_root/restore-hooks.log" >&2
  fail 'prepare could not restore the integration'
fi
if ! run_android_gradle restored-catalog; then
  cat "$test_root/restored-catalog.log" >&2
  fail 'prepare did not restore the shared catalog for Gradle'
fi
echo 'PASS: ordinary prepare restores the missing Settings integration and Gradle succeeds'

mv "$android_dir/cordova/Api.js" "$android_dir/cordova/Api.js.saved-for-test"
printf 'existing generated files must be preserved\n' >"$android_dir/keep-me.txt"
run_platform_bootstrap incomplete-platform
[ -f "$android_dir/cordova/Api.js" ] || fail 'incomplete platform was not regenerated'
backup_marker=$(find "$fixture_dir/.tools/cordova-platform-backups" -type f -name keep-me.txt)
[ -f "$backup_marker" ] || fail 'existing platform files were not backed up'
[ -f "$(dirname -- "$backup_marker")/cordova/Api.js.saved-for-test" ] || fail 'backup lost the old platform contents'
NODE_PATH='' node -e 'require(process.argv[1])' "$android_dir/cordova/Api.js" || fail 'regenerated platform API cannot load'
echo 'PASS: a platform missing Api.js is backed up and regenerated without deleting existing files'

printf 'keep the healthy platform\n' >"$android_dir/healthy-platform-marker.txt"
run_platform_bootstrap healthy-platform
[ -f "$android_dir/healthy-platform-marker.txt" ] || fail 'healthy platform was replaced'
grep -Fq CORDOVA_NATIVE_GRADLE "$android_dir/app/build-extras.gradle" || fail 'prepare lost native integration'
if grep -Fq 'Incomplete Android platform' "$test_root/healthy-platform.log"; then
  fail 'healthy platform was incorrectly marked incomplete'
fi
echo 'PASS: a healthy platform is prepared without replacement'

mv "$fixture_dir/node_modules/cordova" "$test_root/installed-cordova"
if "$fixture_dir/scripts/cordova.sh" --version >"$test_root/missing.log" 2>&1; then
  fail 'missing CLI unexpectedly succeeded'
fi
grep -Fq 'Cordova CLI is missing:' "$test_root/missing.log" || fail 'missing CLI path was not explained'
grep -Fq 'npm ci --include=dev' "$test_root/missing.log" || fail 'recovery command was not explained'
echo 'PASS: a missing CLI reports its path and the recovery command'

node - "$fixture_dir/package.json" <<'JS'
const fs = require('node:fs');
const file = process.argv[2];
const pkg = JSON.parse(fs.readFileSync(file, 'utf8'));
delete pkg.devDependencies.cordova;
fs.writeFileSync(file, JSON.stringify(pkg, null, 2) + '\n');
JS
if "$fixture_dir/scripts/bootstrap.sh" >"$test_root/declaration.log" 2>&1; then
  fail 'bootstrap unexpectedly accepted a package.json without Cordova'
fi
grep -Fq "Cordova CLI is not declared in: $fixture_dir/package.json" "$test_root/declaration.log" \
  || fail 'bootstrap did not identify the root package.json missing the dependency'
echo 'PASS: a missing root Cordova declaration is identified before npm ci'

cp "$workspace_dir/package.json" "$fixture_dir/package.json"
node - "$fixture_dir/CordovaComposeApp/package.json" <<'JS'
const fs = require('node:fs');
const file = process.argv[2];
const pkg = JSON.parse(fs.readFileSync(file, 'utf8'));
if (pkg.dependencies) delete pkg.dependencies['cordova-android'];
if (pkg.devDependencies) delete pkg.devDependencies['cordova-android'];
fs.writeFileSync(file, JSON.stringify(pkg, null, 2) + '\n');
JS
if "$fixture_dir/scripts/bootstrap.sh" >"$test_root/backend-declaration.log" 2>&1; then
  fail 'bootstrap unexpectedly accepted an app without cordova-android'
fi
grep -Fq "Cordova Android is not declared in: $fixture_dir/CordovaComposeApp/package.json" "$test_root/backend-declaration.log" \
  || fail 'bootstrap did not identify the app package.json missing the backend'
echo 'PASS: a missing app backend declaration is identified before npm ci'
echo '12 tests passed.'
