#!/bin/bash
set -e

echo "=== Setting up Microsoft Edge (user-space) and EdgeDriver for UI tests ==="

# Versions
EDGE_VERSION="138.0.3351.95"

# Direct URLs
EDGE_DEB_URL="https://artefacts.tax.service.gov.uk/artifactory/edge-browser/pool/main/m/microsoft-edge-stable/microsoft-edge-stable_${EDGE_VERSION}-1_amd64.deb"
EDGEDRIVER_ZIP_URL="https://artefacts.tax.service.gov.uk/artifactory/edge-driver/${EDGE_VERSION}/edgedriver_linux64.zip"

# Install directories
EDGE_INSTALL_BASE="$HOME/.local"
DRIVER_BASE="$HOME/.local"

EDGE_INSTALL_DIR="$EDGE_INSTALL_BASE/microsoft-edge-$EDGE_VERSION"
DRIVER_DIR="$DRIVER_BASE/edgedriver-$EDGE_VERSION"

# ------------------------
# Clean up old Edge/Driver versions
# ------------------------
echo "Cleaning up old Edge versions..."
for dir in "$EDGE_INSTALL_BASE"/microsoft-edge-*; do
    [ "$dir" != "$EDGE_INSTALL_DIR" ] && rm -rf "$dir"
done

echo "Cleaning up old EdgeDriver versions..."
for dir in "$DRIVER_BASE"/edgedriver-*; do
    [ "$dir" != "$DRIVER_DIR" ] && rm -rf "$dir"
done

mkdir -p "$EDGE_INSTALL_DIR" "$DRIVER_DIR"

# ------------------------
# Install Edge
# ------------------------
if [ -d "$EDGE_INSTALL_DIR/usr" ]; then
    echo "Microsoft Edge $EDGE_VERSION already installed."
else
    echo "Installing Microsoft Edge $EDGE_VERSION..."
    TMP_DEB="/tmp/microsoft-edge-${EDGE_VERSION}.deb"
    curl -L -o "$TMP_DEB" "$EDGE_DEB_URL"
    dpkg-deb -x "$TMP_DEB" "$EDGE_INSTALL_DIR"
    rm "$TMP_DEB"
    echo "Microsoft Edge installed in $EDGE_INSTALL_DIR"
fi

# ------------------------
# Install EdgeDriver
# ------------------------
if [ -f "$DRIVER_DIR/msedgedriver" ]; then
    echo "EdgeDriver $EDGE_VERSION already installed."
else
    echo "Installing EdgeDriver $EDGE_VERSION..."
    TMP_ZIP="/tmp/edgedriver_linux64_${EDGE_VERSION}.zip"
    curl -L -o "$TMP_ZIP" "$EDGEDRIVER_ZIP_URL"
    unzip -o "$TMP_ZIP" -d "$DRIVER_DIR"
    rm "$TMP_ZIP"
    chmod +x "$DRIVER_DIR/msedgedriver"
    echo "EdgeDriver installed in $DRIVER_DIR"
fi

# ------------------------
# Set environment variables for Selenium
# ------------------------
export EDGE_BINARY="$EDGE_INSTALL_DIR/usr/bin/microsoft-edge"
export WEBDRIVER_EDGE_DRIVER="$DRIVER_DIR/msedgedriver"
export PATH="$DRIVER_DIR:$EDGE_INSTALL_DIR/usr/bin:$PATH"

echo "=== Starting UI tests ==="

ENVIRONMENT="${ENVIRONMENT:=local}"

# Run SBT tests
sbt clean -Dbrowser="edge" -Denvironment="$ENVIRONMENT" "testOnly uk.gov.hmrc.test.ui.cucumber.runner.Runner"