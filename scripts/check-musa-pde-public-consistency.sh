#!/usr/bin/env bash
set -euo pipefail

PRODUCT_SLUG=${PRODUCT_SLUG:-metodo-musa-7-dias}
BACKEND_PUBLIC_BASE_URL=${BACKEND_PUBLIC_BASE_URL:-http://191.252.181.168}
PDE_PUBLIC_BASE_URL=${PDE_PUBLIC_BASE_URL:-https://v5.clubemusa.com.br}
EXPECTED_EXPERIENCE_VERSION=${EXPECTED_EXPERIENCE_VERSION:-}
EXPECTED_SLOT_CODE=${EXPECTED_SLOT_CODE:-}
EXPECTED_PUBLIC_FIRST_FOLD_HEADLINE=${EXPECTED_PUBLIC_FIRST_FOLD_HEADLINE:-}
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

  local slot_code="${EXPECTED_SLOT_CODE}"
  if [[ -z "${slot_code}" ]]; then
    slot_code="$(python3 - "${PDE_PUBLIC_BASE_URL}" <<'PY'
from urllib.parse import urlparse
import sys
host = urlparse(sys.argv[1]).hostname or ""
slot = host.split(".", 1)[0]
print(slot if slot.startswith("v") and slot[1:].isdigit() else "")
PY
)"
  fi
  local contract_query=""
  if [[ -n "${slot_code}" ]]; then
    contract_query="?slotCode=${slot_code}"
  elif [[ -n "${EXPECTED_EXPERIENCE_VERSION}" ]]; then
    contract_query="?experienceVersion=${EXPECTED_EXPERIENCE_VERSION}"
  fi
  local canonical_url="${BACKEND_PUBLIC_BASE_URL%/}/api/products/public/${PRODUCT_SLUG}/pde-experience${contract_query}"
  local backend_alias_url="${BACKEND_PUBLIC_BASE_URL%/}/api/pde/products/${PRODUCT_SLUG}${contract_query}"
  local pde_alias_url="${PDE_PUBLIC_BASE_URL%/}/api/pde/products/${PRODUCT_SLUG}${contract_query}"
  local pde_health_url="${PDE_PUBLIC_BASE_URL%/}/healthz"
  local pde_slot_diagnostics_url="${PDE_PUBLIC_BASE_URL%/}/slot-diagnostics.json"
  local pde_page_url="${PDE_PUBLIC_BASE_URL%/}/"
  local runtime_config_url="${PDE_PUBLIC_BASE_URL%/}/runtime-config.js"

  fetch_url "${canonical_url}" "${TMP_DIR}/canonical.json"
  fetch_url "${backend_alias_url}" "${TMP_DIR}/backend-alias.json"
  fetch_url "${pde_alias_url}" "${TMP_DIR}/pde-alias.json"
  fetch_url "${pde_health_url}" "${TMP_DIR}/pde-health.txt"
  fetch_url "${pde_slot_diagnostics_url}" "${TMP_DIR}/slot-diagnostics.json"
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

  python3 - "${PRODUCT_SLUG}" "${TMP_DIR}" "${PDE_PUBLIC_BASE_URL}" "${EXPECTED_EXPERIENCE_VERSION}" "${EXPECTED_HERO_VIDEO_PATH}" "${EXPECTED_PUBLIC_FIRST_FOLD_HEADLINE}" <<'PY'
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

def without_nulls(value):
    if isinstance(value, dict):
        return {key: without_nulls(item) for key, item in value.items() if item is not None}
    if isinstance(value, list):
        return [without_nulls(item) for item in value]
    return value

canonical = load_json("canonical.json")
backend_alias = load_json("backend-alias.json")
pde_alias = load_json("pde-alias.json")
slot_diagnostics = load_json("slot-diagnostics.json")

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

pde_comparison_fields = comparison_fields
for key in pde_comparison_fields:
    expected = field(canonical, key)
    actual = field(pde_alias, key)
    if actual != expected:
        raise SystemExit(
            f"pde-alias divergente no campo {key}: esperado={expected} retornado={actual}"
        )

canonical_layout_key = canonical.get("layoutKey")
if canonical_layout_key is not None and str(canonical_layout_key).strip():
    pde_layout_key = field(pde_alias, "layoutKey")
    if pde_layout_key != str(canonical_layout_key):
        raise SystemExit(
            "Layout público PDE divergente do contrato publicado: "
            f"esperado={canonical_layout_key} retornado={pde_layout_key}"
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

expected_public_first_fold_headline = sys.argv[6].strip() if len(sys.argv) > 6 else ""
canonical_first_fold = canonical.get("publicFirstFold")
pde_first_fold = pde_alias.get("publicFirstFold")
if expected_public_first_fold_headline:
    if not isinstance(pde_first_fold, dict) or pde_first_fold.get("headline") != expected_public_first_fold_headline:
        raise SystemExit(
            "Copy pública PDE não contém a headline esperada: "
            f"esperado={expected_public_first_fold_headline!r} "
            f"retornado={(pde_first_fold or {}).get('headline')!r}"
        )
elif isinstance(canonical_first_fold, dict) and canonical_first_fold.get("headline"):
    if pde_first_fold != canonical_first_fold:
        raise SystemExit(
            "Copy pública PDE divergente do contrato publicado: "
            f"headline_esperada={canonical_first_fold.get('headline')!r} "
            f"headline_retornada={(pde_first_fold or {}).get('headline')!r}"
        )

canonical_hero_videos = canonical.get("heroVideos") or []
pde_hero_videos = pde_alias.get("heroVideos") or []
if without_nulls(canonical_hero_videos) != without_nulls(pde_hero_videos):
    raise SystemExit("Vídeos hero públicos do PDE divergem do contrato canônico publicado")

health = (base / "pde-health.txt").read_text(encoding="utf-8", errors="replace")
if "UP" not in health.upper():
    raise SystemExit("Health público do PDE não contém status UP")

if field(slot_diagnostics, "status").upper() != "UP":
    raise SystemExit("Diagnóstico público do slot PDE não contém status UP")

for key in ["slot", "experienceVersion", "image", "imageTag", "commitSha"]:
    field(slot_diagnostics, key)

if expected_experience_version:
    slot_experience_version = field(slot_diagnostics, "experienceVersion")
    if slot_experience_version != expected_experience_version:
        raise SystemExit(
            "Diagnóstico público do slot PDE divergente: "
            f"url={pde_public_base_url}/slot-diagnostics.json "
            f"esperado={expected_experience_version} retornado={slot_experience_version}"
        )

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
