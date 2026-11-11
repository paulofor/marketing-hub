#!/usr/bin/env bash
# Prepara ferramentas versionadas e comprova o formato realmente consumido pelo build.
set -euo pipefail
: "${RUNNER_TEMP:?Informe o diretório temporário do job}"
: "${GITHUB_PATH:?Informe o arquivo PATH do job}"
video_tools_dir="$RUNNER_TEMP/pde-video-tools"
video_source_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
mkdir -p "$video_tools_dir"
if command -v ffmpeg >/dev/null && command -v ffprobe >/dev/null; then
  video_ffmpeg=$(command -v ffmpeg)
  video_ffprobe=$(command -v ffprobe)
else
  video_archive="$video_tools_dir/ffmpeg.tar.xz"
  curl --fail --silent --show-error --location --max-time 90 \
    https://github.com/FFmpeg-Builds/FFmpeg-Builds/releases/download/autobuild-2026-11-11-12-39/ffmpeg-n8.1.3-35-gf1acb0a266-linux64-gpl-8.1.tar.xz \
    --output "$video_archive"
  printf '%s  %s\n' '32430dc6642d835511470728811e1555ec7ec09d4d27638b059f0e577a98d4e7' "$video_archive" | sha256sum --check -
  tar -xJf "$video_archive" --strip-components=2 -C "$video_tools_dir" \
    --wildcards '*/bin/ffmpeg' '*/bin/ffprobe'
  chmod 755 "$video_tools_dir/ffmpeg" "$video_tools_dir/ffprobe"
  video_ffmpeg="$video_tools_dir/ffmpeg"
  video_ffprobe="$video_tools_dir/ffprobe"
fi
"$video_ffmpeg" -version >/dev/null
"$video_ffprobe" -version >/dev/null
# Uma versão válida não comprova que o decoder SVG funciona; verifica a saída real antes do PATH.
"$video_ffmpeg" -hide_banner -loglevel error -y \
  -i "$video_source_root/pde-platform/frontend/src/video-reference-assets/musa-product-ugc-reference.svg" \
  -frames:v 1 -pix_fmt rgb24 "$video_tools_dir/svg-smoke.png"
video_dimensions=$("$video_ffprobe" -v error -show_entries stream=width,height -of csv=p=0 "$video_tools_dir/svg-smoke.png")
[[ "$video_dimensions" == '1080,1920' ]] || { echo 'Ferramentas de vídeo não produziram o PNG esperado.' >&2; exit 1; }
if [[ "$video_ffmpeg" == "$video_tools_dir/ffmpeg" ]]; then
  printf '%s\n' "$video_tools_dir" >> "$GITHUB_PATH"
fi
