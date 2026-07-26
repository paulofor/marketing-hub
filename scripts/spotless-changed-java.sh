#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat >&2 <<'EOF'
Uso: scripts/spotless-changed-java.sh [--apply] [--base <ref>]

Checa com Spotless apenas arquivos Java alterados no backend/ads-service.

Opções:
  --apply       Formata os arquivos alterados em vez de apenas validar.
  --base <ref> Referência Git usada para detectar alterações. Padrão: HEAD.
  -h, --help   Mostra esta ajuda.
EOF
}

MODE="check"
BASE_REF="HEAD"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --apply)
      MODE="apply"
      shift
      ;;
    --base)
      if [[ $# -lt 2 ]]; then
        echo "Erro: --base exige uma referência Git." >&2
        usage
        exit 1
      fi
      BASE_REF="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Erro: opção desconhecida: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if ! command -v git >/dev/null 2>&1; then
  echo "Erro: git não encontrado." >&2
  exit 1
fi

if ! command -v mvn >/dev/null 2>&1; then
  echo "Erro: mvn não encontrado." >&2
  exit 1
fi

REPO_ROOT="$(git rev-parse --show-toplevel)"
MODULE_DIR="backend/ads-service"

if [[ ! -f "${REPO_ROOT}/${MODULE_DIR}/pom.xml" ]]; then
  echo "Erro: pom.xml não encontrado em ${MODULE_DIR}." >&2
  exit 1
fi

if ! git -C "$REPO_ROOT" rev-parse --verify "${BASE_REF}^{commit}" >/dev/null 2>&1; then
  echo "Erro: referência Git inválida para --base: ${BASE_REF}" >&2
  exit 1
fi

mapfile -t changed_files < <(
  {
    git -C "$REPO_ROOT" diff --name-only --diff-filter=ACMR "$BASE_REF" -- "$MODULE_DIR"
    git -C "$REPO_ROOT" ls-files --others --exclude-standard -- "$MODULE_DIR"
  } | awk '!seen[$0]++'
)

java_files=()
for file in "${changed_files[@]}"; do
  if [[ "$file" == "${MODULE_DIR}/"*".java" && -f "${REPO_ROOT}/${file}" ]]; then
    java_files+=("${file#${MODULE_DIR}/}")
  fi
done

if [[ ${#java_files[@]} -eq 0 ]]; then
  echo "Nenhum arquivo Java alterado em ${MODULE_DIR}."
  exit 0
fi

spotless_files="$(IFS=,; echo "${java_files[*]}")"

echo "Executando Spotless ${MODE} em ${#java_files[@]} arquivo(s) Java alterado(s):"
printf ' - %s\n' "${java_files[@]}"

cd "${REPO_ROOT}/${MODULE_DIR}"
mvn -q "spotless:${MODE}" "-DspotlessFiles=${spotless_files}"
