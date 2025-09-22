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

if [ -z "$LATEST_DEB_INFO" ]; then
  echo "Error: Could not find Edge .deb URL in JSON."
  exit 1
fi

DEB_URL=$(echo "$LATEST_DEB_INFO" | awk '{print $1}')
LATEST_VERSION=$(echo "$LATEST_DEB_INFO" | awk '{print $2}')

# User-space install directory
EDGE_HOME="$HOME/.local/microsoft-edge-$LATEST_VERSION"
mkdir -p "$EDGE_HOME"

# Check if Edge is already installed locally
if [ -x "$EDGE_HOME/usr/bin/microsoft-edge" ]; then
    INSTALLED_VERSION=$("$EDGE_HOME/usr/bin/microsoft-edge" --version | awk '{print $3}')
    if [ "$INSTALLED_VERSION" = "$LATEST_VERSION" ]; then
        echo "Microsoft Edge $INSTALLED_VERSION is already installed in user space. Skipping installation."
    else
        echo "Updating Microsoft Edge to $LATEST_VERSION in user space."
        INSTALL_EDGE=true
    fi
else
    echo "Installing Microsoft Edge $LATEST_VERSION in user space."
    INSTALL_EDGE=true
fi

if [ "$INSTALL_EDGE" = true ]; then
    # Download the .deb to /tmp
    DEB_FILE="/tmp/$(basename $DEB_URL)"
    curl -L -o "$DEB_FILE" "$DEB_URL"

    # Extract .deb locally
    cd "$EDGE_HOME"
    ar x "$DEB_FILE"  # extracts control.tar.* and data.tar.*
    tar -xf data.tar.*  # unpack Edge files
    rm -f control.tar.* data.tar.* "$DEB_FILE"

    echo "Microsoft Edge $LATEST_VERSION installed successfully in $EDGE_HOME."
fi

# Add Edge to PATH
export PATH="$EDGE_HOME/usr/bin:$PATH"

echo "=== Starting UI tests ==="

ENV="local"
sbt clean -Dbrowser="edge" -Denvironment="${ENVIRONMENT:=local}" \
    "testOnly uk.gov.hmrc.test.ui.cucumber.runner.Runner" testReport
