# Device Mask 1.2.0 测试版

本版增加可选 LSPosed 屏幕查询伪装。安装模块 ZIP 后，控制 APK 会自动安装；在 LSPosed 中启用 Device Mask，并只勾选需要伪装的目标应用。模块默认不对任何应用启用屏幕伪装。

## 屏幕伪装

在控制 APK 点击“LSPosed 屏幕”，设置竖屏宽度、高度和逻辑 DPI 后保存。屏幕设置作用于所有被 LSPosed 勾选的应用进程，关闭“启用”即可停用。保存后必须强制停止并重新打开目标应用，部分应用需要重启手机或重启 LSPosed。

此功能只修改目标进程收到的 Java 层查询结果，不调用 `wm size`、`wm density`，不改变系统实际分辨率、显示服务或物理屏幕。当前拦截：`Resources.getDisplayMetrics()`、`Display.getMetrics()` 和 `Display.getRealMetrics()`。

不保证覆盖：WindowMetrics、WindowManager 原生 Binder 查询、NDK/native 查询、OpenGL/Vulkan、游戏引擎、厂商私有 API。目标应用可能因为收到的逻辑尺寸和真实窗口不一致而布局异常，因此应逐个应用测试。

## 限制和恢复

- 需要 LSPosed 93+ 或兼容实现，以及目标应用作用域授权。
- 控制 APK 和 LSPosed 模块使用同一包，更新时保留同一签名。
- 不要勾选系统 UI、设置、Root 管理器或桌面等关键进程。
- 屏幕配置保存在控制 APK 的 `screen_mask` 偏好中；关闭开关、取消目标应用作用域或在 LSPosed 禁用模块即可恢复真实查询结果。
- 这不是完整硬件身份伪装，也不改变 CPU、GPU、内存、传感器、IMEI、硬件证明或实际屏幕。

## 验证

APK 使用 Xposed API compileOnly 依赖构建成功；模块 ZIP CRC 检查通过。尚未在真实 LSPosed、具体 Android ROM 和目标应用上进行运行时验证。