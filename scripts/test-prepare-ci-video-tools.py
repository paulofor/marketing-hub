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
        for name in ("mkdir", "chmod", "dirname"):
            (self.bin / name).symlink_to(shutil.which(name))
        self.write("curl", '''printf 'download\\n' >> "$VIDEO_TEST_LOG"
[[ "${VIDEO_TEST_FAIL:-}" != 1 ]] || exit 48
''')
        self.write("sha256sum", '''[[ "${VIDEO_TEST_BAD_HASH:-}" != 1 ]] || exit 42
''')
        self.write("tar", '''printf '#!/bin/bash\\nexit 0\\n' > "$RUNNER_TEMP/pde-video-tools/ffmpeg"
printf '#!/bin/bash\\n[[ "$1" == -version ]] || echo 1080,1920\\n' > "$RUNNER_TEMP/pde-video-tools/ffprobe"
''')


    def write(self, name, body="exit 0\n"):
        p = self.bin / name
        p.write_text("#!/bin/bash\n" + body)
        p.chmod(0o755)

    def run_tools(self, fail=False, bad_hash=False):
        return subprocess.run([self.bash, str(ROOT / "scripts/prepare-ci-video-tools.sh")],
            env={"PATH": str(self.bin), "RUNNER_TEMP": str(self.root), "GITHUB_PATH": str(self.root / "path"),
                 "VIDEO_TEST_LOG": str(self.root / "calls"), "VIDEO_TEST_FAIL": str(int(fail)), "VIDEO_TEST_BAD_HASH": str(int(bad_hash))},
            text=True, capture_output=True, timeout=10)

    def test_existing_tools_do_not_pull_or_contact_package_mirrors(self):
        self.write("ffmpeg"); self.write("ffprobe", '[[ "$1" == -version ]] || echo 1080,1920\n')
        result = self.run_tools()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertFalse((self.root / "calls").exists())
        self.assertFalse((self.root / "path").exists())

    def test_missing_tools_validate_svg_before_exposing_pinned_binaries(self):
        result = self.run_tools()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual((self.root / "calls").read_text().strip(), "download")
        self.assertEqual((self.root / "path").read_text().strip(), str(self.root / "pde-video-tools"))
        script = (ROOT / "scripts/prepare-ci-video-tools.sh").read_text()
        self.assertIn("autobuild-2026-11-11-12-39", script)
        self.assertIn("32430dc6642d835511470728811e1555ec7ec09d4d27638b059f0e577a98d4e7", script)

    def test_wrong_svg_dimensions_fail_before_exposing_path(self):
        self.write("ffmpeg")
        self.write("ffprobe", '[[ "$1" == -version ]] || echo 1,1\n')
        result = self.run_tools()
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse((self.root / "path").exists())

    def test_hash_mismatch_does_not_extract_or_execute_download(self):
        result = self.run_tools(bad_hash=True)
        self.assertEqual(result.returncode, 42, result.stderr)
        self.assertFalse((self.root / "path").exists())
        self.assertFalse((self.root / "pde-video-tools/ffmpeg").exists())

    def test_failed_binary_download_never_exposes_missing_tools(self):
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
