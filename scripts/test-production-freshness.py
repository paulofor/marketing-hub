#!/usr/bin/env python3
import importlib.util
from datetime import datetime, timedelta, timezone
from pathlib import Path
import json
import subprocess
import tempfile
import unittest
import sys

import musa_pde_watchdog

ROOT = Path(__file__).resolve().parent.parent
spec = importlib.util.spec_from_file_location("freshness", ROOT / "scripts" / "check-production-freshness.py")
freshness = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = freshness
spec.loader.exec_module(freshness)

NOW = datetime(2026, 9, 15, 22, 0, tzinfo=timezone.utc)
BASE = "a" * 40
CHANGE = "b" * 40
HEAD = "c" * 40


class FakeRepo:
    def __init__(self, exists=None, ancestor=None, commits=None, times=None):
        self._exists = exists or {BASE, CHANGE, HEAD}
        self._ancestor = ancestor or {(BASE, CHANGE), (BASE, HEAD), (CHANGE, HEAD)}
        self._commits = commits or [CHANGE, HEAD]
        self._times = times or {CHANGE: NOW - timedelta(minutes=45), HEAD: NOW - timedelta(minutes=5)}

    def exists(self, sha):
        return sha in self._exists

    def is_ancestor(self, a, b):
        return a == b or (a, b) in self._ancestor

    def commits_between(self, base, head):
        return list(self._commits)

    def commit_time(self, sha):
        return self._times[sha]


class FakeDetector:
    def __init__(self, relevant=None):
        self.relevant = {CHANGE} if relevant is None else set(relevant)
        self.order = [BASE, CHANGE, HEAD]

    def changed(self, base, head, target):
        if base == head:
            return False
        left = self.order.index(base) + 1
        right = self.order.index(head) + 1
        return any(commit in self.relevant for commit in self.order[left:right])


def deploy(sha=HEAD, age=10):
    return freshness.LiveDeploy(1, sha, "in_progress", NOW - timedelta(minutes=age), "https://example/run/1")


