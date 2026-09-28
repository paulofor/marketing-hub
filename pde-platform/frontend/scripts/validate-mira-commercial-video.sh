#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
MEDIA_DIR="${MIRA_COMMERCIAL_MEDIA_DIR:-${FRONTEND_DIR}/public-mira-commercial/media}"
VIDEO_FILE="${MEDIA_DIR}/mira-commercial-demo-v2.mp4"
POSTER_FILE="${MEDIA_DIR}/mira-commercial-demo-v2-poster.jpg"
CONTROL_FILE="${MEDIA_DIR}/mira-commercial-control-v2.png"
HLS_DIR="${MEDIA_DIR}/mira-commercial-demo-v2-hls"
HLS_PLAYLIST="${HLS_DIR}/index.m3u8"
HLS_CHECKSUMS="${HLS_DIR}/checksums.sha256"
EXPECTED_VIDEO_SHA256="a768205bf65ee8557724899901e3050ba753755391e7e0576ffabf167162faa1"
EXPECTED_POSTER_SHA256="8a3e327ce7e201dc2255d805a75675c1951d21a8371984aa4df73629884087cf"
EXPECTED_CONTROL_SHA256="8aca36e2a1fa9433484e691d2f673c7646e7e03c49fd36f411d1e3c5aeb66472"
EXPECTED_HLS_CHECKSUMS_SHA256="d7b2b1c93af591ce04d61720813c88ed4924f8a239af7ea5a5aa0a7b90d71521"

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
