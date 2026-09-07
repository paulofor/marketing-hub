#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY_ROOT="$(cd "${SCRIPT_DIRECTORY}/.." && pwd)"
PUBLISHER="${REPOSITORY_ROOT}/scripts/publish-harness-cards.sh"
WORKFLOW="${REPOSITORY_ROOT}/.github/workflows/publicar-harness-cards.yml"
TEST_ROOT="$(mktemp -d)"
TEST_REPOSITORY="${TEST_ROOT}/repository"
FAKE_BIN="${TEST_ROOT}/bin"
REQUEST_LOG="${TEST_ROOT}/requests.tsv"

cleanup() {
  rm -rf -- "${TEST_ROOT}"
}
trap cleanup EXIT

fail() {
  echo "Falha no contrato de publicação dos cards: $*" >&2
  exit 1
}

assert_request_count() {
  local expected="$1"
  local actual=0

  if [[ -f "${REQUEST_LOG}" ]]; then
    actual="$(wc -l < "${REQUEST_LOG}")"
  fi
  [[ "${actual}" == "${expected}" ]] \
    || fail "esperava ${expected} chamada(s), recebeu ${actual}"
}

commit_fixture() {
  local message="$1"
  git -C "${TEST_REPOSITORY}" add -A
  git -C "${TEST_REPOSITORY}" commit -q -m "${message}"
  git -C "${TEST_REPOSITORY}" rev-parse HEAD
}

write_card() {
  local card="$1"
  local collection="$2"
  local card_key="$3"
  local source_kind="$4"
  local source_uri="$5"
  local source_sha="$6"

  mkdir -p "$(dirname "${card}")"
  jq -n \
    --arg cardKey "${card_key}" \
    --arg collection "${collection}" \
    --arg sourceKind "${source_kind}" \
    --arg sourceUri "${source_uri}" \
    --arg sourceSha256 "${source_sha}" \
    '{
      cardKey: $cardKey,
      collection: $collection,
      title: "Título de homologação",
      finding: "Achado sintético para validar a automação.",
      mechanism: "Mecanismo sintético sem alegação comercial.",
      commercialApplication: "Aplicação apenas no teste local segregado.",
      evidenceStrength: "Evidência sintética.",
      publishedOn: "2026-09-07",
      validUntil: "2027-09-07",
      experimentHypothesis: "A automação deve registrar exatamente uma versão.",
      risks: "Nenhuma inferência de venda.",
      limits: "O cenário não acessa produção.",
      sourceKind: $sourceKind,
      sourceUri: $sourceUri,
      sourceTitle: "Fonte sintética local",
      sourceSha256: $sourceSha256
    }' > "${card}"
}

write_repo_card() {
  local source_collection="$1"
  local card_key="$2"
  local routed_collection="${3:-${source_collection}}"
  local source_path="pesquisas/${source_collection}/cards/fontes/${card_key}.md"
  local card_path="pesquisas/${source_collection}/cards/${card_key}.json"
  local source_sha

  mkdir -p "${TEST_REPOSITORY}/$(dirname "${source_path}")"
  printf 'Fonte sintética de %s.\n' "${card_key}" \
    > "${TEST_REPOSITORY}/${source_path}"
  source_sha="$(sha256sum "${TEST_REPOSITORY}/${source_path}" | awk '{print $1}')"
  write_card \
    "${TEST_REPOSITORY}/${card_path}" \
    "${routed_collection}" \
    "${card_key}" \
    "MARKDOWN" \
    "repo:${source_path}" \
    "${source_sha}"
}

run_publisher_for_root() {
  local fixture_root="$1"
  shift

  env \
    PATH="${FAKE_BIN}:${PATH}" \
    HARNESS_CARDS_REPOSITORY_ROOT="${fixture_root}" \
    HARNESS_LIBRARY_URL="https://harness.invalid" \
    HARNESS_LIBRARY_API_KEY="segredo-sintetico-nao-produtivo-0001" \
    HARNESS_LIBRARY_ACTOR="teste-automatico@marketing-hub" \
    FAKE_REQUEST_LOG="${REQUEST_LOG}" \
    bash "$@"
}

run_publisher() {
  run_publisher_for_root "${TEST_REPOSITORY}" "$@"
}

mkdir -p \
  "${TEST_REPOSITORY}/.github/workflows" \
  "${TEST_REPOSITORY}/scripts" \
  "${FAKE_BIN}"
git -C "${TEST_REPOSITORY}" init -q -b main
git -C "${TEST_REPOSITORY}" config user.name "Codex Homologação"
git -C "${TEST_REPOSITORY}" config user.email "codex-homologacao@marketing-hub"
printf 'fixture\n' > "${TEST_REPOSITORY}/README.md"
printf 'workflow fixture\n' \
  > "${TEST_REPOSITORY}/.github/workflows/publicar-harness-cards.yml"
