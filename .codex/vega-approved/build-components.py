import json,pathlib,subprocess,sys
root=pathlib.Path('.codex/vega-approved')
m=json.loads((root/'source-manifest.json').read_text());d=m['sourceDigest']
components={
'backend':('backend/ads-service/Dockerfile','.'),
'frontend':('frontend/Dockerfile','.'),
'customer-agent-worker':('customer-agent-worker/Dockerfile','customer-agent-worker'),
'meta-ad-approver-worker':('meta-ad-approver-worker/Dockerfile','meta-ad-approver-worker'),
'pde-platform-frontend-vega':('pde-platform/frontend/Dockerfile.vega-private','pde-platform/frontend')}
for name in sys.argv[1:]:
 dockerfile,context=components[name]
 cmd=['docker','build','--label','com.marketinghub.source-digest='+d,'--label','org.opencontainers.image.revision='+m['baseCommit'],'-f',dockerfile,'-t','marketing-hub/'+name+':vega-approved-'+d[:12],context]
 with (root/('build-'+name+'.log')).open('w') as log:subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,check=True)
 print('BUILT',name,flush=True)
