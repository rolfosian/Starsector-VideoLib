#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(realpath "$SCRIPT_DIR/../..")"

JAVA_HOME="$HOME/Downloads/New Folder/jdk-17.0.12/jdk-17.0.12"
CC="clang"  # use clang on macOS
FFMPEG_SDK_DIR="$REPO_ROOT/ffmpeg-static/mac"

SRC_FILE="$SCRIPT_DIR/ffmpegjni.c"
OUT_DIR="$REPO_ROOT/ffmpeg-jni/bin/mac"
OUT_SO="$OUT_DIR/ffmpegjni.dylib"

mkdir -p "$OUT_DIR"

INCLUDE_FLAGS=(
    "-I$FFMPEG_SDK_DIR/include"
    "-I$JAVA_HOME/include"
    "-I$JAVA_HOME/include/darwin"
)

LIB_FLAGS=(
    "-L$FFMPEG_SDK_DIR/lib"
    "$FFMPEG_SDK_DIR/lib/libswscale.a"
    "$FFMPEG_SDK_DIR/lib/libavformat.a"
    "$FFMPEG_SDK_DIR/lib/libavcodec.a"
    "$FFMPEG_SDK_DIR/lib/libavutil.a"
    "$FFMPEG_SDK_DIR/lib/libswresample.a"
    -lpthread
    -lm
    -lz
)

CFLAGS=(
    -O2
    -DNDEBUG
    -fPIC
)

LDFLAGS=(
    -dynamiclib
    -Wl,-undefined,error
)

echo "Building ffmpegjni (JNI bridge)"
echo "Using JDK:    $JAVA_HOME"
echo "Using FFmpeg: $FFMPEG_SDK_DIR"
echo "Using CC:     $CC"
echo "Output SO:    $OUT_SO"

$CC "${CFLAGS[@]}" "${INCLUDE_FLAGS[@]}" -o "$OUT_SO" "$SRC_FILE" "${LIB_FLAGS[@]}" "${LDFLAGS[@]}"

echo "Build succeeded: $OUT_SO"