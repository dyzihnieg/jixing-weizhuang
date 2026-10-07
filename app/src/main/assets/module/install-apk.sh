#!/system/bin/sh
MODDIR=${0%/*}
STATE=/data/adb/device_mask
APK=$MODDIR/companion.apk
umask 077
mkdir -p "$STATE" || exit 1
[ -f "$APK" ] || { echo 'Companion APK missing' > "$STATE/apk-install.log"; exit 1; }
HASH=$(sha256sum "$APK" | cut -d ' ' -f 1)
[ -n "$HASH" ] || exit 1
if [ "$(cat "$STATE/apk-installed.sha256" 2>/dev/null)" = "$HASH" ] && pm path com.java.myapplication 2>/dev/null | grep -q '^package:'; then
  exit 0
fi
# Package manager cannot reliably read an APK from a module's private directory.
STAGE=/data/local/tmp/device-mask-install-$$.apk
trap 'rm -f "$STAGE"' EXIT HUP INT TERM
cp "$APK" "$STAGE" || exit 1
chmod 0644 "$STAGE"
RESULT=$(pm install -r --user 0 "$STAGE" 2>&1)
CODE=$?
printf '%s\n' "$RESULT" > "$STATE/apk-install.log"
if [ "$CODE" = 0 ] && printf '%s\n' "$RESULT" | grep -q '^Success'; then
  printf '%s\n' "$HASH" > "$STATE/apk-installed.sha256"
  exit 0
fi
exit 1