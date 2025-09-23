#!/bin/bash
set -euo pipefail

echo "=== Setting up Microsoft Edge and EdgeDriver for UI tests ==="

# ------------------------
# Versions
# ------------------------
EDGE_VERSION="138.0.3351.95"   # update to the desired version
EDGEDRIVER_VERSION="$EDGE_VERSION"  # usually EdgeDriver version matches Edge version

# Direct URLs
EDGE_DEB_URL="https://artefacts.tax.service.gov.uk/artifactory/edge-browser/pool/main/m/microsoft-edge-stable/microsoft-edge-stable_${EDGE_VERSION}-1_amd64.deb"
EDGEDRIVER_ZIP_URL="https://artefacts.tax.service.gov.uk/artifactory/edge-driver/${EDGEDRIVER_VERSION}/edgedriver_linux64.zip"

# Install directories
INSTALL_BASE="$HOME/.local"
EDGE_INSTALL_DIR="$INSTALL_BASE/microsoft-edge-$EDGE_VERSION"
DRIVER_INSTALL_DIR="$INSTALL_BASE/edgedriver-$EDGE_VERSION"

# ------------------------
# Clean up old versions
# ------------------------
echo "Cleaning up old versions..."
find "$INSTALL_BASE" -maxdepth 1 -type d -name "microsoft-edge-*" ! -name "microsoft-edge-$EDGE_VERSION" -exec rm -rf {} +
find "$INSTALL_BASE" -maxdepth 1 -type d -name "edgedriver-*" ! -name "edgedriver-$EDGE_VERSION" -exec rm -rf {} +

mkdir -p "$EDGE_INSTALL_DIR" "$DRIVER_INSTALL_DIR"

# ------------------------
# Install Edge
# ------------------------
if [ -f "$EDGE_INSTALL_DIR/usr/bin/microsoft-edge" ]; then
    echo "Microsoft Edge $EDGE_VERSION already installed."
else
    echo "Installing Microsoft Edge $EDGE_VERSION..."
    TMP_DEB="/tmp/microsoft-edge-${EDGE_VERSION}.deb"
    curl -sSL -o "$TMP_DEB" "$EDGE_DEB_URL"
    dpkg-deb -x "$TMP_DEB" "$EDGE_INSTALL_DIR"
    rm "$TMP_DEB"
    chmod +x "$EDGE_INSTALL_DIR/usr/bin/microsoft-edge"
    echo "Microsoft Edge installed at $EDGE_INSTALL_DIR"
fi

# ------------------------
# Install EdgeDriver
# ------------------------
if [ -f "$DRIVER_INSTALL_DIR/msedgedriver" ]; then
    echo "EdgeDriver $EDGEDRIVER_VERSION already installed."
else
    echo "Installing EdgeDriver $EDGEDRIVER_VERSION..."
    TMP_ZIP="/tmp/edgedriver_linux64_${EDGEDRIVER_VERSION}.zip"
    curl -sSL -o "$TMP_ZIP" "$EDGEDRIVER_ZIP_URL"
    unzip -o "$TMP_ZIP" -d "$DRIVER_INSTALL_DIR"
    rm "$TMP_ZIP"
    chmod +x "$DRIVER_INSTALL_DIR/msedgedriver"
    echo "EdgeDriver installed at $DRIVER_INSTALL_DIR"
fi

# ------------------------
# Export environment variables for Selenium
# ------------------------
export EDGE_BINARY="$EDGE_INSTALL_DIR/usr/bin/microsoft-edge"
export WEBDRIVER_EDGE_DRIVER="$DRIVER_INSTALL_DIR/msedgedriver"
export PATH="$DRIVER_INSTALL_DIR:$EDGE_INSTALL_DIR/usr/bin:$PATH"

echo "Environment configured:"
echo "  EDGE_BINARY=$EDGE_BINARY"
echo "  WEBDRIVER_EDGE_DRIVER=$WEBDRIVER_EDGE_DRIVER"
echo "  PATH=$PATH"

echo "=== Edge setup complete ==="

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
