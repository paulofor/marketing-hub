#!/usr/bin/env python3
"""Homologa coordenação com GitHub simulado, processos reais e estado inteiramente local."""

import copy
import importlib.util
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
from urllib.parse import parse_qs, urlsplit

ROOT = Path(__file__).resolve().parent.parent
spec = importlib.util.spec_from_file_location("coordination", ROOT / "scripts/coordinate-deploy-intervention.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
SHA = "a" * 40
INTEGRATED = "b" * 40


class FakeGitHub:
    """Simula os contratos da API, inclusive mudanças concorrentes e falhas transitórias."""

    def __init__(self):
        files = list(dict.fromkeys(file for files in module.SCOPES.values() for file in files))
        self.workflows = {file: {"id": index + 1, "path": f".github/workflows/{file}", "state": "active"}
                          for index, file in enumerate(files)}
        self.calls, self.runs, self.fail = [], {}, None
        self.integrated = True
        self.after_scan = None

    def api(self, path, method="GET"):
        self.calls.append((method, path))
        if self.fail and self.fail(path, method):
            raise module.CoordinationError("Falha GitHub simulada.")
        if path == "git/ref/heads/main":
            return {"object": {"sha": SHA}}
        if path.startswith("compare/"):
            return {"status": "ahead" if self.integrated else "diverged"}
        match = re.fullmatch(r"actions/workflows/([^/]+)(?:/(enable|disable))?", path)
        if match:
            key, action = match.groups()
            workflow = self.workflows.get(key) or next(v for v in self.workflows.values() if str(v["id"]) == key)
            if action:
                if method != "PUT":
                    raise AssertionError("Contrato de mutação incorreto.")
                workflow["state"] = "active" if action == "enable" else "disabled_manually"
            return copy.deepcopy(workflow)
        raise AssertionError(f"API não simulada: {method} {path}")

    def live_runs(self, workflow_id):
        if self.after_scan:
            self.after_scan(workflow_id)
        return copy.deepcopy(self.runs.get(workflow_id, []))


class LocalCoordinationTest(unittest.TestCase):
    """Exercita o controlador com o mesmo protocolo e lock usados no host real."""

    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="deploy-coordination-")
        self.directory = Path(self.temporary.name)
        self.command = [sys.executable, str(ROOT / "scripts/deploy_intervention_store.py"), str(self.directory)]
        self.github = FakeGitHub()
        self.store = module.RemoteStore(self.command).__enter__()
        self.coordinator = module.Coordinator(self.github, self.store, settle=lambda: None)

    def tearDown(self):
        self.store.__exit__()
        self.temporary.cleanup()

    def begin(self, scopes=None):
        return self.coordinator.begin(scopes or ["app"], "operador-teste", "homologação local",
                                      "autorização sintética", "Vega v11 / ciclo 2")

    def resume(self, state):
        return self.coordinator.resume(state["id"], INTEGRATED, "relatório-local-aprovado")

    def test_happy_path_pauses_and_restores_only_scope_without_deploy(self):
        state = self.begin()
        self.assertEqual(state["phase"], "ACTIVE")
        self.assertEqual(len(state["workflows"]), 4)
        self.assertEqual(self.github.workflows["pde-platform-metodo-musa-ci.yml"]["state"], "active")
        self.assertEqual(self.resume(state)["phase"], "RELEASED")
        self.assertTrue(all(w["state"] == "active" for w in self.github.workflows.values()))
        self.assertFalse(any("dispatch" in path or "cancel" in path or "rerun" in path for _, path in self.github.calls))

    def test_existing_disabled_workflows_stay_disabled(self):
        workflow = self.github.workflows["customer-agent-worker-ci.yml"]
        workflow["state"] = "disabled_inactivity"
        state = self.begin()
        self.resume(state)
        self.assertEqual(workflow["state"], "disabled_inactivity")
        self.assertNotIn(("PUT", f"actions/workflows/{workflow['id']}/enable"), self.github.calls)

    def test_combined_scopes_deduplicate_consumers(self):
        state = self.begin(["app", "psique", "pde", "dedalo"])
        self.assertEqual(len(state["workflows"]), 7)
        self.assertEqual(len({w["id"] for w in state["workflows"]}), 7)
        self.assertEqual(self.github.workflows["financial-agent-worker-ci.yml"]["state"], "active")

    def test_in_progress_and_queued_runs_block_until_complete_without_cancellation(self):
        identifier = self.github.workflows["deploy-containers.yml"]["id"]
        for status in module.LIVE_STATUSES:
            with self.subTest(status=status):
                self.github.runs[identifier] = [{"id": 101, "status": status, "head_sha": SHA}]
                previous = self.store.load()
                state = self.coordinator.protect(previous["id"]) if previous else self.begin()
                self.assertEqual(state["phase"], "DRAINING")
                with self.assertRaises(module.CoordinationError):
                    self.coordinator.check(state["id"])
        self.github.runs.clear()
        state = self.coordinator.protect(state["id"])
        self.assertEqual(state["phase"], "ACTIVE")
        self.assertFalse(any(method == "POST" for method, _ in self.github.calls))

    def test_second_observation_catches_delayed_run(self):
        identifier = self.github.workflows["deploy-containers.yml"]["id"]
        self.coordinator.settle = lambda: self.github.runs.update({identifier: [{"id": 102, "status": "pending"}]})
        self.assertEqual(self.begin()["phase"], "DRAINING")

    def test_partial_pause_can_be_retried_and_never_releases_implicitly(self):
        identifier = self.github.workflows["customer-agent-worker-ci.yml"]["id"]
        self.github.fail = lambda path, method: path == f"actions/workflows/{identifier}/disable"
        with self.assertRaises(module.CoordinationError):
            self.begin()
        state = self.store.load()
        self.assertEqual(state["phase"], "DRAINING")
        self.assertEqual(self.github.workflows["product-discovery-worker-ci.yml"]["state"], "disabled_manually")
        self.github.fail = None
        self.assertEqual(self.coordinator.protect(state["id"])["phase"], "ACTIVE")

    def test_preflight_error_does_not_mutate_workflows(self):
        self.github.workflows["customer-agent-worker-ci.yml"]["state"] = "deleted"
        with self.assertRaises(module.CoordinationError):
            self.begin()
        self.assertIsNone(self.store.load())
        self.assertFalse(any(method != "GET" for method, _ in self.github.calls))

    def test_another_intervention_is_rejected(self):
        self.begin()
        with self.assertRaisesRegex(module.CoordinationError, "já está aberta"):
            self.begin(["pde"])

    def test_real_lock_excludes_second_operator(self):
        state = self.begin()
        with module.RemoteStore(self.command) as other:
            with self.assertRaises(module.CoordinationError):
                other.load()
        self.assertEqual(self.store.load()["id"], state["id"])

    def test_connection_loss_preserves_hold_and_allows_next_operator_to_resume_it(self):
        state = self.begin()
        self.store.process.kill()
        self.store.process.wait()
        self.store.__exit__()
        self.store = module.RemoteStore(self.command).__enter__()
        self.assertEqual(self.store.load()["phase"], "ACTIVE")
        coordinator = module.Coordinator(self.github, self.store, settle=lambda: None)
        self.assertEqual(coordinator.check(state["id"])["id"], state["id"])

    def test_unknown_id_never_enables_anything(self):
        state = self.begin()
        with self.assertRaises(module.CoordinationError):
            self.coordinator.resume("0" * 32, INTEGRATED, "validação")
        self.assertEqual(self.store.load()["phase"], "ACTIVE")
        self.assertEqual(self.store.load()["id"], state["id"])

    def test_resume_requires_full_commit_and_evidence(self):
        state = self.begin()
        for commit, evidence in [("main", "validado"), ("abc", "validado"), (SHA, " ")]:
            with self.subTest(commit=commit, evidence=evidence), self.assertRaises(module.CoordinationError):
                self.coordinator.resume(state["id"], commit, evidence)
        self.assertEqual(self.store.load()["phase"], "ACTIVE")

    def test_unmerged_commit_keeps_hold(self):
        state = self.begin()
        self.github.integrated = False
        with self.assertRaisesRegex(module.CoordinationError, "ausente da main"):
            self.resume(state)
        self.assertEqual(self.store.load()["phase"], "ACTIVE")
        self.assertFalse(any(path.endswith("/enable") for _, path in self.github.calls))

    def test_api_error_on_check_is_never_safe(self):
        state = self.begin()
        self.github.fail = lambda *_: True
        with self.assertRaises(module.CoordinationError):
            self.coordinator.check(state["id"])

    def test_external_enable_invalidates_readiness(self):
        state = self.begin()
        self.github.workflows["deploy-containers.yml"]["state"] = "active"
        with self.assertRaises(module.CoordinationError):
            self.coordinator.check(state["id"])
        self.assertEqual(self.store.load()["phase"], "DRAINING")
        self.assertEqual(self.coordinator.protect(state["id"])["phase"], "ACTIVE")

    def test_partial_resume_is_audited_and_reconciled(self):
        state = self.begin()
        identifier = self.github.workflows["customer-agent-worker-ci.yml"]["id"]
        self.github.fail = lambda path, _: path == f"actions/workflows/{identifier}/enable"
        with self.assertRaises(module.CoordinationError):
            self.resume(state)
        self.assertEqual(self.store.load()["phase"], "RESUMING")
        self.github.fail = None
        self.assertEqual(self.resume(state)["phase"], "RELEASED")

    def test_resume_is_idempotent(self):
        state = self.begin()
        self.resume(state)
        self.github.calls.clear()
        self.assertEqual(self.resume(state)["phase"], "RELEASED")
        self.assertEqual(self.github.calls, [])

    def test_new_intervention_preserves_previous_audit(self):
        first = self.begin()
        self.resume(first)
        second = self.begin(["pde"])
        self.assertNotEqual(first["id"], second["id"])
        archived = json.loads((self.directory / (first["id"] + ".json")).read_text())
        self.assertEqual(archived["phase"], "RELEASED")
        self.assertEqual(archived["authorization"], "autorização sintética")
        self.assertEqual(archived["integrated_commit"], INTEGRATED)
        self.assertEqual((self.directory / "current.json").stat().st_mode & 0o777, 0o600)

    def test_corrupt_state_is_not_treated_as_unprotected(self):
        (self.directory / "current.json").write_text('{"schema": 1}')
        with self.assertRaises(module.CoordinationError):
            self.begin()
        self.assertEqual(self.github.calls, [])

    def test_protected_execute_holds_lock_and_records_only_exit_and_executable(self):
        state = self.begin()
        marker = self.directory / "command-ran"
        script = "from pathlib import Path; import sys; Path(sys.argv[1]).write_text('done')"
        status = self.coordinator.execute(state["id"], ["app"], [sys.executable, "-c", script, str(marker), "synthetic-secret"])
        self.assertEqual(status, 0)
        self.assertEqual(marker.read_text(), "done")
        saved = self.store.load()
        self.assertNotIn("synthetic-secret", json.dumps(saved))
        self.assertEqual(saved["history"][-1]["event"], "command_finished")
        self.assertEqual(saved["phase"], "ACTIVE")

    def test_failed_command_preserves_hold(self):
        state = self.begin()
        self.assertEqual(self.coordinator.execute(state["id"], ["app"], [sys.executable, "-c", "raise SystemExit(7)"]), 7)
        self.assertEqual(self.store.load()["phase"], "ACTIVE")
        self.assertEqual(self.store.load()["history"][-1]["exit_code"], 7)

    def test_interrupted_command_cannot_be_released_or_reexecuted(self):
        state = self.begin()
        with patch.object(module.subprocess, "run", side_effect=KeyboardInterrupt):
            with self.assertRaises(KeyboardInterrupt):
                self.coordinator.execute(state["id"], ["app"], ["synthetic-command"])
        with self.assertRaises(module.CoordinationError):
            self.resume(state)
        with self.assertRaises(module.CoordinationError):
            self.coordinator.protect(state["id"])
        self.assertEqual(self.store.load()["phase"], "OPERATING")
        with self.assertRaises(module.CoordinationError):
            self.coordinator.reconcile_command(state["id"], "", True)
        with self.assertRaises(module.CoordinationError):
            self.coordinator.reconcile_command(state["id"], "conferido", False)
        reconciled = self.coordinator.reconcile_command(state["id"], "processo encerrado no host local", True)
        self.assertEqual(reconciled["phase"], "ACTIVE")

    def test_unprotected_command_never_starts(self):
        state = self.begin(["psique"])
        with patch.object(module.subprocess, "run") as run:
            with self.assertRaises(module.CoordinationError):
                self.coordinator.execute(state["id"], ["app"], ["docker", "compose", "up"])
            run.assert_not_called()

    def test_renamed_workflow_blocks(self):
        state = self.begin()
        self.github.workflows["deploy-containers.yml"]["path"] = ".github/workflows/replacement.yml"
        with self.assertRaises(module.CoordinationError):
            self.coordinator.check(state["id"])

    def test_discard_removes_only_pending_app_run_without_any_job(self):
        app = self.github.workflows["deploy-containers.yml"]["id"]
        items = [{"id": i, "status": "pending", "head_sha": SHA} for i in (201, 202, 203, 204, 205)]
        self.github.runs[app] = items
        state = self.begin()
        original_api = self.github.api
        cancelled = []
        reads = {}

        def api(path, method="GET"):
            match = re.match(r"actions/runs/(\d+)(.*)", path)
            if not match:
                return original_api(path, method)
            run_id, suffix = int(match[1]), match[2]
            if suffix.startswith("/jobs"):
                return {"total_count": 1, "jobs": [{"id": 1}]} if run_id == 202 else {"total_count": 0, "jobs": []}
            if suffix == "/cancel":
                self.assertEqual(method, "POST")
                cancelled.append(run_id)
                self.github.runs[app] = [r for r in self.github.runs[app] if r["id"] != run_id]
                return None
            reads[run_id] = reads.get(run_id, 0) + 1
            return {"workflow_id": app if run_id != 203 else -1, "head_sha": SHA,
                    "status": "in_progress" if run_id == 204 and reads[run_id] > 1 else
                              "queued" if run_id == 205 else "pending"}

        with patch.object(self.github, "api", side_effect=api):
            result = self.coordinator.discard_unstarted(state["id"])
        self.assertEqual(cancelled, [201, 205])
        self.assertEqual(result["phase"], "DRAINING")
        self.assertEqual(len(result["pending_runs"]), 3)

    def test_discard_can_preserve_existing_current_main_run(self):
        app = self.github.workflows["deploy-containers.yml"]["id"]
        self.github.runs[app] = [{"id": 301, "status": "pending", "head_sha": SHA}]
        state = self.begin()
        original_api = self.github.api
        retained = {"id": 301, "workflow_id": app, "head_sha": SHA, "head_branch": "main", "event": "push"}

        def api(path, method="GET"):
            if path == "actions/runs/301":
                return retained
            return original_api(path, method)

        with patch.object(self.github, "api", side_effect=api):
            self.assertEqual(self.coordinator.discard_unstarted(state["id"], 301)["phase"], "DRAINING")
            retained["head_sha"] = "c" * 40
            with self.assertRaises(module.CoordinationError):
                self.coordinator.discard_unstarted(state["id"], 301)
        self.assertFalse(any(method == "POST" for method, _ in self.github.calls))


