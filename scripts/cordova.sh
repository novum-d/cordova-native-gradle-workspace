#!/bin/sh
# アプリディレクトリへ移動し、共有の実行環境でローカルのCordova CLIを呼び出す。
# run・buildはCLI側でprepareも行うため、ここでは引数をそのまま渡す。
set -eu

workspace_dir=$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd -P)
cordova_cli="$workspace_dir/node_modules/cordova/bin/cordova"
if [ ! -f "$cordova_cli" ]; then
  echo "Cordova CLI is missing: $cordova_cli" >&2
  echo "Run npm ci --include=dev from the workspace root: $workspace_dir" >&2
  exit 1
fi
cd "$workspace_dir/CordovaComposeApp"

# .binリンクを作らないnpm設定でも動くよう、CLI本体をNodeへ直接渡す。
exec "$workspace_dir/scripts/with-env.sh" node "$cordova_cli" "$@"
