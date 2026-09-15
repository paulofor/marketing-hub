#!/usr/bin/env python3
"""Integra aprovações com MySQL e experiência reais, mantendo provedores e dinheiro simulados."""
import concurrent.futures
import datetime as dt
import hashlib
import importlib.util
import json
from pathlib import Path

spec=importlib.util.spec_from_file_location('finance','infra/testing/video-finance/validate.py')
v=importlib.util.module_from_spec(spec)
spec.loader.exec_module(v)
out=Path('.codex/vega-approved')
c=v.prepare()
catalog=v.http(v.API+f'/catalog?chainId={c["chainDefinitionId"]}&productId={c["productId"]}&cycleId={c["id"]}')
target=next(t for t in catalog['returnTargets'] if t['processCode']=='pde-construction-approval')
version='musa-pde-entry-v12-primeiro-ajuste-aplicavel'
private=dict(prototypeVersion=version,privateAccessUrl='https://localhost:18443/vega-private',image='local:repository-built',evidenceReference='internal://fixture/private-homologation',observedAt=dt.datetime.now(dt.timezone.utc).isoformat(),**{k:True for k in ['desktopValidated','mobileValidated','firstResultValidated','resumeValidated','failuresValidated','testDataExcluded','noExternalSideEffects']})
c=v.command(c,dict(productVersion=version,rootCause='Testar vínculo privado sem exigir publicação',returnProcessId=target['processDefinitionId'],returnActivityId=target['activityId'],privatePrototype=private),'REWORK')
c=v.command(c,dict(productVersion=version,changeEvidence='internal://fixture/version'))
budget=v.http(v.API+f'/products/{c["productId"]}/{c["id"]}/video-budget',v.authorize(c))
c=v.http(v.API+f'/products/{c["productId"]}?chainId={c["chainDefinitionId"]}')[0]
c=v.command(c,dict(briefReference='internal://fixture/brief',campaignGoal='Aplicar primeiro ajuste',campaignCta='Ver primeiro ajuste',campaignMetric='Sessões qualificadas',pdeGoal='Entender como aplicar',pdeCta='Começar',pdeMetric='Primeiros resultados',controlledVariables='Mesma hipótese e produto',productionBudgetReference=budget['currentAuthorization']['reference']))
assets=v.http('/fixture/videos',dict(experimentId=c['experimentId'],productVersion=version))
medias={}
for key,file,asset_id in [('campaignVideo','ad',assets['campaignVideoAssetId']),('heroVideo','hero',assets['pdeVideoAssetId'])]:
    sha=hashlib.sha256((out/'media'/f'{file}.mp4').read_bytes()).hexdigest()
    medias[key]=dict(assetId=asset_id,assetUrl=f'https://localhost:18443/media/{file}.mp4',hlsPlaybackUrl=f'https://localhost:18443/media/{file}.m3u8',durationSeconds=3,
        metadata={'hls_delivery':{'sourceSha256':sha},'captions':{'burned_in':True,'text':'Demonstração sintética sem valor comercial.'}})
v.http('/fixture/video-continuation/media',medias)
c=v.command(c,dict(campaignVideoAssetId=assets['campaignVideoAssetId'],productionEvidence='internal://fixture/ad'))
c=v.command(c,dict(pdeVideoAssetId=assets['pdeVideoAssetId'],productionEvidence='internal://fixture/hero'))
assert c['stage']=='VIDEO_APPROVAL' and c['automaticContinuation']
before=len(c['events'])
with concurrent.futures.ThreadPoolExecutor(max_workers=5) as pool:
    list(pool.map(lambda _:v.http(f'/fixture/video-continuation/{c["productId"]}/{c["id"]}/integrate',{}),range(5)))
c=v.http(v.API+f'/products/{c["productId"]}?chainId={c["chainDefinitionId"]}')[0]
assert c['stage']=='VALIDATION' and c['automaticContinuation'] and len(c['events'])==before+1
event=c['events'][-1]
assert event['fromStage']=='VIDEO_APPROVAL' and event['evidence']['humanApprovalReused']
assert event['evidence']['destinationUrl']==private['privateAccessUrl']
assert not event['evidence']['publicationAuthorized'] and not event['evidence']['paymentEnabled']
assert v.sql(f'SELECT evidence_quality FROM business_process_activity_instance WHERE id=(SELECT current_instance_id FROM learning_sales_cycle_v1 WHERE id={c["id"]})')=='NOT_RECORDED'
assert int(v.sql(f"SELECT count(*) FROM learning_sales_cycle_event_v1 WHERE cycle_id={c['id']} AND from_stage='VIDEO_APPROVAL' AND action='COMPLETE'"))==1
proof=event['evidence']
out.joinpath('local-cycle.json').write_text(json.dumps(c))
out.joinpath('harness-input.json').write_text(json.dumps(dict(mode='TECHNICAL',captureSessionId='video-continuation-local',sourceUrl=private['privateAccessUrl'],sourceReference=f'experiment:{c["experimentId"]}',productId=c['productId'],productSlug='metodo-musa-7-dias',prototypeVersion=version,cycleId=c['id'],videoIntegration=proof)))
session=v.http('/api/pde/vega/private/v1/internal/sessions',dict(cycleId=c['id'],prototypeVersion=version,origin='QA_INTERNAL'),headers={'X-PDE-Internal-Token':'vega-local-internal-only'})
assert session['videoIntegration']==proof and session['events']=={}
v.http('/fixture/videos/mutate',dict(id=assets['pdeVideoAssetId'],kind='reject'))
session=v.http('/api/pde/vega/private/v1/session',headers={'X-Vega-Session':session['sessionToken']})
assert 'videoIntegration' not in session
assert not session['paymentEnabled']
# Restaura a aprovação sintética pela fonte de teste, preservando exatamente responsável e horário.
v.http('/fixture/video-continuation/restore-approval',dict(assetId=assets['pdeVideoAssetId']))
print('PASS integração real, concorrência, recibo único, revogação, isolamento e gates comerciais')
