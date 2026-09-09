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
            payload = response.read()
            status, result = response.status, json.loads(payload) if payload else None
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
    return dict(requestKey=str(uuid.uuid4()), chainDefinitionId=91002, experimentId=experiment,
        previousCycleId=predecessor, baseline=baseline, productVersion='fixture-v1', hypothesis='Ação concreta aumenta primeiro uso',
        mainChange='Instrução executável', successCriterion='Cinco vendas líquidas com uso e contribuição positiva',
        audience='Pessoas aderentes consentidas', offer='Oferta de teste segregada', acquisition='Canal simulado',
        budgetLimitBrl=100, windowStart=iso(now()-dt.timedelta(days=1)), windowEnd=iso(now()+dt.timedelta(days=1)),
        sampleTarget=10, minimumNetSales=5, operatorName='Homologação local')


def command(cycle, action='COMPLETE', evidence=None, expected=200, request=None):
    request = request or dict(requestKey=str(uuid.uuid4()), expectedRevision=cycle['revision'], action=action,
        operatorName='Homologação local', summary='Evidência local sem venda real', evidenceReference='internal://fixture/learning-cycles', evidence=evidence or {})
    return http(f'{API}/products/{cycle["productId"]}/{cycle["id"]}/commands', request, expected)


def configure_measurement(experiment=91001, **values):
    return http(f'/fixture/experiments/{experiment}/measurement', values)


def reconcile(cycle, expected=200, request=None, product=None):
    request = request or dict(requestKey=str(uuid.uuid4()), expectedRevision=cycle['revision'])
    return http(
        f'{API}/products/{product or cycle["productId"]}/{cycle["id"]}/measurement-reconciliation',
        request,
        expected)


def to_validation(cycle):
    cycle=command(cycle,evidence=dict(learning='Valor precisa ser mais concreto',competingExplanation='Tráfego pequeno também explica o resultado'))
    cycle=command(cycle,evidence=dict(planReference='internal://plan/v1',stopRule='Parar no teto ou fim da janela'))
    return with_videos(command(cycle,evidence=dict(productVersion=cycle['productVersion'],changeEvidence='internal://release/v1')))


def with_videos(cycle):
    assert cycle['stage'] == 'VIDEO_BRIEF', cycle
    refs=http('/fixture/videos',dict(experimentId=cycle['experimentId'],productVersion=cycle['productVersion']))
    cycle=command(cycle,evidence={field:'Briefing local: '+field for field in ('briefReference','campaignGoal','campaignCta','campaignMetric','pdeGoal','pdeCta','pdeMetric','controlledVariables','productionBudgetReference')})
    assert cycle['stage']=='CAMPAIGN_VIDEO' and cycle['workLinks']
    command(cycle,evidence=dict(campaignVideoAssetId=refs['pdeVideoAssetId'],productionEvidence='Papel incorreto'),expected=409)
    cycle=command(cycle,evidence=dict(campaignVideoAssetId=refs['campaignVideoAssetId'],productionEvidence='Estúdio simulado'))
    assert cycle['stage']=='PDE_ENTRY_VIDEO'
    cycle=command(cycle,evidence=dict(pdeVideoAssetId=refs['pdeVideoAssetId'],productionEvidence='Versão real simulada'))
    assert cycle['stage']=='VIDEO_APPROVAL'
    proof=dict(creativeId=refs['creativeId'],pdeSlotId=refs['pdeSlotId'],technicalEvidence='Reprodução, fallback e desempenho',customerReviewEvidence='Parecer independente simulado',captionsVerified=True,mobileVerified=True,optionalPlaybackVerified=True,testDataExcluded=True)
    command(cycle,evidence=dict(proof,optionalPlaybackVerified=False),expected=409)
    cycle=command(cycle,evidence=proof)
    assert cycle['stage']=='VALIDATION'
    return cycle


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
catalog=http(f'{API}/catalog?chainId=91002&productId=91001')
entry=catalog['entry']
parent_id=entry['parentProcessDefinitionId']
def parent_history(product=91001, chain=91002, cycle_id=None):
    query=f'?chainId={chain}' + (f'&learningCycleId={cycle_id}' if cycle_id else '')
    return http(f'/api/business-processes/{parent_id}/products/{product}/activity-executions{query}')


def cycle_call(history):
    return next(activity for activity in history['activities'] if activity['activityId']=='learningCycle')


