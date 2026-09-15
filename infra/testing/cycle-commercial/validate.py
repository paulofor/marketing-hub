#!/usr/bin/env python3
"""Reproduz espera, preparação, revisões e autorização usando API/MySQL reais e fontes sintéticas."""
import concurrent.futures
import json
import uuid
from client import *

checks = []


def check(text):
    checks.append(text)
    print('PASS ' + text, flush=True)


cycle = prepare()
original_events = cycle['events']
parent = start(cycle)
assert parent['status'] == 'WAITING_INPUT' and parent['userAction']['code'] == 'PREPARE_CYCLE_COMMERCIAL', parent
assert parent['completedActivities'] == 0 and parent['costCoverage'] == 'NO_EXECUTIONS'
assert f'cycleId={cycle["id"]}' in parent['userAction']['actionUrl']
stored_run = sql(f'SELECT revision,reason FROM product_process_run_v1 WHERE id={parent["id"]}')
assert fixture.process_read(cycle)['userAction'] == parent['userAction']
assert sql(f'SELECT revision,reason FROM product_process_run_v1 WHERE id={parent["id"]}') == stored_run
assert not next(c for c in cycle['commands'] if c['action'] == 'COMPLETE')['available']
assert cycle['nextAction'] == cycle['commercialPreparation']['guidance']
budget_before = http('/fixture/experiments/91001/budget-state')
command(cycle, body=authorize(cycle), expected=409)
assert read(cycle)['events'] == original_events
assert http('/fixture/experiments/91001/budget-state') == budget_before
activation = start(cycle, 'pde-commercial-homologation-activation')
assert activation['status'] == 'WAITING_INPUT' and 'Checkout' in activation['reason'], activation
assert http('/fixture/commercial-tasks') == []
check('Pendências canônicas impedem autorização e revisores; GET não grava e custo ausente não vira zero completo')

paused = fixture.reconcile(http(process_path(cycle) + f'/{parent["id"]}/pause', {}))
assert paused['status'] == 'PAUSED' and paused['userAction'] is None, paused
parent = fixture.reconcile(http(process_path(cycle) + f'/{parent["id"]}/resume', {}))
assert parent['status'] == 'WAITING_INPUT'
http('/fixture/commercial-preparation/91001', [])
cycle = read(cycle)
parent = fixture.reconcile(parent)
assert cycle['commercialPreparation']['readyForReview'] and parent['status'] == 'WAITING_HUMAN'
assert parent['userAction']['code'] == 'AUTHORIZE_CYCLE_MEDIA'
assert read(cycle)['events'] == original_events
check('Preparação validada muda orientação sem autoaprovar; pausa e retomada conservam contexto')

body = authorize(cycle)
for changes in (dict(confirmed=False), dict(productVersion='fixture-outra-versao'), dict(budgetLimitBrl=101)):
    bad = dict(body, requestKey=str(uuid.uuid4()), evidence=dict(body['evidence'], **changes))
    command(cycle, body=bad, expected=409)
command(cycle, body=dict(body, expectedRevision=cycle['revision']-1), expected=409)
http(f'{API}/products/91002/{cycle["id"]}/commands', body, 404)
assert http('/fixture/experiments/91001/budget-state') == budget_before

def concurrent_authorization(key):
    try:
        return command(cycle, body=dict(body, requestKey=key))
    except AssertionError as error:
        assert error.args[0][2] == 409, error
        return None

keys = [str(uuid.uuid4()), str(uuid.uuid4())]
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
    results = list(pool.map(concurrent_authorization, keys))
assert sum(result is not None for result in results) == 1
winner = next(i for i, value in enumerate(results) if value is not None)
cycle = results[winner]
assert command(cycle, body=dict(body, requestKey=keys[winner]))['id'] == cycle['id']
assert cycle['stage'] == 'PUBLICATION'
assert len([event for event in cycle['events'] if event['fromStage'] == 'AUTHORIZATION' and event['action'] == 'COMPLETE']) == 1
assert http('/fixture/experiments/91001/state')['status'] == 'PLANNED'
check('Aceite, identidade, revisão, concorrência e idempotência preservam orçamento atômico sem publicar')

