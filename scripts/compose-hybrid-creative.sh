#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 4 ]]; then
  echo "uso: $0 <post.png> <story.png> <saida.png> <feed|story>" >&2
  exit 2
fi

post_file="$1"
story_file="$2"
output_file="$3"
placement="$4"
font_file="/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"

for input_file in "$post_file" "$story_file"; do
  [[ -s "$input_file" ]] || { echo "referência ausente: $input_file" >&2; exit 3; }
done
[[ -r "$font_file" ]] || { echo "fonte determinística ausente: $font_file" >&2; exit 4; }

case "$placement" in
  feed)
    canvas="1080x1350"; post_size="620:620"; story_size="340:604"
    post_x=50; post_y=380; story_x=690; story_y=396
    header_height=285; title_y=58; subtitle_y=145; post_label_y=320; story_label_y=320; story_label_x=690; story_label_font=28
    footer_y=1080; footer_text_y=1170
    ;;
  story)
    canvas="1080x1920"; post_size="700:700"; story_size="400:711"
    post_x=50; post_y=535; story_x=630; story_y=860
    header_height=420; title_y=105; subtitle_y=215; post_label_y=465; story_label_y=790; story_label_x=770; story_label_font=22
    footer_y=1650; footer_text_y=1745
    ;;
  *) echo "formato inválido: $placement" >&2; exit 5 ;;
esac

filter="[1:v]scale=${post_size}:force_original_aspect_ratio=decrease,pad=${post_size}:(ow-iw)/2:(oh-ih)/2:color=0xF5E9E2,setsar=1[post];"
filter+="[2:v]scale=${story_size},setsar=1[story];"
filter+="[0:v]drawbox=x=0:y=0:w=iw:h=${header_height}:color=0x641334:t=fill,"
filter+="drawbox=x=$((post_x - 20)):y=$((post_y - 20)):w=$(( ${post_size%%:*} + 40 )):h=$(( ${post_size##*:} + 40 )):color=black@0.18:t=fill,"
filter+="drawbox=x=$((story_x - 20)):y=$((story_y - 20)):w=$(( ${story_size%%:*} + 40 )):h=$(( ${story_size##*:} + 40 )):color=black@0.18:t=fill,"
filter+="drawbox=x=0:y=${footer_y}:w=iw:h=ih-${footer_y}:color=0x2B2026:t=fill,"
filter+="drawtext=fontfile=${font_file}:text='KIT DIGITAL PERSONALIZÁVEL':fontcolor=0xF6D9A7:fontsize=40:x=70:y=${title_y},"
filter+="drawtext=fontfile=${font_file}:text='POSTS + STORIES PRONTOS':fontcolor=white:fontsize=58:x=70:y=${subtitle_y},"
filter+="drawtext=fontfile=${font_file}:text='10 POSTS  •  10 STORIES  •  TEXTOS PRONTOS':fontcolor=white:fontsize=31:x=(w-text_w)/2:y=${footer_text_y}[stage];"
filter+="[stage][post]overlay=${post_x}:${post_y}[with_post];"
filter+="[with_post][story]overlay=${story_x}:${story_y},"
filter+="drawtext=fontfile=${font_file}:text='EXEMPLO DE POST':fontcolor=0x641334:fontsize=28:x=${post_x}:y=${post_label_y},"
filter+="drawtext=fontfile=${font_file}:text='EXEMPLO DE STORY':fontcolor=0x641334:fontsize=${story_label_font}:x=${story_label_x}:y=${story_label_y},format=rgb24[out]"

ffmpeg -hide_banner -loglevel error -y \
  -f lavfi -i "color=c=0xF5E9E2:s=${canvas}:d=1" \
  -i "$post_file" -i "$story_file" \
  -filter_complex "$filter" -map "[out]" -frames:v 1 "$output_file"

actual_size="$(ffprobe -v error -select_streams v:0 -show_entries stream=width,height -of csv=s=x:p=0 "$output_file")"
[[ "$actual_size" == "$canvas" ]] || { echo "dimensão inválida: $actual_size" >&2; exit 6; }
