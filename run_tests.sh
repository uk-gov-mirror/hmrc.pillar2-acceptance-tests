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
LATEST_DEB_URL=$(jq -r '
  .[] | select(.Product=="Stable") |
  .Releases[] | select(.Platform=="Linux" and .Architecture=="x64") |
  .Artifacts[] | select(.ArtifactName=="deb") |
  "\(.Location) \(.ProductVersion)"' "$TMP_JSON" | sort -V | tail -n1)

rm "$TMP_JSON"

if [ -z "$LATEST_DEB_URL" ]; then
  echo "Error: Could not find Edge .deb URL in JSON."
  exit 1
fi

DEB_URL=$(echo "$LATEST_DEB_URL" | awk '{print $1}')
LATEST_VERSION=$(echo "$LATEST_DEB_URL" | awk '{print $2}')

# Local install directory
EDGE_HOME="$HOME/.local/microsoft-edge-$LATEST_VERSION"
EDGE_BIN="$EDGE_HOME/usr/bin/microsoft-edge"

# Check if Edge is installed locally
if [ -x "$EDGE_BIN" ]; then
    echo "Microsoft Edge $LATEST_VERSION already installed locally."
else
    echo "Installing Microsoft Edge $LATEST_VERSION locally..."

    TMP_DEB="/tmp/$(basename $DEB_URL)"
    curl -L -o "$TMP_DEB" "$DEB_URL"

    mkdir -p "$EDGE_HOME"
    # Extract .deb contents
    ar x "$TMP_DEB" --output="$EDGE_HOME"
    tar -xf "$EDGE_HOME/data.tar.xz" -C "$EDGE_HOME"

    rm "$TMP_DEB" "$EDGE_HOME/control.tar.*" "$EDGE_HOME/data.tar.*" "$EDGE_HOME/debian-binary"

    echo "Microsoft Edge $LATEST_VERSION installed in $EDGE_HOME"
fi

# Add local Edge to PATH for this session
export PATH="$EDGE_HOME/usr/bin:$PATH"

echo "=== Starting UI tests ==="

ENV="local"
sbt clean -Dbrowser="edge" -Denvironment="${ENVIRONMENT:=local}" \
  "testOnly uk.gov.hmrc.test.ui.cucumber.runner.Runner" testReport
