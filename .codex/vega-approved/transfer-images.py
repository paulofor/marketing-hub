import json,pathlib,subprocess
root=pathlib.Path('.codex/vega-approved');digest=json.loads((root/'source-manifest.json').read_text())['sourceDigest']
for host,names in [('root@163.245.202.80',['customer-agent-worker','meta-ad-approver-worker']),('root@191.252.181.168',['backend','frontend']),('root@163.245.200.7',['pde-platform-frontend-vega'])]:
 images=[f'marketing-hub/{n}:vega-approved-{digest[:12]}' for n in names]
 print(json.dumps({'host':host,'state':'TRANSFERRING','images':images}),flush=True)
 save=subprocess.Popen(['docker','save',*images],stdout=subprocess.PIPE)
 zipped=subprocess.Popen(['gzip','-1'],stdin=save.stdout,stdout=subprocess.PIPE);save.stdout.close()
 load=subprocess.run(['sandbox-ssh',host,'docker','load'],stdin=zipped.stdout);zipped.stdout.close()
 codes=[load.returncode,zipped.wait(),save.wait()]
 if any(codes):raise RuntimeError(f'Transporte interrompido {host}: {codes}')
 print(json.dumps({'host':host,'state':'LOADED','images':images}),flush=True)
