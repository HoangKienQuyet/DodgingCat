param([ValidateSet('run', 'test', 'package')][string]$Task = 'run')
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

# 1. Locate JDK 25 or 23+
$jdkCandidates = @()
if ($env:JAVA_HOME) { $jdkCandidates += $env:JAVA_HOME }
if (Test-Path -LiteralPath 'C:\Program Files\Eclipse Adoptium') {
    $jdkCandidates += Get-ChildItem -LiteralPath 'C:\Program Files\Eclipse Adoptium' -Directory | Sort-Object Name -Descending | Select-Object -ExpandProperty FullName
}
if (Test-Path -LiteralPath 'C:\Program Files\Java') {
    $jdkCandidates += Get-ChildItem -LiteralPath 'C:\Program Files\Java' -Directory | Sort-Object Name -Descending | Select-Object -ExpandProperty FullName
}
$javaCommand = Get-Command javac.exe -ErrorAction SilentlyContinue
if ($javaCommand -and $javaCommand.Source -notmatch 'javapath') {
    $jdkCandidates += Split-Path (Split-Path $javaCommand.Source -Parent) -Parent
}
$jdkPath = $jdkCandidates | Where-Object { Test-Path -LiteralPath (Join-Path $_ 'bin\javac.exe') } | Select-Object -First 1
if (-not $jdkPath) { throw 'JDK 25 (or JDK 23+) not found. Please install JDK 25 and set JAVA_HOME.' }
$env:JAVA_HOME = $jdkPath
$env:PATH = (Join-Path $jdkPath 'bin') + ';' + $env:PATH

Write-Host "Java Development Kit: $jdkPath" -ForegroundColor Cyan
Write-Host "Windows Graphics Settings app: $(Join-Path $jdkPath 'bin\java.exe')" -ForegroundColor Yellow

# 2. Locate or download Apache Maven
$mavenPath = $null
$mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if (-not $mavenCommand) {
    $mavenCommand = Get-Command mvn -ErrorAction SilentlyContinue
}

$mavenSearchRoots = @(
    'C:\Program Files\Apache\maven',
    'C:\Program Files\apache-maven',
    'C:\ProgramData\chocolatey\lib\maven\apache-maven',
    'C:\tools\apache-maven'
)
foreach ($root in $mavenSearchRoots) {
    if (Test-Path -LiteralPath $root) {
        $found = Get-ChildItem -LiteralPath $root -Filter "mvn.cmd" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) { $mavenPath = $found.FullName; break }
    }
}

$localToolsDir = Join-Path $PSScriptRoot '.tools'
$mavenVersion = '3.9.11'
$localMaven = Join-Path $localToolsDir "apache-maven-$mavenVersion\bin\mvn.cmd"

if ($mavenCommand) {
    $mavenPath = $mavenCommand.Source
} elseif (-not $mavenPath -and (Test-Path -LiteralPath $localMaven)) {
    $mavenPath = $localMaven
} elseif (-not $mavenPath) {
    if (-not (Test-Path -LiteralPath $localToolsDir)) {
        New-Item -ItemType Directory -Path $localToolsDir -Force | Out-Null
    }
    $archive = Join-Path $localToolsDir "apache-maven-$mavenVersion-bin.zip"
    $baseUrl = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$mavenVersion/apache-maven-$mavenVersion-bin.zip"
    Write-Host "Downloading Apache Maven $mavenVersion from Maven Central..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri $baseUrl -OutFile $archive -UseBasicParsing
    Expand-Archive -LiteralPath $archive -DestinationPath $localToolsDir -Force
    $mavenPath = $localMaven
}

$goals = switch ($Task) {
    'run' { @('compile', 'javafx:run') }
    'test' { @('test') }
    'package' { @('package') }
}

Write-Host "Executing Maven $Task ($($goals -join ' '))..." -ForegroundColor Green
& $mavenPath $goals
exit $LASTEXITCODE
