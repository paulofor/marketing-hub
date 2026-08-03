#!/usr/bin/env bash
set -euo pipefail

# Gera, revisa e promove lotes fotográficos do Agenda Cheia sem expor credenciais.
command_name="${1:-}"
library_root="${AGENDA_CHEIA_LIBRARY_ROOT:-/var/lib/marketinghub/agenda-cheia/photo-library}"
model="${OPENAI_IMAGE_MODEL:-gpt-image-2-2026-04-21}"
batch_id="${2:-$(date -u +%Y%m%dT%H%M%SZ)}"
candidate_dir="$library_root/candidates/$batch_id"
approved_dir="$library_root/approved"

require_tool() { command -v "$1" >/dev/null || { echo "Ferramenta obrigatória ausente: $1" >&2; exit 2; }; }

generate() {
  : "${OPENAI_API_KEY:?OPENAI_API_KEY é obrigatória}"
  require_tool curl; require_tool jq; require_tool base64; require_tool sha256sum
  mkdir -p "$candidate_dir"
  touch "$candidate_dir/candidate-manifest.tsv"
  styles=("clean leitoso" "french moderno" "cat-eye vinho" "chrome rose" "jelly nude" "micro french dourado" "baby boomer" "vermelho cereja" "nude mocha" "azul profundo")
  for index in "${!styles[@]}"; do
    filename="photo-$(printf '%02d' "$((index + 1))").png"
    if [[ -s "$candidate_dir/$filename" ]] && grep -q "^$filename"$'\t' "$candidate_dir/candidate-manifest.tsv"; then
      echo "Candidata já concluída: $filename"
      continue
    fi
    prompt="Fotografia macro editorial premium e fotorrealista de nail design ${styles[$index]}. Unhas grandes, nítidas e protagonistas; mãos anatomicamente corretas; composição comercial distinta; fundo elegante. Sem letras, logotipos, marcas d'água, telefone, interface, moldura ou texto incorporado."
    response="$(curl --fail --silent --show-error --connect-timeout 20 --max-time 300 \
      --retry 2 --retry-delay 5 https://api.openai.com/v1/images/generations \
      -H "Authorization: Bearer $OPENAI_API_KEY" -H 'Content-Type: application/json' \
      --data "$(jq -n --arg model "$model" --arg prompt "$prompt" '{model:$model,prompt:$prompt,size:"1024x1024",quality:"high"}')")"
    jq -er '.data[0].b64_json' <<<"$response" | base64 -d > "$candidate_dir/$filename"
    hash="$(sha256sum "$candidate_dir/$filename" | cut -d' ' -f1)"
    printf '%s\t%s\t%s\tPENDING\n' "$filename" "$hash" "$model" >> "$candidate_dir/candidate-manifest.tsv"
  done
  echo "Lote candidato gerado em $candidate_dir"
}

promote() {
  review_file="${1:?Informe o TSV de revisão humana}"
  require_tool sha256sum
  [[ -f "$candidate_dir/candidate-manifest.tsv" && -f "$review_file" ]] || { echo "Lote ou revisão inexistente" >&2; exit 3; }
  tmp_dir="$library_root/approved.next.$batch_id"; mkdir -p "$tmp_dir"
  : > "$tmp_dir/approved-manifest.tsv"; unique_hashes=""
  while IFS=$'\t' read -r filename score detected_text decision notes; do
    [[ "$filename" == \#* || -z "$filename" ]] && continue
    [[ "$decision" == "APPROVED" ]] || continue
    awk "BEGIN { exit !($score >= 9.0) }" || { echo "Nota inferior a 9: $filename" >&2; exit 4; }
    [[ "$detected_text" == "false" ]] || { echo "Texto incorporado detectado: $filename" >&2; exit 5; }
    source_file="$candidate_dir/$filename"; [[ -f "$source_file" ]] || exit 6
    hash="$(sha256sum "$source_file" | cut -d' ' -f1)"
    grep -q $'\t'"$hash"$'\t' "$candidate_dir/candidate-manifest.tsv" || { echo "Hash não auditado: $filename" >&2; exit 7; }
    [[ "$unique_hashes" != *"$hash"* ]] || { echo "Imagem repetida: $filename" >&2; exit 8; }
    unique_hashes="$unique_hashes $hash"; cp "$source_file" "$tmp_dir/$filename"
    printf '%s\t%s\t%s\t%s\tAPPROVED\tfalse\t%s\n' "$filename" "$hash" "$model" "$score" "$notes" >> "$tmp_dir/approved-manifest.tsv"
  done < "$review_file"
  count="$(find "$tmp_dir" -maxdepth 1 -type f \( -name '*.png' -o -name '*.jpg' -o -name '*.jpeg' \) | wc -l)"
  [[ "$count" -ge 10 ]] || { echo "São necessárias 10 fotos aprovadas; recebidas: $count" >&2; exit 9; }
  backup="$library_root/archive/approved-$(date -u +%Y%m%dT%H%M%SZ)"; mkdir -p "$(dirname "$backup")"
  [[ ! -d "$approved_dir" ]] || mv "$approved_dir" "$backup"
  mv "$tmp_dir" "$approved_dir"
  echo "Lote $batch_id promovido com $count fotos; acervo anterior: $backup"
}

case "$command_name" in
  generate) generate ;;
  promote) promote "${3:-}" ;;
  *) echo "Uso: $0 generate [batch-id] | promote <batch-id> <review.tsv>" >&2; exit 1 ;;
esac
