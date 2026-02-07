#!/bin/bash

# Usage: ./mask_video.sh <video> <image>
# Exits if less than 2 arguments are provided
# Applies image alpha mask to video and outputs transparent WebM with power-of-2 dimensions in line with Starsector's runtime texture semantics and the ffmpeg-jni vflip implementation

if [ "$#" -le 1 ]; then
    echo "Usage: $0 <video> <image>"
    exit 1
fi

video="$1"
image="$2"
output="output_final.webm"

frameCount=$(ffprobe -v error -select_streams v:0 -show_entries stream=nb_frames -of default=noprint_wrappers=1:nokey=1 "$video")
fps=$(ffprobe -v error -select_streams v:0 -show_entries stream=r_frame_rate -of default=noprint_wrappers=1:nokey=1 "$video")

width=$(ffprobe -v error -select_streams v:0 -show_entries stream=width -of default=noprint_wrappers=1:nokey=1 "$image")
height=$(ffprobe -v error -select_streams v:0 -show_entries stream=height -of default=noprint_wrappers=1:nokey=1 "$image")
w=$((width))
h=$((height))

next_pot() {
    n=$1
    pot=1
    while [ $pot -lt $n ]; do
        pot=$((pot * 2))
    done
    echo $pot
}

potWidth=$(next_pot $w)
potHeight=$(next_pot $h)
yOffset=$((potHeight - h))

ffmpeg -y \
    -i "$video" \
    -loop 1 -framerate "$fps" -i "$image" \
    -filter_complex "[0:v]scale=${w}:${h}[scaled_video]; \
[1:v]format=rgba,alphaextract,format=gray[mask]; \
[scaled_video][mask]alphamerge,pad=${potWidth}:${potHeight}:0:${yOffset}:color=black@0" \
    -c:v libvpx-vp9 \
    -pix_fmt yuva420p \
    -b:v 0 -crf 30 \
    -an \
    -frames:v "$frameCount" \
    -fps_mode cfr \
    "$output"