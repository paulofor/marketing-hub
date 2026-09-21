#!/usr/bin/env python3
"""Homologa planos financeiros com API/JPA/MySQL reais e Plutus simulado, sem publicação."""
import argparse, hashlib, json, os, pathlib, subprocess, time, urllib.request, xml.etree.ElementTree as ET
ROOT=pathlib.Path(__file__).resolve().parents[3]
DEFAULT_PROJECT='aihub-339e0d38-fe59-4d38-bc4c-5cbf81bc5012-56dddc3aa6'
PROJECT=os.environ.get('FINANCIAL_PLAN_COMPOSE_PROJECT',DEFAULT_PROJECT)
if os.environ.get('GITHUB_ACTIONS')!='true' and PROJECT!=DEFAULT_PROJECT:
    raise SystemExit('Use o projeto exclusivo desta sandbox.')
COMPOSE=['docker','compose','-p',PROJECT,'-f',str(ROOT/'infra/testing/product-financial-plan/compose.yml')]
parser=argparse.ArgumentParser()
parser.add_argument('--rounds',type=int,default=1)
parser.add_argument('--persistence-only',action='store_true',help='Diagnóstico/CI de persistência; não conta como matriz completa')
parser.add_argument('--integration-diagnostic',action='store_true',help='Diagnóstico de integração e navegador; não conta como matriz completa')
args=parser.parse_args()
if args.persistence_only and args.integration_diagnostic:
    parser.error('Escolha somente um modo de diagnóstico.')
ART=ROOT/'artifacts/product-financial-plan'
ART.mkdir(parents=True,exist_ok=True)
FRONT_TESTS=['src/pages/financial/FinancialPlansPage.test.tsx','src/pages/product/ProductListPage.test.tsx','src/pages/product/ProductFinancialPage.test.tsx','src/pages/productType/ProductTypeCatalogPage.test.tsx','src/components/MainNavigation.test.tsx']

def run(command, log, cwd=ROOT, env=None):
    print(json.dumps({'phase':log.stem}),flush=True)
    with log.open('w') as f:
        r=subprocess.run(command,cwd=cwd,env=env,stdout=f,stderr=subprocess.STDOUT)
    if r.returncode:
        print(log.read_text()[-4000:],flush=True)
        raise RuntimeError(f'Falha em {log.relative_to(ROOT)}')

def stop(process):
    if process and process.poll() is None:
        process.terminate()
        try:process.wait(timeout=20)
        except subprocess.TimeoutExpired:process.kill();process.wait()

def ready(process,url):
    deadline=time.monotonic()+100
    while time.monotonic()<deadline:
        if process.poll() is not None:raise RuntimeError('Aplicação local encerrou; confira o log')
        try:
            with urllib.request.urlopen(url,timeout=2) as r:
                if r.status==200:return
        except OSError:time.sleep(.4)
    raise RuntimeError('Aplicação local não ficou disponível')

def read(path):
    with urllib.request.urlopen('http://127.0.0.1:18095'+path,timeout=10) as r:return json.load(r)

def test_count(module):
    result=dict(tests=0,failures=0,errors=0,skipped=0)
    for file in (ROOT/module/'target/surefire-reports').glob('TEST-*.xml'):
        node=ET.parse(file).getroot()
        for key in result:result[key]+=int(node.attrib.get(key,0))
    if not result['tests'] or result['failures'] or result['errors']:raise RuntimeError('Testes inválidos: '+module)
    return result

def fingerprint():
    names=subprocess.check_output(['git','diff','--name-only'],cwd=ROOT,text=True).splitlines()
    names+=subprocess.check_output(['git','ls-files','--others','--exclude-standard'],cwd=ROOT,text=True).splitlines()
    h=hashlib.sha256()
    for name in sorted(set(names)):
        if name.startswith(('docs/','artifacts/')):continue
        path=ROOT/name
        if path.is_file():h.update(name.encode());h.update(path.read_bytes())
    return h.hexdigest()

