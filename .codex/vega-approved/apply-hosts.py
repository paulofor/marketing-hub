import pathlib,subprocess,json,sys,time
root=pathlib.Path('.codex/vega-approved');digest=json.loads((root/'source-manifest.json').read_text())['sourceDigest']
sys.path.insert(0,str(root));from mcp import call
script=pathlib.Path('infra/testing/video-continuation/apply-image.py').read_bytes()
targets=[('root@163.245.200.7','pde-platform-frontend-vega-private'),('root@191.252.181.168','marketinghub-frontend'),('root@163.245.202.80','customer-agent-worker-customer-agent-worker-1'),('root@163.245.202.80','meta-ad-approver-worker-meta-ad-approver-worker-1'),('root@191.252.181.168','marketinghub-backend')]
for host,name in targets:
 subprocess.run(['sandbox-ssh',host,'python3','-','--container',name,'--digest',digest,'--check-only'],input=script,check=True)
query="SELECT t.id,t.status,a.nickname FROM agent_task t JOIN agent a ON a.id=t.assigned_agent_id WHERE t.status IN ('IN_PROGRESS','PENDING') AND a.agent_key IN ('customer-agent','meta-ad-approver')"
for attempt in range(120):
 result=call('db_query',{'query':query})
 if 'error' in result:raise RuntimeError('Não foi possível conferir tarefas em curso.')
 rows=result['result']['structuredContent']['rows']
 if not rows:break
 print(json.dumps({'state':'WAITING_EXISTING_TASKS','tasks':rows}),flush=True);time.sleep(15)
else:raise RuntimeError('Trabalhos existentes preservados; troca ainda não iniciada.')
for host,name in targets:
 subprocess.run(['sandbox-ssh',host,'python3','-','--container',name,'--digest',digest],input=script,check=True)
 for attempt in range(60):
  state=subprocess.check_output(['sandbox-ssh',host,'docker','inspect','--format','{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{end}}',name],text=True).strip()
  if state=='running healthy':break
  if state.startswith(('exited','dead')):raise RuntimeError(f'{name}: {state}')
  time.sleep(5)
 else:raise RuntimeError(f'Saúde não comprovada: {name}')
 print(json.dumps({'container':name,'state':'HEALTHY','digest':digest}),flush=True)
