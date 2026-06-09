#!/usr/bin/env bash
set -euo pipefail

REPO="sudoloser/sunset"
INSTALL_DIR="$HOME/.sunset/bin"
TMP_DIR="$HOME/.sunset/tmp"

ARCH="$(uname -m)"
case "$ARCH" in
  x86_64)  ASSET="sunset-server-x86_64-unknown-linux-gnu" ;;
  aarch64) ASSET="sunset-server-aarch64-unknown-linux-gnu" ;;
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

DL_URL=$(echo "$JSON" | grep '"browser_download_url"' | grep "$ASSET" | cut -d'"' -f4 | head -1)
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

# Find the binary
BINARY=""
if [ -f "$TMP_DIR/extracted/$ASSET" ]; then
  BINARY="$TMP_DIR/extracted/$ASSET"
else
  BINARY=$(find "$TMP_DIR/extracted" -type f -name "$ASSET" 2>/dev/null | head -1)
fi

if [ -z "$BINARY" ]; then
  echo "Binary not found in archive."
  exit 1
fi

INSTALL_PATH="$INSTALL_DIR/$ASSET"
cp "$BINARY" "$INSTALL_PATH"
chmod +x "$INSTALL_PATH"

echo "[4/4] Cleaning up..."
rm -rf "$TMP_DIR"

echo ""
echo "  ✓ Installed to $INSTALL_PATH"
echo ""
echo "  Run it:"
echo "    $INSTALL_PATH"
echo ""
echo "  Or add to your PATH:"
echo "    export PATH=\"\$HOME/.sunset/bin:\$PATH\""
echo "    sunset-server"
echo ""
