#!/usr/bin/env python3
"""Homologa a auditoria de branches com históricos Git locais e isolados."""

import importlib.util
import json
from pathlib import Path
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).with_name("audit-harness-research-branches.py")
spec = importlib.util.spec_from_file_location("research_audit", SCRIPT)
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)


class ResearchBranchesTest(unittest.TestCase):
    """Exercita integração, divergência, perda de história e isolamento do auditor."""

    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.repo = Path(self.temporary.name)
        self.git("init", "-q", "-b", "main")
        self.git("config", "user.name", "Homologação local")
        self.git("config", "user.email", "teste@sandbox.local")
        self.write("README.md", "fixture isolada\n")
        self.base = self.commit("Base")

    def git(self, *args):
        return subprocess.check_output(["git", "-C", str(self.repo), *args], text=True).strip()

    def write(self, path, content):
        target = self.repo / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content)

    def commit(self, message):
        self.git("add", "-A")
        self.git("commit", "-q", "-m", message)
        return self.git("rev-parse", "HEAD")

    def branch(self, name, path="pesquisas/video/2026-10-01-video.md", content="Pesquisa\n"):
        self.git("checkout", "-q", "-b", name, self.base)
        self.write(path, content)
        sha = self.commit(name)
        self.git("update-ref", f"refs/remotes/origin/{name}", sha)
        self.git("checkout", "-q", "main")
        return sha

    def report(self):
        return audit.inventory(self.repo, "main")

    def test_integrated_and_pending_preserve_exact_head_and_do_not_modify_git(self):
        self.git("update-ref", "refs/remotes/origin/automation/gartner-integrada", self.base)
        sha = self.branch("radar-video-pendente")
        self.git("update-ref", "refs/remotes/origin/fix/fora-do-escopo", sha)
        before = self.git("show-ref")
        entries = {entry["branch"]: entry for entry in self.report()["branches"]}
        self.assertEqual(entries["automation/gartner-integrada"]["status"], "INTEGRATED")
        self.assertEqual(entries["radar-video-pendente"]["status"], "PENDING")
        self.assertEqual(entries["radar-video-pendente"]["sha"], sha)
        self.assertEqual(len(entries), 2)
        self.assertEqual(before, self.git("show-ref"))
        self.assertEqual(self.git("status", "--porcelain"), "")

    def test_cherry_picked_content_is_incorporated_without_redundant_merge(self):
        self.branch("automation/video")
        self.write("pesquisas/video/2026-10-01-video.md", "Pesquisa\n")
        self.commit("Entrega independente com a mesma árvore")
        self.assertEqual(self.report()["branches"][0]["status"], "INCORPORATED")

    def test_concurrent_revision_requires_reconciliation(self):
        path = "pesquisas/video/2026-10-01-video.md"
        self.write(path, "Inicial\n")
        self.base = self.commit("Fonte inicial")
        self.branch("radar-revisao", path, "Revisão da branch\n")
        self.write(path, "Correção mais nova da main\n")
        self.commit("Correção da main")
        entry = self.report()["branches"][0]
        self.assertEqual(entry["status"], "PENDING")
        self.assertEqual(entry["concurrentPaths"], [path])
        self.assertEqual((self.repo / path).read_text(), "Correção mais nova da main\n")

    def test_temporary_files_and_code_are_blocked(self):
        self.branch("automation/mista", "pesquisas/video/source.md.tmp", "temporário\n")
        self.branch("radar-codigo", "backend/service.java", "código\n")
        entries = self.report()["branches"]
        self.assertTrue(all(entry["status"] == "BLOCKED" for entry in entries))
        self.assertEqual(len(entries), 2)

    def test_all_research_files_including_json_and_source_hash_are_allowed(self):
        self.git("checkout", "-q", "-b", "automation/cards", self.base)
        for path in ("pesquisas/ia-aplicada/2026-10-01.md", "pesquisas/ia-aplicada/cards/card.json",
                     "pesquisas/ia-aplicada/cards/fontes/card.md", "pesquisas/ia-aplicada/cards/fontes/card.sha256"):
            self.write(path, "fixture\n")
        sha = self.commit("Conjunto completo")
        self.git("update-ref", "refs/remotes/origin/automation/cards", sha)
        self.git("checkout", "-q", "main")
        self.assertEqual(self.report()["branches"][0]["status"], "PENDING")
        self.assertEqual(len(self.report()["branches"][0]["paths"]), 4)

    def test_symlink_is_blocked_without_reading_target(self):
        self.git("checkout", "-q", "-b", "radar-link", self.base)
        target = self.repo / "pesquisas/video/source.md"
        target.parent.mkdir(parents=True)
        target.symlink_to("/arquivo-inexistente-fora-da-fixture")
        sha = self.commit("Fonte externa por link")
        self.git("update-ref", "refs/remotes/origin/radar-link", sha)
        self.git("checkout", "-q", "main")
        self.assertEqual(self.report()["branches"][0]["status"], "BLOCKED")

    def test_missing_common_history_is_explicit_block(self):
        self.git("checkout", "-q", "--orphan", "radar-orfa")
        self.git("rm", "-q", "-rf", ".")
        self.write("pesquisas/video/fonte.md", "Origem independente\n")
        sha = self.commit("Outra origem")
        self.git("update-ref", "refs/remotes/origin/radar-orfa", sha)
        self.git("checkout", "-q", "main")
        entry = self.report()["branches"][0]
        self.assertEqual(entry["status"], "BLOCKED")
        self.assertIn("Histórico comum", entry["reason"])

    def test_cli_keeps_report_and_summary_when_pending_fails(self):
        self.branch("radar-pendente")
        output, summary = self.repo / "report.json", self.repo / "summary.md"
        result = subprocess.run(
            ["python3", str(SCRIPT), "--repository", str(self.repo), "--base", "main",
             "--output", str(output), "--summary", str(summary), "--fail-pending"],
            capture_output=True, text=True,
        )
        self.assertEqual(result.returncode, 1)
        self.assertEqual(json.loads(output.read_text())["branches"][0]["status"], "PENDING")
        self.assertIn("radar-pendente", summary.read_text())
        self.assertIn("pendências: 1", result.stdout)

    def test_empty_inventory_succeeds(self):
        self.assertEqual(self.report()["branches"], [])


if __name__ == "__main__":
    unittest.main()
