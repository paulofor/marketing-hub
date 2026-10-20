#!/usr/bin/env python3
"""Homologa renovação, isolamento e consultas TLS usando certificados e servidores locais."""

import importlib.util
import io
import json
import os
from pathlib import Path
import socket
import ssl
import subprocess
import tempfile
import threading
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parent.parent
spec = importlib.util.spec_from_file_location("tls_check", ROOT / "scripts/check-mcp-tls.py")
check = importlib.util.module_from_spec(spec)
spec.loader.exec_module(check)

MOCK = r'''#!/usr/bin/env python3
import json, os, pathlib, shutil, sys
root=pathlib.Path(os.environ['TLS_TEST_ROOT'])
name=pathlib.Path(sys.argv[0]).name
args=sys.argv[1:]
with (root/'calls.jsonl').open('a') as f: f.write(json.dumps([name,*args])+'\n')
if name == 'docker':
 if args[0] == 'inspect':
  if '.State.Running' in args[2]: print('true')
  elif os.environ.get('BAD_MOUNT'): print('/unrelated -> /etc/letsencrypt')
  else:
   print(str(root/'volumes/mcp/certbot/conf')+' -> /etc/letsencrypt')
   print(str(root/'volumes/mcp/certbot/www')+' -> /var/www/certbot')
 elif args[0] == 'run':
  if os.environ.get('CA_FAILURE'): sys.exit(4)
  if '--dry-run' not in args and not os.environ.get('NO_RENEWAL'):
   shutil.copyfile(os.environ['NEW_CERT'],root/'volumes/mcp/certbot/conf/live/mcpserverdigi.shop/fullchain.pem')
 elif args[-1] == '-t' and os.environ.get('NGINX_INVALID'): sys.exit(5)
 elif args[-1] == 'reload' and os.environ.get('RELOAD_FAILURE'): sys.exit(6)
elif name == 'curl':
 print('wrong-webroot' if os.environ.get('BAD_CHALLENGE') else args[-1].rsplit('/',1)[-1],end='')
elif name == 'crontab':
 p=root/'crontab.txt'
 if args == ['-l']: print(p.read_text(),end='')
 elif args == ['-']: p.write_text(sys.stdin.read())
 else: sys.exit(7)
else: sys.exit(8)
'''


def run(*args):
    """Executa ferramenta local sem imprimir material privado dos certificados de teste."""
    return subprocess.run(args, check=True, capture_output=True, text=True)


class Certificates(unittest.TestCase):
    """Cria uma CA descartável e versões válidas, vencidas e de outro domínio."""

    @classmethod
    def setUpClass(cls):
        cls.certificates = tempfile.TemporaryDirectory(prefix="mcp-tls-ca-")
        cls.certs = Path(cls.certificates.name)
        cls.key = cls.certs / "key.pem"
        cls.ca = cls.certs / "ca.pem"
        run("openssl", "req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "30",
            "-subj", "/CN=MCP local test CA", "-keyout", str(cls.certs / "ca.key"), "-out", str(cls.ca))
        run("openssl", "req", "-new", "-newkey", "rsa:2048", "-nodes", "-subj", "/CN=mcpserverdigi.shop",
            "-keyout", str(cls.key), "-out", str(cls.certs / "request.pem"))
        for name, days, domains in (("valid", "30", "mcpserverdigi.shop,DNS:www.mcpserverdigi.shop"),
                                    ("expired", "-1", "mcpserverdigi.shop,DNS:www.mcpserverdigi.shop"),
                                    ("other", "30", "outro-produto.test")):
            ext = cls.certs / f"{name}.ext"
            ext.write_text(f"subjectAltName=DNS:{domains}\n")
            run("openssl", "x509", "-req", "-in", str(cls.certs / "request.pem"), "-CA", str(cls.ca),
                "-CAkey", str(cls.certs / "ca.key"), "-CAcreateserial", "-days", days,
                "-extfile", str(ext), "-out", str(cls.certs / f"{name}.pem"))

    @classmethod
    def tearDownClass(cls):
        cls.certificates.cleanup()


