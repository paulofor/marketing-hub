#!/usr/bin/env python3
"""Executa os smokes reais contra PDE local e snapshot exportado pelo teste MySQL."""

import argparse
import contextlib
import http.client
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import mimetypes
import os
import signal
from pathlib import Path
import subprocess
import threading
import time
import urllib.error
import urllib.parse
import urllib.request


ROOT = Path(__file__).resolve().parents[3]
FRONTEND = ROOT / "pde-platform/frontend"
SLUG = "metodo-musa-7-dias"
VERSIONS = {
    "v5": "musa-pde-entry-v5-video-explicativo",
    "v6": "musa-pde-entry-v6-video-motivacional",
    "v7": "musa-pde-entry-v7-espelho-antes-de-sair",
    "v8": "musa-pde-entry-v12-primeiro-ajuste-aplicavel",
}
VALIDATION_TOKEN = "local-pde-candidate-validation-token"


def wait_ready(url, process):
    """Aguarda um serviço local com prazo e detecção de saída antecipada."""
    for _ in range(120):
        if process.poll() is not None:
            raise RuntimeError(f"Processo local terminou: {process.returncode}")
        try:
            with urllib.request.urlopen(url, timeout=1) as response:
                if response.status == 200:
                    return
        except (OSError, urllib.error.URLError):
            pass
        time.sleep(0.25)
    raise RuntimeError(f"Serviço local não respondeu: {url}")


@contextlib.contextmanager
def process(command, env, log, cwd=ROOT):
    """Mantém o processo e seu log até o encerramento da jornada."""
    with log.open("w") as output:
        child = subprocess.Popen(command, cwd=cwd, env=env, stdout=output,
                                 stderr=subprocess.STDOUT, start_new_session=True)
        try:
            yield child
        finally:
            try:
                os.killpg(child.pid, signal.SIGTERM)
            except ProcessLookupError:
                pass
            try:
                child.wait(timeout=10)
            except subprocess.TimeoutExpired:
                os.killpg(child.pid, signal.SIGKILL)
                child.wait()


