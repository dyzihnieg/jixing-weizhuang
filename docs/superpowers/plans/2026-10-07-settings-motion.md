# 二级设置与 Dock 动画实现计划

> 面向 AI 代理的工作者：按独立文件分工实现，主代理统一构建与验证。

**目标：** 设置包含颜色和动画速度两个二级页面，Dock 切换有可调速的过渡。

**架构：** 扩展现有 AppearanceSettings 和 SharedPreferences；设置页本地导航，复用颜色控件；真实 Dock 与交互预览共享动画组件。

**技术栈：** Kotlin、Jetpack Compose、Android SharedPreferences、JUnit。

**规格：** 用户本轮要求两级选择和可调速 Dock，此前已授权实现 UI 并生成 APK。

## 约束和接口

- 保留现有玻璃背景捕获及模块业务行为，不增加依赖；备份在 `.backup/ui-before-motion-20261007`。
- `AnimationSpeed` 包含 `SLOW / STANDARD / FAST`，`storageKey` 分别为 `slow / standard / fast`，`displayName` 分别为慢速、标准、快速，`durationMillis` 分别为 520、320、180。
- `AppearanceSettings.animationSpeed` 默认 STANDARD；新增偏好键 `animation_speed`，缺失或非法值回落 STANDARD。
- `GlassDockTabs(selectedTab: Int, onSelect: (Int) -> Unit, animationSpeed: AnimationSpeed)` 共享胶囊位移、图标缩放与颜色过渡，使用 FastOutSlowInEasing。
- 不要求额外批准；当前不是 Git 仓库，直接编辑已有备份的工作区。

## 实现与检查

- [x] 模型与保存：修改 `ui/theme/Appearance.kt` 和 `AppearanceStore.kt`，测试旧设置迁移、非法值回落、所有档位往返、换色/换速度互不重置。
- [x] 二级页面：修改 `AppearanceScreen.kt`，首页两张入口卡，子页标题和返回按钮；系统返回优先回首页；颜色与动画页面均可滚动，Dialog 复用同样控件。
- [x] Dock：修改 `GlassDock.kt`，提取共享导航组件；胶囊、图标颜色、缩放使用同一速度；连续点击平滑改目标，无循环动画或延迟任务。
- [x] 验证：主代理执行 `Verify-Ui.ps1 -Prepare -Build -Offline`、`:app:lintDebug`；独立静态审查返回逻辑、小屏、预览与捕获隔离。
- [x] 交付：保留旧产物，新 APK/ZIP 命名 `-motion`；验证签名、CRC、内嵌 APK、模块文件、源码与编译副本一致，记录测试数量及无真机 UI 验证的限制。
