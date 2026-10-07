SKIPUNZIP=0
ui_print '- 机型伪装：离线机型配置'
ui_print '- 支持 Magisk / KernelSU / APatch；需要 resetprop'
ui_print '- 默认关闭，请在配套应用中配置。'
set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm "$MODPATH/post-fs-data.sh" 0 0 0755
set_perm "$MODPATH/action.sh" 0 0 0755
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/install-apk.sh" 0 0 0755
if [ "$BOOTMODE" = true ] && [ "$(getprop sys.boot_completed)" = 1 ]; then
  ui_print '- 正在为主用户安装配套应用……'
  if sh "$MODPATH/install-apk.sh"; then
    ui_print '- 配套应用已安装'
  else
    ui_print '- 安装暂缓，将在重启后重试'
  fi
else
  ui_print '- 配套应用将在重启后安装'
fi
