"""Valida contexto e continuidade no backend local com MySQL 5.7 e registros sintéticos."""
import os,json,subprocess,urllib.request,urllib.error,uuid
PROJECT=os.environ['LEARNING_CYCLES_COMPOSE_PROJECT']
assert PROJECT.startswith('aihub-')
COMPOSE=['docker','compose','-p',PROJECT,'-f','backend/ads-service/docker-compose.learning-cycles-local.yml']
BASE='http://127.0.0.1:18091'

def sql(value):
 r=subprocess.run(COMPOSE+['exec','-T','learning-cycles-mysql','mysql','-uroot','-pcycles-root-local-only','--default-character-set=utf8mb4','--batch','--skip-column-names','learning_cycles_local','-e',value],capture_output=True,text=True,check=True)
 return r.stdout.strip()

def q(value):
 return "'"+str(value).replace('\\','\\\\').replace("'","''")+"'"

def get(path,expected=200):
 try:
  with urllib.request.urlopen(BASE+path,timeout=30) as r: code,data=r.status,r.read()
 except urllib.error.HTTPError as e: code,data=e.code,e.read()
 assert code==expected,(path,code,data[:1000]); return json.loads(data) if data else None

sql('UPDATE learning_sales_cycle_v1 SET current_instance_id=NULL; DELETE FROM learning_cycle_decision_proposal_v1; DELETE FROM learning_sales_cycle_event_v1; DELETE FROM learning_sales_cycle_v1 ORDER BY id DESC; DELETE FROM business_process_activity_instance;')
process=int(sql("SELECT id FROM business_process_definition WHERE process_code='value-chain-learning-sales-cycle' ORDER BY version_number DESC LIMIT 1"))
chain=int(sql("SELECT id FROM business_process_chain_definition ORDER BY version_number DESC LIMIT 1"))
cols='id,product_id,chain_definition_id,chain_code,process_definition_id,experiment_id,previous_cycle_id,request_key,creation_json,brief_json,inherited_learning_json,stage,status,product_version,budget_limit_brl,window_start,window_end,return_process_id,return_activity_id,open_slot,revision,baseline,created_at,updated_at,version_changed_at,closed_at'
brief=json.dumps({'hypothesis':'Uma microação útil aumenta a continuidade','mainChange':'Primeiro resultado aplicável','operatorName':'QA local'},ensure_ascii=False)
memory=json.dumps({'cycleId':92001,'experimentId':91001,'events':[{'action':'MEASURE','summary':'Quatro sessões, nenhum pedido: dados sintéticos','evidenceReference':'internal://qa/measurement','evidence':{}},{'action':'ADJUST','summary':'Ajuste aprovado na fixture','evidenceReference':'internal://qa/decision','evidence':{'learning':'Uso parcial não comprova conversão','rootCause':'Amostra pequena; sem causa comprovada','nextHypothesis':'Melhorar a utilidade inicial'}}]},ensure_ascii=False)
for cid,exp,prev,state,slot,inherit in [(92001,91001,'NULL','ADJUSTED','NULL','{}'),(92002,91002,'92001','OPEN','1',memory)]:
 vals=[str(cid),'91001',str(chain),q('pde-value-creation-delivery'),str(process),str(exp),prev,q(str(uuid.uuid4())),q('{}'),q(brief),q(inherit),q('ADJUSTMENT'),q(state),q('successor-v8' if prev!='NULL' else 'historical-v7'),'100','NOW()-INTERVAL 1 DAY','NOW()+INTERVAL 1 DAY','2',q('work'),slot,'0','0','NOW()-INTERVAL 1 HOUR','NOW()','NOW()-INTERVAL 1 HOUR','NULL' if state=='OPEN' else 'NOW()-INTERVAL 1 MINUTE']
 sql(f'INSERT INTO learning_sales_cycle_v1 ({cols}) VALUES ({",".join(vals)})')
# O Processo 2 desta passagem terminou. A construção ainda não começou.
def instance(process_id,activity,status='COMPLETED',source='experiment:91002',ago='30 MINUTE'):
 aid=int(sql(f'SELECT id FROM business_process_activity_definition WHERE process_definition_id={process_id} AND activity_id={q(activity)}'))
 sql(f"INSERT INTO business_process_activity_instance (activity_definition_id,source_reference,occurrence_number,status,entered_at,exited_at,objective_achieved,known_cost_usd,cost_coverage,evidence_quality,created_at,updated_at) VALUES ({aid},{q(source)},1,{q(status)},NOW()-INTERVAL {ago},NOW(),{1 if status=='COMPLETED' else 0},0,'COMPLETE','DIRECT',NOW()-INTERVAL {ago},NOW())")
instance(2,'work')
# Uma conclusão antiga do mesmo produto não conclui esta iteração.
instance(3,'rework',source='experiment:91001',ago='2 DAY')
path=f'/api/business-process-chains/learning-cycles/v1/products/91001/process-context?processDefinitionId=2&chainId={chain}'
c=get(path)
assert c['cycleId']==92002 and c['cycleNumber']==2 and c['experimentId']==91002,c
assert c['nextWork']['processNumber']==3 and c['nextWork']['activityId']=='rework',c
assert len(c['previousLearning'])==2 and c['previousLearning'][1]['limitation']=='Amostra pequena; sem causa comprovada'
assert f'learningCycleId=92002&chainId={chain}' in c['nextWork']['url']
a=get(f'/api/business-processes/3/products/91001/activity-executions?learningCycleId=92002&chainId={chain}')
assert a['completedActivityCount']==0 and a['uniqueTaskCount']==0,a
get(path.replace('products/91001','products/91002')+'&cycleId=92002',409)
get(path+'&cycleId=99999',409)
get(path.replace(f'chainId={chain}','chainId=91000')+'&cycleId=92002',409)
assert sql('SELECT COUNT(*) FROM agent_task')=='0'
instance(3,'rework','BLOCKED')
c=get(path);assert c['nextWork']['state']=='BLOCKED',c
sql("UPDATE business_process_activity_instance SET status='COMPLETED',objective_achieved=1 WHERE source_reference='experiment:91002' AND status='BLOCKED'")
c=get(path);assert c['nextWork']['activityId']=='agentValidationGate',c
# Estado que será aberto no navegador: a primeira atividade de construção aguarda execução.
sql("DELETE FROM business_process_activity_instance WHERE source_reference='experiment:91002' AND activity_definition_id IN (SELECT id FROM business_process_activity_definition WHERE process_definition_id=3)")
print(json.dumps({'checks':10,'passed':True,'database':'MySQL 5.7','externalCalls':0,'chainId':chain,'cycleId':92002,'productId':91001}))
