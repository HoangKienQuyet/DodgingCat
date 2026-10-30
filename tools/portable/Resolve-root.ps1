param([string]$GameDirectory = (Join-Path $PSScriptRoot 'DodgingCat'))
$ErrorActionPreference='Stop'
$gameRoot=[IO.Path]::GetFullPath($GameDirectory).TrimEnd('\')
if(-not (Test-Path -LiteralPath (Join-Path $gameRoot 'runtime/bin/java.exe'))) { throw 'Portable runtime is missing. Extract the ZIP fully.' }
$runRoot=$gameRoot
if($gameRoot -match '[^\x00-\x7F]') {
    # JDK's Windows command launcher can fail to locate java.dll in non-ACP paths.
    # Keep native Java binaries in a physical ASCII path. Junctions alone are not
    # sufficient because native-library loading canonicalizes them back to Unicode.
    $sid=[Security.Principal.WindowsIdentity]::GetCurrent().User.Value
    $hash=[BitConverter]::ToString([Security.Cryptography.SHA256]::Create().ComputeHash([Text.Encoding]::UTF8.GetBytes($gameRoot))).Replace('-','').Substring(0,20)
    $resolved=$false
    foreach($base in @($env:LOCALAPPDATA,$env:PUBLIC)) {
        if(-not $base -or $base -match '[^\x00-\x7F]') { continue }
        try {
            $cache=Join-Path $base ('DodgingCatPortableRuntime\'+$sid)
            New-Item -ItemType Directory -Path $cache -Force | Out-Null
            $destination=Join-Path $cache $hash
            if(Test-Path -LiteralPath $destination) {
                $existing=Get-Item -LiteralPath $destination -Force
                if($existing.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Unexpected redirected cache directory' }
                if([IO.File]::ReadAllText((Join-Path $destination '.origin.txt')) -ne $gameRoot) { throw 'Cache belongs to another portable copy' }
            } else {
                New-Item -ItemType Directory -Path $destination | Out-Null
                [IO.File]::WriteAllText((Join-Path $destination '.origin.txt'),$gameRoot,[Text.UTF8Encoding]::new($false))
            }
            foreach($file in Get-ChildItem -LiteralPath $gameRoot -File -Recurse -Force) {
                $relative=$file.FullName.Substring($gameRoot.Length+1)
                if($relative -match '^(data|build)\\' -or $relative -in @('.images-ready','.audio-ready')) { continue }
                $copy=Join-Path $destination $relative
                if(-not (Test-Path -LiteralPath $copy) -or (Get-Item -LiteralPath $copy).Length -ne $file.Length -or (Get-Item -LiteralPath $copy).LastWriteTimeUtc -lt $file.LastWriteTimeUtc) {
                    New-Item -ItemType Directory -Path (Split-Path $copy -Parent) -Force | Out-Null
                    Copy-Item -LiteralPath $file.FullName -Destination $copy -Force
                }
            }
            $runRoot=$destination;$resolved=$true;break
        } catch { continue }
    }
    if(-not $resolved) { throw 'Cannot prepare an ASCII runtime cache. Extract the ZIP to a writable folder with an English/ASCII path.' }
}
[IO.File]::WriteAllText((Join-Path $runRoot 'profile-location.txt'),(Join-Path $gameRoot 'data/profile.json'),[Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText((Join-Path $PSScriptRoot 'portable-root.txt'),$runRoot,[Text.Encoding]::ASCII)
