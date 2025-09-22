#!/usr/bin/env bash
set -euo pipefail
set -x  # Debug mode for Jenkins logs

WORKSPACE="${WORKSPACE:-$HOME/workspace}"
EDGE_DIR="$WORKSPACE/microsoft-edge"
DRIVER_BIN="$WORKSPACE/msedgedriver"

mkdir -p "$WORKSPACE" "$EDGE_DIR"

# 1. Get latest EdgeDriver version
LATEST_EDGE_VERSION=$(curl -s https://msedgedriver.azureedge.net/LATEST_RELEASE_STABLE)
echo "Latest Edge version: $LATEST_EDGE_VERSION"

# 2. Download EdgeDriver if missing
if [ ! -f "$DRIVER_BIN" ]; then
    DRIVER_URL="https://msedgedriver.azureedge.net/${LATEST_EDGE_VERSION}/edgedriver_linux64.zip"
    echo "Downloading EdgeDriver $LATEST_EDGE_VERSION..."
    wget -q "$DRIVER_URL" -O "$WORKSPACE/edgedriver.zip"
    unzip -o "$WORKSPACE/edgedriver.zip" -d "$WORKSPACE"
    chmod +x "$DRIVER_BIN"
    rm "$WORKSPACE/edgedriver.zip"
else
    echo "EdgeDriver already installed locally."
fi

# 3. Download Microsoft Edge if missing
EDGE_DEB_URL=$(curl -s https://www.microsoft.com/en-us/edge/business/download?form=MA13FJ \
  | grep -oP 'https://.*amd64\.deb' | head -n 1)

if [ -z "$EDGE_DEB_URL" ]; then
    echo "Failed to retrieve latest Edge .deb URL"
    exit 1
fi

EDGE_DEB="$WORKSPACE/edge_latest.deb"

if [ ! -f "$EDGE_DIR/opt/microsoft/msedge/msedge" ]; then
    echo "Downloading Microsoft Edge..."
    wget -q "$EDGE_DEB_URL" -O "$EDGE_DEB"

    echo "Extracting Edge..."
    dpkg-deb -x "$EDGE_DEB" "$EDGE_DIR"
    chmod +x "$EDGE_DIR/opt/microsoft/msedge/msedge"
else
    echo "Edge browser already available."
fi

# 4. Update PATH
export PATH="$EDGE_DIR/opt/microsoft/msedge:$WORKSPACE:$PATH"

# 5. Check everything before running tests
echo "=== Verifying Edge setup ==="
which msedge || echo "msedge NOT found!"
which msedgedriver || echo "msedgedriver NOT found!"
"$EDGE_DIR/opt/microsoft/msedge/msedge" --version || echo "Failed to launch Edge!"

# 6. Run sbt tests
sbt -Dwebdriver.edge.driver="$DRIVER_BIN" \
    -Dbrowser="edge" \
    -Denvironment="${ENVIRONMENT:-local}" \
    "testOnly uk.gov.hmrc.test.ui.cucumber.runner.Runner"

# 7. Check reports
echo "=== Checking report files ==="
ls -R target || echo "Target folder missing!"
