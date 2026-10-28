param()
$ErrorActionPreference = 'Stop'
$support = [IO.Path]::GetFullPath($PSScriptRoot).TrimEnd('\')
$root = [IO.Path]::GetFullPath((Join-Path $support '..')).TrimEnd('\')
$archive = Join-Path $support 'platform.7z'
$expected = [IO.File]::ReadAllText((Join-Path $support 'platform.sha256')).Trim().ToLowerInvariant()
$readyFile = Join-Path $support '.platform-ready'
$ready = if (Test-Path -LiteralPath $readyFile) { [IO.File]::ReadAllText($readyFile).Trim().ToLowerInvariant() } else { '' }
if ($ready -ne $expected -or -not (Test-Path -LiteralPath (Join-Path $support 'runtime/bin/javac.exe')) -or -not (Test-Path -LiteralPath (Join-Path $support 'app/game-code.jar'))) {
    $inputStream = [IO.File]::OpenRead($archive)
    try { $actual = [BitConverter]::ToString([Security.Cryptography.SHA256]::Create().ComputeHash($inputStream)).Replace('-', '').ToLowerInvariant() }
    finally { $inputStream.Dispose() }
    if ($actual -ne $expected) { throw 'The bundled Java archive is incomplete or damaged.' }
    & (Join-Path $support '7zr.exe') x $archive ('-o' + $support) -y -bso0
    if ($LASTEXITCODE -ne 0) { throw 'Could not extract the bundled Java runtime.' }
    [IO.File]::WriteAllText($readyFile, $expected, [Text.Encoding]::ASCII)
}
& (Join-Path $support 'Resolve-root.ps1') -GameDirectory $root
$runRoot = [IO.File]::ReadAllText((Join-Path $support 'portable-root.txt')).Trim()
if (-not (Test-Path -LiteralPath (Join-Path $runRoot 'support/runtime/bin/javac.exe')) -or -not (Test-Path -LiteralPath (Join-Path $runRoot 'src/main/java/com/dodgingcat/Launcher.java'))) {
    throw 'Could not prepare the game path.'
}