assert entry['sequenceNumber']==6 and entry['canStartCycle'] and entry['integrated']
assert entry['activitySequenceNumber']==4 and entry['parentUrl'].endswith('#activity-learningCycle')
assert [route['sequenceNumber'] for route in entry['returnRoutes']]==[2,3,4,5,6]
assert http('/api/business-process-chains/91002')['processCount']==6
before=sql('SELECT COUNT(*) FROM learning_sales_cycle_v1')
for process_id in (parent_id,entry['processDefinitionId']):
    direct=http(f'{API}/entry?processDefinitionId={process_id}&productId=91001&chainId=91002')
    assert direct['workspaceUrl']==entry['workspaceUrl'] and direct['parentProcessDefinitionId']==parent_id
assert sql('SELECT COUNT(*) FROM learning_sales_cycle_v1')==before
assert http(f'{API}/entry?processDefinitionId={parent_id}&chainId=91000') is None
assert http(f'{API}/entry?processDefinitionId=1&productId=91001') is None
http(f'{API}/entry?processDefinitionId={parent_id}&productId=91999',expected=404)
old=http(f'{API}/catalog?chainId=91000&productId=91001')
assert not old['entry']['integrated'] and not old['entry']['canStartCycle']
historical_request=brief();historical_request['chainDefinitionId']=91000
http(f'{API}/products/91001',historical_request,expected=409)
check('Entrada pelo BPM e subprocesso conserva contexto; seis processos; GET não cria ciclo; histórico e produtos segregados')

started=http(f'{API}/products/91001',brief())
resumed=http(f'{API}/entry?processDefinitionId={parent_id}&productId=91001')
assert f'cycleId={started["id"]}' in resumed['workspaceUrl']
assert 'cycleId=' not in http(f'{API}/entry?processDefinitionId={parent_id}&productId=91002')['workspaceUrl']
assert sql('SELECT COUNT(*) FROM learning_sales_cycle_v1')=='1'
parent=parent_history()
assert parent['operationalState']=='IN_PROGRESS' and parent['currentActivityId']=='learningCycle'
assert cycle_call(parent)['stateEvidence']=='NOT_RECORDED' and parent['salesFlow']['cycleId']==started['id']
assert f'cycleId={started["id"]}' in cycle_call(parent)['executionControl']['navigationUrl']
assert not cycle_call(parent_history(91002))['objectiveAchieved']
assert 'cycleId=' not in cycle_call(parent_history(91002))['executionControl']['navigationUrl']
http(f'/api/business-processes/{parent_id}/products/91001/activity-executions?chainId=91000',expected=409)
assert [a['executionControl']['interactionType'] for a in parent['activities']]==['SUBPROCESS','SUBPROCESS','AUTOMATIC','SUBPROCESS']
assert not any(a['objectiveAchieved'] for a in parent['activities'])
check('Retomada usa a ocorrência aberta do produto sem duplicar ou contaminar outro produto')
check('Atividade do pai projeta ciclo e estado reais, conserva a cadeia e distingue as três chamadas')
reset()
assert len(catalog['diagram']['nodes']) == 17 and catalog['version'] == 3 and catalog['entry']['integrated']
assert any(flow.get('kind')=='REWORK' and flow['to']=='LEARNING' for flow in catalog['diagram']['flows'])
target=next(item for item in catalog['returnTargets'] if item['processCode']=='pde-construction-approval' and item['activityId']=='rework')
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
cycle=with_videos(command(cycle,evidence=dict(productVersion='fixture-v2',changeEvidence='Correção aplicada')))
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
cycle=with_videos(command(cycle,evidence=dict(productVersion='fixture-v3',changeEvidence='Correção final aplicada')))
cycle=command(cycle,evidence=validation_data(cycle))
check('Reprovação retorna ao ajuste sem duplicar experimento; aprovação antiga ou de outro produto bloqueada')

command(cycle,evidence=dict(authorization_data(cycle),confirmed=False),expected=409)
cycle=command(cycle,evidence=authorization_data(cycle))
command(cycle,expected=409)
http('/fixture/experiments/91001/publish',{})
cycle=command(cycle)
assert cycle['stage']=='DECISION'
assert cycle['events'][-1]['action']=='MEASURE' and cycle['events'][-1]['evidence']['automatic']
assert cycle['events'][-1]['operatorName']=='Marketing Hub · backend'
assert cycle['events'][-1]['evidence']['sources']['pdeAnalytics']['trafficQualityIncluded']=='HUMAN'
check('Autorização, publicação e conciliação automática comprovadas sem digitação de métricas')

