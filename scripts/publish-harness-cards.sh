#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY_ROOT="${HARNESS_CARDS_REPOSITORY_ROOT:-$(cd "${SCRIPT_DIRECTORY}/.." && pwd)}"
CARD_PATHSPEC=':(glob)pesquisas/*/cards/*.json'
AUTOMATION_PATHS=(
  ".github/workflows/publicar-harness-cards.yml"
  "scripts/publish-harness-cards.sh"
)
MAX_CARD_BYTES=32768

fail() {
  echo "::error::$*" >&2
  exit 1
}

usage() {
  cat >&2 <<'EOF'
Uso:
  publish-harness-cards.sh --all
  publish-harness-cards.sh --card pesquisas/<origem>/cards/<card>.json
  publish-harness-cards.sh --changed <before-sha> <after-sha>
EOF
  exit 2
}

select_all_cards() {
  git ls-files -z -- "${CARD_PATHSPEC}"
}

select_changed_cards() {
  local before_sha="$1"
  local after_sha="$2"

  git cat-file -e "${after_sha}^{commit}" 2>/dev/null \
    || fail "Commit final não encontrado para selecionar cards: ${after_sha}"

  if [[ "${before_sha}" =~ ^0+$ ]] \
    || ! git cat-file -e "${before_sha}^{commit}" 2>/dev/null; then
    echo "::notice::Commit anterior indisponível; reconciliando todos os cards versionados." >&2
    select_all_cards
    return
  fi

  if ! git diff --quiet "${before_sha}" "${after_sha}" -- "${AUTOMATION_PATHS[@]}"; then
    echo "::notice::Automação alterada; reconciliando todos os cards versionados." >&2
    select_all_cards
    return
  fi

  git diff \
    --diff-filter=ACMR \
    --name-only \
    -z \
    "${before_sha}" \
    "${after_sha}" \
    -- "${CARD_PATHSPEC}"
}

validate_card_path() {
  local card="$1"

  if [[ ! "${card}" =~ ^pesquisas/[a-z0-9]+(-[a-z0-9]+)*/cards/[A-Za-z0-9][A-Za-z0-9._-]*\.json$ ]]; then
    fail "Card fora da pasta de origem segura pesquisas/<origem>/cards: ${card}"
  fi
}

validate_iso_date() {
  local value="$1"
  local field="$2"
  local card="$3"
  local normalized

  if ! normalized="$(date -u -d "${value}" '+%F' 2>/dev/null)" \
    || [[ "${normalized}" != "${value}" ]]; then
    fail "Data inválida em ${field} no card ${card}: ${value}"
  fi
}

validate_card_payload() {
  local card="$1"

  jq -e '
    def required_string($maximum):
      type == "string" and test("\\S") and length <= $maximum;
    def exact_keys:
      (keys | sort) == ([
        "cardKey",
        "collection",
        "title",
        "finding",
        "mechanism",
        "commercialApplication",
        "evidenceStrength",
        "publishedOn",
        "validUntil",
        "experimentHypothesis",
        "risks",
        "limits",
        "sourceKind",
        "sourceUri",
        "sourceTitle",
        "sourceSha256"
      ] | sort);

    type == "object" and
    exact_keys and
    (.cardKey | required_string(120) and test("^[a-z0-9][a-z0-9-]{2,119}$")) and
    (.collection | required_string(80) and IN(
      "video",
      "prazer-audio-visual",
      "neuromarketing",
      "momentos-de-compra-b2c"
    )) and
    (.title | required_string(240)) and
    (.finding | required_string(700)) and
    (.mechanism | required_string(700)) and
    (.commercialApplication | required_string(700)) and
    (.evidenceStrength | required_string(500)) and
    (.publishedOn | type == "string" and test("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")) and
    (.validUntil | type == "string" and test("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")) and
    (.validUntil >= .publishedOn) and
    (.experimentHypothesis | required_string(700)) and
    (.risks | required_string(700)) and
    (.limits | required_string(700)) and
    (.sourceKind | IN("URL", "PDF", "MARKDOWN", "TEXT")) and
    (.sourceUri | required_string(1024) and test("^(https://|urn:|repo:|s3://)[^[:space:]]+$")) and
    (.sourceTitle | required_string(240)) and
    (.sourceSha256 | type == "string" and test("^[0-9a-f]{64}$"))
  ' "${card}" >/dev/null \
    || fail "JSON fora do contrato da Biblioteca do Harness: ${card}"
}

