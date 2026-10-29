param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
if (-not $SkipBuild) {
    & (Join-Path $PSScriptRoot 'dev.ps1') -Task package
    if ($LASTEXITCODE -ne 0) { throw 'Build or tests failed.' }
}
# dev.ps1 resolves JAVA_HOME in its own script scope/environment.
$jlinkPath = Join-Path $env:JAVA_HOME 'bin\jlink.exe'
if (-not (Test-Path -LiteralPath $jlinkPath)) { throw 'The selected JDK does not include jlink.' }
$destination = Join-Path $PSScriptRoot 'dist\DodgingCat'
if (Test-Path -LiteralPath $destination) {
    throw 'dist\DodgingCat already exists. Rename it before packaging a new build.'
}
New-Item -ItemType Directory -Path (Join-Path $destination 'lib') -Force | Out-Null
Copy-Item -LiteralPath 'target\dodging-cat-1.0.0.jar' -Destination (Join-Path $destination 'dodging-cat.jar')
Get-ChildItem -LiteralPath 'target\lib' -File | Where-Object { $_.Name -like '*-win.jar' -or $_.Name -like 'gson-*.jar' } | Copy-Item -Destination (Join-Path $destination 'lib')
& $jlinkPath --add-modules java.base,java.desktop,java.logging,java.xml,jdk.unsupported,jdk.unsupported.desktop --strip-debug --no-header-files --no-man-pages --compress=zip-6 --output (Join-Path $destination 'runtime')
if ($LASTEXITCODE -ne 0) { throw 'jlink could not create the bundled Java runtime.' }
Copy-Item -LiteralPath 'distribution\Play.cmd' -Destination $destination
Copy-Item -LiteralPath 'distribution\HUONG_DAN.txt' -Destination $destination
Copy-Item -LiteralPath 'README.md' -Destination $destination
Copy-Item -LiteralPath 'docs\THIRD_PARTY.md' -Destination $destination
Copy-Item -LiteralPath 'docs' -Destination $destination -Recurse
Copy-Item -LiteralPath (Join-Path $env:JAVA_HOME 'LICENSE') -Destination (Join-Path $destination 'runtime\LICENSE')
Compress-Archive -LiteralPath $destination -DestinationPath 'dist\DodgingCat-Windows-x64.zip' -Force
Write-Host 'Ready: dist\DodgingCat-Windows-x64.zip' -ForegroundColor Green