command(cycle,'REWORK',dict(return_to,productVersion='fixture-v4',technicalOnly=True),expected=409)
http('/fixture/experiments/91001/stop',{})
command(cycle,'REWORK',dict(return_to,productVersion='fixture-v4'),expected=409)
cycle=command(cycle,'REWORK',dict(return_to,productVersion='fixture-v4',technicalOnly=True))
cycle=with_videos(command(cycle,evidence=dict(productVersion='fixture-v4',changeEvidence='Correção técnica sem mudar a hipótese')))
proof=validation_data(cycle); proof.pop('humanObservationEvidence')
cycle=command(cycle,evidence=proof)
cycle=command(cycle,evidence=authorization_data(cycle))
command(cycle,expected=409)
http('/fixture/experiments/91001/publish',{})
cycle=command(cycle)
assert cycle['experimentId']==91001 and cycle['stage']=='MEASUREMENT'
assert cycle['events'][-1]['action']=='MEASUREMENT_BLOCKED'
assert 'fontes ainda não mudaram' in cycle['events'][-1]['evidence']['blocker']
configure_measurement(snapshot='technical-v2')
cycle=reconcile(cycle)
assert cycle['stage']=='DECISION'
check('Recuperação técnica preserva experimento, refaz gates e exige uma fotografia realmente nova')

command(cycle,'MEASURE',dict(experimentId=91002),expected=409)
reconcile(cycle,expected=404,product=91002)
reconcile(cycle,expected=400,request=dict(requestKey=str(uuid.uuid4())))
reconcile(cycle,expected=409,request=dict(requestKey=str(uuid.uuid4()),expectedRevision=cycle['revision']-1))
assert not next(item for item in cycle['commands'] if item['action']=='SCALE')['available']
cycle=command(cycle,'CONTINUE')
assert cycle['stage']=='MEASUREMENT' and cycle['events'][-1]['action']=='MEASUREMENT_BLOCKED'
configure_measurement(ready=False,snapshot='source-down',blocker='Snapshot final da campanha indisponível')
retry_request=dict(requestKey=str(uuid.uuid4()),expectedRevision=cycle['revision'])
cycle=reconcile(cycle,request=retry_request)
assert cycle['stage']=='MEASUREMENT' and 'indisponível' in cycle['events'][-1]['evidence']['blocker']
assert 'sessions' not in cycle['events'][-1]['evidence']
assert parent_history()['currentActivityId']=='consolidate' and parent_history()['operationalState']=='BLOCKED'
assert cycle_call(parent_history())['operationalState']=='WAITING'
assert 'indisponível' in parent_history()['currentActivityStateReason']
assert reconcile(cycle,request=retry_request)['revision']==cycle['revision']
reconcile(cycle,expected=409,request=dict(retry_request,expectedRevision=cycle['revision']))
configure_measurement(snapshot='cap-v3',netSales=0,spendBrl=100,contributionBrl=-100)
cycle=reconcile(cycle)
assert cycle['stage']=='DECISION'
command(cycle,'CONTINUE',expected=409)
check('Fonte indisponível não vira zero; produto, revisão, amostra, vendas e teto governam a coleta')

configure_measurement(snapshot='sales-v4',netSales=5,checkouts=6,spendBrl=100,revenueBrl=335,
    contributionBrl=200,deliveryVerified=True,useVerified=True,satisfactionVerified=True)
cycle=command(cycle,'FIX_MEASUREMENT',dict(rootCause='Fotografia anterior não tinha vendas',correctionPlan='Reconciliar novo snapshot oficial'))
assert cycle['stage']=='DECISION'

cycle=command(cycle,'SCALE',dict(scaleHypothesis='Ampliar após vendas úteis'))
command(cycle,'AUTHORIZE_SCALE',dict(confirmed=True,productVersion=cycle['productVersion'],budgetLimitBrl=150,windowEnd=iso(now()+dt.timedelta(days=2))),expected=409)
http('/fixture/experiments/91001/budget',dict(budgetLimitBrl=150))
configure_measurement(snapshot='scale-v5',netSales=5,checkouts=6,spendBrl=110,revenueBrl=335,
    contributionBrl=190,deliveryVerified=True,useVerified=True,satisfactionVerified=True)
