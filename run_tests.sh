#!/usr/bin/env bash
set -euo pipefail

# ---- Step 1: Setup EdgeDriver ----
DRIVER_DIR="$HOME/workspace/drivers"
mkdir -p "$DRIVER_DIR"
cd "$DRIVER_DIR"

LATEST_VERSION=$(curl -s https://msedgedriver.azureedge.net/LATEST_STABLE)
echo "Latest EdgeDriver version: $LATEST_VERSION"

curl -L -o msedgedriver.zip "https://msedgedriver.azureedge.net/${LATEST_VERSION}/edgedriver_linux64.zip"
unzip -o msedgedriver.zip
chmod +x msedgedriver
rm msedgedriver.zip

echo "EdgeDriver $LATEST_VERSION is ready at $DRIVER_DIR/msedgedriver"

# Add to PATH so sbt can find it
export PATH="$DRIVER_DIR:$PATH"

# ---- Step 2: Run your acceptance tests ----
ENVIRONMENT="${ENVIRONMENT:=local}"   # default to 'local' if not set
sbt clean -Dbrowser="edge" -Denvironment="${ENVIRONMENT}" "testOnly uk.gov.hmrc.test.ui.cucumber.runner.Runner" testReport
