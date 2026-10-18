#!/bin/sh
set -eu

source_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd -P)
test_root=$(mktemp -d "${TMPDIR:-/tmp}/native-git-hooks.XXXXXX")
trap 'rm -rf "$test_root"' EXIT
trap 'exit 1' HUP INT TERM

# 利用者のGit設定・作業リポジトリからテストを分離する。
export GIT_CONFIG_NOSYSTEM=1
export GIT_CONFIG_GLOBAL=/dev/null
unset GIT_DIR GIT_WORK_TREE GIT_INDEX_FILE GIT_COMMON_DIR

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

fixture() {
  case_dir="$test_root/$1"
  fixture_dir="$case_dir/日本語 workspace"
  mkdir -p "$fixture_dir/scripts"
  cp "$source_dir/scripts/setup-git-hooks.sh" "$fixture_dir/scripts/setup-git-hooks.sh"
  git -C "$fixture_dir" init --quiet --template=
  git -C "$fixture_dir" config user.name 'Hook Test'
  git -C "$fixture_dir" config user.email 'hook@example.invalid'
  git -C "$fixture_dir" config commit.gpgSign false
  hook_file="$fixture_dir/.git/hooks/pre-commit"
  mkdir -p "$fixture_dir/.git/hooks"
}

setup() {
  (cd "$case_dir" && sh "$fixture_dir/scripts/setup-git-hooks.sh") >"$case_dir/setup.log" 2>&1
}

expect_setup_success() {
  if ! setup; then
    cat "$case_dir/setup.log" >&2
    fail 'setup should succeed'
  fi
}

expect_setup_failure() {
  if setup; then
    fail 'setup should fail'
  fi
}

fixture repeat
expect_setup_success
[ -x "$hook_file" ] || fail 'installed hook is not executable'
cp "$hook_file" "$case_dir/before"
hook_inode=$(ls -di "$hook_file" | awk '{print $1}')
chmod 644 "$hook_file"
expect_setup_success
cmp -s "$hook_file" "$case_dir/before" || fail 'repeated setup changed the hook'
[ "$(ls -di "$hook_file" | awk '{print $1}')" = "$hook_inode" ] || fail 'repeated setup replaced the hook'
[ -x "$hook_file" ] || fail 'repeated setup did not restore execution permission'
echo 'PASS: installs an executable hook and can be repeated without replacing it'

fixture commit
expect_setup_success
cat >"$fixture_dir/scripts/with-env.sh" <<'SH'
#!/bin/sh
printf '%s\n' "$PWD" "$@" >"$HOOK_CALL_LOG"
exit "${HOOK_EXIT_CODE:-0}"
SH
chmod +x "$fixture_dir/scripts/with-env.sh"
if HOOK_CALL_LOG="$case_dir/call.log" HOOK_EXIT_CODE=42 \
  git -C "$fixture_dir/scripts" commit --allow-empty -m 'Hook check' >"$case_dir/commit.log" 2>&1; then
  fail 'Git accepted a commit after the hook failed'
fi
if git -C "$fixture_dir" rev-parse --verify HEAD >/dev/null 2>&1; then
  fail 'failed commit created HEAD'
fi
printf '%s\n' "$(CDPATH= cd -- "$fixture_dir" && pwd -P)" './native/gradlew' '-p' 'native' ':preCommit' >"$case_dir/expected.log"
cmp -s "$case_dir/call.log" "$case_dir/expected.log" || fail 'hook used the wrong directory or arguments'
if ! HOOK_CALL_LOG="$case_dir/call.log" HOOK_EXIT_CODE=0 \
  git -C "$fixture_dir/scripts" commit --allow-empty -m 'Hook check' >"$case_dir/commit.log" 2>&1; then
  cat "$case_dir/commit.log" >&2
  fail 'Git rejected a commit after the hook succeeded'
fi
[ "$(git -C "$fixture_dir" log -1 --format=%s)" = 'Hook check' ] || fail 'successful commit is missing'
echo 'PASS: Git invokes Gradle through with-env and follows its exit status'

fixture existing
printf '#!/bin/sh\necho existing hook\n' >"$hook_file"
chmod 644 "$hook_file"
cp "$hook_file" "$case_dir/before"
expect_setup_failure
grep -q 'not overwritten' "$case_dir/setup.log" || fail 'missing existing-hook error'
cmp -s "$hook_file" "$case_dir/before" || fail 'existing hook was modified'
[ "$(ls -ld "$hook_file" | awk '{print substr($1, 1, 10)}')" = '-rw-r--r--' ] || fail 'existing hook permissions changed'
echo 'PASS: refuses to overwrite an existing custom hook'

fixture symlink
link_target="$case_dir/missing-hook"
ln -s "$link_target" "$hook_file"
expect_setup_failure
[ -L "$hook_file" ] || fail 'symbolic link was replaced'
[ "$(readlink "$hook_file")" = "$link_target" ] || fail 'symbolic link target changed'
[ ! -e "$link_target" ] || fail 'symbolic link target was created'
echo 'PASS: refuses to replace a symbolic link'

fixture configured
git -C "$fixture_dir" config core.hooksPath .custom-hooks
expect_setup_failure
grep -q 'core.hooksPath' "$case_dir/setup.log" || fail 'missing hooksPath error'
[ "$(git -C "$fixture_dir" config --get core.hooksPath)" = '.custom-hooks' ] || fail 'hooksPath was changed'
[ ! -e "$hook_file" ] || fail 'installed a hook into an inactive directory'
[ ! -e "$fixture_dir/.custom-hooks" ] || fail 'custom hooks directory was modified'
echo 'PASS: preserves core.hooksPath and does not install into an inactive directory'

echo '5 tests passed.'
