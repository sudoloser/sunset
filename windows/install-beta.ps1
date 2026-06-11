$Repo = "sudoloser/sunset"
$InstallDir = "$env:USERPROFILE\.sunset\bin"
$TmpDir = "$env:USERPROFILE\.sunset\tmp"

$Arch = if ([Environment]::Is64BitOperatingSystem) { "x86_64" } else { "x86" }
if ($Arch -ne "x86_64") {
  Write-Host "Unsupported architecture: $Arch" -ForegroundColor Red
  exit 1
}
$Asset = "sunset-server-windows-x64.exe"

Write-Host ""
Write-Host "  ___  _   _ _____ _____ _   _  _____ " -ForegroundColor Cyan
Write-Host " / __|| | | |_   _| ____| \ | |/ ___|" -ForegroundColor Cyan
Write-Host " \__ \| | | | | | |  _| |  \| | |    " -ForegroundColor Cyan
Write-Host " |__/| |_| | | | | |___| |\  | |___ " -ForegroundColor Cyan
Write-Host " \___/ \___/  |_| |_____|_| \_|\____|" -ForegroundColor Cyan
Write-Host ""
Write-Host "SunSet Beta Installer — $Asset" -ForegroundColor White
Write-Host ""

Write-Host "[1/4] Fetching beta release..." -ForegroundColor Yellow
$Json = Invoke-RestMethod -Uri "https://api.github.com/repos/$Repo/releases/tags/beta"
$Tag = $Json.tag_name
Write-Host "  Version: $Tag (beta)" -ForegroundColor Green

$AssetObj = $Json.assets | Where-Object { $_.name -like "*$Asset*" } | Select-Object -First 1
if (-not $AssetObj) {
  Write-Host "Failed to find download asset for $Asset" -ForegroundColor Red
  Write-Host "Beta release may not be available yet. Run the workflow first." -ForegroundColor Yellow
  exit 1
}
$DlUrl = $AssetObj.browser_download_url

New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
New-Item -ItemType Directory -Force -Path $TmpDir | Out-Null

Write-Host "[2/4] Downloading..." -ForegroundColor Yellow
$ZipPath = "$TmpDir\update.zip"
Invoke-WebRequest -Uri $DlUrl -OutFile $ZipPath

Write-Host "[3/4] Extracting..." -ForegroundColor Yellow
Expand-Archive -Path $ZipPath -DestinationPath "$TmpDir\extracted" -Force

$BinaryPath = Get-ChildItem -Path "$TmpDir\extracted" -Recurse -Filter "$Asset" | Select-Object -First 1
if (-not $BinaryPath) {
  Write-Host "Binary not found in archive." -ForegroundColor Red
  exit 1
}

$InstallPath = "$InstallDir\$Asset"
Copy-Item -Path $BinaryPath.FullName -Destination $InstallPath -Force

Write-Host "[4/4] Cleaning up..." -ForegroundColor Yellow
Remove-Item -Path $TmpDir -Recurse -Force -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "  ✓ Installed beta to $InstallPath" -ForegroundColor Green
Write-Host ""
Write-Host "  Run it:" -ForegroundColor White
Write-Host "    $InstallPath" -ForegroundColor Gray
Write-Host ""
Write-Host "  Or add to your PATH manually." -ForegroundColor White
Write-Host ""
