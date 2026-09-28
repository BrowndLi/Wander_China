#!/usr/bin/env bash
# 在模拟器里安装并打开应用，分别在有网络和断网时截图，再检查是否崩溃。
set -x
PKG=com.browndli.wanderchina
mkdir -p ci-out
adb install -r WanderChina.apk 2>&1 | tee ci-out/install.txt
adb logcat -c
adb shell am start -W -n "$PKG/.MainActivity" 2>&1 | tee ci-out/start-online.txt
sleep 35
adb exec-out screencap -p > ci-out/1-online.png
adb shell pidof "$PKG" > ci-out/pid-online.txt || echo "not running" > ci-out/pid-online.txt

# 断网后重新打开，验证离线缓存
adb shell svc wifi disable || true
adb shell svc data disable || true
sleep 3
adb shell am force-stop "$PKG"
sleep 2
adb shell am start -W -n "$PKG/.MainActivity" 2>&1 | tee ci-out/start-offline.txt
sleep 20
adb exec-out screencap -p > ci-out/2-offline.png
adb shell pidof "$PKG" > ci-out/pid-offline.txt || echo "not running" > ci-out/pid-offline.txt

adb logcat -d > ci-out/logcat.txt
grep -E "chromium|Console|Uncaught|FATAL|AndroidRuntime" ci-out/logcat.txt > ci-out/logcat-app.txt || true
if grep -q "FATAL EXCEPTION" ci-out/logcat.txt; then
  echo "应用崩溃" | tee ci-out/result.txt
  exit 1
fi
if grep -q "not running" ci-out/pid-online.txt; then
  echo "应用没有保持运行" | tee ci-out/result.txt
  exit 1
fi
echo "通过" | tee ci-out/result.txt
exit 0
