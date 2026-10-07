import pathlib
import unittest


ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE = (ROOT / "app/src/main/java/com/java/myapplication/ScreenHook.java").read_text()


class ScreenHookRegressionTest(unittest.TestCase):
    def test_hooks_configuration_and_rect_size_queries(self):
        self.assertIn('"getConfiguration"', SOURCE)
        self.assertIn('"getRectSize"', SOURCE)

    def test_reports_installation_failures_instead_of_false_loaded(self):
        self.assertIn("int installedHooks", SOURCE)
        self.assertIn("installedHooks == 0", SOURCE)
        self.assertIn("installedHooks++", SOURCE)


if __name__ == "__main__":
    unittest.main(verbosity=2)