cycle=command(cycle,'AUTHORIZE_SCALE',dict(confirmed=True,productVersion=cycle['productVersion'],budgetLimitBrl=150,windowEnd=iso(now()+dt.timedelta(days=2))))
assert cycle['stage']=='DECISION' and cycle['budgetLimitBrl']==150
assert cycle['events'][-1]['action']=='MEASURE' and cycle['events'][-1]['evidence']['sourceFingerprint'].endswith('scale-v5')
check('Escala exige nova autorização, limite oficial e nova fotografia, sem alterar mídia pelo ciclo')

command(cycle,'ADJUST',dict(return_to,learning='Melhorar microação',nextHypothesis='Ação guiada'),expected=409)
http('/fixture/experiments/91001/stop',{})
cycle=command(cycle,'ADJUST',dict(return_to,learning='Melhorar microação',nextHypothesis='Ação guiada'))
assert cycle['status']=='ADJUSTED' and cycle['canCreateSuccessor']
successor=http(f'{API}/products/91001',brief(91002,cycle['id']))
assert successor['stage']=='LEARNING' and successor['inheritedLearning']['experimentId']==91001
assert f'learningCycleId={successor["id"]}' in successor['workUrl'] or successor['workUrl']==f'/experiments/{successor["experimentId"]}'
assert len(http(f'{API}/products/91001?chainId=91002'))==2
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
assert all(option['id']==91006 for option in http(f'{API}/catalog?chainId=91002&productId=91002')['experiments'])
check('Ciclos simultâneos segregados entre Vega e Mira de teste')

reset()
http('/fixture/experiments/91001/publish',{});http('/fixture/experiments/91001/stop',{})
http('/fixture/experiments/91001/plan-again',{})
option=next(item for item in http(f'{API}/catalog?chainId=91002&productId=91001')['experiments'] if item['id']==91001)
assert option['baseline'] and option['available']
http(f'{API}/products/91001',brief(),409)
historical=http(f'{API}/products/91001',brief(baseline=True))
assert historical['stage']=='DECISION' and len(historical['events'])==2
assert historical['events'][0]['action']=='ADOPT_BASELINE'
assert historical['events'][0]['evidence']['source']=='PRODUCTION_RUN'
assert historical['events'][1]['action']=='MEASURE' and historical['events'][1]['evidence']['automatic']
historical=command(historical,'INCONCLUSIVE')
assert historical['status']=='INCONCLUSIVE' and not historical['canCreateSuccessor']
assert sql("SELECT COUNT(*) FROM business_process_activity_instance WHERE objective_achieved=1")=='1'
check('Adoção histórica concilia automaticamente e não fabrica homologação, venda ou aprovação')

reset()
http('/fixture/experiments/91001/legacy-publication',{})
before=http('/fixture/experiments/91001/state')
option=next(item for item in http(f'{API}/catalog?chainId=91002&productId=91001')['experiments'] if item['id']==91001)
assert option['baseline'] and option['available'] and 'sem publicação registrada em run/preflight' in option['reason']
payload=brief(baseline=True)
legacy=http(f'{API}/products/91001',payload)
assert http(f'{API}/products/91001',payload)['id']==legacy['id']
assert len(legacy['events'])==2 and legacy['stage']=='DECISION'
proof=legacy['events'][0]
assert proof['action']=='ADOPT_BASELINE' and proof['evidence']['source']=='LEGACY_META_CAMPAIGN'
assert not proof['evidence']['preflightRecorded'] and proof['evidence']['experimentId']==91001
assert legacy['events'][1]['action']=='MEASURE' and legacy['events'][1]['evidence']['automatic']
assert http('/fixture/experiments/91001/state')==before==dict(status='USER_STOPPED',runCount=0,campaignCount=1)
legacy=command(legacy,'ADJUST',dict(return_to,learning='Amostra pequena e microação insuficiente',nextHypothesis='Primeiro ajuste executável'))
assert cycle_call(parent_history())['operationalState']=='IN_PROGRESS'
assert cycle_call(parent_history())['executionControl']['actionAvailable']
successor=http(f'{API}/products/91001',brief(91002,legacy['id']))
assert cycle_call(parent_history())['operationalState']=='IN_PROGRESS'
assert cycle_call(parent_history(cycle_id=legacy['id']))['operationalState']=='COMPLETED'
assert successor['stage']=='LEARNING' and successor['events']==[]
assert successor['inheritedLearning']['events'][0]['evidence']==proof['evidence']
assert http('/fixture/experiments/91002/state')==dict(status='PLANNED',runCount=0,campaignCount=0)
successor=to_validation(successor)
command(successor,evidence=dict(approvalInstanceId=1,journeyEvidence='Sem gate',instrumentationVerified=True),expected=409)
successor=command(successor,evidence=validation_data(successor))
successor=command(successor,evidence=authorization_data(successor))
command(successor,expected=409)
http('/fixture/experiments/91002/publish',{})
successor=command(successor)
assert successor['stage']=='DECISION'
assert successor['events'][-1]['action']=='MEASURE' and successor['events'][-1]['evidence']['automatic']
assert http('/fixture/experiments/91001/state')==before
assert http('/fixture/experiments/91002/state')['status']=='RUNNING'
assert sql("SELECT COUNT(*) FROM learning_sales_cycle_event_v1 WHERE action='MEASURE' AND operator_name<>'Marketing Hub · backend'")=='0'
check('Legado sem run → medição automática → ajuste → sucessor segregado → gates próprios → publicação simulada')


