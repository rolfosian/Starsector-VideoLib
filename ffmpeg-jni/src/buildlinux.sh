#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(realpath "$SCRIPT_DIR/../..")"

if [[ -z "${JAVA_HOME:-}" ]]; then
    if [[ -d "/usr/lib/jvm" ]]; then
        JAVA_HOME="$(dirname "$(readlink -f /usr/bin/javac 2>/dev/null || command -v javac)" 2>/dev/null | sed 's@/bin$@@')"
    fi
fi
if [[ -z "${JAVA_HOME:-}" || ! -d "$JAVA_HOME" ]]; then
    echo "ERROR: JAVA_HOME is not set or invalid. Please ensure JDK 17 is installed and JAVA_HOME is exported." >&2
    exit 1
fi
GCC="gcc"
FFMPEG_SDK_DIR="$REPO_ROOT/ffmpeg-static/linux/ffmpeg-static-linux"

SRC_FILE="$SCRIPT_DIR/ffmpegjni.c"
OUT_DIR="$REPO_ROOT/ffmpeg-jni/bin/linux"
OUT_SO="$OUT_DIR/ffmpegjni.so"

mkdir -p "$OUT_DIR"

INCLUDE_FLAGS=(
    "-I$FFMPEG_SDK_DIR/include"
    "-I$FFMPEG_SDK_DIR/include/dav1d"
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
    "$FFMPEG_SDK_DIR/lib/libdav1d.a"
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
    -Wl,-Bsymbolic
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
