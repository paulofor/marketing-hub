#!/usr/bin/env python3
"""Homologa recuperação com GitHub simulado e armazenamento/CLI reais, sem mutações externas."""

import copy
import importlib.util
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import textwrap
import unittest
from unittest.mock import patch

from deploy_publisher_recovery import APP, POLICY, PublisherRecovery

ROOT = Path(__file__).resolve().parent.parent


def load(name, file):
    """Carrega contratos CLI sem executá-los como comandos operacionais."""
    spec = importlib.util.spec_from_file_location(name, ROOT / "scripts" / file)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


base = load("intervention_tests", "test-deploy-intervention.py")
guard = load("recovery_guard", "validate-publisher-recovery.py")
module = base.module
SHA, VALIDATED = base.SHA, base.INTEGRATED


class RecoveryGitHub(base.FakeGitHub):
    """Simula push, dispatch, continuações, revisão concorrente e perda de recibos."""

    def __init__(self):
        super().__init__()
        self.sha = SHA
        self.completed_runs = {}
        self.jobs = {}
        self.dispatches = []
        self.lose_receipt = False

    def api(self, path, method="GET", payload=None):
        if path == "git/ref/heads/main":
            return {"object": {"sha": self.sha}}
        if re.fullmatch(r"actions/runs/\d+", path):
            return copy.deepcopy(next(run for runs in self.completed_runs.values() for run in runs
                                      if run["id"] == int(path.split("/")[-1])))
        if path.endswith("/dispatches"):
            self.calls.append((method, path))
            assert method == "POST" and payload["ref"] == "main"
            assert payload["inputs"]["recovery_sha"] == self.sha
            name = next(k for k, w in self.workflows.items() if str(w["id"]) == path.split("/")[2])
            run = self.add_run(name, status="queued", conclusion=None, event="workflow_dispatch")
            self.dispatches.append((name, copy.deepcopy(payload)))
            if self.lose_receipt:
                raise module.CoordinationError("Timeout após aceite simulado.")
            return {"workflow_run_id": run["id"], "html_url": run["html_url"]}
        return super().api(path, method)

    def add_run(self, file, status="completed", conclusion="success", event="push", **override):
        """Cria execução rastreável posterior à homologação sintética."""
        identifier = 100 + sum(len(runs) for runs in self.completed_runs.values())
        run = {"id": identifier, "status": status, "conclusion": conclusion, "event": event,
               "workflow_id": self.workflows[file]["id"],
               "head_sha": self.sha, "head_branch": "main", "created_at": "2099-01-01T00:00:00Z",
               "html_url": f"https://github.com/test/actions/runs/{identifier}", **override}
        self.completed_runs.setdefault(file, []).append(run)
        return run

    def runs_for(self, file, sha, since):
        """Deixa os filtros para o código real, exercitando sua validação defensiva."""
        return copy.deepcopy(self.completed_runs.get(file, []))

    def pages(self, path, field):
        """Expõe os jobs que comprovam publicação, distinguindo continuação vazia."""
        assert field == "jobs"
        return copy.deepcopy(self.jobs.get(int(path.split("/")[2]), []))

    def live_runs(self, workflow_id):
        """Inclui solicitações aceitas nas consultas de fila de qualquer revisão."""
        name = next(k for k, value in self.workflows.items() if value["id"] == workflow_id)
        return super().live_runs(workflow_id) + [copy.deepcopy(r) for r in self.completed_runs.get(name, [])
                                                if r["status"] != "completed"]


