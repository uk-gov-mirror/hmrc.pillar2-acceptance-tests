#!/bin/bash
set -e

# --- Step 1: Remove old Edge and EdgeDriver ---
echo "Removing old Edge installations..."
rm -rf /home/jenkins/.local/microsoft-edge-* /home/jenkins/.local/edgedriver-*

# --- Step 2: Download and install Edge 138 ---
EDGE_VERSION=138.0.3351.95
EDGE_DIR=/home/jenkins/.local/microsoft/msedge-$EDGE_VERSION
EDGEDRIVER_DIR=/home/jenkins/.local/edgedriver-$EDGE_VERSION

echo "Installing Microsoft Edge $EDGE_VERSION..."
mkdir -p $EDGE_DIR
curl -L "https://msedgedriver.azureedge.net/$EDGE_VERSION/edgedriver_linux64.zip" -o /tmp/edgedriver.zip
unzip /tmp/edgedriver.zip -d $EDGEDRIVER_DIR
curl -L "https://msedge.sf.dl.delivery.mp.microsoft.com/filestreamingservice/files/$(curl -s https://msedgedriver.azureedge.net/LATEST_RELEASE_$EDGE_VERSION)/MicrosoftEdge-linux-x64-$EDGE_VERSION.tar.gz" \
     -o /tmp/edge.tar.gz
mkdir -p $EDGE_DIR
tar -xzf /tmp/edge.tar.gz -C $EDGE_DIR

# --- Step 3: Set environment variables for Selenium ---
export EDGE_BINARY=$EDGE_DIR/msedge
export WEBDRIVER_EDGE_DRIVER=$EDGEDRIVER_DIR/msedgedriver
export PATH=$EDGEDRIVER_DIR:$EDGE_DIR:$PATH

# --- Step 4: Create unique Edge profile directory ---
USER_DATA_DIR="/tmp/edge-profile-$(date +%s%N)"
mkdir -p $USER_DATA_DIR
export EDGE_USER_DATA_DIR=$USER_DATA_DIR

# --- Step 5: Run SBT tests ---
sbt -Dwebdriver.edge.driver=$WEBDRIVER_EDGE_DRIVER \
    -Dedge.binary=$EDGE_BINARY \
    "testOnly uk.gov.hmrc.test.ui.cucumber.runner"