def main():
    """Valida versões, recusas e segregação com API e navegadores reais."""
    parser = argparse.ArgumentParser()
    parser.add_argument("--snapshot", required=True, type=Path)
    parser.add_argument("--evidence", required=True, type=Path)
    args = parser.parse_args()
    evidence = args.evidence.resolve()
    evidence.mkdir(parents=True, exist_ok=True)
    snapshot = json.loads(args.snapshot.read_text())
    assert snapshot["experienceVersion"] == VERSIONS["v5"]
    results = []
    generated = {}
    env = {**os.environ, "CI": "true", "MARKETING_HUB_CONTRACT_SERVER_PORT": "58181"}

    def run(name, command, extra=None, cwd=ROOT):
        """Registra comando e código de saída de cada gate local."""
        with process(command, {**env, **(extra or {})}, evidence / f"{name}.log", cwd) as child:
            exit_code = child.wait()
        results.append({"gate": name, "exitCode": exit_code})
        (evidence / "results.json").write_text(json.dumps(results, indent=2))
        if exit_code:
            raise RuntimeError(f"Falhou {name}; veja {evidence / (name + '.log')}")
        print(f"OK {name}", flush=True)

    for index, (version, experience) in enumerate(VERSIONS.items()):
        directory = evidence / version
        directory.mkdir(exist_ok=True)
        generated[version] = directory
        run(f"runtime-{version}", ["sh", str(FRONTEND / "docker-entrypoint.d/10-runtime-config.sh")], {
            "MUSA_RUNTIME_CONFIG_FILE": str(directory / "runtime-config.js"),
            "MUSA_VERSION_DIAGNOSTICS_FILE": str(directory / "version-diagnostics.json"),
            "MUSA_SLOT_DIAGNOSTICS_FILE": str(directory / "slot-diagnostics.json"),
            "PDE_HEALTH_CONTRACT_FILE": str(directory / "pde-health-contract.json"),
            "PDE_FRONTEND_VERSION": version,
            "PDE_FRONTEND_PUBLIC_URL": f"http://127.0.0.1:{58205 + index}",
            "PDE_FRONTEND_IMAGE": f"local-fixture/{version}",
            "PDE_FRONTEND_IMAGE_VERSION_ID": "local-qa",
            "PDE_DEPLOY_IMAGE_TAG": "local-qa",
            "PDE_DEPLOY_COMMIT_SHA": "local-worktree",
            "VITE_PDE_PRODUCT_SLUG": SLUG,
            "VITE_MUSA_EXPERIENCE_VERSION_OVERRIDE": experience,
            "VITE_MUSA_CHECKOUT_URL": "http://127.0.0.1:58182/unused-qa-checkout",
        })

    class Handler(BaseHTTPRequestHandler):
        """Simula Hub/proxy local; as APIs PDE são executadas pelo backend Java real."""
        def log_message(self, *unused):
            pass

        def do_GET(self):
            self.respond()

        def do_POST(self):
            self.respond()

        def respond(self):
            parsed = urllib.parse.urlsplit(self.path)
            query = urllib.parse.parse_qs(parsed.query)
            local_version = getattr(self.server, "version", None)
            if local_version and parsed.path.startswith("/api/"):
                connection = http.client.HTTPConnection("127.0.0.1", 58096, timeout=30)
                body = self.rfile.read(int(self.headers.get("Content-Length", "0")))
                connection.request(self.command, self.path, body, {
                    "Host": local_version + ".clubemusa.com.br",
                    "Content-Type": self.headers.get("Content-Type", "application/json"),
                })
                reply = connection.getresponse()
                self.send(reply.status, reply.read(), reply.getheader("Content-Type", "application/json"))
                connection.close()
                return
            if not local_version:
                code = query.get("slotCode", [None])[0]
                experience = query.get("experienceVersion", [None])[0]
                internal_experience = parsed.path == (
                    f"/api/internal/pde-validation-contract/v1/products/{SLUG}/experience")
                internal_offer = parsed.path == (
                    f"/api/internal/pde-validation-contract/v1/products/{SLUG}/commercial-offer")
                if internal_experience or internal_offer:
                    if self.headers.get("X-PDE-Internal-Token") != VALIDATION_TOKEN:
                        self.send(403, b'{"error":"token invalido"}')
                        return
                    if code != "v8" or (experience and experience != VERSIONS["v8"]):
                        self.send(404, b'{"error":"candidata inexistente"}')
                        return
                    public_path = (f"/api/products/public/{SLUG}/commercial-offer"
                                   if internal_offer else f"/api/products/public/{SLUG}/pde-experience")
                    self.forward_fixture(f"{public_path}?slotCode=v8")
                    return
                public_experience = parsed.path == f"/api/products/public/{SLUG}/pde-experience"
                public_offer = parsed.path == f"/api/products/public/{SLUG}/commercial-offer"
                backend_alias = parsed.path == f"/api/pde/products/{SLUG}"
                if not public_experience and not public_offer and not backend_alias:
                    self.send(404, b'{"error":"rota inexistente"}')
                    return
                if code == "unpublished-qa":
                    self.send(409, b'{"error":"Contrato da versao PDE nao publicado"}')
                    return
                code = code or next((v for v, e in VERSIONS.items() if e == experience), "v7" if not experience else None)
                if code not in VERSIONS:
                    self.send(404, b'{"error":"Versao inexistente"}')
                    return
                if code == "v8":
                    self.send(409, b'{"error":"Candidata ainda nao promovida"}')
                    return
                if code == "v5" and (public_experience or backend_alias):
                    self.send(200, json.dumps(snapshot, ensure_ascii=False).encode())
                    return
                public_path = (f"/api/products/public/{SLUG}/commercial-offer"
                               if public_offer else f"/api/products/public/{SLUG}/pde-experience")
                self.forward_fixture(f"{public_path}?slotCode={code}")
                return
            if parsed.path == "/healthz":
                self.send(200, b'{"status":"UP"}')
                return
            relative = parsed.path.lstrip("/") or "index.html"
            base = generated[local_version]
            file = (base / relative).resolve()
            if not file.is_relative_to(base) or not file.is_file():
                base = FRONTEND / "dist"
                file = (base / relative).resolve()
            if not file.is_relative_to(base) or not file.is_file():
                self.send(404, b"not found", "text/plain")
                return
            self.send(200, file.read_bytes(), mimetypes.guess_type(file)[0] or "application/octet-stream")

        def forward_fixture(self, path):
            """Encaminha contratos determinísticos ao servidor-fixture compartilhado."""
            with urllib.request.urlopen(f"http://127.0.0.1:58181{path}", timeout=5) as reply:
                self.send(reply.status, reply.read(), reply.getheader("Content-Type", "application/json"))

        def send(self, status, data, content_type="application/json; charset=utf-8"):
            self.send_response(status)
            self.send_header("Content-Type", content_type)
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)

    servers = []
    with contextlib.ExitStack() as stack:
        fixture = stack.enter_context(process(["node", str(FRONTEND / "tests/marketing-hub-contract-server.mjs")], env, evidence / "hub-fixture.log"))
        wait_ready(f"http://127.0.0.1:58181/api/products/public/{SLUG}/pde-experience", fixture)
        for port, version in [(58182, None), (58205, "v5"), (58206, "v6"), (58207, "v7"), (58208, "v8")]:
            server = ThreadingHTTPServer(("127.0.0.1", port), Handler)
            server.version = version
            servers.append(server)
            threading.Thread(target=server.serve_forever, daemon=True).start()
            stack.callback(server.server_close)
            stack.callback(server.shutdown)
        backend_env = {**env, "PDE_MARKETING_HUB_BASE_URL": "http://127.0.0.1:58182",
            "PDE_ACCESS_JDBC_URL": "", "PDE_ACCESS_REQUIRE_JDBC": "false",
            "PDE_ACCESS_STORAGE_PATH": str(evidence / "access.json"),
            "PDE_AI_STORAGE_PATH": str(evidence / "ai.json"),
            "PDE_MIRA_PRIVATE_STORAGE_PATH": str(evidence / "mira.json"),
            "PDE_INTERNAL_API_TOKEN": VALIDATION_TOKEN,
            "LOGGING_FILE_NAME": str(evidence / "pde.log"),
            "PDE_PEPPER_API_TOKEN": "", "PDE_PEPPER_API_BASE_URL": "http://127.0.0.1:58182",
            "PDE_SMTP_HOST": "sandbox-mail", "PDE_SMTP_PORT": "1025",
            "PDE_SMTP_USERNAME": "", "PDE_SMTP_PASSWORD": "", "PDE_MAIL_TRANSPORT": "smtp"}
        jar = ROOT / "pde-platform/backend/target/pde-platform-backend-0.0.1-SNAPSHOT.jar"
        backend = stack.enter_context(process(["java", "-XX:ActiveProcessorCount=2", "-Xmx768m", "-jar", str(jar),
            "--server.port=58096", "--server.address=127.0.0.1", "--server.tomcat.threads.max=16",
            "--server.tomcat.threads.min-spare=2"], backend_env, evidence / "backend.log"))
        wait_ready("http://127.0.0.1:58096/actuator/health", backend)
        for index, (version, experience) in enumerate(VERSIONS.items()):
            base_url = f"http://127.0.0.1:{58205 + index}"
            options = {"PDE_PUBLIC_HEALTH_URL": base_url, "PDE_EXPECTED_EXPERIENCE_VERSION": experience}
            run(f"health-{version}", ["npm", "run", "test:public-health", "--", "--workers=1"], options, FRONTEND)
            run(f"diagnostic-{version}", ["npm", "run", "test:public-diagnostic-smoke", "--", "--workers=1"], options, FRONTEND)
            if version != "v8":
                run(f"consistency-{version}", ["bash", "scripts/check-musa-pde-public-consistency.sh"], {
                    "BACKEND_PUBLIC_BASE_URL": "http://127.0.0.1:58182", "PDE_PUBLIC_BASE_URL": base_url,
                    "EXPECTED_SLOT_CODE": version, "EXPECTED_EXPERIENCE_VERSION": experience})
        with urllib.request.urlopen(
                f"http://127.0.0.1:58096/api/pde/products/{SLUG}?slotCode=v8", timeout=5) as reply:
            candidate = json.loads(reply.read())
            assert candidate["experienceVersion"] == VERSIONS["v8"]
        with urllib.request.urlopen(
                f"http://127.0.0.1:58208/api/pde/products/{SLUG}/commercial-offer",
                timeout=5) as reply:
            offer = json.loads(reply.read())
            assert offer["experienceVersion"] == VERSIONS["v8"]
            assert offer["primaryCta"] and offer["priceBrl"] == 67
        print("OK candidata v8 usa preflight autenticado sem snapshot publico", flush=True)
        for code, status in [("unknown-qa", 404), ("unpublished-qa", 409)]:
            try:
                urllib.request.urlopen(f"http://127.0.0.1:58096/api/pde/products/{SLUG}?slotCode={code}", timeout=5)
            except urllib.error.HTTPError as error:
                assert error.code == status, (code, error.code)
            else:
                raise AssertionError(f"A recusa {code} virou sucesso")
        print("OK recusas HTTP 404/409 preservadas; nenhuma IA paga executada", flush=True)
        records = json.loads((evidence / "ai.json").read_text())
        assert len(records) == 4
        assert all(record["productSlug"] == SLUG for record in records.values())
        assert all(not record.get("costUsd") for record in records.values())
        assert not (evidence / "access.json").exists(), "O smoke criou acesso comercial"
        results.append({"gate": "http-rejections-and-local-persistence", "exitCode": 0})
        (evidence / "results.json").write_text(json.dumps(results, indent=2))


if __name__ == "__main__":
    main()
