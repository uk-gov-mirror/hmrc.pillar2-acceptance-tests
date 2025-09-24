#!/usr/bin/env bash
set -euo pipefail

EDGE_VERSION="138.0.3351.95"
EDGE_BASE="$HOME/.local/microsoft-edge-${EDGE_VERSION}"

echo "=== Setting up Microsoft Edge and EdgeDriver for UI tests ==="
echo "Cleaning up old versions..."
rm -rf "$HOME/.local/microsoft-edge-"*

echo "Installing Microsoft Edge $EDGE_VERSION..."
wget -qO edge.deb "https://packages.microsoft.com/repos/edge/pool/main/m/microsoft-edge-stable/microsoft-edge-stable_${EDGE_VERSION}-1_amd64.deb"

mkdir -p "$EDGE_BASE"
dpkg-deb -x edge.deb "$EDGE_BASE"

EDGE_BINARY="$EDGE_BASE/opt/microsoft/msedge/msedge"
if [ ! -f "$EDGE_BINARY" ]; then
  echo "ERROR: Edge binary not found at $EDGE_BINARY"
  exit 1
fi

chmod +x "$EDGE_BINARY"
export EDGE_BINARY

echo "Installing EdgeDriver..."
wget -qO msedgedriver.zip "https://msedgedriver.azureedge.net/${EDGE_VERSION}/edgedriver_linux64.zip"
unzip -q msedgedriver.zip -d "$HOME/.local/edgedriver-${EDGE_VERSION}"

export WEBDRIVER_EDGE_DRIVER="$HOME/.local/edgedriver-${EDGE_VERSION}/msedgedriver"

echo "Edge setup complete:"
echo "EDGE_BINARY=$EDGE_BINARY"
echo "WEBDRIVER_EDGE_DRIVER=$WEBDRIVER_EDGE_DRIVER"


# ------------------------
# Run SBT tests
# ------------------------
ENVIRONMENT="${ENVIRONMENT:=local}"

echo "Starting tests with SBT..."
sbt clean \
  -Dbrowser="edge" \
  -Dwebdriver.edge.driver="$WEBDRIVER_EDGE_DRIVER" \
  -Dedge.binary="$EDGE_BINARY" \
  -Denvironment="$ENVIRONMENT" \
  "testOnly uk.gov.hmrc.test.ui.cucumber.runner.Runner"
