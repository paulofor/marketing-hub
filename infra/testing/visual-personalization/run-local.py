#!/usr/bin/env python3
"""Homologa entrada → API/JPA/MySQL → gateway → worker → entrega privada, sem IA externa."""
import json, os, pathlib, re, subprocess, time, urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[3]
ART = ROOT / 'artifacts/visual-personalization'
PROJECT = os.environ.get('VISUAL_COMPOSE_PROJECT')
if not PROJECT or not re.fullmatch(r'aihub-[a-z0-9]+(?:-[a-z0-9]+)*', PROJECT):
    raise SystemExit('Informe o projeto Compose exclusivo autorizado desta sandbox.')
COMPOSE = ['docker', 'compose', '-p', PROJECT, '-f', str(ROOT / 'infra/testing/product-financial-plan/compose.yml')]
ART.mkdir(parents=True, exist_ok=True)
processes = []
logs = []

def command(args, name, cwd=ROOT):
    with (ART / (name + '.log')).open('w') as log:
        result = subprocess.run(args, cwd=cwd, stdout=log, stderr=subprocess.STDOUT)
    if result.returncode:
        raise RuntimeError(name + ': ' + (ART / (name + '.log')).read_text()[-2500:])

def launch(args, name, env=None):
    log = (ART / (name + '.log')).open('w'); logs.append(log)
    process = subprocess.Popen(args, cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT)
    processes.append(process)
    return process

def ready(process, url):
    for _ in range(180):
        if process.poll() is not None:
            raise RuntimeError('Aplicação local encerrou; confira os logs da matriz.')
        try:
            with urllib.request.urlopen(url, timeout=2) as response:
                if response.status == 200: return
        except OSError: time.sleep(.5)
    raise RuntimeError('Aplicação local não ficou disponível: ' + url)

def java(module, main):
    folder = ROOT / module
    cp = os.pathsep.join([str(folder/'target/test-classes'), str(folder/'target/classes'), (folder/'target/visual.classpath').read_text().strip()])
    return ['java', '-XX:ActiveProcessorCount=2', '-Xmx512m', '-cp', cp, main]

try:
    for args, name in [(['docker','version'], 'docker'), (['docker','buildx','version'], 'buildx'), (['docker','compose','version'], 'compose')]: command(args, name)
    command(COMPOSE + ['up', '-d', '--wait'], 'mysql')
    main = launch(java('backend/ads-service', 'com.marketinghub.pde.visualpersonalization.v1.service.VisualPreparationLocalApplication'), 'main-api')
    ready(main, 'http://127.0.0.1:18095/api/pde/visual-personalization/v1/products/95501/context?commercialPlanId=95501&experimentId=95501')
    gateway = launch(java('pde-platform/backend', 'com.marketinghub.pde.visualpersonalization.v1.VisualGatewayLocalApplication'), 'pde-gateway')
    request = urllib.request.Request('http://127.0.0.1:18096/api/pde/visual-personalization/v1/internal/stage-executions/pending', headers={'X-PDE-Internal-Token':'visual-local-only'})
    ready(gateway, request)
    private = launch(['node','infra/testing/visual-personalization/local-server.mjs'], 'private-and-worker', dict(os.environ, VISUAL_FIXTURE_SPOOL=str(ART/'spool')))
    ready(private, 'http://127.0.0.1:15184/personalization.html')
    ui = launch(['node','infra/testing/product-financial-plan/frontend-server.mjs'], 'hub-frontend')
    ready(ui, 'http://127.0.0.1:15175/ai/image-generator')
    command(['node','infra/testing/visual-personalization/browser-matrix.mjs'], 'browser-matrix')
    saved = json.loads((ART/'browser/recovery-state.json').read_text())
    delivery_request = urllib.request.Request('http://127.0.0.1:18096/api/pde/visual-personalization/v1/preparations/' + saved['jobId'], headers={'X-PDE-Visual-Session':saved['token']})
    with urllib.request.urlopen(delivery_request, timeout=10) as response: before = json.load(response)
    main.terminate(); main.wait(timeout=15)
    main = launch(java('backend/ads-service', 'com.marketinghub.pde.visualpersonalization.v1.service.VisualPreparationLocalApplication'), 'main-api-restarted')
    ready(main, 'http://127.0.0.1:18095/api/pde/visual-personalization/v1/products/95501/context?commercialPlanId=95501&experimentId=95501')
    with urllib.request.urlopen(delivery_request, timeout=10) as response: after = json.load(response)
    assert before == after, 'Reinício alterou entrega, entrada, auditoria ou custo'
    (ART/'browser/recovery-state.json').unlink()
    sql = "SELECT COUNT(*),COUNT(DISTINCT job_id),SUM(status='PDE_COMPLETED') FROM image_generation_request; SELECT COUNT(*),COUNT(DISTINCT source_id),SUM(estimated_cost_usd) FROM studio_cost_ledger_entry; SELECT @@version;"
    command(COMPOSE + ['exec','-T','financial-plan-mysql','mysql','-uroot','-pfinancial-plans-local-only','-N','financial_plans_local','-e',sql], 'mysql-evidence')
    evidence = (ART/'mysql-evidence.log').read_text().splitlines()
    rows = [line.split('\t') for line in evidence if '\t' in line]
    assert rows[0] == ['7','7','7'], rows
    assert rows[1][:2] == ['7','7'] and float(rows[1][2]) > 0, rows
    (ART/'summary.json').write_text(json.dumps({'result':'PASS','realMysql57':True,'realJavaApis':2,'devices':3,'products':2,'externalProviderCalls':0,'persistedAttempts':7,'ledgerEntries':7,'callbackRecoveredWithoutInference':True}, indent=2)+'\n')
    print((ART/'summary.json').read_text(), flush=True)
finally:
    for process in reversed(processes):
        if process.poll() is None:
            process.terminate()
            try: process.wait(timeout=15)
            except subprocess.TimeoutExpired: process.kill(); process.wait()
    for log in logs: log.close()
    command(COMPOSE + ['down','--volumes','--remove-orphans'], 'cleanup')
