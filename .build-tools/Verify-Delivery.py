"""Audit the UI APK and module ZIP produced by the isolated Windows build."""
import argparse
import hashlib
import json
from pathlib import Path
import xml.etree.ElementTree as ET
from zipfile import ZipFile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--variant', choices=('glass', 'motion', 'coolapk', 'hdglass'), default='glass')
parser.add_argument('--version', default='2.1.0')
parser.add_argument('--expected-tests', type=int, default=22)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
apk = root / f'dist/DeviceMask-{args.version}-{args.variant}.apk'
module_zip = root / f'dist/device-mask-v{args.version}-{args.variant}.zip'
build_apk = root / '.build-ui/app/build/outputs/apk/debug/app-debug.apk'
module = root / 'app/src/main/assets/module'
apk_bytes = apk.read_bytes()
assert apk_bytes == build_apk.read_bytes(), 'Delivery APK differs from the compiled APK'

with ZipFile(apk) as app, ZipFile(module_zip) as archive:
    assert app.testzip() is None, 'APK CRC check failed'
    assert archive.testzip() is None, 'Module ZIP CRC check failed'
    assert archive.read('companion.apk') == apk_bytes, 'Embedded APK differs'
    dex = b''.join(app.read(name) for name in app.namelist() if name.endswith('.dex'))
    for name in ('AppearanceSettings', 'ThemeMode', 'AccentColor', 'ColorStyle', 'MaskUiKt', 'GlassDockKt', 'AppearanceScreenKt'):
        assert name.encode() in dex, f'New UI class missing: {name}'
    if args.variant in {'motion', 'coolapk', 'hdglass'}:
        assert b'AnimationSpeed' in dex, 'Animation speed class missing'
    if args.variant in {'coolapk', 'hdglass'}:
        for name in ('DockNavigationKt', 'GlassOpticsKt', 'DockGlassEffectApi33'):
            assert name.encode() in dex, f'New Dock class missing: {name}'
    if args.variant == 'hdglass':
        assert b'ScreenClassification' in dex, 'Screen classification fix missing'
    module_files = [file for file in module.rglob('*') if file.is_file()]
    expected_entries = {file.relative_to(module).as_posix() for file in module_files} | {'companion.apk'}
    assert set(archive.namelist()) == expected_entries, 'Unexpected module ZIP entries'
    for file in module_files:
        relative = file.relative_to(module).as_posix()
        assert archive.read(relative) == file.read_bytes(), f'Module file changed: {relative}'
        assert app.read(f'assets/module/{relative}') == file.read_bytes(), f'APK asset differs: {relative}'
    xposed = root / 'app/src/main/resources/META-INF/xposed'
    for file in xposed.iterdir():
        if file.is_file():
            assert app.read(f'META-INF/xposed/{file.name}') == file.read_bytes(), f'Xposed metadata differs: {file.name}'

suites = []
for file in (root / '.build-ui/app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'):
    attrs = ET.parse(file).getroot().attrib
    suites.append({key: attrs[key] for key in ('name', 'tests', 'failures', 'errors')})
assert sum(int(suite['tests']) for suite in suites) == args.expected_tests, f'Expected {args.expected_tests} JVM unit tests'
assert all(int(suite['failures']) == int(suite['errors']) == 0 for suite in suites), 'JVM tests failed'

old_artifacts = {
    'app/build/outputs/apk/debug/app-debug.apk': 'ece83bdf9e898fc478938d12a72944910db79c0658fe02e85e3c180f543c8655',
    'device-mask-v2.1.0.zip': 'f3374944a6c4dc6051291bc90b0421e5b0d9ad92b8507736f8b07728f1e8eabc',
    'dist/DeviceMask-2.1.0-ui.apk': 'cdf3548525e827bb1af514801295c6bb4cf5da839247daf50ec85fa104c4f2e3',
    'dist/device-mask-v2.1.0-ui.zip': 'fa9786dfa4e5fff2d39acdf3fdbd3cd58f4b809f8880646d702985cebbddd701',
}
if args.variant in {'motion', 'coolapk', 'hdglass'}:
    old_artifacts.update({
        'dist/DeviceMask-2.1.0-glass.apk': '7883b4a1b59ac3e772e2ad42f69148537bb97978887e5f178fd53b5df671e894',
        'dist/device-mask-v2.1.0-glass.zip': 'c3c02772d77b162f2190786ee215fe3a574484e9a3c239f5e0f55e84f4588165',
    })
if args.variant in {'coolapk', 'hdglass'}:
    old_artifacts.update({
        'dist/DeviceMask-2.1.0-motion.apk': '3d3eb8dd74ce4c6a7c15f00575fc68c01bf9cfd89d321d2abe8d144ced1a47b5',
        'dist/device-mask-v2.1.0-motion.zip': 'eea973edffdf3983f8aa5bd5b279bf414c9c51eda30350ae1899d360ccb10455',
    })
if args.variant == 'hdglass':
    old_artifacts.update({
        'dist/DeviceMask-2.1.0-coolapk.apk': 'dfff10ff11622386c7d66131140b42281d54d95e3020327dbf9b35fff628228e',
        'dist/device-mask-v2.1.0-coolapk.zip': 'a71b9179b1d17d428bd3ad5984f45dd39dfe9683c9db806b1eb5b12c186cce23',
    })
for path, expected_hash in old_artifacts.items():
    assert hashlib.sha256((root / path).read_bytes()).hexdigest() == expected_hash, f'Original artifact overwritten: {path}'

source_root = root / 'app/src'
for file in source_root.rglob('*'):
    if file.is_file():
        compiled_source = root / '.build-ui/app/src' / file.relative_to(source_root)
        assert compiled_source.read_bytes() == file.read_bytes(), f'Compiled source differs: {file}'
lint_issues = ET.parse(root / '.build-ui/app/build/reports/lint-results-debug.xml').getroot()
lint_errors = [issue for issue in lint_issues.findall('issue') if issue.get('severity') in {'Error', 'Fatal'}]
assert not lint_errors, 'Android Lint has errors'

report = {
    'toolchain': {'jdk': '21.0.12.1', 'gradle': '9.1.0', 'android_platform': 36, 'build_tools': '36.0.0'},
    'files': {file.name: {'bytes': file.stat().st_size, 'sha256': hashlib.sha256(file.read_bytes()).hexdigest()} for file in (apk, module_zip)},
    'jvm_unit_tests': suites,
    'module_entries_verified': len(module_files),
    'new_ui_classes_present': True,
    'embedded_apk_matches': True,
    'xposed_metadata_matches': True,
    'original_artifacts_unchanged': True,
    'compiled_sources_match': True,
    'lint_errors': len(lint_errors),
    'lint_warnings': sum(issue.get('severity') == 'Warning' for issue in lint_issues.findall('issue')),
}
(root / f'dist/build-info-{args.variant}.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, ensure_ascii=False, indent=2))
