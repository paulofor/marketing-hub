#!/usr/bin/env python3
"""Homologa o smoke real com HTTP local e o diagnóstico produzido pelo entrypoint versionado."""

import copy
import json
import os
from pathlib import Path
import subprocess
import tempfile
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


ROOT = Path(__file__).resolve().parent.parent
SMOKE = ROOT / "scripts/check-musa-pde-public-consistency.sh"
ENTRYPOINT = ROOT / "pde-platform/frontend/docker-entrypoint.d/10-runtime-config.sh"
SLUG = "metodo-musa-7-dias"
EXPERIENCE = "musa-pde-entry-v7-espelho-antes-de-sair"
CONTRACT = {
    "slug": SLUG,
    "experienceVersion": EXPERIENCE,
    "funnelVersion": "musa-funnel-test-v1",
    "layoutKey": "musa-test-v7",
    "publicFirstFold": {"headline": "Teste local da experiência"},
    "heroVideos": [],
}


class PublicConsistencyTest(unittest.TestCase):
    """Exige consistência funcional sem contato com produção ou eventos comerciais."""

    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        directory = Path(self.directory.name)
        generated = {
            "MUSA_RUNTIME_CONFIG_FILE": directory / "runtime-config.js",
            "MUSA_VERSION_DIAGNOSTICS_FILE": directory / "version-diagnostics.json",
            "MUSA_SLOT_DIAGNOSTICS_FILE": directory / "slot-diagnostics.json",
            "PDE_HEALTH_CONTRACT_FILE": directory / "pde-health-contract.json",
        }
        env = {
            **os.environ,
            **{key: str(value) for key, value in generated.items()},
            "VITE_PDE_PRODUCT_SLUG": SLUG,
            "VITE_MUSA_EXPERIENCE_VERSION_OVERRIDE": EXPERIENCE,
            "PDE_FRONTEND_VERSION": "v7",
            "PDE_FRONTEND_IMAGE": "aihub-homologation/local/pde:v7",
            "PDE_DEPLOY_IMAGE_TAG": "test-only",
            "PDE_DEPLOY_COMMIT_SHA": "a" * 40,
        }
        subprocess.run(["sh", str(ENTRYPOINT)], env=env, check=True, capture_output=True)
        self.diagnostic = json.loads(generated["MUSA_VERSION_DIAGNOSTICS_FILE"].read_text())
        self.canonical_path = f"/backend/api/products/public/{SLUG}/pde-experience?slotCode=v7"
        self.alias_path = f"/backend/api/pde/products/{SLUG}?slotCode=v7"
        self.public_path = f"/frontend/api/pde/products/{SLUG}?slotCode=v7"
        self.diagnostic_path = "/frontend/version-diagnostics.json"
        self.responses = {
            self.canonical_path: copy.deepcopy(CONTRACT),
            self.alias_path: copy.deepcopy(CONTRACT),
            self.public_path: copy.deepcopy(CONTRACT),
            self.diagnostic_path: self.diagnostic,
            "/frontend/healthz": b'{"status":"UP"}',
            "/frontend/": b'<html><div id="root"></div><script src="/assets/app.js"></script></html>',
            "/frontend/runtime-config.js": generated["MUSA_RUNTIME_CONFIG_FILE"].read_bytes(),
        }
        self.requests = []
        fixture = self

        class Handler(BaseHTTPRequestHandler):
            def do_GET(self):
                fixture.requests.append(self.path)
                value = fixture.responses.get(self.path)
                self.send_response(200 if value is not None else 404)
                self.end_headers()
                if value is not None:
                    self.wfile.write(json.dumps(value).encode() if isinstance(value, dict) else value)

            def log_message(self, *_args):
                pass

        self.server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        self.thread = threading.Thread(target=self.server.serve_forever, kwargs={"poll_interval": 0.01})
        self.thread.start()
        self.addCleanup(self.close_server)
        base = f"http://127.0.0.1:{self.server.server_port}"
        self.env = {
            **os.environ,
            "PRODUCT_SLUG": SLUG,
            "BACKEND_PUBLIC_BASE_URL": base + "/backend",
            "PDE_PUBLIC_BASE_URL": base + "/frontend",
            "EXPECTED_SLOT_CODE": "v7",
            "EXPECTED_EXPERIENCE_VERSION": EXPERIENCE,
            "EXPECTED_PUBLIC_FIRST_FOLD_HEADLINE": "",
            "EXPECTED_HERO_VIDEO_PATH": "",
            "TIMEOUT_SECONDS": "3",
            "NO_PROXY": "127.0.0.1,localhost",
        }

    def close_server(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(timeout=3)

    def run_smoke(self):
        return subprocess.run(
            ["bash", str(SMOKE)], env=self.env, capture_output=True, text=True, timeout=15
        )

    def assert_blocked(self, expected):
        result = self.run_smoke()
        self.assertNotEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn(expected, result.stdout + result.stderr)

    def test_accepts_real_entrypoint_and_uses_only_canonical_diagnostic(self):
        self.assertNotIn("slot", self.diagnostic)
        result = self.run_smoke()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("OK: contratos PDE consistentes", result.stdout)
        self.assertIn(self.diagnostic_path, self.requests)
        self.assertNotIn("/frontend/slot-diagnostics.json", self.requests)
        self.assertEqual(len(self.requests), 7)

    def test_rejects_legacy_slot_without_version(self):
        self.diagnostic["slot"] = self.diagnostic.pop("version")
        self.assert_blocked("version")

    def test_rejects_other_public_version(self):
        self.diagnostic["version"] = "v6"
        self.assert_blocked("esperado=v7 retornado=v6")

    def test_rejects_other_product(self):
        self.diagnostic["productSlug"] = "mira"
        self.assert_blocked("productSlug")

    def test_rejects_other_experience_without_optional_override(self):
        self.env["EXPECTED_EXPERIENCE_VERSION"] = ""
        self.diagnostic["experienceVersion"] = "mira-private-v2"
        self.assert_blocked("experienceVersion")

    def test_rejects_missing_image_identity(self):
        for field in ["image", "imageTag", "commitSha"]:
            with self.subTest(field=field):
                value = self.diagnostic.pop(field)
                self.assert_blocked(field)
                self.diagnostic[field] = value

    def test_rejects_down_diagnostic(self):
        self.diagnostic["status"] = "DOWN"
        self.assert_blocked("status UP")

    def test_rejects_http_failure_without_legacy_fallback(self):
        self.responses.pop(self.diagnostic_path)
        self.responses["/frontend/slot-diagnostics.json"] = self.diagnostic
        self.assert_blocked("404")
        self.assertNotIn("/frontend/slot-diagnostics.json", self.requests)

    def test_rejects_invalid_json(self):
        self.responses[self.diagnostic_path] = b"<html>fallback SPA</html>"
        self.assert_blocked("JSON válido")

    def test_rejects_wrong_canonical_product(self):
        self.responses[self.canonical_path]["slug"] = "mira"
        self.assert_blocked("Slug canônico divergente")

    def test_rejects_aliases_from_other_experience(self):
        for path in [self.alias_path, self.public_path]:
            with self.subTest(path=path):
                self.responses[path]["experienceVersion"] = "versao-incorreta"
                self.assert_blocked("divergente no campo experienceVersion")
                self.responses[path]["experienceVersion"] = EXPERIENCE

    def test_rejects_wrong_expected_experience(self):
        self.env["EXPECTED_EXPERIENCE_VERSION"] = "versao-esperada-incorreta"
        self.assert_blocked("Versão publica PDE divergente")

    def test_rejects_runtime_from_other_experience(self):
        self.responses["/frontend/runtime-config.js"] = b"window.__MUSA_RUNTIME_CONFIG__ = {};"
        self.assert_blocked("Runtime config publico PDE divergente")

    def test_rejects_divergent_layout_copy_or_video(self):
        for field, value, expected in [
            ("layoutKey", "outro-layout", "Layout público"),
            ("publicFirstFold", {"headline": "Outra oferta"}, "Copy pública"),
            ("heroVideos", [{"url": "/outro.mp4"}], "Vídeos hero"),
        ]:
            with self.subTest(field=field):
                self.responses[self.public_path][field] = value
                self.assert_blocked(expected)
                self.responses[self.public_path] = copy.deepcopy(CONTRACT)

    def test_rejects_down_health(self):
        self.responses["/frontend/healthz"] = b'{"status":"DOWN"}'
        self.assert_blocked("Health público")

    def test_rejects_incomplete_page(self):
        self.responses["/frontend/"] = b"<html>Sem aplicacao</html>"
        self.assert_blocked("marcador obrigatório")

    def test_ci_executes_contract_before_publication(self):
        workflow = (ROOT / ".github/workflows/pde-platform-metodo-musa-ci.yml").read_text()
        self.assertEqual(workflow.count('      - "scripts/check-musa-pde-public-consistency.sh"'), 2)
        self.assertEqual(workflow.count('      - "scripts/test-musa-pde-public-consistency.py"'), 2)
        self.assertIn("run: python3 ../../scripts/test-musa-pde-public-consistency.py", workflow)


if __name__ == "__main__":
    unittest.main(verbosity=2)
