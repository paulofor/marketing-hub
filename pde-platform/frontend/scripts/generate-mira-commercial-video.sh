#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
OUTPUT_DIR="${MIRA_COMMERCIAL_MEDIA_DIR:-${FRONTEND_DIR}/public-mira-commercial/media}"
VIDEO_FILE="${OUTPUT_DIR}/mira-commercial-demo-v3.mp4"
POSTER_FILE="${OUTPUT_DIR}/mira-commercial-demo-v3-poster.jpg"
HLS_DIR="${OUTPUT_DIR}/mira-commercial-demo-v3-hls"
HLS_PLAYLIST="${HLS_DIR}/index.m3u8"
AUDIO_SOURCE="${SCRIPT_DIR}/media/mira-approved-voice-asset-47-v1.m4a"
AUDIO_SOURCE_SHA256="19be7c4776e20dae6ed783264495e85d97ee2156fc67075e85f79af639e0ef63"
PRODUCT_PROOF="${OUTPUT_DIR}/mira-commercial-product-proof-v1.png"
PRODUCT_PROOF_SHA256="4ffda62d502d8ea644fa06a0cd6cf0768c39c4278b43bdcefa3665e9d578c2f3"

if ! command -v ffmpeg >/dev/null 2>&1; then
  echo "ffmpeg é obrigatório para gerar o vídeo comercial de Mira." >&2
  exit 1
fi
if ! command -v ffprobe >/dev/null 2>&1; then
  echo "ffprobe é obrigatório para validar o vídeo comercial de Mira." >&2
  exit 1
fi
if [[ ! -f "${AUDIO_SOURCE}" ]]; then
  echo "Narração comercial aprovada de Mira não encontrada: ${AUDIO_SOURCE}" >&2
  exit 1
fi
if [[ ! -f "${PRODUCT_PROOF}" ]]; then
  echo "Prova de produto de Mira não encontrada: ${PRODUCT_PROOF}" >&2
  exit 1
fi

observed_audio_sha256="$(sha256sum "${AUDIO_SOURCE}" | awk '{print $1}')"
if [[ "${observed_audio_sha256}" != "${AUDIO_SOURCE_SHA256}" ]]; then
  echo "Narração comercial de Mira diverge do ativo aprovado." >&2
  exit 1
fi
observed_product_proof_sha256="$(sha256sum "${PRODUCT_PROOF}" | awk '{print $1}')"
if [[ "${observed_product_proof_sha256}" != "${PRODUCT_PROOF_SHA256}" ]]; then
  echo "Prova de produto de Mira diverge do ativo aprovado para composição." >&2
  exit 1
fi

find_font() {
  local candidate
  for candidate in "$@"; do
    if [[ -f "${candidate}" ]]; then
      printf '%s\n' "${candidate}"
      return 0
    fi
  done
  return 1
}

FONT_REGULAR="$(find_font \
  /usr/share/fonts/dejavu/DejaVuSans.ttf \
  /usr/share/fonts/ttf-dejavu/DejaVuSans.ttf \
  /usr/share/fonts/truetype/dejavu/DejaVuSans.ttf)" || {
  echo "Fonte DejaVu Sans não encontrada." >&2
  exit 1
}
FONT_BOLD="$(find_font \
  /usr/share/fonts/dejavu/DejaVuSans-Bold.ttf \
  /usr/share/fonts/ttf-dejavu/DejaVuSans-Bold.ttf \
  /usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf)" || {
  echo "Fonte DejaVu Sans Bold não encontrada." >&2
  exit 1
}

mkdir -p "${OUTPUT_DIR}"

ffmpeg -hide_banner -loglevel error -y \
  -f lavfi -i "color=c=#f7f1f8:s=1080x1920:r=30:d=15" \
  -i "${AUDIO_SOURCE}" \
  -loop 1 -i "${PRODUCT_PROOF}" \
  -filter_complex "
[0:v]
drawbox=x=0:y=0:w=iw:h=ih:color=#f7f1f8:t=fill,
drawbox=x='-180+34*t':y='80+7*t':w=520:h=520:color=#ead8ee@0.55:t=fill,
drawbox=x='760-20*t':y='1450-5*t':w=440:h=440:color=#dfc9e5@0.42:t=fill,