# Reproduz localmente um ciclo persistido antes da evolução do pai: a versão original fica intacta.
reset()
http('/fixture/experiments/91001/legacy-publication',{})
legacy=http(f'{API}/products/91001',brief(baseline=True))
sql(f"UPDATE learning_sales_cycle_v1 SET chain_definition_id=91001 WHERE id={legacy['id']}")
old_catalog=http(f'{API}/catalog?chainId=91001&productId=91001')
assert not old_catalog['entry']['canStartCycle'] and old_catalog['successorChainDefinitionId']==91002
current=parent_history()
position=http('/api/products/value-chain-positions/91001')
assert current['salesFlow']['currentActivityId']==position['subprocessPosition']['salesFlow']['currentActivityId']=='learningCycle'
assert position['subprocessPosition']['currentSubprocessSequenceNumber']==4
assert position['subprocessPosition']['salesFlow']['chainDefinitionId']==91001
assert 'chainId=91001' in current['salesFlow']['navigationUrl']
assert current['currentExecutionReference']=='experiment:91001'
assert [a['state'] for a in current['salesFlow']['activities']]==['HISTORICAL','NOT_APPLICABLE','COMPLETED','IN_PROGRESS']
assert all(t['flowId'] for t in current['salesFlow']['transitions'])
legacy=command(legacy,'ADJUST',dict(return_to,learning='Memória da versão anterior preservada',nextHypothesis='Entrega mais aplicável'))
successor=http(f'{API}/products/91001',brief(91002,legacy['id']))
assert successor['chainDefinitionId']==91002 and successor['previousCycleId']==legacy['id']
assert sql(f"SELECT chain_definition_id FROM learning_sales_cycle_v1 WHERE id={legacy['id']}")=='91001'
assert parent_history()['salesFlow']['experimentId']==91002
assert parent_history()['salesFlow']['activities'][2]['state']=='WAITING'
assert parent_history()['salesFlow']['activities'][0]['state']=='WAITING'
check('Ciclo em cadeia anterior → posição única 6.4 → sucessor na versão vigente sem reabrir operação histórica')

reset()
http('/fixture/experiments/91001/legacy-publication',{})
configure_measurement(netSales=2,refunds=0,deliveryVerified=False)
sales=http(f'{API}/products/91001',brief(baseline=True))
assert parent_history()['currentActivityId']=='delivery'
assert not next(c for c in sales['commands'] if c['action']=='ADJUST')['available']
command(sales,'ADJUST',dict(return_to,learning='Entrega ainda não comprovada',nextHypothesis='Corrigir entrega'),expected=409)
configure_measurement(snapshot='delivered',netSales=2,refunds=0,deliveryVerified=True)
sales=command(sales,'FIX_MEASUREMENT',dict(rootCause='Entrega pendente',correctionPlan='Comprovar entrega e reconciliar'))
assert sales['stage']=='DECISION' and parent_history()['currentActivityId']=='learningCycle'
assert parent_history()['salesFlow']['activities'][1]['objectiveAchieved']
check('Venda sem entrega bloqueia avanço; entrega comprovada e reconciliação liberam decisão')

print(json.dumps({'checks':len(checks),'passed':checks,'database':'MySQL 5.7','externalCalls':0},ensure_ascii=False),flush=True)
