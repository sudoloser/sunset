#!/usr/bin/env bash
set -euo pipefail

REPO="sudoloser/sunset"
INSTALL_DIR="$HOME/.sunset/bin"

ARCH="$(uname -m)"
case "$ARCH" in
  x86_64)  ASSET="sunset-server-linux-x86_64" ;;
  aarch64*) ASSET="sunset-server-linux-aarch64" ;;
  *)       echo "Unsupported architecture: $ARCH"; exit 1 ;;
esac

echo "  ___  _   _ _____ _____ _   _  _____ "
echo " / __|| | | |_   _| ____| \ | |/ ___|"
echo " \__ \| | | | | | |  _| |  \| | |    "
echo " |__/| |_| | | | | |___| |\  | |___ "
echo " \___/ \___/  |_| |_____|_| \_|\____|"
echo ""
echo "SunSet Beta Installer — $ASSET"
echo ""

echo "[1/3] Fetching beta release..."
# Check dependencies
for cmd in curl; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "  Error: '$cmd' is not installed."
    exit 1
  fi
done

JSON=$(curl -fsL "https://api.github.com/repos/$REPO/releases/tags/beta")
TAG=$(echo "$JSON" | grep '"tag_name"' | cut -d'"' -f4)
echo "  Version: $TAG (beta)"

DL_URL=$(echo "$JSON" | grep '"browser_download_url"' | grep "$ASSET" | cut -d'"' -f4 | head -1 || true)
if [ -z "$DL_URL" ]; then
  echo "Failed to find download asset for $ASSET"
  echo "Beta release may not be available yet. Run the workflow first."
  exit 1
fi

mkdir -p "$INSTALL_DIR"

echo "[2/3] Downloading..."
INSTALL_PATH="$INSTALL_DIR/sunset-server"
curl -fsL "$DL_URL" -o "$INSTALL_PATH"
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