drawtext=fontfile='${FONT_BOLD}':text='MIRA':fontcolor=#6b3e7d:fontsize=42:x=(w-text_w)/2:y=150:enable='between(t,0,3.2)',
drawtext=fontfile='${FONT_BOLD}':text='APLICAÇÃO WEB':fontcolor=#7a4e8c:fontsize=36:x=(w-text_w)/2:y=405:enable='between(t,0,3.2)',
drawtext=fontfile='${FONT_BOLD}':text='Organiza sua rotina':fontcolor=#2e2034:fontsize=72:x=(w-text_w)/2:y=500:enable='between(t,0,3.2)',
drawtext=fontfile='${FONT_BOLD}':text='com o que você já tem.':fontcolor=#2e2034:fontsize=58:x=(w-text_w)/2:y=605:enable='between(t,0,3.2)',
drawbox=x=230:y=830:w=620:h=110:color=#6b3e7d:t=fill:enable='between(t,0,3.2)',
drawtext=fontfile='${FONT_BOLD}':text='VEJA COMO FUNCIONA':fontcolor=white:fontsize=34:x=(w-text_w)/2:y=867:enable='between(t,0,3.2)',

drawtext=fontfile='${FONT_BOLD}':text='PASSO 1':fontcolor=#7a4e8c:fontsize=36:x=100:y=150:enable='between(t,3.2,6.3)',
drawtext=fontfile='${FONT_BOLD}':text='Conte o que você já usa':fontcolor=#2e2034:fontsize=62:x=100:y=235:enable='between(t,3.2,6.3)',
drawtext=fontfile='${FONT_REGULAR}':text='Nome e orientação do rótulo. Só isso.':fontcolor=#6b526f:fontsize=36:x=100:y=330:enable='between(t,3.2,6.3)',
drawbox=x=100:y=470:w=880:h=255:color=white@0.96:t=fill:enable='between(t,3.2,6.3)',
drawtext=fontfile='${FONT_REGULAR}':text='Produto 1':fontcolor=#7a687f:fontsize=29:x=145:y=520:enable='between(t,3.2,6.3)',
drawtext=fontfile='${FONT_BOLD}':text='Sabonete facial':fontcolor=#302336:fontsize=43:x=145:y=575:enable='between(t,3.2,6.3)',
drawtext=fontfile='${FONT_REGULAR}':text='Limpar e enxaguar':fontcolor=#6b526f:fontsize=31:x=145:y=645:enable='between(t,3.2,6.3)',
drawbox=x=100:y=760:w=880:h=255:color=white@0.96:t=fill:enable='between(t,3.2,6.3)',
drawtext=fontfile='${FONT_REGULAR}':text='Produto 2':fontcolor=#7a687f:fontsize=29:x=145:y=810:enable='between(t,3.2,6.3)',
drawtext=fontfile='${FONT_BOLD}':text='Hidratante':fontcolor=#302336:fontsize=43:x=145:y=865:enable='between(t,3.2,6.3)',
drawtext=fontfile='${FONT_REGULAR}':text='Aplicar após a limpeza':fontcolor=#6b526f:fontsize=31:x=145:y=935:enable='between(t,3.2,6.3)',
drawbox=x=255:y=1120:w=570:h=108:color=#6b3e7d:t=fill:enable='between(t,3.2,6.3)',
drawtext=fontfile='${FONT_BOLD}':text='ORGANIZAR MINHA ROTINA':fontcolor=white:fontsize=31:x=(w-text_w)/2:y=1158:enable='between(t,3.2,6.3)',

