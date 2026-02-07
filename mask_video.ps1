# Usage: .\mask_video.ps1 <video> <image>
# Applies image alpha mask to video and outputs transparent WebM with power-of-2 dimensions in line with Starsector's runtime texture semantics and the ffmpeg-jni vflip implementation

if ($args.Count -le 1) {
    Write-Host "Usage: .\script.ps1 <video> <image>"
    exit 1
}

$video = $args[0]
$image = $args[1]
$output = "output_final.webm"

$frameCount = & ffprobe -v error -select_streams v:0 -show_entries stream=nb_frames -of default=noprint_wrappers=1:nokey=1 "$video"
$fps = & ffprobe -v error -select_streams v:0 -show_entries stream=r_frame_rate -of default=noprint_wrappers=1:nokey=1 "$video"

$width = & ffprobe -v error -select_streams v:0 -show_entries stream=width -of default=noprint_wrappers=1:nokey=1 "$image"
$height = & ffprobe -v error -select_streams v:0 -show_entries stream=height -of default=noprint_wrappers=1:nokey=1 "$image"
$w = [int]$width
$h = [int]$height

function NextPOT($n) {
    $pot = 1
    while ($pot -lt $n) { $pot *= 2 }
    return $pot
}

$potWidth = NextPOT $w
$potHeight = NextPOT $h
$yOffset = $potHeight - $h

& ffmpeg -y `
    -i "$video" `
    -loop 1 -framerate $fps -i "$image" `
    -filter_complex `
    "[0:v]scale=${w}:${h}[scaled_video]; `
     [1:v]format=rgba,alphaextract,format=gray[mask]; `
     [scaled_video][mask]alphamerge,pad=${potWidth}:${potHeight}:0:${yOffset}:color=black@0" `
    -c:v libvpx-vp9 `
    -pix_fmt yuva420p `
    -b:v 0 -crf 30 `
    -an `
    -frames:v $frameCount `
    -fps_mode cfr `
    "$output"