printf 'publisher fixture\n' \
  > "${TEST_REPOSITORY}/scripts/publish-harness-cards.sh"
base_sha="$(commit_fixture "Base")"

cat > "${FAKE_BIN}/curl" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

output_file=""
data_file=""
idempotency_key=""
url=""

while (( $# > 0 )); do
  case "$1" in
    --output|--write-out|--connect-timeout|--max-time|--retry|--retry-delay|-X)
      if [[ "$1" == "--output" ]]; then
        output_file="$2"
      fi
      shift 2
      ;;
    -H)
      if [[ "$2" == Idempotency-Key:* ]]; then
        idempotency_key="${2#Idempotency-Key: }"
      fi
      shift 2
      ;;
    --data-binary)
      data_file="${2#@}"
      shift 2
      ;;
    --silent|--show-error|--location|--retry-all-errors)
      shift
      ;;
    http://*|https://*)
      url="$1"
      shift
      ;;
    *)
      echo "Argumento inesperado no curl falso: $1" >&2
      exit 90
      ;;
  esac
done

if [[ "${FAKE_CURL_EXIT:-0}" != "0" ]]; then
  exit "${FAKE_CURL_EXIT}"
fi

card_key="$(jq -r '.cardKey' "${data_file}")"
source_sha="$(jq -r '.sourceSha256' "${data_file}")"
status="${FAKE_RESPONSE_STATUS:-DRAFT}"

printf '%s\t%s\t%s\n' "${idempotency_key}" "${card_key}" "${url}" \
  >> "${FAKE_REQUEST_LOG}"
if [[ "${FAKE_INVALID_RESPONSE:-false}" == "true" ]]; then
  printf '{"status":"DRAFT"}\n' > "${output_file}"
else
  jq -n \
    --arg cardKey "${card_key}" \
    --arg sourceSha256 "${source_sha}" \
    --arg status "${status}" \
    '{
      cardId: "RI1-ABCDEF123456",
      cardKey: $cardKey,
      version: 1,
      status: $status,
      effectiveStatus: $status,
      sourceSha256: $sourceSha256,
      routableAgents: ["videomaker"]
    }' > "${output_file}"
fi
printf '%s' "${FAKE_HTTP_CODE:-201}"
EOF
chmod +x "${FAKE_BIN}/curl"

write_repo_card "video" "homologacao-video"
write_repo_card "neuromarketing" "homologacao-neuromarketing"
write_repo_card "ia-aplicada" "homologacao-origem-ia" "neuromarketing"
cards_sha="$(commit_fixture "Adiciona três cards")"

: > "${REQUEST_LOG}"
run_publisher "${PUBLISHER}" --changed "${base_sha}" "${cards_sha}" >/dev/null
assert_request_count 3
first_keys="$(cut -f1 "${REQUEST_LOG}")"

run_publisher "${PUBLISHER}" --changed "${base_sha}" "${cards_sha}" >/dev/null
assert_request_count 6
second_keys="$(tail -n 3 "${REQUEST_LOG}" | cut -f1)"
[[ "${first_keys}" == "${second_keys}" ]] \
  || fail "a repetição do mesmo JSON mudou a chave idempotente"

printf 'publisher fixture revisado\n' \
  > "${TEST_REPOSITORY}/scripts/publish-harness-cards.sh"
automation_sha="$(commit_fixture "Atualiza automação")"
: > "${REQUEST_LOG}"
run_publisher "${PUBLISHER}" --changed "${cards_sha}" "${automation_sha}" >/dev/null
assert_request_count 3

printf '{"ignorado":true}\n' > "${TEST_REPOSITORY}/outro.json"
unrelated_sha="$(commit_fixture "Adiciona JSON fora da pasta de cards")"
: > "${REQUEST_LOG}"
run_publisher "${PUBLISHER}" --changed "${automation_sha}" "${unrelated_sha}" >/dev/null
assert_request_count 0

video_card="${TEST_REPOSITORY}/pesquisas/video/cards/homologacao-video.json"
jq '.title = "Título revisado de homologação"' "${video_card}" > "${video_card}.tmp"
mv "${video_card}.tmp" "${video_card}"
modified_sha="$(commit_fixture "Atualiza uma versão")"
: > "${REQUEST_LOG}"
run_publisher "${PUBLISHER}" --changed "${unrelated_sha}" "${modified_sha}" >/dev/null
assert_request_count 1