# A dependência comercial pode seguir, mas continua sujeita aos gates e à fila de tarefas reais.
activation = fixture.reconcile(activation)
assert activation['status'] == 'WAITING_ACTIVITY', activation
tasks = http('/fixture/commercial-tasks')
assert len(tasks) == 1 and tasks[0]['sourceReference'] == 'experiment:91001', tasks
fixture.reconcile(activation)
assert http('/fixture/commercial-tasks') == tasks
pausing = http(process_path(cycle, 'pde-commercial-homologation-activation') + f'/{activation["id"]}/pause', {})
assert fixture.reconcile(pausing)['status'] == 'PAUSING'
http(f'/fixture/commercial-tasks/{tasks[0]["id"]}/complete', {})
assert fixture.reconcile(pausing)['status'] == 'PAUSED'
activation = fixture.reconcile(http(process_path(cycle, 'pde-commercial-homologation-activation') + f'/{activation["id"]}/resume', {}))
for _ in range(8):
    for task in http('/fixture/commercial-tasks'):
        if task['status'] == 'PENDING':
            http(f'/fixture/commercial-tasks/{task["id"]}/complete', {})
    if len(http('/fixture/commercial-tasks')) == 2 and all(task['status'] == 'COMPLETED' for task in http('/fixture/commercial-tasks')):
        # Callback técnico sintético explicitamente separado das revisões e da aprovação humana.
        sql(f"INSERT INTO business_process_activity_instance (activity_definition_id,source_reference,occurrence_number,status,entered_at,exited_at,objective_achieved,objective_evidence_json,known_cost_usd,cost_coverage,evidence_quality,created_at,updated_at) SELECT id,'experiment:91001',1,'COMPLETED',UTC_TIMESTAMP(),UTC_TIMESTAMP(),1,'{{\"fixture\":true,\"source\":\"technical-preflight-double\"}}',0,'COMPLETE','DIRECT',UTC_TIMESTAMP(),UTC_TIMESTAMP() FROM business_process_activity_definition WHERE process_definition_id=5 AND activity_id='preflight' ON DUPLICATE KEY UPDATE objective_achieved=1")
    activation = fixture.reconcile(activation)
    if activation['status'] == 'WAITING_HUMAN':
        break
assert activation['status'] == 'WAITING_HUMAN', activation
assert len(http('/fixture/commercial-tasks')) == 2
assert activation['knownCostUsd'] == .24, activation
assert read(cycle)['stage'] == 'PUBLICATION'
assert http('/fixture/experiments/91001/state')['status'] == 'PLANNED'
unrelated = start(cycle, 'pde-commercial-plan-offer')
assert unrelated['status'] == 'QUEUED', unrelated
check('Fila circular corrigida; dois revisores simulados concluem, custo preservado e aprovação final continua humana')

# As demais transições e conciliações completas são cobertas também pela matriz REST principal.
command(cycle, expected=409)
http('/fixture/experiments/91001/publish', {})
cycle = command(cycle)
assert cycle['stage'] == 'DECISION'
assert cycle['events'][-1]['action'] == 'MEASURE' and cycle['events'][-1]['evidence']['netSales'] == 0
assert not next(c for c in cycle['commands'] if c['action'] == 'SCALE')['available']
assert all(event in cycle['events'] for event in original_events)
check('Somente recibo externo simulado permite medir; zero vendas não libera escala nem fabrica entrega')

other = prepare(experiment=91002, missing=False)
assert other['commercialPreparation']['readyForReview']
assert '91002' in other['authorizationReview']['summary']
command(other, body=authorize(other))
assert http('/fixture/experiments/91001/budget-state')['mediaSpendLimit'] is None
check('Outra identidade válida usa o mesmo contrato sem herdar orçamento, evidências ou tarefas')

expired = prepare(missing=False)
sql(f"UPDATE learning_sales_cycle_v1 SET window_start=DATE_SUB(UTC_TIMESTAMP(),INTERVAL 2 DAY),window_end=DATE_SUB(UTC_TIMESTAMP(),INTERVAL 1 DAY) WHERE id={int(expired['id'])}")
expired = read(expired)
assert not next(c for c in expired['commands'] if c['action'] == 'COMPLETE')['available']
command(expired, body=authorize(expired), expected=409)
assert http('/fixture/experiments/91001/budget-state')['mediaSpendLimit'] is None
check('Janela expirada bloqueia pela mesma regra na leitura e no comando, sem estender prazo automaticamente')

print(json.dumps(dict(passed=len(checks), checks=checks, externalCalls=0, commercialResults='SYNTHETIC_ONLY'), ensure_ascii=False, indent=2))
