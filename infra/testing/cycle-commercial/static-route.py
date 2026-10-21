"""Reproduz a recuperação estática pela API e pelo MySQL locais, sem modelo nem mídia real."""
import datetime as dt
import json
import uuid
from client import fixture, http, sql, API, command, read


def seed_preparation(reference):
    """Instala objetivos sintéticos explícitos dos processos delegados da fixture existente."""
    sql(f"""INSERT INTO business_process_activity_instance
      (activity_definition_id,source_reference,occurrence_number,status,entered_at,exited_at,
       objective_achieved,objective_evidence_json,known_cost_usd,cost_coverage,evidence_quality,created_at,updated_at)
      SELECT id,'{reference}',1,'COMPLETED',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),1,
       '{{"fixture":true,"evidenceType":"DELEGATED_EXECUTOR_DOUBLE"}}',0,'COMPLETE','DIRECT',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)
      FROM business_process_activity_definition WHERE process_definition_id IN (2,3,4)""")


def install_route(cycle, task):
    """Registra a decisão de formato com identidade e versão, sem tratar adiamento como dispensa."""
    reference = f'experiment:{cycle["experimentId"]}'
    http(f'/fixture/static-communication/{cycle["experimentId"]}', dict(taskId=task,
         createdAt=(dt.datetime.now(dt.timezone.utc)-dt.timedelta(seconds=5)).isoformat()))
    sql("""INSERT IGNORE INTO business_process_definition
      (id,process_code,name,purpose,owner_name,trigger_description,outcome_description,
       version_number,status,process_type,execution_scope,diagram_json,created_at)
      SELECT 91021,'creative-production-approval','Comunicação estática sintética',purpose,
       'Backend',trigger_description,outcome_description,1,'PUBLISHED',process_type,execution_scope,diagram_json,UTC_TIMESTAMP(6)
      FROM business_process_definition WHERE id=4""")
    for activity in ('route', 'audiovisual'):
        sql(f"""INSERT IGNORE INTO business_process_activity_definition
          (process_definition_id,activity_id,name,owner_name,definition_json,created_at)
          VALUES (91021,'{activity}','{activity}','Apolo','{{}}',UTC_TIMESTAMP(6))""")
        proof = dict(evidenceType='COMMUNICATION_FORMATS_V1' if activity == 'route' else 'OPTIONAL_ACTIVITY_NOT_REQUIRED_V1',
            communicationTaskId=task, sourceReference=reference, audiovisualRequired=False,
            audiovisualPlanned=False, audiovisualProductionDeferred=False, activityId=activity,
            privateCreativePreparation=dict(prototypeVersion=cycle['productVersion']))
        encoded = json.dumps(proof, ensure_ascii=False).encode().hex()
        sql(f"""INSERT INTO business_process_activity_instance
          (activity_definition_id,source_reference,occurrence_number,status,entered_at,exited_at,
           objective_achieved,objective_evidence_json,known_cost_usd,cost_coverage,evidence_quality,created_at,updated_at)
          SELECT id,'{reference}',1,'{'COMPLETED' if activity == 'route' else 'NOT_APPLICABLE'}',
           UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),{1 if activity == 'route' else 0},
           CONVERT(0x{encoded} USING utf8mb4),0,'COMPLETE','DIRECT',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)
          FROM business_process_activity_definition WHERE process_definition_id=91021 AND activity_id='{activity}'""")


def scenario(product, experiment, task):
    """Percorre briefing preso, recuperação, gate e bloqueio comercial com outra identidade."""
    http('/fixture/reset', {})
    now = dt.datetime.now(dt.timezone.utc)
    cycle = http(f'{API}/products/{product}', dict(requestKey=str(uuid.uuid4()),chainDefinitionId=91002,
        experimentId=experiment,baseline=False,productVersion=f'fixture-static-{experiment}-v14',
        hypothesis='Cartão contextual pode favorecer aplicação e continuidade',mainChange='Encerramento coerente',
        successCriterion='Compras conciliadas e contribuição positiva',audience='Público sintético segregado',
        offer='Oferta sintética',acquisition='Meta simulada',budgetLimitBrl=100,
        windowStart=(now-dt.timedelta(days=1)).isoformat(),windowEnd=(now+dt.timedelta(days=2)).isoformat(),
        sampleTarget=100,minimumNetSales=5,operatorName='Homologação estática local'))
    cycle = command(cycle,dict(learning='Sem demanda comprovada',competingExplanation='Amostra insuficiente'))
    seed_preparation(f'experiment:{experiment}')
    cycle = command(cycle,dict(planReference='internal://fixture/plan',stopRule='Teto e fim da janela'))
    cycle = command(cycle,dict(productVersion=cycle['productVersion'],changeEvidence='internal://fixture/static-version'))
    assert cycle['stage']=='VIDEO_BRIEF'
    install_route(cycle,task)
    before=read(cycle)
    assert before['stage']=='VIDEO_BRIEF'
    assert before['videoBudget'] is None
    assert read(cycle)['events']==before['events'], 'GET alterou o ciclo'
    path=f'/fixture/static-communication/{product}/{cycle["id"]}/advance'
    assert http(path,{})['advanced']
    cycle=read(cycle)
    assert cycle['stage']=='VALIDATION'
    assert cycle['events'][-1]['action']=='RESOLVE_OPTIONAL_AUDIOVISUAL'
    assert not cycle['events'][-1]['evidence']['spendAuthorized']
    # Não existe gate próprio válido: a continuidade não fabrica aprovação.
    assert not http(path,{})['advanced']
    gate=http('/fixture/approval',dict(productId=product,productVersion=cycle['productVersion']))
    assert http(path,{})['advanced']
    cycle=read(cycle)
    assert cycle['stage']=='AUTHORIZATION'
    assert cycle['events'][-1]['evidence']['approvalInstanceId']==gate['approvalInstanceId']
    assert not http(path,{})['advanced']
    assert read(cycle)['events']==cycle['events']
    assert http(f'/fixture/experiments/{experiment}/state')['status']=='PLANNED'
    assert not cycle['commercialPreparation']['readyForReview']
    assert http('/fixture/commercial-tasks')==[dict(id=task,status='COMPLETED',sourceReference=f'experiment:{experiment}',activityId='communicationContract',simulated=True)]
    return dict(productId=product,experimentId=experiment,stage=cycle['stage'],events=len(cycle['events']),
                paidCalls=0,mediaSpend=0,externalPublication=False)


if __name__ == '__main__':
    results=[scenario(91001,91001,9900786),scenario(91002,91006,9900886)]
    print(json.dumps(dict(passed=results,commercialResults='SYNTHETIC_ONLY'),ensure_ascii=False,indent=2))
