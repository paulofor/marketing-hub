#!/usr/bin/env python3
"""Ativa uma imagem já homologada, preservando a configuração e os secrets somente em memória."""
import argparse
import json
import os
import subprocess


def literal_compose(value):
    """Preserva cifrões literais ao entregar configuração já resolvida novamente ao Compose."""
    if isinstance(value, str):
        return value.replace('$', '$$')
    if isinstance(value, dict):
        return {key: literal_compose(item) for key, item in value.items()}
    if isinstance(value, list):
        return [literal_compose(item) for item in value]
    return value


def main():
    """Recria exclusivamente o serviço existente solicitado, sem build, pull ou alteração de secrets."""
    parser = argparse.ArgumentParser()
    parser.add_argument('--container', required=True)
    parser.add_argument('--image', required=True)
    args = parser.parse_args()
    if not args.image.startswith('marketing-hub/') or not args.image.endswith(':vega386-v11'):
        raise ValueError('Imagem fora da publicação homologada desta correção.')
    current = json.loads(subprocess.check_output(['docker', 'inspect', args.container]))[0]
    labels = current['Config']['Labels']
    service = labels['com.docker.compose.service']
    if service not in ['backend', 'customer-agent-worker', 'landing-generator-agent-worker', 'vega-private-frontend']:
        raise ValueError('Serviço fora do escopo das atividades 3.6 e 3.7.')
    project = labels['com.docker.compose.project']
    workdir = labels['com.docker.compose.project.working_dir']
    source_files = labels.get('com.marketinghub.original-compose-files', labels['com.docker.compose.project.config_files'])
    source = ['docker', 'compose', '-p', project]
    for filename in source_files.split(','):
        source += ['-f', filename]
    subprocess.run(['docker', 'image', 'inspect', args.image], stdout=subprocess.DEVNULL, check=True)
    # Secrets do container atual são reaproveitados, sem arquivos intermediários ou saída.
    running_env = dict(value.split('=', 1) for value in current['Config']['Env'] if '=' in value)
    environment_values = dict(os.environ) | running_env
    mounts = {m['Destination']: m['Source'] for m in current['Mounts']}
    if service == 'customer-agent-worker':
        environment_values.update(CUSTOMER_AGENT_IMAGE=args.image,
            CUSTOMER_AGENT_CODEX_HOME=mounts['/home/operator/.codex'], MARKETING_HUB_REPOSITORY=mounts['/workspace'])
    elif service == 'landing-generator-agent-worker':
        environment_values.update(LANDING_GENERATOR_AGENT_IMAGE=args.image,
            LANDING_GENERATOR_CODEX_HOME=mounts['/home/landingagent/.codex'],
            MARKETING_HUB_REPOSITORY_HOST=mounts['/workspace/marketing-hub'])
    elif service == 'vega-private-frontend':
        worker = json.loads(subprocess.check_output(['docker', 'inspect', 'pde-vega-private-worker']))[0]
        worker_env = dict(v.split('=', 1) for v in worker['Config']['Env'] if '=' in v)
        environment_values.update(VEGA_PRIVATE_FRONTEND_IMAGE=args.image, VEGA_PRIVATE_WORKER_IMAGE=worker['Config']['Image'],
            PDE_INTERNAL_API_TOKEN=worker_env['PDE_INTERNAL_API_TOKEN'], VEGA_OPENAI_API_KEY=worker_env['OPENAI_API_KEY'])
    config = json.loads(subprocess.check_output(source + ['config', '--format', 'json'], cwd=workdir, env=environment_values))
    selected = config['services'][service]
    selected.setdefault('labels', {})['com.marketinghub.original-compose-files'] = source_files
    environment = selected.get('environment', {})
    for key in list(environment):
        if key in running_env:
            environment[key] = running_env[key]
    if service == 'backend':
        environment['PDE_VEGA_PROTOTYPE_VERSION'] = 'musa-pde-entry-v11-primeiro-ajuste-aplicavel'
    elif service in ['customer-agent-worker', 'landing-generator-agent-worker']:
        environment['AGENT_BUILD_REFERENCE'] = 'vega386-v11'
    selected['environment'] = environment
    selected['image'] = args.image
    selected.pop('build', None)
    selected.pop('depends_on', None)
    config['services'] = {service: selected}
    payload = json.dumps(literal_compose(config)).encode()
    command = ['docker', 'compose', '-p', project, '-f', '-']
    subprocess.run(command + ['config', '--quiet'], input=payload, cwd=workdir, env=environment_values, check=True)
    subprocess.run(command + ['up', '-d', '--no-build', '--pull', 'never', '--no-deps', service],
                   input=payload, cwd=workdir, env=environment_values, check=True)
    updated = json.loads(subprocess.check_output(['docker', 'inspect', args.container]))[0]
    expected = json.loads(subprocess.check_output(['docker', 'image', 'inspect', args.image]))[0]['Id']
    if updated['Image'] != expected:
        raise RuntimeError('O container não usa a imagem homologada.')
    updated_env = dict(value.split('=', 1) for value in updated['Config']['Env'] if '=' in value)
    for key, value in environment.items():
        if value is not None and updated_env.get(key) != str(value):
            raise RuntimeError('A configuração não foi preservada: ' + key)
    print(json.dumps({'container': args.container, 'image': args.image, 'imageId': expected,
                      'status': updated['State']['Status']}))


if __name__ == '__main__':
    main()
