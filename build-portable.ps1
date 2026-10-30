param(
    [string]$OutputDirectory,
    [string]$MavenRepository = (Join-Path $env:USERPROFILE '.m2/repository'),
    [string]$PreencodedImages,
    [switch]$SkipCompile,
    [switch]$DemoOnly
)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
Set-Location -LiteralPath $projectRoot
if (-not $OutputDirectory) { $OutputDirectory = Join-Path $projectRoot ('deliverables/portable-' + (Get-Date -Format 'yyyyMMdd-HHmmss')) }
$OutputDirectory = [IO.Path]::GetFullPath($OutputDirectory)
if (Test-Path -LiteralPath $OutputDirectory) { throw 'Choose a new output directory; existing deliveries are never overwritten.' }
$jdkDirectory = if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin/jpackage.exe'))) { $env:JAVA_HOME } else { Split-Path (Split-Path (Get-Command javac.exe).Source -Parent) -Parent }
function Native([string]$Executable, [string[]]$Arguments) {
    & $Executable @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Command failed ($LASTEXITCODE): $Executable" }
}
function Fetch([string]$Url,[string]$Destination,[string]$Sha256) {
    if (-not (Test-Path -LiteralPath $Destination)) {
        New-Item -ItemType Directory -Path (Split-Path $Destination -Parent) -Force | Out-Null
        Invoke-WebRequest -Uri $Url -OutFile $Destination -UseBasicParsing
    }
    if ($Sha256 -and (Get-FileHash -LiteralPath $Destination -Algorithm SHA256).Hash.ToLowerInvariant() -ne $Sha256) { throw "Checksum mismatch: $Destination" }
}
function RelativeFile([string]$Directory,[string]$File) {
    $prefix=[IO.Path]::GetFullPath($Directory).TrimEnd('\')+'\'
    $full=[IO.Path]::GetFullPath($File)
    if(-not $full.StartsWith($prefix,[StringComparison]::OrdinalIgnoreCase)) { throw 'File outside expected source directory' }
    return $full.Substring($prefix.Length)
}
function StoreJar([string]$JarPath) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $stored=$JarPath+'.stored'
    $original=[IO.Compression.ZipFile]::OpenRead($JarPath)
    $output=[IO.Compression.ZipFile]::Open($stored,[IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach($entry in $original.Entries) {
            $copy=$output.CreateEntry($entry.FullName,[IO.Compression.CompressionLevel]::NoCompression)
            $copy.LastWriteTime=$entry.LastWriteTime;$copy.ExternalAttributes=$entry.ExternalAttributes
            $inputStream=$entry.Open();$outputStream=$copy.Open()
            try { $inputStream.CopyTo($outputStream) } finally { $inputStream.Dispose();$outputStream.Dispose() }
        }
    } finally { $original.Dispose();$output.Dispose() }
    Move-Item -LiteralPath $stored -Destination $JarPath -Force
}
if (-not $SkipCompile) { Native (Get-Command mvn.cmd).Source @('-q','-DskipTests','compile') }
$toolRoot = Join-Path $projectRoot 'target/portable-tools'
$sevenRoot=Join-Path $toolRoot '7zip'
Fetch 'https://github.com/ip7z/7zip/releases/download/26.05/7zr.exe' (Join-Path $sevenRoot '7zr.exe') '9bf582b894dfa665f852d7e9112b02290a1c303c2d95a21fcdc41ffcb03143b7'
Fetch 'https://github.com/ip7z/7zip/releases/download/26.05/7z2605-extra.7z' (Join-Path $sevenRoot 'extra.7z') 'f27a8f103c84785026edcba1f67faf1357225df9d19ddb0b7760d55947141bc7'
if (-not (Test-Path (Join-Path $sevenRoot 'extra/x64/7za.exe'))) { Native (Join-Path $sevenRoot '7zr.exe') @('x',(Join-Path $sevenRoot 'extra.7z'),('-o'+(Join-Path $sevenRoot 'extra')),'-y') }
$webpRoot=Join-Path $toolRoot 'webp'
Fetch 'https://storage.googleapis.com/downloads.webmproject.org/releases/webp/libwebp-1.6.0-windows-x64.zip' (Join-Path $webpRoot 'libwebp.zip') '48886f506b21f62e4661f0f4cbfca19800897c385128e8902542d29a950c93f1'
if (-not (Test-Path (Join-Path $webpRoot 'libwebp-1.6.0-windows-x64/bin/dwebp.exe'))) { Expand-Archive -LiteralPath (Join-Path $webpRoot 'libwebp.zip') -DestinationPath $webpRoot }
$cwebp=Join-Path $webpRoot 'libwebp-1.6.0-windows-x64/bin/cwebp.exe'
$dwebp=Join-Path $webpRoot 'libwebp-1.6.0-windows-x64/bin/dwebp.exe'
$wavRoot=Join-Path $toolRoot 'wavpack'
Fetch 'https://github.com/dbry/WavPack/releases/download/5.9.0/wavpack-5.9.0-x64.zip' (Join-Path $wavRoot 'tools.zip') 'ad5e94bcde6f4edfc859210d98f144f225805a391c633c68f6dbb15a9e52570e'
if(-not (Test-Path (Join-Path $wavRoot 'wvunpack.exe'))) { Expand-Archive -LiteralPath (Join-Path $wavRoot 'tools.zip') -DestinationPath $wavRoot }
$work=Join-Path $OutputDirectory 'build'
$inputDirectory=Join-Path $work 'input'
$fxDirectory=Join-Path $inputDirectory 'fx'
New-Item -ItemType Directory -Path $fxDirectory -Force | Out-Null
foreach ($module in @('base','graphics','media','swing')) {
    Copy-Item -LiteralPath (Join-Path $MavenRepository "org/openjfx/javafx-$module/25/javafx-$module-25-win.jar") -Destination $fxDirectory
}
Copy-Item -LiteralPath (Join-Path $MavenRepository 'com/google/code/gson/gson/2.13.2/gson-2.13.2.jar') -Destination (Join-Path $inputDirectory 'gson.jar')
Copy-Item -LiteralPath (Join-Path $MavenRepository 'javazoom/jlayer/1.0.1/jlayer-1.0.1.jar') -Destination (Join-Path $inputDirectory 'jlayer.jar')
Native (Join-Path $jdkDirectory 'bin/jar.exe') @('--create','--file',(Join-Path $inputDirectory 'game-code.jar'),'-C',(Join-Path $projectRoot 'target/classes'),'com')
foreach($jar in Get-ChildItem -LiteralPath $inputDirectory -Filter '*.jar' -File -Recurse) { StoreJar $jar.FullName }
$runtime=Join-Path $work 'runtime'
$modules=if($DemoOnly){'java.desktop,java.logging,java.xml,jdk.unsupported,jdk.unsupported.desktop,jdk.crypto.ec'}else{'java.desktop,java.logging,java.xml,jdk.unsupported,jdk.unsupported.desktop,jdk.crypto.ec,jdk.compiler,jdk.jartool'}
Native (Join-Path $jdkDirectory 'bin/jlink.exe') @('--add-modules',$modules,'--strip-debug','--no-header-files','--no-man-pages','--compress','zip-0','--output',$runtime)
Native (Join-Path $jdkDirectory 'bin/jpackage.exe') @('--type','app-image','--name','DodgingCat','--app-version','1.0.0','--vendor','Dodging Cat project group','--input',$inputDirectory,'--main-jar','game-code.jar','--main-class','com.dodgingcat.Launcher','--runtime-image',$runtime,'--dest',(Join-Path $work 'image'),'--java-options','--enable-native-access=javafx.graphics,javafx.media','--java-options','--module-path=$APPDIR/fx','--java-options','--add-modules=javafx.graphics,javafx.media,javafx.swing','--java-options','-Dfile.encoding=UTF-8','--java-options','-Ddodgingcat.profile=$ROOTDIR/data/profile.json')
$image=Join-Path $work 'image/DodgingCat'
$source=Join-Path $image 'source'
New-Item -ItemType Directory -Path $source -Force | Out-Null
foreach ($directory in @('src','docs','tools')) { Copy-Item -LiteralPath (Join-Path $projectRoot $directory) -Destination $source -Recurse }
foreach ($name in @('pom.xml','README.md','README_vi.md','dev.ps1','run.cmd','test.cmd','.gitignore','build-portable.ps1')) { Copy-Item -LiteralPath (Join-Path $projectRoot $name) -Destination $source }
$cfgPath=Join-Path $image 'app/DodgingCat.cfg'
$cfg=Get-Content -LiteralPath $cfgPath -Raw
$cfg=$cfg -replace '(?m)^app.classpath=\$APPDIR\\fx\\[^\r\n]*\r?\n',''
$cfg=$cfg.Replace('[JavaOptions]','app.classpath=$APPDIR\..\source\src\main\resources'+"`r`n`r`n"+'[JavaOptions]')
[IO.File]::WriteAllText($cfgPath,$cfg,[Text.UTF8Encoding]::new($false))
$packed=Join-Path $image 'packed-images'
New-Item -ItemType Directory -Path $packed,(Join-Path $image 'tools') -Force | Out-Null
Copy-Item -LiteralPath $dwebp -Destination (Join-Path $image 'tools/dwebp.exe')
Copy-Item -LiteralPath (Join-Path $wavRoot 'wvunpack.exe') -Destination (Join-Path $image 'tools/wvunpack.exe')
$audio=Join-Path $source 'src/main/resources/assets/audio'
$packedAudio=Join-Path $image 'packed-audio'
New-Item -ItemType Directory -Path $packedAudio -Force | Out-Null
foreach($wav in Get-ChildItem -LiteralPath $audio -Filter '*.wav' -File -Recurse) {
    $relative=RelativeFile $audio $wav.FullName;$encoded=Join-Path $packedAudio ($relative+'.wv')
    New-Item -ItemType Directory -Path (Split-Path $encoded -Parent) -Force | Out-Null
    Native (Join-Path $wavRoot 'wavpack.exe') @('-hh','-x3','-m','-v','-q','-y',$wav.FullName,'-o',$encoded)
}
Native (Join-Path $jdkDirectory 'bin/java.exe') @('-cp',(Join-Path $image 'app/game-code.jar'),'com.dodgingcat.app.PortableAssets','--audio-manifest',$audio,$packedAudio)
foreach($wav in Get-ChildItem -LiteralPath $audio -Filter '*.wav' -File -Recurse) { Remove-Item -LiteralPath $wav.FullName }
$images=Join-Path $source 'src/main/resources/assets/images'
foreach ($png in Get-ChildItem -LiteralPath $images -Filter '*.png' -File -Recurse) {
    $relative=RelativeFile $images $png.FullName
    $encoded=Join-Path $packed ($relative+'.webp')
    New-Item -ItemType Directory -Path (Split-Path $encoded -Parent) -Force | Out-Null
    # Indexed PNGs (including TinyPNG output) usually beat a truecolor WebP.
    # Keep the user's bytes directly in this case; never quantize or resize here.
    if([IO.File]::ReadAllBytes($png.FullName)[25] -eq 3) {
        Copy-Item -LiteralPath $png.FullName -Destination (Join-Path $packed ($relative+'.original.png'))
        continue
    }
    $cacheRoot=if($PreencodedImages){$PreencodedImages}else{Join-Path $projectRoot 'target/portable-image-cache'}
    $cached=Join-Path $cacheRoot ($relative+'.webp')
    $hashFile=$cached+'.sha256'
    $sourceHash=(Get-FileHash -LiteralPath $png.FullName -Algorithm SHA256).Hash
    if((Test-Path -LiteralPath $cached) -and (Test-Path -LiteralPath $hashFile) -and ([IO.File]::ReadAllText($hashFile) -eq $sourceHash)) { Copy-Item -LiteralPath $cached -Destination $encoded }
    else {
        Native $cwebp @('-z','6','-exact','-quiet',$png.FullName,'-o',$encoded)
        New-Item -ItemType Directory -Path (Split-Path $cached -Parent) -Force | Out-Null
        Copy-Item -LiteralPath $encoded -Destination $cached -Force
        [IO.File]::WriteAllText($hashFile,$sourceHash)
    }
    if((Get-Item -LiteralPath $encoded).Length -gt $png.Length) {
        Copy-Item -LiteralPath $png.FullName -Destination (Join-Path $packed ($relative+'.original.png'))
        Remove-Item -LiteralPath $encoded
    }
}
Native (Join-Path $jdkDirectory 'bin/java.exe') @('-Xmx512m','-cp',(Join-Path $image 'app/game-code.jar'),'com.dodgingcat.app.PortableAssets','--manifest',$images,$packed)
# Delete only staged image files. Original project art is never rewritten here.
foreach ($png in Get-ChildItem -LiteralPath $images -Filter '*.png' -File -Recurse) { Remove-Item -LiteralPath $png.FullName }
Native (Join-Path $image 'runtime/bin/java.exe') @('-Xmx512m','-Djava.awt.headless=true','-cp',(Join-Path $image 'app/game-code.jar'),'com.dodgingcat.app.PortableAssets',$image)
# First-launch reconstruction was checked against the original pixel hashes.
foreach ($png in Get-ChildItem -LiteralPath $images -Filter '*.png' -File -Recurse) { Remove-Item -LiteralPath $png.FullName }
Remove-Item -LiteralPath (Join-Path $image '.images-ready')
foreach($wav in Get-ChildItem -LiteralPath $audio -Filter '*.wav' -File -Recurse) { Remove-Item -LiteralPath $wav.FullName }
Remove-Item -LiteralPath (Join-Path $image '.audio-ready')
if (Test-Path (Join-Path $image 'prepare-assets.log')) { Remove-Item -LiteralPath (Join-Path $image 'prepare-assets.log') }
$template=Join-Path $projectRoot 'tools/portable'
if(-not $DemoOnly){
    Copy-Item -LiteralPath (Join-Path $template 'Compile.cmd') -Destination $image
    $compilePath=Join-Path $image 'Compile.cmd'
    [IO.File]::WriteAllText($compilePath,[IO.File]::ReadAllText($compilePath).Replace("`r`n","`n").Replace("`n","`r`n"),[Text.UTF8Encoding]::new($false))
}
$licenses=Join-Path $image 'licenses'
New-Item -ItemType Directory -Path $licenses -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $wavRoot 'license.txt') -Destination (Join-Path $licenses 'WavPack-license.txt')
foreach($name in @('LICENSE','ADDITIONAL_LICENSE_INFO','ASSEMBLY_EXCEPTION','modules/javafx.graphics/src/main/legal/jpeg_fx.md','modules/javafx.graphics/src/main/legal/mesa3d.md','modules/javafx.graphics/src/main/legal/pipewire.md','modules/javafx.media/src/main/legal/directshow.md','modules/javafx.media/src/main/legal/glib.md','modules/javafx.media/src/main/legal/gstreamer.md','modules/javafx.media/src/main/legal/libffi.md')) {
    $cached=Join-Path $toolRoot ('portable-licenses/javafx/'+$name)
    Fetch ('https://raw.githubusercontent.com/openjdk/jfx/jfx25/'+$name) $cached ''
    $destination=Join-Path $licenses ('javafx/'+$name);New-Item -ItemType Directory -Path (Split-Path $destination -Parent) -Force | Out-Null
    Copy-Item -LiteralPath $cached -Destination $destination
}
foreach($name in @('COPYING','PATENTS','AUTHORS')) {
    $cached=Join-Path $toolRoot ('portable-licenses/webp/'+$name)
    Fetch ('https://raw.githubusercontent.com/webmproject/libwebp/v1.6.0/'+$name) $cached ''
    New-Item -ItemType Directory -Path (Join-Path $licenses 'webp') -Force | Out-Null
    Copy-Item -LiteralPath $cached -Destination (Join-Path $licenses 'webp')
}
$gsonLicense=Join-Path $toolRoot 'portable-licenses/gson-LICENSE'
Fetch 'https://raw.githubusercontent.com/google/gson/gson-parent-2.13.2/LICENSE' $gsonLicense ''
Copy-Item -LiteralPath $gsonLicense -Destination $licenses
$jlayerSources=Join-Path $toolRoot 'portable-licenses/jlayer-1.0.1-sources.jar'
Fetch 'https://repo.maven.apache.org/maven2/javazoom/jlayer/1.0.1/jlayer-1.0.1-sources.jar' $jlayerSources ''
Copy-Item -LiteralPath $jlayerSources -Destination $licenses
Copy-Item -LiteralPath (Join-Path $template 'THIRD-PARTY.txt') -Destination $image
$bundle=Join-Path $OutputDirectory 'DodgingCat-Windows-x64'
New-Item -ItemType Directory -Path $bundle -Force | Out-Null
$payload=Join-Path $bundle 'game.7z'
Native (Join-Path $sevenRoot 'extra/x64/7za.exe') @('a','-t7z',$payload,$image,'-mx=9','-m0=LZMA2','-md=256m','-mfb=273','-ms=on','-mmt=2','-bsp0')
$payloadHash=(Get-FileHash -LiteralPath $payload -Algorithm SHA256).Hash.ToLowerInvariant()
$bundleFiles=if($DemoOnly){@('Run.cmd','Open-source.cmd','Troubleshoot.cmd','Resolve-root.ps1','Sync-assets.ps1')}else{@('Run.cmd','Build.cmd','Open-source.cmd','README.txt','Troubleshoot.cmd','Resolve-root.ps1','Sync-assets.ps1')}
foreach($name in $bundleFiles) { Copy-Item -LiteralPath (Join-Path $template $name) -Destination $bundle }
if($DemoOnly){Copy-Item -LiteralPath (Join-Path $template 'README-demo.txt') -Destination (Join-Path $bundle 'README.txt')}
$prepare=(Get-Content -LiteralPath (Join-Path $template 'Prepare.cmd') -Raw).Replace('@PAYLOAD_SHA256@',$payloadHash)
[IO.File]::WriteAllText((Join-Path $bundle 'Prepare.cmd'),$prepare,[Text.UTF8Encoding]::new($false))
Copy-Item -LiteralPath (Join-Path $sevenRoot '7zr.exe') -Destination $bundle
$commands=@(Get-ChildItem -LiteralPath $bundle -Filter '*.cmd' -File)
if(-not $DemoOnly){$commands+=Get-Item -LiteralPath (Join-Path $image 'Compile.cmd')}
foreach($cmd in $commands) {
    $text=[IO.File]::ReadAllText($cmd.FullName).Replace("`r`n","`n").Replace("`n","`r`n")
    [IO.File]::WriteAllText($cmd.FullName,$text,[Text.UTF8Encoding]::new($false))
}
$zip=Join-Path $OutputDirectory 'DodgingCat-Windows-x64-portable.zip'
Add-Type -AssemblyName System.IO.Compression.FileSystem
[IO.Compression.ZipFile]::CreateFromDirectory($bundle,$zip,[IO.Compression.CompressionLevel]::NoCompression,$true)
$bytes=(Get-Item -LiteralPath $zip).Length
$included=@('source and tests','all active assets, losslessly packed','native launcher','Java 25 runtime','JavaFX native dependencies','Gson','offline first-run decoder','third-party notices')
if(-not $DemoOnly){$included+=@('javac and jar','offline Build.cmd')}
$report=[ordered]@{created=(Get-Date).ToString('o');platform='Windows x64';edition=if($DemoOnly){'demo'}else{'buildable'};bytes=$bytes;MiB=[math]::Round($bytes/1MB,2);payloadSha256=$payloadHash;zipSha256=(Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLowerInvariant();includes=$included;requiresInstalledJava=$false;requiresMavenToRun=$false;requiresNetworkToRun=$false;originalPixelsVerified=$true;originalImagesVerifiedARGB=@(Get-Content -LiteralPath (Join-Path $packed 'images.tsv')).Count;originalWavsVerifiedByteForByte=@(Get-Content -LiteralPath (Join-Path $packedAudio 'audio.tsv')).Count;includesCompiler=(-not $DemoOnly);under100MillionBytes=($bytes -lt 100000000)}
$report | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $OutputDirectory 'build-report.json') -Encoding utf8
$report | ConvertTo-Json -Depth 4
if($bytes -ge 100000000) { throw 'Portable ZIP exceeds the conservative LMS limit; do not submit this archive.' }
