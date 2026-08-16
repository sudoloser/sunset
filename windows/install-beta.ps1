param(
  [string]$Workflow = "",
  [string]$GhToken = ""
)

$ErrorActionPreference = 'Stop'

$Repo = "sudoloser/sunset"
$InstallDir = "$env:USERPROFILE\.sunset\bin"
$TmpDir = "$env:USERPROFILE\.sunset\tmp"

$Arch = if ([Environment]::Is64BitOperatingSystem) { "x86_64" } else { "x86" }
if ($Arch -ne "x86_64") {
  Write-Host "Unsupported architecture: $Arch" -ForegroundColor Red
  exit 1
}
$Asset = "sunset-server-windows-x64.exe"
$Target = "x86_64-pc-windows-msvc"

if ($Workflow -eq "-h" -or $Workflow -eq "--help") {
  Write-Host "Usage: .\install-beta.ps1 [-Workflow <name-or-run-link>] [-GhToken <token>]"
  Write-Host ""
  Write-Host "Options:"
  Write-Host "  -Workflow <arg>    Download from a GitHub Actions workflow instead of the"
  Write-Host "                     beta release. Pass a workflow name (uses its latest"
  Write-Host "                     successful run) or a link to a specific run, e.g."
  Write-Host "                     https://github.com/sudoloser/sunset/actions/runs/31282787558"
  Write-Host "  -GhToken <token>   GitHub token used to download workflow artifacts"
  Write-Host "                     (overrides GITHUB_TOKEN/GH_TOKEN env vars)."
  exit 0
}

Write-Host ""
Write-Host "  ___  _   _ _____ _____ _   _  _____ " -ForegroundColor Cyan
Write-Host " / __|| | | |_   _| ____| \ | |/ ___|" -ForegroundColor Cyan
Write-Host " \__ \| | | | | | |  _| |  \| | |    " -ForegroundColor Cyan
Write-Host " |__/| |_| | | | | |___| |\  | |___ " -ForegroundColor Cyan
Write-Host " \___/ \___/  |_| |_____|_| \_|\____|" -ForegroundColor Cyan
Write-Host ""
if ($Workflow) {
  Write-Host "SunSet Beta Installer — $Asset (from workflow: $Workflow)" -ForegroundColor White
} else {
  Write-Host "SunSet Beta Installer — $Asset" -ForegroundColor White
}
Write-Host ""

$ApiHeaders = @{}
if ($GhToken) {
  $ApiHeaders["Authorization"] = "Bearer $GhToken"
} elseif ($env:GITHUB_TOKEN) {
  $ApiHeaders["Authorization"] = "Bearer $env:GITHUB_TOKEN"
} elseif ($env:GH_TOKEN) {
  $ApiHeaders["Authorization"] = "Bearer $env:GH_TOKEN"
}

function Invoke-GitHubApi {
  param([string]$Uri)
  Invoke-RestMethod -Uri $Uri -Headers $ApiHeaders
}

try {
  Write-Host "[1/4] Fetching beta build..." -ForegroundColor Yellow
  if ($Workflow) {
    Write-Host "  Downloading from workflow: $Workflow" -ForegroundColor Green
    if ($Workflow -match '/actions/runs/(\d+)') {
      $RunId = $Matches[1]
      Write-Host "  Run ID: $RunId (from run link)" -ForegroundColor Green
    } else {
      $Workflows = (Invoke-GitHubApi -Uri "https://api.github.com/repos/$Repo/actions/workflows?per_page=100").workflows
      $Wf = $Workflows | Where-Object { $_.name -eq $Workflow } | Select-Object -First 1
      if (-not $Wf) {
        Write-Host "  Workflow '$Workflow' not found." -ForegroundColor Red
        exit 1
      }

      $Runs = (Invoke-GitHubApi -Uri "https://api.github.com/repos/$Repo/actions/workflows/$($Wf.id)/runs?status=success&per_page=1").workflow_runs
      $Run = $Runs | Select-Object -First 1
      if (-not $Run) {
        Write-Host "  No successful run found for workflow '$Workflow'." -ForegroundColor Red
        exit 1
      }
      $RunId = $Run.id
      Write-Host "  Run ID: $RunId" -ForegroundColor Green
    }

    $Artifacts = (Invoke-GitHubApi -Uri "https://api.github.com/repos/$Repo/actions/runs/$RunId/artifacts").artifacts
    $Artifact = $Artifacts | Where-Object { $_.name -eq "sunset-server-$Target" } | Select-Object -First 1
    if (-not $Artifact) {
      Write-Host "  Artifact 'sunset-server-$Target' not found in run $RunId." -ForegroundColor Red
      exit 1
    }

    $DlUrl = "https://api.github.com/repos/$Repo/actions/artifacts/$($Artifact.id)/zip"
  } else {
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
  }
} catch {
  Write-Host "Failed to fetch beta build info." -ForegroundColor Red
  Write-Host "  $_" -ForegroundColor Red
  exit 1
}

try {
  New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
  New-Item -ItemType Directory -Force -Path $TmpDir | Out-Null

  Write-Host "[2/4] Downloading..." -ForegroundColor Yellow
  $ZipPath = "$TmpDir\update.zip"
  Invoke-WebRequest -Uri $DlUrl -OutFile $ZipPath -Headers $ApiHeaders
} catch {
  Write-Host "Download failed." -ForegroundColor Red
  Write-Host "  $_" -ForegroundColor Red
  if ($Workflow -and $ApiHeaders.Count -eq 0) {
    Write-Host "  Downloading workflow artifacts requires authentication." -ForegroundColor Yellow
    Write-Host "  Pass -GhToken, or set GITHUB_TOKEN or GH_TOKEN, and try again." -ForegroundColor Yellow
  }
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

$BinaryPath = Get-ChildItem -Path "$TmpDir\extracted" -Recurse -Filter "$Asset" | Select-Object -First 1
if (-not $BinaryPath) {
  Write-Host "Binary not found in archive." -ForegroundColor Red
  exit 1
}

try {
  $InstallPath = "$InstallDir\$Asset"
  Copy-Item -Path $BinaryPath.FullName -Destination $InstallPath -Force
} catch {
  Write-Host "Failed to copy binary to install directory." -ForegroundColor Red
  Write-Host "  $_" -ForegroundColor Red
  exit 1
}

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
