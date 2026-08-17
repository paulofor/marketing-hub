#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
test_dir="$(mktemp -d)"
trap 'rm -rf "$test_dir"' EXIT

ffmpeg -hide_banner -loglevel error -y -f lavfi -i "color=c=red:s=1080x1080:d=1" -frames:v 1 "$test_dir/post.png"
ffmpeg -hide_banner -loglevel error -y -f lavfi -i "color=c=blue:s=1080x1920:d=1" -frames:v 1 "$test_dir/story.png"
"$script_dir/compose-hybrid-creative.sh" "$test_dir/post.png" "$test_dir/story.png" "$test_dir/feed.png" feed
"$script_dir/compose-hybrid-creative.sh" "$test_dir/post.png" "$test_dir/story.png" "$test_dir/story-output.png" story
[[ "$(ffprobe -v error -show_entries stream=width,height -of csv=s=x:p=0 "$test_dir/feed.png")" == "1080x1350" ]]
[[ "$(ffprobe -v error -show_entries stream=width,height -of csv=s=x:p=0 "$test_dir/story-output.png")" == "1080x1920" ]]
