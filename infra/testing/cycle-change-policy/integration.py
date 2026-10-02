"""Valida o contrato em MySQL local com a fixture LearningCycleLocalApplication."""
import sys
from policy import revise
import json,urllib.request,urllib.error,uuid,datetime as dt,pathlib
B='http://127.0.0.1:18091'
A='/api/business-process-chains/learning-cycles/v1'
def req(path, body=None, expected=200, method=None):
 r=urllib.request.Request(B+path,data=json.dumps(body).encode() if body is not None else None,headers={'Content-Type':'application/json'},method=method)
 try:
  with urllib.request.urlopen(r,timeout=40) as response: status=response.status; data=json.load(response)
 except urllib.error.HTTPError as e: status=e.code;data=json.load(e)
 assert status==expected,(path,status,expected,data)
 return data
req('/fixture/reset',{})
source=req('/api/business-process-chains/91002')
chain=next(c for c in req('/api/business-process-chains') if c['chainCode']==source['chainCode'])
cat=req(A+'/catalog?chainId='+str(chain['id'])+'&productId=91001')
p=req('/api/business-processes/'+str(cat['processDefinitionId']))
original=p.copy()
if p['diagram'].get('experimentChangePolicy') != 'CHANGE_PER_CYCLE_V1':
 p=revise(p)
 new=req('/api/business-processes',p,201)
 req('/api/business-processes/'+str(new['id'])+'/publish',{})
now=dt.datetime.now(dt.timezone.utc)
body=dict(requestKey=str(uuid.uuid4()),chainDefinitionId=chain['id'],experimentId=91001,baseline=False,productVersion='local-candidate',hypothesis='Criativo alternativo facilita entendimento',mainChange='Criativo',successCriterion='Compra e contribuição por experimento',audience='Público sintético',offer='Oferta local',acquisition='Simulação',budgetLimitBrl=100,windowStart=now.isoformat(),windowEnd=(now+dt.timedelta(days=1)).isoformat(),sampleTarget=10,minimumNetSales=5,operatorName='QA local')
c=req(A+'/products/91001',body)
print('created',c['id'],c['stage'],[x['action'] for x in c['commands']])
def cmd(action,data,status=200,key=None,revision=None):
 return req(A+f'/products/91001/{c["id"]}/commands',dict(requestKey=key or str(uuid.uuid4()),expectedRevision=c['revision'] if revision is None else revision,action=action,operatorName='QA local',summary='Novo criativo, sem resultados comerciais',evidenceReference='internal://local-policy',evidence=data),status)
cmd('REWORK',{'productVersion':'changed','technicalOnly':True},409)
cmd('AUTHORIZE_SCALE',{'budgetLimitBrl':999,'confirmed':True},409)
target=next(x for x in cat['returnTargets'] if x['processCode']=='pde-communication-sales-journey')
evidence=dict(rootCause='Comparação pede peça diferente',learning='Ainda sem exposição; hipótese de comunicação',nextHypothesis='Demonstrar entrega facilita entendimento',returnProcessId=target['processDefinitionId'],returnActivityId=target['activityId'])
req('/fixture/experiments/91001/pending-publication',{'pending':True})
cmd('ADJUST',evidence,409)
req('/fixture/experiments/91001/pending-publication',{'pending':False})
key=str(uuid.uuid4()); revision=c['revision'];c=cmd('ADJUST',evidence,key=key)
assert c['status']=='ADJUSTED' and c['productVersion']=='local-candidate' and c['experimentId']==91001 and c['canCreateSuccessor']
assert all(e['action']!='MEASURE' for e in c['events'])
replay=cmd('ADJUST',evidence,key=key,revision=revision);assert len(replay['events'])==len(c['events'])
body.update(requestKey=str(uuid.uuid4()),previousCycleId=c['id'])
req(A+'/products/91001',body,409)
body.update(requestKey=str(uuid.uuid4()),experimentId=91002)
succ=req(A+'/products/91001',body)
assert succ['previousCycleId']==c['id'] and succ['experimentId']==91002
assert succ['inheritedLearning']['experimentId']==91001
assert not any(e['action']=='MEASURE' for e in succ['events'])
pathlib.Path('.tmp/cycle-change/local-cycle-proof.json').write_text(json.dumps({'predecessor':c,'successor':succ},ensure_ascii=False,indent=2))
print('PASS blocked direct bypass, preserved source, isolated successor, replay, no fake metrics',c['id'],succ['id'])

# A exposição real da fixture impede fingir que a mudança continua em preparação.
c=succ
req('/fixture/experiments/91002/publish',{})
cmd('ADJUST',evidence,409)
req('/fixture/experiments/91002/stop',{})
cmd('ADJUST',evidence,409)
req('/fixture/experiments/91002/plan-again',{})
cmd('ADJUST',evidence,409)
assert req('/fixture/experiments/91001/state')['runCount']==0
print('PASS exposição com status divergente exige conciliação; fonte anterior intacta')
