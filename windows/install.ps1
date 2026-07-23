$ErrorActionPreference = 'Stop'

$Repo = "sudoloser/sunset"
$InstallDir = "$env:USERPROFILE\.sunset\bin"
$TmpDir = "$env:USERPROFILE\.sunset\tmp"

$Arch = if ([Environment]::Is64BitOperatingSystem) { "x86_64" } else { "x86" }
if ($Arch -ne "x86_64") {
  Write-Host "Unsupported architecture: $Arch" -ForegroundColor Red
  exit 1
}
$Asset = "sunset-server-x86_64-pc-windows-msvc"

Write-Host ""
Write-Host "  ___  _   _ _____ _____ _   _  _____ " -ForegroundColor Cyan
Write-Host " / __|| | | |_   _| ____| \ | |/ ___|" -ForegroundColor Cyan
Write-Host " \__ \| | | | | | |  _| |  \| | |    " -ForegroundColor Cyan
Write-Host " |__/| |_| | | | | |___| |\  | |___ " -ForegroundColor Cyan
Write-Host " \___/ \___/  |_| |_____|_| \_|\____|" -ForegroundColor Cyan
Write-Host ""
Write-Host "SunSet Installer — $Asset" -ForegroundColor White
Write-Host ""

try {
  Write-Host "[1/4] Fetching latest release..." -ForegroundColor Yellow
  $Json = Invoke-RestMethod -Uri "https://api.github.com/repos/$Repo/releases/latest"
  $Tag = $Json.tag_name
  Write-Host "  Version: $Tag" -ForegroundColor Green
} catch {
  Write-Host "Failed to fetch latest release info." -ForegroundColor Red
  Write-Host "  $_" -ForegroundColor Red
  exit 1
}

$AssetObj = $Json.assets | Where-Object { $_.name -like "*$Asset*" } | Select-Object -First 1
if (-not $AssetObj) {
  Write-Host "Failed to find download asset for $Asset" -ForegroundColor Red
  exit 1
}
$DlUrl = $AssetObj.browser_download_url

try {
  New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
  New-Item -ItemType Directory -Force -Path $TmpDir | Out-Null

  Write-Host "[2/4] Downloading..." -ForegroundColor Yellow
  $ZipPath = "$TmpDir\update.zip"
  Invoke-WebRequest -Uri $DlUrl -OutFile $ZipPath
} catch {
  Write-Host "Download failed." -ForegroundColor Red
  Write-Host "  $_" -ForegroundColor Red
  exit 1
}

try {
  Write-Host "[3/4] Extracting..." -ForegroundColor Yellow
  Expand-Archive -Path $ZipPath -DestinationPath "$TmpDir\extracted" -Force
} catch {
  Write-Host "Extraction failed." -ForegroundColor Red
  Write-Host "  $_" -ForegroundColor Red
  exit 1
}

$BinaryPath = Get-ChildItem -Path "$TmpDir\extracted" -Recurse -Filter "$Asset.exe" | Select-Object -First 1
if (-not $BinaryPath) {
  Write-Host "Binary not found in archive." -ForegroundColor Red
  exit 1
}

try {
  $InstallPath = "$InstallDir\$Asset.exe"
  Copy-Item -Path $BinaryPath.FullName -Destination $InstallPath -Force
} catch {
  Write-Host "Failed to copy binary to install directory." -ForegroundColor Red
  Write-Host "  $_" -ForegroundColor Red
  exit 1
}

Write-Host "[4/4] Cleaning up..." -ForegroundColor Yellow
Remove-Item -Path $TmpDir -Recurse -Force -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "  ✓ Installed to $InstallPath" -ForegroundColor Green
Write-Host ""
Write-Host "  Run it:" -ForegroundColor White
Write-Host "    $InstallPath" -ForegroundColor Gray
Write-Host ""
Write-Host "  Or add to your PATH manually." -ForegroundColor White
Write-Host ""
