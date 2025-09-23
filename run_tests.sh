#!/bin/bash
set -e

echo "=== Setting up Microsoft Edge (user-space) and EdgeDriver for UI tests ==="

# --- Config ---
EDGE_VERSION="138.0.3351.95"
EDGE_INSTALL_DIR="$HOME/.local/microsoft-edge-$EDGE_VERSION"
DRIVER_DIR="$HOME/.local/edgedriver-$EDGE_VERSION"

# --- URLs ---
EDGE_DEB_URL="https://artefacts.tax.service.gov.uk/artifactory/edge-browser/pool/main/m/microsoft-edge-stable/microsoft-edge-stable_${EDGE_VERSION}-1_amd64.deb"
EDGEDRIVER_ZIP_URL="https://artefacts.tax.service.gov.uk/artifactory/edge-driver/${EDGE_VERSION}/edgedriver_linux64.zip"

# --- Install Microsoft Edge ---
if [ -d "$EDGE_INSTALL_DIR" ]; then
    echo "Microsoft Edge $EDGE_VERSION already installed. Skipping."
else
    echo "Installing Microsoft Edge $EDGE_VERSION in user space..."
    TMP_DEB="/tmp/microsoft-edge-$EDGE_VERSION.deb"
    curl -L -o "$TMP_DEB" "$EDGE_DEB_URL"

    mkdir -p "$EDGE_INSTALL_DIR"
    dpkg-deb -x "$TMP_DEB" "$EDGE_INSTALL_DIR"
    echo "Installed Edge in $EDGE_INSTALL_DIR"
fi

# Add Edge binary to PATH
export PATH="$EDGE_INSTALL_DIR/usr/bin:$PATH"

# --- Install EdgeDriver ---
if [ -d "$DRIVER_DIR" ]; then
    echo "EdgeDriver $EDGE_VERSION already installed. Skipping."
else
    echo "Installing EdgeDriver $EDGE_VERSION..."
    TMP_ZIP="/tmp/edgedriver_linux64_$EDGE_VERSION.zip"
    curl -L -o "$TMP_ZIP" "$EDGEDRIVER_ZIP_URL"

    mkdir -p "$DRIVER_DIR"
    unzip -o "$TMP_ZIP" -d "$DRIVER_DIR"
    chmod +x "$DRIVER_DIR/msedgedriver"
    echo "Installed EdgeDriver in $DRIVER_DIR"
fi

# Add EdgeDriver to PATH
export PATH="$DRIVER_DIR:$PATH"
export WEBDRIVER_EDGE_DRIVER="$DRIVER_DIR/msedgedriver"

echo "=== Starting UI tests ==="

# Environment for SBT
ENVIRONMENT="${ENVIRONMENT:=local}"

# Run SBT tests (change Runner class if needed)
sbt clean -Dbrowser="edge" -Denvironment="$ENVIRONMENT" "testOnly uk.gov.hmrc.test.ui.cucumber.runner.Runner"

echo "=== UI tests completed ==="
