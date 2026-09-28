#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
MEDIA_DIR="${MIRA_COMMERCIAL_MEDIA_DIR:-${FRONTEND_DIR}/public-mira-commercial/media}"
VIDEO_FILE="${MEDIA_DIR}/mira-commercial-demo-v1.mp4"
POSTER_FILE="${MEDIA_DIR}/mira-commercial-demo-v1-poster.jpg"
CONTROL_FILE="${MEDIA_DIR}/mira-commercial-control-v1.png"
HLS_DIR="${MEDIA_DIR}/mira-commercial-demo-v1-hls"
HLS_PLAYLIST="${HLS_DIR}/index.m3u8"
HLS_CHECKSUMS="${HLS_DIR}/checksums.sha256"
EXPECTED_VIDEO_SHA256="e1123f4bf456dcfb459bc2709c5db7800c742178fc6f2b8db165ab552abb6b43"
EXPECTED_POSTER_SHA256="7964614ac7a5c7e42006473be7c0deabff17f45281bd39ec05f30e9ab6d9a081"
EXPECTED_CONTROL_SHA256="a87cc42da6a23a642fdd98fa8b0cf34718d29b43f937e4764ba1090ac9f0d1dc"
EXPECTED_HLS_CHECKSUMS_SHA256="215fcc351f952b006de9a924cd0563fd0a26c33a60d8516a7f29c8588f5805e0"

for required_command in ffmpeg ffprobe sha256sum find grep sort awk; do
  if ! command -v "${required_command}" >/dev/null 2>&1; then
    echo "${required_command} é obrigatório para validar o vídeo comercial de Mira." >&2
    exit 1
  fi
done

for media_file in \
  "${VIDEO_FILE}" \
  "${POSTER_FILE}" \
  "${CONTROL_FILE}" \
  "${HLS_PLAYLIST}" \
  "${HLS_CHECKSUMS}"; do
  if [[ ! -f "${media_file}" ]]; then
    echo "Ativo comercial canônico de Mira não encontrado: ${media_file}" >&2
    exit 1
  fi
done

observed_video_sha256="$(sha256sum "${VIDEO_FILE}" | awk '{print $1}')"
observed_poster_sha256="$(sha256sum "${POSTER_FILE}" | awk '{print $1}')"
observed_control_sha256="$(sha256sum "${CONTROL_FILE}" | awk '{print $1}')"
observed_hls_checksums_sha256="$(sha256sum "${HLS_CHECKSUMS}" | awk '{print $1}')"
if [[ "${observed_video_sha256}" != "${EXPECTED_VIDEO_SHA256}" \
  || "${observed_poster_sha256}" != "${EXPECTED_POSTER_SHA256}" \
  || "${observed_control_sha256}" != "${EXPECTED_CONTROL_SHA256}" \
  || "${observed_hls_checksums_sha256}" != "${EXPECTED_HLS_CHECKSUMS_SHA256}" ]]; then
  echo "Ativos comerciais canônicos de Mira divergem dos hashes homologados." >&2
  exit 1
fi

mapfile -t hls_segments < <(
  find "${HLS_DIR}" -maxdepth 1 -type f -name 'segment-*.ts' -print | sort
)
if [[ "${#hls_segments[@]}" -lt 1 ]]; then
  echo "HLS comercial de Mira não possui segmentos." >&2
  exit 1
fi
if grep -Evq '^[0-9a-f]{64}  (index[.]m3u8|segment-[0-9]{3}[.]ts)$' "${HLS_CHECKSUMS}"; then
  echo "Manifesto de hashes HLS de Mira contém caminho ou formato inválido." >&2
  exit 1
fi
(
  cd "${HLS_DIR}"
  sha256sum -c checksums.sha256 >/dev/null
)

manifest_segment_count="$(grep -Ec '^segment-[0-9]{3}[.]ts$' "${HLS_PLAYLIST}")"
if [[ "${manifest_segment_count}" -ne "${#hls_segments[@]}" ]] \
  || ! grep -Fqx '#EXT-X-PLAYLIST-TYPE:VOD' "${HLS_PLAYLIST}" \
  || ! grep -Fqx '#EXT-X-INDEPENDENT-SEGMENTS' "${HLS_PLAYLIST}" \
  || ! grep -Fqx '#EXT-X-ENDLIST' "${HLS_PLAYLIST}"; then
  echo "Playlist HLS de Mira não representa integralmente o vídeo canônico VOD." >&2
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
control_dimensions="$(ffprobe -v error -select_streams v:0 \
  -show_entries stream=width,height -of csv=s=x:p=0 "${CONTROL_FILE}")"
video_duration="$(ffprobe -v error -show_entries format=duration \
  -of default=noprint_wrappers=1:nokey=1 "${VIDEO_FILE}")"
hls_audio_codec="$(ffprobe -v error -select_streams a:0 \
  -show_entries stream=codec_name -of default=noprint_wrappers=1:nokey=1 "${HLS_PLAYLIST}" \
  | awk 'NF' | sort -u)"
hls_video_codec="$(ffprobe -v error -select_streams v:0 \
  -show_entries stream=codec_name -of default=noprint_wrappers=1:nokey=1 "${HLS_PLAYLIST}" \
  | awk 'NF' | sort -u)"
hls_dimensions="$(ffprobe -v error -select_streams v:0 \
  -show_entries stream=width,height -of csv=s=x:p=0 "${HLS_PLAYLIST}" \
  | awk 'NF' | sort -u)"
hls_duration="$(ffprobe -v error -show_entries format=duration \
  -of default=noprint_wrappers=1:nokey=1 "${HLS_PLAYLIST}")"

if [[ "${audio_codec}" != "aac" || "${video_codec}" != "h264" \
  || "${video_dimensions}" != "1080x1920" \
  || "${poster_dimensions}" != "1080x1920" \
  || "${control_dimensions}" != "1080x1350" \
  || ! "${video_duration}" =~ ^15([.]0+)?$ \
  || "${hls_audio_codec}" != "aac" || "${hls_video_codec}" != "h264" \
  || "${hls_dimensions}" != "1080x1920" \
  || ! "${hls_duration}" =~ ^15([.]0+)?$ ]]; then
  echo "Ativos comerciais de Mira divergem do contrato H.264/AAC, 1080x1920 e 15 segundos." >&2
  exit 1
fi

ffmpeg -hide_banner -loglevel error -xerror -i "${VIDEO_FILE}" -f null -
ffmpeg -hide_banner -loglevel error -xerror -i "${HLS_PLAYLIST}" -f null -
printf 'Ativos comerciais canônicos de Mira validados.\n'
