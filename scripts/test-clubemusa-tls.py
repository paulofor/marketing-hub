#!/usr/bin/env python3
"""Exercita a renovação real com CA local e fronteiras Docker/HTTP simuladas."""

import importlib.util
from datetime import datetime, timedelta, timezone
import json
import os
from pathlib import Path
import shutil
import socket
import ssl
import subprocess
import tempfile
import threading
import unittest

ROOT = Path(__file__).resolve().parent.parent
SCRIPT = ROOT / "lead-portal-payments-service/scripts/renew-clubemusa-tls.sh"
spec = importlib.util.spec_from_file_location("clubemusa_tls", ROOT / "scripts/check-clubemusa-tls.py")
tls = importlib.util.module_from_spec(spec)
spec.loader.exec_module(tls)

MOCK = r'''#!/usr/bin/env python3
import json, os, pathlib, shutil, sys
root=pathlib.Path(os.environ['TLS_TEST_ROOT']); args=sys.argv[1:]
name=pathlib.Path(sys.argv[0]).name
with (root/'calls.jsonl').open('a') as f: f.write(json.dumps([name,*args])+'\n')
if name == 'docker':
 if args[0] == 'inspect':
  if '.State.Running' in args[2]: print('false' if os.environ.get('PROXY_DOWN') else 'true')
  elif os.environ.get('BAD_MOUNT'): print('/another -> /etc/letsencrypt')
  else:
   print(str(root/'conf')+' -> /etc/letsencrypt')
   print(str(root/'deploy/docker/proxy/html')+' -> /usr/share/nginx/html')
 elif args[0] == 'run':
  if os.environ.get('CA_FAILURE'): sys.exit(4)
  if not os.environ.get('NO_RENEWAL'):
   domain=args[args.index('--cert-name')+1]
   shutil.copyfile(os.environ['NEW_CERT'],root/'conf/live'/domain/'fullchain.pem')
 elif args[-1] == '/docker-entrypoint.d/40-ensure-certs.sh':
  if os.environ.get('COPY_FAILURE'): sys.exit(5)
  domain=next(a.split('=',1)[1] for a in args if a.startswith('SSL_DOMAINS='))
  shutil.copyfile(root/'conf/live'/domain/'fullchain.pem',root/'active.pem')
 elif args[-1] == '-t' and os.environ.get('NGINX_INVALID'): sys.exit(6)
 elif args[-1] == 'reload' and os.environ.get('RELOAD_FAILURE'): sys.exit(7)
elif name == 'curl':
 print('wrong' if os.environ.get('BAD_CHALLENGE') else args[-1].rsplit('/',1)[-1],end='')
else: sys.exit(8)
'''


def run(*args):
    """Executa OpenSSL sem expor as chaves descartáveis."""
    return subprocess.run(args, check=True, capture_output=True, text=True)


