#!/usr/bin/env python3
"""Testes unitários do contrato de promoção e rollback das superfícies PDE."""

from __future__ import annotations

import importlib.util
import copy
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import threading
import unittest
from unittest import mock
import urllib.error

ROOT = Path(__file__).resolve().parent.parent.parent
SPEC = importlib.util.spec_from_file_location(
    "pde_release_contract", ROOT / "pde-platform/scripts/pde_release_contract.py"
)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


class PdeReleaseContractTest(unittest.TestCase):
    def setUp(self):
        self.inventory = MODULE.load_object(
            ROOT / "pde-platform/contracts/product-runtime-isolation-v1.json"
        )

    def test_inventory_resolves_every_supported_surface_once(self):
        MODULE.validate_inventory(self.inventory)
        self.assertEqual(
            {surface["deployTarget"] for surface in MODULE.surfaces(self.inventory)},
            {
                "v5",
                "v6",
                "v7",
                "v8",
                "mira",
                "mira-commercial",
                "kit-whatsapp",
                "alcyone",
            },
        )

    def test_commercial_mira_has_its_own_runtime_identity(self):
        surface = MODULE.select_surface(self.inventory, "mira-commercial")
        self.assertEqual(
            MODULE.diagnostic_surface(surface),
            "pde-platform-frontend-mira-commercial",
        )

    def test_alcyone_has_its_own_runtime_identity(self):
        surface = MODULE.select_surface(self.inventory, "alcyone")
        self.assertEqual(
            MODULE.diagnostic_surface(surface),
            "pde-platform-frontend-alcyone",
        )

    def test_alcyone_v3_manifest_binds_corrected_runtime_source(self):
        surface = MODULE.select_surface(self.inventory, "alcyone")
        contract = MODULE.load_object(
            ROOT / "pde-platform/contracts/alcyone-private-homologation-v5.json"
        )
        source = contract["publicationContract"]["requiredFrontendSourceSha256"]

        MODULE.validate_release_contract(surface, contract, source)
        self.assertEqual(
            contract["product"]["experienceVersion"], "alcyone-private-v3"
        )
        self.assertFalse(contract["operationalState"]["commercialPublicationAuthorized"])
        self.assertFalse(contract["operationalState"]["mediaAuthorized"])

    def test_commercial_mira_release_manifest_binds_homologated_source(self):
        surface = MODULE.select_surface(self.inventory, "mira-commercial")
        contract = MODULE.load_object(
            ROOT / "pde-platform/contracts/mira-commercial-homologation-v10.json"
        )
        source = contract["publicationContract"]["requiredFrontendSourceSha256"]

        MODULE.validate_release_contract(surface, contract, source)
        self.assertEqual(
            contract["liveVisualContract"]["requiredFirstFoldCtas"],
            ["Organizar minha rotina por R$ 49"],
        )
        self.assertEqual(
            contract["mediaContract"]["hlsPlaybackPath"],
            "/media/mira-commercial-demo-v3-hls/index.m3u8",
        )
        self.assertEqual(len(contract["mediaContract"]["hlsSegments"]), 3)
        self.assertEqual(
            contract["mediaContract"]["staticControlPath"],
            "/media/mira-commercial-control-v4.png",
        )
        self.assertEqual(
            contract["mediaContract"]["productProofPath"],
            "/media/mira-commercial-product-proof-v1.png",
        )
        self.assertEqual(
            contract["liveVisualContract"]["requiredTheme"],
            {
                "primary": "#6b3e7d",
                "accent": "#7a4e8c",
                "background": "#f7f2fa",
            },
        )
        self.assertIn(
            "Reembolso integral: peça em até 7 dias corridos",
            contract["liveVisualContract"]["requiredVisibleTexts"],
        )
        self.assertIn(
            "Suporte: disponível por 30 dias",
            contract["liveVisualContract"]["requiredVisibleTexts"],
        )
        self.assertEqual(contract["operationalState"]["experimentStatus"], "PAUSED")
        self.assertIn(
            "respeita a superfície selecionada",
            " ".join(contract["changeScope"]["includedChanges"]),
        )

    def test_diagnostics_bind_target_image_commit_and_source(self):
        surface = MODULE.select_surface(self.inventory, "v8")
        diagnostics = {
            "status": "UP",
            "surface": "pde-platform-frontend",
            "version": "v8",
            "imageVersionId": "v8",
            "publicUrl": "https://v8.clubemusa.com.br/",
            "experienceVersion": "musa-pde-entry-v12-primeiro-ajuste-aplicavel",
            "productSlug": "metodo-musa-7-dias",
            "image": "registry/v8:" + "a" * 40,
            "commitSha": "a" * 40,
            "frontendSourceSha256": "b" * 64,
        }
        MODULE.validate_diagnostics(
            surface,
            diagnostics,
            "registry/v8:" + "a" * 40,
            "a" * 40,
            "b" * 64,
        )

    def test_diagnostics_reject_another_build(self):
        surface = MODULE.select_surface(self.inventory, "v8")
        with self.assertRaisesRegex(ValueError, "frontendSourceSha256"):
            MODULE.validate_diagnostics(
                surface,
                {
                    "status": "UP",
                    "surface": "pde-platform-frontend",
                    "version": "v8",
                    "imageVersionId": "v8",
                    "publicUrl": "https://v8.clubemusa.com.br",
                    "experienceVersion": "musa-pde-entry-v12-primeiro-ajuste-aplicavel",
                    "productSlug": "metodo-musa-7-dias",
                    "image": "registry/v8:" + "a" * 40,
                    "commitSha": "a" * 40,
                    "frontendSourceSha256": "c" * 64,
                },
                "registry/v8:" + "a" * 40,
                "a" * 40,
                "b" * 64,
            )

    def test_release_contract_binds_product_target_and_source(self):
        surface = MODULE.select_surface(self.inventory, "v8")
        source = "b" * 64
        contract = {
            "contractVersion": "musa-v12-commercial-homologation.v6",
            "status": "READY_FOR_INDEPENDENT_REVIEW",
            "product": {
                "id": 4,
                "slug": "metodo-musa-7-dias",
                "experienceVersion": "musa-pde-entry-v12-primeiro-ajuste-aplicavel",
                "publicUrl": "https://v8.clubemusa.com.br/",
            },
            "publicationContract": {
                "automaticDeployOnMerge": True,
                "frontendVersion": "v8",
                "publicUrl": "https://v8.clubemusa.com.br/",
                "requiredFrontendSourceSha256": source,
            },
            "liveVisualContract": {
                "runtimeIdentity": {
                    "version": "v8",
                    "experienceVersion": "musa-pde-entry-v12-primeiro-ajuste-aplicavel",
                    "frontendSourceSha256": source,
                }
            },
        }
        MODULE.validate_release_contract(surface, contract, source)
        contract["publicationContract"]["frontendVersion"] = "v7"
        with self.assertRaisesRegex(ValueError, "publication.frontendVersion"):
            MODULE.validate_release_contract(surface, contract, source)

    def test_vega_preparation_keeps_private_access_out_of_public_slot_identity(self):
        surface = MODULE.select_surface(self.inventory, "v8")
        contract = MODULE.load_object(
            ROOT / "pde-platform/contracts/vega-cycle7-preparation-v2.json"
        )
        source = contract["publicationContract"]["requiredFrontendSourceSha256"]
        MODULE.validate_release_contract(surface, contract, source)
        self.assertEqual(contract["privateAccessUrl"], surface["publicUrl"] + "/agent-validation")
        self.assertFalse(contract["externalAuthorization"]["additionalBudgetAuthorized"])
        contract["product"]["publicUrl"] = contract["privateAccessUrl"]
        with self.assertRaisesRegex(ValueError, "product.publicUrl"):
            MODULE.validate_release_contract(surface, contract, source)

    def test_resolved_precheck_is_required_before_image_builds(self):
        workflow = (ROOT / ".github/workflows/pde-platform-metodo-musa-ci.yml").read_text()
        precheck = workflow.index("Validate resolved release precheck before building images")
        self.assertLess(precheck, workflow.index("\n  backend:"))
        self.assertIn("pde_release_contract.py validate-release", workflow[:workflow.index("\n  backend:")])

    def test_private_smoke_requires_explicit_safe_contract_and_preserves_commercial_default(self):
        surface = MODULE.select_surface(self.inventory, "v8")
        contract = MODULE.load_object(ROOT / "pde-platform/contracts/vega-cycle7-preparation-v3.json")
        source = contract["publicationContract"]["requiredFrontendSourceSha256"]
        self.assertEqual(MODULE.smoke_profile(surface, contract, source), "PRIVATE_PREPARATION_READ_ONLY")
        for field in ("additionalBudgetAuthorized", "mediaAuthorized", "paymentAuthorized", "paidVideoAuthorized"):
            unsafe = copy.deepcopy(contract)
            unsafe["externalAuthorization"][field] = True
            with self.assertRaisesRegex(ValueError, field):
                MODULE.smoke_profile(surface, unsafe, source)
        for field, value in (("readOnly", False), ("pagePath", "/"), ("contractPath", "/api/other")):
            unsafe = copy.deepcopy(contract)
            unsafe["deploymentValidation"][field] = value
            with self.assertRaisesRegex(ValueError, "não corresponde"):
                MODULE.smoke_profile(surface, unsafe, source)
        unsafe = copy.deepcopy(contract)
        unsafe["deploymentValidation"]["mode"] = "IGNORE_ERRORS"
        with self.assertRaisesRegex(ValueError, "desconhecido"):
            MODULE.smoke_profile(surface, unsafe, source)
        legacy = MODULE.load_object(ROOT / "pde-platform/contracts/vega-cycle7-preparation-v2.json")
        self.assertEqual(MODULE.smoke_profile(surface, legacy, source), "COMMERCIAL")

    def test_musa_rejects_inventory_as_release_contract(self):
        with self.assertRaisesRegex(ValueError, "manifesto imutável"):
            MODULE.validate_release_contract(
                MODULE.select_surface(self.inventory, "v8"), self.inventory, "b" * 64
            )

    def test_private_successor_binds_its_version_without_relabelling_public_root(self):
        surface = MODULE.select_surface(self.inventory, "v8")
        contract = MODULE.load_object(ROOT / "pde-platform/contracts/vega-cycle7-preparation-v3.json")
        successor = "musa-pde-entry-v13-primeiro-ajuste-aplicavel"
        contract["product"]["experienceVersion"] = successor
        contract["publicationContract"]["privatePrototypeVersion"] = successor
        source = contract["publicationContract"]["requiredFrontendSourceSha256"]
        self.assertEqual(MODULE.smoke_profile(surface, contract, source), "PRIVATE_PREPARATION_READ_ONLY")
        for section, field, value in (
            ("publicationContract", "privatePrototypeVersion", "musa-pde-entry-v14-primeiro-ajuste-aplicavel"),
            ("liveVisualContract", "runtimeIdentity", {"version": "v8", "experienceVersion": successor, "frontendSourceSha256": source}),
            ("deploymentValidation", "mode", "COMMERCIAL"),
            ("externalAuthorization", "paymentAuthorized", True),
        ):
            unsafe = copy.deepcopy(contract)
            unsafe[section][field] = value
            with self.assertRaises(ValueError):
                MODULE.smoke_profile(surface, unsafe, source)
        del contract["publicationContract"]["privatePrototypeVersion"]
        with self.assertRaisesRegex(ValueError, "product.experienceVersion"):
            MODULE.smoke_profile(surface, contract, source)

    def test_non_musa_temporarily_accepts_supported_inventory_contract(self):
        MODULE.validate_release_contract(
            MODULE.select_surface(self.inventory, "mira"), self.inventory, "b" * 64
        )

    def test_rollback_requires_the_exact_previous_identity(self):
        before = {
            "status": "UP",
            "surface": "pde-platform-frontend",
            "version": "v8",
            "imageVersionId": "v8",
            "publicUrl": "https://v8.clubemusa.com.br",
            "experienceVersion": "musa-pde-entry-v12-primeiro-ajuste-aplicavel",
            "productSlug": "metodo-musa-7-dias",
            "image": "registry/v8:old",
            "imageTag": "d" * 40,
            "commitSha": "d" * 40,
            "frontendSourceSha256": "e" * 64,
        }
        MODULE.validate_rollback(before, dict(before))
        changed = dict(before, imageTag="f" * 40)
        with self.assertRaisesRegex(ValueError, "imageTag"):
            MODULE.validate_rollback(before, changed)


