#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
OUTPUT_DIR="${MIRA_COMMERCIAL_MEDIA_DIR:-${FRONTEND_DIR}/public-mira-commercial/media}"
VIDEO_FILE="${OUTPUT_DIR}/mira-commercial-demo-v1.mp4"
POSTER_FILE="${OUTPUT_DIR}/mira-commercial-demo-v1-poster.jpg"

if ! command -v ffmpeg >/dev/null 2>&1; then
  echo "ffmpeg é obrigatório para gerar o vídeo comercial de Mira." >&2
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
  -f lavfi -i "color=c=#f7f1f8:s=1080x1920:r=30:d=18" \
  -vf "
drawbox=x=0:y=0:w=iw:h=ih:color=#f7f1f8:t=fill,
drawbox=x='-180+34*t':y='80+7*t':w=520:h=520:color=#ead8ee@0.55:t=fill,
drawbox=x='760-20*t':y='1450-5*t':w=440:h=440:color=#dfc9e5@0.42:t=fill,

drawtext=fontfile='${FONT_BOLD}':text='MIRA':fontcolor=#6b3e7d:fontsize=42:x=(w-text_w)/2:y=150:enable='between(t,0,3.6)',
drawtext=fontfile='${FONT_BOLD}':text='Mais clareza para':fontcolor=#2e2034:fontsize=76:x=(w-text_w)/2:y=475:enable='between(t,0,3.6)',
drawtext=fontfile='${FONT_BOLD}':text='cuidar de você.':fontcolor=#2e2034:fontsize=76:x=(w-text_w)/2:y=575:enable='between(t,0,3.6)',
drawtext=fontfile='${FONT_REGULAR}':text='Com os produtos que você já tem.':fontcolor=#6b526f:fontsize=43:x=(w-text_w)/2:y=735:enable='between(t,0,3.6)',
drawbox=x=230:y=930:w=620:h=110:color=#6b3e7d:t=fill:enable='between(t,0,3.6)',
drawtext=fontfile='${FONT_BOLD}':text='VEJA COMO FUNCIONA':fontcolor=white:fontsize=34:x=(w-text_w)/2:y=967:enable='between(t,0,3.6)',

drawtext=fontfile='${FONT_BOLD}':text='PASSO 1':fontcolor=#7a4e8c:fontsize=36:x=100:y=150:enable='between(t,3.6,7.2)',
drawtext=fontfile='${FONT_BOLD}':text='Conte o que você já usa':fontcolor=#2e2034:fontsize=62:x=100:y=235:enable='between(t,3.6,7.2)',
drawtext=fontfile='${FONT_REGULAR}':text='Nome e orientação do rótulo. Só isso.':fontcolor=#6b526f:fontsize=36:x=100:y=330:enable='between(t,3.6,7.2)',
drawbox=x=100:y=470:w=880:h=255:color=white@0.96:t=fill:enable='between(t,3.6,7.2)',
drawtext=fontfile='${FONT_REGULAR}':text='Produto 1':fontcolor=#7a687f:fontsize=29:x=145:y=520:enable='between(t,3.6,7.2)',
drawtext=fontfile='${FONT_BOLD}':text='Sabonete facial':fontcolor=#302336:fontsize=43:x=145:y=575:enable='between(t,3.6,7.2)',
drawtext=fontfile='${FONT_REGULAR}':text='Limpar e enxaguar':fontcolor=#6b526f:fontsize=31:x=145:y=645:enable='between(t,3.6,7.2)',
drawbox=x=100:y=760:w=880:h=255:color=white@0.96:t=fill:enable='between(t,3.6,7.2)',
drawtext=fontfile='${FONT_REGULAR}':text='Produto 2':fontcolor=#7a687f:fontsize=29:x=145:y=810:enable='between(t,3.6,7.2)',
drawtext=fontfile='${FONT_BOLD}':text='Hidratante':fontcolor=#302336:fontsize=43:x=145:y=865:enable='between(t,3.6,7.2)',
drawtext=fontfile='${FONT_REGULAR}':text='Aplicar após a limpeza':fontcolor=#6b526f:fontsize=31:x=145:y=935:enable='between(t,3.6,7.2)',
drawbox=x=255:y=1120:w=570:h=108:color=#6b3e7d:t=fill:enable='between(t,3.6,7.2)',
drawtext=fontfile='${FONT_BOLD}':text='ORGANIZAR MINHA ROTINA':fontcolor=white:fontsize=31:x=(w-text_w)/2:y=1158:enable='between(t,3.6,7.2)',

