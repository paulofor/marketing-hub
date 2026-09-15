#!/usr/bin/env bash
set -euo pipefail

# Baixa um artefato produzido por um CI de pull request somente quando a árvore
# Git aprovada é idêntica à árvore atualmente integrada em main.
#
# Uso:
#   download-approved-pr-artifact.sh <workflow.yml> <artifact-name> <destination>
#
# Retornos esperados:
#   0 = artefato aprovado e compatível extraído
#   3 = não há candidato/artefato aprovado reutilizável
#   4 = artefato existe, mas a árvore aprovada diverge da árvore atual

if [[ "$#" -ne 3 ]]; then
  echo "uso: $0 <workflow.yml> <artifact-name> <destination>" >&2
  exit 2
fi

workflow="$1"
artifact_name="$2"
destination="$3"

if [[ -z "${GITHUB_REPOSITORY:-}" ]]; then
  echo "GITHUB_REPOSITORY não definido." >&2
  exit 2
fi

if ! command -v gh >/dev/null 2>&1 || ! command -v jq >/dev/null 2>&1; then
  echo "gh e jq são obrigatórios para localizar artefatos aprovados." >&2
  exit 2
fi

current_tree="$(git rev-parse 'HEAD^{tree}')"
read -r -a commit_line <<<"$(git rev-list --parents -n 1 HEAD)"

# Para push normal após merge, o segundo pai é a cabeça do PR que acabou de ser
# integrada. Outros formatos de commit caem no caminho conservador de rebuild.
if [[ "${#commit_line[@]}" -ne 3 ]]; then
  echo "Commit atual não é merge de dois pais; rebuild será usado."
  exit 3
fi

candidate_sha="${commit_line[2]}"
runs_json="$(gh api \
  -H 'Accept: application/vnd.github+json' \
  "repos/${GITHUB_REPOSITORY}/actions/workflows/${workflow}/runs?head_sha=${candidate_sha}&event=pull_request&status=success&per_page=20")"

run_id="$(printf '%s' "${runs_json}" | jq -r --arg sha "${candidate_sha}" '
  [.workflow_runs[] | select(.head_sha == $sha)]
  | sort_by(.run_number)
  | reverse
  | .[0].id // empty
')"

if [[ -z "${run_id}" ]]; then
  echo "Nenhum run aprovado de ${workflow} encontrado para ${candidate_sha}."
  exit 3
fi

artifacts_json="$(gh api \
  -H 'Accept: application/vnd.github+json' \
  "repos/${GITHUB_REPOSITORY}/actions/runs/${run_id}/artifacts?per_page=100")"

download_url="$(printf '%s' "${artifacts_json}" | jq -r --arg name "${artifact_name}" '
  [.artifacts[] | select(.name == $name and (.expired == false))]
  | .[0].archive_download_url // empty
')"

if [[ -z "${download_url}" ]]; then
  echo "Run ${run_id} não possui o artefato ${artifact_name} disponível."
  exit 3
fi

archive="$(mktemp)"
trap 'rm -f "${archive}"' EXIT
rm -rf "${destination}"
mkdir -p "${destination}"

gh api "${download_url}" > "${archive}"
unzip -q "${archive}" -d "${destination}"

marker="$(find "${destination}" -type f -name approved-tree.sha -print -quit)"
if [[ -z "${marker}" ]]; then
  echo "Artefato ${artifact_name} não contém approved-tree.sha; rebuild será usado."
  rm -rf "${destination}"
  exit 3
fi

approved_tree="$(tr -d '\r\n' < "${marker}")"
if [[ "${approved_tree}" != "${current_tree}" ]]; then
  echo "Árvore aprovada diverge da main atual: aprovado=${approved_tree} atual=${current_tree}."
  rm -rf "${destination}"
  exit 4
fi

# O backend precisa reutilizar exatamente o JAR executável e as classes que
# passaram nos testes do PR. A verificação pós-merge compara essas classes com
# BOOT-INF/classes do JAR antes de construir a imagem.
if [[ "${artifact_name}" == "backend-approved-package" ]]; then
  runtime_jar="$(find "${destination}" -type f -name app-exec.jar -print -quit)"
  classes_dir="$(find "${destination}" -type d -name classes -print -quit)"
  if [[ -z "${runtime_jar}" || -z "${classes_dir}" ]]; then
    echo "Artefato backend aprovado não contém app-exec.jar e classes compiladas; rebuild será usado."
    rm -rf "${destination}"
    exit 3
  fi

  mkdir -p backend/ads-service/target
  cp "${runtime_jar}" backend/ads-service/target/app-exec.jar
  rm -rf backend/ads-service/target/classes
  cp -a "${classes_dir}" backend/ads-service/target/classes
fi

printf 'Artefato %s do run %s reutilizável: PR head=%s tree=%s\n' \
  "${artifact_name}" "${run_id}" "${candidate_sha}" "${current_tree}"
