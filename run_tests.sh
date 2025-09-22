#!/usr/bin/env bash
set -euo pipefail

ENV="${ENVIRONMENT:=local}"
EDGE_BIN="/usr/bin/microsoft-edge"

# Function to install Microsoft Edge if missing
install_edge_browser() {
  echo "Checking for Microsoft Edge browser..."
  if ! command -v microsoft-edge &> /dev/null; then
    echo "Microsoft Edge not found. Installing..."
    wget -q https://packages.microsoft.com/keys/microsoft.asc -O- | sudo apt-key add -
    sudo add-apt-repository "deb [arch=amd64] https://packages.microsoft.com/repos/edge stable main"
    sudo apt-get update
    sudo apt-get install -y microsoft-edge-stable
  else
    echo "Microsoft Edge is already installed."
  fi
}

# Function to install matching EdgeDriver
install_edge_driver() {
  echo "Detecting Microsoft Edge version..."
  EDGE_VERSION=$("$EDGE_BIN" --version | awk '{print $3}')
  echo "Installed Edge version: $EDGE_VERSION"

  DRIVER_BASE_URL="https://msedgedriver.azureedge.net"
  DRIVER_URL="$DRIVER_BASE_URL/${EDGE_VERSION}/edgedriver_linux64.zip"

  echo "Downloading matching EdgeDriver from: $DRIVER_URL"
  wget -q "$DRIVER_URL" -O edgedriver.zip

  echo "Extracting EdgeDriver..."
  unzip -o edgedriver.zip -d edgedriver
  sudo mv edgedriver/msedgedriver /usr/local/bin/msedgedriver
  sudo chmod +x /usr/local/bin/msedgedriver
  rm -rf edgedriver.zip edgedriver

  echo "EdgeDriver installed successfully at /usr/local/bin/msedgedriver"
}

# --- MAIN EXECUTION ---
install_edge_browser
install_edge_driver

echo "Running sbt tests..."
sbt clean -Dbrowser="edge" -Denvironment="${ENV}" \
  "testOnly uk.gov.hmrc.test.ui.cucumber.runner.Runner" testReport
