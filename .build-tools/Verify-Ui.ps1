param(
    [switch]$Prepare,
    [switch]$Build,
    [switch]$Offline
)

$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskCopy = Join-Path $taskRoot '.build-ui'
$taskSdk = Join-Path $PSScriptRoot 'android-sdk'
$taskGradle = Join-Path $PSScriptRoot 'gradle-9.1.0\bin\gradle.bat'
$taskJava = 'C:\Program Files\Java\jdk-21.0.12.1'

function Resolve-ToolPath {
    param(
        [string[]]$Candidates,
        [string[]]$Required = @()
    )
    foreach ($candidate in $Candidates) {
        if ($candidate -and (Test-Path -LiteralPath $candidate)) {
            $complete = $true
            foreach ($file in $Required) {
                if (-not (Test-Path -LiteralPath (Join-Path $candidate $file))) { $complete = $false; break }
            }
            if ($complete) { return (Resolve-Path -LiteralPath $candidate).Path }
        }
    }
    return $null
}

$sdkCandidates = @(
    (Join-Path $PSScriptRoot 'android-sdk'),
    $env:ANDROID_SDK_ROOT,
    $env:ANDROID_HOME,
    (Join-Path $env:LOCALAPPDATA 'Android\Sdk')
)
$taskSdk = Resolve-ToolPath -Candidates $sdkCandidates -Required @(
    'platforms\android-36\android.jar',
    'platforms\android-36\core-for-system-modules.jar',
    'platforms\android-36\source.properties',
    'build-tools\36.0.0\aapt2.exe',
    'build-tools\36.0.0\d8.bat',
    'build-tools\36.0.0\zipalign.exe',
    'build-tools\36.0.0\apksigner.bat',
    'build-tools\36.0.0\lib\d8.jar',
    'build-tools\36.0.0\lib\apksigner.jar',
    'build-tools\36.0.0\core-lambda-stubs.jar',
    'build-tools\36.0.0\source.properties'
)
$gradleCandidates = @((Join-Path $PSScriptRoot 'gradle-9.1.0\bin\gradle.bat'))
if ($env:GRADLE_HOME) {
    $gradleCandidates += Join-Path $env:GRADLE_HOME 'bin\gradle.bat'
}
$gradleCommand = Get-Command gradle.bat -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source -First 1
if ($gradleCommand) { $gradleCandidates += $gradleCommand }
$taskGradle = Resolve-ToolPath $gradleCandidates
$javaCandidates = @(
    'C:\Program Files\Java\jdk-21.0.12.1',
    $env:JAVA_HOME
)
$taskJava = Resolve-ToolPath -Candidates $javaCandidates -Required @('bin\java.exe', 'bin\javac.exe', 'bin\keytool.exe')
if (-not $taskSdk) { $taskSdk = Join-Path $PSScriptRoot 'android-sdk' }
if (-not $taskGradle) { $taskGradle = Join-Path $PSScriptRoot 'gradle-9.1.0\bin\gradle.bat' }
if (-not $taskJava) { $taskJava = 'C:\Program Files\Java\jdk-21.0.12.1' }
$taskAapt = Join-Path $taskSdk 'build-tools\36.0.0\aapt2.exe'
$taskBuildTools = Join-Path $taskSdk 'build-tools\36.0.0'
$taskPlatform = Join-Path $taskSdk 'platforms\android-36'
$taskTools = @(
    (Join-Path $taskJava 'bin\java.exe'),
    (Join-Path $taskJava 'bin\javac.exe'),
    (Join-Path $taskJava 'bin\keytool.exe'),
    $taskGradle,
    (Join-Path $taskPlatform 'android.jar'),
    (Join-Path $taskPlatform 'core-for-system-modules.jar'),
    (Join-Path $taskPlatform 'source.properties'),
    $taskAapt,
    (Join-Path $taskBuildTools 'd8.bat'),
    (Join-Path $taskBuildTools 'zipalign.exe'),
    (Join-Path $taskBuildTools 'apksigner.bat'),
    (Join-Path $taskBuildTools 'lib\d8.jar'),
    (Join-Path $taskBuildTools 'lib\apksigner.jar'),
    (Join-Path $taskBuildTools 'core-lambda-stubs.jar'),
    (Join-Path $taskBuildTools 'source.properties')
)
$taskKeystore = Join-Path $PSScriptRoot 'debug.keystore'