drawtext=fontfile='${FONT_BOLD}':text='PASSO 2':fontcolor=#7a4e8c:fontsize=36:x=100:y=150:enable='between(t,6.3,9.2)',
drawtext=fontfile='${FONT_BOLD}':text='Veja a aplicação web':fontcolor=#2e2034:fontsize=58:x=100:y=235:enable='between(t,6.3,9.2)',
drawtext=fontfile='${FONT_REGULAR}':text='A mesma tela que organiza e salva sua rotina.':fontcolor=#6b526f:fontsize=34:x=100:y=330:enable='between(t,6.3,9.2)',
drawbox=x=100:y=480:w=880:h=245:color=white@0.96:t=fill:enable='between(t,6.3,9.2)',
drawbox=x=145:y=530:w=76:h=76:color=#6b3e7d:t=fill:enable='between(t,6.3,9.2)',
drawtext=fontfile='${FONT_BOLD}':text='1':fontcolor=white:fontsize=40:x=171:y=546:enable='between(t,6.3,9.2)',
drawtext=fontfile='${FONT_BOLD}':text='Sabonete facial':fontcolor=#302336:fontsize=42:x=260:y=520:enable='between(t,6.3,9.2)',
drawtext=fontfile='${FONT_REGULAR}':text='Limpar e enxaguar':fontcolor=#6b526f:fontsize=31:x=260:y=585:enable='between(t,6.3,9.2)',
drawbox=x=100:y=765:w=880:h=245:color=white@0.96:t=fill:enable='between(t,6.3,9.2)',
drawbox=x=145:y=815:w=76:h=76:color=#6b3e7d:t=fill:enable='between(t,6.3,9.2)',
drawtext=fontfile='${FONT_BOLD}':text='2':fontcolor=white:fontsize=40:x=171:y=831:enable='between(t,6.3,9.2)',
drawtext=fontfile='${FONT_BOLD}':text='Hidratante':fontcolor=#302336:fontsize=42:x=260:y=805:enable='between(t,6.3,9.2)',
drawtext=fontfile='${FONT_REGULAR}':text='Aplicar após a limpeza':fontcolor=#6b526f:fontsize=31:x=260:y=870:enable='between(t,6.3,9.2)',
drawtext=fontfile='${FONT_BOLD}':text='Você decide. Mira organiza.':fontcolor=#6b3e7d:fontsize=42:x=(w-text_w)/2:y=1135:enable='between(t,6.3,9.2)',

drawtext=fontfile='${FONT_BOLD}':text='PASSO 3':fontcolor=#7a4e8c:fontsize=36:x=100:y=150:enable='between(t,9.2,11.6)',
drawtext=fontfile='${FONT_BOLD}':text='Consulte quando precisar':fontcolor=#2e2034:fontsize=58:x=100:y=235:enable='between(t,9.2,11.6)',
drawbox=x=100:y=470:w=880:h=150:color=white@0.96:t=fill:enable='between(t,9.2,11.6)',
drawtext=fontfile='${FONT_BOLD}':text='✓  A ordem fica salva':fontcolor=#4f315b:fontsize=39:x=155:y=520:enable='between(t,9.2,11.6)',
drawbox=x=100:y=655:w=880:h=150:color=white@0.96:t=fill:enable='between(t,9.2,11.6)',
drawtext=fontfile='${FONT_BOLD}':text='✓  Sem empurrar novos cosméticos':fontcolor=#4f315b:fontsize=37:x=155:y=705:enable='between(t,9.2,11.6)',
drawbox=x=100:y=840:w=880:h=150:color=white@0.96:t=fill:enable='between(t,9.2,11.6)',
drawtext=fontfile='${FONT_BOLD}':text='✓  Sem diagnóstico ou prescrição':fontcolor=#4f315b:fontsize=37:x=155:y=890:enable='between(t,9.2,11.6)',
drawtext=fontfile='${FONT_REGULAR}':text='Se faltar informação do rótulo, Mira avisa.':fontcolor=#6b526f:fontsize=34:x=(w-text_w)/2:y=1100:enable='between(t,9.2,11.6)',

