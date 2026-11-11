#!/usr/bin/env python3
"""Confere preparo de ferramentas de vídeo sem APT, execução de containers ou dados produtivos."""
import os
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

class VideoToolsTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="video-ci-tools-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.bin = self.root / "bin"
        self.bin.mkdir()
        self.bash = shutil.which("bash")
        for name in ("mkdir", "chmod"):
            (self.bin / name).symlink_to(shutil.which(name))
        self.write("docker", '''printf '%s\\n' "$*" >> "$VIDEO_TEST_LOG"
case "$1" in
create) [[ "${VIDEO_TEST_FAIL:-}" != 1 ]] || exit 48; printf 'test-tool-container' ;;
cp) printf '#!/bin/bash\\nexit 0\\n' > "$3" ;;
rm) exit 0 ;;
*) exit 49 ;;
esac
''')

    def write(self, name, body="exit 0\n"):
        p = self.bin / name
        p.write_text("#!/bin/bash\n" + body)
        p.chmod(0o755)

    def run_tools(self, fail=False):
        return subprocess.run([self.bash, str(ROOT / "scripts/prepare-ci-video-tools.sh")],
            env={"PATH": str(self.bin), "RUNNER_TEMP": str(self.root), "GITHUB_PATH": str(self.root / "path"),
                 "VIDEO_TEST_LOG": str(self.root / "calls"), "VIDEO_TEST_FAIL": str(int(fail))},
            text=True, capture_output=True, timeout=10)

    def test_existing_tools_do_not_pull_or_contact_package_mirrors(self):
        self.write("ffmpeg"); self.write("ffprobe")
        result = self.run_tools()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertFalse((self.root / "calls").exists())
        self.assertFalse((self.root / "path").exists())

    def test_missing_tools_use_pinned_image_without_running_container(self):
        result = self.run_tools()
        self.assertEqual(result.returncode, 0, result.stderr)
        calls = (self.root / "calls").read_text()
        self.assertIn("mwader/static-ffmpeg:7.1.1@sha256:6769881cc02c80d33e387750a8e144d162adfab2775e934dd97899261dda3a0c", calls)
        self.assertIn("rm test-tool-container", calls)
        self.assertNotIn("run ", calls)
        self.assertEqual((self.root / "path").read_text().strip(), str(self.root / "pde-video-tools"))

    def test_failed_image_download_never_exposes_missing_tools(self):
        result = self.run_tools(fail=True)
        self.assertEqual(result.returncode, 48, result.stderr)
        self.assertFalse((self.root / "path").exists())

    def test_workflows_install_pinned_browser_without_apt_dependencies(self):
        for name in ("liquibase-mysql57.yml", "pde-platform-metodo-musa-ci.yml"):
            workflow = (ROOT / ".github/workflows" / name).read_text()
            self.assertIn("npx playwright install chromium", workflow)
            self.assertNotIn("playwright install --with-deps", workflow)
        workflow = (ROOT / ".github/workflows/pde-platform-metodo-musa-ci.yml").read_text()
        self.assertIn("bash ../../scripts/prepare-ci-video-tools.sh", workflow)
        self.assertNotIn("apt-get install -y --no-install-recommends ffmpeg", workflow)

if __name__ == "__main__":
    unittest.main()
