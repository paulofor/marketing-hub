#!/usr/bin/env bash
set -euo pipefail

PRODUCT_SLUG=${PRODUCT_SLUG:-metodo-musa-7-dias}
BACKEND_PUBLIC_BASE_URL=${BACKEND_PUBLIC_BASE_URL:-http://191.252.181.168}
PDE_PUBLIC_BASE_URL=${PDE_PUBLIC_BASE_URL:-https://v5.clubemusa.com.br}
TIMEOUT_SECONDS=${TIMEOUT_SECONDS:-30}
TMP_DIR=""

log() {
  printf '[%s] [musa-pde-health] %s\n' "$(date -Is)" "$*"
}

fail() {
  printf '[%s] [musa-pde-health] ERRO: %s\n' "$(date -Is)" "$*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "comando obrigatório não encontrado: $1"
}

fetch_url() {
  local url="$1"
  local output_file="$2"

  log "Validando ${url}"
  curl --fail --silent --show-error --location --max-time "${TIMEOUT_SECONDS}" \
    --header 'Accept: application/json,text/html,*/*' \
    "${url}" >"${output_file}"
}

main() {
  require_command curl
  require_command python3

  TMP_DIR="$(mktemp -d)"
  trap 'rm -rf "${TMP_DIR}"' EXIT

  local canonical_url="${BACKEND_PUBLIC_BASE_URL%/}/api/products/public/${PRODUCT_SLUG}/pde-experience"
  local backend_alias_url="${BACKEND_PUBLIC_BASE_URL%/}/api/pde/products/${PRODUCT_SLUG}"
  local pde_alias_url="${PDE_PUBLIC_BASE_URL%/}/api/pde/products/${PRODUCT_SLUG}"
  local pde_health_url="${PDE_PUBLIC_BASE_URL%/}/healthz"
  local pde_page_url="${PDE_PUBLIC_BASE_URL%/}/"

  fetch_url "${canonical_url}" "${TMP_DIR}/canonical.json"
  fetch_url "${backend_alias_url}" "${TMP_DIR}/backend-alias.json"
  fetch_url "${pde_alias_url}" "${TMP_DIR}/pde-alias.json"
  fetch_url "${pde_health_url}" "${TMP_DIR}/pde-health.txt"
  fetch_url "${pde_page_url}" "${TMP_DIR}/pde-page.html"

  python3 - "${PRODUCT_SLUG}" "${TMP_DIR}" <<'PY'
import json
import pathlib
import sys

product_slug = sys.argv[1]
base = pathlib.Path(sys.argv[2])

def load_json(name):
    path = base / name
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise SystemExit(f"{name} não retornou JSON válido: {exc}") from exc

def field(payload, key):
    value = payload.get(key)
    if value is None or str(value).strip() == "":
        raise SystemExit(f"Campo obrigatório ausente ou vazio: {key}")
    return str(value)

canonical = load_json("canonical.json")
backend_alias = load_json("backend-alias.json")
pde_alias = load_json("pde-alias.json")

canonical_slug = field(canonical, "slug")
if canonical_slug != product_slug:
    raise SystemExit(f"Slug canônico divergente: esperado={product_slug} retornado={canonical_slug}")

comparison_fields = ["slug", "experienceVersion", "funnelVersion"]
for name, payload in [("backend-alias", backend_alias), ("pde-alias", pde_alias)]:
    for key in comparison_fields:
        expected = field(canonical, key)
        actual = field(payload, key)
        if actual != expected:
            raise SystemExit(
                f"{name} divergente no campo {key}: esperado={expected} retornado={actual}"
            )

health = (base / "pde-health.txt").read_text(encoding="utf-8", errors="replace")
if "UP" not in health.upper():
    raise SystemExit("Health público do PDE não contém status UP")

page = (base / "pde-page.html").read_text(encoding="utf-8", errors="replace")
required_page_markers = ["root", "assets"]
for marker in required_page_markers:
    if marker not in page:
        raise SystemExit(f"Página pública do PDE não contém marcador obrigatório: {marker}")

print(
    "OK: contratos PDE consistentes "
    f"slug={canonical['slug']} "
    f"experienceVersion={canonical['experienceVersion']} "
    f"funnelVersion={canonical['funnelVersion']}"
)
PY
}

main "$@"
