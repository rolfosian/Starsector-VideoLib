import subprocess
# This script only works with ffmpeg/ffprobe bin executables in path

input_file = "ufos.mp4" # This video has no alpha channel. We want to convert all black pixels of this video to transparent pixels and output a video with alpha channel
output_file = "ufos_transparent.webm"

import os
if os.path.exists(output_file):
    os.remove(output_file)

try:
    cmd_probe = [
        "ffprobe",
        "-v", "error",
        "-select_streams", "v:0",
        "-show_entries", "stream=bit_rate",
        "-of", "default=noprint_wrappers=1:nokey=1",
        input_file
    ]
    bitrate_str = subprocess.check_output(cmd_probe).decode().strip()

    if not bitrate_str or bitrate_str == "N/A":
        print("Bitrate not found, using a default of 2000k")
        bitrate = "2000k"
    else:
        bitrate_bps = int(bitrate_str)
        bitrate = f"{bitrate_bps // 1000}k"

except (subprocess.CalledProcessError, ValueError) as e:
    raise ValueError(f"Could not extract a valid bitrate from input file: {e}")

print(f"Encoding {output_file} at a target bitrate of {bitrate}...")

cmd_ffmpeg = [
    "ffmpeg",
    "-i", input_file,
    "-vf", "format=yuva420p,chromakey=black:0.03:0.0",
    "-pix_fmt", "yuva420p",
    "-c:v", "libvpx-vp9",
    "-auto-alt-ref", "0",
    "-an",
    "-b:v", bitrate,
    "-f", "webm",
    output_file
]

try:
    subprocess.run(cmd_ffmpeg, check=True, capture_output=True, text=True)
    print(f"Successfully encoded {output_file} at bitrate {bitrate}")

    print("Verifying output pixel format...")
    cmd_verify = [
        "ffprobe",
        "-v", "error",
        "-select_streams", "v:0",
        "-show_entries", "stream=pix_fmt",
        "-of", "default=noprint_wrappers=1:nokey=1",
        output_file
    ]
    pix_fmt_result = subprocess.check_output(cmd_verify).decode().strip()
    print(f"ffprobe reports pix_fmt={pix_fmt_result}", 
          "If this reports yuv420p it is only half correct. The alpha channel of yuva420p is in a separate video stream to the main stream.",
          "libvpx handles this internally, but because we are working at a lower level in the JNI code we have to merge the packets with 2 decoders before we can convert to RGBA OpenGL textures.")

except subprocess.CalledProcessError as e:
    print("--- FFMPEG FAILED ---")
    print("STDOUT:", e.stdout)
    print("STDERR:", e.stderr)