invalid_card="${TEST_REPOSITORY}/pesquisas/video/cards/invalido.json"
printf '{}\n' > "${invalid_card}"
invalid_sha="$(commit_fixture "Adiciona card inválido")"
: > "${REQUEST_LOG}"
if run_publisher "${PUBLISHER}" --changed "${modified_sha}" "${invalid_sha}" >/dev/null 2>&1; then
  fail "JSON inválido foi aceito"
fi
assert_request_count 0
git -C "${TEST_REPOSITORY}" rm -q \
  "pesquisas/video/cards/invalido.json"
after_invalid_sha="$(commit_fixture "Remove fixture inválida")"

write_repo_card "video" "data-invalida"
invalid_date_card="${TEST_REPOSITORY}/pesquisas/video/cards/data-invalida.json"
jq '.publishedOn = "2026-02-30"' "${invalid_date_card}" > "${invalid_date_card}.tmp"
mv "${invalid_date_card}.tmp" "${invalid_date_card}"
invalid_date_sha="$(commit_fixture "Adiciona card com data inválida")"
: > "${REQUEST_LOG}"
if run_publisher "${PUBLISHER}" --changed "${after_invalid_sha}" "${invalid_date_sha}" \
  >/dev/null 2>&1; then
  fail "data inexistente foi aceita"
fi
assert_request_count 0
git -C "${TEST_REPOSITORY}" rm -q \
  "pesquisas/video/cards/data-invalida.json" \
  "pesquisas/video/cards/fontes/data-invalida.md"
after_invalid_date_sha="$(commit_fixture "Remove fixture de data")"

oversized_card="${TEST_REPOSITORY}/pesquisas/video/cards/card-grande.json"
write_card \
  "${oversized_card}" \
  "video" \
  "card-grande" \
  "TEXT" \
  "urn:homologacao:card-grande" \
  "$(printf 'c%.0s' {1..64})"
oversized_finding="$(dd if=/dev/zero bs=33000 count=1 status=none | tr '\0' 'x')"
jq --arg finding "${oversized_finding}" '.finding = $finding' \
  "${oversized_card}" > "${oversized_card}.tmp"
mv "${oversized_card}.tmp" "${oversized_card}"
oversized_sha="$(commit_fixture "Adiciona card acima do limite")"
: > "${REQUEST_LOG}"
if run_publisher "${PUBLISHER}" --changed "${after_invalid_date_sha}" "${oversized_sha}" \
  >/dev/null 2>&1; then
  fail "card acima de 32 KiB foi aceito"
fi
assert_request_count 0
git -C "${TEST_REPOSITORY}" rm -q \
  "pesquisas/video/cards/card-grande.json"
after_oversized_sha="$(commit_fixture "Remove fixture acima do limite")"

traversal_card="${TEST_REPOSITORY}/pesquisas/video/cards/travessia.json"
write_card \
  "${traversal_card}" \
  "video" \
  "travessia" \
  "MARKDOWN" \
  "repo:../fora-do-repositorio.md" \
  "$(printf 'd%.0s' {1..64})"
traversal_sha="$(commit_fixture "Adiciona referência com travessia")"
: > "${REQUEST_LOG}"
if run_publisher "${PUBLISHER}" --changed "${after_oversized_sha}" "${traversal_sha}" \
  >/dev/null 2>&1; then
  fail "travessia em fonte repo: foi aceita"
fi
assert_request_count 0
git -C "${TEST_REPOSITORY}" rm -q \
  "pesquisas/video/cards/travessia.json"
after_traversal_sha="$(commit_fixture "Remove fixture de travessia")"

write_repo_card "prazer-audio-visual" "hash-divergente"
printf 'Fonte alterada depois do hash.\n' \
  > "${TEST_REPOSITORY}/pesquisas/prazer-audio-visual/cards/fontes/hash-divergente.md"
bad_hash_sha="$(commit_fixture "Adiciona fonte com hash divergente")"
: > "${REQUEST_LOG}"
if run_publisher "${PUBLISHER}" --changed "${after_traversal_sha}" "${bad_hash_sha}" >/dev/null 2>&1; then
  fail "fonte com hash divergente foi aceita"
fi
assert_request_count 0
git -C "${TEST_REPOSITORY}" rm -q \
  "pesquisas/prazer-audio-visual/cards/hash-divergente.json" \
  "pesquisas/prazer-audio-visual/cards/fontes/hash-divergente.md"
after_hash_sha="$(commit_fixture "Remove fixture de hash")"

unknown_collection_card="${TEST_REPOSITORY}/pesquisas/ia-aplicada/cards/colecao-desconhecida.json"
write_card \
  "${unknown_collection_card}" \
  "ia-aplicada" \
  "colecao-desconhecida" \
  "TEXT" \
  "urn:homologacao:colecao-desconhecida" \
  "$(printf 'a%.0s' {1..64})"
