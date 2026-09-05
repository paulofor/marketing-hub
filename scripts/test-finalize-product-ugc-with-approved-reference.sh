#!/usr/bin/env bash
set -euo pipefail

script_directory=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
workspace_directory=$(cd -- "$script_directory/.." && pwd)
test_directory=$(mktemp -d)
cleanup() {
  rm -rf -- "$test_directory"
}
trap cleanup EXIT

raw_video="$test_directory/raw.mp4"
reference="$test_directory/reference.png"
audio_video="$test_directory/audio.mp4"
captions="$test_directory/captions.vtt"
output="$test_directory/final.mp4"

ffmpeg -hide_banner -loglevel error -y \
  -f lavfi -i 'color=c=0x334455:s=1080x1920:r=24:d=2' \
  -an -c:v libx264 -pix_fmt yuv420p "$raw_video"
ffmpeg -hide_banner -loglevel error -y \
  -f lavfi -i 'color=c=0xF5E5D2:s=1080x1920' -frames:v 1 "$reference"
ffmpeg -hide_banner -loglevel error -y \
  -f lavfi -i 'color=c=black:s=1080x1920:r=24:d=2' \
  -f lavfi -i 'sine=frequency=440:sample_rate=48000:duration=2' \
  -c:v libx264 -pix_fmt yuv420p -c:a aac -shortest "$audio_video"
printf 'WEBVTT\n\n1\n00:00:00.000 --> 00:00:02.000\nTeste premium.\n' > "$captions"

chmod +x "$workspace_directory/scripts/finalize-product-ugc-with-approved-reference.sh"
result=$(
  "$workspace_directory/scripts/finalize-product-ugc-with-approved-reference.sh" \
    "$raw_video" "$reference" "$audio_video" "$captions" '0.5:1.5' "$output"
)

jq -e '
  .status == "APPROVED_REFERENCE_APPLIED"
  and .width == 1080
  and .height == 1920
  and .videoCodec == "h264"
  and .audioCodec == "aac"
' <<< "$result" >/dev/null
test -s "$output"
test "$(ffprobe -v error -select_streams a:0 -show_entries stream=codec_name -of csv=p=0 "$output")" = "aac"
