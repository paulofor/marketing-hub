#!/usr/bin/env python3
import importlib.util
from datetime import datetime, timedelta, timezone
from pathlib import Path
import unittest
import sys

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
        ):
            self.assertIn(required, source)
        self.assertIn("FRESHNESS_GRACE_MINUTES: '30'", source)
        self.assertIn("MAX_DEPLOY_RUNTIME_MINUTES: '75'", source)

    def test_contract_workflow_reexecutes_on_implementation_changes(self):
        source = (ROOT / ".github/workflows/production-freshness-contract.yml").read_text()
        for path in (
            "scripts/check-production-freshness.py",
            "scripts/test-production-freshness.py",
            "scripts/detect-deployment-changes.sh",
            "scripts/read-frontend-build-revision.sh",
        ):
            self.assertGreaterEqual(source.count(path), 2)
        self.assertIn("python3 scripts/test-production-freshness.py", source)


if __name__ == "__main__":
    unittest.main(verbosity=2)