class FreshnessTest(unittest.TestCase):
    def evaluate(self, *, base=BASE, detector=None, deploys=None, repo=None, grace=30, target="app"):
        return freshness.evaluate_target(
            target=target,
            observed_revision=base,
            head=HEAD,
            repo=repo or FakeRepo(),
            detector=detector or FakeDetector(),
            deploys=deploys or [],
            now=NOW,
            grace_minutes=grace,
        )

    def test_stale_uses_first_relevant_change_not_latest_main_commit(self):
        result = self.evaluate()
        self.assertEqual(result["status"], "STALE")
        self.assertEqual(result["first_pending_commit"], CHANGE)
        self.assertEqual(result["stale_minutes"], 45.0)

    def test_irrelevant_commits_do_not_make_production_stale(self):
        result = self.evaluate(detector=FakeDetector(relevant=set()))
        self.assertEqual(result["status"], "CURRENT")

    def test_recent_relevant_change_gets_grace(self):
        repo = FakeRepo(times={CHANGE: NOW - timedelta(minutes=10), HEAD: NOW - timedelta(minutes=2)})
        self.assertEqual(self.evaluate(repo=repo)["status"], "GRACE")

    def test_reverted_pending_interval_does_not_inherit_old_staleness_age(self):
        revert = "d" * 40
        repo = FakeRepo(
            commits=[CHANGE, revert, HEAD],
            times={CHANGE: NOW - timedelta(minutes=90), revert: NOW - timedelta(minutes=40), HEAD: NOW - timedelta(minutes=5)},
        )

        class SequenceDetector:
            states = {CHANGE: True, revert: False, HEAD: True}

            def changed(self, base, head, target):
                return self.states.get(head, False)

        result = self.evaluate(repo=repo, detector=SequenceDetector())
        self.assertEqual(result["status"], "GRACE")
        self.assertEqual(result["first_pending_commit"], HEAD)
        self.assertEqual(result["stale_minutes"], 5.0)

    def test_live_deploy_covering_change_prevents_false_alarm(self):
        result = self.evaluate(deploys=[deploy(HEAD)])
        self.assertEqual(result["status"], "DEPLOYING")
        self.assertEqual(result["deploy_run_id"], 1)

    def test_old_live_deploy_is_not_considered_valid(self):
        payload = {"workflow_runs": [{
            "id": 1, "name": freshness.DEPLOY_WORKFLOW, "head_branch": "main", "head_sha": HEAD,
            "status": "in_progress", "created_at": (NOW - timedelta(minutes=90)).isoformat(), "html_url": "x"
        }]}
        self.assertEqual(freshness.live_deploys(payload, NOW, 75), [])

    def test_unknown_revision_is_stale_without_current_deploy(self):
        result = self.evaluate(base="MISSING")
        self.assertEqual(result["status"], "STALE")

    def test_unknown_revision_is_tolerated_during_current_deploy(self):
        result = self.evaluate(base="MISSING", deploys=[deploy(HEAD)])
        self.assertEqual(result["status"], "DEPLOYING")

    def test_previous_deploy_that_contains_change_can_cover_newer_irrelevant_head(self):
        run = deploy(CHANGE)
        result = self.evaluate(deploys=[run], detector=FakeDetector(relevant={CHANGE}))
        self.assertEqual(result["status"], "DEPLOYING")

    def test_previous_deploy_does_not_cover_a_later_relevant_change(self):
        run = deploy(CHANGE)
        result = self.evaluate(deploys=[run], detector=FakeDetector(relevant={CHANGE, HEAD}))
        self.assertEqual(result["status"], "STALE")

    def test_diverged_revision_is_stale(self):
        repo = FakeRepo(ancestor={(CHANGE, HEAD)})
        result = self.evaluate(repo=repo)
        self.assertEqual(result["status"], "STALE")
        self.assertIn("não é ancestral", result["reason"])

    def test_overall_status(self):
        self.assertEqual(freshness.overall([{"status": "CURRENT"}, {"status": "CURRENT"}]), "FRESH")
        self.assertEqual(freshness.overall([{"status": "CURRENT"}, {"status": "GRACE"}]), "PENDING")
        self.assertEqual(freshness.overall([{"status": "DEPLOYING"}, {"status": "STALE"}]), "STALE")

    def test_psique_uses_its_own_deploy_workflow_and_change_key(self):
        self.assertEqual(freshness.TARGET_KEYS["psique"], "customer_agent")
        payload = {"workflow_runs": [{
            "id": 2, "name": freshness.CUSTOMER_AGENT_DEPLOY_WORKFLOW,
            "head_branch": "main", "head_sha": HEAD, "status": "in_progress",
            "created_at": (NOW - timedelta(minutes=5)).isoformat(), "html_url": "x"
        }]}
        runs = freshness.live_deploys(
            payload, NOW, 75, freshness.CUSTOMER_AGENT_DEPLOY_WORKFLOW)
        self.assertEqual([run.id for run in runs], [2])
        self.assertEqual(self.evaluate(target="psique", deploys=runs)["status"], "DEPLOYING")

    def test_musa_pde_uses_its_own_deploy_workflow(self):
        payload = {"workflow_runs": [{
            "id": 3, "name": freshness.MUSA_PDE_DEPLOY_WORKFLOW,
            "head_branch": "main", "head_sha": HEAD, "status": "in_progress",
            "created_at": (NOW - timedelta(minutes=5)).isoformat(), "html_url": "x"
        }]}
        runs = freshness.live_deploys(
            payload, NOW, 75, freshness.MUSA_PDE_DEPLOY_WORKFLOW)
        self.assertEqual([run.id for run in runs], [3])
        self.assertEqual(self.evaluate(target="musa_pde", deploys=runs)["status"], "DEPLOYING")


