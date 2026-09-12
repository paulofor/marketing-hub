"""Executa o runner real até o primeiro gate, com Node real e infraestrutura simulada."""

import os
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
RUNNER = Path("infra/testing/process-automation/run-round.sh")


class RunnerContractTest(unittest.TestCase):
    def setUp(self):
        fixtures = ROOT / "artifacts/process-runner-contract"
        fixtures.mkdir(parents=True, exist_ok=True)
        self.temp = tempfile.TemporaryDirectory(prefix="fixture-", dir=fixtures)
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / RUNNER.parent).mkdir(parents=True)
        shutil.copy2(ROOT / RUNNER, self.root / RUNNER)
        shutil.copytree(ROOT / "process-execution-worker", self.root / "process-execution-worker")
        self.output = self.root / "artifacts/process-automation/contract"
        doubles = self.root / "bin"
        doubles.mkdir()
        self.env = {
            **os.environ,
            "PATH": str(doubles) + os.pathsep + os.environ["PATH"],
            "PROCESS_COMPOSE_PROJECT": "runner-contract-no-containers",
        }
        # O Maven encerra o teste no próximo gate; nenhum container ou banco é iniciado.
        for name, body in {
            "mvn": "touch maven-called\necho 'falha simulada do próximo gate'\nexit 37\n",
            "docker": '[[ "$*" == *"down --volumes --remove-orphans" ]] || exit 99\ntouch cleanup-called\n',
        }.items():
            path = doubles / name
            path.write_text("#!/usr/bin/env bash\nset -euo pipefail\n" + body)
            path.chmod(0o755)

    def run_runner(self):
        return subprocess.run(
            ["bash", str(RUNNER), "contract"],
            cwd=self.root,
            env=self.env,
            capture_output=True,
            text=True,
            timeout=30,
        )

    def test_discovers_worker_tests_and_preserves_next_failure(self):
        result = self.run_runner()
        self.assertEqual(result.returncode, 37, result.stdout + result.stderr)
        report = (self.output / "worker-tests.log").read_text()
        self.assertRegex(report, r"# tests [1-9][0-9]*")
        self.assertRegex(report, r"# fail 0\b")
        self.assertTrue((self.root / "maven-called").exists())
        self.assertIn("backend-tests.log", result.stderr)
        self.assertIn("falha simulada do próximo gate", result.stderr)
        self.assertTrue((self.root / "cleanup-called").exists())
        self.assertFalse((self.output / "result.txt").exists())

    def test_new_nested_failure_blocks_following_gates_and_remains_visible(self):
        nested = self.root / "process-execution-worker/test/nested"
        nested.mkdir()
        (nested / "sentinel.test.mjs").write_text(
            'import test from "node:test";\n'
            'test("sentinela de descoberta", () => { throw new Error("falha sentinela"); });\n'
        )
        result = self.run_runner()
        self.assertEqual(result.returncode, 1, result.stdout + result.stderr)
        report = (self.output / "worker-tests.log").read_text()
        self.assertIn("falha sentinela", report)
        self.assertRegex(report, r"# fail 1\b")
        self.assertIn("worker-tests.log", result.stderr)
        self.assertIn("falha sentinela", result.stderr)
        self.assertFalse((self.root / "maven-called").exists())
        self.assertTrue((self.root / "cleanup-called").exists())
        self.assertFalse((self.output / "result.txt").exists())


if __name__ == "__main__":
    unittest.main()
