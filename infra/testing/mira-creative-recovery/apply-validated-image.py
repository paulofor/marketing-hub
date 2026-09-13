#!/usr/bin/env python3
"""Aplica exclusivamente backend ou Íris homologados, sob execute do coordenador externo."""
import argparse
import json
import os
import re
import subprocess


def literal(value):
    """Impede reinterpretação de cifrões em secrets de um Compose já resolvido."""
    if isinstance(value, str):
        return value.replace('$', '$$')
    if isinstance(value, dict):
        return {key: literal(item) for key, item in value.items()}
    if isinstance(value, list):
        return [literal(item) for item in value]
    return value


def selected_configuration(config, current, image, revision):
    """Mantém configuração, redes e volumes; troca apenas imagem e identificação de revisão."""
    labels = current['Config']['Labels']
    service = labels['com.docker.compose.service']
    expected = {
        'backend': 'marketinghub-backend',
        'communication-agent-worker': 'communication-agent-worker-communication-agent-worker-1',
    }
    if service not in expected or current['Name'].lstrip('/') != expected[service]:
        raise ValueError('Container fora do escopo homologado de Mira.')
    if image != f'marketing-hub/{service}:mira-creative-review-v1-{revision[:12]}':
        raise ValueError('Imagem incompatível com serviço e revisão homologados.')
    selected = config['services'][service]
    running_env = dict(item.split('=', 1) for item in current['Config']['Env'] if '=' in item)
    environment = dict(selected.get('environment', {})) | running_env
    if service == 'backend':
        environment.update(BACKEND_BUILD_COMMIT=revision, BACKEND_BUILD_BRANCH='mira-creative-review-v1')
    else:
        environment['AGENT_BUILD_REFERENCE'] = revision
    selected.update(image=image, environment=environment)
    selected.pop('build', None)
    selected.pop('depends_on', None)
    source_files = labels.get('com.marketinghub.original-compose-files') or labels['com.docker.compose.project.config_files']
    selected.setdefault('labels', {})['com.marketinghub.original-compose-files'] = source_files
    config['services'] = {service: selected}
    return config


def read(command, **kwargs):
    """Captura configurações em memória sem publicar saída que possa conter secrets."""
    result = subprocess.run(command, capture_output=True, text=True, **kwargs)
    if result.returncode:
        raise RuntimeError('Falha operacional; comando=' + command[0] + ' exit=' + str(result.returncode))
    return json.loads(result.stdout)


def main():
    """Valida identidade antes da troca e conserva a imagem anterior por tag exclusiva de retorno."""
    parser = argparse.ArgumentParser()
    parser.add_argument('--container', required=True)
    parser.add_argument('--image', required=True)
    parser.add_argument('--revision', required=True)
    args = parser.parse_args()
    if not re.fullmatch('[0-9a-f]{40}', args.revision):
        raise ValueError('Revisão exata obrigatória.')
    service_images = [f'marketing-hub/{name}:mira-creative-review-v1-{args.revision[:12]}'
                      for name in ('backend', 'communication-agent-worker')]
    if args.image not in service_images:
        raise ValueError('Imagem fora da recuperação homologada.')
    current = read(['docker', 'inspect', args.container])[0]
    candidate = read(['docker', 'image', 'inspect', args.image])[0]
    if candidate['Config'].get('Labels', {}).get('org.opencontainers.image.revision') != args.revision:
        raise ValueError('Imagem não comprova a revisão validada.')
    if current['Image'] == candidate['Id']:
        print(json.dumps({'container': args.container, 'image': args.image, 'status': 'ALREADY_APPLIED'}))
        return
    labels = current['Config']['Labels']
    service = labels['com.docker.compose.service']
    project = labels['com.docker.compose.project']
    workdir = labels['com.docker.compose.project.working_dir']
    source_files = labels.get('com.marketinghub.original-compose-files') or labels['com.docker.compose.project.config_files']
    environment = dict(os.environ) | dict(item.split('=', 1) for item in current['Config']['Env'] if '=' in item)
    mounts = {item['Destination']: item['Source'] for item in current['Mounts']}
    if service == 'communication-agent-worker':
        environment.update(COMMUNICATION_AGENT_IMAGE=args.image,
            COMMUNICATION_AGENT_CODEX_HOME=mounts['/home/operator/.codex'],
            MARKETING_HUB_REPOSITORY_HOST=mounts['/workspace/marketing-hub'])
    source = ['docker', 'compose', '-p', project]
    for filename in source_files.split(','):
        source.extend(['-f', filename])
    config = read(source + ['config', '--format', 'json'], cwd=workdir, env=environment)
    config = selected_configuration(config, current, args.image, args.revision)
    rollback = f'marketing-hub/{service}:mira-creative-review-rollback-{args.revision[:12]}'
    # A tag exclusiva preserva o conteúdo anterior sem sobrescrever tags usadas pelos publicadores.
    subprocess.run(['docker', 'tag', current['Image'], rollback], check=True)
    print(json.dumps({'container': args.container, 'previousImage': current['Config']['Image'],
        'rollbackImage': rollback, 'rollbackImageId': current['Image']}), flush=True)
    payload = json.dumps(literal(config))
    command = ['docker', 'compose', '-p', project, '-f', '-']
    for operation in (['config', '--quiet'], ['up', '-d', '--no-build', '--pull', 'never', '--no-deps', service]):
        result = subprocess.run(command + operation, input=payload, text=True,
            capture_output=True, cwd=workdir, env=environment)
        if result.returncode:
            raise RuntimeError('Compose recusou a operação ' + operation[0] + '; exit=' + str(result.returncode))
    updated = read(['docker', 'inspect', args.container])[0]
    if updated['Image'] != candidate['Id']:
        raise RuntimeError('Container diverge da imagem homologada.')
    actual = dict(item.split('=', 1) for item in updated['Config']['Env'] if '=' in item)
    for key, value in config['services'][service]['environment'].items():
        if value is not None and actual.get(key) != str(value):
            raise RuntimeError('Configuração diverge; campo=' + key)
    print(json.dumps({'container': args.container, 'image': args.image,
        'imageId': candidate['Id'], 'state': updated['State']['Status'], 'revision': args.revision}))


if __name__ == '__main__':
    main()
