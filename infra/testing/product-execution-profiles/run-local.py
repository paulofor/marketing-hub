#!/usr/bin/env python3
"""Executa a matriz local completa em séries sequenciais, sem publicar ou usar dados reais."""
import argparse, hashlib, json, os, pathlib, shutil, subprocess, time, urllib.request, xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[3]
PROJECT = "aihub-d33d79dd-a6c7-4e8d-ba01-75a4f1936965-82a7669330"
COMPOSE = ["docker", "compose", "-p", PROJECT, "-f", str(ROOT / "infra/testing/product-execution-profiles/compose.yml")]
parser = argparse.ArgumentParser()
parser.add_argument("--rounds", type=int, default=2)
parser.add_argument("--skip-unit", action="store_true", help="Somente diagnóstico; não conta como homologação completa")
args = parser.parse_args()
ART = ROOT / "artifacts/product-execution-profiles"
ART.mkdir(parents=True, exist_ok=True)


def command(values, log, cwd=ROOT, env=None):
    with log.open("w") as output:
        result = subprocess.run(values, cwd=cwd, env=env, stdout=output, stderr=subprocess.STDOUT)
        if result.returncode:
            raise RuntimeError(f"{values[0]} falhou ({result.returncode}); consulte {log.relative_to(ROOT)}")


def tests(module):
    total = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    files = list((ROOT / module / "target/surefire-reports").glob("TEST-*.xml"))
    if not files:
        raise RuntimeError("Nenhum relatório de testes em " + module)
    for path in files:
        root = ET.parse(path).getroot()
        for key in total:
            total[key] += int(root.attrib.get(key, 0))
    if total["failures"] or total["errors"]:
        raise RuntimeError("Falhas em " + module)
    return total


def readiness(process):
    deadline = time.monotonic() + 90
    while time.monotonic() < deadline:
        if process.poll() is not None:
            raise RuntimeError("Backend local encerrou; consulte backend.log")
        try:
            with urllib.request.urlopen("http://127.0.0.1:18094/fixture/health", timeout=2) as response:
                if response.status == 200:
                    return
        except OSError:
            time.sleep(0.5)
    raise RuntimeError("Backend local não ficou disponível")


def stop(process):
    if process and process.poll() is None:
        process.terminate()
        try:
            process.wait(timeout=20)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait()


def fingerprint():
    files = subprocess.check_output(["git", "diff", "--name-only"], cwd=ROOT, text=True).splitlines()
    files += subprocess.check_output(["git", "ls-files", "--others", "--exclude-standard"], cwd=ROOT, text=True).splitlines()
    digest = hashlib.sha256()
    for name in sorted(set(files)):
        if name.startswith("docs/"):
            continue
        path = ROOT / name
        digest.update(name.encode())
        if path.is_file():
            digest.update(path.read_bytes())
    return digest.hexdigest()


