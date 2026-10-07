# 酷安风格液态玻璃 Dock

参考用户提供的 `CoolApk-16.6.4-2609291-coolapk-arm64-sign.apk`，独立实现底栏外观和交互。
APK 仅用于只读参考；没有把 APK 内的代码、图片或库打入本应用。

## 已确认的参考特征

- 资源定义 `liquid_bottom_navigation_height = 56dp`、`liquid_bottom_navigation_content_padding = 4dp`。
- 提供液态玻璃、背景模糊、半透明三个兼容层级。
- 可读组件符号包含 LiquidGlassTabPanel、LiquidTabMotionState、DampedDragAnimation、InteractiveHighlight，
  包含位移、拖动、按压形变和互动高光。加固 APK 无法据此确认全部运行时参数；本版参数自行调校。

## 本应用实现

- 悬浮圆角胶囊，最大宽度 420dp；标准字体下高度 56dp，大字体自然增高。
- 三个入口为配置、设置、关于。未选中使用线框图标，选中平滑切到实心图标与用户强调色。
- 选中胶囊两端略有不同的过渡时长，形成滑动拉伸；按下时胶囊轻微膨胀，图标收缩。
- 可点击或左右拖动，松手切换到最近的入口，支持 RTL；取消拖动恢复当前入口。
- Android 13+：真实页面背景先模糊，再由独立 AGSL 着色器产生边缘折射。
  Android 12：背景模糊；Android 8–11：半透明底色与高光。
- 二级颜色设置、自由拼色、三档动画速度继续生效，设置自动保存；预览复用实际 Dock 交互。

文件分工：`GlassDock.kt` 管理背景捕获与玻璃容器，`DockNavigation.kt` 管理导航和交互，
`GlassOptics.kt` 管理系统兼容及折射。新源码备份位于 `.backup/ui-before-coolapk-20261007`。

## 验证

实际产品着色器已由 Skia RuntimeEffect 编译并完成 8 组尺寸、密度及深浅背景的离屏渲染检查，
确认中央区域不变、边缘折射生效。独立静态审查覆盖拖动/点击冲突、RTL、API 隔离及捕获坐标。
Android APK 构建通过，29 项 JVM 单元测试全部通过，Lint 为 0 错误、23 个现有警告。
APK v2 签名通过，与上一版玻璃 UI 使用相同证书，可覆盖安装。
交付审计确认 APK/ZIP CRC、ZIP 内嵌 APK、11 个模块资源、Xposed 元数据和编译源码一致，
此前各版本产物哈希不变。完整结果见 `dist/build-info-coolapk.json`。

交付：`dist/DeviceMask-2.1.0-coolapk.apk`、`dist/device-mask-v2.1.0-coolapk.zip`。

没有在真机上运行此版或酷安作逐像素、手势观感对比；离屏 Skia 检查不代替 Android GPU 验证。
