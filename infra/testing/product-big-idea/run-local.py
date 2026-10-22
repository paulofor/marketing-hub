"""Homologa Big Idea com API real, MySQL descartável e referências sintéticas."""
import json
import os
from pathlib import Path
import subprocess
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[3]
ART = ROOT / 'artifacts/product-big-idea/persistence'
ART.mkdir(parents=True, exist_ok=True)
project = os.environ['BIG_IDEA_COMPOSE_PROJECT']
if not project.startswith('aihub-'):
    raise RuntimeError('Use projeto Compose exclusivo aihub-*')
compose = ['docker', 'compose', '-p', project, '-f', str(ROOT / 'infra/testing/product-big-idea/compose.yml')]
api = None

def run(command, name, cwd=ROOT):
    with (ART / name).open('w') as log:
        result = subprocess.run(command, cwd=cwd, stdout=log, stderr=subprocess.STDOUT)
    if result.returncode:
        print((ART / name).read_text()[-6000:])
        raise RuntimeError(f'{name}: exit {result.returncode}')

def read(path):
    with urllib.request.urlopen('http://127.0.0.1:18099' + path, timeout=10) as response:
        return json.load(response)

def start():
    cp = os.pathsep.join([str(ROOT/'backend/ads-service/target/test-classes'), str(ROOT/'backend/ads-service/target/classes'), (ROOT/'backend/ads-service/target/big-idea.classpath').read_text().strip()])
    process = subprocess.Popen(['java', '-Xmx512m', '-cp', cp, 'com.marketinghub.product.bigidea.v1.service.BigIdeaLocalApplication'], cwd=ROOT, stdout=(ART/'api.log').open('a'), stderr=subprocess.STDOUT)
    for _ in range(90):
        if process.poll() is not None:
            raise RuntimeError((ART/'api.log').read_text()[-6000:])
        try:
            read('/fixture/health')
            return process
        except (OSError, ValueError):
            time.sleep(1)
    process.terminate()
    raise RuntimeError('API local não ficou pronta')

def stop(process):
    if process and process.poll() is None:
        process.terminate()
        try:
            process.wait(timeout=15)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=5)

try:
    run(['docker', 'version'], 'docker.log')
    run(['docker', 'buildx', 'version'], 'buildx.log')
    run(['docker', 'compose', 'version'], 'compose.log')
    run(['mvn', '-q', '-DskipTests', 'test-compile', 'org.apache.maven.plugins:maven-dependency-plugin:3.6.1:build-classpath', '-DincludeScope=test', '-Dmdep.outputFile=target/big-idea.classpath'], 'compile.log', ROOT/'backend/ads-service')
    run(compose+['up','-d','--wait'], 'mysql.log')
    api = start()
    run(['node','infra/testing/product-big-idea/api-matrix.mjs'], 'matrix.log')
    if os.environ.get('BIG_IDEA_BROWSER') == '1':
        run(['node', 'infra/testing/product-big-idea/browser.mjs'], 'browser.log')
    before = read('/api/products/99101/big-idea')
    stop(api)
    api = start()  # Reaplica Liquibase e valida recuperação sem alterar revisões.
    assert before == read('/api/products/99101/big-idea'), 'Reinício alterou o histórico'
    (ART/'summary.json').write_text(json.dumps({'result':'PASS','restart':'PASS','liquibaseReapply':'PASS','commercialEvents':0})+'\n')
    print((ART/'summary.json').read_text())
finally:
    stop(api)
    run(compose+['down','--volumes','--remove-orphans'], 'cleanup.log')
