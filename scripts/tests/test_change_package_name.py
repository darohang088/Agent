"""Exercise package renames in isolated project fixtures, never the working checkout."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


PROJECT = Path(__file__).resolve().parents[2]
SCRIPT = PROJECT / "change_package_name.sh"
OLD = "com.original.app"


class ChangePackageNameTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="agent-package-test-")
        self.root = Path(self.temporary.name)
        shutil.copy2(SCRIPT, self.root / SCRIPT.name)
        self.write("app/build.gradle.kts", '''android {
    namespace = "com.original.app"
    defaultConfig {
        applicationId = "com.original.app"
    }
    productFlavors { create("staging") { applicationIdSuffix = ".staging" } }
}
''')
        for source_set in ("main", "debug", "release", "test", "testDebug", "androidTest"):
            self.write(f"app/src/{source_set}/java/com/original/app/Sample.kt",
                       "package com.original.app\nimport com.original.app.domain.Model\n")
        self.write("app/src/main/kotlin/com/original/app/domain/Model.kt", "package com.original.app.domain\n")
        self.write("app/src/main/AndroidManifest.xml", '<activity android:name="com.original.app.MainActivity"/>')
        self.write("scripts/check_policy.py", 'PATH = "app/src/release/java/com/original/app"\n')
        self.write("README.md", "Namespace com.original.app; source app/src/main/java/com/original/app\n")
        self.write("settings.gradle.kts", 'rootProject.name = "com.original.app"\n')
        self.write("app/src/main/res/values/strings.xml", '<resources><string name="app_name">com.original.app</string></resources>')
        self.write("app/src/staging/res/values/config.xml", '<string name="api_base_url">https://com.original.app/api/</string>')
        self.write("app/document/reference-imports/original.txt", "package com.original.app\n")
        self.write("app/build/generated/generated.kt", "package com.original.app\n")
        self.write("local.properties", "sample=com.original.app\n")
        self.write(".env", "sample=com.original.app\n")
        self.write("app/google-services.json", '{"package_name":"com.original.app"}')

    def tearDown(self):
        self.temporary.cleanup()

    def write(self, name, text):
        path = self.root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text)

    def snapshot(self):
        return {str(path.relative_to(self.root)): path.read_bytes()
                for path in self.root.rglob("*") if path.is_file()}

    def run_script(self, *args):
        # Invocation from another directory verifies paths are relative to the script itself.
        return subprocess.run(["bash", str(self.root / SCRIPT.name), *args], cwd="/tmp",
                              text=True, capture_output=True)

    def test_dry_run_changes_nothing(self):
        before = self.snapshot()
        result = self.run_script("--dry-run", "io.example.mobile")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("no files changed", result.stdout)
        self.assertEqual(before, self.snapshot())

    def test_rename_updates_all_source_sets_and_helpers(self):
        result = self.run_script("io.example.mobile")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn('namespace = "io.example.mobile"', (self.root / "app/build.gradle.kts").read_text())
        self.assertIn('applicationId = "io.example.mobile"', (self.root / "app/build.gradle.kts").read_text())
        self.assertIn('applicationIdSuffix = ".staging"', (self.root / "app/build.gradle.kts").read_text())
        for source_set in ("main", "debug", "release", "test", "testDebug", "androidTest"):
            path = self.root / f"app/src/{source_set}/java/io/example/mobile/Sample.kt"
            self.assertIn("package io.example.mobile", path.read_text())
            self.assertFalse((self.root / f"app/src/{source_set}/java/com/original/app").exists())
        self.assertIn("io/example/mobile", (self.root / "scripts/check_policy.py").read_text())
        self.assertIn("io.example.mobile.MainActivity", (self.root / "app/src/main/AndroidManifest.xml").read_text())
        self.assertIn("package io.example.mobile.domain", (self.root / "app/src/main/kotlin/io/example/mobile/domain/Model.kt").read_text())

    def test_urls_branding_local_files_and_reference_copies_are_preserved(self):
        names = ["settings.gradle.kts", "app/src/main/res/values/strings.xml",
                 "app/src/staging/res/values/config.xml", "app/document/reference-imports/original.txt",
                 "app/build/generated/generated.kt", "local.properties", ".env", "app/google-services.json"]
        before = {name: (self.root / name).read_bytes() for name in names}
        result = self.run_script("io.example.mobile")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(before, {name: (self.root / name).read_bytes() for name in names})

    def test_collision_is_rejected_before_any_changes(self):
        self.write("app/src/main/java/io/example/mobile/Sample.kt", "existing code\n")
        before = self.snapshot()
        result = self.run_script("io.example.mobile")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Destination already exists", result.stderr)
        self.assertEqual(before, self.snapshot())

    def test_invalid_packages_are_rejected_without_changes(self):
        before = self.snapshot()
        for value in ("../outside", "single", "com.class.app", "com.123.app", "com.bad-name.app"):
            result = self.run_script(value)
            self.assertNotEqual(0, result.returncode)
            self.assertEqual(before, self.snapshot())

    def test_same_package_is_a_noop(self):
        before = self.snapshot()
        result = self.run_script(OLD)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(before, self.snapshot())

    def test_child_package_move_does_not_rewrite_twice(self):
        result = self.run_script("com.original.app.next")
        self.assertEqual(0, result.returncode, result.stderr)
        path = self.root / "app/src/main/java/com/original/app/next/Sample.kt"
        self.assertIn("package com.original.app.next\n", path.read_text())
        self.assertNotIn("next.next", path.read_text())

    def test_parent_package_move_works(self):
        result = self.run_script("com.original")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("package com.original\n", (self.root / "app/src/main/java/com/original/Sample.kt").read_text())

    def test_a_second_rename_detects_the_updated_namespace(self):
        self.assertEqual(0, self.run_script("io.example.mobile").returncode)
        result = self.run_script("org.example.client")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("package org.example.client\n", (self.root / "app/src/main/java/org/example/client/Sample.kt").read_text())

    def test_distinct_namespace_and_application_id_both_update(self):
        path = self.root / "app/build.gradle.kts"
        path.write_text(path.read_text().replace('applicationId = "com.original.app"', 'applicationId = "com.original.client"'))
        self.write("app/src/main/res/values/provider.xml", '<string name="authority">com.original.client.provider</string>')
        result = self.run_script("io.example.mobile")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn('applicationId = "io.example.mobile"', path.read_text())
        self.assertIn("io.example.mobile.provider", (self.root / "app/src/main/res/values/provider.xml").read_text())


if __name__ == "__main__":
    unittest.main()
