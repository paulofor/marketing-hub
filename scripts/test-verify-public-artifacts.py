#!/usr/bin/env python3
"""Homologa a prova pública com servidor HTTP local e fontes independentes."""

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import importlib.util
from pathlib import Path
import subprocess
import sys
import tempfile
from threading import Thread
import unittest

SCRIPT = Path(__file__).with_name("verify-public-artifacts.py")
SPEC = importlib.util.spec_from_file_location("public_artifacts", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class Handler(BaseHTTPRequestHandler):
    """Simula publicação, cache antigo, redirecionamento e indisponibilidade."""

    def do_GET(self):
        if self.path == "/" or self.path.startswith("/missing"):
            self.send_error(404)
            return
        if self.path.startswith("/redirect"):
            self.send_response(302)
            self.send_header("Location", f"http://localhost:{self.server.server_port}/page.html")
            self.end_headers()
            return
        self.send_response(200)
        self.end_headers()
        self.wfile.write(b"new page" if self.path.startswith("/page.html") else b"old page")

    def log_message(self, *_args):
        pass


class PublicArtifactsTest(unittest.TestCase):
    """Confere entrega real, destinos distintos e falhas sem publicação remota."""

    @classmethod
    def setUpClass(cls):
        cls.server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        cls.thread = Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()
        cls.base = f"http://127.0.0.1:{cls.server.server_port}/"

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()
        cls.thread.join()

    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        for name in ["page.html", "old.html", "missing.html", "redirect.html"]:
            Path(self.directory.name, name).write_bytes(b"new page")

    def test_matching_publication(self):
        evidence = MODULE.verify(self.base, "127.0.0.1", self.directory.name, ["page.html"])
        self.assertEqual(len(evidence), 1)
        self.assertEqual(len(evidence[0]["sha256"]), 64)

    def test_service_without_homepage_still_verifies_its_publication(self):
        with self.assertRaises(MODULE.urllib.error.HTTPError) as response:
            MODULE.urllib.request.urlopen(self.base)
        self.assertEqual(response.exception.code, 404)
        self.assertEqual(len(MODULE.verify(self.base, "127.0.0.1", self.directory.name, ["page.html"])), 1)

    def test_old_page_fails_even_with_http_200(self):
        with self.assertRaisesRegex(ValueError, "Artefato público divergente"):
            MODULE.verify(self.base, "127.0.0.1", self.directory.name, ["old.html"])

    def test_dns_mismatch(self):
        with self.assertRaisesRegex(ValueError, "Destino DNS divergente"):
            MODULE.verify(self.base, "127.0.0.2", self.directory.name, ["page.html"])

    def test_missing_publication(self):
        with self.assertRaises(MODULE.urllib.error.HTTPError):
            MODULE.verify(self.base, "127.0.0.1", self.directory.name, ["missing.html"])

    def test_redirect_to_another_origin(self):
        with self.assertRaisesRegex(ValueError, "outra origem"):
            MODULE.verify(self.base, "127.0.0.1", self.directory.name, ["redirect.html"])

    def test_invalid_local_file(self):
        with self.assertRaisesRegex(ValueError, "Artefato local inválido"):
            MODULE.verify(self.base, "127.0.0.1", self.directory.name, ["../page.html"])

    def test_cli_reports_evidence_and_failure(self):
        command = [sys.executable, str(SCRIPT), "--base-url", self.base, "--expected-address", "127.0.0.1",
                   "--source-directory", self.directory.name, "--attempts", "1"]
        success = subprocess.run(command + ["page.html"], capture_output=True, text=True)
        self.assertEqual(success.returncode, 0, success.stderr)
        self.assertIn('"verified": true', success.stdout)
        failure = subprocess.run(command + ["old.html"], capture_output=True, text=True)
        self.assertNotEqual(failure.returncode, 0)
        self.assertIn("Artefato público divergente", failure.stderr)
        self.assertNotIn("old page", failure.stderr)


if __name__ == "__main__":
    unittest.main()
