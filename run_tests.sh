#!/usr/bin/env bash
set -euo pipefail

WORKSPACE="${WORKSPACE:-$HOME/workspace}"
DRIVER_BIN="$WORKSPACE/msedgedriver"
EDGE_DIR="$WORKSPACE/microsoft-edge"

mkdir -p "$WORKSPACE" "$EDGE_DIR"

# 1. Define known working version temporarily
EDGE_VERSION="140.0.3485.81"
echo "Using EdgeDriver version: $EDGE_VERSION"

# 2. Download EdgeDriver
DRIVER_URL="https://msedgedriver.microsoft.com/${EDGE_VERSION}/edgedriver_linux64.zip"
echo "Downloading EdgeDriver from $DRIVER_URL"
wget -q "$DRIVER_URL" -O "$WORKSPACE/edgedriver.zip"
unzip -o "$WORKSPACE/edgedriver.zip" -d "$WORKSPACE"
chmod +x "$DRIVER_BIN"
rm "$WORKSPACE/edgedriver.zip"

# 3. Add to PATH
export PATH="$WORKSPACE:$PATH"

# 4. Run tests
sbt clean -Dbrowser="edge" -Denvironment="${ENVIRONMENT:-local}" \
  "testOnly uk.gov.hmrc.test.ui.cucumber.runner.Runner" testReport
