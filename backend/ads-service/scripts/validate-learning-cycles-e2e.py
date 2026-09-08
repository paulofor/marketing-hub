#!/usr/bin/env python3
"""Homologa contratos REST, MySQL e retornos com integrações exclusivamente locais."""
import concurrent.futures
import datetime as dt
import json
import os
import subprocess
import urllib.request
import urllib.error
import uuid

BASE = 'http://127.0.0.1:18091'
API = '/api/business-process-chains/learning-cycles/v1'
PROJECT = os.environ['LEARNING_CYCLES_COMPOSE_PROJECT']
COMPOSE = ['docker', 'compose', '-p', PROJECT, '-f', 'backend/ads-service/docker-compose.learning-cycles-local.yml']
checks = []


def iso(value):
    return value.isoformat(timespec='seconds').replace('+00:00', 'Z')


def now():
    return dt.datetime.now(dt.timezone.utc)


def http(path, body=None, expected=200):
    data = None if body is None else json.dumps(body).encode()
    request = urllib.request.Request(BASE + path, data=data, headers={'Content-Type': 'application/json'})
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            status, result = response.status, json.load(response)
    except urllib.error.HTTPError as error:
        status, result = error.code, json.loads(error.read())
    assert status == expected, (path, status, expected, result)
    return result


def sql(query):
    result = subprocess.run(COMPOSE + ['exec', '-T', 'learning-cycles-mysql', 'mysql', '-ucycles_local',
        '-pcycles-local-only', '--default-character-set=utf8mb4', '--batch', '--skip-column-names', 'learning_cycles_local', '-e', query],
        capture_output=True, text=True, check=True)
    return result.stdout.strip()


def reset():
    sql('UPDATE learning_sales_cycle_v1 SET current_instance_id=NULL; DELETE FROM learning_sales_cycle_event_v1; '
        'DELETE FROM learning_sales_cycle_v1 ORDER BY id DESC; DELETE FROM business_process_activity_instance;')
    http('/fixture/reset', {})


def brief(experiment=91001, predecessor=None, baseline=False):
    return dict(requestKey=str(uuid.uuid4()), chainDefinitionId=91001, experimentId=experiment,
        previousCycleId=predecessor, baseline=baseline, productVersion='fixture-v1', hypothesis='Ação concreta aumenta primeiro uso',
        mainChange='Instrução executável', successCriterion='Cinco vendas líquidas com uso e contribuição positiva',
        audience='Pessoas aderentes consentidas', offer='Oferta de teste segregada', acquisition='Canal simulado',
        budgetLimitBrl=100, windowStart=iso(now()-dt.timedelta(days=1)), windowEnd=iso(now()+dt.timedelta(days=1)),
        sampleTarget=10, minimumNetSales=5, operatorName='Homologação local')


def command(cycle, action='COMPLETE', evidence=None, expected=200, request=None):
    request = request or dict(requestKey=str(uuid.uuid4()), expectedRevision=cycle['revision'], action=action,
        operatorName='Homologação local', summary='Evidência local sem venda real', evidenceReference='internal://fixture/learning-cycles', evidence=evidence or {})
    return http(f'{API}/products/{cycle["productId"]}/{cycle["id"]}/commands', request, expected)


def metrics(cycle, **overrides):
    data = dict(experimentId=cycle['experimentId'], currency='BRL', source='Conciliação simulada, sem métricas de produção',
        periodStart=cycle['windowStart'], periodEnd=iso(now()-dt.timedelta(seconds=2)), observedAt=iso(now()),
        sessions=10, starts=8, firstResults=7, checkouts=6, netSales=5, refunds=0, spendBrl=40, revenueBrl=335,
        contributionBrl=200, dataValid=True, testDataExcluded=True, deliveryVerified=True, useVerified=True, satisfactionVerified=True)
    data.update(overrides)
    return data


def to_validation(cycle):
    cycle=command(cycle,evidence=dict(learning='Valor precisa ser mais concreto',competingExplanation='Tráfego pequeno também explica o resultado'))
    cycle=command(cycle,evidence=dict(planReference='internal://plan/v1',stopRule='Parar no teto ou fim da janela'))
    return command(cycle,evidence=dict(productVersion=cycle['productVersion'],changeEvidence='internal://release/v1'))


def approval(cycle, product=None, version=None):
    return http('/fixture/approval',dict(productId=product or cycle['productId'],productVersion=version or cycle['productVersion']))['approvalInstanceId']


def validation_data(cycle, gate=None):
    return dict(approvalInstanceId=gate or approval(cycle),journeyEvidence='internal://journey/approved',
        humanObservationEvidence='internal://consent/observations',instrumentationVerified=True)


def authorization_data(cycle):
    return dict(confirmed=True,productVersion=cycle['productVersion'],budgetLimitBrl=cycle['budgetLimitBrl'])


def live(cycle):
    cycle=to_validation(cycle)
    cycle=command(cycle,evidence=validation_data(cycle))
    cycle=command(cycle,evidence=authorization_data(cycle))
    http(f'/fixture/experiments/{cycle["experimentId"]}/publish',{})
    return command(cycle)


