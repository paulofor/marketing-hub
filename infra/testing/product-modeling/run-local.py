"""Homologa API, MySQL 5.7 e tela reais, com dependências externas segregadas."""
import json
import os
from pathlib import Path
import subprocess
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[3]
ART = ROOT / 'artifacts/product-modeling/local'
ART.mkdir(parents=True, exist_ok=True)
project = os.environ['MODELING_COMPOSE_PROJECT']
if not project.startswith('aihub-'):
    raise RuntimeError('Use projeto Compose exclusivo aihub-*')
compose = ['docker', 'compose', '-p', project, '-f', str(ROOT/'infra/testing/product-modeling/compose.yml')]
processes = []

def run(cmd, name, cwd=ROOT):
    with (ART/name).open('w') as log:
        r = subprocess.run(cmd, cwd=cwd, stdout=log, stderr=subprocess.STDOUT)
    if r.returncode:
        raise RuntimeError(name + ': ' + (ART/name).read_text()[-4500:])

def spawn(cmd, name):
    p = subprocess.Popen(cmd, cwd=ROOT, stdout=(ART/name).open('a'), stderr=subprocess.STDOUT)
    processes.append(p)
    return p

def wait(url, p):
    for _ in range(90):
        if p.poll() is not None:
            raise RuntimeError('Processo local terminou antes de ficar pronto')
        try:
            with urllib.request.urlopen(url, timeout=2) as r:
                return r.read()
        except OSError:
            time.sleep(1)
    raise RuntimeError('Serviço local não ficou pronto')

def stop(p):
    if p.poll() is None:
        p.terminate()
        try:
            p.wait(timeout=10)
        except subprocess.TimeoutExpired:
            p.kill()
            p.wait()

try:
    run(['docker','version'],'docker.log')
    run(['docker','buildx','version'],'buildx.log')
    run(['docker','compose','version'],'compose.log')
    run(['mvn','-q','-DskipTests','test-compile','org.apache.maven.plugins:maven-dependency-plugin:3.6.1:build-classpath','-DincludeScope=test','-Dmdep.outputFile=target/modeling.classpath'],'compile.log',ROOT/'backend/ads-service')
    run(compose+['up','-d','--wait'],'mysql.log')
    cp=os.pathsep.join([str(ROOT/'backend/ads-service/target/test-classes'),str(ROOT/'backend/ads-service/target/classes'),(ROOT/'backend/ads-service/target/modeling.classpath').read_text().strip()])
    cmd=['java','-Xmx512m','-cp',cp,'com.marketinghub.product.modeling.v1.service.ModelingLocalApplication']
    api=spawn(cmd,'api.log');wait('http://127.0.0.1:18101/fixture/health',api)
    ui=spawn(['node','frontend/node_modules/vite/bin/vite.js','--config','infra/testing/product-modeling/vite.config.mjs'],'ui.log');wait('http://127.0.0.1:5179',ui)
    if os.environ.get('MODELING_BROWSER', '1') == '1':
        run(['node','infra/testing/product-modeling/browser.cjs'],'browser.log')
    run(['node','infra/testing/product-modeling/api-matrix.cjs'],'matrix.log')
    before=wait('http://127.0.0.1:18101/api/product-modeling/v1',api)
    stop(api);api=spawn(cmd,'api.log');wait('http://127.0.0.1:18101/fixture/health',api)
    after=wait('http://127.0.0.1:18101/api/product-modeling/v1',api)
    assert json.loads(before)==json.loads(after), 'Reinício alterou modelagens'
    (ART/'summary.json').write_text(json.dumps({'result':'PASS','mysql57':'PASS','restart':'PASS','browser':'PASS' if os.environ.get('MODELING_BROWSER', '1') == '1' else 'NOT_REPEATED','externalSpend':0})+'\n')
    print((ART/'summary.json').read_text())
finally:
    for p in reversed(processes):
        stop(p)
    run(compose+['down','--volumes','--remove-orphans'],'cleanup.log')
