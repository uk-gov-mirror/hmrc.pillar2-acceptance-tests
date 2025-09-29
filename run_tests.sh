#!/bin/bash
set -euo pipefail

# --- Versions ---
EDGE_VERSION="138.0.3351.95"
EDGE_INSTALL_BASE="$HOME/.local"
EDGE_INSTALL_DIR="$EDGE_INSTALL_BASE/microsoft-edge-$EDGE_VERSION"
DRIVER_DIR="$EDGE_INSTALL_BASE/edgedriver-$EDGE_VERSION"

# --- Remove old Edge installations ---
echo "Removing old Edge installations..."
for dir in "$EDGE_INSTALL_BASE"/microsoft-edge-*; do
    if [ "$dir" != "$EDGE_INSTALL_DIR" ]; then
        echo "Deleting $dir"
        rm -rf "$dir"
    fi
done

echo "Cleaning old Edge processes and profiles..."
pkill -f "msedgedriver" || true
pkill -f "msedge" || true
rm -rf /tmp/edge-profile-*

# --- Detect locked Edge user data directory ---
EDGE_PROFILE_BASE="/tmp/edge-profile-check"
LOCK_FILE="$EDGE_PROFILE_BASE/SingletonLock"

echo "Checking for existing Edge user data lock in: $EDGE_PROFILE_BASE"
if [ -f "$LOCK_FILE" ]; then
    echo "ERROR: Edge user data directory lock file detected: $LOCK_FILE"
    echo "Attempting to clean up..."
    pkill -f "msedge" || true
    pkill -f "msedgedriver" || true
    sleep 2

    if [ -f "$LOCK_FILE" ]; then
        echo "Lock file still exists after killing processes. Removing manually."
        rm -f "$LOCK_FILE"
    fi
else
    echo "No lock detected. Edge profile directory is free to use."
fi

# --- Remove old EdgeDriver installations ---
echo "Removing old EdgeDriver installations..."
for dir in "$EDGE_INSTALL_BASE"/edgedriver-*; do
    if [ "$dir" != "$DRIVER_DIR" ]; then
        echo "Deleting $dir"
        rm -rf "$dir"
    fi
done

# --- Clear SeleniumManager cache ---
echo "Clearing SeleniumManager cache..."
rm -rf "$HOME/.cache/selenium"

# --- Download and extract Microsoft Edge ---
if [ ! -d "$EDGE_INSTALL_DIR" ]; then
    echo "Installing Microsoft Edge $EDGE_VERSION..."
    mkdir -p "$EDGE_INSTALL_DIR"
    curl -L -o /tmp/microsoft-edge.deb \
        "https://artefacts.tax.service.gov.uk/artifactory/edge-browser/pool/main/m/microsoft-edge-stable/microsoft-edge-stable_${EDGE_VERSION}-1_amd64.deb"
    dpkg-deb -x /tmp/microsoft-edge.deb "$EDGE_INSTALL_DIR"
fi

EDGE_BINARY="$EDGE_INSTALL_DIR/opt/microsoft/msedge/msedge"

# --- Download and extract EdgeDriver ---
if [ ! -d "$DRIVER_DIR" ]; then
    echo "Installing EdgeDriver $EDGE_VERSION..."
    mkdir -p "$DRIVER_DIR"
    curl -L -o /tmp/edgedriver.zip \
        "https://artefacts.tax.service.gov.uk/artifactory/edge-driver/${EDGE_VERSION}/edgedriver_linux64.zip"
    unzip /tmp/edgedriver.zip -d "$DRIVER_DIR"
    chmod +x "$DRIVER_DIR/msedgedriver"
fi

# --- Export environment variables ---
export EDGE_BINARY
export WEBDRIVER_EDGE_DRIVER="$DRIVER_DIR/msedgedriver"
export PATH="$DRIVER_DIR:$(dirname "$EDGE_BINARY"):$PATH"

# --- Include selenium-devtools-v138 in SBT classpath ---
export DEVTOOLS_JAR="$HOME/.ivy2/cache/org.seleniumhq.selenium/selenium-devtools-v138/jars/selenium-devtools-v138-4.35.0.jar"
export SBT_OPTS="-cp $DEVTOOLS_JAR ${SBT_OPTS:-}"

echo "Edge and EdgeDriver $EDGE_VERSION installed successfully."
echo "=== Starting UI tests ==="

ENVIRONMENT="${ENVIRONMENT:=local}"

# --- Check Edge profile lock ---
PROFILE_DIR="/tmp/edge-profile-check"

echo "Checking for existing Edge user data lock in: $PROFILE_DIR"

if [ -f "$PROFILE_DIR/SingletonLock" ]; then
    echo "WARNING: Detected existing SingletonLock file."
    ls -lh "$PROFILE_DIR/SingletonLock"

    echo "Checking which process (if any) holds the lock..."
    if lsof "$PROFILE_DIR/SingletonLock"; then
        echo "Lock is currently held by an active process!"
        echo "Listing processes for msedge/msedgedriver:"
        ps -ef | grep -E 'msedge|msedgedriver' || true
        echo "Please investigate before continuing."
        exit 1
    else
        echo "No process owns the lock — stale lock detected. Cleaning up..."
        rm -rf "$PROFILE_DIR"
    fi
else
    echo "No lock detected. Edge profile directory is free to use."
fi

# Ensure clean profile directory exists before starting
mkdir -p "$PROFILE_DIR"

# --- Run SBT tests ---
sbt clean \
    -Dbrowser="edge" \
    -Dwebdriver.edge.driver="$WEBDRIVER_EDGE_DRIVER" \
    -Denvironment="$ENVIRONMENT" \
    "testOnly uk.gov.hmrc.test.ui.cucumber.runner.Runner"
