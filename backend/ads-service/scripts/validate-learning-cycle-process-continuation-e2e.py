#!/usr/bin/env python3
"""Homologa a passagem real entre processos no MySQL local, com provas sintéticas segregadas."""
import concurrent.futures
import json
import runpy
import subprocess
import urllib.request

# Reutiliza toda a regressão da decisão antes de exercitar sua nova continuação.
fixture = runpy.run_path('backend/ads-service/scripts/validate-learning-cycle-decision-e2e.py')
http, create, valid, audit, result, command, sql = (fixture[k] for k in ('http', 'create', 'valid', 'audit', 'result', 'command', 'sql'))
API, INTERNAL = fixture['API'], fixture['INTERNAL']
compose = fixture['compose']
AUTOMATION = '/api/internal/business-processes/automation/v1/stage-executions'


def reconcile(run_id):
    request = urllib.request.Request('http://127.0.0.1:18091' + AUTOMATION + f'/{run_id}/reconcile',
        data=b'{}', headers={'Content-Type': 'application/json', 'X-Process-Worker-Token': 'cycles-process-fixture-only'})
    with urllib.request.urlopen(request, timeout=45) as response:
        return json.load(response)


def rows(query):
    output = subprocess.check_output(compose + ['exec', '-T', 'learning-cycles-mysql', 'mysql',
        '-ucycles_local', '-pcycles-local-only', 'learning_cycles_local', '-N', '-B', '-e', query],
        text=True, stderr=subprocess.DEVNULL)
    return output.strip().splitlines()


def proof(process_id, experiment_id):
    # Simula apenas o callback do executor: o motor, projeção, locks e eventos são reais.
    sql(f"""INSERT INTO business_process_activity_instance
      (activity_definition_id,source_reference,occurrence_number,status,entered_at,exited_at,
       objective_achieved,objective_evidence_json,known_cost_usd,cost_coverage,evidence_quality,created_at,updated_at)
      SELECT id,'experiment:{experiment_id}',1,'COMPLETED',NOW(6),NOW(6),1,
        '{{"evidenceType":"LOCAL_SYNTHETIC_CALLBACK","testDataExcluded":true}}',0,'COMPLETE','AUTOMATIC',NOW(6),NOW(6)
      FROM business_process_activity_definition WHERE process_definition_id={process_id}""")


for product, experiment in [(91001, 91001), (91002, 91006)]:
    http('/fixture/reset', {})
    cycle = create(product, experiment)
    job = http(INTERNAL + '/pending')[0]
    audit(job)
    _, proposal = result(job, valid(job))
    approved = http(f'{API}/products/{product}/{cycle["id"]}/commands', command(cycle, proposal))
    assert approved['status'] == 'ADJUSTED'
    assert http(INTERNAL + '/pending') == []
    successor = next(c for c in http(f'{API}/products/{product}') if c['previousCycleId'] == cycle['id'])
    cycle_id, experiment_id, chain_id = successor['id'], successor['experimentId'], successor['chainDefinitionId']
    assert successor['stage'] == 'PLANNING' and successor['budgetLimitBrl'] == 0
    query = f'?chainId={chain_id}&learningCycleId={cycle_id}&sourceReference=experiment:{experiment_id}'
    root = lambda process: f'/api/business-processes/{process}/products/{product}/automation/v1'
    planning = http(root(2) + query)
    assert planning['id'] and planning['status'] == 'QUEUED', planning
    proof(2, experiment_id)
    if product == 91001:
        sql("CREATE TRIGGER reject_process_continuation BEFORE INSERT ON product_process_run_v1 FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Falha sintetica de passagem'")
        try:
            failed = reconcile(planning['id'])
            assert failed['status'] == 'WAITING_INPUT', failed
            unchanged = next(c for c in http(f'{API}/products/{product}') if c['id'] == cycle_id)
            assert unchanged['stage'] == 'PLANNING' and unchanged['revision'] == successor['revision'], unchanged
            assert rows(f'SELECT COUNT(*) FROM product_process_run_v1 WHERE learning_cycle_id={cycle_id}') == ['1']
        finally:
            sql('DROP TRIGGER reject_process_continuation')
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        completions = list(pool.map(lambda _: reconcile(planning['id']), range(2)))
    assert all(r['status'] == 'COMPLETED' for r in completions), completions
    construction = http(root(3) + query)
    assert construction['id'] and construction['status'] == 'QUEUED', construction
    current = next(c for c in http(f'{API}/products/{product}') if c['id'] == cycle_id)
    assert current['stage'] == 'ADJUSTMENT' and current['revision'] == successor['revision'] + 1, current
    paused = http(root(3) + f'/{construction["id"]}/pause', {})
    assert reconcile(construction['id'])['status'] == 'PAUSED'
    reconcile(planning['id'])
    assert http(root(3) + query)['status'] == 'PAUSED'
    http(root(3) + f'/{construction["id"]}/resume', {})
    proof(3, experiment_id)
    completed = reconcile(construction['id'])
    assert completed['status'] == 'COMPLETED', completed
    communication = http(root(4) + query)
    assert communication['id'] and communication['status'] == 'QUEUED', communication
    proof(4, experiment_id)
    assert reconcile(communication['id'])['status'] == 'COMPLETED'
    final = next(c for c in http(f'{API}/products/{product}') if c['id'] == cycle_id)
    assert final['stage'] == 'VIDEO_BRIEF' and final['budgetLimitBrl'] == 0, final
    assert final['windowStart'] is None and final['windowEnd'] is None
    assert rows(f'SELECT COUNT(*) FROM product_process_run_v1 WHERE learning_cycle_id={cycle_id}') == ['3']
    assert rows(f"SELECT COUNT(*) FROM product_process_run_event_v1 e JOIN product_process_run_v1 r ON r.id=e.run_id WHERE r.learning_cycle_id={cycle_id} AND e.event_type='CYCLE_PROCESS_CONTINUED'") == ['2']
    events = [e for e in final['events'] if e['fromStage'] in ('PLANNING', 'ADJUSTMENT')]
    assert len(events) == 2 and all(e['operatorName'] == 'Marketing Hub · continuidade entre processos' for e in events), events
    assert http(f'{API}/products/{product}/{cycle["id"]}/decision-proposal')['approvedEventId'] == approved['events'][-1]['id']
    print(f'PASS Aprovação → planejamento → construção → comunicação; replay, pausa e limites: produto {product}', flush=True)
print(json.dumps({'status': 'PASS', 'products': 2, 'physicalDatabase': 'MySQL 5.7', 'externalModel': 'SIMULATED', 'commercialSpendAuthorized': False}))
