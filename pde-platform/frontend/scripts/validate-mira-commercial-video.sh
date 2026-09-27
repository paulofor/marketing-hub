#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
MEDIA_DIR="${MIRA_COMMERCIAL_MEDIA_DIR:-${FRONTEND_DIR}/public-mira-commercial/media}"
VIDEO_FILE="${MEDIA_DIR}/mira-commercial-demo-v1.mp4"
POSTER_FILE="${MEDIA_DIR}/mira-commercial-demo-v1-poster.jpg"
EXPECTED_VIDEO_SHA256="d28f786b4d4ce3da3c2f8e0576974bcbddcfc61cb171057f8094067f1067b3b6"
EXPECTED_POSTER_SHA256="0765609b03abb364c2d562f700506541f3eafcb88f487c82fde4148547fb5637"

for required_command in ffmpeg ffprobe sha256sum; do
  if ! command -v "${required_command}" >/dev/null 2>&1; then
    echo "${required_command} é obrigatório para validar o vídeo comercial de Mira." >&2
    exit 1
  fi
done

for media_file in "${VIDEO_FILE}" "${POSTER_FILE}"; do
  if [[ ! -f "${media_file}" ]]; then
    echo "Ativo comercial canônico de Mira não encontrado: ${media_file}" >&2
    exit 1
  fi
done

observed_video_sha256="$(sha256sum "${VIDEO_FILE}" | awk '{print $1}')"
observed_poster_sha256="$(sha256sum "${POSTER_FILE}" | awk '{print $1}')"
if [[ "${observed_video_sha256}" != "${EXPECTED_VIDEO_SHA256}" \
  || "${observed_poster_sha256}" != "${EXPECTED_POSTER_SHA256}" ]]; then
  echo "Ativos comerciais canônicos de Mira divergem dos hashes homologados." >&2
  exit 1
fi

audio_codec="$(ffprobe -v error -select_streams a:0 \
  -show_entries stream=codec_name -of default=noprint_wrappers=1:nokey=1 "${VIDEO_FILE}")"
video_codec="$(ffprobe -v error -select_streams v:0 \
  -show_entries stream=codec_name -of default=noprint_wrappers=1:nokey=1 "${VIDEO_FILE}")"
video_dimensions="$(ffprobe -v error -select_streams v:0 \
  -show_entries stream=width,height -of csv=s=x:p=0 "${VIDEO_FILE}")"
poster_dimensions="$(ffprobe -v error -select_streams v:0 \
  -show_entries stream=width,height -of csv=s=x:p=0 "${POSTER_FILE}")"
video_duration="$(ffprobe -v error -show_entries format=duration \
  -of default=noprint_wrappers=1:nokey=1 "${VIDEO_FILE}")"

if [[ "${audio_codec}" != "aac" || "${video_codec}" != "h264" \
  || "${video_dimensions}" != "1080x1920" \
  || "${poster_dimensions}" != "1080x1920" \
  || ! "${video_duration}" =~ ^15([.]0+)?$ ]]; then
  echo "Ativos comerciais de Mira divergem do contrato H.264/AAC, 1080x1920 e 15 segundos." >&2
  exit 1
fi

ffmpeg -hide_banner -loglevel error -xerror -i "${VIDEO_FILE}" -f null -
printf 'Ativos comerciais canônicos de Mira validados.\n'
