$ErrorActionPreference='Stop'
$original=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'DodgingCat'))
$running=[IO.File]::ReadAllText((Join-Path $PSScriptRoot 'portable-root.txt')).Trim()
if($running -eq $original) { exit 0 }
if([IO.File]::ReadAllText((Join-Path $running '.origin.txt')) -ne $original) { throw 'Invalid portable cache binding' }
$from=Join-Path $running 'source/src/main/resources/assets'
$to=Join-Path $original 'source/src/main/resources/assets'
foreach($file in Get-ChildItem -LiteralPath $from -File -Recurse) {
    if($file.Extension -notin @('.png','.wav')) { continue }
    $copy=Join-Path $to $file.FullName.Substring($from.Length+1)
    if(-not (Test-Path -LiteralPath $copy)) {
        New-Item -ItemType Directory -Path (Split-Path $copy -Parent) -Force | Out-Null
        Copy-Item -LiteralPath $file.FullName -Destination $copy
    }
}