class Certificates(unittest.TestCase):
    """Compartilha uma CA de teste com certificados válidos, vencidos e de outro domínio."""

    @classmethod
    def setUpClass(cls):
        cls.temp_ca = tempfile.TemporaryDirectory(prefix="clubemusa-ca-")
        cls.ca_root = Path(cls.temp_ca.name)
        cls.key = cls.ca_root / "key.pem"
        cls.ca = cls.ca_root / "ca.pem"
        ca_config = cls.ca_root / "ca.cnf"
        ca_config.write_text("[req]\ndistinguished_name=dn\nx509_extensions=ca_extensions\n"
                             "[dn]\n[ca_extensions]\nbasicConstraints=critical,CA:TRUE\n"
                             "keyUsage=critical,keyCertSign,cRLSign\nsubjectKeyIdentifier=hash\n")
        run("openssl", "req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "30",
            "-config", str(ca_config), "-subj", "/CN=Local TLS fixture",
            "-keyout", str(cls.ca_root / "ca.key"), "-out", str(cls.ca))
        run("openssl", "req", "-new", "-newkey", "rsa:2048", "-nodes", "-subj", "/CN=clubemusa.com.br",
            "-keyout", str(cls.key), "-out", str(cls.ca_root / "request.pem"))
        (cls.ca_root / "index.txt").touch()
        (cls.ca_root / "serial").write_text("1000\n")
        issuer = cls.ca_root / "issuer.cnf"
        issuer.write_text(f"[ca]\ndefault_ca=local\n[local]\ndatabase={cls.ca_root}/index.txt\n"
                          f"serial={cls.ca_root}/serial\nnew_certs_dir={cls.ca_root}\n"
                          f"certificate={cls.ca}\nprivate_key={cls.ca_root}/ca.key\n"
                          "default_md=sha256\npolicy=subject\nunique_subject=no\n"
                          "[subject]\ncommonName=supplied\n")
        names = "clubemusa.com.br,DNS:www.clubemusa.com.br,DNS:v5.clubemusa.com.br,DNS:v10.clubemusa.com.br"
        kit = "kit-whatsapp-pronto.digicomdigital.com.br"
        now = datetime.now(timezone.utc)
        for name, days, domains in (("valid", 28, names), ("expired", -1, names),
                                    ("short", 3, names), ("other", 28, "alcyone.example.test"),
                                    ("missing", 28, "clubemusa.com.br,DNS:www.clubemusa.com.br"),
                                    ("localhost", 28, "localhost"), ("localhost-expired", -1, "localhost"),
                                    ("localhost-short", 3, "localhost"),
                                    ("kit-valid", 28, kit), ("kit-expired", -1, kit),
                                    ("kit-short", 3, kit), ("kit-missing", 28, "www." + kit)):
            ext = cls.ca_root / f"{name}.ext"
            ext.write_text(f"[leaf]\nsubjectAltName=DNS:{domains}\nbasicConstraints=critical,CA:FALSE\n"
                           "keyUsage=critical,digitalSignature,keyEncipherment\nextendedKeyUsage=serverAuth\n"
                           "subjectKeyIdentifier=hash\nauthorityKeyIdentifier=keyid,issuer\n")
            # Segue a fixture MCP: datas explícitas, sem duração negativa recusada no runner.
            run("openssl", "ca", "-batch", "-notext", "-config", str(issuer),
                "-in", str(cls.ca_root / "request.pem"), "-extensions", "leaf",
                "-startdate", (now - timedelta(days=2)).strftime("%Y%m%d%H%M%SZ"),
                "-enddate", (now + timedelta(days=days)).strftime("%Y%m%d%H%M%SZ"),
                "-extfile", str(ext), "-out", str(cls.ca_root / f"{name}.pem"))

    @classmethod
    def tearDownClass(cls):
        cls.temp_ca.cleanup()


