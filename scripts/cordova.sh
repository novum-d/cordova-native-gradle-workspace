#!/bin/sh
set -eu

# プロジェクト環境用のCordova CLI
workspace_dir=$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd -P)
cordova_cli="$workspace_dir/node_modules/cordova/bin/cordova"
if [ ! -f "$cordova_cli" ]; then
  echo "Cordova CLI is missing: $cordova_cli" >&2
  echo "Run npm ci --include=dev from the workspace root: $workspace_dir" >&2
  exit 1
fi
cd "$workspace_dir/CordovaComposeApp"

# Use the installed CLI directly, also when npm bin-links=false.
exec "$workspace_dir/scripts/with-env.sh" node "$cordova_cli" "$@"