def check(name):
    checks.append(name)
    print(f'PASS {name}', flush=True)


reset()
catalog=http(f'{API}/catalog?chainId=91001&productId=91001')
assert len(catalog['diagram']['nodes']) == 13 and len(catalog['returnTargets']) == 2
assert any(flow.get('kind')=='REWORK' and flow['to']=='LEARNING' for flow in catalog['diagram']['flows'])
target=catalog['returnTargets'][1]
return_to=dict(returnProcessId=target['processDefinitionId'],returnActivityId=target['activityId'],rootCause='Microação abstrata')
check('BPM publicado com losangos, atividades reais e retorno ao aprendizado')

payload=brief(); cycle=http(f'{API}/products/91001',payload)
assert http(f'{API}/products/91001',payload)['id']==cycle['id']
http(f'{API}/products/91001',dict(payload,hypothesis='Outro conteúdo'),409)
http(f'{API}/products/91001',brief(91002),409)
assert sql('SELECT COUNT(*) FROM learning_sales_cycle_v1')=='1'
check('Adoção idempotente, conteúdo divergente recusado e um ciclo aberto por produto/cadeia')

command(cycle,action='SCALE',expected=409)
command(cycle,expected=409)
http(f'{API}/products/91002/{cycle["id"]}/commands',dict(requestKey=str(uuid.uuid4()),expectedRevision=0,action='STOP',operatorName='Local',summary='X',evidenceReference='fixture',evidence={}),404)
http(f'{API}/products/91001',dict(payload,requestKey='inválida'),400)
check('Validação de entrada, isolamento de produto e proibição de salto de etapas')

cycle=to_validation(cycle)
old_gate=approval(cycle)
cycle=command(cycle,'REWORK',dict(return_to,productVersion='fixture-v2'))
assert cycle['stage']=='ADJUSTMENT' and cycle['experimentId']==91001
cycle=command(cycle,evidence=dict(productVersion='fixture-v2',changeEvidence='Correção aplicada'))
command(cycle,evidence=validation_data(cycle,old_gate),expected=409)
command(cycle,evidence=validation_data(cycle,approval(cycle,product=91002)),expected=409)
good_gate=approval(cycle)
http('/fixture/approval',dict(productId=cycle['productId'],productVersion=cycle['productVersion'],approved=False))
command(cycle,evidence=validation_data(cycle,good_gate),expected=409)
cycle=command(cycle,evidence=validation_data(cycle))
assert cycle['stage']=='AUTHORIZATION' 
http('/fixture/approval',dict(productId=cycle['productId'],productVersion=cycle['productVersion'],approved=False))
command(cycle,evidence=authorization_data(cycle),expected=409)
cycle=command(cycle,'REWORK',dict(return_to,productVersion='fixture-v3'))
cycle=command(cycle,evidence=dict(productVersion='fixture-v3',changeEvidence='Correção final aplicada'))
cycle=command(cycle,evidence=validation_data(cycle))
check('Reprovação retorna ao ajuste sem duplicar experimento; aprovação antiga ou de outro produto bloqueada')

command(cycle,evidence=dict(authorization_data(cycle),confirmed=False),expected=409)
cycle=command(cycle,evidence=authorization_data(cycle))
command(cycle,expected=409)
http('/fixture/experiments/91001/publish',{})
cycle=command(cycle)
assert cycle['stage']=='MEASUREMENT'
check('Autorização explícita e publicação comprovada pelo run produtivo com preflight')

command(cycle,'REWORK',dict(return_to,productVersion='fixture-v4',technicalOnly=True),expected=409)
http('/fixture/experiments/91001/stop',{})
command(cycle,'REWORK',dict(return_to,productVersion='fixture-v4'),expected=409)
cycle=command(cycle,'REWORK',dict(return_to,productVersion='fixture-v4',technicalOnly=True))
cycle=command(cycle,evidence=dict(productVersion='fixture-v4',changeEvidence='Correção técnica sem mudar a hipótese'))
proof=validation_data(cycle); proof.pop('humanObservationEvidence')
cycle=command(cycle,evidence=proof)
cycle=command(cycle,evidence=authorization_data(cycle))
command(cycle,expected=409)
http('/fixture/experiments/91001/publish',{})
cycle=command(cycle)
assert cycle['experimentId']==91001 and cycle['stage']=='MEASUREMENT'
check('Recuperação técnica após publicação preserva experimento e exige pausa, nova homologação e publicação posterior')

command(cycle,'MEASURE',dict(metrics(cycle),experimentId=91002),expected=409)
cycle=command(cycle,'MEASURE',metrics(cycle,dataValid=False,testDataExcluded=False))
command(cycle,'SCALE',dict(scaleHypothesis='Ampliar'),expected=409)
command(cycle,'ADJUST',dict(return_to,learning='Insuficiente',nextHypothesis='Nova versão'),expected=409)
cycle=command(cycle,'FIX_MEASUREMENT',dict(rootCause='Eventos sem origem',correctionPlan='Corrigir correlação no backend'))
assert cycle['stage']=='MEASUREMENT'
check('Métrica contaminada ou de outro experimento não governa ajuste ou escala')

