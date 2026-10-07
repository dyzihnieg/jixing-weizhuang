# Device Mask 1.4.0 测试版

作者：dyzihnieg。

移除导出模块按钮，增加底部“配置 / 关于”导航。关于页包含版本、作者和基本环境信息。

LSPosed：启用屏幕或 SoC 时未就绪会尝试打开独立管理器 org.lsposed.manager。寄生管理器需从通知或快捷方式打开。当前使用 legacy Xposed API，不提供原生作用域申请弹窗，不支持自动授予作用域；仍需手动勾选控制 APK 自检和目标应用。不要将管理器跳转当成授权成功。

CPU：保留全局 ro.soc.* 修改，增加目标进程内 Build.SOC_MODEL/SOC_MANUFACTURER 字段替换。需要 LSPosed 注入与共享配置可用，以及 Android 支持对应字段。开关或机型变更会同步应用内 SoC 配置，强制停止并重新打开目标应用生效。全局属性仍需应用配置并重启。不覆盖 /proc/cpuinfo、native、GPU、频率、核心数或硬件证明，尚未真机验证识别效果。

安装 device-mask-v1.4.0.zip，控制 APK 自动安装逻辑保留，失败查看 /data/adb/device_mask/apk-install.log。

验证：assembleDebug 通过，最终 ZIP CRC 和内置 APK 内容校验通过。未进行真机 UI、LSPosed 作用域、跨进程共享配置和 CPU 检测验证。