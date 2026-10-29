param([ValidateSet('run', 'test', 'package')][string]$Task = 'run')
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

# Prefer a real JDK; some Oracle javapath launcher shims can hang.
$jdkCandidates = @()
if ($env:JAVA_HOME) { $jdkCandidates += $env:JAVA_HOME }
if (Test-Path -LiteralPath 'C:\Program Files\Java') {
    $jdkCandidates += Get-ChildItem -LiteralPath 'C:\Program Files\Java' -Directory | Sort-Object Name -Descending | Select-Object -ExpandProperty FullName
}
$javaCommand = Get-Command javac.exe -ErrorAction SilentlyContinue
if ($javaCommand -and $javaCommand.Source -notmatch 'javapath') {
    $jdkCandidates += Split-Path (Split-Path $javaCommand.Source -Parent) -Parent
}
$jdkPath = $jdkCandidates | Where-Object { Test-Path -LiteralPath (Join-Path $_ 'bin\javac.exe') } | Select-Object -First 1
if (-not $jdkPath) { throw 'Install JDK 25 (or JDK 23+) and set JAVA_HOME, then run again.' }
$env:JAVA_HOME = $jdkPath
$env:PATH = (Join-Path $jdkPath 'bin') + ';' + $env:PATH
$releaseText = Get-Content -LiteralPath (Join-Path $jdkPath 'release') -Raw
$jdkVersion = [regex]::Match($releaseText, 'JAVA_VERSION="(\d+)').Groups[1].Value
if (-not $jdkVersion -or [int]$jdkVersion -lt 23) { throw 'JavaFX 25 requires JDK 23 or later. Please install JDK 25.' }
$compatArgs = @()
if ([int]$jdkVersion -lt 25) {
    Write-Host "Using JDK $jdkVersion compatibility build. Team default in pom.xml is JDK 25." -ForegroundColor Yellow
    $compatArgs += "-Dmaven.compiler.release=$jdkVersion"
}
$mavenVersion = '3.9.11'
$localMaven = Join-Path $PSScriptRoot ".tools\apache-maven-$mavenVersion\bin\mvn.cmd"
$mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if (Test-Path -LiteralPath $localMaven) { $mavenPath = $localMaven }
elseif ($mavenCommand) { $mavenPath = $mavenCommand.Source }
else {
    New-Item -ItemType Directory -Path '.tools' -Force | Out-Null
    $archive = Join-Path $PSScriptRoot ".tools\apache-maven-$mavenVersion-bin.zip"
    $baseUrl = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$mavenVersion/apache-maven-$mavenVersion-bin.zip"
    Write-Host 'Downloading Apache Maven from Maven Central (first run only)...'
    Invoke-WebRequest -Uri $baseUrl -OutFile $archive -UseBasicParsing
    $expected = (Invoke-WebRequest -Uri "$baseUrl.sha512" -UseBasicParsing).Content.Trim().Split(' ')[0]
    $actual = (Get-FileHash -LiteralPath $archive -Algorithm SHA512).Hash
    if ($actual -ine $expected) { throw 'Maven download checksum does not match. Please retry.' }
    Expand-Archive -LiteralPath $archive -DestinationPath '.tools' -Force
    $mavenPath = $localMaven
}
$goals = switch ($Task) {
    'run' { @('compile', 'javafx:run') }
    'test' { @('test') }
    'package' { @('package') }
}
& $mavenPath '-Dmaven.repo.local=.tools/m2' @compatArgs @goals
exit $LASTEXITCODE
