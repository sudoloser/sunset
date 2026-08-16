#!/usr/bin/env bash
set -euo pipefail

REPO="sudoloser/sunset"
INSTALL_DIR="$HOME/.sunset/bin"

WORKFLOW=""
GH_TOKEN_ARG=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --workflow)
      if [[ -z "${2:-}" ]]; then
        echo "Error: --workflow requires a workflow name or run link." >&2
        exit 1
      fi
      WORKFLOW="$2"
      shift 2
      ;;
    --workflow=*)
      WORKFLOW="${1#*=}"
      shift
      ;;
    --gh_token)
      if [[ -z "${2:-}" ]]; then
        echo "Error: --gh_token requires a token value." >&2
        exit 1
      fi
      GH_TOKEN_ARG="$2"
      shift 2
      ;;
    --gh_token=*)
      GH_TOKEN_ARG="${1#*=}"
      shift
      ;;
    -h|--help)
      echo "Usage: $0 [--workflow <name-or-run-link>] [--gh_token <token>]"
      echo ""
      echo "Download and install the SunSet beta server."
      echo ""
      echo "Options:"
      echo "  --workflow <arg>   Download from a GitHub Actions workflow instead of the"
      echo "                     beta release. Pass a workflow name (uses its latest"
      echo "                     successful run) or a link to a specific run, e.g."
      echo "                     https://github.com/sudoloser/sunset/actions/runs/31282787558"
      echo "  --gh_token <token> GitHub token used to download workflow artifacts"
      echo "                     (overrides GITHUB_TOKEN/GH_TOKEN env vars)."
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      echo "Run '$0 --help' for usage." >&2
      exit 1
      ;;
  esac
done

ARCH="$(uname -m)"
case "$ARCH" in
  x86_64)   ASSET="sunset-server-linux-x86_64";  TARGET="x86_64-unknown-linux-gnu" ;;
  aarch64*) ASSET="sunset-server-linux-aarch64"; TARGET="aarch64-unknown-linux-gnu" ;;
  *)        echo "Unsupported architecture: $ARCH"; exit 1 ;;
esac

echo "  ___  _   _ _____ _____ _   _  _____ "
echo " / __|| | | |_   _| ____| \ | |/ ___|"
echo " \__ \| | | | | | |  _| |  \| | |    "
echo " |__/| |_| | | | | |___| |\  | |___ "
echo " \___/ \___/  |_| |_____|_| \_|\____|"
echo ""
if [[ -n "$WORKFLOW" ]]; then
  echo "SunSet Beta Installer — $ASSET (from workflow: $WORKFLOW)"
else
  echo "SunSet Beta Installer — $ASSET"
fi
echo ""

echo "[1/3] Fetching beta build..."
for cmd in curl; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "  Error: '$cmd' is not installed." >&2
    exit 1
  fi
done

API_HEADERS=()
if [[ -n "$GH_TOKEN_ARG" ]]; then
  API_HEADERS=(-H "Authorization: Bearer $GH_TOKEN_ARG")
elif [[ -n "${GITHUB_TOKEN:-}" ]]; then
  API_HEADERS=(-H "Authorization: Bearer $GITHUB_TOKEN")
elif [[ -n "${GH_TOKEN:-}" ]]; then
  API_HEADERS=(-H "Authorization: Bearer $GH_TOKEN")
fi

api() {
  curl -fsSL "${API_HEADERS[@]}" "$1"
}

WORKFLOW_DL=0
if [[ -n "$WORKFLOW" ]]; then
  RUN_ID=$(printf '%s' "$WORKFLOW" | grep -Eo '/actions/runs/[0-9]+' | grep -o '[0-9]*' | head -1 || true)
  if [[ -n "$RUN_ID" ]]; then
    echo "  Run: $RUN_ID (from run link)"
  else
    WF_NAME_ESC=$(printf '%s' "$WORKFLOW" | sed 's/[^^]/[&]/g; s/\^/\\^/g')
    WF_JSON=$(api "https://api.github.com/repos/$REPO/actions/workflows?per_page=100" | tr -d '\n\r') || {
      echo "Failed to fetch workflow list." >&2
      exit 1
    }
    WF_ID=$(printf '%s' "$WF_JSON" | grep -Eo "\"id\":\s*[0-9]*,\s*\"node_id\":\s*\"[^\"]*\",\s*\"name\":\s*\"$WF_NAME_ESC\"" | grep -o '[0-9]*' | head -1 || true)
    if [[ -z "$WF_ID" ]]; then
      echo "Workflow '$WORKFLOW' not found." >&2
      exit 1
    fi

    RUN_ID=$(api "https://api.github.com/repos/$REPO/actions/workflows/$WF_ID/runs?status=success&per_page=1" | tr -d '\n\r' | grep -Eo '"id":\s*[0-9]*' | head -1 | grep -o '[0-9]*' || true)
    if [[ -z "$RUN_ID" ]]; then
      echo "No successful run found for workflow '$WORKFLOW'." >&2
      exit 1
    fi
    echo "  Run: $RUN_ID"
  fi

  ARTIFACTS_JSON=$(api "https://api.github.com/repos/$REPO/actions/runs/$RUN_ID/artifacts" | tr -d '\n\r') || {
    echo "Failed to fetch artifacts." >&2
    exit 1
  }
  ARTIFACT_ID=$(printf '%s' "$ARTIFACTS_JSON" | grep -Eo "\"id\":\s*[0-9]*,\s*\"node_id\":\s*\"[^\"]*\",\s*\"name\":\s*\"sunset-server-$TARGET\"" | grep -o '[0-9]*' | head -1 || true)
  if [[ -z "$ARTIFACT_ID" ]]; then
    echo "Artifact 'sunset-server-$TARGET' not found in run $RUN_ID." >&2
    exit 1
  fi

  DL_URL="https://api.github.com/repos/$REPO/actions/artifacts/$ARTIFACT_ID/zip"
  WORKFLOW_DL=1
