# Device Mask 1.6.0 屏幕兼容性测试版

保留 CPU 伪装逻辑，补充 Display.getSize/getRealSize/getWidth/getHeight 与 Android 11+ WindowMetrics.getBounds、Android 14+ WindowMetrics.getDensity。独立安装屏幕 Hook，单个接口失败不阻断全部屏幕 Hook。仍不修改真实分辨率或显示服务。

安装模块 ZIP 并重启，确认控制 APK 关于显示 1.6.0。开启屏幕开关，选与真实屏幕分辨率不同的机型，确认目标检测软件在 LSPosed 作用域内，强制停止并重新打开目标检测软件。

LSPosed 日志中 DeviceMask screen config 行显示目标包名、屏幕配置开关及宽高。新增尺寸/WindowMetrics 查询首次命中记录 DeviceMask screen query hit。没有命中日志不代表 Metrics 接口没有执行，因为旧 Metrics 接口尚不记录命中。失败日志标记接口名称。

WindowMetrics 会影响目标应用用于布局的窗口尺寸判断，多窗口、横屏、外接屏和折叠屏尤其可能出现布局问题；关闭屏幕开关并重启目标应用恢复。仍不覆盖原生接口，也不保证英寸数和原厂物理 DPI 精确匹配。

APK 编译及 ZIP CRC、内置 APK 一致性校验通过；尚未真实 LSPosed 和目标检测软件验证。需要提供软件名称、版本、未变化的指标及日志来继续定位。

作者：dyzihnieg。