param(
    [switch]$Offline,
    [switch]$Build
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$taskTools = $PSScriptRoot
$taskDownloads = Join-Path $taskTools 'downloads'
$taskSdk = Join-Path $taskTools 'android-sdk'
$taskJava = 'C:\Program Files\Java\jdk-21.0.12.1'
if (-not (Test-Path -LiteralPath (Join-Path $taskJava 'bin\java.exe'))) {
    if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
        $taskJava = $env:JAVA_HOME
    } else {
        throw 'Install JDK 17 or newer, or set JAVA_HOME, before running this script.'
    }
}
New-Item -ItemType Directory -Path $taskDownloads -Force | Out-Null
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

function Get-Download {
    param([string]$Url, [string]$Path)
    if (Test-Path -LiteralPath $Path) { return }
    if ($Offline) { throw "Offline archive or metadata missing: $Path" }
    Write-Output "Downloading $Url"
    $partial = $Path + '.part'
    Invoke-WebRequest -Uri $Url -OutFile $partial -UseBasicParsing -TimeoutSec 600
    Move-Item -LiteralPath $partial -Destination $Path -Force
}

function Get-VerifiedArchive {
    param([string]$Url, [string]$Path, [string]$Algorithm, [string]$Checksum, [long]$Size = 0)
    Get-Download -Url $Url -Path $Path
    $validLength = $Size -eq 0 -or (Get-Item -LiteralPath $Path).Length -eq $Size
    $hash = (Get-FileHash -LiteralPath $Path -Algorithm $Algorithm).Hash
    if (-not $validLength -or $hash -ne $Checksum.Trim()) {
        throw "Archive checksum or size mismatch: $Path. Replace it with a complete official download."
    }
    Write-Output "Verified $([IO.Path]::GetFileName($Path)) ($Algorithm)"
}

function Install-Archive {
    param([string]$Archive, [string]$Destination, [string[]]$Required)
    $staging = Join-Path $taskTools ('extract-' + [Guid]::NewGuid().ToString('N'))
    Expand-Archive -LiteralPath $Archive -DestinationPath $staging
    $source = @(Get-ChildItem -LiteralPath $staging -Directory | Where-Object {
        Test-Path -LiteralPath (Join-Path $_.FullName $Required[0])
    })
    if ($source.Count -ne 1) { throw "Unexpected archive layout: $Archive" }
    foreach ($file in $Required) {
        if (-not (Test-Path -LiteralPath (Join-Path $source[0].FullName $file))) {
            throw "Incomplete archive: $Archive, missing $file"
        }
    }
    if (Test-Path -LiteralPath $Destination) {
        $backup = $Destination + '.before-' + [Guid]::NewGuid().ToString('N')
        Move-Item -LiteralPath $Destination -Destination $backup
    }
    New-Item -ItemType Directory -Path (Split-Path -Parent $Destination) -Force | Out-Null
    Move-Item -LiteralPath $source[0].FullName -Destination $Destination
    # Only remove the empty extraction directory created by this invocation.
    if (-not (Get-ChildItem -LiteralPath $staging -Force)) {
        Remove-Item -LiteralPath $staging
    }
}

function Test-Installed {
    param([string]$Directory, [string[]]$Required)
    foreach ($file in $Required) {
        if (-not (Test-Path -LiteralPath (Join-Path $Directory $file))) { return $false }
    }
    return $true
}

$gradleRequired = @('bin\gradle.bat', 'lib')
$gradleDirectory = Join-Path $taskTools 'gradle-9.1.0'
if (-not (Test-Installed -Directory $gradleDirectory -Required $gradleRequired)) {
    $gradleZip = Join-Path $taskDownloads 'gradle-9.1.0-bin.zip'
    $gradleChecksum = $gradleZip + '.sha256'
    Get-Download -Url 'https://services.gradle.org/distributions/gradle-9.1.0-bin.zip.sha256' -Path $gradleChecksum
    $expected = [IO.File]::ReadAllText($gradleChecksum).Trim()
    if ($expected -notmatch '^[0-9a-fA-F]{64}$') { throw 'Invalid Gradle SHA-256 metadata.' }
    Get-VerifiedArchive -Url 'https://services.gradle.org/distributions/gradle-9.1.0-bin.zip' -Path $gradleZip -Algorithm SHA256 -Checksum $expected
    Install-Archive -Archive $gradleZip -Destination $gradleDirectory -Required $gradleRequired
}

