#!/usr/bin/env python3
"""Prepara ou aplica CORS de leitura para vídeos, preservando regras existentes e rollback."""
import argparse
import copy
import hashlib
import json
import pathlib
import subprocess
from urllib.parse import urlsplit

RULE_ID = 'marketing-hub-video-read-v1'


def digest(config):
    """Identifica a configuração pública sem incluir credenciais."""
    return hashlib.sha256(json.dumps(config, sort_keys=True, separators=(',', ':')).encode()).hexdigest()


def merge_read_rule(current, origins):
    """Acrescenta somente origens explícitas de leitura sem remover regras ou liberar escrita."""
    if not origins:
        raise ValueError('Informe a origem real da tela.')
    for origin in origins:
        parsed = urlsplit(origin)
        if parsed.scheme not in {'http', 'https'} or not parsed.hostname or parsed.username or parsed.password or parsed.path or parsed.query or parsed.fragment or '*' in origin:
            raise ValueError('Origem deve conter apenas protocolo, host e porta, sem wildcard.')
    result = copy.deepcopy(current)
    rules = result.setdefault('CORSRules', [])
    existing = [r for r in rules if r.get('ID') == RULE_ID]
    if len(existing) > 1:
        raise ValueError('Regra duplicada; não substituir configuração ambígua.')
    rule = existing[0] if existing else {'ID': RULE_ID, 'AllowedOrigins': [], 'AllowedMethods': ['GET', 'HEAD'],
                                      'AllowedHeaders': ['Range'],
                                      'ExposeHeaders': ['Accept-Ranges', 'Content-Length', 'Content-Range', 'ETag'],
                                      'MaxAgeSeconds': 300}
    if set(rule.get('AllowedMethods', [])) != {'GET', 'HEAD'}:
        raise ValueError('A regra existente contém operações diferentes de leitura.')
    rule['AllowedOrigins'] = sorted(set(rule.get('AllowedOrigins', []) + origins))
    if not existing:
        rules.append(rule)
    return result


def read_current(command, bucket):
    """Consulta CORS via AWS CLI sem imprimir variáveis de autenticação."""
    result = subprocess.run(command + ['get-bucket-cors', '--bucket', bucket], capture_output=True, text=True, timeout=60)
    if result.returncode == 0:
        return json.loads(result.stdout)
    if 'NoSuchCORSConfiguration' in result.stderr:
        return {'CORSRules': []}
    raise RuntimeError('Não foi possível consultar CORS: ' + result.stderr.strip())


def main():
    """Só aplica após conferir a configuração esperada e gravar uma cópia pública de retorno."""
    parser = argparse.ArgumentParser()
    parser.add_argument('--bucket', required=True)
    parser.add_argument('--endpoint-url', required=True)
    parser.add_argument('--origin', action='append', required=True)
    parser.add_argument('--output', required=True)
    parser.add_argument('--apply', action='store_true')
    parser.add_argument('--expected-current-sha256')
    args = parser.parse_args()
    command = ['aws', '--output', 'json', '--endpoint-url', args.endpoint_url, 's3api']
    current = read_current(command, args.bucket)
    planned = merge_read_rule(current, args.origin)
    output = pathlib.Path(args.output)
    output.mkdir(parents=True, exist_ok=True)
    if args.apply and (not args.expected_current_sha256 or digest(current) != args.expected_current_sha256):
        raise ValueError('CORS mudou desde a revisão; nenhuma alteração aplicada.')
    backup = output / ('cors-before-' + digest(current) + '.json')
    backup.write_text(json.dumps(current, indent=2))
    candidate = output / 'cors-candidate.json'
    candidate.write_text(json.dumps(planned, indent=2))
    if args.apply and planned != current:
        subprocess.run(command + ['put-bucket-cors', '--bucket', args.bucket, '--cors-configuration', 'file://' + str(candidate.resolve())], check=True, timeout=60)
        if digest(read_current(command, args.bucket)) != digest(planned):
            raise RuntimeError('O retorno de CORS diverge da configuração aplicada.')
    print(json.dumps({'applied': args.apply, 'changed': planned != current,
                      'previousSha256': digest(current), 'plannedSha256': digest(planned),
                      'origins': args.origin, 'backup': str(backup), 'candidate': str(candidate)}))


if __name__ == '__main__':
    main()
