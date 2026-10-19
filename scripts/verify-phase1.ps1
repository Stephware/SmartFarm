# From repository root: .\scripts\verify-phase1.ps1 [-SkipAndroid]
param([switch]$SkipAndroid)
$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
Push-Location (Join-Path $PSScriptRoot "..")
try {
    if (-not (Get-Command dotnet -ErrorAction SilentlyContinue)) {
        throw "Install .NET 10 SDK."
    }
    Write-Host "[1/4] SDK"
    & dotnet --version
    if ($LASTEXITCODE -ne 0) { throw "dotnet --version failed" }
    Write-Host "[2/4] Restore + build"
    & dotnet restore SmartFarm.slnx
    if ($LASTEXITCODE -ne 0) { throw "dotnet restore failed" }
    & dotnet build SmartFarm.slnx --no-restore
    if ($LASTEXITCODE -ne 0) { throw "dotnet build failed" }
    Write-Host "[3/4] Tests"
    & dotnet test SmartFarm.slnx --no-build
    if ($LASTEXITCODE -ne 0) { throw "dotnet test failed" }
    if ($SkipAndroid) {
        Write-Host "[4/4] Android skipped by request."
    } else {
        Write-Host "[4/4] Android debug build + unit tests"
        Push-Location "mobile/SmartFarm.Android"
        try {
            & .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
            if ($LASTEXITCODE -ne 0) { throw "Android Gradle checks failed" }
        } finally { Pop-Location }
    }
    Write-Host "PASS: local checks completed."
} finally { Pop-Location }

