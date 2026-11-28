#!/bin/bash
STATUS=/workspace/build_status.txt
echo "START $(date +%H:%M:%S)" > $STATUS
export http_proxy=http://127.0.0.1:18080
export https_proxy=http://127.0.0.1:18080

# 1. JDK 21
if [ ! -d /opt/jdk ]; then
  mkdir -p /opt/jdk
  cd /opt/jdk
  curl -sSL -o jdk21.tar.gz "https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse" || { echo "JDK-FAIL" >> $STATUS; exit 1; }
  tar -xzf jdk21.tar.gz && rm jdk21.tar.gz
fi
echo "JDK-DONE $(date +%H:%M:%S)" >> $STATUS

# 2. Android SDK cmdline-tools
if [ ! -d /opt/android-sdk/cmdline-tools/latest ]; then
  mkdir -p /opt/android-sdk/cmdline-tools
  cd /opt/android-sdk/cmdline-tools
  curl -sSL -o tools.zip "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" || { echo "TOOLS-FAIL" >> $STATUS; exit 1; }
  unzip -q tools.zip && mv cmdline-tools latest && rm tools.zip
fi
echo "TOOLS-DONE $(date +%H:%M:%S)" >> $STATUS

# 3. SDK packages
export JAVA_HOME=$(ls -d /opt/jdk/jdk-21*)
SDKMGR=/opt/android-sdk/cmdline-tools/latest/bin/sdkmanager
if [ ! -d /opt/android-sdk/platforms/android-35 ]; then
  yes | $SDKMGR --licenses > /dev/null 2>&1
  $SDKMGR --proxy=http --proxy_host=127.0.0.1 --proxy_port=18080 "platform-tools" "platforms;android-35" "build-tools;35.0.0" > /dev/null || { echo "SDK-FAIL" >> $STATUS; exit 1; }
fi
echo "SDK-DONE $(date +%H:%M:%S)" >> $STATUS

# 4. Gradle build
export ANDROID_HOME=/opt/android-sdk
export PATH=$JAVA_HOME/bin:$PATH
cd /workspace
rm -rf /workspace/app/build
gradle :app:assembleDebug --no-daemon --max-workers=2 \
  -Dkotlin.compiler.execution.strategy=in-process \
  -Dorg.gradle.jvmargs="-Xmx1536m -XX:MaxMetaspaceSize=384m -Dfile.encoding=UTF-8" \
  -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=18080 \
  -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=18080 > /workspace/build_log.txt 2>&1
GRADLE_EC=$?
echo "GRADLE-EXIT=$GRADLE_EC $(date +%H:%M:%S)" >> $STATUS

# 5. Copy APK
if [ -f /workspace/app/build/outputs/apk/debug/app-debug.apk ]; then
  cp /workspace/app/build/outputs/apk/debug/app-debug.apk /workspace/Z-BEDOO-v1.0-debug.apk
  echo "APK-READY $(date +%H:%M:%S)" >> $STATUS
else
  echo "APK-MISSING" >> $STATUS
  tail -30 /workspace/build_log.txt >> $STATUS
fi
echo "ALL-DONE" >> $STATUS