results = []
for number in range(1, args.rounds + 1):
    folder = ART / (f"diagnostic-{number}" if args.skip_unit else f"round-{number}")
    if folder.exists():
        archive = ART / "attempts" / (str(time.time_ns()) + "-" + folder.name)
        archive.parent.mkdir(parents=True, exist_ok=True)
        shutil.move(str(folder), str(archive))
    folder.mkdir(parents=True, exist_ok=True)
    runtime = proxy = None
    revision = fingerprint()
    started = time.monotonic()
    summary = {"round": number, "completeMatrix": not args.skip_unit, "sourceFingerprint": revision}
    print(json.dumps({"round": number, "phase": "unit-and-contracts"}), flush=True)
    try:
        if not args.skip_unit:
            for module in ["backend/ads-service", "financial-agent-worker", "landing-generator-agent-worker", "ai-worker"]:
                print(json.dumps({"round": number, "module": module}), flush=True)
                command(["mvn", "-B", "test"], folder / (module.replace("/", "-") + ".log"), ROOT / module)
                summary[module] = tests(module)
            command(["npm", "run", "typecheck"], folder / "typecheck.log", ROOT / "frontend")
            command(["npm", "test", "--", "--run"], folder / "frontend-tests.log", ROOT / "frontend")
        command(["npm", "run", "build"], folder / "frontend-build.log", ROOT / "frontend")
        command(["mvn", "-q", "test-compile", "dependency:build-classpath", "-Dmdep.outputFile=target/profile-test.classpath", "-DincludeScope=test"], folder / "fixture-compile.log", ROOT / "backend/ads-service")
        classpath = os.pathsep.join([str(ROOT / "backend/ads-service/target/test-classes"), str(ROOT / "backend/ads-service/target/classes"), (ROOT / "backend/ads-service/target/profile-test.classpath").read_text().strip()])
        command(COMPOSE + ["down", "--volumes", "--remove-orphans"], folder / "cleanup-before.log")
        command(COMPOSE + ["up", "-d", "--wait"], folder / "mysql-start.log")
        runtime_log = (folder / "backend.log").open("w")
        java = ["java", "-Xmx768m", "-cp", classpath, "com.marketinghub.product.executionprofile.v1.service.ExecutionProfileLocalApplication"]
        runtime = subprocess.Popen(java, cwd=ROOT, stdout=runtime_log, stderr=subprocess.STDOUT)
        readiness(runtime)
        proxy_log = (folder / "frontend-server.log").open("w")
        proxy = subprocess.Popen(["node", "infra/testing/product-execution-profiles/frontend-server.mjs"], cwd=ROOT, stdout=proxy_log, stderr=subprocess.STDOUT)
        env = dict(os.environ, PROFILE_TEST_ARTIFACTS=str(folder))
        print(json.dumps({"round": number, "phase": "api-mysql-and-concurrency"}), flush=True)
        command(["node", "infra/testing/product-execution-profiles/api-matrix.mjs"], folder / "api.log", env=env)
        print(json.dumps({"round": number, "phase": "desktop-iphone-pixel"}), flush=True)
        command(["node", "infra/testing/product-execution-profiles/browser-matrix.mjs"], folder / "browser.log", env=env)
        with urllib.request.urlopen("http://127.0.0.1:18094/api/products/94001/execution-profiles/v1") as response:
            before = json.load(response)
        stop(runtime)
        runtime = subprocess.Popen(java, cwd=ROOT, stdout=runtime_log, stderr=subprocess.STDOUT)
        readiness(runtime)
        with urllib.request.urlopen("http://127.0.0.1:18094/api/products/94001/execution-profiles/v1") as response:
            after = json.load(response)
        if before != after:
            raise RuntimeError("Reinício alterou o histórico persistido")
        stop(runtime)
        runtime = None
        command(["java", "-cp", classpath, "com.marketinghub.product.executionprofile.v1.service.ExecutionProfileMigrationCheck"], folder / "mysql-migrations.log")
        command(["java", "-cp", classpath, "com.marketinghub.product.executionprofile.v1.service.ExecutionProfileMigrationCheck", "reapply"], folder / "mysql-reapply.log")
        command(["git", "diff", "--check"], folder / "diff-check.log")
        if fingerprint() != revision:
            raise RuntimeError("Código mudou durante a rodada; recomece duas rodadas completas")
        summary.update(status="PASS", durationSeconds=round(time.monotonic() - started, 1), restart="PASS", mysql="PASS", paidCalls=0)
        (folder / "result.json").write_text(json.dumps(summary, indent=2))
        results.append(summary)
        print(json.dumps(summary), flush=True)
    except Exception as error:
        summary.update(status="FAIL", error=str(error))
        (folder / "result.json").write_text(json.dumps(summary, indent=2))
        raise
    finally:
        stop(runtime)
        stop(proxy)
        command(COMPOSE + ["down", "--volumes", "--remove-orphans"], folder / "cleanup-after.log")
(ART / ("diagnostic-results.json" if args.skip_unit else "results.json")).write_text(json.dumps(results, indent=2))
