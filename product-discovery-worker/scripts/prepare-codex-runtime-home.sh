#!/usr/bin/env bash

set -euo pipefail

if [ "$#" -ne 3 ]; then
  echo "Uso: prepare-codex-runtime-home.sh <diretorio> <uid-runtime> <gid-runtime>" >&2
  exit 2
fi

runtime_home="$1"
runtime_uid="$2"
runtime_gid="$3"

case "$runtime_uid:$runtime_gid" in
  *[!0-9:]* | :* | *:)
    echo "Erro: UID e GID do runtime devem ser numéricos." >&2
    exit 2
    ;;
esac

if [ -z "$runtime_home" ] || [ "$runtime_home" = "/" ]; then
  echo "Erro: o diretório da sessão Codex deve ser específico e não pode ser a raiz." >&2
  exit 2
fi

if [ -L "$runtime_home" ] || { [ -e "$runtime_home" ] && [ ! -d "$runtime_home" ]; }; then
  echo "Erro: o caminho da sessão Codex deve ser um diretório real." >&2
  exit 1
fi

install -d -o "$runtime_uid" -g "$runtime_gid" -m 700 "$runtime_home"

# O Codex gerencia esta árvore enquanto o worker anterior ainda pode estar ativo.
# Ela é validada como diretório real, mas permanece opaca para não disputar arquivos
# transitórios nem confundir os wrappers simbólicos legítimos com estado persistente.
transient_directory="$runtime_home/tmp"
if [ -L "$transient_directory" ] || \
  { [ -e "$transient_directory" ] && [ ! -d "$transient_directory" ]; }; then
  echo "Erro: o diretório temporário da sessão Codex deve ser um diretório real." >&2
  exit 1
fi
install -d -o "$runtime_uid" -g "$runtime_gid" -m 700 "$transient_directory"

if find -P "$runtime_home" \
  -path "$transient_directory" -prune -o \
  -mindepth 1 -type l -print -quit | grep -q .; then
  echo "Erro: a sessão Codex contém link simbólico e não pode ter permissões reconciliadas com segurança." >&2
  exit 1
fi

find -P "$runtime_home" \
  -path "$transient_directory" -prune -o \
  -exec chown --no-dereference "$runtime_uid:$runtime_gid" {} +
find -P "$runtime_home" \
  -path "$transient_directory" -prune -o \
  -type d -exec chmod 700 {} +
find -P "$runtime_home" \
  -path "$transient_directory" -prune -o \
  -type f -exec chmod 600 {} +

actual_uid="$(stat -c '%u' "$runtime_home")"
actual_gid="$(stat -c '%g' "$runtime_home")"
actual_mode="$(stat -c '%a' "$runtime_home")"

if [ "$actual_uid" != "$runtime_uid" ] || [ "$actual_gid" != "$runtime_gid" ] || \
  [ "$actual_mode" != "700" ]; then
  echo "Erro: a sessão Codex não atende ao contrato do usuário não privilegiado." >&2
  exit 1
fi

echo "[product-discovery-worker] sessão Codex preparada para o usuário não privilegiado do runtime"
