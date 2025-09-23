#!/bin/bash
set -e

EDGE_VERSION="138.0.3351.95"
EDGE_BASE_URL="https://artefacts.tax.service.gov.uk/artifactory/edge-browser/$EDGE_VERSION"
EDGE_DEB="microsoft-edge-${EDGE_VERSION}.deb"
EDGE_INSTALL_DIR="$HOME/.local/microsoft-edge-$EDGE_VERSION"

echo "=== Setting up Microsoft Edge (user-space) for UI tests ==="

if [ -d "$EDGE_INSTALL_DIR" ]; then
    echo "Microsoft Edge $EDGE_VERSION already installed in user space. Skipping installation."
else
    echo "Installing Microsoft Edge $EDGE_VERSION in user space..."

    TMP_DEB="/tmp/$EDGE_DEB"

    echo "Downloading Edge .deb from: $EDGE_BASE_URL/$EDGE_DEB"
    curl -L -o "$TMP_DEB" "$EDGE_BASE_URL/$EDGE_DEB"

    mkdir -p "$EDGE_INSTALL_DIR"
    dpkg-deb -x "$TMP_DEB" "$EDGE_INSTALL_DIR"

    echo "Microsoft Edge $EDGE_VERSION installed successfully in $EDGE_INSTALL_DIR."
fi


export PATH="$EDGE_INSTALL_DIR/usr/bin:$PATH"

echo "Installed Edge version:"
"$EDGE_INSTALL_DIR/usr/bin/microsoft-edge" --version || echo "Edge binary not found!"

echo "=== Starting UI tests ==="

ENV="local"

sbt clean -Dbrowser="edge" -Denvironment="${ENVIRONMENT:=local}" "testOnly uk.gov.hmrc.test.ui.cucumber.runner.Runner"
