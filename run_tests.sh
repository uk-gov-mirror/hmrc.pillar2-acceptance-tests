#!/usr/bin/env bash
set -euo pipefail

WORKSPACE="${WORKSPACE:-$HOME/workspace}"
EDGE_DIR="$WORKSPACE/microsoft-edge"
DRIVER_BIN="$WORKSPACE/msedgedriver"

mkdir -p "$WORKSPACE" "$EDGE_DIR"

LATEST_EDGE_VERSION="140.0.3485.81" # fallback hardcoded version

# 1. Check if Microsoft URL is reachable
MICROSOFT_URL="https://msedgedriver.microsoft.com/140.0.3485.81/edgedriver_linux64.zip"

echo "Checking network access to Microsoft EdgeDriver..."
if curl -s --head --connect-timeout 5 "$MICROSOFT_URL" | grep "200 OK" > /dev/null; then
    echo "✅ Network access to Microsoft confirmed."
    NETWORK_OK=true
else
    echo "❌ Unable to reach Microsoft EdgeDriver site - likely a network block."
    NETWORK_OK=false
fi

# 2. Choose source URL
if [ "$NETWORK_OK" = true ]; then
    DRIVER_URL="$MICROSOFT_URL"
else
    echo "Falling back to internal artefact repository..."
    DRIVER_URL="https://artefacts.tax.service.gov.uk/msedgedriver/140.0.3485.81/edgedriver_linux64.zip"
fi

# 3. Download EdgeDriver
if [ ! -f "$DRIVER_BIN" ]; then
    echo "Downloading EdgeDriver version: $LATEST_EDGE_VERSION"
    if ! wget --quiet --timeout=15 --tries=2 "$DRIVER_URL" -O "$WORKSPACE/edgedriver.zip"; then
        echo "ERROR: Unable to download EdgeDriver from either Microsoft or internal repo."
        exit 1
    fi

    unzip -o "$WORKSPACE/edgedriver.zip" -d "$WORKSPACE"
    chmod +x "$DRIVER_BIN"
    rm "$WORKSPACE/edgedriver.zip"
else
    echo "EdgeDriver already installed locally."
fi