class RenewalTest(Certificates):
    """Executa o shell real com Docker, CA, DNS HTTP e crontab simulados."""

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="mcp-renew-test-")
        self.directory = Path(self.temp.name)
        self.cert_root = self.directory / "volumes/mcp/certbot"
        self.lineage = self.cert_root / "conf/live/mcpserverdigi.shop"
        self.lineage.mkdir(parents=True)
        (self.cert_root / "conf/renewal").mkdir()
        (self.cert_root / "conf/renewal/mcpserverdigi.shop.conf").touch()
        self.installed = self.lineage / "fullchain.pem"
        self.installed.write_bytes((self.certs / "expired.pem").read_bytes())
        (self.lineage / "privkey.pem").write_bytes(self.key.read_bytes())
        (self.cert_root / "www").mkdir()
        bin_dir = self.directory / "bin"
        bin_dir.mkdir()
        for command in ("docker", "curl", "crontab"):
            target = bin_dir / command
            target.write_text(MOCK)
            target.chmod(0o755)
        self.environment = {**os.environ, "PATH": str(bin_dir) + ":" + os.environ["PATH"],
                            "DEPLOY_DIR": str(self.directory), "TLS_TEST_ROOT": str(self.directory),
                            "MCP_TLS_LOCK": str(self.directory / "renew.lock"),
                            "NEW_CERT": str(self.certs / "valid.pem")}

    def tearDown(self):
        self.temp.cleanup()

    def execute(self, *args, script="renew-mcp-tls.sh", **environment):
        return subprocess.run(["bash", str(ROOT / "deploy/bin" / script), *args],
                              env={**self.environment, **environment}, capture_output=True, text=True)

    def calls(self):
        path = self.directory / "calls.jsonl"
        return [json.loads(line) for line in path.read_text().splitlines()] if path.exists() else []

    def reloaded(self):
        return any(call[-2:] == ["-s", "reload"] for call in self.calls())

    def test_expired_certificate_renews_and_reloads_only_mcp(self):
        result = self.execute()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(self.installed.read_bytes(), (self.certs / "valid.pem").read_bytes())
        self.assertTrue(self.reloaded())
        container = next(call for call in self.calls() if call[:2] == ["docker", "run"])
        self.assertNotIn("--deploy-hook", container)
        self.assertNotIn("docker.sock", " ".join(container))
        self.assertIn("certbot/certbot:v5.8.0", container)
        self.assertIn("--cert-name", container)
        self.assertFalse(list((self.cert_root / "www/.well-known/acme-challenge").iterdir()))
        self.assertTrue(all(call[2] == "marketinghub-mcp-nginx" for call in self.calls() if call[:2] == ["docker", "exec"]))

    def test_valid_disk_recovers_previous_reload_failure_without_new_certificate(self):
        self.installed.write_bytes((self.certs / "valid.pem").read_bytes())
        result = self.execute(NO_RENEWAL="1")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertTrue(self.reloaded())

    def test_ca_failure_preserves_certificate_and_proxy(self):
        before = self.installed.read_bytes()
        self.assertNotEqual(0, self.execute(CA_FAILURE="1").returncode)
        self.assertEqual(before, self.installed.read_bytes())
        self.assertFalse(self.reloaded())

    def test_failed_renewal_cannot_report_success_with_expired_disk_certificate(self):
        self.assertNotEqual(0, self.execute(NO_RENEWAL="1").returncode)
        self.assertFalse(self.reloaded())

    def test_wrong_mount_or_challenge_blocks_before_ca(self):
        for variable in ("BAD_MOUNT", "BAD_CHALLENGE", "NGINX_INVALID"):
            with self.subTest(variable=variable):
                self.assertNotEqual(0, self.execute(**{variable: "1"}).returncode)
                self.assertFalse(any(call[:2] == ["docker", "run"] for call in self.calls()))
                self.assertFalse(self.reloaded())

    def test_other_product_certificate_is_rejected(self):
        self.assertNotEqual(0, self.execute(NEW_CERT=str(self.certs / "other.pem")).returncode)
        self.assertFalse(self.reloaded())

    def test_reload_failure_is_reported_and_recoverable(self):
        self.assertNotEqual(0, self.execute(RELOAD_FAILURE="1").returncode)
        result = self.execute(NO_RENEWAL="1")
        self.assertEqual(0, result.returncode, result.stderr)

    def test_dry_run_does_not_replace_or_reload_production_certificate(self):
        before = self.installed.read_bytes()
        result = self.execute("--dry-run")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(before, self.installed.read_bytes())
        self.assertFalse(self.reloaded())

    def test_legacy_cron_migration_preserves_other_jobs_and_is_idempotent(self):
        other = "7 2 * * * backup-important-data\n"
        old = "17 3 * * * docker run certbot renew --deploy-hook 'docker exec marketinghub-mcp-nginx nginx -s reload' # mcpserverdigi.shop certbot renew\n"
        cron = self.directory / "crontab.txt"
        cron.write_text(other + old)
        result = self.execute(script="retire-legacy-mcp-renewal.sh")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(other, cron.read_text())
        self.assertEqual(other + old, (self.cert_root / "legacy-crontab-before-tls-workflow.txt").read_text())
        self.assertEqual(0, self.execute(script="retire-legacy-mcp-renewal.sh").returncode)

    def test_unrecognized_cron_is_preserved(self):
        cron = self.directory / "crontab.txt"
        original = "17 3 * * * custom-renew # mcpserverdigi.shop certbot renew\n"
        cron.write_text(original)
        self.assertNotEqual(0, self.execute(script="retire-legacy-mcp-renewal.sh").returncode)
        self.assertEqual(original, cron.read_text())


