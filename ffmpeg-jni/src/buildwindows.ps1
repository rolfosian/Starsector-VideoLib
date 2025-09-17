$repoRoot = Resolve-Path -Path (Join-Path $PSScriptRoot "..\..")
$repoRoot = $repoRoot.Path

$JavaHome = $env:JAVA_HOME
if (-not $JavaHome -or -not (Test-Path $JavaHome)) {
    throw "JAVA_HOME is not set or invalid. Ensure JDK 17 is installed and JAVA_HOME is exported."
}
$Gcc = "gcc"
$FfmpegSdkDir = Join-Path $repoRoot 'ffmpeg-static\windows\ffmpeg-static-windows'

$ErrorActionPreference = 'Stop'

Write-Host "Building ffmpegjni.dll (JNI bridge)"

$srcDir = $PSScriptRoot
$srcFile = Join-Path $srcDir 'ffmpegjni.c'
if (-not (Test-Path $srcFile)) {
	throw "Source file not found: $srcFile"
}

$outDir = Join-Path $repoRoot 'ffmpeg-jni\bin\windows'
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir | Out-Null }
$outDll = Join-Path $outDir 'ffmpegjni.dll'

$includeFlags = @(
	"-I$($FfmpegSdkDir)\include",
	"-I$JavaHome\include",
	"-I$JavaHome\include\win32",
	"-IC:\msys64\mingw64\include"
)

$libFlags = @(
    "-L$($FfmpegSdkDir)\lib",
    "$($FfmpegSdkDir)\lib\libswresample.a",
    "$($FfmpegSdkDir)\lib\libswscale.a",
    "$($FfmpegSdkDir)\lib\libavformat.a",
    "$($FfmpegSdkDir)\lib\libavcodec.a",
    "$($FfmpegSdkDir)\lib\libavutil.a",
	"-LC:\msys64\mingw64\lib",
	"C:\msys64\mingw64\lib\libdav1d.a",
	"C:\msys64\mingw64\lib\libbz2.a",
	"-lws2_32",
	"-lwinmm",
	"-lbcrypt",
	"-lcrypt32",
	"-lz",
	"-lsecur32",
	"-lole32",
	"-luuid",
	"-lncrypt",
	"-liconv",
	"-lpthread"
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
