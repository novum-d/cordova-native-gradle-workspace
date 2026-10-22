#!/bin/sh
# Version Catalogの[versions]から指定aliasの文字列を取り出し、他のシェルへ渡す。
set -eu

workspace_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
# aliasを小文字英数字・ハイフンに限定し、awkの検索パターンへ任意の式を混ぜない。
if [ "$#" -ne 1 ]; then
  echo 'A catalog version alias is required' >&2
  exit 1
fi
case "$1" in
'' | [!a-z]* | *[!a-z0-9-]*)
  echo 'A catalog version alias is required' >&2
  exit 1
  ;;
esac

# [versions]内の name = "value" だけを読む簡易パーサー。TOML全般の構文解析は行わない。
awk -v requested="$1" '
  /^[[:space:]]*\[/ {
    in_versions = ($0 ~ /^[[:space:]]*\[versions\][[:space:]]*(#.*)?$/)
    next
  }
  in_versions && $0 ~ ("^[[:space:]]*" requested "[[:space:]]*=") {
    value = $0
    sub(/^[^=]*=[[:space:]]*/, "", value)
    if (value !~ /^"[^"]+"/) exit 1
    sub(/^"/, "", value)
    sub(/".*$/, "", value)
    printf "%s", value
    found = 1
    exit
  }
  # aliasがなければ空文字の成功扱いにせず、呼び出し元も失敗させる。
  END {
    if (!found) {
      print "Missing catalog version: " requested > "/dev/stderr"
      exit 1
    }
  }
' "$workspace_dir/native/gradle/libs.versions.toml"