cycle=command(cycle,'MEASURE',metrics(cycle,netSales=0))
assert not next(item for item in cycle['commands'] if item['action']=='SCALE')['available']
cycle=command(cycle,'CONTINUE')
cycle=command(cycle,'MEASURE',metrics(cycle,spendBrl=100))
command(cycle,'CONTINUE',expected=409)
check('Amostra e vendas bloqueiam escala; continuidade respeita teto autorizado')

cycle=command(cycle,'SCALE',dict(scaleHypothesis='Ampliar após vendas úteis'))
command(cycle,'AUTHORIZE_SCALE',dict(confirmed=True,productVersion=cycle['productVersion'],budgetLimitBrl=150,windowEnd=iso(now()+dt.timedelta(days=2))),expected=409)
http('/fixture/experiments/91001/budget',dict(budgetLimitBrl=150))
cycle=command(cycle,'AUTHORIZE_SCALE',dict(confirmed=True,productVersion=cycle['productVersion'],budgetLimitBrl=150,windowEnd=iso(now()+dt.timedelta(days=2))))
assert cycle['stage']=='MEASUREMENT' and cycle['budgetLimitBrl']==150
check('Escala exige nova autorização e limite oficial consistente, sem alterar mídia pelo ciclo')

cycle=command(cycle,'MEASURE',metrics(cycle))
command(cycle,'ADJUST',dict(return_to,learning='Melhorar microação',nextHypothesis='Ação guiada'),expected=409)
http('/fixture/experiments/91001/stop',{})
cycle=command(cycle,'ADJUST',dict(return_to,learning='Melhorar microação',nextHypothesis='Ação guiada'))
assert cycle['status']=='ADJUSTED' and cycle['canCreateSuccessor']
successor=http(f'{API}/products/91001',brief(91002,cycle['id']))
assert successor['stage']=='LEARNING' and successor['inheritedLearning']['experimentId']==91001
assert f'learningCycleId={successor["id"]}' in successor['workUrl'] or successor['workUrl']==f'/experiments/{successor["experimentId"]}'
assert len(http(f'{API}/products/91001?chainId=91001'))==2
assert successor['inheritedLearning']['events'][-1]['evidence']['nextHypothesis']=='Ação guiada'
assert sql('SELECT COUNT(*) FROM learning_sales_cycle_v1')=='2'
command(cycle,'STOP',expected=409)
check('Sucessor com experimento novo, hipótese e memória herdada; predecessor permanece imutável')

request=dict(requestKey=str(uuid.uuid4()),expectedRevision=successor['revision'],action='COMPLETE',operatorName='Local',summary='Revisão concorrente',evidenceReference='fixture',evidence=dict(learning='Aprender',competingExplanation='Outra hipótese'))
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
    results=list(pool.map(lambda _:command(successor,request=request),range(2)))
assert results[0]['revision']==results[1]['revision']==1
request2=dict(request,requestKey=str(uuid.uuid4()))
command(successor,request=request2,expected=409)
assert sql(f'SELECT COUNT(*) FROM learning_sales_cycle_event_v1 WHERE cycle_id={successor["id"]}')=='1'
check('Concorrência real MySQL: uma transição e um recibo; revisão obsoleta recusada')

other=http(f'{API}/products/91002',brief(91006))
assert other['productId']==91002 and other['inheritedLearning']=={}
assert len(http(f'{API}/products/91002'))==1
assert all(option['id']==91006 for option in http(f'{API}/catalog?chainId=91001&productId=91002')['experiments'])
check('Ciclos simultâneos segregados entre Vega e Mira de teste')

reset()
http('/fixture/experiments/91001/publish',{});http('/fixture/experiments/91001/stop',{})
http('/fixture/experiments/91001/plan-again',{})
option=next(item for item in http(f'{API}/catalog?chainId=91001&productId=91001')['experiments'] if item['id']==91001)
assert option['baseline'] and option['available']
http(f'{API}/products/91001',brief(),409)
historical=http(f'{API}/products/91001',brief(baseline=True))
assert historical['stage']=='MEASUREMENT' and historical['events']==[]
historical=command(historical,'MEASURE',metrics(historical,netSales=0))
historical=command(historical,'INCONCLUSIVE')
assert historical['status']=='INCONCLUSIVE' and not historical['canCreateSuccessor']
assert sql("SELECT COUNT(*) FROM business_process_activity_instance WHERE objective_achieved=1")=='1'
check('Adoção histórica inicia por conciliação e não fabrica homologação, venda ou aprovação')

print(json.dumps({'checks':len(checks),'passed':checks,'database':'MySQL 5.7','externalCalls':0},ensure_ascii=False),flush=True)
