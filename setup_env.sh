#!/bin/bash
set -e
export http_proxy=http://127.0.0.1:18080
export https_proxy=http://127.0.0.1:18080

echo "=== 1. Download JDK 21 ==="
mkdir -p /opt/jdk
cd /opt/jdk
curl -sSL -o jdk21.tar.gz "https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse"
tar -xzf jdk21.tar.gz
rm jdk21.tar.gz
ls -d /opt/jdk/jdk-21*
echo "=== JDK DONE ==="

echo "=== 2. Download Android cmdline-tools ==="
mkdir -p /opt/android-sdk/cmdline-tools
cd /opt/android-sdk/cmdline-tools
curl -sSL -o tools.zip "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
unzip -q tools.zip
mv cmdline-tools latest
rm tools.zip
echo "=== CMDLINE-TOOLS DONE ==="

echo "=== 3. Install SDK packages ==="
export JAVA_HOME=$(ls -d /opt/jdk/jdk-21*)
SDKMGR=/opt/android-sdk/cmdline-tools/latest/bin/sdkmanager
yes | $SDKMGR --licenses > /dev/null 2>&1 || true
$SDKMGR --proxy=http --proxy_host=127.0.0.1 --proxy_port=18080 "platform-tools" "platforms;android-35" "build-tools;35.0.0" > /dev/null
echo "=== SDK-DONE ==="
