#!/usr/bin/env bash
set -euo pipefail

# Monta o anúncio vertical de Capella somente com os criativos aprovados do experimento #88.

readonly FEED_URL="https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/experiments/creatives/2026/08/17/exp-88/163e8dda6c42-agenda-cheia-creative-feed-v3.png"
readonly STORY_URL="https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/experiments/creatives/2026/08/17/exp-88/2d41f0aa57f9-agenda-cheia-creative-story-v3.png"
readonly FEED_SHA256="1301003493f1b8f9edc13034605d4400113f81144a9b527b6b2e0f753bbbc689"
readonly STORY_SHA256="99cee8b3e5ab0cd94fec66feca1069c6aa3aeb37df6c6053944dbc04ce2303fa"
readonly FONT_BOLD="/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
readonly FONT_REGULAR="/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"

output_path="${1:-/tmp/capella-successor-video-v2.mp4}"
temp_dir="$(mktemp -d)"

# Remove somente o diretório efêmero criado por esta execução.
cleanup() {
  rm -rf -- "${temp_dir}"
}
trap cleanup EXIT

# Interrompe a montagem quando um asset deixa de ser exatamente a versão já aprovada.
verify_asset() {
  local asset_path="$1"
  local expected_sha="$2"
  local actual_sha
  actual_sha="$(sha256sum "${asset_path}" | awk '{print $1}')"
  if [[ "${actual_sha}" != "${expected_sha}" ]]; then
    printf 'Asset divergente: %s\n' "${asset_path}" >&2
    exit 1
  fi
}

command -v curl >/dev/null
command -v ffmpeg >/dev/null
command -v sha256sum >/dev/null
[[ -f "${FONT_BOLD}" ]]
[[ -f "${FONT_REGULAR}" ]]

mkdir -p "$(dirname "${output_path}")"
curl --fail --silent --show-error --location --max-time 60 \
  --output "${temp_dir}/feed.png" "${FEED_URL}"
curl --fail --silent --show-error --location --max-time 60 \
  --output "${temp_dir}/story.png" "${STORY_URL}"
verify_asset "${temp_dir}/feed.png" "${FEED_SHA256}"
verify_asset "${temp_dir}/story.png" "${STORY_SHA256}"

printf '%s\n' \
  'Seu trabalho é caprichado.' \
  'Seu Instagram mostra isso?' >"${temp_dir}/hook.txt"
printf '%s\n' \
  'Veja amostras do kit' \
  'antes de decidir.' >"${temp_dir}/proof.txt"
printf '%s\n' \
  'A amostra mostra o estilo.' \
  'R$ 67 libera o kit completo' \
  'personalizado:' \
  '10 posts  •  10 stories  •  10 legendas' \
  '5 mensagens + calendário de 7 dias' >"${temp_dir}/value.txt"
printf '%s\n' \
  'Briefing simples.' \
  'Entrega em até 3 dias úteis.' >"${temp_dir}/easy.txt"
printf '%s\n' \
  'Ver amostras do kit' \
  'Só compre se fizer sentido.' >"${temp_dir}/cta.txt"

