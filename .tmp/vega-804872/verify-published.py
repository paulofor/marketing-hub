import urllib.request,json,pathlib,datetime
base='http://191.252.181.168'
headers={'X-Tenant-ID':'default','X-User-Email':'time@marketinghub.io'}
def get(url):
 return json.load(urllib.request.urlopen(urllib.request.Request(url,headers=headers),timeout=45))
r={'observedAt':datetime.datetime.now(datetime.timezone.utc).isoformat()}
r['backendHealth']=get(base+'/ops-mh-observability-v2/health')
r['frontendHealth']=get(base+':5173/healthz')
a=get(base+'/api/business-processes/121/products/4/activity-executions?learningCycleId=18&chainId=27&sourceReference=experiment%3A111&includePromptAudit=false')
audio=next(a for a in a['activities'] if a['activityId']=='audiovisual')
r['historicalAudiovisual']={k:audio.get(k) for k in ['activityInstanceId','operationalState','stateReason','objectiveAchieved','taskCount']}
assert audio['activityInstanceId']==817 and 'pedido governado' in audio['stateReason'] and not audio['objectiveAchieved']
p=get(base+'/api/sales-videos/projects/14')
r['project']={k:p.get(k) for k in ['id','productId','experimentId','campaignKey','salesVideoProfileId','status','targetDurationSeconds']}
assert p['productId']==4 and p['experimentId']==114 and p['salesVideoProfileId']==69
r['productionCycles']=get(base+'/api/sales-videos/projects/14/autonomy/v1/cycles')
r['jobs']=get(base+'/api/sales-videos/profiles/69/jobs')
f=get(base+'/api/business-process-chains/learning-cycles/v1/products/4/21/financial-review?chainId=27')
r['financial']={k:f.get(k) for k in ['cycleId','revision','authorization','videoBudgetUsd','knownPreparationUsd','unreportedTasks']}
pathlib.Path('.tmp/vega-804872/published-verification.json').write_text(json.dumps(r,ensure_ascii=False,indent=2));print(json.dumps(r,ensure_ascii=False))