class PrivateBackendDependencyTest(unittest.TestCase):
    """Exercita a dependência real de leitura e a ordem anterior à promoção privada."""

    def setUp(self):
        self.inventory = MODULE.load_object(ROOT / "pde-platform/contracts/product-runtime-isolation-v1.json")
        self.surface = MODULE.select_surface(self.inventory, "v8")
        self.contract = MODULE.load_object(ROOT / "pde-platform/contracts/vega-cycle10-preparation-v4.json")
        self.source = self.contract["publicationContract"]["requiredFrontendSourceSha256"]
        self.capability = {
            "productSlug": self.surface["productSlug"],
            "prototypeVersion": self.surface["experienceVersion"],
            "supportedPrototypeVersions": [self.surface["experienceVersion"], self.contract["product"]["experienceVersion"]],
            "agentValidationGenerationMode": "DETERMINISTIC_FIXTURE",
            "checkoutMode": "SIMULATED_NO_CHARGE", "paymentEnabled": False,
            "published": False, "mediaSpendBrl": 0,
            "syntheticLimits": self.contract["syntheticLimits"],
        }
        self.elapsed = 0

    def sleep(self, duration):
        self.elapsed += duration

    def wait(self, fetch, *, contract=None, source=None, timeout=30):
        return MODULE.wait_private_capability(
            self.surface, contract or self.contract, source or self.source, timeout,
            fetch=fetch, clock=lambda: self.elapsed, sleep=self.sleep,
        )

    def test_delayed_backend_is_ready_before_promotion_is_allowed(self):
        previous = copy.deepcopy(self.capability)
        previous.pop("supportedPrototypeVersions")
        fetch = mock.Mock(side_effect=[previous, previous, self.capability])
        self.assertTrue(self.wait(fetch))
        self.assertEqual(self.elapsed, 20)
        self.assertEqual(fetch.call_count, 3)
        fetch.assert_called_with(self.surface["publicUrl"] + self.contract["deploymentValidation"]["contractPath"])

    def test_another_successor_uses_declared_capability_without_cycle_id_exception(self):
        contract = copy.deepcopy(self.contract)
        contract.update(learningCycleId=91, experimentId=904, sourceReference="experiment:904")
        version = "musa-pde-entry-v14-primeiro-ajuste-aplicavel"
        contract["product"]["experienceVersion"] = version
        contract["publicationContract"]["privatePrototypeVersion"] = version
        capability = copy.deepcopy(self.capability)
        capability["supportedPrototypeVersions"].append(version)
        self.assertTrue(self.wait(mock.Mock(return_value=capability), contract=contract))

    def test_valid_predecessor_requires_no_capabilities_extension(self):
        contract = MODULE.load_object(ROOT / "pde-platform/contracts/vega-cycle7-preparation-v3.json")
        capability = copy.deepcopy(self.capability)
        capability.pop("supportedPrototypeVersions")
        self.assertTrue(self.wait(mock.Mock(return_value=capability), contract=contract, source=contract["publicationContract"]["requiredFrontendSourceSha256"]))
        self.assertEqual(self.elapsed, 0)

    def test_commercial_mira_preserves_default_without_private_network_request(self):
        contract = MODULE.load_object(ROOT / "pde-platform/contracts/mira-commercial-homologation-v10.json")
        fetch = mock.Mock(side_effect=AssertionError("Consulta privada indevida"))
        result = MODULE.wait_private_capability(
            MODULE.select_surface(self.inventory, "mira-commercial"), contract,
            contract["publicationContract"]["requiredFrontendSourceSha256"], 0, fetch=fetch,
        )
        self.assertFalse(result)
        fetch.assert_not_called()

    def test_missing_capability_expires_without_allowing_promotion(self):
        previous = copy.deepcopy(self.capability)
        previous.pop("supportedPrototypeVersions")
        with self.assertRaisesRegex(ValueError, "não pode ser promovida"):
            self.wait(mock.Mock(return_value=previous), timeout=13)
        self.assertEqual(self.elapsed, 13)

    def test_transport_or_temporary_http_failure_recovers_but_forbidden_read_does_not_retry(self):
        for error in (urllib.error.URLError("rede"), TimeoutError(), urllib.error.HTTPError("url", 502, "gateway", None, None), urllib.error.HTTPError("url", 429, "limite", None, None)):
            with self.subTest(error=type(error).__name__, code=getattr(error, "code", None)):
                self.elapsed = 0
                self.assertTrue(self.wait(mock.Mock(side_effect=[error, self.capability])))
                self.assertEqual(self.elapsed, 10)
        fetch = mock.Mock(side_effect=urllib.error.HTTPError("url", 403, "protegido", None, None))
        with self.assertRaisesRegex(ValueError, "HTTP 403"):
            self.wait(fetch)
        self.assertEqual(fetch.call_count, 1)

    def test_unsafe_capability_and_stale_source_are_never_treated_as_readiness(self):
        for field, value in (("productSlug", "outro-produto"), ("prototypeVersion", "versão-incorreta"), ("paymentEnabled", True), ("published", True), ("mediaSpendBrl", 1), ("checkoutMode", "REAL"), ("agentValidationGenerationMode", "PROVIDER"), ("syntheticLimits", {"sessionsPerCycleVersion": 19})):
            with self.subTest(field=field), self.assertRaises(ValueError):
                self.wait(mock.Mock(return_value=dict(self.capability, **{field: value})))
        fetch = mock.Mock(side_effect=AssertionError("Não consultar antes de validar o contrato"))
        with self.assertRaises(ValueError):
            self.wait(fetch, source="a" * 64)
        fetch.assert_not_called()

    def test_cli_reads_only_the_real_local_http_contract(self):
        requests = []
        payload = json.dumps(self.capability).encode()

        class Handler(BaseHTTPRequestHandler):
            def do_GET(self):
                requests.append((self.command, self.path, self.headers.get("Authorization")))
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.end_headers()
                self.wfile.write(payload)

            def log_message(self, *_args):
                pass

        server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            with tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                url = f"http://127.0.0.1:{server.server_port}"
                for product in self.inventory["products"]:
                    for surface in product["surfaces"]:
                        if surface["deployTarget"] == "v8":
                            surface["publicUrl"] = url
                contract = copy.deepcopy(self.contract)
                contract["product"]["publicUrl"] = url
                contract["publicationContract"]["publicUrl"] = url
                contract["privateAccessUrl"] = url + "/agent-validation"
                (root / "inventory.json").write_text(json.dumps(self.inventory))
                (root / "contract.json").write_text(json.dumps(contract))
                result = subprocess.run([
                    sys.executable, str(ROOT / "pde-platform/scripts/pde_release_contract.py"),
                    "--inventory", str(root / "inventory.json"), "wait-private-capability",
                    "--target", "v8", "--contract", str(root / "contract.json"),
                    "--expected-source", self.source, "--timeout", "0",
                ], capture_output=True, text=True, timeout=10)
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertIn("Dependência privada disponível", result.stdout)
                self.assertEqual(requests, [("GET", "/api/pde/vega/private/v1/contract", None)])
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=2)

    def test_workflow_waits_before_remote_changes_and_runs_prevention_in_ci(self):
        workflow = (ROOT / ".github/workflows/pde-platform-metodo-musa-ci.yml").read_text()
        deploy = workflow[workflow.index("\n  deploy_production:"):]
        gate = deploy.index("Wait for private backend capability before promotion")
        self.assertLess(gate, deploy.index("Configure SSH"))
        self.assertLess(gate, deploy.index("Publish PDE Platform production"))
        self.assertIn("pde_release_contract.py wait-private-capability", deploy[:deploy.index("Configure SSH")])
        self.assertIn("run: python3 pde-platform/scripts/test_pde_release_contract.py", workflow)


if __name__ == "__main__":
    unittest.main(verbosity=2)