validate_source() {
  local card="$1"
  local source_kind source_uri source_path expected_sha actual_sha

  source_kind="$(jq -r '.sourceKind' "${card}")"
  source_uri="$(jq -r '.sourceUri' "${card}")"

  case "${source_kind}" in
    URL)
      [[ "${source_uri}" == https://* ]] \
        || fail "Fonte URL deve usar HTTPS: ${card}"
      ;;
    PDF)
      [[ "${source_uri}" == https://* || "${source_uri}" == s3://* ]] \
        || fail "Fonte PDF deve usar HTTPS ou S3: ${card}"
      ;;
    MARKDOWN)
      [[ "${source_uri}" == https://* || "${source_uri}" == repo:* ]] \
        || fail "Fonte Markdown deve usar HTTPS ou repo: ${card}"
      ;;
    TEXT)
      [[ "${source_uri}" == urn:* ]] \
        || fail "Fonte textual deve usar URN: ${card}"
      ;;
    *)
      fail "Tipo de fonte não suportado: ${source_kind}"
      ;;
  esac

  [[ "${source_uri}" == repo:* ]] || return 0

  source_path="${source_uri#repo:}"
  if [[ -z "${source_path}" \
    || "${source_path}" == /* \
    || "/${source_path}/" == *"/../"* \
    || "/${source_path}/" == *"/./"* ]]; then
    fail "Caminho repo: inválido no card ${card}: ${source_path}"
  fi

  git ls-files --error-unmatch -- "${source_path}" >/dev/null 2>&1 \
    || fail "Fonte repo: não está versionada: ${source_path}"
  [[ -f "${source_path}" ]] || fail "Fonte repo: não encontrada: ${source_path}"

  expected_sha="$(jq -r '.sourceSha256' "${card}")"
  actual_sha="$(sha256sum "${source_path}" | awk '{print $1}')"
  [[ "${actual_sha}" == "${expected_sha}" ]] \
    || fail "SHA-256 da fonte não corresponde ao card: ${card}"
}

publish_card() {
  local card="$1"
  local response="$2"
  local card_sha idempotency_key http_code expected_card_key expected_source_sha
  local published_on valid_until

  echo "Validando ${card}"
  validate_card_path "${card}"

  if (( $(wc -c < "${card}") > MAX_CARD_BYTES )); then
    fail "Card excede ${MAX_CARD_BYTES} bytes: ${card}"
  fi

  validate_card_payload "${card}"

  published_on="$(jq -r '.publishedOn' "${card}")"
  valid_until="$(jq -r '.validUntil' "${card}")"
  validate_iso_date "${published_on}" "publishedOn" "${card}"
  validate_iso_date "${valid_until}" "validUntil" "${card}"

  validate_source "${card}"

  card_sha="$(sha256sum "${card}" | awk '{print $1}')"
  idempotency_key="gha-${card_sha:0:32}"

  if ! http_code="$(
    curl \
      --silent \
      --show-error \
      --connect-timeout 15 \
      --max-time 60 \
      --retry 2 \
      --retry-all-errors \
      --retry-delay 2 \
      --output "${response}" \
      --write-out '%{http_code}' \
      -X POST "${HARNESS_LIBRARY_URL}/v1/cards" \
      -H "Content-Type: application/json" \
      -H "X-API-Key: ${HARNESS_LIBRARY_API_KEY}" \
      -H "X-Actor: ${HARNESS_LIBRARY_ACTOR}" \
      -H "Idempotency-Key: ${idempotency_key}" \
      --data-binary "@${card}"
  )"; then
    fail "Falha de transporte ao publicar card: ${card}"
  fi

  if [[ "${http_code}" != "201" ]]; then
    echo "::error::Mkt Hub respondeu HTTP ${http_code} para ${card}" >&2
    jq . "${response}" 2>/dev/null || sed -n '1,80p' "${response}" >&2
    exit 1
  fi

  expected_card_key="$(jq -r '.cardKey' "${card}")"
  expected_source_sha="$(jq -r '.sourceSha256' "${card}")"
  jq -e \
    --arg card_key "${expected_card_key}" \
    --arg source_sha "${expected_source_sha}" '
      (.cardKey == $card_key) and
      (.sourceSha256 == $source_sha) and
      (.version | type == "number" and . >= 1) and
      (.status | IN("DRAFT", "IN_REVIEW", "ACTIVE", "ARCHIVED")) and
      (.cardId | type == "string" and startswith("RI1-"))
    ' "${response}" >/dev/null \
    || fail "Resposta inválida do Mkt Hub para o card: ${card}"

  echo "Card sincronizado:"
  jq '{
    cardId,
    cardKey,
    version,
    status,
    effectiveStatus,
    routableAgents
  }' "${response}"
}

if [[ "$(git -C "${REPOSITORY_ROOT}" rev-parse --is-inside-work-tree 2>/dev/null || true)" != "true" ]]; then
  fail "Raiz Git inválida para publicar cards: ${REPOSITORY_ROOT}"
fi

cd "${REPOSITORY_ROOT}"

declare -a cards=()
case "${1:-}" in
  --all)
    (( $# == 1 )) || usage
    mapfile -d '' -t cards < <(select_all_cards)
    ;;
  --card)
    (( $# == 2 )) || usage
    card="${2#./}"
    validate_card_path "${card}"
    git ls-files --error-unmatch -- "${card}" >/dev/null 2>&1 \
      || fail "Card manual não está versionado: ${card}"
    cards=("${card}")
    ;;
  --changed)
    (( $# == 3 )) || usage
    mapfile -d '' -t cards < <(select_changed_cards "$2" "$3")
    ;;
  *)
    usage
    ;;
esac

if (( ${#cards[@]} == 0 )); then
  echo "::notice::Nenhum card novo ou alterado para sincronizar."
  if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
    echo "Nenhum card novo ou alterado para sincronizar." >> "${GITHUB_STEP_SUMMARY}"
  fi
  exit 0
fi

mapfile -d '' -t cards < <(printf '%s\0' "${cards[@]}" | sort -zu)

[[ -n "${HARNESS_LIBRARY_API_KEY:-}" ]] \
  || fail "HARNESS_LIBRARY_API_KEY não configurado"
HARNESS_LIBRARY_URL="${HARNESS_LIBRARY_URL:-https://mkthub.api.br}"
HARNESS_LIBRARY_ACTOR="${HARNESS_LIBRARY_ACTOR:-github-actions@marketing-hub}"

temporary_directory="$(mktemp -d)"
trap 'rm -rf -- "${temporary_directory}"' EXIT

published_count=0
for card in "${cards[@]}"; do
  publish_card "${card}" "${temporary_directory}/response-${published_count}.json"
  (( published_count += 1 ))
done

echo "::notice::${published_count} card(s) sincronizado(s) com a Biblioteca do Harness."
if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  echo "${published_count} card(s) sincronizado(s) com a Biblioteca do Harness." \
    >> "${GITHUB_STEP_SUMMARY}"
fi