ffmpeg -hide_banner -loglevel warning -y \
  -f lavfi -t 2.6 -i "color=c=0x6B1035:s=1080x1920:r=30" \
  -loop 1 -t 3.8 -i "${temp_dir}/story.png" \
  -loop 1 -t 3.8 -i "${temp_dir}/feed.png" \
  -f lavfi -t 4.6 -i "color=c=0xF5E8E2:s=1080x1920:r=30" \
  -f lavfi -t 3.2 -i "color=c=0x261B21:s=1080x1920:r=30" \
  -filter_complex "
    [0:v]drawtext=fontfile=${FONT_BOLD}:textfile=${temp_dir}/hook.txt:fontcolor=white:fontsize=56:line_spacing=24:x=(w-text_w)/2:y=(h-text_h)/2,format=yuv420p[v0];
    [1:v]scale=1080:1920,zoompan=z='min(1+on*0.00045,1.05)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=1:s=1080x1920:fps=30,drawtext=fontfile=${FONT_BOLD}:textfile=${temp_dir}/proof.txt:fontcolor=white:fontsize=50:line_spacing=12:box=1:boxcolor=0x261B21CC:boxborderw=28:x=(w-text_w)/2:y=h-text_h-170[v1];
    [2:v]scale=1080:-2,pad=1080:1920:0:(oh-ih)/2:color=0xF5E8E2,zoompan=z='min(1+on*0.00035,1.035)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=1:s=1080x1920:fps=30[v2];
    [3:v]drawtext=fontfile=${FONT_BOLD}:textfile=${temp_dir}/value.txt:fontcolor=0x6B1035:fontsize=40:line_spacing=28:x=(w-text_w)/2:y=420,drawtext=fontfile=${FONT_REGULAR}:textfile=${temp_dir}/easy.txt:fontcolor=0x261B21:fontsize=50:line_spacing=18:x=(w-text_w)/2:y=1280,format=yuv420p[v3];
    [4:v]drawtext=fontfile=${FONT_BOLD}:textfile=${temp_dir}/cta.txt:fontcolor=white:fontsize=64:line_spacing=24:x=(w-text_w)/2:y=(h-text_h)/2,drawtext=fontfile=${FONT_REGULAR}:text='Kit completo personalizado por R$ 67':fontcolor=0xF5D9A8:fontsize=38:x=(w-text_w)/2:y=h-220,format=yuv420p[v4];
    [v0][v1][v2][v3][v4]concat=n=5:v=1:a=0[v];
    sine=frequency=261.63:duration=1.5,afade=t=in:st=0:d=0.08,afade=t=out:st=1.35:d=0.15[n0];
    sine=frequency=329.63:duration=1.5,afade=t=in:st=0:d=0.08,afade=t=out:st=1.35:d=0.15[n1];
    sine=frequency=392.00:duration=1.5,afade=t=in:st=0:d=0.08,afade=t=out:st=1.35:d=0.15[n2];
    sine=frequency=329.63:duration=1.5,afade=t=in:st=0:d=0.08,afade=t=out:st=1.35:d=0.15[n3];
    sine=frequency=220.00:duration=1.5,afade=t=in:st=0:d=0.08,afade=t=out:st=1.35:d=0.15[n4];
    sine=frequency=261.63:duration=1.5,afade=t=in:st=0:d=0.08,afade=t=out:st=1.35:d=0.15[n5];
    sine=frequency=329.63:duration=1.5,afade=t=in:st=0:d=0.08,afade=t=out:st=1.35:d=0.15[n6];
    sine=frequency=392.00:duration=1.5,afade=t=in:st=0:d=0.08,afade=t=out:st=1.35:d=0.15[n7];
    sine=frequency=174.61:duration=1.5,afade=t=in:st=0:d=0.08,afade=t=out:st=1.35:d=0.15[n8];
    sine=frequency=220.00:duration=1.5,afade=t=in:st=0:d=0.08,afade=t=out:st=1.35:d=0.15[n9];
    sine=frequency=261.63:duration=1.5,afade=t=in:st=0:d=0.08,afade=t=out:st=1.35:d=0.15[n10];
    sine=frequency=329.63:duration=1.5,afade=t=in:st=0:d=0.08,afade=t=out:st=1.35:d=0.15[n11];
    [n0][n1][n2][n3][n4][n5][n6][n7][n8][n9][n10][n11]concat=n=12:v=0:a=1,volume=0.45,lowpass=f=1400,afade=t=in:st=0:d=0.4,afade=t=out:st=17:d=1[a]
  " \
  -map "[v]" -map "[a]" \
  -c:v libx264 -preset medium -crf 20 -pix_fmt yuv420p \
  -c:a aac -b:a 128k -movflags +faststart -shortest "${output_path}"

printf 'Vídeo criado em %s\n' "${output_path}"
