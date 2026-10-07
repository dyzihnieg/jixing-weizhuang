# Windows 构建

最新产物：`dist/DeviceMask-2.1.1-hdglass.apk` 与 `dist/device-mask-v2.1.1-hdglass.zip`。
本轮说明见 `DEVICE_MASK_2.1.1.md`，交付审计使用
`.build-tools/Verify-Delivery.py --variant hdglass --version 2.1.1 --expected-tests 35`。

本机已在 `.build-tools` 安装并运行 Gradle 9.1.0、Android API 36 平台及
Build Tools 36.0.0，使用 `C:\Program Files\Java\jdk-21.0.12.1`。
Gradle 下载按官方 SHA-256 校验；SDK 下载按 Google 仓库公布的校验值和大小验证。

在工作区根目录执行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .build-tools\Setup-AndroidEnv.ps1 -Build
```

脚本只在工作区内配置工具和缓存，不修改系统 PATH。已有完整工具时跳过下载。
它把当前源码复制到 `.build-ui`，移除该副本的 Linux/Proot AAPT2 规则，
设置 Windows SDK 和调试签名，再执行 `assembleDebug` 和 `testDebugUnitTest`。
构建结果位于 `.build-ui\app\build\outputs\apk\debug\app-debug.apk`。

工具与依赖完整缓存后，可使用 `-Offline -Build`；首次构建需要网络。

打包隔离构建生成的 APK：

```powershell
python tools\package_module.py --apk .build-ui\app\build\outputs\apk\debug\app-debug.apk --output dist\device-mask-v2.1.1-hdglass.zip
```

打包器校验 APK 的 ZIP 完整性、AndroidManifest.xml 和 classes.dex，
并校验模块 ZIP 内的 companion.apk 与输入 APK 字节完全一致。

新调试密钥位于 `.build-tools\debug.keystore`，后续本机构建会继续使用它。
没有找到旧 APK 的签名私钥；新签名与旧版不同，已有旧版配套应用时需先卸载再安装，
随后在 LSPosed 重新启用配套应用并检查作用域。请保留此密钥供后续升级使用。

玻璃版本继续使用同一工作区密钥，可覆盖安装上一版 UI APK。
新版界面的真机检查包括：系统/浅色/深色主题、七种颜色、自由拼色、重启后颜色保持、
玻璃背景动态模糊、大字体、窄屏、横屏、键盘展开时的搜索及确认对话框。