drawbox=x=0:y=0:w=iw:h=ih:color=#392442:t=fill:enable='between(t,11.6,15)',
drawtext=fontfile='${FONT_BOLD}':text='MIRA':fontcolor=#eacff0:fontsize=42:x=(w-text_w)/2:y=220:enable='between(t,11.6,15)',
drawtext=fontfile='${FONT_BOLD}':text='Duas organizações':fontcolor=white:fontsize=72:x=(w-text_w)/2:y=425:enable='between(t,11.6,15)',
drawtext=fontfile='${FONT_BOLD}':text='individualizadas no total':fontcolor=white:fontsize=58:x=(w-text_w)/2:y=525:enable='between(t,11.6,15)',
drawtext=fontfile='${FONT_BOLD}':text='por R$ 49':fontcolor=white:fontsize=82:x=(w-text_w)/2:y=625:enable='between(t,11.6,15)',
drawtext=fontfile='${FONT_REGULAR}':text='Cada uma usa 1 das 2 tentativas':fontcolor=#ead8ee:fontsize=35:x=(w-text_w)/2:y=750:enable='between(t,11.6,15)',
drawtext=fontfile='${FONT_REGULAR}':text='Pagamento único':fontcolor=#ead8ee:fontsize=34:x=(w-text_w)/2:y=815:enable='between(t,11.6,15)',
drawbox=x=190:y=940:w=700:h=125:color=#f2deef:t=fill:enable='between(t,11.6,15)',
drawtext=fontfile='${FONT_BOLD}':text='ORGANIZAR MINHA ROTINA':fontcolor=#392442:fontsize=38:x=(w-text_w)/2:y=983:enable='between(t,11.6,15)',
drawtext=fontfile='${FONT_REGULAR}':text='Comece com o que você já tem.':fontcolor=#ead8ee:fontsize=37:x=(w-text_w)/2:y=1160:enable='between(t,11.6,15)'
[base];
[2:v]scale=900:900,format=rgba[proof];
[base][proof]overlay=x=90:y=430:enable='between(t,6.3,9.2)',
fade=t=in:st=0:d=0.35,fade=t=out:st=14.65:d=0.35,format=yuv420p[video]
" \
  -af "atrim=start=0:end=8.42,apad=pad_dur=15,atrim=duration=15" \
  -map "[video]" -map 1:a:0 \
  -c:v libx264 -preset medium -crf 19 -profile:v high -level 4.1 \
  -c:a aac -b:a 128k -ar 48000 -ac 1 \
  -t 15 \
  -movflags +faststart "${VIDEO_FILE}"

audio_codec="$(ffprobe -v error -select_streams a:0 \
  -show_entries stream=codec_name -of default=noprint_wrappers=1:nokey=1 "${VIDEO_FILE}")"
video_codec="$(ffprobe -v error -select_streams v:0 \
  -show_entries stream=codec_name -of default=noprint_wrappers=1:nokey=1 "${VIDEO_FILE}")"
video_dimensions="$(ffprobe -v error -select_streams v:0 \
  -show_entries stream=width,height -of csv=s=x:p=0 "${VIDEO_FILE}")"
video_duration="$(ffprobe -v error -show_entries format=duration \
  -of default=noprint_wrappers=1:nokey=1 "${VIDEO_FILE}")"
if [[ "${audio_codec}" != "aac" || "${video_codec}" != "h264" \
  || "${video_dimensions}" != "1080x1920" \
  || ! "${video_duration}" =~ ^15([.]0+)?$ ]]; then
  echo "Vídeo comercial de Mira diverge do contrato H.264/AAC, 1080x1920 e 15 segundos." >&2
  exit 1
fi

ffmpeg -hide_banner -loglevel error -y \
  -ss 1.6 -i "${VIDEO_FILE}" -frames:v 1 -q:v 2 "${POSTER_FILE}"

mkdir -p "${HLS_DIR}"
find "${HLS_DIR}" -maxdepth 1 -type f \
  \( -name 'index.m3u8' -o -name 'segment-*.ts' -o -name 'checksums.sha256' \) \
  -delete
ffmpeg -hide_banner -loglevel error -y \
  -i "${VIDEO_FILE}" \
  -map 0:v:0 -map 0:a:0 \
  -c:v libx264 -preset medium -crf 19 -profile:v high -level 4.1 \
  -g 150 -keyint_min 150 -sc_threshold 0 \
  -c:a aac -b:a 128k -ar 48000 -ac 1 \
  -hls_time 5 -hls_playlist_type vod -hls_flags independent_segments \
  -hls_segment_filename "${HLS_DIR}/segment-%03d.ts" \
  "${HLS_PLAYLIST}"
(
  cd "${HLS_DIR}"
  sha256sum index.m3u8 segment-*.ts > checksums.sha256
)

printf 'Vídeo comercial de Mira gerado em %s\n' "${VIDEO_FILE}"
printf 'Poster comercial de Mira gerado em %s\n' "${POSTER_FILE}"
printf 'HLS comercial de Mira gerado em %s\n' "${HLS_PLAYLIST}"
