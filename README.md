# 手机1机型伪装

基于 Android、Jetpack Compose 和 Root 模块的机型伪装项目，应用内名称为 Device Mask。当前版本为 **2.1.1**。

支持手机和平板机型预设、设备身份属性配置，以及通过 LSPosed 实现的可选处理器名称和屏幕信息伪装。控制应用无网络权限，配置保存在设备本地。

## 功能与运行条件

- 提供 20 款手机、10 款平板预设，支持修改机型、市场名称、品牌和制造商等身份属性。
- 提供完整模式、最小模式和恢复原机型操作；全局身份属性修改需要重启设备。
- 支持处理器名称和屏幕信息配置；这些可选功能需要兼容现代 libxposed API 102 的 LSPosed 框架，并为目标应用启用作用域。
- 提供主题、配色和动画设置，以及玻璃风格导航界面。
- 控制应用最低支持 Android 8.0（API 26）；全局属性伪装依赖 Magisk、KernelSU 或 APatch 的模块能力及 `resetprop`。

机型预设不等同于对应设备的完整固件或真实硬件身份。微信平板登录等目标应用的识别结果仍需真机验证，最新兼容性说明见 [2.1.1 版本说明](DEVICE_MASK_2.1.1.md)。

## 项目结构

```text
app/                         Android 应用、资源、Root 模块脚本和测试
gradle/                      依赖版本目录与 Gradle 启动器
.build-tools/                Windows 环境准备、构建和验证脚本
tools/                       模块打包、脚本测试和 ARM64 AAPT2 工具
docs/                        设计与实现记录
BUILD_WINDOWS.md             Windows 构建详细说明
DEVICE_MASK*.md              各版本功能、安装和验证记录
setup_android_env.sh         Linux ARM64 环境准备脚本
```

## 构建

需要 JDK 17 或更新版本，推荐 JDK 21。工程使用 Gradle 9.1.0、Android SDK API 36 和 Build Tools 36.0.0；首次配置需要联网下载构建工具与依赖。

### Windows

安装 JDK 并配置 `JAVA_HOME`，在项目根目录执行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .build-tools\Setup-AndroidEnv.ps1 -Build
```

脚本在项目内准备工具，校验下载文件，并在 `.build-ui` 副本中执行 APK 构建和 JVM 测试。产物位置：

```text
.build-ui/app/build/outputs/apk/debug/app-debug.apk
```

工具和依赖已完整缓存时，可追加 `-Offline`。构建细节见 [Windows 构建说明](BUILD_WINDOWS.md)。

### Linux ARM64 / Proot

工程保留 ARM64 AAPT2 适配，环境准备脚本会配置 SDK、Gradle 并替换相关工具：

```bash
chmod +x setup_android_env.sh gradlew
./setup_android_env.sh
./gradlew assembleDebug
```

此流程针对 Linux ARM64 / Proot；其他平台需要调整 AAPT2 配置。内置工具位于 `tools/aapt2/aapt2-arm64-v8a`，来源为 [ReVanced AAPT2 v1.0.0](https://github.com/ReVanced/aapt2/releases/tag/v1.0.0)，SHA-256 为 `e5b5ff7f0d4f6ecd7fa5d05d77fed3f09f6f1bf80f078b8aada82bc578848561`。

### 打包 Root 模块

安装 Python 3 后，将构建好的 APK 与模块脚本打包：

```powershell
python tools\package_module.py --apk .build-ui\app\build\outputs\apk\debug\app-debug.apk --output dist\device-mask-v2.1.1.zip
```

Linux 环境将 `--apk` 改为 `app/build/outputs/apk/debug/app-debug.apk`。打包工具会校验 APK 格式和模块中的配套 APK 内容。

## 安装与使用

1. 安装控制 APK，再通过 Root 管理器安装打包后的模块 ZIP。
2. 重启设备，打开 Device Mask 并授予 Root 权限。
3. 选择机型预设、配置所需选项并保存；全局身份属性修改后再次重启设备。
4. 使用处理器或屏幕伪装时，在 LSPosed 中启用模块并勾选目标应用，完整停止并重新打开目标应用；首次启用或升级后按版本说明重启设备。

恢复原机型同样需要重启。出现异常时，优先在 Root 管理器中禁用或卸载模块后重启；安装前应了解设备的安全模式和恢复方式。

## Git 文件管理

仓库保留应用源码、模块资源、构建配置、Gradle 启动器、必要的 ARM64 工具、测试脚本和中文文档。

`.gitignore` 排除构建缓存、下载的 SDK 和 Gradle、构建副本、历史备份、会话记录、Python 缓存、APK 和模块 ZIP、日志、本机路径配置及签名密钥。已有本地文件不会被删除。

`local.properties` 和签名密钥需在各开发环境单独配置。首次在新环境构建会生成新的调试签名；覆盖安装已有应用需要使用原签名密钥。

## 版本记录

- [初始模块说明](DEVICE_MASK.md)
- [2.0 版本说明](DEVICE_MASK_2.0.md)
- [2.1.1 版本说明](DEVICE_MASK_2.1.1.md)
- [界面调整记录](UI_CHANGES.md)
- [玻璃界面调整记录](GLASS_UI_CHANGES.md)
- [动画调整记录](MOTION_UI_CHANGES.md)
- [导航调整记录](COOLAPK_DOCK_CHANGES.md)

各版本文档中的验证记录对应当时的构建环境；Git 仓库初始化与上传不代表新增真机测试结果。
