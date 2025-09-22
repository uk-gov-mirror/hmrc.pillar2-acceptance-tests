#!/usr/bin/env bash
set -euo pipefail

# ---- Step 0: Environment ----
ENVIRONMENT="${ENVIRONMENT:=local}"   # default to 'local' if not set
echo "Running acceptance tests in environment: $ENVIRONMENT"

# ---- Step 1: Setup EdgeDriver ----
DRIVER_DIR="$HOME/workspace/drivers"
mkdir -p "$DRIVER_DIR"
cd "$DRIVER_DIR"

# Detect installed Edge version
EDGE_VERSION=$(microsoft-edge --version | awk '{print $3}')
MAJOR_VERSION=$(echo $EDGE_VERSION | cut -d. -f1)
echo "Installed Edge version: $EDGE_VERSION (major: $MAJOR_VERSION)"

# Download matching EdgeDriver
DRIVER_VERSION=$(curl -s "https://msedgedriver.azureedge.net/LATEST_RELEASE_$MAJOR_VERSION")
echo "EdgeDriver version to download: $DRIVER_VERSION"

curl -L -o msedgedriver.zip "https://msedgedriver.azureedge.net/${DRIVER_VERSION}/edgedriver_linux64.zip"
unzip -o msedgedriver.zip
chmod +x msedgedriver
rm msedgedriver.zip
export PATH="$DRIVER_DIR:$PATH"

echo "EdgeDriver $DRIVER_VERSION is ready at $DRIVER_DIR/msedgedriver"

# ---- Step 2: Verify SBT ----
echo "Checking sbt installation..."
which sbt
sbt sbtVersion

# ---- Step 3: Run acceptance tests ----
echo "Running acceptance tests..."
sbt clean -Dbrowser="edge" -Denvironment="${ENVIRONMENT}" "testOnly uk.gov.hmrc.test.ui.cucumber.runner.Runner" testReport | tee sbt.log

# ---- Step 4: Confirm reports ----
if [ -d "target/test-reports" ]; then
  echo "Test reports generated successfully."
else
  echo "WARNING: No test reports found!"
fi
