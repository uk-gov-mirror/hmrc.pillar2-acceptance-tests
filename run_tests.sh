#!/usr/bin/env bash
set -euo pipefail

# --- Step 1: Verify or install Microsoft Edge ---
echo "DEBUG: Checking for Edge binary..."
if ! which microsoft-edge >/dev/null 2>&1; then
  echo "Microsoft Edge not found. Installing..."
  apt-get update
  apt-get install -y microsoft-edge-stable
else
  echo "Microsoft Edge is already installed."
fi

EDGE_VERSION=$(microsoft-edge --version | awk '{print $3}')
echo "Detected Edge version: $EDGE_VERSION"

# --- Step 2: Download matching EdgeDriver ---
DRIVER_DIR="$HOME/workspace/drivers"
mkdir -p "$DRIVER_DIR"
cd "$DRIVER_DIR"

curl -L -o msedgedriver.zip "https://msedgedriver.microsoft.com/${EDGE_VERSION}/edgedriver_linux64.zip"
unzip -o msedgedriver.zip
chmod +x msedgedriver
rm msedgedriver.zip

export PATH="$DRIVER_DIR:$PATH"
echo "EdgeDriver ready at $DRIVER_DIR/msedgedriver"

# --- Step 3: Run tests ---
ENVIRONMENT="${ENVIRONMENT:=local}"
echo "Running tests with browser=edge environment=$ENVIRONMENT"

sbt clean \
  -Dbrowser="edge" \
  -Denvironment="$ENVIRONMENT" \
  "testOnly uk.gov.hmrc.test.ui.cucumber.runner.Runner" testReport
