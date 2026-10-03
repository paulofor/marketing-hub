#!/usr/bin/env python3
"""Homologa a auditoria de branches com históricos Git locais e isolados."""

import importlib.util
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from urllib.parse import unquote


SCRIPT = Path(__file__).with_name("audit-harness-research-branches.py")
spec = importlib.util.spec_from_file_location("research_audit", SCRIPT)
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)


class GitRepositoryFixture(unittest.TestCase):
    """Fornece histórico Git local com identidade de teste e arquivos isolados."""

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


class ResearchBranchesTest(GitRepositoryFixture):
    """Exercita integração, divergência, perda de história e isolamento do auditor."""

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

    def test_slash_radar_prefix_is_not_silently_ignored(self):
        self.branch("radar/agentes-inteligentes-2026-10-01")
        entry = self.report()["branches"][0]
        self.assertEqual(entry["branch"], "radar/agentes-inteligentes-2026-10-01")
        self.assertEqual(entry["status"], "PENDING")

    def test_shallow_history_blocks_inventory_before_classifying_integration(self):
        (self.repo / ".git/shallow").write_text(self.base + "\n")
        with self.assertRaisesRegex(RuntimeError, "Histórico raso"):
            self.report()


class GitHubFixture:
    """Simula somente metadados GitHub; remoções Git usam um remoto bare real e isolado."""

    repository = "fixture/harness"

    def __init__(self, sha):
        self.sha = sha
        self.protected = False
        self.pull_pages = [[]]
        self.run_pages = [{"workflow_runs": []}]
        self.before_runs = None
        self.calls = []

    def request(self, path, paginate=False):
        self.calls.append((path, paginate))
        if path == "":
            return {"default_branch": "main"}
        if path.startswith("/branches/"):
            return {"protected": self.protected, "commit": {"sha": self.sha}}
        if path.startswith("/pulls?"):
            return self.pull_pages
        if path.startswith("/actions/runs?"):
            if self.before_runs:
                self.before_runs()
            return self.run_pages
        raise AssertionError(f"Consulta inesperada: {path}")