else
  JSON=$(curl -fsSL "https://api.github.com/repos/$REPO/releases/tags/beta") || {
    echo "Failed to fetch beta release info." >&2
    exit 1
  }
  TAG=$(echo "$JSON" | grep '"tag_name"' | cut -d'"' -f4)
  echo "  Version: $TAG (beta)"

  DL_URL=$(echo "$JSON" | grep '"browser_download_url"' | grep "$ASSET" | cut -d'"' -f4 | head -1 || true)
  if [ -z "$DL_URL" ]; then
    echo "Failed to find download asset for $ASSET" >&2
    echo "Beta release may not be available yet. Run the workflow first." >&2
    exit 1
  fi
fi

mkdir -p "$INSTALL_DIR"

echo "[2/3] Downloading..."
INSTALL_PATH="$INSTALL_DIR/sunset-server"
if [[ "$WORKFLOW_DL" == "1" ]]; then
  TMP_DIR="$(mktemp -d)"
  trap 'rm -rf "$TMP_DIR"' EXIT
  ZIP_PATH="$TMP_DIR/artifact.zip"
  if ! curl -fsSL "${API_HEADERS[@]}" "$DL_URL" -o "$ZIP_PATH"; then
    echo "Download failed." >&2
    if [[ ${#API_HEADERS[@]} -eq 0 ]]; then
      echo "  Downloading workflow artifacts requires authentication." >&2
      echo "  Pass --gh_token, or set GITHUB_TOKEN or GH_TOKEN, and try again." >&2
    fi
    exit 1
  fi

  EXTRACT_DIR="$TMP_DIR/extracted"
  mkdir -p "$EXTRACT_DIR"
  if command -v unzip >/dev/null 2>&1; then
    unzip -q "$ZIP_PATH" -d "$EXTRACT_DIR"
  elif command -v python3 >/dev/null 2>&1; then
    python3 -c "import zipfile,sys; zipfile.ZipFile(sys.argv[1]).extractall(sys.argv[2])" "$ZIP_PATH" "$EXTRACT_DIR"
  else
    echo "'unzip' (or python3) is required to extract the workflow artifact." >&2
    exit 1
  fi

  BIN_PATH=$(find "$EXTRACT_DIR" -type f -name "$ASSET" | head -1)
  if [[ -z "$BIN_PATH" ]]; then
    echo "Binary '$ASSET' not found in artifact." >&2
    exit 1
  fi
  cp "$BIN_PATH" "$INSTALL_PATH"
else
  curl -fsSL "$DL_URL" -o "$INSTALL_PATH" || {
    echo "Download failed." >&2
    exit 1
  }
fi
chmod +x "$INSTALL_PATH"

echo "[3/3] Adding to PATH..."
SHELL_RC=""
if [ -f "$HOME/.zshrc" ]; then
  SHELL_RC="$HOME/.zshrc"
elif [ -f "$HOME/.bashrc" ]; then
  SHELL_RC="$HOME/.bashrc"
elif [ -f "$HOME/.bash_profile" ]; then
  SHELL_RC="$HOME/.bash_profile"
fi

if [ -n "$SHELL_RC" ]; then
  if grep -q '\.sunset/bin' "$SHELL_RC" 2>/dev/null; then
    echo "  PATH already configured in $SHELL_RC"
  else
    echo "" >> "$SHELL_RC"
    echo "# SunSet" >> "$SHELL_RC"
    echo "export PATH=\"\$HOME/.sunset/bin:\$PATH\"" >> "$SHELL_RC"
    echo "  Added ~/.sunset/bin to PATH in $SHELL_RC"
  fi
else
  echo "  Could not detect shell rc file. Add this manually:"
  echo "    export PATH=\"\$HOME/.sunset/bin:\$PATH\""
fi

echo ""
echo "  ✓ Installed beta to $INSTALL_PATH"
echo ""
echo "  Run it:"
echo "    source $SHELL_RC"
echo "    sunset-server"
echo ""
