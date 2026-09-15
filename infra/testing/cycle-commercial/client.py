"""Entradas sintéticas para controller, ciclo e automação reais na sandbox."""
import datetime as dt
import importlib.util
import json
import os
import pathlib
import re
import uuid

os.environ['VIDEO_FINANCE_COMPOSE_PROJECT'] = os.environ['LEARNING_CYCLES_COMPOSE_PROJECT']
spec = importlib.util.spec_from_file_location('finance_fixture', pathlib.Path(__file__).parents[1] / 'video-finance/validate.py')
fixture = importlib.util.module_from_spec(spec)
spec.loader.exec_module(fixture)
http, sql, API = fixture.http, fixture.sql, fixture.API


def commercial_definition():
    source = pathlib.Path('backend/ads-service/src/main/resources/db/changelog/changesets/2026-08-28-agent-responsibility-matrix-v3.yaml').read_text()
    match = re.search(r"SELECT 'pde-commercial-homologation-activation'.*?'(\{\"nodes\":.*?\})'", source, re.S)
    diagram = json.loads(match.group(1))
    assert [n['id'] for n in diagram['nodes'] if n['type'] == 'TASK'] == ['humanExperienceReview', 'commercialIntegrityReview', 'preflight', 'authorization']
    def encoded(text):
        return "CONVERT(0x" + str(text).encode().hex() + " USING utf8mb4)"
    sql(f"UPDATE business_process_definition SET diagram_json={encoded(json.dumps(diagram, ensure_ascii=False))} WHERE id=5 AND process_code='pde-commercial-homologation-activation' AND version_number=6")
    sql("DELETE FROM business_process_activity_definition WHERE process_definition_id=5 AND activity_id='work'")
    for node in diagram['nodes']:
        if node['type'] != 'TASK':
            continue
        values = [encoded(node[key]) for key in ('id', 'label', 'description', 'owner')]
        sub = encoded(node['subprocessCode']) if 'subprocessCode' in node else 'NULL'
        sql(f"INSERT INTO business_process_activity_definition (process_definition_id,activity_id,name,objective,owner_name,subprocess_code,definition_json,created_at) VALUES (5,{','.join(values)},{sub},{encoded(json.dumps(node, ensure_ascii=False))},UTC_TIMESTAMP()) ON DUPLICATE KEY UPDATE definition_json=VALUES(definition_json)")


def request(cycle, evidence=None):
    return dict(requestKey=str(uuid.uuid4()), expectedRevision=cycle['revision'], action='COMPLETE',
                operatorName='Homologação comercial local', summary='Evidência sintética segregada',
                evidenceReference='internal://fixture/cycle-commercial', evidence=evidence or {})


def command(cycle, evidence=None, expected=200, body=None):
    return http(f'{API}/products/{cycle["productId"]}/{cycle["id"]}/commands', body or request(cycle, evidence), expected)


def read(cycle):
    return next(item for item in http(f'{API}/products/{cycle["productId"]}?chainId={cycle["chainDefinitionId"]}') if item['id'] == cycle['id'])


def prepare(experiment=91001, reset=True, missing=True):
    if reset:
        http('/fixture/reset', {})
    commercial_definition()
    now = dt.datetime.now(dt.timezone.utc)
    cycle = http(f'{API}/products/91001', dict(requestKey=str(uuid.uuid4()), chainDefinitionId=91002,
        experimentId=experiment, baseline=False, productVersion=f'fixture-commercial-{experiment}-v12',
        hypothesis='Primeiro ajuste aplicável aumenta utilidade e compras', mainChange='Explicar aplicação e retomada',
        successCriterion='Vendas conciliadas, uso, satisfação e contribuição positiva', audience='Público sintético',
        offer='Oferta sintética segregada', acquisition='Meta simulada', budgetLimitBrl=100,
        windowStart=(now-dt.timedelta(days=1)).isoformat(), windowEnd=(now+dt.timedelta(days=1)).isoformat(),
        sampleTarget=100, minimumNetSales=5, operatorName='Homologação local'))
    cycle = command(cycle, dict(learning='Uso parcial não comprova venda', competingExplanation='Amostra pequena'))
    cycle = command(cycle, dict(planReference='internal://fixture/plan', stopRule='Parar no teto ou fim da janela'))
    cycle = command(cycle, dict(productVersion=cycle['productVersion'], changeEvidence='internal://fixture/release'))
    refs = http('/fixture/videos', dict(experimentId=experiment, productVersion=cycle['productVersion']))
    cycle = command(cycle, {key: 'Briefing sintético ' + key for key in ('briefReference', 'campaignGoal', 'campaignCta', 'campaignMetric', 'pdeGoal', 'pdeCta', 'pdeMetric', 'controlledVariables', 'productionBudgetReference')})
    cycle = command(cycle, dict(campaignVideoAssetId=refs['campaignVideoAssetId'], productionEvidence='Produção simulada'))
    cycle = command(cycle, dict(pdeVideoAssetId=refs['pdeVideoAssetId'], productionEvidence='Produção simulada'))
    cycle = command(cycle, dict(creativeId=refs['creativeId'], pdeSlotId=refs['pdeSlotId'], technicalEvidence='Reprodução sintética validada', customerReviewEvidence='Revisão sintética independente', captionsVerified=True, mobileVerified=True, optionalPlaybackVerified=True, testDataExcluded=True))
    gate = http('/fixture/approval', dict(productId=cycle['productId'], productVersion=cycle['productVersion']))
    cycle = command(cycle, dict(approvalInstanceId=gate['approvalInstanceId'], journeyEvidence='internal://fixture/journey', humanObservationEvidence='internal://fixture/consent', instrumentationVerified=True))
    assert cycle['stage'] == 'AUTHORIZATION'
    if missing:
        http(f'/fixture/commercial-preparation/{experiment}', ['CREATIVE_APPROVED', 'CHECKOUT_READY', 'TARGETING_READY'])
    return read(cycle)


def process_path(cycle, code='pde-sales-delivery-learning'):
    assert code in ('pde-sales-delivery-learning', 'pde-commercial-homologation-activation', 'pde-commercial-plan-offer')
    process = sql(f"SELECT d.id FROM business_process_chain_item i JOIN business_process_definition d ON d.id=i.process_definition_id WHERE i.chain_definition_id={int(cycle['chainDefinitionId'])} AND d.process_code='{code}'")
    assert process.isdigit(), process
    return f'/api/business-processes/{process}/products/{cycle["productId"]}/automation/v1'


def start(cycle, code='pde-sales-delivery-learning'):
    run = http(process_path(cycle, code), dict(chainId=cycle['chainDefinitionId'], learningCycleId=cycle['id'], sourceReference=f'experiment:{cycle["experimentId"]}'))
    return fixture.reconcile(run)


def authorize(cycle):
    body = request(cycle, dict(confirmed=True, productVersion=cycle['productVersion'], budgetLimitBrl=cycle['budgetLimitBrl']))
    body.update(summary=cycle['authorizationReview']['summary'], evidenceReference=cycle['authorizationReview']['evidenceReference'])
    return body


if __name__ == '__main__':
    cycle = prepare()
    run = start(cycle)
    print(json.dumps(dict(cycle=cycle, run=run, processPath=process_path(cycle)), ensure_ascii=False))
