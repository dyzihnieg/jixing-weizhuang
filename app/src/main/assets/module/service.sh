#!/system/bin/sh
MODDIR=${0%/*}
# Bounded wait and retry; never install in post-fs-data's boot-critical phase.
n=0
while [ "$(getprop sys.boot_completed)" != 1 ]; do
  n=$((n + 1))
  [ "$n" -lt 120 ] || exit 0
  sleep 5
done
for delay in 0 15 30; do
  sleep "$delay"
  [ ! -f "$MODDIR/disable" ] && [ ! -f "$MODDIR/remove" ] || exit 0
  sh "$MODDIR/install-apk.sh" && exit 0
done
exit 0