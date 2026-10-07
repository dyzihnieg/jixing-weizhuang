# Device Mask 1.0.0 测试版

独立 Android 控制 APK + Root 模块，不依赖 LSPosed，无网络权限。

## 安装

1. 安装 app/build/outputs/apk/debug/app-debug.apk。这是调试签名测试包。
2. 在 Magisk、KernelSU 或 APatch 的模块页面安装 device-mask-v1.0.0.zip；也可以从 APK 的“导出模块 ZIP”取得安装包。
3. 重启一次，使模块生成伪装前的属性记录。模块默认关闭。
4. 打开 Device Mask，授予 Root，选择手机或平板，点击应用并确认。
5. 保存配置后重启。恢复原机型同样需要重启。

APK 最低 Android 7.0/API 24。Root 模块依赖管理器的模块功能及 resetprop。KernelSU 新版本或衍生版可能需要另行配置模块挂载支持；不要认为检测到 Root 就代表模块可用。

## 行为

- 20 款手机、10 款平板；区域型号不同，预设不是完整固件身份。
- 完整模式修改已有的 model、marketname、brand、manufacturer 属性；最小模式只修改 model 和 marketname。
- 保留 Android 版本、fingerprint、device、name、build.product、处理器、屏幕及安全认证。
- 检测当前设备、Android 版本、Root 版本及框架路径。路径识别有残留误判可能，不保证识别管理器 APK 的版本。
- 每次开机在本模块修改前记录属性。其他模块若先伪装，记录不一定是出厂身份，请先关闭其他机型伪装模块。
- 配置存储在 /data/adb/device_mask，目录权限 0700，配置原子替换。
- 不承诺识别成平板、快充不受影响、通过反作弊、风控或硬件证明。

## 故障恢复

优先在 Root 管理器禁用或卸载模块后重启。无法正常启动时使用该管理器的安全模式禁用模块。首次使用前确保了解手机自己的恢复方式。

模块卸载后不会继续运行，但 /data/adb/device_mask 会保留配置；重新安装可能恢复先前配置。需要重置时在控制 APK 点击“恢复原机型”并重启后再卸载。

## 验证

assembleDebug 构建通过。tools/test_module.py 的 5 项隔离测试通过。
尚未进行真实 Android UI、实际 resetprop、各管理器安装、重启、充电或目标应用识别测试。本版本仅适合有恢复能力的测试设备。
