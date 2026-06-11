#!/usr/bin/env bash
set -euo pipefail

REPO="sudoloser/sunset"
INSTALL_DIR="$HOME/.sunset/bin"
TMP_DIR="$HOME/.sunset/tmp"

ARCH="$(uname -m)"
case "$ARCH" in
  x86_64)  ASSET="sunset-server-x86_64-unknown-linux-gnu" ;;
  aarch64*) ASSET="sunset-server-aarch64-unknown-linux-gnu" ;;
  *)       echo "Unsupported architecture: $ARCH"; exit 1 ;;
esac

echo "  ___  _   _ _____ _____ _   _  _____ "
echo " / __|| | | |_   _| ____| \ | |/ ___|"
echo " \__ \| | | | | | |  _| |  \| | |    "
echo " |__/| |_| | | | | |___| |\  | |___ "
echo " \___/ \___/  |_| |_____|_| \_|\____|"
echo ""
echo "SunSet Installer — $ASSET"
echo ""

echo "[1/4] Fetching latest release..."
JSON=$(curl -sL "https://api.github.com/repos/$REPO/releases/latest")
TAG=$(echo "$JSON" | grep '"tag_name"' | cut -d'"' -f4)
echo "  Version: $TAG"

DL_URL=$(echo "$JSON" | grep '"browser_download_url"' | grep "$ASSET" | cut -d'"' -f4 | head -1 || true)
if [ -z "$DL_URL" ]; then
  echo "Failed to find download asset for $ASSET"
  exit 1
fi

mkdir -p "$INSTALL_DIR" "$TMP_DIR"

echo "[2/4] Downloading..."
ZIP_PATH="$TMP_DIR/update.zip"
curl -sL "$DL_URL" -o "$ZIP_PATH"

echo "[3/4] Extracting..."
unzip -o "$ZIP_PATH" -d "$TMP_DIR/extracted" >/dev/null 2>&1

BINARY=$(find "$TMP_DIR/extracted" -type f \( -name "sunset-server*" -o -executable \) 2>/dev/null | head -1)

if [ -z "$BINARY" ]; then
  echo "Binary not found in archive."
  echo "Contents of extracted folder:"
  find "$TMP_DIR/extracted" -type f
  exit 1
fi

INSTALL_PATH="$INSTALL_DIR/sunset-server"
cp "$BINARY" "$INSTALL_PATH"
chmod +x "$INSTALL_PATH"

echo "[4/4] Adding to PATH..."
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

rm -rf "$TMP_DIR"

echo ""
echo "  ✓ Installed to $INSTALL_PATH"
echo ""
echo "  Run it:"
echo "    source $SHELL_RC"
echo "    sunset-server"
echo ""
