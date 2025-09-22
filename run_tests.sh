#!/bin/bash
set -e

echo "=== Setting up Microsoft Edge (user-space) for UI tests ==="

# Metadata URL (mirror)
MIRROR_URL="https://artefacts.tax.service.gov.uk/ui/api/v1/download/contentBrowsing/edge-browser/api/products/"

# Temporary file to store JSON
TMP_JSON=$(mktemp)

# Fetch JSON metadata
curl -s -o "$TMP_JSON" "$MIRROR_URL"

# Extract the latest Linux x64 stable .deb URL and version using jq
LATEST_DEB_INFO=$(jq -r '
  .[] | select(.Product=="Stable") |
  .Releases[] | select(.Platform=="Linux" and .Architecture=="x64") |
  .Artifacts[] | select(.ArtifactName=="deb") |
  "\(.Location) \(.ProductVersion)"' "$TMP_JSON" | sort -V | tail -n1)

rm "$TMP_JSON"

DEB_URL=$(echo "$LATEST_DEB_INFO" | awk '{print $1}')
LATEST_VERSION=$(echo "$LATEST_DEB_INFO" | awk '{print $2}')

if [ -z "$DEB_URL" ] || [ -z "$LATEST_VERSION" ]; then
    echo "Error: Could not determine Edge .deb URL or version from JSON."
    exit 1
fi

EDGE_INSTALL_DIR="$HOME/.local/microsoft-edge-$LATEST_VERSION"

if [ -d "$EDGE_INSTALL_DIR" ]; then
    echo "Microsoft Edge $LATEST_VERSION already installed in user space. Skipping installation."
else
    echo "Installing Microsoft Edge $LATEST_VERSION in user space..."

    TMP_DEB="/tmp/$(basename $DEB_URL)"
    curl -L -o "$TMP_DEB" "$DEB_URL"

    # Extract .deb in user space
    mkdir -p "$EDGE_INSTALL_DIR"
    dpkg-deb -x "$TMP_DEB" "$EDGE_INSTALL_DIR"

    echo "Microsoft Edge $LATEST_VERSION installed successfully in $EDGE_INSTALL_DIR."
fi

# Add Edge binary to PATH for this session
export PATH="$EDGE_INSTALL_DIR/usr/bin:$PATH"

echo "=== Starting UI tests ==="

ENV="local"

# Run sbt tests (without testReport)
sbt clean -Dbrowser="edge" -Denvironment="${ENVIRONMENT:=local}" "testOnly uk.gov.hmrc.test.ui.cucumber.runner.Runner"