class Renewal(Certificates):
    """Executa o shell de produção sem rede externa e sem alterar containers."""

    PROFILE = "clubemusa"
    DOMAIN = "clubemusa.com.br"
    PREFIX = ""

    def certificate(self, name):
        """Seleciona o certificado da própria linhagem de teste."""
        return self.ca_root / f"{self.PREFIX}{name}.pem"

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="clubemusa-renew-")
        self.root = Path(self.temp.name)
        self.lineage = self.root / "conf/live" / self.DOMAIN
        self.lineage.mkdir(parents=True)
        (self.root / "conf/renewal").mkdir()
        (self.root / "conf/renewal" / f"{self.DOMAIN}.conf").touch()
        self.cert = self.lineage / "fullchain.pem"
        shutil.copyfile(self.certificate("expired"), self.cert)
        shutil.copyfile(self.key, self.lineage / "privkey.pem")
        (self.root / "deploy/docker/proxy/html").mkdir(parents=True)
        (self.root / "another-product.pem").write_text("preserve")
        (self.root / "bin").mkdir()
        for command in ("docker", "curl"):
            target = self.root / "bin" / command
            target.write_text(MOCK)
            target.chmod(0o755)
        self.env = {**os.environ, "PATH": str(self.root / "bin") + ":" + os.environ["PATH"],
                    "TLS_TEST_ROOT": str(self.root), "DEPLOY_DIR": str(self.root / "deploy"),
                    "CERT_ROOT": str(self.root / "conf"), "CERT_LOG_DIR": str(self.root / "logs"),
                    "TLS_LOCK": str(self.root / "lock"), "NEW_CERT": str(self.certificate("valid"))}

    def tearDown(self):
        self.temp.cleanup()

    def execute(self, **overrides):
        return subprocess.run(["bash", str(SCRIPT), *([] if self.PROFILE == "clubemusa" else [self.PROFILE])], env={**self.env, **overrides},
                              text=True, capture_output=True)

    def calls(self):
        file = self.root / "calls.jsonl"
        return [json.loads(line) for line in file.read_text().splitlines()] if file.exists() else []

    def test_expired_lineage_renews_preserves_all_names_and_other_products(self):
        result = self.execute()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(self.certificate("valid").read_bytes(), (self.root / "active.pem").read_bytes())
        self.assertEqual("preserve", (self.root / "another-product.pem").read_text())
        calls = self.calls()
        container = next(c for c in calls if c[:2] == ["docker", "run"])
        self.assertIn("certbot/certbot:v5.8.0", container)
        self.assertEqual(self.DOMAIN, container[container.index("--cert-name") + 1])
        self.assertNotIn("--force-renewal", container)
        self.assertNotIn("--privileged", container)
        self.assertNotIn("/var/run/docker.sock", " ".join(container))
        self.assertEqual(4 if self.PROFILE == "clubemusa" else 1, len([c for c in calls if c[0] == "curl"]))
        self.assertIn(["docker", "exec", "-e", f"SSL_DOMAINS={self.DOMAIN}",
                       "lead-portal-payments-service-proxy-1", "sh", "/docker-entrypoint.d/40-ensure-certs.sh"], calls)
        self.assertEqual([], list((self.root / "deploy/docker/proxy/html/.well-known/acme-challenge").iterdir()))

    def test_valid_certificate_is_idempotent_and_recovers_a_previous_missed_reload(self):
        shutil.copyfile(self.certificate("valid"), self.cert)
        result = self.execute(NO_RENEWAL="1")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertTrue((self.root / "active.pem").exists())
        self.assertEqual(1, len([c for c in self.calls() if c[-1] == "reload"]))

    def test_preflight_failures_do_not_call_authority(self):
        for flag in ("PROXY_DOWN", "BAD_MOUNT", "BAD_CHALLENGE", "NGINX_INVALID"):
            with self.subTest(flag=flag):
                (self.root / "calls.jsonl").unlink(missing_ok=True)
                self.assertNotEqual(0, self.execute(**{flag: "1"}).returncode)
                self.assertFalse(any(c[:2] == ["docker", "run"] for c in self.calls()))
                self.assertFalse((self.root / "active.pem").exists())

    def test_ca_failure_cannot_copy_or_reload(self):
        self.assertNotEqual(0, self.execute(CA_FAILURE="1").returncode)
        self.assertFalse((self.root / "active.pem").exists())
        self.assertFalse(any(c[-1] == "reload" for c in self.calls()))

    def test_expired_or_short_certificate_cannot_report_success(self):
        for cert in ("expired", "short"):
            with self.subTest(cert=cert):
                self.assertNotEqual(0, self.execute(NEW_CERT=str(self.certificate(cert))).returncode)
                self.assertFalse((self.root / "active.pem").exists())

    def test_renewal_cannot_drop_a_previous_hostname(self):
        self.assertNotEqual(0, self.execute(NEW_CERT=str(self.certificate("missing"))).returncode)
        self.assertFalse((self.root / "active.pem").exists())

    def test_other_product_lineage_is_not_renewed(self):
        shutil.copyfile(self.ca_root / "other.pem", self.cert)
        self.assertNotEqual(0, self.execute().returncode)
        self.assertFalse(any(c[:2] == ["docker", "run"] for c in self.calls()))

    def test_copy_failure_prevents_reload(self):
        self.assertNotEqual(0, self.execute(COPY_FAILURE="1").returncode)
        self.assertFalse(any(c[-1] == "reload" for c in self.calls()))

    def test_reload_failure_does_not_report_success(self):
        self.assertNotEqual(0, self.execute(RELOAD_FAILURE="1").returncode)