for round in range(1,args.rounds+1):
    folder=ART/(('persistence-' if args.persistence_only else 'diagnostic-' if args.integration_diagnostic else 'round-')+str(round))
    if folder.exists():folder.rename(ART/(folder.name+'-prior-'+str(time.time_ns())))
    folder.mkdir()
    api=ui=None
    summary={'round':round,'completeMatrix':not (args.persistence_only or args.integration_diagnostic),'sourceFingerprint':fingerprint()}
    start=time.monotonic()
    try:
        run(['docker','version'],folder/'docker.log')
        run(['docker','buildx','version'],folder/'buildx.log')
        run(['docker','compose','version'],folder/'compose.log')
        if not args.persistence_only:
            if not args.integration_diagnostic:
                for module in ['backend/ads-service','financial-agent-worker']:
                    run(['mvn','-q','test'],folder/(module.replace('/','-')+'-tests.log'),ROOT/module)
                    summary[module]=test_count(module)
            run(['npm','run','typecheck'],folder/'typecheck.log',ROOT/'frontend')
            run(['npm','test','--','--run',*FRONT_TESTS],folder/'frontend-tests.log',ROOT/'frontend')
            run(['npm','run','build'],folder/'frontend-build.log',ROOT/'frontend',env=dict(os.environ,VITE_API_URL='http://127.0.0.1:15175'))
        run(['mvn','-q','-DskipTests','test-compile','dependency:build-classpath','-DincludeScope=test','-Dmdep.outputFile=target/financial-plan.classpath'],folder/'fixture-compile.log',ROOT/'backend/ads-service')
        classpath=os.pathsep.join([str(ROOT/'backend/ads-service/target/test-classes'),str(ROOT/'backend/ads-service/target/classes'),(ROOT/'backend/ads-service/target/financial-plan.classpath').read_text().strip()])
        run(COMPOSE+['down','--volumes','--remove-orphans'],folder/'cleanup-before.log')
        run(COMPOSE+['up','-d','--wait'],folder/'mysql.log')
        java=['java','-Xmx768m','-cp',classpath,'com.marketinghub.financialplan.v1.service.FinancialPlanLocalApplication']
        api_log=(folder/'api.log').open('w')
        api=subprocess.Popen(java,cwd=ROOT,stdout=api_log,stderr=subprocess.STDOUT)
        ready(api,'http://127.0.0.1:18095/fixture/health')
        run(['node','infra/testing/product-financial-plan/api-matrix.mjs'],folder/'api-matrix.log')
        if not args.persistence_only:
            ui_log=(folder/'ui.log').open('w')
            ui=subprocess.Popen(['node','infra/testing/product-financial-plan/frontend-server.mjs'],cwd=ROOT,stdout=ui_log,stderr=subprocess.STDOUT)
            ready(ui,'http://127.0.0.1:15175/financial/plans')
            run(['node','infra/testing/product-financial-plan/browser-matrix.mjs'],folder/'browser-matrix.log',env=dict(os.environ,FINANCIAL_PLAN_ARTIFACTS=str(folder/'browser')))
            run(['node','infra/testing/product-financial-plan/preparation-matrix.mjs'],folder/'preparation-matrix.log',env=dict(os.environ,FINANCIAL_PLAN_ARTIFACTS=str(folder/'preparation-browser')))
        before=read('/api/financial-plans/v1/products/95101')
        stop(api)
        api=subprocess.Popen(java,cwd=ROOT,stdout=api_log,stderr=subprocess.STDOUT)
        ready(api,'http://127.0.0.1:18095/fixture/health')
        if read('/api/financial-plans/v1/products/95101')!=before:raise RuntimeError('Reinício alterou histórico/parecer/custo')
        stop(api);api=None
        run(['java','-cp',classpath,'com.marketinghub.financialplan.v1.service.FinancialPlanMigrationCheck'],folder/'migration.log')
        run(['bash','scripts/validate-liquibase-mysql57.sh'],folder/'liquibase-static.log')
        run(['git','diff','--check'],folder/'diff.log')
        if summary['sourceFingerprint']!=fingerprint():raise RuntimeError('Código mudou durante a matriz')
        summary.update(result='PASS',durationSeconds=time.monotonic()-start)
        (folder/'summary.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n')
        print(json.dumps(summary),flush=True)
    finally:
        stop(ui);stop(api)
        run(COMPOSE+['down','--volumes','--remove-orphans'],folder/'cleanup.log')
