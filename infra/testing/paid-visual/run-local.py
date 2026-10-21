#!/usr/bin/env python3
"""Homologa a entrega paga com controllers reais e doubles locais, sem credenciais externas."""
from pathlib import Path
import os
import signal
import subprocess
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "artifacts" / "paid-visual-local"
OUT.mkdir(parents=True, exist_ok=True)
processes = []
handles = []


def run(args, cwd=ROOT):
    """Executa validação e conserva o log, propagando falhas."""
    with (OUT / "build.log").open("a") as log:
        subprocess.run(args, cwd=cwd, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=900)


def start(args, cwd, name, env=None):
    """Inicia exclusivamente serviço local e guarda seu processo para limpeza."""
    handle = (OUT / name).open("w")
    handles.append(handle)
    process = subprocess.Popen(args, cwd=cwd, env=env, stdout=handle, stderr=subprocess.STDOUT, start_new_session=True)
    processes.append(process)
    return process


def ready(url, process):
    """Aguarda serviço local sem ocultar falha de inicialização."""
    for _ in range(40):
        if process.poll() is not None:
            raise RuntimeError("Serviço local encerrou antes da homologação")
        try:
            with urllib.request.urlopen(url, timeout=1) as response:
                if response.status == 200:
                    return
        except (OSError, TimeoutError):
            pass
        time.sleep(0.5)
    raise RuntimeError("Serviço local não respondeu: " + url)


try:
    backend = ROOT / "backend" / "ads-service"
    frontend = ROOT / "pde-platform" / "frontend"
    run(["mvn", "-q", "test-compile", "dependency:build-classpath", "-Dmdep.outputFile=target/paid-test-classpath.txt"], backend)
    classpath = "target/test-classes:target/classes:" + (backend / "target/paid-test-classpath.txt").read_text().strip()
    app = start(["java", "-cp", classpath, "com.marketinghub.pde.paidvisual.v1.service.PaidVisualLocalApplication"], backend, "backend.log")
    ready("http://127.0.0.1:8189/_fixture/image", app)
    ui = start(["npm", "exec", "--", "vite", "--config", "vite.paid-visual.local.config.ts"], frontend, "frontend.log", dict(os.environ, NODE_ENV="development"))
    ready("http://127.0.0.1:5189/paid-visual.html", ui)
    run(["node", "pde-platform/frontend/tests/paid-visual-local.cjs"])
    print("PASS: compra, acesso, entrega, recuperação e regeneração em três dispositivos; zero chamadas externas.")
finally:
    for process in reversed(processes):
        if process.poll() is not None:
            continue
        os.killpg(process.pid, signal.SIGTERM)
        try:
            process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            os.killpg(process.pid, signal.SIGKILL)
            process.wait(timeout=5)
    for handle in handles:
        handle.close()
