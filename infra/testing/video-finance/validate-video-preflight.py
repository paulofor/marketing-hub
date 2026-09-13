#!/usr/bin/env python3
"""Comprova orientação de preflight bloqueado em API e MySQL locais, sem acessar provedores."""
import datetime as dt
import json
import pathlib
import re
from validate import API, authorize, command, http, prepare, process_path, process_read, reconcile, sql, start_process


def query_contract():
    """Executa o SQL canônico no MySQL 5.7 com identidades sintéticas divergentes."""
    source = pathlib.Path('backend/ads-service/src/main/java/com/marketinghub/repository/jpa/salesvideo/VideoProductionCycleRepository.java').read_text()
    query = re.search(r'SELECT c\.\* FROM video_production_cycle c.*?LIMIT 1', source, re.S).group()
    for key, value in dict(productId='91001', experimentId='91001', productVersion="'fixture-v12'",
                           strategyRole="'CAMPAIGN_QUALIFICATION'", versionChangedAt="'2026-09-12 08:00:00'").items():
        query = query.replace(':' + key, value)
    sql('CREATE TABLE IF NOT EXISTS video_project (id BIGINT PRIMARY KEY,product_id BIGINT,experiment_id BIGINT,campaign_key VARCHAR(191),strategy_role VARCHAR(64));'
        'CREATE TABLE IF NOT EXISTS video_production_cycle (id BIGINT PRIMARY KEY,video_project_id BIGINT,product_id BIGINT,experiment_id BIGINT,created_at DATETIME,status VARCHAR(40));')
    mutations = [None, 'UPDATE video_project SET product_id=91002', 'UPDATE video_project SET experiment_id=91002',
                 'UPDATE video_production_cycle SET product_id=91002', 'UPDATE video_production_cycle SET experiment_id=91002',
                 "UPDATE video_project SET campaign_key='fixture-v11'", "UPDATE video_project SET strategy_role='PDE_HERO_CONVERSION'",
                 "UPDATE video_production_cycle SET created_at='2026-09-11 08:00:00'"]
    for mutation in mutations:
        sql("DELETE FROM video_production_cycle;DELETE FROM video_project;"
            "INSERT INTO video_project VALUES (91004,91001,91001,'fixture-v12','CAMPAIGN_QUALIFICATION');"
            "INSERT INTO video_production_cycle VALUES (91012,91004,91001,91001,'2026-09-12 08:00:01','PROVIDER_PREFLIGHT_ONLY_BLOCKED');")
        if mutation: sql(mutation)
        result = sql(query)
        assert (bool(result) if mutation is None else not result), (mutation, result)
    sql("UPDATE video_production_cycle SET created_at='2026-09-12 08:00:01';"
        "INSERT INTO video_production_cycle VALUES (91013,91004,91001,91001,'2026-09-12 08:00:02','PENDING_PROVIDER_PREFLIGHT_ONLY');")
    assert sql(query).split('\t')[0] == '91013'
    sql('DROP TABLE video_production_cycle;DROP TABLE video_project;')
    return 9


def run():
    """Demonstra espera explícita, pausa e precedência de nova tentativa no processo real."""
    sql_checks = query_contract()
    cycle = prepare()
    run = start_process(cycle)
    budget = http(f'{API}/products/91001/{cycle["id"]}/video-budget', authorize(cycle))
    cycle['revision'] = budget['revision']
    proof = {key: 'Contexto sintético segregado' for key in ['briefReference','campaignGoal','campaignCta','campaignMetric',
            'pdeGoal','pdeCta','pdeMetric','controlledVariables']}
    proof['productionBudgetReference'] = budget['currentAuthorization']['reference']
    cycle = command(cycle, proof)
    assert cycle['stage'] == 'CAMPAIGN_VIDEO'
    assert reconcile(run)['userAction'] is None
    sample = dict(id=92012,projectId=91004,productId=91001,experimentId=91001,
                  productVersion=cycle['productVersion'],strategyRole='PDE_HERO_CONVERSION',
                  createdAt=dt.datetime.now(dt.timezone.utc).isoformat(),status='PROVIDER_PREFLIGHT_ONLY_BLOCKED')
    http('/fixture/video-preflights', sample)
    assert reconcile(run)['userAction'] is None
    sample['strategyRole'] = 'CAMPAIGN_QUALIFICATION'
    http('/fixture/video-preflights', sample)
    blocked = reconcile(run)
    assert blocked['status'] == 'WAITING_HUMAN' and blocked['completedActivities'] == 0, blocked
    action = blocked['userAction']
    assert action['code'] == 'RESOLVE_VIDEO_PREFLIGHT' and action['actionUrl'] == '/audio-video-studio/projects/91004', action
    assert 'configuração' in action['reason'] and 'anúncio' in action['title']
    assert sql(f'SELECT status FROM product_process_run_v1 WHERE id={run["id"]}') == 'WAITING_HUMAN'
    assert sql(f'SELECT message FROM product_process_run_event_v1 WHERE run_id={run["id"]} ORDER BY id DESC LIMIT 1') == action['reason']
    revision = sql(f'SELECT revision FROM product_process_run_v1 WHERE id={run["id"]}')
    assert process_read(cycle)['userAction'] == action
    assert sql(f'SELECT revision FROM product_process_run_v1 WHERE id={run["id"]}') == revision
    paused = reconcile(http(process_path(cycle) + f'/{run["id"]}/pause', {}))
    assert paused['status'] == 'PAUSED' and paused['userAction'] is None
    assert reconcile(http(process_path(cycle) + f'/{run["id"]}/resume', {}))['status'] == 'WAITING_HUMAN'
    sample.update(id=92013,createdAt=dt.datetime.now(dt.timezone.utc).isoformat(),status='PENDING_PROVIDER_PREFLIGHT_ONLY')
    http('/fixture/video-preflights', sample)
    active = reconcile(run)
    assert active['status'] == 'WAITING_ACTIVITY' and active['userAction'] is None and active['completedActivities'] == 0
    sample['status'] = 'PROVIDER_PREFLIGHT_ONLY_BLOCKED'
    http('/fixture/video-preflights', sample)
    blocked = reconcile(run)
    assert '/cycles/92013/' in blocked['userAction']['evidenceReference']
    assert http('/fixture/experiments/91001/state') == dict(status='PLANNED', runCount=0, campaignCount=0)
    result = dict(sqlChecks=sql_checks,restChecks=8,externalCalls=0,cycle=cycle['id'],processUrl=process_path(cycle),action=blocked['userAction'])
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    run()
