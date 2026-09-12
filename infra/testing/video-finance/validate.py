#!/usr/bin/env python3
"""Homologa o financeiro real em API/MySQL locais, sem acessar provedores ou produção."""
import concurrent.futures
import datetime as dt
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request
import uuid

API = '/api/business-process-chains/learning-cycles/v1'
BASE = 'http://127.0.0.1:18091'
PROJECT = os.environ['VIDEO_FINANCE_COMPOSE_PROJECT']


def http(path, body=None, expected=200, headers=None):
    request = urllib.request.Request(BASE + path, data=None if body is None else json.dumps(body).encode(),
                                     headers={'Content-Type': 'application/json', **(headers or {})})
    try:
        with urllib.request.urlopen(request, timeout=40) as response:
            status, result = response.status, json.load(response)
    except urllib.error.HTTPError as error:
        status, result = error.code, json.load(error)
    assert status == expected, (path, expected, status, result)
    return result


def sql(query):
    command = ['docker', 'compose', '-p', PROJECT, '-f', 'backend/ads-service/docker-compose.learning-cycles-local.yml',
               'exec', '-T', 'learning-cycles-mysql', 'mysql', '-ucycles_local', '-pcycles-local-only',
               '--default-character-set=utf8mb4', '--batch', '--skip-column-names', 'learning_cycles_local', '-e', query]
    return subprocess.run(command, text=True, capture_output=True, check=True).stdout.strip()


def command(cycle, evidence, action='COMPLETE', expected=200):
    return http(f'{API}/products/{cycle["productId"]}/{cycle["id"]}/commands', dict(
        requestKey=str(uuid.uuid4()), expectedRevision=cycle['revision'], action=action, operatorName='Homologação local',
        summary='Evidência sintética segregada', evidenceReference='internal://fixture/video-finance', evidence=evidence), expected)


def prepare():
    http('/fixture/reset', {})
    now = dt.datetime.now(dt.timezone.utc)
    cycle = http(f'{API}/products/91001', dict(requestKey=str(uuid.uuid4()), chainDefinitionId=91002, experimentId=91001,
        baseline=False, productVersion='fixture-video-finance-v1', hypothesis='Ajuste aplicável aumenta uso e vendas',
        mainChange='Demonstrar o benefício real', successCriterion='Vendas líquidas e contribuição positiva',
        audience='Público simulado', offer='Oferta sintética', acquisition='Canal de teste', budgetLimitBrl=100,
        windowStart=(now-dt.timedelta(days=1)).isoformat(), windowEnd=(now+dt.timedelta(days=1)).isoformat(),
        sampleTarget=10, minimumNetSales=5, operatorName='Homologação local'))
    cycle = command(cycle, dict(learning='Sem amostra comercial', competingExplanation='Variação aleatória'))
    cycle = command(cycle, dict(planReference='internal://fixture/plan', stopRule='Respeitar limite'))
    cycle = command(cycle, dict(productVersion=cycle['productVersion'], changeEvidence='internal://fixture/version'))
    assert cycle['stage'] == 'VIDEO_BRIEF'
    return cycle


def authorize(cycle, amount=20):
    return dict(requestKey=str(uuid.uuid4()), expectedRevision=cycle['revision'], chainDefinitionId=cycle['chainDefinitionId'],
        experimentId=cycle['experimentId'], productVersion=cycle['productVersion'], budgetLimitUsd=amount,
        operatorName='Homologação financeira local', justification='Demonstrar o primeiro ajuste e testar vendas', confirmed=True)


def process_path(cycle):
    parent = sql(f"SELECT d.id FROM business_process_chain_item i JOIN business_process_definition d ON d.id=i.process_definition_id WHERE i.chain_definition_id={cycle['chainDefinitionId']} AND d.process_code='pde-sales-delivery-learning'")
    assert parent.isdigit(), parent
    return f'/api/business-processes/{parent}/products/{cycle["productId"]}/automation/v1'


def process_read(cycle):
    return http(process_path(cycle) + f'?chainId={cycle["chainDefinitionId"]}&learningCycleId={cycle["id"]}&sourceReference=experiment:{cycle["experimentId"]}')