class AutomaticRecoveryTest(unittest.TestCase):
    """Exercita estado persistente, proteção e integração com dependências locais."""

    def setUp(self):
        self.fixture = base.LocalCoordinationTest()
        self.fixture.setUp()
        self.github = RecoveryGitHub()
        self.store = self.fixture.store
        self.coordinator = module.Coordinator(self.github, self.store, settle=lambda: None)
        self.recovery = PublisherRecovery(self.coordinator)

    def tearDown(self):
        self.fixture.tearDown()

    def begin(self, scopes=None):
        """Abre somente uma intervenção simulada."""
        return self.coordinator.begin(scopes or ["temis"], "teste", "homologação local", "autorização sintética", "v1")

    def prepare(self, scopes=None):
        """Encerra a homologação para retomar somente após integração comprovada."""
        state = self.begin(scopes)
        return self.coordinator.prepare_resume(state["id"], VALIDATED, "duas rodadas locais aprovadas")

    def test_complete_pause_merge_dispatch_completion_and_no_further_work(self):
        self.prepare()
        self.assertEqual(self.recovery.reconcile()["status"], "WAITING")
        self.assertEqual(self.store.load()["phase"], "RELEASED")
        self.assertEqual(len(self.github.dispatches), 1)
        self.recovery.reconcile()
        self.assertEqual(len(self.github.dispatches), 1)
        self.github.completed_runs["communication-agent-worker-ci.yml"][0].update(status="completed", conclusion="success")
        self.assertEqual(self.recovery.reconcile()["status"], "COMPLETE")
        self.github.sha = "c" * 40
        self.assertEqual(self.recovery.reconcile()["status"], "COMPLETE")
        self.assertEqual(len(self.github.dispatches), 1)

    def test_old_active_and_released_records_do_not_gain_authorization(self):
        state = self.begin()
        self.assertEqual(self.recovery.reconcile()["status"], "NO_ACTION")
        self.coordinator.resume(state["id"], VALIDATED, "validado")
        self.assertEqual(self.recovery.reconcile()["status"], "NO_ACTION")
        self.assertEqual(self.github.dispatches, [])

    def test_time_and_unmerged_revision_never_release_hold(self):
        self.prepare()
        self.github.integrated = False
        for _ in range(3):
            self.assertEqual(self.recovery.reconcile()["status"], "WAITING")
        self.assertEqual(self.store.load()["phase"], "AWAITING_MERGE")
        self.assertEqual(self.github.dispatches, [])

    def test_preparation_requires_full_commit_evidence_and_idle_operation(self):
        state = self.begin()
        for sha, evidence in [("main", "teste"), (VALIDATED, " ")]:
            with self.assertRaises(module.CoordinationError):
                self.coordinator.prepare_resume(state["id"], sha, evidence)
        state["phase"] = "OPERATING"
        self.store.save(state)
        with self.assertRaises(module.CoordinationError):
            self.coordinator.prepare_resume(state["id"], VALIDATED, "teste")

    def test_closed_validation_rejects_more_runtime_commands(self):
        state = self.prepare()
        with patch.object(module.subprocess, "run") as run:
            with self.assertRaises(module.CoordinationError):
                self.coordinator.execute(state["id"], ["temis"], ["docker"])
            run.assert_not_called()
        with self.assertRaises(module.CoordinationError):
            self.coordinator.protect(state["id"])

    def test_previously_disabled_publisher_and_proxy_are_never_dispatched(self):
        self.github.workflows["customer-agent-worker-ci.yml"]["state"] = "disabled_manually"
        self.prepare(["app", "pde"])
        self.recovery.reconcile()
        names = [name for name, _ in self.github.dispatches]
        self.assertNotIn("customer-agent-worker-ci.yml", names)
        self.assertNotIn("recover-public-proxy.yml", names)
        self.assertEqual(self.github.workflows["customer-agent-worker-ci.yml"]["state"], "disabled_manually")
        pde = next(payload for name, payload in self.github.dispatches if name.startswith("pde-platform"))
        self.assertEqual(pde["inputs"]["frontend_version"], "all")

    def test_app_first_then_agents_with_same_revision_and_successful_jobs(self):
        self.prepare(["app"])
        self.recovery.reconcile()
        self.assertEqual([name for name, _ in self.github.dispatches], [APP])
        self.github.completed_runs[APP][0].update(status="completed", conclusion="success")
        self.recovery.reconcile()
        self.assertEqual(len(self.github.dispatches), 4)
        for name in POLICY:
            if POLICY[name].get("requires_app"):
                run = self.github.completed_runs[name][0]
                run.update(status="completed", conclusion="success")
                self.github.jobs[run["id"]] = [{"name": POLICY[name]["publication_job"], "conclusion": "success"}]
        self.assertEqual(self.recovery.reconcile()["status"], "COMPLETE")

    def test_existing_success_is_reused_without_dispatch(self):
        self.prepare()
        self.github.add_run("communication-agent-worker-ci.yml")
        self.assertEqual(self.recovery.reconcile()["status"], "COMPLETE")
        self.assertEqual(self.github.dispatches, [])

    def test_existing_live_run_is_reused_without_dispatch(self):
        self.prepare()
        self.github.add_run("communication-agent-worker-ci.yml", status="queued", conclusion=None)
        self.assertEqual(self.recovery.reconcile()["status"], "WAITING")
        self.assertEqual(self.github.dispatches, [])

    def test_historical_same_sha_success_is_not_proof_of_recovery(self):
        self.prepare()
        self.github.add_run("communication-agent-worker-ci.yml", created_at="2020-01-01T00:00:00Z")
        self.recovery.reconcile()
        self.assertEqual(len(self.github.dispatches), 1)

    def test_wrong_sha_branch_or_pr_is_not_reused(self):
        self.prepare()
        for overrides in ({"head_sha": "c" * 40}, {"head_branch": "topic"}, {"event": "pull_request"}):
            self.github.add_run("communication-agent-worker-ci.yml", **overrides)
        self.recovery.reconcile()
        self.assertEqual(len(self.github.dispatches), 1)

    def test_failed_build_is_reported_without_retry_loop(self):
        self.prepare()
        self.github.add_run("communication-agent-worker-ci.yml", conclusion="failure")
        for _ in range(3):
            self.assertEqual(self.recovery.reconcile()["status"], "BLOCKED")
        self.assertEqual(self.github.dispatches, [])

    def test_uncertain_dispatch_is_never_duplicated_after_disconnect(self):
        self.prepare()
        self.github.lose_receipt = True
        with self.assertRaises(module.CoordinationError):
            self.recovery.reconcile()
        hidden = self.github.completed_runs.pop("communication-agent-worker-ci.yml")
        self.github.lose_receipt = False
        for _ in range(2):
            self.recovery.reconcile()
        self.assertEqual(len(self.github.dispatches), 1)
        self.github.completed_runs["communication-agent-worker-ci.yml"] = hidden
        self.recovery.reconcile()
        self.assertEqual(len(self.github.dispatches), 1)

    def test_api_outage_does_not_release_or_dispatch(self):
        self.prepare()
        self.github.fail = lambda path, method: path.startswith("compare/")
        with self.assertRaises(module.CoordinationError):
            self.recovery.reconcile()
        self.assertEqual(self.store.load()["phase"], "AWAITING_MERGE")
        self.assertEqual(self.github.dispatches, [])

    def test_partial_enable_is_recovered_through_original_resume(self):
        self.prepare(["app"])
        target = self.github.workflows["customer-agent-worker-ci.yml"]["id"]
        self.github.fail = lambda path, method: path == f"actions/workflows/{target}/enable"
        with self.assertRaises(module.CoordinationError):
            self.recovery.reconcile()
        self.assertEqual(self.store.load()["phase"], "RESUMING")
        self.github.fail = None
        self.recovery.reconcile()
        self.assertEqual(self.store.load()["phase"], "RELEASED")

    def test_partial_enable_with_late_run_keeps_draining_until_it_finishes(self):
        self.prepare(["app"])
        target = self.github.workflows["customer-agent-worker-ci.yml"]["id"]
        self.github.fail = lambda path, method: path == f"actions/workflows/{target}/enable"
        with self.assertRaises(module.CoordinationError):
            self.recovery.reconcile()
        self.github.fail = None
        run = self.github.add_run("product-discovery-worker-ci.yml", status="in_progress", conclusion=None)
        for _ in range(2):
            self.assertEqual(self.recovery.reconcile()["status"], "WAITING")
            self.assertEqual(self.store.load()["phase"], "DRAINING")
        run.update(status="completed", conclusion="success")
        self.recovery.reconcile()
        self.assertEqual(self.store.load()["phase"], "RELEASED")

    def test_concurrent_operator_is_excluded_by_real_lock(self):
        self.prepare()
        with module.RemoteStore(self.fixture.command) as other:
            coordinator = module.Coordinator(self.github, other, settle=lambda: None)
            with self.assertRaises(module.CoordinationError):
                PublisherRecovery(coordinator).reconcile()
        self.assertEqual(self.github.dispatches, [])

    def test_new_intervention_prevents_old_recovery_from_running(self):
        self.prepare()
        self.recovery.reconcile()
        self.begin(["pde"])
        count = len(self.github.dispatches)
        self.assertEqual(self.recovery.reconcile()["status"], "NO_ACTION")
        self.assertEqual(len(self.github.dispatches), count)

    def test_main_drift_reconciles_new_revision_instead_of_old_run(self):
        self.prepare()
        self.recovery.reconcile()
        self.github.sha = "c" * 40
        self.recovery.reconcile()
        self.assertEqual(len(self.github.dispatches), 1)
        self.github.completed_runs["communication-agent-worker-ci.yml"][0].update(status="completed", conclusion="success")
        self.recovery.reconcile()
        self.assertEqual(self.github.dispatches[-1][1]["inputs"]["recovery_sha"], "c" * 40)

    def test_main_drift_does_not_duplicate_uncertain_previous_dispatch(self):
        self.prepare()
        self.github.lose_receipt = True
        with self.assertRaises(module.CoordinationError):
            self.recovery.reconcile()
        self.github.completed_runs.clear()
        self.github.sha = "c" * 40
        self.github.lose_receipt = False
        self.recovery.reconcile()
        self.assertEqual(len(self.github.dispatches), 1)

    def test_main_moves_between_check_and_dispatch_recovers_current_revision(self):
        self.prepare()
        original = self.github.api

        def drift(path, method="GET", payload=None):
            result = original(path, method, payload)
            if path.endswith("/dispatches"):
                self.github.sha = "c" * 40
                self.github.completed_runs["communication-agent-worker-ci.yml"][-1]["head_sha"] = self.github.sha
            return result

        with patch.object(self.github, "api", side_effect=drift):
            self.recovery.reconcile()
        mismatched = self.github.completed_runs["communication-agent-worker-ci.yml"][0]
        env = RecoveryContractsTest().environment("communication-agent-worker-ci.yml")
        with self.assertRaises(ValueError):
            guard.validate(self.github, {**env, "GITHUB_SHA": mismatched["head_sha"]})
        mismatched.update(status="completed", conclusion="failure")
        self.recovery.reconcile()
        self.assertEqual(len(self.github.dispatches), 2)
        self.assertEqual(self.github.dispatches[-1][1]["inputs"]["recovery_sha"], "c" * 40)

    def test_disabled_after_resume_is_not_silently_reenabled(self):
        self.prepare()
        self.recovery.reconcile()
        self.github.workflows["communication-agent-worker-ci.yml"]["state"] = "disabled_manually"
        self.assertEqual(self.recovery.reconcile()["status"], "BLOCKED")
        self.assertEqual(self.github.workflows["communication-agent-worker-ci.yml"]["state"], "disabled_manually")

    def test_completed_push_without_continuation_is_recovered_after_app(self):
        self.prepare(["app"])
        self.github.add_run(APP)
        for name in POLICY:
            if POLICY[name].get("requires_app"):
                self.github.add_run(name)
        self.recovery.reconcile()
        self.assertEqual(len(self.github.dispatches), 3)

    def test_noop_continuation_is_not_mistaken_for_deployed_agent(self):
        self.prepare(["app"])
        self.github.add_run(APP)
        for name in POLICY:
            if POLICY[name].get("requires_app"):
                self.github.add_run(name, event="workflow_run")
        self.recovery.reconcile()
        self.assertEqual(len(self.github.dispatches), 3)

    def test_successful_continuation_is_reused_by_actual_publication_job(self):
        self.prepare(["app"])
        self.github.add_run(APP)
        for name in POLICY:
            if POLICY[name].get("requires_app"):
                run = self.github.add_run(name, event="workflow_run")
                self.github.jobs[run["id"]] = [{"name": POLICY[name]["publication_job"], "conclusion": "success"}]
        self.assertEqual(self.recovery.reconcile()["status"], "COMPLETE")
        self.assertEqual(self.github.dispatches, [])