class ResearchCleanupTest(GitRepositoryFixture):
    """Comprova limpeza, retomada e proteção contra perda e concorrência num Git real."""

    def setUp(self):
        super().setUp()
        self.remote_temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.remote_temporary.cleanup)
        self.remote = Path(self.remote_temporary.name) / "remote.git"
        subprocess.run(["git", "init", "--bare", "-q", str(self.remote)], check=True)
        self.git("remote", "add", "origin", "https://github.com/fixture/harness.git")
        self.git("config", f"url.{self.remote.as_uri()}.insteadOf", "https://github.com/fixture/harness.git")
        self.git("push", "-q", "origin", "main")

    def integrated(self, branch="automation/integrada"):
        sha = self.branch(branch)
        self.git("merge", "--ff-only", branch)
        self.git("push", "-q", "origin", "main", branch)
        return sha

    def cleanup(self, report=None, github=None):
        return audit.prune_integrated(self.repo, report or self.report(), github or self.github)

    def test_exact_integrated_tip_is_removed_and_history_remains_in_main(self):
        sha = self.integrated()
        self.github = GitHubFixture(sha)
        entry = self.cleanup()["branches"][0]
        self.assertEqual(entry["cleanup"]["status"], "DELETED")
        self.assertIsNone(audit.remote_head(self.repo, "automation/integrada"))
        self.assertEqual(audit.remote_head(self.repo, "main"), sha)
        self.git("merge-base", "--is-ancestor", sha, "main")
        self.assertEqual(self.git("status", "--porcelain"), "")

    def test_pending_and_out_of_scope_branches_are_never_deleted(self):
        sha = self.branch("radar/pendente")
        self.git("push", "-q", "origin", "radar/pendente")
        self.git("push", "-q", "origin", "main:refs/heads/ai-hub/compartilhada")
        self.github = GitHubFixture(sha)
        report = self.cleanup()
        self.assertNotIn("cleanup", report["branches"][0])
        self.assertEqual(audit.remote_head(self.repo, "radar/pendente"), sha)
        self.assertEqual(audit.remote_head(self.repo, "ai-hub/compartilhada"), self.base)

    def test_content_equality_without_ancestry_preserves_unique_history(self):
        sha = self.branch("automation/incorporada")
        self.git("push", "-q", "origin", "automation/incorporada")
        self.write("pesquisas/video/2026-10-01-video.md", "Pesquisa\n")
        self.commit("Integração independente")
        self.git("push", "-q", "origin", "main")
        self.github = GitHubFixture(sha)
        entry = self.cleanup()["branches"][0]
        self.assertEqual(entry["status"], "INCORPORATED")
        self.assertNotIn("cleanup", entry)
        self.assertEqual(audit.remote_head(self.repo, "automation/incorporada"), sha)

    def test_open_pr_or_waiting_run_on_later_pages_prevents_deletion(self):
        sha = self.integrated("radar/revisao")
        self.github = GitHubFixture(sha)
        self.github.pull_pages = [[], [{"number": 100}]]
        entry = self.cleanup()["branches"][0]
        self.assertIn("PR aberto", entry["cleanup"]["reason"])
        self.github.pull_pages = [[]]
        for status in ("queued", "in_progress", "waiting", "pending", "requested"):
            with self.subTest(status=status):
                self.github.run_pages = [{"workflow_runs": [{"status": "completed"}]},
                                         {"workflow_runs": [{"status": status}]}]
                entry = self.cleanup()["branches"][0]
                self.assertEqual(entry["cleanup"]["status"], "RETAINED")
                self.assertIn("workflow em curso", entry["cleanup"]["reason"])
        self.assertTrue(all(paginate for path, paginate in self.github.calls if "?" in path))
        self.assertTrue(any(unquote(path).startswith("/branches/radar/") for path, _ in self.github.calls))
        self.assertEqual(audit.remote_head(self.repo, "radar/revisao"), sha)

    def test_protected_branch_is_preserved(self):
        sha = self.integrated()
        self.github = GitHubFixture(sha)
        self.github.protected = True
        entry = self.cleanup()["branches"][0]
        self.assertEqual(entry["cleanup"]["status"], "RETAINED")
        self.assertIn("protegida", entry["cleanup"]["reason"])

    def test_tip_updated_after_audit_is_preserved(self):
        sha = self.integrated()
        report = self.report()
        self.write("pesquisas/video/nova.md", "Complemento ainda não integrado\n")
        newer = self.commit("Nova pesquisa")
        self.git("push", "-q", "origin", "main:refs/heads/automation/integrada")
        self.github = GitHubFixture(sha)
        entry = self.cleanup(report)["branches"][0]
        self.assertIn("novos commits", entry["cleanup"]["reason"])
        self.assertEqual(audit.remote_head(self.repo, "automation/integrada"), newer)

    def test_lease_preserves_a_push_between_last_check_and_deletion(self):
        sha = self.integrated()
        report = self.report()
        self.write("pesquisas/video/nova.md", "Complemento concorrente\n")
        newer = self.commit("Push concorrente")
        self.github = GitHubFixture(sha)
        self.github.before_runs = lambda: self.git("push", "-q", "origin", "main:refs/heads/automation/integrada")
        entry = self.cleanup(report)["branches"][0]
        self.assertEqual(entry["cleanup"]["status"], "RETAINED")
        self.assertIn("lease preservou", entry["cleanup"]["reason"])
        self.assertEqual(audit.remote_head(self.repo, "automation/integrada"), newer)

    def test_changed_main_or_wrong_origin_blocks_cleanup(self):
        sha = self.integrated()
        report = self.report()
        self.write("README.md", "Atualização da main\n")
        self.commit("Main avançou")
        self.git("push", "-q", "origin", "main")
        self.github = GitHubFixture(sha)
        entry = self.cleanup(report)["branches"][0]
        self.assertIn("main mudou", entry["cleanup"]["reason"])
        self.git("remote", "set-url", "origin", "https://github.com/outro/repositorio.git")
        with self.assertRaisesRegex(RuntimeError, "Origin não corresponde"):
            self.cleanup(report)

    def test_forged_integrated_status_is_rechecked(self):
        sha = self.branch("automation/pendente")
        self.git("push", "-q", "origin", "automation/pendente")
        report = self.report()
        report["branches"][0]["status"] = "INTEGRATED"
        self.github = GitHubFixture(sha)
        entry = self.cleanup(report)["branches"][0]
        self.assertIn("não é ancestral", entry["cleanup"]["reason"])
        self.assertEqual(audit.remote_head(self.repo, "automation/pendente"), sha)

    def test_retry_after_deletion_is_idempotent(self):
        sha = self.integrated()
        report = self.report()
        self.github = GitHubFixture(sha)
        self.cleanup(report)
        entry = self.cleanup(report)["branches"][0]
        self.assertEqual(entry["cleanup"]["status"], "ALREADY_REMOVED")

    def test_api_failure_is_reported_without_deleting_branch(self):
        sha = self.integrated()
        self.github = GitHubFixture(sha)
        self.github.before_runs = lambda: (_ for _ in ()).throw(RuntimeError("API indisponível"))
        entry = self.cleanup()["branches"][0]
        self.assertEqual(entry["cleanup"]["status"], "ERROR")
        self.assertEqual(audit.remote_head(self.repo, "automation/integrada"), sha)


class ResearchWorkflowTest(unittest.TestCase):
    """Impede execução de código de pesquisa no job com permissão para excluir refs."""

    def test_cleanup_is_serialized_and_only_uses_trusted_main(self):
        workflow = SCRIPT.parent.parent / ".github/workflows/harness-research-integration.yml"
        text = workflow.read_text()
        validation, cleanup = text.split("  audit:\n", 1)
        self.assertIn('"radar/**"', text)
        self.assertNotIn("contents: write", validation)
        self.assertIn("if: github.event_name != 'pull_request'", cleanup)
        self.assertIn("ref: main", cleanup)
        self.assertIn("fetch-depth: 0", cleanup)
        self.assertIn("contents: write", cleanup)
        self.assertIn("pull-requests: read", cleanup)
        self.assertIn("actions: read", cleanup)
        self.assertIn("group: harness-research-branch-maintenance", cleanup)
        self.assertIn("cancel-in-progress: false", cleanup)
        self.assertIn("--prune-integrated --github-repository", cleanup)
        self.assertNotIn("git merge", cleanup)


if __name__ == "__main__":
    unittest.main()
