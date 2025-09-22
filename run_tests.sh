#!/usr/bin/env bash
set -euo pipefail

# ---- Step 1: Setup directories ----
WORK_DIR="$HOME/workspace/drivers"
mkdir -p "$WORK_DIR"
cd "$WORK_DIR"

# ---- Step 2: Get latest EdgeDriver version ----
EDGE_LATEST=$(curl -s https://msedgedriver.azureedge.net/LATEST_STABLE)
echo "Latest EdgeDriver version: $EDGE_LATEST"

# ---- Step 3: Download EdgeDriver ----
EDGE_DRIVER_ZIP="edgedriver_linux64.zip"
curl -L -o "$EDGE_DRIVER_ZIP" "https://msedgedriver.azureedge.net/${EDGE_LATEST}/edgedriver_linux64.zip"
unzip -o "$EDGE_DRIVER_ZIP"
chmod +x msedgedriver
rm "$EDGE_DRIVER_ZIP"
echo "EdgeDriver $EDGE_LATEST ready at $WORK_DIR/msedgedriver"

# ---- Step 4: Download portable Edge browser ----
EDGE_PORTABLE_DIR="$WORK_DIR/microsoft-edge"
mkdir -p "$EDGE_PORTABLE_DIR"

EDGE_TAR="microsoft-edge-${EDGE_LATEST}-linux-x64.tar.bz2"
curl -L -o "$EDGE_TAR" "https://msedgedriver.azureedge.net/${EDGE_LATEST}/edgedriver-linux64.zip"

# ---- Step 5: Update PATH ----
export PATH="$WORK_DIR:$PATH"
export EDGE_BINARY="$EDGE_PORTABLE_DIR/microsoft-edge"

# ---- Step 6: Run acceptance tests ----
ENVIRONMENT="${ENVIRONMENT:=local}"
sbt clean -Dbrowser="edge" -Denvironment="${ENVIRONMENT}" "testOnly uk.gov.hmrc.test.ui.cucumber.runner.Runner" testReport
