"""Run module logic in an isolated directory with mocked Android commands."""
import pathlib
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
MODULE = ROOT / "app/src/main/assets/module"


class ModuleTest(unittest.TestCase):
    def test_profiles(self):
        rows = [line.split('|') for line in (MODULE / 'profiles.tsv').read_text().splitlines() if line]
        self.assertEqual(30, len(rows))
        self.assertEqual(30, len({r[0] for r in rows}))
        self.assertEqual(20, sum(r[1] == 'phone' for r in rows))
        self.assertEqual(10, sum(r[1] == 'tablet' for r in rows))
        self.assertTrue(all(len(r) == 6 and all(r) for r in rows))

    def run_case(self, config=None, fail=False):
        with tempfile.TemporaryDirectory() as folder:
            base = pathlib.Path(folder)
            state = base / 'state'
            state.mkdir()
            if config is not None:
                (state / 'config').write_text(config + '\n')
            (base / 'profiles.tsv').write_bytes((MODULE / 'profiles.tsv').read_bytes())
            (base / 'soc.tsv').write_bytes((MODULE / 'soc.tsv').read_bytes())
            script = (MODULE / 'post-fs-data.sh').read_text().replace('/data/adb/device_mask', str(state))
            (base / 'post-fs-data.sh').write_text(script)
            (base / 'getprop').write_text('#!/bin/sh\nprintf "original-value\\n"\n')
            (base / 'resetprop').write_text('#!/bin/sh\nprintf "%s|%s\\n" "$2" "$3" >> "$TEST_LOG"\n' + ('[ "$3" = original-value ]\n' if fail else 'exit 0\n'))
            (base / 'getprop').chmod(0o755)
            (base / 'resetprop').chmod(0o755)
            import os
            env = dict(os.environ, PATH=str(base) + ':' + os.environ['PATH'], TEST_LOG=str(base / 'calls'))
            subprocess.run(['sh', str(base / 'post-fs-data.sh')], env=env, timeout=5, capture_output=True)
            calls = (base / 'calls').read_text().splitlines() if (base / 'calls').exists() else []
            return (state / 'status').read_text().strip(), calls

    def test_disabled(self):
        self.assertEqual(('disabled', []), self.run_case())
        self.assertEqual(('disabled', []), self.run_case('off|full'))

    def test_full_and_min(self):
        status, calls = self.run_case('pixel8|full')
        self.assertEqual('applied: pixel8 (full)', status)
        self.assertEqual(24, len(calls))
        status, calls = self.run_case('pady700|min')
        self.assertEqual('applied: pady700 (min)', status)
        self.assertEqual(12, len(calls))
        self.assertTrue(all('.model|' in c or '.marketname|' in c for c in calls))

    def test_screen_presets(self):
        screens = [r.split('|') for r in (MODULE / 'screens.tsv').read_text().splitlines() if r]
        ids = {r.split('|')[0] for r in (MODULE / 'profiles.tsv').read_text().splitlines() if r}
        self.assertEqual(ids, {r[0] for r in screens})
        self.assertEqual(30, len(screens))
        for row in screens:
            w, h, d = map(int, row[1:])
            self.assertTrue(320 <= w <= h <= 7680)
            self.assertTrue(120 <= d <= 960)

    def test_soc(self):
        status, calls = self.run_case('pixel8|full|on')
        self.assertTrue(status.startswith('applied:'))
        self.assertEqual(26, len(calls))
        self.assertIn('ro.soc.model|Tensor G3', calls)
        self.assertIn('ro.soc.manufacturer|Google', calls)

    def test_reject_bad_config(self):
        for config in ['unknown|full', 'pixel8|broken', '$(touch nope)|full']:
            status, calls = self.run_case(config)
            self.assertTrue(status.startswith('error:'))
            self.assertEqual([], calls)

    def test_rollback(self):
        status, calls = self.run_case('pixel8|full', fail=True)
        self.assertIn('rollback attempted', status)
        self.assertEqual(50, len(calls))
        self.assertTrue(all(c.endswith('|original-value') for c in calls[24:]))


if __name__ == '__main__':
    unittest.main(verbosity=2)
