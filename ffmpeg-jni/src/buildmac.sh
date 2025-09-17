#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(realpath "$SCRIPT_DIR/../..")"

if [[ -z "${JAVA_HOME:-}" ]]; then
    if command -v /usr/libexec/java_home >/dev/null 2>&1; then
        JAVA_HOME="$((/usr/libexec/java_home -v 17) 2>/dev/null || true)"
    fi
fi
if [[ -z "${JAVA_HOME:-}" || ! -d "$JAVA_HOME" ]]; then
    echo "ERROR: JAVA_HOME is not set or invalid. Please ensure JDK 17 is installed and JAVA_HOME is exported." >&2
    exit 1
fi
GCC="clang"
FFMPEG_SDK_DIR="$REPO_ROOT/ffmpeg-static/mac/ffmpeg-static-mac"

SRC_FILE="$SCRIPT_DIR/ffmpegjni.c"
OUT_DIR="$REPO_ROOT/ffmpeg-jni/bin/mac"
OUT_SO="$OUT_DIR/ffmpegjni.dylib"

mkdir -p "$OUT_DIR"

INCLUDE_FLAGS=(
    "-I$FFMPEG_SDK_DIR/include"
    "-I$FFMPEG_SDK_DIR/include/dav1d"
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
    "$FFMPEG_SDK_DIR/lib/libdav1d.a"
    -lpthread
    -lm
    -lz
    -liconv
    -lbz2
)

CFLAGS=(
    -O2
    -DNDEBUG
    -fPIC
    -arch x86_64
    # -mmacosx-version-min=10.13
)

LDFLAGS=(
    -dynamiclib
    -arch x86_64
    -Wl,-install_name,@rpath/ffmpegjni.dylib
)

echo "Building ffmpegjni (JNI bridge)"
echo "Using JDK:    $JAVA_HOME"
echo "Using FFmpeg: $FFMPEG_SDK_DIR"
echo "Using GCC:    $GCC"
echo "Output DYLIB: $OUT_SO"

$GCC "${CFLAGS[@]}" "${INCLUDE_FLAGS[@]}" -o "$OUT_SO" "${LDFLAGS[@]}" "$SRC_FILE" "${LIB_FLAGS[@]}"

echo "Build succeeded: $OUT_SO"
