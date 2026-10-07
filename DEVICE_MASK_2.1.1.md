# Device Mask 2.1.1：微信平板识别尝试修复与清透玻璃

本版针对微信 8.0.78 在登录页短暂显示平板比例、随后恢复手机登录的问题。
修复了模块中可确认的信号不一致；微信最终是否提供平板登录仍需真机验证。

## 微信兼容

- 原实现只改像素、密度与 dp，保留手机的 `Configuration.screenLayout`。
  现在按 Android 的尺寸/长宽比分级规则重算分类，并保留布局方向、圆屏等无关位。
- 微信进程在 `Application.attach` 前同步资源配置，资源更新时继续保持所选预设，
  使内部资源选择与查询结果一致。旋转仍保留原方向。
- 同步 `DisplayMetrics.noncompat*` 基准字段，避免系统兼容处理恢复原手机像素尺寸。
- 仅在微信进程、屏幕伪装开启且最小宽度至少 600dp 时，将 Java 层的
  `ro.build.characteristics` 查询返回平板类型，并保留其他属性标记。
- 不写全局设备类型属性、不清理微信数据。首次查询会记录配置分类与属性命中，便于继续排查。

日志标签为 `DeviceMask`，关注 `Screen class package=com.tencent.mm`、
`WeChat resource qualifiers aligned before attach` 和 `WeChat characteristics`。
其他判定、native 属性读取和微信服务端策略未通过本版真机验证。

## 液态玻璃 Dock

背景模糊从 8dp 降为 0.7dp，降低不透明底色；导航整体和选中胶囊分别采样真实页面。
Android 13+ 提供色散边缘折射、随触点移动的反光带与边缘高光，拖动期间反光动态变化。
Android 12 提供轻度模糊与移动高光；旧系统采用半透明和移动高光。
三档动画速度和颜色设置继续保存，图标与文字独立绘制。

参考的是用户提出的 iOS 26 液态玻璃观感，属于独立实现，未作逐像素或真机对照。

## 安装与验证

产物为 `dist/DeviceMask-2.1.1-hdglass.apk` 和 `dist/device-mask-v2.1.1-hdglass.zip`。
可用同一工作区签名覆盖安装上一版 UI APK。版本码从 10 提升为 11，版本名为 2.1.1。

选择平板预设，打开屏幕伪装并保存；确认 LSPosed 已勾选微信。
更新模块/APK 后重启手机，随后完整强制停止微信再打开进行登录验证。
不要为此清除微信数据；若仍恢复手机登录，保留 DeviceMask 日志可判断哪条信号已生效。

源码备份：`.backup/ui-before-hd-reflection-20261007/src`。
构建/测试/签名与打包审计记录见 `dist/build-info-hdglass.json`；
本轮没有连接 Android 真机或运行微信 8.0.78。

已通过 APK 构建、35 项 JVM 测试及 APK v2 签名验证；10 组实际着色器离屏渲染检查通过。
Android Lint 为 0 错误、25 警告，其中新增 2 个是兼容 Hook 所需隐藏 API 反射提示。
旧产物哈希、ZIP 内嵌 APK、源码与编译副本、模块资源与 Xposed 元数据一致性均已验证。
原有 7 项模块脚本测试通过（Windows 使用 bundled Git sh，并将临时 fixture 写为 LF）。
原测试直接在 Windows 运行缺少 sh，临时 fixture 的 CRLF 也会误报无效配置；
上述适配仅用于测试环境，未修改模块脚本。
