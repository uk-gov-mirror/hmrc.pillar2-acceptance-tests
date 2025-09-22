#!/bin/bash
set -e

# =============================
# CONFIG
# =============================
EDGE_VERSION="126"
DRIVER_DIR="$(pwd)/drivers"
DRIVER_BINARY="$DRIVER_DIR/msedgedriver"

# =============================
# PREP EDGE DRIVER
# =============================
mkdir -p "$DRIVER_DIR"

# Download EdgeDriver if not already present
if [ ! -f "$DRIVER_BINARY" ]; then
    echo "Downloading EdgeDriver version $EDGE_VERSION..."
    ZIP_FILE="$DRIVER_DIR/msedgedriver-$EDGE_VERSION-linux64.zip"
    curl -L -o "$ZIP_FILE" "https://artefacts.tax.service.gov.uk/artifactory/edge-driver/msedgedriver-$EDGE_VERSION-linux64.zip"
    unzip -o "$ZIP_FILE" -d "$DRIVER_DIR"
    chmod +x "$DRIVER_BINARY"
fi

# Add driver to PATH so Selenium picks it up
export PATH="$DRIVER_DIR:$PATH"
echo "Using EdgeDriver from: $(which msedgedriver)"

# =============================
# RUN TESTS
# =============================
echo "Starting tests..."
./run_tests.sh edge local