if ($Prepare) {
    New-Item -ItemType Directory -Path $taskCopy -Force | Out-Null
    foreach ($taskFile in @('settings.gradle.kts', 'build.gradle.kts', 'gradle.properties', 'gradlew', 'gradlew.bat')) {
        Copy-Item -LiteralPath (Join-Path $taskRoot $taskFile) -Destination $taskCopy -Force
    }
    New-Item -ItemType Directory -Path (Join-Path $taskCopy 'app'), (Join-Path $taskCopy 'gradle') -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $taskRoot 'gradle\libs.versions.toml') -Destination (Join-Path $taskCopy 'gradle') -Force
    Copy-Item -LiteralPath (Join-Path $taskRoot 'gradle\wrapper') -Destination (Join-Path $taskCopy 'gradle') -Recurse -Force
    foreach ($taskFile in @('build.gradle.kts', 'proguard-rules.pro')) {
        Copy-Item -LiteralPath (Join-Path $taskRoot "app\$taskFile") -Destination (Join-Path $taskCopy 'app') -Force
    }
    Copy-Item -LiteralPath (Join-Path $taskRoot 'app\src') -Destination (Join-Path $taskCopy 'app') -Recurse -Force
    $taskSdkForward = $taskSdk.Replace('\', '/').Replace(':', '\:')
    [IO.File]::WriteAllText((Join-Path $taskCopy 'local.properties'), "sdk.dir=$taskSdkForward`n")
    $windowsProps = Join-Path $taskCopy 'gradle.properties'
    $props = [IO.File]::ReadAllText($windowsProps)
    $props = [regex]::Replace($props, '(?ms)^\s*# Proot/Termux Compatibility Settings.*?android\.aapt2\.process\.daemon=false\s*', "`n")
    [IO.File]::WriteAllText($windowsProps, $props.TrimEnd() + "`n")
    $windowsBuild = Join-Path $taskCopy 'app\build.gradle.kts'
    $buildText = [IO.File]::ReadAllText($windowsBuild)
    $buildText = [regex]::Replace($buildText, '(?ms)\r?\n// Force use of ARM64 binaries for AAPT2 in Proot environment\r?\nconfigurations\.all \{.*?\r?\n\}\r?\n', "`n")
    $signing = @"
android {
    signingConfigs {
        getByName("debug") {
            storeFile = file("$($taskKeystore.Replace('\', '/'))")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
"@
    $buildText = $buildText.Replace('android {', $signing)
    [IO.File]::WriteAllText($windowsBuild, $buildText)
    $wrapperPath = Join-Path $taskCopy 'gradle\wrapper\gradle-wrapper.properties'
    $wrapper = [IO.File]::ReadAllText($wrapperPath)
    $wrapper = [regex]::Replace($wrapper, '(?m)^distributionUrl=.*$', 'distributionUrl=https\://services.gradle.org/distributions/gradle-9.1.0-bin.zip')
    [IO.File]::WriteAllText($wrapperPath, $wrapper)
    if (Test-Path -LiteralPath $taskAapt) {
        [IO.File]::AppendAllText($windowsProps, "android.aapt2FromMavenOverride=$($taskAapt.Replace('\', '/').Replace(':', '\:'))`n")
    }
    Write-Output "Prepared isolated sources: $taskCopy"
}

$taskMissing = @($taskTools | Where-Object { -not (Test-Path -LiteralPath $_ -PathType Leaf) })
if ($taskMissing.Count) {
    Write-Output 'Missing tools:'
    $taskMissing | Write-Output
    if ($Build) { throw 'The isolated Android build tools are incomplete.' }
    exit 0
}
Write-Output 'The isolated Android build tools are present.'

if ($Build) {
    if (-not (Test-Path -LiteralPath (Join-Path $taskCopy 'local.properties'))) {
        throw 'Run this script with -Prepare after the UI sources are final.'
    }
    $env:JAVA_HOME = $taskJava
    $env:ANDROID_HOME = $taskSdk
    $env:ANDROID_SDK_ROOT = $taskSdk
    $env:GRADLE_USER_HOME = Join-Path $PSScriptRoot 'gradle-user-home'
    $env:ANDROID_USER_HOME = Join-Path $PSScriptRoot 'android-user-home'
    New-Item -ItemType Directory -Path $env:ANDROID_USER_HOME -Force | Out-Null
    $versionOutput = & $taskGradle --version --no-daemon 2>&1
    if ($LASTEXITCODE -ne 0 -or (($versionOutput | Out-String) -notmatch '(?m)^Gradle 9\.1\.0\s*$')) {
        throw "This build requires Gradle 9.1.0. Selected executable: $taskGradle"
    }
    $versionOutput | Write-Output
    if (-not (Test-Path -LiteralPath $taskKeystore)) {
        & (Join-Path $taskJava 'bin\keytool.exe') -genkeypair -keystore $taskKeystore -storetype JKS -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Android Debug,O=Android,C=US' -noprompt
        if ($LASTEXITCODE -ne 0) { throw 'Could not create the workspace debug signing key.' }
    }
    $taskArgs = @('-p', $taskCopy, ':app:assembleDebug', ':app:testDebugUnitTest', '--no-daemon', '--console=plain')
    if ($Offline) { $taskArgs += '--offline' }
    & $taskGradle @taskArgs
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Write-Output "APK: $(Join-Path $taskCopy 'app\build\outputs\apk\debug\app-debug.apk')"
}