class MusaPdeWatchdogTest(unittest.TestCase):
    def publication(self, **overrides):
        values = {
            "path": "pde-platform/contracts/musa-v12-v5.json",
            "contract_version": "musa-v12.v5",
            "process_version": 5,
            "experience_version": "musa-pde-entry-v12",
            "frontend_version": "v8",
            "public_url": "https://v8.example.com",
            "source_sha256": "d" * 64,
        }
        values.update(overrides)
        return musa_pde_watchdog.MusaPublication(**values)

    def contract(self, *, experience="musa-pde-entry-v12", revision=5, frontend="v8"):
        return {
            "contractVersion": f"musa-v12-commercial.v{revision}",
            "processVersion": revision,
            "status": "READY_FOR_INDEPENDENT_REVIEW",
            "product": {
                "id": 4,
                "slug": "metodo-musa-7-dias",
                "experienceVersion": experience,
            },
            "publicationContract": {
                "automaticDeployOnMerge": True,
                "frontendVersion": frontend,
                "publicUrl": f"https://{frontend}.clubemusa.com.br/",
                "requiredFrontendSourceSha256": "d" * 64,
            },
        }

    def test_selects_current_manifest_without_fixing_v8(self):
        current = self.contract(experience="musa-pde-entry-v13", revision=1, frontend="v9")
        selected = musa_pde_watchdog.select_current_publication({
            "pde-platform/contracts/musa-v12-v5.json": self.contract(),
            "pde-platform/contracts/musa-v13-v1.json": current,
        })
        self.assertEqual(selected.frontend_version, "v9")
        self.assertEqual(selected.experience_version, "musa-pde-entry-v13")

    def test_ignores_other_products_and_non_publicable_contracts(self):
        other = self.contract()
        other["product"]["id"] = 10
        draft = self.contract(revision=6)
        draft["publicationContract"]["automaticDeployOnMerge"] = False
        selected = musa_pde_watchdog.select_current_publication({
            "other.json": other,
            "draft.json": draft,
            "current.json": self.contract(),
        })
        self.assertEqual(selected.path, "current.json")

    def test_public_probe_validates_build_and_pixels_identity(self):
        publication = self.publication()
        documents = {
            "https://v8.example.com/healthz": {"status": "UP"},
            "https://v8.example.com/version-diagnostics.json": {
                "status": "UP",
                "surface": "pde-platform-frontend",
                "version": "v8",
                "imageVersionId": "v8",
                "publicUrl": "https://v8.example.com/",
                "experienceVersion": "musa-pde-entry-v12",
                "productSlug": "metodo-musa-7-dias",
                "frontendSourceSha256": "d" * 64,
                "commitSha": BASE,
            },
        }
        revision = musa_pde_watchdog.probe_publication(
            publication, reader=documents.__getitem__)
        self.assertEqual(revision, BASE)

    def test_public_probe_rejects_stale_source_fingerprint(self):
        publication = self.publication()
        with self.assertRaisesRegex(ValueError, "frontendSourceSha256"):
            musa_pde_watchdog.validate_public_diagnostics(
                publication,
                {"status": "UP"},
                {
                    "status": "UP",
                    "surface": "pde-platform-frontend",
                    "version": "v8",
                    "imageVersionId": "v8",
                    "publicUrl": "https://v8.example.com",
                    "experienceVersion": "musa-pde-entry-v12",
                    "productSlug": "metodo-musa-7-dias",
                    "frontendSourceSha256": "e" * 64,
                    "commitSha": BASE,
                },
            )

    def test_repository_current_manifest_is_the_musa_v12_v5_candidate(self):
        selected = musa_pde_watchdog.publication_at(ROOT, "HEAD")
        self.assertEqual(selected.frontend_version, "v8")
        self.assertEqual(selected.experience_version, "musa-pde-entry-v12-primeiro-ajuste-aplicavel")

    def test_only_a_new_publication_contract_makes_musa_pde_pending(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            subprocess.run(["git", "init", "-q"], cwd=root, check=True)
            subprocess.run(
                ["git", "config", "user.email", "watchdog@sandbox.local"],
                cwd=root,
                check=True,
            )
            subprocess.run(
                ["git", "config", "user.name", "Watchdog Test"], cwd=root, check=True
            )
            contracts = root / "pde-platform" / "contracts"
            contracts.mkdir(parents=True)
            (contracts / "musa-v12-v5.json").write_text(json.dumps(self.contract()))
            subprocess.run(["git", "add", "."], cwd=root, check=True)
            subprocess.run(["git", "commit", "-qm", "base"], cwd=root, check=True)
            base = subprocess.run(
                ["git", "rev-parse", "HEAD"],
                cwd=root,
                text=True,
                capture_output=True,
                check=True,
            ).stdout.strip()

            (root / "README.md").write_text("mudanca sem promocao\n")
            subprocess.run(["git", "add", "."], cwd=root, check=True)
            subprocess.run(["git", "commit", "-qm", "docs"], cwd=root, check=True)
            unrelated = subprocess.run(
                ["git", "rev-parse", "HEAD"],
                cwd=root,
                text=True,
                capture_output=True,
                check=True,
            ).stdout.strip()
            self.assertFalse(musa_pde_watchdog.publication_changed(root, base, unrelated))

            (contracts / "musa-v13-v1.json").write_text(
                json.dumps(
                    self.contract(
                        experience="musa-pde-entry-v13", revision=1, frontend="v9"
                    )
                )
            )
            subprocess.run(["git", "add", "."], cwd=root, check=True)
            subprocess.run(["git", "commit", "-qm", "promove v13"], cwd=root, check=True)
            promoted = subprocess.run(
                ["git", "rev-parse", "HEAD"],
                cwd=root,
                text=True,
                capture_output=True,
                check=True,
            ).stdout.strip()
            self.assertTrue(musa_pde_watchdog.publication_changed(root, unrelated, promoted))


class WorkflowContractTest(unittest.TestCase):
    def test_watchdog_workflow_contract(self):
        source = (ROOT / ".github/workflows/production-freshness-watchdog.yml").read_text()
        for required in (
            "cron: '*/5 * * * *'",
            "issues: write",
            ".deployed-app-revision",
            "http://localhost:5173/healthz",
            "read-frontend-build-revision.sh",
            "check-production-freshness.py",
            "Build & Deploy containers",
            "[Watchdog] Produção desatualizada",
            "fetch-depth: 0",
            "deploy-control-known-hosts",
            "PSIQUE_VPS_IP",
            "Configure Psique VPS SSH",
            "Customer Agent Worker CI/CD",
            "CI - PDE Platform Metodo MUSA",
            "customer-agent-worker-ci.yml/runs",
            "pde-platform-metodo-musa-ci.yml/runs",
            "--psique-revision",
            "--psique-runs-json",
            "musa_pde_watchdog.py",
            "--musa-pde-revision",
            "--musa-pde-runs-json",
        ):
            self.assertIn(required, source)
        self.assertIn("FRESHNESS_GRACE_MINUTES: '30'", source)
        self.assertIn("MAX_DEPLOY_RUNTIME_MINUTES: '75'", source)

    def test_contract_workflow_reexecutes_on_implementation_changes(self):
        source = (ROOT / ".github/workflows/production-freshness-contract.yml").read_text()
        for path in (
            "scripts/check-production-freshness.py",
            "scripts/test-production-freshness.py",
            "scripts/musa_pde_watchdog.py",
            "scripts/detect-deployment-changes.sh",
            "scripts/read-frontend-build-revision.sh",
            "scripts/configure-vps-ssh-fallback.sh",
        ):
            self.assertGreaterEqual(source.count(path), 2)
        self.assertIn("python3 scripts/test-production-freshness.py", source)


if __name__ == "__main__":
    unittest.main(verbosity=2)
