#!/usr/bin/env python3
"""Aplica uma imagem homologada desta recuperação sob execute do coordenador, preservando configuração."""
import argparse
import copy
import json
import os
import re
import subprocess

TARGETS = {
    'marketinghub-backend': ('backend', 'backend'),
    'marketinghub-frontend': ('frontend', 'frontend'),
    'customer-agent-worker-customer-agent-worker-1': ('customer-agent-worker', 'customer-agent-worker'),
    'meta-ad-approver-worker-meta-ad-approver-worker-1': ('meta-ad-approver-worker', 'meta-ad-approver-worker'),
    'pde-platform-frontend-vega-private': ('vega-private-frontend', 'pde-platform-frontend-vega'),
}


def literal(value):
    """Preserva cifrões de valores já resolvidos sem interpolar credenciais novamente."""
    if isinstance(value, str): return value.replace('$', '$$')
    if isinstance(value, dict): return {key: literal(item) for key, item in value.items()}
    if isinstance(value, list): return [literal(item) for item in value]
    return value


def read(command, **kwargs):
    """Captura configuração em memória; erros nunca imprimem o conteúdo ou credenciais."""
    result = subprocess.run(command, capture_output=True, text=True, **kwargs)
    if result.returncode: raise RuntimeError(f'Operação recusada: {command[0]} (exit {result.returncode})')
    return json.loads(result.stdout)


def plan(config, current, image, digest):
    """Troca somente imagem e identificação, mantendo todos os valores do container existente."""
    name = current['Name'].lstrip('/')
    if name not in TARGETS or not re.fullmatch('[0-9a-f]{64}', digest): raise ValueError('Alvo ou revisão fora da recuperação.')
    service, repository = TARGETS[name]
    if current['Config']['Labels']['com.docker.compose.service'] != service: raise ValueError('Serviço divergente.')
    if image != f'marketing-hub/{repository}:vega-approved-{digest[:12]}': raise ValueError('Imagem divergente da revisão.')
    result = copy.deepcopy(config)
    selected = result['services'][service]
    selected['environment'] = dict(item.split('=',1) for item in current['Config']['Env'] if '=' in item)
    selected.update(image=image)
    selected.pop('build',None); selected.pop('depends_on',None)
    labels = current['Config']['Labels']
    selected.setdefault('labels',{}).update({
        'com.marketinghub.original-compose-files': labels.get('com.marketinghub.original-compose-files',labels['com.docker.compose.project.config_files']),
        'com.marketinghub.source-digest': digest,
    })
    result['services'] = {service:selected}
    return result


def main():
    """Confere a revisão local, preserva rollback e recria exclusivamente o serviço selecionado."""
    parser=argparse.ArgumentParser()
    parser.add_argument('--container',required=True,choices=TARGETS)
    parser.add_argument('--digest',required=True)
    parser.add_argument('--check-only',action='store_true')
    args=parser.parse_args()
    service, repository=TARGETS[args.container]
    image=f'marketing-hub/{repository}:vega-approved-{args.digest[:12]}'
    current=read(['docker','inspect',args.container])[0]
    candidate=read(['docker','image','inspect',image])[0]
    if candidate['Config'].get('Labels',{}).get('com.marketinghub.source-digest')!=args.digest: raise ValueError('Imagem não comprova a revisão homologada.')
    labels=current['Config']['Labels'];project=labels['com.docker.compose.project'];workdir=labels['com.docker.compose.project.working_dir']
    sources=labels.get('com.marketinghub.original-compose-files',labels['com.docker.compose.project.config_files'])
    env=dict(os.environ)|dict(item.split('=',1) for item in current['Config']['Env'] if '=' in item)
    mounts={m['Destination']:m['Source'] for m in current['Mounts']}
    if service=='customer-agent-worker': env.update(CUSTOMER_AGENT_IMAGE=image,CUSTOMER_AGENT_CODEX_HOME=mounts['/home/operator/.codex'],MARKETING_HUB_REPOSITORY=mounts['/workspace'])
    if service=='meta-ad-approver-worker': env.update(META_AD_APPROVER_IMAGE=image,META_AD_APPROVER_CODEX_HOME=mounts['/home/approver/.codex'],MARKETING_HUB_REPOSITORY_HOST=mounts['/workspace/marketing-hub'])
    if service=='frontend':
        backend=read(['docker','inspect','marketinghub-backend'])[0]
        if backend['Config']['Labels']['com.docker.compose.project']!=project: raise ValueError('Backend pertence a outro projeto.')
        env=dict(os.environ)|dict(v.split('=',1) for v in backend['Config']['Env'] if '=' in v)|env
    if service=='vega-private-frontend':
        worker=read(['docker','inspect','pde-vega-private-worker'])[0]
        worker_env=dict(v.split('=',1) for v in worker['Config']['Env'] if '=' in v)
        env=dict(os.environ)|worker_env|env
        env.update(VEGA_PRIVATE_FRONTEND_IMAGE=image,VEGA_PRIVATE_WORKER_IMAGE=worker['Config']['Image'],VEGA_OPENAI_API_KEY=worker_env['OPENAI_API_KEY'])
    command=['docker','compose','-p',project]
    for filename in sources.split(','): command+=['-f',filename]
    config=plan(read(command+['config','--format','json'],cwd=workdir,env=env),current,image,args.digest)
    payload=json.dumps(literal(config))
    apply=['docker','compose','-p',project,'-f','-']
    check=subprocess.run(apply+['config','--quiet'],input=payload,text=True,capture_output=True,cwd=workdir,env=env)
    if check.returncode: raise RuntimeError('Compose recusou a configuração preservada.')
    rollback=f'marketing-hub/{repository}:vega-approved-rollback-{args.digest[:12]}'
    if args.check_only:
        print(json.dumps({'checked':True,'container':args.container,'image':image,'previousImageId':current['Image']}));return
    if current['Image']==candidate['Id']:
        print(json.dumps({'status':'ALREADY_APPLIED','container':args.container}));return
    subprocess.run(['docker','tag',current['Image'],rollback],check=True)
    print(json.dumps({'container':args.container,'rollbackImage':rollback,'rollbackImageId':current['Image']}),flush=True)
    result=subprocess.run(apply+['up','-d','--no-build','--pull','never','--no-deps',service],input=payload,text=True,capture_output=True,cwd=workdir,env=env)
    if result.returncode: raise RuntimeError('Compose recusou a troca; rollback preservado.')
    updated=read(['docker','inspect',args.container])[0]
    if updated['Image']!=candidate['Id']: raise RuntimeError('Imagem publicada diverge da homologada.')
    actual=dict(v.split('=',1) for v in updated['Config']['Env'] if '=' in v)
    if any(actual.get(key)!=value for key,value in config['services'][service]['environment'].items()): raise RuntimeError('Configuração operacional não foi preservada.')
    print(json.dumps({'container':args.container,'image':image,'imageId':candidate['Id'],'digest':args.digest,'state':updated['State']['Status']}))


if __name__=='__main__': main()
