"""Package the compiled APK alongside module scripts, not inside APK assets."""
import argparse
from pathlib import Path
from zipfile import BadZipFile, ZipFile, ZIP_DEFLATED

root = Path(__file__).resolve().parents[1]
module = root / 'app/src/main/assets/module'
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--apk', type=Path, default=root / 'app/build/outputs/apk/debug/app-debug.apk',
                    help='Compiled APK to include (default: app/build/outputs/apk/debug/app-debug.apk)')
parser.add_argument('--output', type=Path, default=root / 'device-mask-v2.1.0.zip',
                    help='Module ZIP output (default: device-mask-v2.1.0.zip)')
args = parser.parse_args()
apk = args.apk.resolve()
output = args.output.resolve()
if not apk.is_file():
    parser.error(f'APK not found: {apk}; build assembleDebug first')
if (module / 'companion.apk').exists():
    parser.error('Do not embed APK inside its own assets')
if output == apk:
    parser.error('Output ZIP must differ from the input APK')
try:
    with ZipFile(apk) as archive:
        missing = {'AndroidManifest.xml', 'classes.dex'} - set(archive.namelist())
        if missing:
            parser.error(f'APK is missing required entries: {", ".join(sorted(missing))}')
        damaged = archive.testzip()
        if damaged:
            parser.error(f'APK has a damaged ZIP entry: {damaged}')
except (BadZipFile, OSError) as error:
    parser.error(f'APK is not a valid ZIP: {error}')
output.parent.mkdir(parents=True, exist_ok=True)
with ZipFile(output, 'w', ZIP_DEFLATED) as archive:
    for file in sorted(module.rglob('*')):
        if file.is_file():
            archive.write(file, file.relative_to(module))
    archive.write(apk, 'companion.apk')
with ZipFile(output) as archive:
    assert archive.testzip() is None
    assert archive.read('companion.apk') == apk.read_bytes()
print(output)
print(f'{output.stat().st_size} bytes; APK verified')