class KitRenewal(Renewal):
    """Repete o mesmo contrato de renovação e falhas para o Kit, sem ampliar seus nomes."""

    PROFILE = "kit_whatsapp"
    DOMAIN = "kit-whatsapp-pronto.digicomdigital.com.br"
    PREFIX = "kit-"


class ExternalTLS(Certificates):
    """Valida o cliente contra um servidor HTTPS local com confiança e nomes reais."""

    def probe(self, certificate, trusted=True):
        server = socket.socket()
        server.bind(("127.0.0.1", 0))
        server.listen(1)
        port = server.getsockname()[1]
        context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        context.load_cert_chain(self.ca_root / f"{certificate}.pem", self.key)

        def serve():
            try:
                connection, _ = server.accept()
                with connection:
                    with context.wrap_socket(connection, server_side=True):
                        pass
            except ssl.SSLError:
                pass
            finally:
                server.close()

        worker = threading.Thread(target=serve, daemon=True)
        worker.start()
        client = ssl.create_default_context(cafile=str(self.ca)) if trusted else ssl.create_default_context()
        try:
            return tls.check("localhost", port, client)
        finally:
            worker.join(timeout=5)

    def test_trusted_valid_tls(self):
        self.assertTrue(self.probe("localhost")["trusted"])

    def test_expired_rejected(self):
        with self.assertRaises(ssl.SSLCertVerificationError):
            self.probe("localhost-expired")

    def test_wrong_name_rejected(self):
        with self.assertRaises(ssl.SSLCertVerificationError):
            self.probe("other")

    def test_untrusted_rejected(self):
        with self.assertRaises(ssl.SSLCertVerificationError):
            self.probe("localhost", trusted=False)

    def test_expiring_soon_rejected(self):
        with self.assertRaises(ValueError):
            self.probe("localhost-short")


class Workflow(unittest.TestCase):
    """Confere acionamento e isolamento da manutenção sem desabilitar os gates existentes."""

    def test_profiles_keep_previous_hosts_and_add_only_the_existing_kit(self):
        musa = tls.profile_hosts("clubemusa")
        kit = tls.profile_hosts("kit_whatsapp")
        self.assertEqual(["kit-whatsapp-pronto.digicomdigital.com.br"], kit)
        self.assertEqual(10, len(musa))
        self.assertEqual(musa + kit, tls.profile_hosts("all"))
        with self.assertRaises(ValueError):
            tls.profile_hosts("another_product")
        rejected = subprocess.run(["bash", str(SCRIPT), "another_product"], capture_output=True)
        self.assertEqual(2, rejected.returncode)

    def test_schedule_and_dispatch_only_on_main_share_vps_lock(self):
        source = (ROOT / ".github/workflows/lead-portal-payments-ci.yml").read_text()
        self.assertIn("cron: '41 5,17 * * *'", source)
        job = source.split("  renew-tls:\n")[1].split("  deploy:\n")[0]
        for fragment in ("github.ref == 'refs/heads/main'", "inputs.operation == 'renew_tls'",
                         "group: deploy-vps-163-245-200-7", "cancel-in-progress: false",
                         "python3 scripts/check-clubemusa-tls.py --profile all",
                         "for profile in clubemusa kit_whatsapp", 'bash -s -- "$profile"'):
            self.assertIn(fragment, job)
        for forbidden in ("docker compose", "rsync", "Publish service", "StrictHostKeyChecking=no"):
            self.assertNotIn(forbidden, job)
        self.assertIn("python3 ../scripts/test-clubemusa-tls.py", source)
        self.assertNotIn("python3 scripts/test-clubemusa-tls.py", job)
        self.assertIn("github.event_name != 'schedule' && inputs.operation != 'renew_tls'", source)
        self.assertIn("github.event_name == 'workflow_dispatch' && inputs.operation != 'renew_tls'", source)


if __name__ == "__main__":
    unittest.main()
