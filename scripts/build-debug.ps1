$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$jdkHome = Join-Path $projectRoot ".jdk\jdk-17.0.19+10"

if (-not (Test-Path (Join-Path $jdkHome "bin\java.exe"))) {
    throw "Portable JDK not found at $jdkHome. Download or install JDK 17 before building."
}

$env:JAVA_HOME = $jdkHome
$env:Path = "$jdkHome\bin;$env:Path"

Push-Location $projectRoot
try {
    .\gradlew.bat :app:assembleDebug
}
finally {
    Pop-Location
}
