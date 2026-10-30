param(
    [string]$OutputDirectory,
    [string]$MavenRepository = (Join-Path $env:USERPROFILE '.m2/repository')
)
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath($PSScriptRoot)
Set-Location -LiteralPath $projectRoot
if (-not $OutputDirectory) { $OutputDirectory = Join-Path $projectRoot ('deliverables/DodgingCat-direct-' + (Get-Date -Format 'yyyyMMdd-HHmmss')) }
$OutputDirectory = [IO.Path]::GetFullPath($OutputDirectory)
if (Test-Path -LiteralPath $OutputDirectory) { throw 'Choose a new output directory; existing deliveries are never overwritten.' }
$jdk = if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/jlink.exe'))) {
    $env:JAVA_HOME
} else {
    Split-Path (Split-Path (Get-Command javac.exe).Source -Parent) -Parent
}
function Native([string]$Executable, [string[]]$Arguments) {
    & $Executable @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Command failed ($LASTEXITCODE): $Executable" }
}
function StoreJar([string]$JarPath) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $stored = $JarPath + '.stored'
    $inputArchive = [IO.Compression.ZipFile]::OpenRead($JarPath)
    $outputArchive = [IO.Compression.ZipFile]::Open($stored, [IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach ($entry in $inputArchive.Entries) {
            $copy = $outputArchive.CreateEntry($entry.FullName, [IO.Compression.CompressionLevel]::NoCompression)
            $copy.LastWriteTime = $entry.LastWriteTime
            $copy.ExternalAttributes = $entry.ExternalAttributes
            $inputStream = $entry.Open()
            $outputStream = $copy.Open()
            try { $inputStream.CopyTo($outputStream) } finally { $inputStream.Dispose(); $outputStream.Dispose() }
        }
    } finally { $inputArchive.Dispose(); $outputArchive.Dispose() }
    Move-Item -LiteralPath $stored -Destination $JarPath -Force
}
Native (Get-Command mvn.cmd).Source @('-q', '-DskipTests', 'compile')
$stage = Join-Path $OutputDirectory 'build/DodgingCat'
$support = Join-Path $stage 'support'
$app = Join-Path $support 'app'
$fx = Join-Path $app 'fx'
New-Item -ItemType Directory -Path $fx -Force | Out-Null
foreach ($name in @('src', 'docs')) { Copy-Item -LiteralPath (Join-Path $projectRoot $name) -Destination $stage -Recurse }
foreach ($name in @('pom.xml', 'README.md', 'README_vi.md')) {
    Copy-Item -LiteralPath (Join-Path $projectRoot $name) -Destination $stage
}
$template = Join-Path $projectRoot 'tools/portable-direct'
foreach ($name in @('Run.cmd', 'Build.cmd')) { Copy-Item -LiteralPath (Join-Path $template $name) -Destination $stage }
foreach ($name in @('Compile.cmd', 'Prepare.ps1', 'Resolve-root.ps1', '7zr.exe')) {
    Copy-Item -LiteralPath (Join-Path $template $name) -Destination $support
}
Copy-Item -LiteralPath (Join-Path $template 'README-PORTABLE.txt') -Destination (Join-Path $stage 'README.txt')
Copy-Item -LiteralPath (Join-Path $template 'licenses') -Destination $support -Recurse
Copy-Item -LiteralPath (Join-Path $template 'THIRD-PARTY.txt') -Destination (Join-Path $support 'licenses/THIRD-PARTY.txt')
foreach ($module in @('base', 'graphics', 'media', 'swing')) {
    Copy-Item -LiteralPath (Join-Path $MavenRepository "org/openjfx/javafx-$module/25/javafx-$module-25-win.jar") -Destination $fx
}
Copy-Item -LiteralPath (Join-Path $MavenRepository 'com/google/code/gson/gson/2.13.2/gson-2.13.2.jar') -Destination (Join-Path $app 'gson.jar')
Copy-Item -LiteralPath (Join-Path $MavenRepository 'javazoom/jlayer/1.0.1/jlayer-1.0.1.jar') -Destination (Join-Path $app 'jlayer.jar')
Native (Join-Path $jdk 'bin/jar.exe') @('--create', '--file', (Join-Path $app 'game-code.jar'), '-C', (Join-Path $projectRoot 'target/classes'), 'com')
foreach ($jarFile in Get-ChildItem -LiteralPath $app -Filter '*.jar' -File -Recurse) { StoreJar $jarFile.FullName }
$runtime = Join-Path $support 'runtime'
Native (Join-Path $jdk 'bin/jlink.exe') @('--add-modules', 'java.desktop,java.logging,java.xml,jdk.unsupported,jdk.unsupported.desktop,jdk.crypto.ec,jdk.compiler,jdk.jartool', '--strip-debug', '--no-header-files', '--no-man-pages', '--compress', 'zip-0', '--output', $runtime)
Push-Location $support
try {
    Native (Join-Path $template '7zr.exe') @('a', '-t7z', (Join-Path $support 'platform.7z'), 'runtime', 'app', '-mx=9', '-m0=LZMA2', '-md=256m', '-mfb=273', '-ms=on', '-mmt=2', '-bsp0')
} finally { Pop-Location }
Native (Join-Path $template '7zr.exe') @('t', (Join-Path $support 'platform.7z'), '-bso0')
$platformHash = (Get-FileHash -LiteralPath (Join-Path $support 'platform.7z') -Algorithm SHA256).Hash.ToLowerInvariant()
[IO.File]::WriteAllText((Join-Path $support 'platform.sha256'), $platformHash, [Text.Encoding]::ASCII)
foreach ($generated in @($runtime, $app)) {
    $full = [IO.Path]::GetFullPath($generated)
    if (-not $full.StartsWith(([IO.Path]::GetFullPath($stage).TrimEnd('\') + '\'), [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe generated directory cleanup.' }
    if ((Get-ChildItem -LiteralPath $full -Recurse -Force | Where-Object { $_.Attributes -band [IO.FileAttributes]::ReparsePoint } | Select-Object -First 1)) { throw 'Unexpected symlink in generated directory.' }
    Remove-Item -LiteralPath $full -Recurse -Force
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = Join-Path $OutputDirectory 'DodgingCat-source-portable-Windows-x64.zip'
[IO.Compression.ZipFile]::CreateFromDirectory($stage, $zip, [IO.Compression.CompressionLevel]::Optimal, $true)
$bytes = (Get-Item -LiteralPath $zip).Length
$report = [ordered]@{
    created = (Get-Date).ToString('o')
    zip = $zip
    bytes = $bytes
    sha256 = (Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLowerInvariant()
    platformSha256 = $platformHash
    sourceAndOriginalAssetsVisibleImmediately = $true
    includesOfflineCompiler = $true
    requiresInstalledJavaMavenOrInternetToRun = $false
    below100MillionBytes = ($bytes -lt 100000000)
}
$report | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $OutputDirectory 'build-report.json') -Encoding utf8
$buildDirectory = [IO.Path]::GetFullPath((Join-Path $OutputDirectory 'build'))
if (-not $buildDirectory.StartsWith(($OutputDirectory.TrimEnd('\') + '\'), [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe build staging cleanup.' }
if ((Get-ChildItem -LiteralPath $buildDirectory -Recurse -Force | Where-Object { $_.Attributes -band [IO.FileAttributes]::ReparsePoint } | Select-Object -First 1)) { throw 'Unexpected symlink in build staging.' }
Remove-Item -LiteralPath $buildDirectory -Recurse -Force
$report | ConvertTo-Json
if ($bytes -ge 100000000) { throw 'ZIP exceeds the LMS limit; reduce package size before submitting.' }