$platformRequired = @('android.jar', 'core-for-system-modules.jar', 'source.properties')
$buildRequired = @('aapt2.exe', 'd8.bat', 'zipalign.exe', 'apksigner.bat', 'lib\d8.jar', 'lib\apksigner.jar', 'core-lambda-stubs.jar', 'source.properties')
$packages = @(
    @{ Id = 'platforms;android-36'; Destination = (Join-Path $taskSdk 'platforms\android-36'); Required = $platformRequired },
    @{ Id = 'build-tools;36.0.0'; Destination = (Join-Path $taskSdk 'build-tools\36.0.0'); Required = $buildRequired }
)
$missingPackages = @($packages | Where-Object { -not (Test-Installed -Directory $_.Destination -Required $_.Required) })
if ($missingPackages.Count -gt 0) {
    $metadata = Join-Path $taskDownloads 'repository2-3.xml'
    Get-Download -Url 'https://dl.google.com/android/repository/repository2-3.xml' -Path $metadata
    $xml = New-Object Xml.XmlDocument
    $xml.XmlResolver = $null
    $xml.Load($metadata)
    foreach ($package in $missingPackages) {
        $nodes = @($xml.SelectNodes("//*[local-name()='remotePackage']") | Where-Object {
            $_.GetAttribute('path') -eq $package.Id
        })
        if ($nodes.Count -ne 1) { throw "Cannot find unambiguous SDK metadata for $($package.Id)." }
        $archive = @($nodes[0].SelectNodes("./*[local-name()='archives']/*[local-name()='archive']") | Where-Object {
            $hostOs = $_.SelectSingleNode("./*[local-name()='host-os']")
            -not $hostOs -or $hostOs.InnerText -eq 'windows'
        })
        if ($archive.Count -ne 1) { throw "Cannot find a Windows archive for $($package.Id)." }
        $complete = $archive[0].SelectSingleNode("./*[local-name()='complete']")
        $urlNode = $complete.SelectSingleNode("./*[local-name()='url']")
        $sizeNode = $complete.SelectSingleNode("./*[local-name()='size']")
        $checksumNode = $complete.SelectSingleNode("./*[local-name()='checksum']")
        if (-not $urlNode -or -not $sizeNode -or -not $checksumNode) { throw 'Incomplete Android SDK metadata.' }
        $url = [Uri]::new([Uri]'https://dl.google.com/android/repository/', $urlNode.InnerText.Trim())
        if ($url.Scheme -ne 'https' -or $url.Host -ne 'dl.google.com') { throw 'Unexpected SDK download host.' }
        $algorithm = $checksumNode.GetAttribute('type').Replace('-', '').ToUpperInvariant()
        if ($algorithm -notin @('SHA1', 'SHA256')) { throw "Unsupported SDK checksum: $algorithm" }
        $archivePath = Join-Path $taskDownloads ([IO.Path]::GetFileName($url.LocalPath))
        Get-VerifiedArchive -Url $url.AbsoluteUri -Path $archivePath -Algorithm $algorithm -Checksum $checksumNode.InnerText -Size ([long]$sizeNode.InnerText)
        Install-Archive -Archive $archivePath -Destination $package.Destination -Required $package.Required
    }
}

$env:JAVA_HOME = $taskJava
$env:ANDROID_HOME = $taskSdk
$env:ANDROID_SDK_ROOT = $taskSdk
$env:GRADLE_USER_HOME = Join-Path $taskTools 'gradle-user-home'
& (Join-Path $gradleDirectory 'bin\gradle.bat') --version --no-daemon
if ($LASTEXITCODE -ne 0) { throw 'Gradle failed to run with the configured JDK.' }
& (Join-Path $taskSdk 'build-tools\36.0.0\aapt2.exe') version
if ($LASTEXITCODE -ne 0) { throw 'Windows AAPT2 failed to run.' }
Write-Output "Android toolchain ready: $taskTools"

if ($Build) {
    # A child process keeps Verify-Ui's exit codes from masking setup failures.
    $buildArgs = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $taskTools 'Verify-Ui.ps1'), '-Prepare', '-Build')
    if ($Offline) { $buildArgs += '-Offline' }
    & powershell.exe @buildArgs
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
