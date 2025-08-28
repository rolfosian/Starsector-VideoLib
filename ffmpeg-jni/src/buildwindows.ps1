$repoRoot = Resolve-Path -Path (Join-Path $PSScriptRoot "..\..")
$repoRoot = $repoRoot.Path


$JavaHome = Join-Path $env:USERPROFILE 'Downloads\jdk-17.0.12_windows-x64_bin\jdk-17.0.12'
$Gcc = "gcc"
$FfmpegSdkDir = Join-Path $repoRoot 'ffmpeg-static\windows'

$ErrorActionPreference = 'Stop'

Write-Host "Building ffmpegjni.dll (JNI bridge)"

# Locate gcc if a path was not provided
try {
	$null = & $Gcc --version 2>$null
} catch {
	# Try common MSYS2 MinGW64 path fallback
	$gccFallback = 'C:\\Program Files (x86)\\msys2\\mingw64\\bin\\gcc.exe'
	if (Test-Path $gccFallback) {
		$Gcc = $gccFallback
	} else {
		throw "gcc not found. Install MSYS2 MinGW-w64 (x86_64) and ensure gcc is on PATH, or pass -Gcc."
	}
}

$srcDir = $PSScriptRoot
$srcFile = Join-Path $srcDir 'ffmpegjni.c'
if (-not (Test-Path $srcFile)) {
	throw "Source file not found: $srcFile"
}

# Output alongside the FFmpeg runtime DLLs so Java can load it easily
$outDir = Join-Path $repoRoot 'ffmpeg-jni\bin\windows'
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir | Out-Null }
$outDll = Join-Path $outDir 'ffmpegjni.dll'

$includeFlags = @(
	"-I$($FfmpegSdkDir)\include",
	"-I$JavaHome\include",
	"-I$JavaHome\include\win32"
)

$libFlags = @(
	"-L$($FfmpegSdkDir)\lib",
	"$($FfmpegSdkDir)\lib\libswresample.a",
	"$($FfmpegSdkDir)\lib\libswscale.a",
    "$($FfmpegSdkDir)\lib\libavformat.a",
    "$($FfmpegSdkDir)\lib\libavcodec.a",
    "$($FfmpegSdkDir)\lib\libavutil.a",
	"-lws2_32",
	"-lwinmm",
	"-lbcrypt",
	"-lcrypt32",
	"-lbz2",
	"-lz",
	"-lsecur32",
	"-lole32",
	"-luuid",
	"-lncrypt",
	"-liconv"
)

$cFlags = @(
	"-O2",
	"-DNDEBUG",
	"-DWIN32_LEAN_AND_MEAN",
	"-DUNICODE",
	"-D_UNICODE",
	"-fPIC"
)

$ldFlags = @(
	"-shared",
	"-static-libgcc"
)

Write-Host "Using JDK:    $JavaHome"
Write-Host "Using FFmpeg: $FfmpegSdkDir"
Write-Host "Using GCC:    $Gcc"
Write-Host "Output DLL:   $outDll"

# Build command
$args = @()
$args += $cFlags
$args += $includeFlags
$args += @("-o", $outDll)
$args += $ldFlags
$args += @($srcFile)
$args += $libFlags

Write-Host "Compiling..."
& $Gcc @args

if ($LASTEXITCODE -ne 0) {
	throw "Build failed (exit $LASTEXITCODE)."
}

Write-Host "Build succeeded: $outDll" -ForegroundColor Green
