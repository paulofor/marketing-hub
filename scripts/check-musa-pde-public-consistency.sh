#!/usr/bin/env bash
set -euo pipefail

PRODUCT_SLUG=${PRODUCT_SLUG:-metodo-musa-7-dias}
BACKEND_PUBLIC_BASE_URL=${BACKEND_PUBLIC_BASE_URL:-http://191.252.181.168}
PDE_PUBLIC_BASE_URL=${PDE_PUBLIC_BASE_URL:-https://v5.clubemusa.com.br}
EXPECTED_EXPERIENCE_VERSION=${EXPECTED_EXPERIENCE_VERSION:-}
EXPECTED_HERO_VIDEO_PATH=${EXPECTED_HERO_VIDEO_PATH:-}
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
  local runtime_config_url="${PDE_PUBLIC_BASE_URL%/}/runtime-config.js"

  fetch_url "${canonical_url}" "${TMP_DIR}/canonical.json"
  fetch_url "${backend_alias_url}" "${TMP_DIR}/backend-alias.json"
  fetch_url "${pde_alias_url}" "${TMP_DIR}/pde-alias.json"
  fetch_url "${pde_health_url}" "${TMP_DIR}/pde-health.txt"
  fetch_url "${pde_page_url}" "${TMP_DIR}/pde-page.html"
  fetch_url "${runtime_config_url}" "${TMP_DIR}/runtime-config.js"
  if [[ -n "${EXPECTED_HERO_VIDEO_PATH}" ]]; then
    local video_url="${PDE_PUBLIC_BASE_URL%/}${EXPECTED_HERO_VIDEO_PATH}"
    fetch_url "${video_url}" "${TMP_DIR}/hero-video.mp4"
    curl --fail --silent --show-error --location --max-time "${TIMEOUT_SECONDS}" \
      --output /dev/null \
      --write-out '%{content_type}' \
      "${video_url}" >"${TMP_DIR}/hero-video-content-type.txt"
  fi

  python3 - "${PRODUCT_SLUG}" "${TMP_DIR}" "${PDE_PUBLIC_BASE_URL}" "${EXPECTED_EXPERIENCE_VERSION}" "${EXPECTED_HERO_VIDEO_PATH}" <<'PY'
import json
import pathlib
import sys

product_slug = sys.argv[1]
base = pathlib.Path(sys.argv[2])
pde_public_base_url = sys.argv[3]
expected_experience_version = sys.argv[4].strip()
expected_hero_video_path = sys.argv[5].strip()

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
for key in comparison_fields:
    expected = field(canonical, key)
    actual = field(backend_alias, key)
    if actual != expected:
        raise SystemExit(
            f"backend-alias divergente no campo {key}: esperado={expected} retornado={actual}"
        )

pde_comparison_fields = ["slug", "funnelVersion"] if expected_experience_version else comparison_fields
for key in pde_comparison_fields:
    expected = field(canonical, key)
    actual = field(pde_alias, key)
    if actual != expected:
        raise SystemExit(
            f"pde-alias divergente no campo {key}: esperado={expected} retornado={actual}"
        )

if expected_experience_version:
    pde_alias_version = field(pde_alias, "experienceVersion")
    if pde_alias_version != expected_experience_version:
        raise SystemExit(
            "Versão publica PDE divergente: "
            f"url={pde_public_base_url} esperado={expected_experience_version} retornado={pde_alias_version}"
        )
    runtime_config = (base / "runtime-config.js").read_text(encoding="utf-8", errors="replace")
    if expected_experience_version not in runtime_config:
        raise SystemExit(
            "Runtime config publico PDE divergente: "
            f"url={pde_public_base_url}/runtime-config.js esperado={expected_experience_version}"
        )

health = (base / "pde-health.txt").read_text(encoding="utf-8", errors="replace")
if "UP" not in health.upper():
    raise SystemExit("Health público do PDE não contém status UP")

page = (base / "pde-page.html").read_text(encoding="utf-8", errors="replace")
required_page_markers = ["root", "assets"]
for marker in required_page_markers:
    if marker not in page:
        raise SystemExit(f"Página pública do PDE não contém marcador obrigatório: {marker}")

if expected_hero_video_path:
    runtime_config = (base / "runtime-config.js").read_text(encoding="utf-8", errors="replace")
    content_type = (base / "hero-video-content-type.txt").read_text(encoding="utf-8", errors="replace")
    video_file = base / "hero-video.mp4"
    if "text/html" in content_type.lower():
        raise SystemExit(
            f"Vídeo esperado retornou HTML em vez de MP4: url={pde_public_base_url}{expected_hero_video_path}"
        )
    if "video/mp4" not in content_type.lower() and "application/octet-stream" not in content_type.lower():
        raise SystemExit(
            "Content-Type inesperado para vídeo PDE: "
            f"url={pde_public_base_url}{expected_hero_video_path} contentType={content_type}"
        )
    if video_file.stat().st_size < 100000:
        raise SystemExit(
            "Arquivo de vídeo PDE parece vazio ou placeholder: "
            f"url={pde_public_base_url}{expected_hero_video_path} bytes={video_file.stat().st_size}"
        )
    if "VITE_MUSA_HERO_VIDEO_URL" not in runtime_config:
        raise SystemExit("runtime-config publico não declara configuração de vídeo do MUSA")

print(
    "OK: contratos PDE consistentes "
    f"slug={canonical['slug']} "
    f"experienceVersion={canonical['experienceVersion']} "
    f"funnelVersion={canonical['funnelVersion']}"
)
PY
}

main "$@"
