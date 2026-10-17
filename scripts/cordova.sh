#!/bin/sh
set -eu
workspace_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$workspace_dir/CordovaComposeApp"
exec "$workspace_dir/scripts/with-env.sh" "$workspace_dir/node_modules/.bin/cordova" "$@"
