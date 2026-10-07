SKIPUNZIP=0
ui_print '- Device Mask: offline identity profiles'
ui_print '- Magisk / KernelSU / APatch; resetprop required'
ui_print '- Default disabled. Use the companion app to configure.'
set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm "$MODPATH/post-fs-data.sh" 0 0 0755
set_perm "$MODPATH/action.sh" 0 0 0755
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/install-apk.sh" 0 0 0755
if [ "$BOOTMODE" = true ] && [ "$(getprop sys.boot_completed)" = 1 ]; then
  ui_print '- Installing companion APK for owner user...'
  if sh "$MODPATH/install-apk.sh"; then
    ui_print '- Companion APK installed'
  else
    ui_print '- Installation deferred; will retry after reboot'
  fi
else
  ui_print '- Companion APK will be installed after reboot'
fi