def reconcile(run):
    return http(f'/api/internal/business-processes/automation/v1/stage-executions/{run["id"]}/reconcile', {},
                headers={'X-Process-Worker-Token': 'cycles-process-fixture-only'})


def start_process(cycle):
    return reconcile(http(process_path(cycle), dict(chainId=cycle['chainDefinitionId'], learningCycleId=cycle['id'],
                                                    sourceReference=f'experiment:{cycle["experimentId"]}')))


def run():
    checks = []
    cycle = prepare()
    run = start_process(cycle)
    assert run['status'] == 'WAITING_HUMAN', run
    assert run['userAction']['code'] == 'AUTHORIZE_VIDEO_BUDGET'
    assert run['completedActivities'] == 0 and run['remainingActivities'] == 4
    assert sql(f'SELECT status FROM product_process_run_v1 WHERE id={run["id"]}') == 'WAITING_HUMAN'
    assert sql(f'SELECT message FROM product_process_run_event_v1 WHERE run_id={run["id"]} ORDER BY id DESC LIMIT 1') == run['userAction']['reason']
    paused = reconcile(http(process_path(cycle) + f'/{run["id"]}/pause', {}))
    assert paused['status'] == 'PAUSED' and paused['userAction'] is None, paused
    run = reconcile(http(process_path(cycle) + f'/{run["id"]}/resume', {}))
    assert run['status'] == 'WAITING_HUMAN' and run['userAction']['code'] == 'AUTHORIZE_VIDEO_BUDGET', run
    persisted_before = sql(f'SELECT revision,reason FROM product_process_run_v1 WHERE id={run["id"]}')
    assert process_read(cycle)['userAction'] == run['userAction']
    assert sql(f'SELECT revision,reason FROM product_process_run_v1 WHERE id={run["id"]}') == persisted_before
    checks.append('Processo persiste espera humana e mostra motivo e financeiro do ciclo; leitura não grava')
    path = f'{API}/products/91001/{cycle["id"]}/video-budget'
    read = path + '?chainId=91002'
    state = http('/fixture/experiments/91001/state')
    instances = sql('SELECT COUNT(*) FROM business_process_activity_instance')
    assert http(read)['currentAuthorization'] is None
    request = authorize(cycle, 20.50)
    recorded = http(path, request)
    assert recorded['currentAuthorization']['budgetLimitUsd'] == 20.50
    assert recorded['revision'] == cycle['revision'] + 1
    assert http(read) == recorded
    oriented = process_read(cycle)
    assert oriented['userAction']['code'] == 'COMPLETE_VIDEO_BRIEF'
    assert oriented['userAction']['evidenceReference'] == recorded['currentAuthorization']['reference']
    assert oriented['completedActivities'] == 0 and oriented['remainingActivities'] == 4
    assert sql(f'SELECT revision,reason FROM product_process_run_v1 WHERE id={run["id"]}') == persisted_before
    reconciled = reconcile(run)
    assert reconciled['userAction']['code'] == 'COMPLETE_VIDEO_BRIEF'
    assert sql(f'SELECT message FROM product_process_run_event_v1 WHERE run_id={run["id"]} ORDER BY id DESC LIMIT 1') == reconciled['userAction']['reason']
    checks.append('Teto atualiza próxima ação e diário sem concluir objetivo ou repetir tarefa')
    assert http(path, request) == recorded
    assert sql(f'SELECT COUNT(*) FROM learning_sales_cycle_event_v1 WHERE cycle_id={cycle["id"]} AND action="AUTHORIZE_VIDEO_BUDGET"') == '1'
    checks.append('Gravação, recarga, recibo e repetição idempotente com MySQL real')
    http(path, dict(request, budgetLimitUsd=21), 409)
    http(path, dict(request, requestKey=str(uuid.uuid4())), 409)
    for field, value in [('chainDefinitionId', 91001), ('experimentId', 91002), ('productVersion', 'outra-versao')]:
        http(path, dict(request, requestKey=str(uuid.uuid4()), expectedRevision=recorded['revision'], **{field: value}), 409)
    http(path.replace('/products/91001/', '/products/91002/'), dict(request, requestKey=str(uuid.uuid4())), 404)
    http(path + '?chainId=91001', expected=409)
    for amount in [0, -1, 1.001, 1000000]:
        http(path, dict(request, requestKey=str(uuid.uuid4()), expectedRevision=recorded['revision'], budgetLimitUsd=amount), 400)
    checks.append('Escopo, produto, cadeia, versão, revisão e centavos recusam dados incorretos')
    cycle['revision'] = recorded['revision']
    left, right = authorize(cycle, 25), authorize(cycle, 30)
    def competing(body):
        try:
            return http(path, body)
        except AssertionError as error:
            assert error.args[0][2] == 409, error
            return None
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        results = list(pool.map(competing, [left, right]))
    assert sum(result is not None for result in results) == 1
    updated = http(read)
    assert len(updated['history']) == 2 and sum(item['current'] for item in updated['history']) == 1
    assert updated['revision'] == recorded['revision'] + 1
    checks.append('Duas autorizações concorrentes resultam em um novo recibo e um conflito')
    assert sql('SELECT COUNT(*) FROM business_process_activity_instance') == instances
    assert http('/fixture/experiments/91001/state') == state == dict(status='PLANNED', runCount=0, campaignCount=0)
    assert sql(f'SELECT stage FROM learning_sales_cycle_v1 WHERE id={cycle["id"]}') == 'VIDEO_BRIEF'
    persisted = json.loads(sql(f'SELECT evidence_json FROM learning_sales_cycle_event_v1 WHERE id={updated["currentAuthorization"]["eventId"]}'))
    assert all(persisted[key] is False for key in ['mediaAuthorized', 'billingAuthorized', 'commercialPublicationAuthorized', 'financialReviewApproved'])
    assert persisted['videoRoles'] == ['AD', 'LANDING_HERO']
    checks.append('Sem avanço do BPM, tarefa paga, campanha, cobrança ou publicação; escopo auditado')
    cycle = next(c for c in http(f'{API}/products/91001') if c['id'] == cycle['id'])
    assert cycle['videoBudget']['reference'] == updated['currentAuthorization']['reference']
    assert any(link['url'] == updated['financeUrl'] for link in cycle['workLinks'])
    proof = {field: 'Briefing sintético: ' + field for field in ['briefReference', 'campaignGoal', 'campaignCta', 'campaignMetric',
             'pdeGoal', 'pdeCta', 'pdeMetric', 'controlledVariables', 'productionBudgetReference']}
    command(cycle, proof, expected=409)
    proof['productionBudgetReference'] = cycle['videoBudget']['reference']
    advanced = command(cycle, proof)
    assert advanced['stage'] == 'CAMPAIGN_VIDEO'
    assert reconcile(run)['userAction'] is None
    assert advanced['events'][-1]['evidence']['productionBudget']['budgetLimitUsd'] == updated['currentAuthorization']['budgetLimitUsd']
    assert not http(read)['canAuthorize']
    http(path, authorize(advanced, 40), 409)
    checks.append('Briefing recebe referência e teto oficiais; etapa seguinte preserva consulta')
    catalog = http(f'{API}/catalog?chainId=91002&productId=91001&cycleId={cycle["id"]}')
    target = next(t for t in catalog['returnTargets'] if t['activityId'] == 'rework' and t['processCode'] == 'pde-construction-approval')
    advanced = command(advanced, dict(productVersion='fixture-video-finance-v2', rootCause='Correção técnica sintética',
        returnProcessId=target['processDefinitionId'], returnActivityId=target['activityId']), 'REWORK')
    assert http(read)['currentAuthorization'] is None and len(http(read)['history']) == 2
    command(advanced, {}, 'STOP')
    closed = http(read)
    assert not closed['canAuthorize'] and 'encerrado' in closed['blocker']
    checks.append('Versão nova não herda teto; encerramento mantém histórico imutável')
    print(json.dumps({'checks': checks, 'passed': len(checks), 'externalCalls': 0}, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    if '--prepare-process' in sys.argv:
        cycle = prepare()
        cycle['automation'] = start_process(cycle)
        print(json.dumps(cycle))
    elif '--prepare' in sys.argv:
        print(json.dumps(prepare()))
    else:
        run()
