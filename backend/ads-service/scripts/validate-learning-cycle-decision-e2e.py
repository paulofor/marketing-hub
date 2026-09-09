#!/usr/bin/env python3
"""Contratos da proposta com HTTP, BPM e MySQL locais; modelo e aprovações exclusivamente de teste."""
import concurrent.futures, datetime as dt, json, os, pathlib, subprocess, urllib.request, urllib.error, uuid
BASE='http://127.0.0.1:18091'
API='/api/business-process-chains/learning-cycles/v1'
INTERNAL='/api/internal/business-process-chains/learning-cycles/v1/decision/stage-executions'
PROJECT=os.environ['LEARNING_CYCLES_COMPOSE_PROJECT']
assert PROJECT.startswith('aihub-')
checks=[]
def http(path, body=None, expected=200, method=None):
    req=urllib.request.Request(BASE+path,data=None if body is None else json.dumps(body).encode(),headers={'Content-Type':'application/json'},method=method)
    try:
        with urllib.request.urlopen(req,timeout=30) as r:
            raw=r.read();status=r.status;value=json.loads(raw) if raw else None
    except urllib.error.HTTPError as e:status=e.code;value=json.loads(e.read())
    assert status in (expected if isinstance(expected,tuple) else (expected,)),(path,status,expected,value)
    return value

def create(product=91001,experiment=91001):
    http(f'/fixture/experiments/{experiment}/legacy-publication',{})
    now=dt.datetime.now(dt.timezone.utc)
    iso=lambda d:d.isoformat(timespec='seconds').replace('+00:00','Z')
    return http(f'{API}/products/{product}',dict(requestKey=str(uuid.uuid4()),chainDefinitionId=91002,experimentId=experiment,baseline=True,previousCycleId=None,productVersion='fixture-v1',hypothesis='Primeira ação útil aumenta continuidade',mainChange='Esforço inicial',successCriterion='Vendas com valor entregue',audience='Público local',offer='Oferta de teste',acquisition='Canal simulado',budgetLimitBrl=100,windowStart=iso(now-dt.timedelta(days=1)),windowEnd=iso(now+dt.timedelta(days=1)),sampleTarget=10,minimumNetSales=5,operatorName='Pessoa responsável local'))

def proposal_url(c):return f'{API}/products/{c["productId"]}/{c["id"]}/decision-proposal'
def valid(job):
    c=job['context'];target=next(t for t in c['returnTargets'] if t['activityId']=='rework')
    value={k:'Hipótese de melhoria; amostra pequena não comprova causa. Validar continuidade até compra.' for k in ('summary','rootCause','learning','nextHypothesis','evidenceLimits','correctionPlan','scaleHypothesis')}
    value.update(contractVersion='LEARNING_CYCLE_DECISION_PROPOSAL_V1',action='ADJUST',returnProcessId=target['processDefinitionId'],returnActivityId=target['activityId'],selectedAlternative=0,evidenceEventIds=[c['measurementEventId']],alternatives=[dict(option='Opção '+str(i),benefit='Valor verificável',risk='Amostra limitada',effort='Médio',salesImpact='Hipótese a medir') for i in range(3)])
    return value

def audit(job):
    schema=json.loads(pathlib.Path('experiment-strategist-worker/src/main/resources/prompts/learning-cycle/v1/decision-schema.json').read_text())
    body=dict(leaseToken=job['leaseToken'],prompt='Prompt simulado com contexto oficial '+json.dumps(job['context']),schema=schema,model='fixture-no-external-model',serviceTier='flex',serviceTierReason=None)
    http(f'{INTERNAL}/{job["proposalId"]}/request',body,method='PUT')
    http(f'{INTERNAL}/{job["proposalId"]}/request',body,method='PUT')
    changed=dict(body,prompt='Request diferente')
    http(f'{INTERNAL}/{job["proposalId"]}/request',changed,409,'PUT')

def result(job,value=None,error=None):
    body=dict(leaseToken=job['leaseToken'],rawResponse=json.dumps(value) if value is not None else None,error=error,inputTokens=500,outputTokens=300,costUsd=None)
    return body,http(f'{INTERNAL}/{job["proposalId"]}/result',body)

def command(c,p,**overrides):
    data=p['proposal'];evidence={k:data[k] for k in ('rootCause','learning','nextHypothesis','returnProcessId','returnActivityId')}
    evidence.update(decisionProposalId=p['id'],humanApproved=True)
    body=dict(requestKey=str(uuid.uuid4()),expectedRevision=c['revision'],action='ADJUST',operatorName='Revisora local',summary='Edição humana para tornar a hipótese mais clara',evidenceReference=data['evidenceReference'],evidence=evidence)
    body.update(overrides);return body

def check(name):checks.append(name);print('PASS '+name,flush=True)