class RecoveryContractsTest(unittest.TestCase):
    """Protege revisão, credenciais, gatilhos e integração de todos os publicadores."""

    def environment(self, workflow=APP):
        """Fornece somente identificadores sintéticos de um evento oficial."""
        return {"RECOVERY_SHA": SHA, "GITHUB_SHA": SHA, "GITHUB_REF": "refs/heads/main",
                "GITHUB_EVENT_NAME": "workflow_dispatch", "GITHUB_REPOSITORY": module.REPOSITORY,
                "GITHUB_WORKFLOW_REF": f"{module.REPOSITORY}/.github/workflows/{workflow}@refs/heads/main"}

    def test_guard_preserves_normal_execution_and_validates_recovery(self):
        github = RecoveryGitHub()
        guard.validate(github, {})
        guard.validate(github, self.environment())
        for key, value in (("RECOVERY_SHA", "main"), ("GITHUB_SHA", "c" * 40),
                           ("GITHUB_REF", "refs/tags/main"), ("GITHUB_EVENT_NAME", "pull_request"),
                           ("GITHUB_REPOSITORY", "fork/repo")):
            with self.subTest(key=key), self.assertRaises(ValueError):
                guard.validate(github, {**self.environment(), key: value})

    def test_agent_guard_requires_app_success_for_same_sha(self):
        github = RecoveryGitHub()
        env = self.environment("customer-agent-worker-ci.yml")
        for overrides in ({"conclusion": "failure"}, {"head_sha": "c" * 40}, {"status": "queued"}):
            github.completed_runs.clear()
            github.add_run(APP, **overrides)
            with self.assertRaises(ValueError):
                guard.validate(github, env)
        github.completed_runs.clear()
        github.add_run(APP)
        guard.validate(github, env)

    def test_all_publication_checkouts_guard_recovery_before_mutations(self):
        for name in POLICY:
            source = (ROOT / ".github/workflows" / name).read_text()
            self.assertIn("      recovery_sha:", source, name)
            checkouts = re.findall(r"uses: actions/checkout@[^\n]+\n((?:        [^\n]*\n|\n)*)", source)
            self.assertTrue(checkouts, name)
            self.assertEqual(source.count("run: python3 scripts/validate-publisher-recovery.py"), len(checkouts), name)
            self.assertIn("working-directory: " + "$" + "{{ github.workspace }}", source)

    def test_reconciler_remains_active_during_all_protected_scopes(self):
        protected = {name for files in module.SCOPES.values() for name in files}
        self.assertEqual(set(POLICY), protected - {"recover-public-proxy.yml"})
        self.assertNotIn("reconcile-publishers.yml", protected)
        source = (ROOT / ".github/workflows/reconcile-publishers.yml").read_text()
        for contract in ("  push:", "  schedule:", "  workflow_run:", "  workflow_dispatch:",
                         "actions: write", "cancel-in-progress: false", "--actions-ssh",
                         "github.ref == 'refs/heads/main'", "deploy-control-known-hosts"):
            self.assertIn(contract, source)
        self.assertNotIn("pull_request:", source)
        self.assertNotIn("StrictHostKeyChecking=no", source)
        self.assertNotIn("docker ", source)

    def test_actions_transport_requires_official_context_and_pinned_identity(self):
        with patch.dict(os.environ, {}, clear=True), self.assertRaises(module.CoordinationError):
            module.RemoteStore(actions_ssh=True)
        with patch.dict(os.environ, {"GITHUB_ACTIONS": "true", "GITHUB_REPOSITORY": module.REPOSITORY}):
            command = module.RemoteStore(actions_ssh=True).command
        self.assertIn("StrictHostKeyChecking=yes", command)
        self.assertIn("root@191.252.181.168", command)
        self.assertNotIn("StrictHostKeyChecking=no", command)
        self.assertTrue((ROOT / "scripts/deploy-control-known-hosts").read_text().startswith("191.252.181.168 ssh-ed25519 "))

    def test_github_payload_uses_stdin_and_compatible_version(self):
        with patch.object(module.subprocess, "run", return_value=subprocess.CompletedProcess([], 0, "{}", "")) as run:
            module.GitHub().api("actions/workflows/1/dispatches", "POST", {"ref": "main"})
        self.assertEqual(json.loads(run.call_args.kwargs["input"]), {"ref": "main"})
        self.assertIn("X-GitHub-Api-Version: 2026-03-10", run.call_args.args[0])

    def test_pagination_and_malformed_queries_fail_closed(self):
        api = module.GitHub()
        for response in ({}, {"total_count": 1000, "workflow_runs": []}, {"total_count": 2, "workflow_runs": [{}]}):
            with patch.object(api, "api", return_value=response), self.assertRaises(module.CoordinationError):
                api.runs_for(APP, SHA, "2026-01-01T00:00:00Z")

    def test_cli_help_exposes_the_complete_operational_flow(self):
        result = subprocess.run([sys.executable, "scripts/coordinate-deploy-intervention.py", "--help"],
                                cwd=ROOT, capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("prepare-resume", result.stdout)
        self.assertIn("reconcile-publishers", result.stdout)

    def test_ci_runs_and_triggers_on_all_recovery_contracts(self):
        source = (ROOT / ".github/workflows/github-actions-contracts.yml").read_text()
        for file in ("deploy_publisher_recovery.py", "deploy-publisher-recovery.json", "validate-publisher-recovery.py",
                     "test-publisher-recovery.py", "deploy-control-known-hosts"):
            self.assertEqual(source.count(f'      - "scripts/{file}"'), 2)
        self.assertIn("python3 scripts/test-publisher-recovery.py", source)


class RecoveryCliTest(unittest.TestCase):
    """Executa os comandos reais com processos gh/SSH locais e estado persistente entre chamadas."""

    def test_cli_from_pause_to_complete_with_both_transports_and_one_dispatch(self):
        with tempfile.TemporaryDirectory(prefix="publisher-cli-") as directory:
            root = Path(directory)
            binary = root / "bin"
            binary.mkdir()
            environment = {
                "PATH": str(binary) + os.pathsep + os.defpath,
                "TEST_REPOSITORY_ROOT": str(ROOT),
                "TEST_GITHUB_STATE": str(root / "github.json"),
                "TEST_CONTROL_STATE": str(root / "control"),
                "GITHUB_ACTIONS": "true", "GITHUB_REPOSITORY": module.REPOSITORY,
                "GITHUB_REF": "refs/heads/main",
            }
            github = textwrap.dedent("""
                import importlib.util, json, os, re, sys
                from pathlib import Path
                from urllib.parse import urlsplit, parse_qs
                source = Path(os.environ["TEST_REPOSITORY_ROOT"]) / "scripts"
                sys.path.insert(0, str(source))
                spec = importlib.util.spec_from_file_location("tests", source / "test-publisher-recovery.py")
                tests = importlib.util.module_from_spec(spec)
                spec.loader.exec_module(tests)
                client = tests.RecoveryGitHub()
                state_file = Path(os.environ["TEST_GITHUB_STATE"])
                if state_file.exists():
                    for key, value in json.loads(state_file.read_text()).items():
                        setattr(client, key, value)
                args = sys.argv[1:]
                method = args[args.index("--method") + 1]
                path = next(a for a in args if a.startswith("repos/")).split("/", 3)[3]
                payload = json.load(sys.stdin) if "--input" in args else None
                parsed = urlsplit(path)
                match = re.fullmatch(r"actions/workflows/([^/]+)/runs", parsed.path)
                if match:
                    key = match[1]
                    name = key if key in client.workflows else next(k for k,v in client.workflows.items() if str(v["id"]) == key)
                    runs = client.completed_runs.get(name, [])
                    query = parse_qs(parsed.query)
                    if "status" in query:
                        runs = [r for r in runs if r["status"] == query["status"][0]]
                    response = {"total_count": len(runs), "workflow_runs": runs}
                else:
                    response = client.api(path, method, payload)
                fields = ("workflows", "sha", "completed_runs", "jobs", "dispatches")
                state_file.write_text(json.dumps({key: getattr(client, key) for key in fields}))
                if response is not None:
                    print(json.dumps(response))
            """)
            transport = textwrap.dedent("""
                import os, shlex, sys
                if os.path.basename(sys.argv[0]) == "ssh":
                    assert "StrictHostKeyChecking=yes" in sys.argv
                    index = sys.argv.index("root@191.252.181.168")
                    args = shlex.split(sys.argv[index + 1])
                else:
                    assert sys.argv[1] == "root@191.252.181.168"
                    args = sys.argv[2:]
                assert args[:2] == ["python3", "-c"]
                args[-1] = os.environ["TEST_CONTROL_STATE"]
                os.execv(sys.executable, [sys.executable, *args[1:]])
            """)
            for name, source in (("gh", github), ("ssh", transport), ("sandbox-ssh", transport)):
                file = binary / name
                file.write_text("#!" + sys.executable + "\n" + source)
                file.chmod(0o700)

            def call(*args):
                result = subprocess.run([sys.executable, "scripts/coordinate-deploy-intervention.py", *args],
                                        cwd=ROOT, env=environment, capture_output=True, text=True, timeout=30)
                self.assertEqual(result.returncode, 0, result.stderr)
                return json.loads(result.stdout)

            state = call("begin", "--scope", "temis", "--owner", "teste", "--reason", "teste local",
                         "--authorization", "sintética", "--protected-version", "v1")
            self.assertEqual(state["phase"], "ACTIVE")
            state = call("prepare-resume", "--id", state["id"], "--validated-commit", VALIDATED,
                         "--evidence", "matriz sintética aprovada")
            self.assertEqual(state["phase"], "AWAITING_MERGE")
            self.assertEqual(call("reconcile-publishers", "--actions-ssh")["status"], "WAITING")
            self.assertEqual(call("reconcile-publishers")["status"], "WAITING")
            state_file = root / "github.json"
            github_state = json.loads(state_file.read_text())
            self.assertEqual(len(github_state["dispatches"]), 1)
            github_state["completed_runs"]["communication-agent-worker-ci.yml"][0].update(status="completed", conclusion="success")
            state_file.write_text(json.dumps(github_state))
            self.assertEqual(call("reconcile-publishers", "--actions-ssh")["status"], "COMPLETE")
            self.assertEqual(call("reconcile-publishers")["status"], "COMPLETE")
            self.assertEqual(len(json.loads(state_file.read_text())["dispatches"]), 1)


if __name__ == "__main__":
    unittest.main(verbosity=2)
