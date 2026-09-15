#!/usr/bin/env python3
"""Comprova persistência e retomada após reiniciar o MySQL dedicado da homologação."""
import importlib.util
import json
import pathlib
import subprocess
import time

spec = importlib.util.spec_from_file_location('finance', 'infra/testing/video-finance/validate.py')
v = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v)
cycle = json.loads(pathlib.Path('.codex/vega-approved/local-cycle.json').read_text())
url = v.API + f'/products/{cycle["productId"]}?chainId={cycle["chainDefinitionId"]}'
before = v.http(url)[0]
session = v.http('/api/pde/vega/private/v1/internal/sessions', dict(cycleId=cycle['id'], prototypeVersion=cycle['productVersion'], origin='QA_INTERNAL'), headers={'X-PDE-Internal-Token': 'vega-local-internal-only'})
subprocess.run(['docker', 'compose', '-p', 'aihub-36e8935e-c4cc-43ea-8bd1-8b9d6b1922ec-6ff58094f8', '-f', 'backend/ads-service/docker-compose.learning-cycles-local.yml', 'restart', 'learning-cycles-mysql'], check=True)
for attempt in range(30):
    try:
        after = v.http(url)[0]
        break
    except Exception:
        if attempt == 29: raise
        time.sleep(2)
assert before['events'] == after['events'] and before['stage'] == after['stage'] == 'VALIDATION'
v.http(f'/fixture/video-continuation/{cycle["productId"]}/{cycle["id"]}/integrate', {})
assert v.http(url)[0]['events'] == before['events']
resumed = v.http('/api/pde/vega/private/v1/session', headers={'X-Vega-Session': session['sessionToken']})
assert resumed['id'] == session['id'] and resumed['videoIntegration'] == session['videoIntegration']
assert not resumed['paymentEnabled']
print('PASS reinício MySQL: recibo único, retomada da sessão e gates preservados')
