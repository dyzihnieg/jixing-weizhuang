import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]

class InstallTest(unittest.TestCase):
    def test_install_success_skip_and_failure(self):
        with tempfile.TemporaryDirectory() as folder:
            base = Path(folder)
            (base / 'companion.apk').write_bytes(b'test-apk')
            source = (ROOT / 'app/src/main/assets/module/install-apk.sh').read_text()
            source = source.replace('/data/adb/device_mask', str(base / 'state')).replace('/data/local/tmp', str(base))
            (base / 'install.sh').write_text(source)
            (base / 'pm').write_text('#!/bin/sh\nif [ "$1" = path ]; then echo package:test; exit 0; fi\necho called >> "$CALLS"\nif [ "$FAIL" = yes ]; then echo Failure; exit 1; fi\necho Success\n')
            (base / 'pm').chmod(0o755)
            env = dict(os.environ, PATH=str(base) + ':' + os.environ['PATH'], CALLS=str(base / 'calls'))
            def run():
                return subprocess.run(['sh', str(base / 'install.sh')], env=env, timeout=5).returncode
            self.assertEqual(0, run())
            self.assertEqual(0, run())
            self.assertEqual(['called'], (base / 'calls').read_text().splitlines())
            (base / 'companion.apk').write_bytes(b'new-apk')
            env['FAIL'] = 'yes'
            self.assertEqual(1, run())
            self.assertIn('Failure', (base / 'state/apk-install.log').read_text())
            self.assertEqual([], list(base.glob('device-mask-install-*.apk')))

if __name__ == '__main__':
    unittest.main(verbosity=2)