class ApiAndInventoryTest(unittest.TestCase):
    """Protege os limites da API, autenticação e inventário dos publicadores reais."""

    def test_pagination_includes_old_live_runs(self):
        api = module.GitHub()
        queried = []

        def page(path):
            query = parse_qs(urlsplit(path).query)
            queried.append(query)
            if query["status"] != ["queued"]:
                return {"total_count": 0, "workflow_runs": []}
            count = 100 if query["page"] == ["1"] else 1
            offset = 0 if count == 100 else 100
            return {"total_count": 101, "workflow_runs": [
                {"id": i + offset, "status": "queued", "head_sha": SHA, "html_url": "https://example.invalid/run"}
                for i in range(count)]}

        with patch.object(api, "api", side_effect=page):
            self.assertEqual(len(api.live_runs(1)), 101)
        self.assertEqual({q["status"][0] for q in queried}, set(module.LIVE_STATUSES))

    def test_truncated_and_malformed_responses_block(self):
        api = module.GitHub()
        for data in ({"total_count": 1000, "workflow_runs": []}, {}, None):
            with self.subTest(data=data), patch.object(api, "api", return_value=data):
                with self.assertRaises(module.CoordinationError):
                    api.live_runs(1)

    def test_api_uses_existing_gh_auth_without_exposing_stderr(self):
        result = subprocess.CompletedProcess([], 1, "", "synthetic-secret HTTP 403")
        with patch.object(module.subprocess, "run", return_value=result) as run:
            with self.assertRaises(module.CoordinationError) as error:
                module.GitHub().api("actions/workflows/1/disable", "PUT")
        self.assertNotIn("synthetic-secret", str(error.exception))
        self.assertIn("HTTP 403", str(error.exception))
        self.assertEqual(run.call_args.kwargs["timeout"], 45)
        self.assertNotIn("env", run.call_args.kwargs)

    def test_api_timeout_and_invalid_json_fail_closed(self):
        api = module.GitHub()
        with patch.object(module.subprocess, "run", side_effect=subprocess.TimeoutExpired("gh", 45)):
            with self.assertRaises(module.CoordinationError):
                api.api("actions/workflows/1")
        with patch.object(module.subprocess, "run", return_value=subprocess.CompletedProcess([], 0, "not-json", "")):
            with self.assertRaises(module.CoordinationError):
                api.api("actions/workflows/1")

    def test_transport_uses_authorized_helper_and_persistent_directory_outside_rsync(self):
        store = module.RemoteStore()
        self.assertEqual(store.command[:4], ["sandbox-ssh", "root@191.252.181.168", "python3", "-c"])
        self.assertEqual(store.command[-1], "/var/lib/marketinghub/deploy-coordination")
        self.assertNotIn("\n", store.command[4])
        self.assertNotIn("StrictHostKeyChecking=no", " ".join(store.command))

    def test_inventory_covers_central_continuations_and_only_existing_workflows(self):
        for files in module.SCOPES.values():
            for file in files:
                self.assertTrue((ROOT / ".github/workflows" / file).is_file(), file)
        consumers = set()
        for path in (ROOT / ".github/workflows").glob("*.yml"):
            source = path.read_text()
            if "workflow_run:" in source and 'workflows: ["Build & Deploy containers"]' in source:
                consumers.add(path.name)
        self.assertTrue(consumers)
        self.assertTrue(consumers <= set(module.SCOPES["app"]))
        self.assertNotIn("backend-ci.yml", module.SCOPES["app"])
        self.assertNotIn("frontend.yml", module.SCOPES["app"])

    def test_ci_protects_implementation_and_contract_changes(self):
        source = (ROOT / ".github/workflows/github-actions-contracts.yml").read_text()
        for file in ("coordinate-deploy-intervention.py", "deploy_intervention_store.py",
                     "deploy-intervention-scopes.json", "test-deploy-intervention.py"):
            self.assertEqual(source.count(f'      - "scripts/{file}"'), 2)
        self.assertIn("run: python3 scripts/test-deploy-intervention.py", source)


if __name__ == "__main__":
    unittest.main(verbosity=2)
