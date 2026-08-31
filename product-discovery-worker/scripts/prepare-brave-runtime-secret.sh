#!/usr/bin/env bash

set -euo pipefail

if [ "$#" -ne 4 ]; then
  echo "Uso: prepare-brave-runtime-secret.sh <origem> <destino> <uid-runtime> <gid-runtime>" >&2
  exit 2
fi

source_path="$1"
target_path="$2"
runtime_uid="$3"
runtime_gid="$4"

if [ ! -s "$source_path" ]; then
  echo "Erro: a credencial Brave de origem está ausente ou vazia." >&2
  exit 1
fi

case "$runtime_uid:$runtime_gid" in
  *[!0-9:]* | :* | *:)
    echo "Erro: UID e GID do runtime devem ser numéricos." >&2
    exit 2
    ;;
esac

target_directory="$(dirname -- "$target_path")"
install -d -m 700 "$target_directory"
install -o "$runtime_uid" -g "$runtime_gid" -m 400 "$source_path" "$target_path"

actual_uid="$(stat -c '%u' "$target_path")"
actual_gid="$(stat -c '%g' "$target_path")"
actual_mode="$(stat -c '%a' "$target_path")"

if [ ! -s "$target_path" ] || [ "$actual_uid" != "$runtime_uid" ] || \
  [ "$actual_gid" != "$runtime_gid" ] || [ "$actual_mode" != "400" ]; then
  echo "Erro: a cópia protegida da credencial Brave não atende ao contrato do runtime." >&2
  exit 1
fi

echo "[product-discovery-worker] credencial Brave preparada para o usuário não privilegiado do runtime"