unknown_collection_sha="$(commit_fixture "Adiciona coleção desconhecida")"
: > "${REQUEST_LOG}"
if run_publisher "${PUBLISHER}" --changed \
  "${after_hash_sha}" "${unknown_collection_sha}" >/dev/null 2>&1; then
  fail "coleção de roteamento desconhecida foi aceita"
fi
assert_request_count 0
git -C "${TEST_REPOSITORY}" rm -q \
  "pesquisas/ia-aplicada/cards/colecao-desconhecida.json"
after_unknown_collection_sha="$(commit_fixture "Remove fixture de coleção")"

text_card="${TEST_REPOSITORY}/pesquisas/momentos-de-compra-b2c/cards/homologacao-texto.json"
write_card \
  "${text_card}" \
  "momentos-de-compra-b2c" \
  "homologacao-texto" \
  "TEXT" \
  "urn:homologacao:texto" \
  "$(printf 'b%.0s' {1..64})"
text_sha="$(commit_fixture "Adiciona card com fonte textual")"
: > "${REQUEST_LOG}"
run_publisher "${PUBLISHER}" --changed \
  "${after_unknown_collection_sha}" "${text_sha}" >/dev/null
assert_request_count 1

: > "${REQUEST_LOG}"
if FAKE_HTTP_CODE=500 run_publisher "${PUBLISHER}" \
  --card "pesquisas/momentos-de-compra-b2c/cards/homologacao-texto.json" \
  >/dev/null 2>&1; then
  fail "HTTP 500 foi tratado como sucesso"
fi
assert_request_count 1

: > "${REQUEST_LOG}"
if FAKE_CURL_EXIT=7 run_publisher "${PUBLISHER}" \
  --card "pesquisas/momentos-de-compra-b2c/cards/homologacao-texto.json" \
  >/dev/null 2>&1; then
  fail "erro de transporte foi tratado como sucesso"
fi
assert_request_count 0

: > "${REQUEST_LOG}"
if FAKE_INVALID_RESPONSE=true run_publisher "${PUBLISHER}" \
  --card "pesquisas/momentos-de-compra-b2c/cards/homologacao-texto.json" \
  >/dev/null 2>&1; then
  fail "resposta inválida foi tratada como sucesso"
fi
assert_request_count 1

: > "${REQUEST_LOG}"
FAKE_RESPONSE_STATUS=ACTIVE run_publisher "${PUBLISHER}" \
  --card "pesquisas/momentos-de-compra-b2c/cards/homologacao-texto.json" \
  >/dev/null
assert_request_count 1

: > "${REQUEST_LOG}"
run_publisher "${PUBLISHER}" --changed \
  "0000000000000000000000000000000000000000" "${text_sha}" >/dev/null
assert_request_count 4

printf '{"fora":"da pasta"}\n' > "${TEST_REPOSITORY}/card-fora.json"
commit_fixture "Adiciona JSON manual fora da pasta" >/dev/null
: > "${REQUEST_LOG}"
if run_publisher "${PUBLISHER}" --card "card-fora.json" >/dev/null 2>&1; then
  fail "caminho manual fora da pasta permitida foi aceito"
fi
assert_request_count 0

grep -Fq 'group: harness-library-cards-main' "${WORKFLOW}" \
  || fail "workflow não serializa todas as publicações"
grep -Fq 'queue: max' "${WORKFLOW}" \
  || fail "workflow pode descartar cards quando vários eventos chegam em sequência"
grep -Fq '"pesquisas/*/cards/*.json"' "${WORKFLOW}" \
  || fail "workflow não observa todas as origens de cards"
grep -Fq '"scripts/publish-harness-cards.sh"' "${WORKFLOW}" \
  || fail "workflow não reconcilia cards quando o publicador é corrigido"
grep -Fq 'repository_dispatch:' "${WORKFLOW}" \
  || fail "workflow não aceita sinal imediato de automações externas"
grep -Fq 'harness-library-card-created' "${WORKFLOW}" \
  || fail "evento externo canônico não foi definido"
grep -Fq 'schedule:' "${WORKFLOW}" \
  || fail "workflow não reconcilia eventos perdidos"
grep -Fq 'bash scripts/publish-harness-cards.sh --all' "${WORKFLOW}" \
  || fail "workflow não usa o publicador testável na reconciliação"

: > "${REQUEST_LOG}"
mapfile -d '' -t repository_cards < <(
  git -C "${REPOSITORY_ROOT}" ls-files -z -- \
    ':(glob)pesquisas/*/cards/*.json'
)
run_publisher_for_root "${REPOSITORY_ROOT}" "${PUBLISHER}" --all >/dev/null
assert_request_count "${#repository_cards[@]}"

echo "Contrato automático de publicação dos cards validado."