drawtext=fontfile='${FONT_BOLD}':text='PASSO 2':fontcolor=#7a4e8c:fontsize=36:x=100:y=150:enable='between(t,7.2,10.8)',
drawtext=fontfile='${FONT_BOLD}':text='Receba uma ordem simples':fontcolor=#2e2034:fontsize=58:x=100:y=235:enable='between(t,7.2,10.8)',
drawtext=fontfile='${FONT_REGULAR}':text='A orientação fica salva para consultar.':fontcolor=#6b526f:fontsize=36:x=100:y=330:enable='between(t,7.2,10.8)',
drawbox=x=100:y=480:w=880:h=245:color=white@0.96:t=fill:enable='between(t,7.2,10.8)',
drawbox=x=145:y=530:w=76:h=76:color=#6b3e7d:t=fill:enable='between(t,7.2,10.8)',
drawtext=fontfile='${FONT_BOLD}':text='1':fontcolor=white:fontsize=40:x=171:y=546:enable='between(t,7.2,10.8)',
drawtext=fontfile='${FONT_BOLD}':text='Sabonete facial':fontcolor=#302336:fontsize=42:x=260:y=520:enable='between(t,7.2,10.8)',
drawtext=fontfile='${FONT_REGULAR}':text='Limpar e enxaguar':fontcolor=#6b526f:fontsize=31:x=260:y=585:enable='between(t,7.2,10.8)',
drawbox=x=100:y=765:w=880:h=245:color=white@0.96:t=fill:enable='between(t,7.2,10.8)',
drawbox=x=145:y=815:w=76:h=76:color=#6b3e7d:t=fill:enable='between(t,7.2,10.8)',
drawtext=fontfile='${FONT_BOLD}':text='2':fontcolor=white:fontsize=40:x=171:y=831:enable='between(t,7.2,10.8)',
drawtext=fontfile='${FONT_BOLD}':text='Hidratante':fontcolor=#302336:fontsize=42:x=260:y=805:enable='between(t,7.2,10.8)',
drawtext=fontfile='${FONT_REGULAR}':text='Aplicar após a limpeza':fontcolor=#6b526f:fontsize=31:x=260:y=870:enable='between(t,7.2,10.8)',
drawtext=fontfile='${FONT_BOLD}':text='Você decide. Mira organiza.':fontcolor=#6b3e7d:fontsize=42:x=(w-text_w)/2:y=1135:enable='between(t,7.2,10.8)',

drawtext=fontfile='${FONT_BOLD}':text='Clareza sem promessas vazias':fontcolor=#2e2034:fontsize=58:x=(w-text_w)/2:y=270:enable='between(t,10.8,14.2)',
drawbox=x=100:y=470:w=880:h=150:color=white@0.96:t=fill:enable='between(t,10.8,14.2)',
drawtext=fontfile='${FONT_BOLD}':text='✓  Use os produtos que já tem':fontcolor=#4f315b:fontsize=39:x=155:y=520:enable='between(t,10.8,14.2)',
drawbox=x=100:y=655:w=880:h=150:color=white@0.96:t=fill:enable='between(t,10.8,14.2)',
drawtext=fontfile='${FONT_BOLD}':text='✓  Sem empurrar novos cosméticos':fontcolor=#4f315b:fontsize=37:x=155:y=705:enable='between(t,10.8,14.2)',
drawbox=x=100:y=840:w=880:h=150:color=white@0.96:t=fill:enable='between(t,10.8,14.2)',
drawtext=fontfile='${FONT_BOLD}':text='✓  Sem diagnóstico ou prescrição':fontcolor=#4f315b:fontsize=37:x=155:y=890:enable='between(t,10.8,14.2)',
drawtext=fontfile='${FONT_REGULAR}':text='Se faltar informação do rótulo, Mira avisa.':fontcolor=#6b526f:fontsize=34:x=(w-text_w)/2:y=1100:enable='between(t,10.8,14.2)',

drawbox=x=0:y=0:w=iw:h=ih:color=#392442:t=fill:enable='between(t,14.2,18)',
drawtext=fontfile='${FONT_BOLD}':text='MIRA':fontcolor=#eacff0:fontsize=42:x=(w-text_w)/2:y=220:enable='between(t,14.2,18)',
drawtext=fontfile='${FONT_BOLD}':text='Duas organizações':fontcolor=white:fontsize=72:x=(w-text_w)/2:y=485:enable='between(t,14.2,18)',
drawtext=fontfile='${FONT_BOLD}':text='por R$ 49':fontcolor=white:fontsize=94:x=(w-text_w)/2:y=585:enable='between(t,14.2,18)',
drawtext=fontfile='${FONT_REGULAR}':text='Pagamento único':fontcolor=#ead8ee:fontsize=38:x=(w-text_w)/2:y=725:enable='between(t,14.2,18)',
drawbox=x=190:y=880:w=700:h=125:color=#f2deef:t=fill:enable='between(t,14.2,18)',
drawtext=fontfile='${FONT_BOLD}':text='ORGANIZAR MINHA ROTINA':fontcolor=#392442:fontsize=38:x=(w-text_w)/2:y=923:enable='between(t,14.2,18)',
drawtext=fontfile='${FONT_REGULAR}':text='Comece com o que você já tem.':fontcolor=#ead8ee:fontsize=37:x=(w-text_w)/2:y=1120:enable='between(t,14.2,18)',
fade=t=in:st=0:d=0.35,fade=t=out:st=17.65:d=0.35,
format=yuv420p
" \
  -an -c:v libx264 -preset medium -crf 19 -profile:v high -level 4.1 \
  -movflags +faststart "${VIDEO_FILE}"

ffmpeg -hide_banner -loglevel error -y \
  -ss 1.6 -i "${VIDEO_FILE}" -frames:v 1 -q:v 2 "${POSTER_FILE}"

printf 'Vídeo comercial de Mira gerado em %s\n' "${VIDEO_FILE}"
printf 'Poster comercial de Mira gerado em %s\n' "${POSTER_FILE}"
