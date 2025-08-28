## These are static ffmpeg libs compiled from the main ffmpeg repo on github using these configure commands

# Windows
`./configure --disable-everything --disable-doc --disable-programs --disable-debug --disable-ffplay --disable-ffprobe  --enable-gpl   --enable-static   --disable-shared   --prefix=/home/ffmpeg-static-windows   --enable-decoder=h264   --enable-decoder=hevc   --enable-decoder=vp8   --enable-decoder=vp9   --enable-decoder=mjpeg   --enable-decoder=png   --enable-decoder=webp   --enable-decoder=gif   --enable-parser=h264   --enable-parser=hevc   --enable-parser=vp8   --enable-parser=vp9   --enable-parser=mjpeg   --enable-parser=png   --enable-parser=webp   --enable-parser=gif   --enable-demuxer=matroska   --enable-demuxer=mov   --enable-demuxer=image2   --enable-demuxer=gif   --enable-protocol=file`

# Linux
`./configure --disable-everything --disable-doc --disable-programs --disable-debug --disable-ffplay --disable-ffprobe --enable-gpl   --enable-static   --disable-shared   --prefix=/home/ffmpeg-static-linux   --enable-decoder=h264   --enable-decoder=hevc   --enable-decoder=vp8   --enable-decoder=vp9   --enable-decoder=mjpeg   --enable-decoder=png   --enable-decoder=webp   --enable-decoder=gif   --enable-parser=h264   --enable-parser=hevc   --enable-parser=vp8   --enable-parser=vp9   --enable-parser=mjpeg   --enable-parser=png   --enable-parser=webp   --enable-parser=gif   --enable-demuxer=matroska   --enable-demuxer=mov   --enable-demuxer=image2   --enable-demuxer=gif  --enable-protocol=file --enable-pic --extra-cflags="-fPIC" --extra-cxxflags="-fPIC"`

# Mac TBD?
`./configure --disable-everything --disable-doc --disable-programs --disable-debug --disable-ffplay --disable-ffprobe --enable-gpl --enable-static --disable-shared --prefix=/home/ffmpeg-static-mac --enable-decoder=h264 --enable-decoder=hevc --enable-decoder=vp8 --enable-decoder=vp9 --enable-decoder=mjpeg --enable-decoder=png --enable-decoder=webp --enable-decoder=gif --enable-parser=h264 --enable-parser=hevc --enable-parser=vp8 --enable-parser=vp9 --enable-parser=mjpeg --enable-parser=png --enable-parser=webp --enable-parser=gif --enable-demuxer=matroska --enable-demuxer=mov --enable-demuxer=image2 --enable-demuxer=gif --enable-protocol=file --enable-pic --extra-cflags="-fPIC" --extra-cxxflags="-fPIC"`

# Codec support
- Video: h264, hevc, vp8, vp9
- Image: jpeg, png, webp, gif
