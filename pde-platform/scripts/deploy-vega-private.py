#!/usr/bin/env python3
"""Publica imagens já testadas, lendo secrets no host sem copiá-los para arquivos ou logs."""
import argparse
import json
import os
from pathlib import Path
import subprocess


def deployment_environment(source, key_path, frontend_image, worker_image):
    """Resolve credenciais existentes em memória e mantém o executor sem privilégio root."""
    values = dict(item.split('=', 1) for item in source['Config']['Env'] if '=' in item)
    internal = values.get('PDE_INTERNAL_API_TOKEN', '').strip()
    key = Path(key_path).read_text().strip()
    if not internal or not key:
        raise ValueError('Credenciais internas/OpenAI ausentes; publicação interrompida.')
    result = dict(os.environ)
    result.update(PDE_INTERNAL_API_TOKEN=internal, VEGA_OPENAI_API_KEY=key,
                  VEGA_PRIVATE_FRONTEND_IMAGE=frontend_image, VEGA_PRIVATE_WORKER_IMAGE=worker_image)
    return result


def main():
    """Valida as imagens locais e inicia exclusivamente os dois serviços privados de Vega."""
    parser = argparse.ArgumentParser()
    parser.add_argument('--compose', required=True)
    parser.add_argument('--credential-container', default='pde-platform-backend')
    parser.add_argument('--openai-key-file', required=True)
    parser.add_argument('--frontend-image', required=True)
    parser.add_argument('--worker-image', required=True)
    args = parser.parse_args()
    for tag in [args.frontend_image, args.worker_image]:
        subprocess.run(['docker', 'image', 'inspect', tag], check=True, stdout=subprocess.DEVNULL)
    source = json.loads(subprocess.check_output(['docker', 'inspect', args.credential_container]))[0]
    env = deployment_environment(source, args.openai_key_file, args.frontend_image, args.worker_image)
    command = ['docker', 'compose', '-p', 'vega-private', '-f', args.compose]
    subprocess.run(command + ['config', '--quiet'], check=True, env=env)
    subprocess.run(command + ['up', '-d', '--no-build', '--pull', 'never'], check=True, env=env)
    print('Serviços privados de Vega iniciados; credenciais mantidas fora de arquivos e saída.')


if __name__ == '__main__':
    main()
