#!/usr/bin/env bash
set -euo pipefail

WORKSPACE="${WORKSPACE:-$HOME/workspace}"
EDGE_BIN="$WORKSPACE/microsoft-edge-stable/microsoft-edge"
DRIVER_BIN="$WORKSPACE/msedgedriver"

mkdir -p "$WORKSPACE"

# 1. Install Microsoft Edge locally if missing
if [ ! -f "$EDGE_BIN" ]; then
  echo "Downloading Microsoft Edge..."
  EDGE_URL="https://msedgeseleniumstorage.blob.core.windows.net/edge/edge_linux64.zip"
  wget -q "$EDGE_URL" -O "$WORKSPACE/edge.zip"
  unzip -o "$WORKSPACE/edge.zip" -d "$WORKSPACE/microsoft-edge-stable"
  rm "$WORKSPACE/edge.zip"
  chmod +x "$EDGE_BIN"
else
  echo "Edge browser already installed locally."
fi

# 2. Install matching EdgeDriver locally
if [ ! -f "$DRIVER_BIN" ]; then
  EDGE_VERSION=$("$EDGE_BIN" --version | awk '{print $3}')
  DRIVER_URL="https://msedgedriver.azureedge.net/${EDGE_VERSION}/edgedriver_linux64.zip"
  echo "Downloading EdgeDriver $EDGE_VERSION..."
  wget -q "$DRIVER_URL" -O "$WORKSPACE/edgedriver.zip"
  unzip -o "$WORKSPACE/edgedriver.zip" -d "$WORKSPACE"
  chmod +x "$DRIVER_BIN"
  rm "$WORKSPACE/edgedriver.zip"
else
  echo "EdgeDriver already installed locally."
fi

# 3. Add local binaries to PATH
export PATH="$WORKSPACE:$PATH"

# 4. Run sbt tests
sbt clean -Dbrowser="edge" -Denvironment="${ENVIRONMENT:-local}" \
  "testOnly uk.gov.hmrc.test.ui.cucumber.runner.Runner" testReport
