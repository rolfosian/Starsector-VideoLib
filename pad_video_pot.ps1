if ($args.Count -le 0) {
    Write-Host "Usage: .\script.ps1 <video>"
    exit 1
}

$video = $args[0]
$output = "output_final.webm"

$frameCount = & ffprobe -v error -select_streams v:0 -show_entries stream=nb_frames -of default=noprint_wrappers=1:nokey=1 "$video"

$width = & ffprobe -v error -select_streams v:0 -show_entries stream=width -of default=noprint_wrappers=1:nokey=1 "$video"
$height = & ffprobe -v error -select_streams v:0 -show_entries stream=height -of default=noprint_wrappers=1:nokey=1 "$video"
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

ffmpeg -y `
    -i "$video" `
    -filter_complex "[0:v]scale=${w}:${h},pad=${potWidth}:${potHeight}:0:${yOffset}:color=black@0" `
    -c:v libvpx-vp9 `
    -pix_fmt yuva420p `
    -b:v 0 -crf 30 `
    -an `
    -frames:v $frameCount `
    -fps_mode cfr `
    "$output"