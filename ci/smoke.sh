#!/usr/bin/env bash
# 在模拟器里安装并打开应用，检查三种情况下都能正常显示：
#   1. 联网打开
#   2. 断网后重新打开（靠网站自己的离线缓存）
#   3. 清空应用数据后断网打开（靠安装包里的离线备份）
# 每种情况截图，并确认应用仍在运行、没有崩溃。
set -x
PKG=com.browndli.wanderchina
mkdir -p ci-out
fail=0

check_running() {
  if adb shell pidof "$PKG" > "ci-out/pid-$1.txt"; then echo "$1: 运行中" >> ci-out/result.txt
  else echo "$1: 没有在运行" >> ci-out/result.txt; fail=1; fi
}

adb install -r WanderChina.apk 2>&1 | tee ci-out/install.txt
adb logcat -c

adb shell am start -W -n "$PKG/.MainActivity" 2>&1 | tee ci-out/start-1.txt
sleep 30
adb exec-out screencap -p > ci-out/1-online.png
check_running 1-online

adb shell svc wifi disable || true
adb shell svc data disable || true
sleep 3
adb shell am force-stop "$PKG"
sleep 2
adb shell am start -W -n "$PKG/.MainActivity" 2>&1 | tee ci-out/start-2.txt
sleep 12
adb exec-out screencap -p > ci-out/2-offline-cache.png
check_running 2-offline-cache

adb shell am force-stop "$PKG"
adb shell pm clear "$PKG"
sleep 2
adb shell am start -W -n "$PKG/.MainActivity" 2>&1 | tee ci-out/start-3.txt
sleep 12
adb exec-out screencap -p > ci-out/3-offline-bundled.png
check_running 3-offline-bundled

adb logcat -d > ci-out/logcat.txt
grep -E "chromium|Console|Uncaught|FATAL|AndroidRuntime" ci-out/logcat.txt > ci-out/logcat-app.txt || true
if grep -q "FATAL EXCEPTION" ci-out/logcat.txt; then echo "发现崩溃" >> ci-out/result.txt; fail=1; fi
[ "$fail" = 0 ] && echo "全部通过" >> ci-out/result.txt
cat ci-out/result.txt
exit "$fail"
