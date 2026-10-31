param(
    [switch]$ColdBoot
)

$ErrorActionPreference = "Stop"

$androidSdk = if ($env:ANDROID_HOME) {
    $env:ANDROID_HOME
} else {
    "D:\APP\AppData\Local\Android\Sdk"
}
$androidAvdHome = "D:\APP\.android\avd"
$avdName = "BukaMusic_Phone_API36"
$emulator = Join-Path $androidSdk "emulator\emulator.exe"
$adb = Join-Path $androidSdk "platform-tools\adb.exe"

if (-not (Test-Path -LiteralPath $emulator)) {
    throw "Android Emulator not found: $emulator"
}
if (-not (Test-Path -LiteralPath (Join-Path $androidAvdHome "$avdName.avd"))) {
    throw "AVD not found: $androidAvdHome\$avdName.avd"
}

$env:ANDROID_AVD_HOME = $androidAvdHome
$arguments = @(
    "-avd", $avdName,
    "-no-boot-anim",
    "-gpu", "auto",
    "-netdelay", "none",
    "-netspeed", "full"
)
if ($ColdBoot) {
    $arguments += "-no-snapshot"
}

$online = & $adb devices | Select-String -Pattern "emulator-\d+\s+device"
if (-not $online) {
    Start-Process -FilePath $emulator -ArgumentList $arguments
    & $adb wait-for-device
}

for ($attempt = 0; $attempt -lt 90; $attempt++) {
    $booted = (& $adb shell getprop sys.boot_completed 2>$null).Trim()
    if ($booted -eq "1") {
        Write-Host "Emulator is ready: $avdName"
        Write-Host "ADB device: emulator-5554"
        exit 0
    }
    Start-Sleep -Seconds 1
}

throw "The emulator did not finish booting within 90 seconds."
