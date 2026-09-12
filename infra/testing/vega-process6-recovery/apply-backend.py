#!/usr/bin/env python3
"""Aplica um serviço APP homologado pelo Compose versionado, preservando sua configuração."""
import argparse
import copy
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess


def literal(value):
    """Preserva cifrões literais na configuração já resolvida pelo Compose."""
    if isinstance(value, str):
        return value.replace('$', '$$')
    if isinstance(value, dict):
        return {key: literal(item) for key, item in value.items()}
    if isinstance(value, list):
        return [literal(item) for item in value]
    return value


def plan(current, config, image, service="backend"):
    """Seleciona o serviço existente e conserva os valores operacionais declarados no Compose."""
    if service not in {'backend', 'frontend'}:
        raise ValueError('Serviço fora do escopo APP desta recuperação.')
    if not re.fullmatch(rf'marketing-hub/{service}:vega-cycle6-[a-f0-9]{{12}}', image):
        raise ValueError('Imagem fora do escopo desta recuperação.')
    labels = current['Config']['Labels']
    if labels.get('com.docker.compose.service') != service:
        raise ValueError('O container não corresponde ao serviço solicitado.')
    selected = copy.deepcopy(config['services'][service])
    if selected.get('container_name') != 'marketinghub-' + service:
        raise ValueError('O Compose não identifica o container esperado.')
    running = dict(value.split('=', 1) for value in current['Config']['Env'] if '=' in value)
    for key in selected.get('environment', {}):
        if key in running:
            selected['environment'][key] = running[key]
    selected.setdefault('labels', {})['com.marketinghub.original-compose-files'] = labels.get(
        'com.marketinghub.original-compose-files', labels['com.docker.compose.project.config_files'])
    selected['image'] = image
    selected.pop('build', None)
    selected.pop('depends_on', None)
    result = copy.deepcopy(config)
    result['services'] = {service: selected}
    return result


def runtime_environment(current, backend):
    """Resolve o Compose integral em memória e prioriza os valores do serviço selecionado."""
    backend_values = dict(value.split('=', 1) for value in backend['Config']['Env'] if '=' in value)
    current_values = dict(value.split('=', 1) for value in current['Config']['Env'] if '=' in value)
    return dict(os.environ) | backend_values | current_values


def read_json(command, **kwargs):
    """Lê configuração operacional em memória sem imprimir valores de credenciais."""
    return json.loads(subprocess.check_output(command, **kwargs))


def main():
    """Confere o Compose e a imagem antes de uma única recriação, ou executa apenas a pré-checagem."""
    parser = argparse.ArgumentParser()
    parser.add_argument('--image', required=True)
    parser.add_argument('--service', choices=['backend', 'frontend'], default='backend')
    parser.add_argument('--compose-sha256', required=True)
    parser.add_argument('--check-only', action='store_true')
    args = parser.parse_args()
    current = read_json(['docker', 'inspect', 'marketinghub-' + args.service])[0]
    labels = current['Config']['Labels']
    filenames = labels.get('com.marketinghub.original-compose-files', labels['com.docker.compose.project.config_files']).split(',')
    if len(filenames) != 1 or hashlib.sha256(Path(filenames[0]).read_bytes()).hexdigest() != args.compose_sha256:
        raise ValueError('O Compose publicado não corresponde ao arquivo validado no repositório.')
    project = labels['com.docker.compose.project']
    workdir = labels['com.docker.compose.project.working_dir']
    backend = current if args.service == 'backend' else read_json(['docker', 'inspect', 'marketinghub-backend'])[0]
    environment = runtime_environment(current, backend)
    config = read_json(['docker', 'compose', '-p', project, '-f', filenames[0], 'config', '--format', 'json'], cwd=workdir, env=environment)
    prepared = plan(current, config, args.image, args.service)
    payload = json.dumps(literal(prepared)).encode()
    command = ['docker', 'compose', '-p', project, '-f', '-']
    subprocess.run(command + ['config', '--quiet'], input=payload, env=environment, cwd=workdir, check=True)
    if args.check_only:
        print(json.dumps({'checked': True, 'services': list(prepared['services']), 'previousImageId': current['Image']}))
        return
    target = read_json(['docker', 'image', 'inspect', args.image])[0]
    subprocess.run(command + ['up', '-d', '--no-build', '--pull', 'never', '--no-deps', args.service], input=payload, env=environment, cwd=workdir, check=True)
    updated = read_json(['docker', 'inspect', 'marketinghub-' + args.service])[0]
    if updated['Image'] != target['Id']:
        raise ValueError('A imagem em execução diverge da imagem homologada.')
    after = dict(value.split('=', 1) for value in updated['Config']['Env'] if '=' in value)
    for key, value in prepared['services'][args.service].get('environment', {}).items():
        if value is not None and after.get(key) != str(value):
            raise ValueError('A configuração operacional não foi preservada: ' + key)
    print(json.dumps({'applied': True, 'image': args.image, 'imageId': updated['Image'], 'previousImageId': current['Image']}))


if __name__ == '__main__':
    main()
