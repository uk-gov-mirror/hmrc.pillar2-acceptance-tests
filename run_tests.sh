#!/bin/bash
set -e

# Directory to place the driver
DRIVER_DIR="$HOME/workspace/drivers"
mkdir -p "$DRIVER_DIR"
cd "$DRIVER_DIR"

# Fetch latest stable EdgeDriver version
LATEST_VERSION=$(curl -s https://msedgedriver.azureedge.net/LATEST_STABLE)
echo "Latest EdgeDriver version: $LATEST_VERSION"

# Download and unzip EdgeDriver
curl -L -o msedgedriver.zip "https://msedgedriver.azureedge.net/${LATEST_VERSION}/edgedriver_linux64.zip"
unzip -o msedgedriver.zip
chmod +x msedgedriver

# Clean up
rm msedgedriver.zip

echo "EdgeDriver $LATEST_VERSION is ready at $DRIVER_DIR/msedgedriver"