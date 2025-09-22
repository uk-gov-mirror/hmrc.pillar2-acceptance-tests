#!/usr/bin/env bash
set -euo pipefail

# ---- Step 0: Check installed Edge version ----
EDGE_BINARY="${EDGE_BINARY:-/usr/bin/microsoft-edge}"  # default path
if [[ ! -f "$EDGE_BINARY" ]]; then
    echo "Error: Edge binary not found at $EDGE_BINARY"
    exit 1
fi

EDGE_VERSION=$("$EDGE_BINARY" --version | awk '{print $3}')
echo "Detected Microsoft Edge version: $EDGE_VERSION"

# Extract major.minor.build version for driver download
EDGE_DRIVER_VERSION=$(echo "$EDGE_VERSION" | awk -F. '{print $1"."$2"."$3}')

# ---- Step 1: Setup EdgeDriver ----
DRIVER_DIR="$HOME/workspace/drivers"
mkdir -p "$DRIVER_DIR"
cd "$DRIVER_DIR"

# Download matching EdgeDriver
EDGEDRIVER_URL="https://msedgedriver.azureedge.net/${EDGE_DRIVER_VERSION}/edgedriver_linux64.zip"
echo "Downloading EdgeDriver from $EDGEDRIVER_URL"
curl -L -o msedgedriver.zip "$EDGEDRIVER_URL"
unzip -o msedgedriver.zip
chmod +x msedgedriver
rm msedgedriver.zip

echo "EdgeDriver $EDGE_DRIVER_VERSION is ready at $DRIVER_DIR/msedgedriver"

# Add to PATH so sbt can find it
export PATH="$DRIVER_DIR:$PATH"

# ---- Step 2: Run your acceptance tests ----
ENVIRONMENT="${ENVIRONMENT:=local}"   # default to 'local'
sbt clean \
    -Dbrowser="edge" \
    -Dedge.binary="$EDGE_BINARY" \
    -Denvironment="${ENVIRONMENT}" \
    "testOnly uk.gov.hmrc.test.ui.cucumber.runner.Runner" testReport
