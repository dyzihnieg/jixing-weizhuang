# Device Mask UI changes

The October 7 floating glass Dock and seven-color/split-color update is documented
in `GLASS_UI_CHANGES.md`. Its APK/ZIP filenames end in `-glass`; the `-ui` files
below are the previous build.

The Compose UI now uses a consistent Material 3 theme, a scrolling configuration
page, selectable device rows, and persistent appearance settings. The settings
button is available on the startup, configuration, and about screens.

Appearance settings support system/light/dark mode and teal/blue/rose/amber accent
colors. They are stored in the separate `appearance` preferences file and do not
modify module configuration.

The original UI sources are backed up in `.backup/ui-before-20261006`.
Root operations, LSPosed queries, screen/SoC writes, profile loading, apply,
restore, and reboot callbacks were compared with that backup.

## Validation

The Windows environment now has Gradle 9.1.0, Android API 36 and Windows Build
Tools 36.0.0 installed under `.build-tools`, running with JDK 21.0.12.1. Official
distribution checksums were verified. The isolated `.build-ui` build removes
Linux/Proot AAPT2 overrides and uses a workspace debug signing key.

To configure and build on this Windows host, run:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .build-tools\Setup-AndroidEnv.ps1 -Build
```

The build completed successfully: `assembleDebug` and `testDebugUnitTest` passed.
All seven JVM unit tests passed, including six appearance preference tests.
The APK v2 signature was verified with apksigner, and APK package/version metadata
was inspected with AAPT2. Delivery auditing confirms the new UI classes are in
the DEX files, module assets and Xposed metadata match the source, and the module
ZIP embeds the exact new APK. APK and ZIP CRC checks passed.

The new delivery files are `dist/DeviceMask-2.1.0-ui.apk` and
`dist/device-mask-v2.1.0-ui.zip`; hashes and audit results are in
`dist/build-info.json`. See `BUILD_WINDOWS.md` for repeatable build commands.

`app/build/outputs/apk/debug/app-debug.apk` is still the original APK. The existing
`device-mask-v2.1.0.zip` was accidentally re-packaged from that original APK during
environment investigation. Its ZIP integrity and embedded APK equality were
checked; it does not contain the new UI.

The old APK private signing key was not available. The new workspace key is in
`.build-tools/debug.keystore` and must be retained for future updates. Existing
users need to uninstall the old companion app before installing the new APK,
then enable the new app in LSPosed and check its scope.

All ten existing Python regression tests passed through the temporary MSYS
fixture adapter `.build-tools/Run-ModuleTests-Msys.py`. It normalizes Windows
paths and CRLF in temporary fixtures and bridges missing shell SHA256/chmod
commands. The original tests and production module scripts were unchanged.
Windows chmod does not verify Linux permission enforcement.

The new UI still needs device rendering checks: light/dark mode, all four
accents, persistence after restart, large fonts, search with the keyboard open,
and apply/restore/reboot confirmation dialogs. No new UI was installed on a
device in this session.
