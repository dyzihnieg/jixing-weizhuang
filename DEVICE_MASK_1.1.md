# Device Mask 1.1.0 测试版

## 安装

在管理器中安装 device-mask-v1.1.0.zip 后重启。ZIP 内置控制 APK，正常 Boot 模式安装时尝试自动安装，失败则开机完成后重试三次。安装对象为主用户 user 0；不自动打开应用，不自动授予 Root，不卸载签名冲突的旧应用。

安装日志：/data/adb/device_mask/apk-install.log。成功记录 APK 哈希，后续相同 APK 不重复安装。某些 ROM、SELinux 策略、用户限制或管理器执行环境可能阻止安装，仍需真机验证。卸载模块不会卸载 APK。

## SoC 伪装

控制界面增加“伪装 SoC 名称”，默认关闭。只修改设备上已经存在的 ro.soc.model、ro.soc.manufacturer，影响读取这些属性的软件以及部分 Android Build.SOC_MODEL / SOC_MANUFACTURER 查询。不存在的属性会跳过，Android 旧版本不保证支持。

30 款机型配备 SoC 预设；预设是区域型号对应的名称，不是原厂完整属性转储。切换或关闭后重启生效。

不修改 /proc/cpuinfo、CPU 核心数与频率、ABI、真实 GPU/OpenGL/Vulkan 信息、RAM、存储容量、屏幕、相机、传感器、IMEI 或硬件认证。没有实现完整硬件身份伪装；不保证骗过硬件检测软件。

避免修改 ro.hardware、ro.board.platform 等驱动选择属性，减少启动故障风险。建议先关闭其他伪装模块，不保证快充或全部应用兼容。

## 构建

bash gradlew assembleDebug
python3 tools/test_module.py
python3 tools/test_install.py
python3 tools/package_module.py

APK 不放入源码 assets，最终打包工具才加入 companion.apk；控制界面导出模块时读取自身安装 APK。支持本工程生成的单 APK，不支持 split APK 导出。

## 验证

APK 构建成功；6 项模块测试及 1 项安装脚本测试通过；最终 ZIP CRC 与内置 APK 内容核对通过。没有实际安装、重启、充电、界面截图或目标应用识别测试。仅供具备故障恢复能力的测试设备使用。

禁用或卸载模块后重启可停止本模块伪装，其他模块行为不受影响。原始属性记录来自本模块启动时读取，若其他模块已修改，不保证出厂身份。