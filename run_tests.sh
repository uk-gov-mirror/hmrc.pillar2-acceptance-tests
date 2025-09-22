#!/usr/bin/env bash
set -euo pipefail

# Workspace and cache directories
WORKSPACE="${WORKSPACE:-$HOME/workspace}"
CACHE_DIR="${WORKSPACE}/.cache"
EDGE_DIR="${CACHE_DIR}/microsoft-edge-stable"
EDGE_BIN="$EDGE_DIR/microsoft-edge"
DRIVER_BIN="$CACHE_DIR/msedgedriver"

mkdir -p "$WORKSPACE" "$CACHE_DIR"

# 1. Install Microsoft Edge if missing
if [ ! -f "$EDGE_BIN" ]; then
  echo "Downloading Microsoft Edge..."
  EDGE_URL="https://msedgeseleniumstorage.blob.core.windows.net/edge/edge_linux64.zip"
  wget -q "$EDGE_URL" -O "$CACHE_DIR/edge.zip" || { echo "Failed to download Edge"; exit 1; }
  unzip -qq -o "$CACHE_DIR/edge.zip" -d "$EDGE_DIR"
  rm "$CACHE_DIR/edge.zip"
  chmod +x "$EDGE_BIN"
else
  echo "Edge browser already installed in cache."
fi

# 2. Install matching EdgeDriver if missing
if [ ! -f "$DRIVER_BIN" ]; then
  EDGE_VERSION=$("$EDGE_BIN" --version | awk '{print $3}')
  DRIVER_URL="https://msedgedriver.azureedge.net/${EDGE_VERSION}/edgedriver_linux64.zip"
  echo "Downloading EdgeDriver $EDGE_VERSION..."
  wget -q "$DRIVER_URL" -O "$CACHE_DIR/edgedriver.zip" || { echo "Failed to download EdgeDriver"; exit 1; }
  unzip -qq -o "$CACHE_DIR/edgedriver.zip" -d "$CACHE_DIR"
  chmod +x "$DRIVER_BIN"
  rm "$CACHE_DIR/edgedriver.zip"
else
  echo "EdgeDriver already installed in cache."
fi

# 3. Add local binaries to PATH
export PATH="$CACHE_DIR:$PATH"

# Optional debug
echo "Using Edge at: $EDGE_BIN"
"$EDGE_BIN" --version
echo "Using EdgeDriver at: $DRIVER_BIN"
"$DRIVER_BIN" --version

# 4. Ensure sbt exists
command -v sbt >/dev/null || { echo "sbt not found in PATH"; exit 1; }

# 5. Run sbt tests
sbt clean -Dbrowser="edge" -Denvironment="${ENVIRONMENT:-local}" \
  "testOnly uk.gov.hmrc.test.ui.cucumber.runner.Runner" testReport
