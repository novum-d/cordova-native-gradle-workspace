#!/bin/sh
# ローカルプラグインをリンク導入し、元のKTソースの編集を通常のCordovaビルドへ反映する。
set -eu

workspace_dir=$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd -P)
cd "$workspace_dir"
plugin_source="$workspace_dir/kotlin-plugin/src/android/dev/example/kotlinplugin/KotlinPlugin.kt"
installed_source="$workspace_dir/CordovaComposeApp/platforms/android/app/src/main/java/dev/example/kotlinplugin/KotlinPlugin.kt"

# JS・plugin.xmlの変更時だけ、利用者の明示指定で再導入する。
case "${1:-}" in
  '') reinstall=false ;;
  --reinstall) reinstall=true ;;
  *) echo 'Usage: setup-kotlin-plugin.sh [--reinstall]' >&2; exit 1 ;;
esac

# bootstrapまたはplatform作成後に呼ぶ。正常なリンクは触らず、毎回の再インストールを避ける。
if [ ! -f "$plugin_source" ] || [ ! -f kotlin-plugin/plugin.xml ]; then
  echo 'Local kotlin-plugin sources are missing. Restore kotlin-plugin/ and retry.' >&2
  exit 1
fi
if [ ! -f CordovaComposeApp/platforms/android/cordova/Api.js ]; then
  echo 'Run ./scripts/bootstrap.sh to create the Android platform first.' >&2
  exit 1
fi
if [ "$reinstall" = false ] && [ -L "$installed_source" ] && cmp -s "$plugin_source" "$installed_source"; then
  echo 'kotlin-plugin source is already linked.'
  exit 0
fi

# prepareによる自動復元はファイルをコピーするため、その場合だけリンク導入へ切り替える。
# --nosaveでpackage.jsonとlockfileの依存宣言を維持し、対象プラグインだけを扱う。
if [ -e CordovaComposeApp/plugins/kotlin-plugin ]; then
  ./scripts/cordova.sh plugin remove kotlin-plugin --nosave
fi
./scripts/cordova.sh plugin add ../kotlin-plugin --link --nosave