class RealTlsTest(Certificates):
    """Realiza handshakes TLS locais para detectar expiração, domínio e CA incorretos."""

    def probe(self, certificate, trusted=True):
        listener = socket.socket()
        listener.bind(("127.0.0.1", 0))
        listener.listen(1)
        address = listener.getsockname()
        server_context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        server_context.load_cert_chain(self.certs / f"{certificate}.pem", self.key)

        def serve():
            with listener:
                connection, _ = listener.accept()
                with connection:
                    try:
                        with server_context.wrap_socket(connection, server_side=True):
                            pass
                    except ssl.SSLError:
                        pass

        thread = threading.Thread(target=serve, daemon=True)
        thread.start()
        create = socket.create_connection
        context = ssl.create_default_context(cafile=str(self.ca)) if trusted else ssl.create_default_context()
        try:
            with patch.object(check.socket, "create_connection", side_effect=lambda *a, **kw: create(address, **kw)):
                return check.certificate_health("mcpserverdigi.shop", context)
        finally:
            thread.join(timeout=3)

    def test_trusted_certificate_passes_real_handshake(self):
        self.assertGreater(self.probe("valid")["remainingDays"], 14)

    def test_expired_certificate_is_rejected_by_tls(self):
        with self.assertRaises(ssl.SSLCertVerificationError):
            self.probe("expired")

    def test_other_product_identity_is_rejected_by_tls(self):
        with self.assertRaises(ssl.SSLCertVerificationError):
            self.probe("other")

    def test_untrusted_ca_is_rejected_by_tls(self):
        with self.assertRaises(ssl.SSLCertVerificationError):
            self.probe("valid", trusted=False)


class RpcTest(unittest.TestCase):
    """Impede que HTTP 200 mas erro funcional seja declarado acesso MCP saudável."""

    def test_errors_and_mismatched_ids_are_rejected(self):
        for payload in ({"id": 1, "error": {"code": -1}},
                        {"id": 1, "result": {"isError": True}}, {"id": 2, "result": {}}):
            with self.subTest(payload=payload):
                response = io.BytesIO(json.dumps(payload).encode())
                with patch.object(check.urllib.request, "urlopen", return_value=response):
                    with self.assertRaises(ValueError):
                        check.rpc("tools/list", {}, ssl.create_default_context())

    def test_valid_mcp_result_is_preserved(self):
        result = {"structuredContent": {"status": "ok"}}
        response = io.BytesIO(json.dumps({"id": 1, "result": result}).encode())
        context = ssl.create_default_context()
        with patch.object(check.urllib.request, "urlopen", return_value=response) as request:
            self.assertEqual(result, check.rpc("tools/call", {"name": "db_health"}, context))
        self.assertIs(request.call_args.kwargs["context"], context)
        self.assertEqual(ssl.CERT_REQUIRED, context.verify_mode)


if __name__ == "__main__":
    unittest.main()
