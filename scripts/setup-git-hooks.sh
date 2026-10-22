#!/bin/sh
# 各cloneのpre-commitに、with-env経由でGradle :preCommitを実行する入口を設置する。
set -eu

workspace_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd -P)
cd "$workspace_dir"

# 別リポジトリのhookを誤って変更しないよう、Gitとworkspaceの実体パスを照合する。
git_root=$(git rev-parse --show-toplevel)
if [ "$(CDPATH= cd -- "$git_root" && pwd -P)" != "$workspace_dir" ]; then
  echo 'Run this script from a checkout whose Git root is the workspace root.' >&2
  exit 1
fi

# スコープを問わずcore.hooksPathが設定されていれば、既存のhook運用を優先して停止する。
if git config --get core.hooksPath >/dev/null; then
  echo 'core.hooksPath is configured; integrate :preCommit into the existing hooks instead.' >&2
  exit 1
fi

hooks_dir=$(git rev-parse --git-path hooks)
hook_file="$hooks_dir/pre-commit"
mkdir -p "$hooks_dir"

# hookと同じディレクトリで一時ファイルを作り、正常終了・割り込み時に回収する。
temporary_hook=$(mktemp "$hooks_dir/.pre-commit.XXXXXX")
trap 'rm -f "$temporary_hook"' EXIT
trap 'exit 1' HUP INT TERM

# インストール済みhookとの比較にも使うため、生成するhookの内容は毎回同一にする。
cat >"$temporary_hook" <<'HOOK'
#!/bin/sh
# Installed by scripts/setup-git-hooks.sh.
cd "$(git rev-parse --show-toplevel)" || exit 1
exec ./scripts/with-env.sh ./native/gradlew -p native :preCommit
HOOK

# 同一内容なら実行権限だけを戻す。独自hookやリンクは、壊れたリンクも含めて置き換えない。
if [ -e "$hook_file" ] || [ -L "$hook_file" ]; then
  if [ -L "$hook_file" ] || ! cmp -s "$temporary_hook" "$hook_file"; then
    echo "Existing pre-commit hook was not overwritten: $hook_file" >&2
    exit 1
  fi
  chmod +x "$hook_file"
  echo "pre-commit hook is already installed: $hook_file"
else
  # ハードリンク作成は同名ファイルがあれば失敗するため、確認後の競合でも上書きしない。
  chmod 755 "$temporary_hook"
  ln "$temporary_hook" "$hook_file"
  echo "Installed pre-commit hook: $hook_file"
fi