http('/fixture/reset',{})
c=create();url=proposal_url(c)
assert c['stage']=='DECISION'
for _ in range(3):assert http(url)['status']=='WAITING'
http(url.replace('/products/91001/','/products/91002/'),expected=404)
assert http(url+'/audit')==[]
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool: reservations=list(pool.map(lambda _:http(INTERNAL+'/pending'),range(2)))
assert sorted(len(x) for x in reservations)==[0,1],reservations
job=next(x[0] for x in reservations if x)
assert job['context']['experimentId']==91001 and job['context']['productId']==91001
assert job['context']['measurementEventId']==c['events'][-1]['id']
check('Leitura sem efeitos; reserva concorrente única; produto e conciliação exatos')
audit(job)
http(f'{INTERNAL}/{job["proposalId"]}/result',dict(leaseToken='lease-alheia',rawResponse='{}'),409)
bad=valid(job);bad['evidenceEventIds']=[999999]
body,failed=result(job,bad)
assert failed['status']=='FAILED' and 'Fonte' in failed['error'],failed
assert http(url+'/audit')[0]['rawResponse']==body['rawResponse']
assert 'leaseToken' not in json.dumps(http(url+'/audit'))
check('Request imutável, lease, fonte inválida e resposta bruta preservada sem aprovação')
retry=dict(expectedRevision=c['revision'],previousProposalId=failed['id'])
queued=http(url+'/retry',retry)
assert queued['status']=='QUEUED'
assert http(url+'/retry',retry)['id']==queued['id']
job2=http(INTERNAL+'/pending')[0]
audit(job2)
callbacks=[dict(leaseToken=job2['leaseToken'],rawResponse=json.dumps(dict(valid(job2),summary='Proposta concorrente '+str(i))),error=None,inputTokens=500,outputTokens=300,costUsd=None) for i in range(2)]
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
    receipts=list(pool.map(lambda b:http(f'{INTERNAL}/{job2["proposalId"]}/result',b,(200,409)),callbacks))
assert sum(r.get('status')=='READY' for r in receipts)==1,receipts
assert sum(r.get('status')==409 for r in receipts)==1,receipts
winner=next(i for i,r in enumerate(receipts) if r.get('status')=='READY')
body,ready=callbacks[winner],receipts[winner]
assert ready['status']=='READY' and ready['operatorName']=='Pessoa responsável local'
assert http(f'{INTERNAL}/{job2["proposalId"]}/result',body)['status']=='READY'
http(f'{INTERNAL}/{job2["proposalId"]}/result',dict(body,rawResponse='{}'),409)
state=http(f'{API}/products/91001')[0]
assert state['revision']==c['revision'] and state['status']=='OPEN'
assert http('/fixture/experiments/91001/state')['status']=='USER_STOPPED'
check('Retentativa auditável, callback idempotente e nenhuma alteração de experimento/ciclo pela proposta')
cmd=command(c,ready)
route=f'{API}/products/91001/{c["id"]}/commands'
http(route,dict(cmd,evidence=dict(cmd['evidence'],humanApproved=False)),409)
http(route,dict(cmd,evidence=dict(cmd['evidence'],decisionProposalId=failed['id'])),409)
http(route,dict(cmd,expectedRevision=0),409)
assert http(url)['status']=='READY'
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool: decisions=list(pool.map(lambda _:http(route,cmd),range(2)))
assert all(x['status']=='ADJUSTED' for x in decisions)
closed=decisions[0]
assert len([e for e in closed['events'] if e['action']=='ADJUST'])==1
assert closed['returnActivityId']=='rework'
assert closed['events'][-1]['summary']==cmd['summary']
assert http(url)['status']=='APPROVED' and http(url)['approvedEventId']==closed['events'][-1]['id']
assert http(url)['proposal']['summary']!=cmd['summary']
assert len(http(url+'/audit'))==2
check('Aprovação humana obrigatória; replay concorrente único; original e edição final preservados')
http('/fixture/reset',{})
c=create();url=proposal_url(c);job=http(INTERNAL+'/pending')[0];audit(job)
_,failed=result(job,None,'Timeout simulado do executor')
assert failed['status']=='FAILED' and 'Timeout' in failed['error']
assert http(INTERNAL+'/pending')==[]
http('/fixture/reset',{})
c=create();url=proposal_url(c);job=http(INTERNAL+'/pending')[0];audit(job)
_,failed=result(job,['saída fora do contrato'])
assert failed['status']=='FAILED'
check('Timeout e saída não estruturada bloqueiam sem inventar sucesso nem repetir modelo automaticamente')
http('/fixture/reset',{})
c=create();url=proposal_url(c);job=http(INTERNAL+'/pending')[0];audit(job)
# Somente no MySQL local, reproduz abandono de lease e mudança de revisão sem adulterar produção.
compose=['docker','compose','-p',PROJECT,'-f','backend/ads-service/docker-compose.learning-cycles-local.yml']
def sql(query):subprocess.run(compose+['exec','-T','learning-cycles-mysql','mysql','-ucycles_local','-pcycles-local-only','learning_cycles_local','-e',query],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
sql(f"UPDATE learning_cycle_decision_proposal_v1 SET started_at=DATE_SUB(UTC_TIMESTAMP(),INTERVAL 2 HOUR) WHERE id={job['proposalId']}")
assert http(url)['status']=='EXPIRED'
retry=http(url+'/retry',dict(expectedRevision=c['revision'],previousProposalId=job['proposalId']))
http(f'{INTERNAL}/{job["proposalId"]}/result',dict(leaseToken=job['leaseToken'],rawResponse=json.dumps(valid(job))),409)
newjob=http(INTERNAL+'/pending')[0];audit(newjob)
sql(f"UPDATE learning_sales_cycle_v1 SET revision=revision+1 WHERE id={c['id']}")
_,failed=result(newjob,valid(newjob))
assert failed['status']=='STALE' and 'ciclo mudou' in failed['error']
check('Lease vencida e resposta atrasada não substituem nova tentativa nem outra revisão')
http('/fixture/reset',{})
print(json.dumps({'checks':len(checks),'status':'PASS','externalModel':'SIMULATED'},ensure_ascii=False))
