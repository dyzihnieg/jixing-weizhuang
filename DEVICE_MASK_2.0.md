# Device Mask 2.0.0 API 102 迁移测试版

作者：dyzihnieg。

Hook 依赖 io.github.libxposed:api:102.0.0（compileOnly），现代入口 META-INF/xposed/java_init.list，最低和目标 Hook API 均为102。控制 APK 使用官方 service:101.0.0 客户端已有的 getScope 与 Remote Preferences，不使用 service102 热重载方法；实际连接时要求框架 API>=102。service102 发布包要求 compileSdk37，而本机SDK仓库暂不提供37，因此采用这些既有服务接口并用 compileSdk36构建。

移除 legacy Xposed API、assets/xposed_init、xposedsharedprefs 与世界可读配置。不再读取 LSPosed 私有数据库，也不要求控制 APK 自身注入。启动检查等待官方服务连接并检查远程配置是否可读。作用域通过官方 getScope 查询，不自动打开管理器或申请授权。

## 升级

安装 device-mask-v2.0.0.zip 后重启，确认自动安装的控制APK关于页显示2.0.0。自行打开支持API102的LSPosed管理器，启用Device Mask，只勾选目标应用，不需要勾选Device Mask自身。不勾选系统、桌面、设置、Root管理器。

旧文件共享配置不会自动迁移。进入控制APK后重新选择机型、设置CPU和屏幕独立开关并保存。全局属性需重启手机，应用内CPU字段需强制停止目标应用再打开。屏幕配置通过Remote Preferences读取，不改变真实显示服务，但可能影响目标应用布局。

## 验证

assembleDebug成功，8项原有脚本隔离测试通过。APK中现代入口三个文件存在，合并Manifest含官方XposedProvider，无旧共享声明；模块ZIP CRC及内置APK一致性校验通过。

尚未真机验证服务101客户端连接API102框架、Remote Preferences跨进程读写、SoC字段替换、屏幕Hook、作用域获取及启动界面。不能仅凭编译保证LSPosed警告消失或屏幕问题修复。旧1.7安装包保留用于回退。
