#!/usr/bin/env bash
set -euo pipefail

# Repo root relative to this script location
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(realpath "$SCRIPT_DIR/../..")"

# Adjust paths as needed
JAVA_HOME="$HOME/Downloads/New Folder/jdk-17.0.12/jdk-17.0.12"
GCC="gcc"
FFMPEG_SDK_DIR="$REPO_ROOT/ffmpeg-static/linux"

SRC_FILE="$SCRIPT_DIR/ffmpegjni.c"
OUT_DIR="$REPO_ROOT/ffmpeg-jni/bin/linux"
OUT_SO="$OUT_DIR/ffmpegjni.so"

mkdir -p "$OUT_DIR"

INCLUDE_FLAGS=(
    "-I$FFMPEG_SDK_DIR/include"
    "-I$JAVA_HOME/include"
    "-I$JAVA_HOME/include/linux"
)

LIB_FLAGS=(
    "-L$FFMPEG_SDK_DIR/lib"
    "$FFMPEG_SDK_DIR/lib/libswscale.a"
    "$FFMPEG_SDK_DIR/lib/libavformat.a"
    "$FFMPEG_SDK_DIR/lib/libavcodec.a"
    "$FFMPEG_SDK_DIR/lib/libavutil.a"
    "$FFMPEG_SDK_DIR/lib/libswresample.a"
    -lpthread
    -ldl
    -lm
    -lz
)

CFLAGS=(
    -O2
    -DNDEBUG
    -fPIC
)

LDFLAGS=(
    -Wl
    -Bsymbolic
    -shared
    -static-libgcc
)

echo "Building ffmpegjni (JNI bridge)"
echo "Using JDK:    $JAVA_HOME"
echo "Using FFmpeg: $FFMPEG_SDK_DIR"
echo "Using GCC:    $GCC"
echo "Output SO:    $OUT_SO"

$GCC "${CFLAGS[@]}" "${INCLUDE_FLAGS[@]}" -o "$OUT_SO" "${LDFLAGS[@]}" "$SRC_FILE" "${LIB_FLAGS[@]}"

echo "Build succeeded: $OUT_SO"
