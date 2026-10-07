#!/system/bin/sh
MODDIR=${0%/*}
STATE=/data/adb/device_mask
umask 077
mkdir -p "$STATE" || exit 1
chmod 700 "$STATE"
RSP=$(command -v resetprop 2>/dev/null)
if [ -z "$RSP" ]; then
  for p in /data/adb/magisk/resetprop /data/adb/ksu/bin/resetprop /data/adb/ap/bin/resetprop; do
    if [ -x "$p" ]; then RSP=$p; break; fi
  done
fi
# Snapshot on every boot, before this module changes any properties.
: > "$STATE/original.tmp"
for pfx in ro.product ro.product.system ro.product.system_ext ro.product.product ro.product.vendor ro.product.odm; do
  for key in model marketname brand manufacturer; do
    value=$(getprop "$pfx.$key")
    printf '%s=%s\n' "$pfx.$key" "$value" >> "$STATE/original.tmp"
  done
done
for key in ro.soc.model ro.soc.manufacturer; do
  printf '%s=%s\n' "$key" "$(getprop "$key")" >> "$STATE/original.tmp"
done
mv -f "$STATE/original.tmp" "$STATE/original.props"
printf '%s\n' 'disabled' > "$STATE/status"
[ -f "$STATE/config" ] || exit 0
IFS='|' read -r ID SCOPE SOC < "$STATE/config"
SOC=${SOC:-off}
case "$SOC" in on|off) ;; *) printf '%s\n' 'error: invalid soc mode' > "$STATE/status"; exit 1 ;; esac
[ "$ID" = off ] && exit 0
case "$SCOPE" in full|min) ;; *) printf '%s\n' 'error: invalid scope' > "$STATE/status"; exit 1 ;; esac
FOUND=0
while IFS='|' read -r PID TYPE LABEL BRAND MAKER MODEL; do
  if [ "$PID" = "$ID" ]; then FOUND=1; break; fi
done < "$MODDIR/profiles.tsv"
[ "$FOUND" = 1 ] || { printf '%s\n' 'error: unknown profile' > "$STATE/status"; exit 1; }
SOC_VENDOR=""
SOC_MODEL=""
while IFS='|' read -r SID SOC_VENDOR_ROW SOC_MODEL_ROW; do
  if [ "$SID" = "$ID" ]; then SOC_VENDOR="$SOC_VENDOR_ROW"; SOC_MODEL="$SOC_MODEL_ROW"; break; fi
done < "$MODDIR/soc.tsv"
[ -n "$RSP" ] || { printf '%s\n' 'error: resetprop unavailable' > "$STATE/status"; exit 1; }
FAILED=0
apply_prop() {
  # Skip absent vendor properties; never invent a new partition identity.
  [ -n "$(getprop "$1")" ] || return 0
  "$RSP" -n "$1" "$2" || FAILED=1
}
for pfx in ro.product ro.product.system ro.product.system_ext ro.product.product ro.product.vendor ro.product.odm; do
  apply_prop "$pfx.model" "$MODEL"
  apply_prop "$pfx.marketname" "$LABEL"
  if [ "$SCOPE" = full ]; then
    apply_prop "$pfx.brand" "$BRAND"
    apply_prop "$pfx.manufacturer" "$MAKER"
  fi
done
if [ "$SOC" = on ]; then
  if [ -n "$SOC_MODEL" ] && [ -n "$SOC_VENDOR" ]; then
    apply_prop ro.soc.model "$SOC_MODEL"
    apply_prop ro.soc.manufacturer "$SOC_VENDOR"
  else
    FAILED=1
  fi
fi
if [ "$FAILED" = 0 ]; then
  printf 'applied: %s (%s)\n' "$ID" "$SCOPE" > "$STATE/status"
else
  # Roll back all existing values on a partial failure.
  while IFS='=' read -r key value; do
    [ -n "$value" ] && "$RSP" -n "$key" "$value"
  done < "$STATE/original.props"
  printf '%s\n' 'error: apply failed; rollback attempted' > "$STATE/status"
fi
