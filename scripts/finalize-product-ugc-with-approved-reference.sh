#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 6 ]]; then
  echo "uso: $0 <video-bruto.mp4> <referencia.png> <video-com-audio.mp4> <legendas.vtt> <intervalos> <saida.mp4>" >&2
  echo "intervalos: inicio:fim separados por vírgula; use fim=end no último plano" >&2
  exit 64
fi

raw_video=$1
approved_reference=$2
audio_video=$3
captions_vtt=$4
product_intervals=$5
output_video=$6

for required_file in "$raw_video" "$approved_reference" "$audio_video" "$captions_vtt"; do
  if [[ ! -s "$required_file" ]]; then
    echo "arquivo obrigatório ausente ou vazio: $required_file" >&2
    exit 66
  fi
done

for required_command in ffmpeg ffprobe jq sha256sum; do
  if ! command -v "$required_command" >/dev/null 2>&1; then
    echo "comando obrigatório indisponível: $required_command" >&2
    exit 69
  fi
done

if [[ "$output_video" == "$raw_video" || "$output_video" == "$audio_video" ]]; then
  echo "a saída não pode sobrescrever uma das fontes" >&2
  exit 64
fi

enable_expression=""
IFS=',' read -r -a interval_list <<< "$product_intervals"
for interval in "${interval_list[@]}"; do
  start=${interval%%:*}
  finish=${interval#*:}
  if [[ "$start" == "$interval" || ! "$start" =~ ^[0-9]+([.][0-9]+)?$ ]]; then
    echo "intervalo inválido: $interval" >&2
    exit 64
  fi
  if [[ "$finish" == "end" ]]; then
    term="gte(t,$start)"
  elif [[ "$finish" =~ ^[0-9]+([.][0-9]+)?$ ]]; then
    term="between(t,$start,$finish)"
  else
    echo "intervalo inválido: $interval" >&2
    exit 64
  fi
  if [[ -n "$enable_expression" ]]; then
    enable_expression+="+"
  fi
  enable_expression+="$term"
done

if [[ -z "$enable_expression" ]]; then
  echo "informe ao menos um intervalo de produto" >&2
  exit 64
fi

duration=$(ffprobe -v error -show_entries format=duration -of default=nw=1:nk=1 "$raw_video")
if [[ ! "$duration" =~ ^[0-9]+([.][0-9]+)?$ ]]; then
  echo "duração inválida no vídeo bruto: $duration" >&2
  exit 65
fi

captions_absolute=$(realpath "$captions_vtt")
if [[ "$captions_absolute" == *":"* || "$captions_absolute" == *"'"* ]]; then
  echo "o caminho das legendas contém caractere incompatível com o filtro ffmpeg" >&2
  exit 64
fi

temporary_directory=$(mktemp -d)
temporary_output="$temporary_directory/final.mp4"
cleanup() {
  rm -rf -- "$temporary_directory"
}
trap cleanup EXIT

video_filter="[1:v]scale=1080:1920:flags=lanczos[product];"
video_filter+="[0:v][product]overlay=0:0:enable='$enable_expression'[clean];"
video_filter+="[clean]subtitles=filename='$captions_absolute':charenc=UTF-8:"
video_filter+="force_style='FontName=DejaVu Sans,FontSize=10,Bold=1,PrimaryColour=&H00FFFFFF,"
video_filter+="BackColour=&H90000000,BorderStyle=3,Outline=1,Shadow=0,MarginV=32,Alignment=2',"
video_filter+="drawtext=fontfile=/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf:"
video_filter+="text='Apresentadora e voz geradas por IA':fontcolor=white:fontsize=28:box=1:boxcolor=black@0.55:"
video_filter+="x=(w-text_w)/2:y=105[vout]"

ffmpeg -hide_banner -loglevel error -y \
  -i "$raw_video" \
  -loop 1 -i "$approved_reference" \
  -i "$audio_video" \
  -filter_complex "$video_filter" \
  -map '[vout]' -map '2:a:0' \
  -c:v libx264 -preset veryfast -crf 20 -pix_fmt yuv420p \
  -c:a copy -t "$duration" -movflags +faststart \
  "$temporary_output"

width=$(ffprobe -v error -select_streams v:0 -show_entries stream=width -of csv=p=0 "$temporary_output")
height=$(ffprobe -v error -select_streams v:0 -show_entries stream=height -of csv=p=0 "$temporary_output")
video_codec=$(ffprobe -v error -select_streams v:0 -show_entries stream=codec_name -of csv=p=0 "$temporary_output")
audio_codec=$(ffprobe -v error -select_streams a:0 -show_entries stream=codec_name -of csv=p=0 "$temporary_output")
output_duration=$(ffprobe -v error -show_entries format=duration -of default=nw=1:nk=1 "$temporary_output")

if [[ "$width" != "1080" || "$height" != "1920" || "$video_codec" != "h264" || -z "$audio_codec" ]]; then
  echo "saída fora do contrato: ${width}x${height} video=$video_codec audio=$audio_codec" >&2
  exit 65
fi

mkdir -p "$(dirname "$output_video")"
mv -- "$temporary_output" "$output_video"
output_sha256=$(sha256sum "$output_video" | awk '{print $1}')

jq -n \
  --arg output "$output_video" \
  --arg sha256 "$output_sha256" \
  --arg duration "$output_duration" \
  --arg intervals "$product_intervals" \
  --arg videoCodec "$video_codec" \
  --arg audioCodec "$audio_codec" \
  '{status:"APPROVED_REFERENCE_APPLIED",output:$output,sha256:$sha256,durationSeconds:($duration|tonumber),intervals:$intervals,width:1080,height:1920,videoCodec:$videoCodec,audioCodec:$audioCodec}'
