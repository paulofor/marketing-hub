#!/usr/bin/env bash
# Reutiliza ferramentas existentes ou extrai a distribuição estática já fixada no repositório.
set -euo pipefail
if command -v ffmpeg >/dev/null && command -v ffprobe >/dev/null; then
  ffmpeg -version >/dev/null
  ffprobe -version >/dev/null
  exit 0
fi
: "${RUNNER_TEMP:?Informe o diretório temporário do job}"
: "${GITHUB_PATH:?Informe o arquivo PATH do job}"
video_tools_dir="$RUNNER_TEMP/pde-video-tools"
video_tools_container=""
cleanup_video_tools() {
  if [[ -n "$video_tools_container" ]]; then
    docker rm "$video_tools_container" >/dev/null
  fi
}
trap cleanup_video_tools EXIT
mkdir -p "$video_tools_dir"
video_tools_container=$(docker create --label marketinghub.purpose=ci-video-tools \
  mwader/static-ffmpeg:7.1.1@sha256:6769881cc02c80d33e387750a8e144d162adfab2775e934dd97899261dda3a0c)
docker cp "$video_tools_container:/ffmpeg" "$video_tools_dir/ffmpeg"
docker cp "$video_tools_container:/ffprobe" "$video_tools_dir/ffprobe"
chmod 755 "$video_tools_dir/ffmpeg" "$video_tools_dir/ffprobe"
"$video_tools_dir/ffmpeg" -version >/dev/null
"$video_tools_dir/ffprobe" -version >/dev/null
printf '%s\n' "$video_tools_dir" >> "$GITHUB_PATH"
