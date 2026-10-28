#!/usr/bin/env python3
"""Valida a migração incremental duas vezes no MySQL 5.7 isolado e limpa a topologia."""
import os
from pathlib import Path
import subprocess
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[3]
PROJECT = os.environ['CHATGPT_COMPOSE_PROJECT']
if not PROJECT.startswith('aihub-'):
    raise ValueError('Use o projeto Compose exclusivo da sandbox')
COMPOSE = ['docker', 'compose', '-p', PROJECT, '-f', str(Path(__file__).with_name('compose.yaml'))]
host = urlparse(os.environ.get('DOCKER_HOST', 'tcp://127.0.0.1')).hostname
jdbc = f'jdbc:mysql://{host}:33371/chatgpt_test?useSSL=false&serverTimezone=UTC'

def execute(args, **kwargs):
    """Executa somente dependências locais, interrompendo em erro real."""
    return subprocess.run(args, check=True, text=True, **kwargs)

def sql(query):
    """Consulta o schema descartável e nunca o MCP de produção."""
    return execute(COMPOSE + ['exec', '-T', 'mysql', 'mysql', '-uroot', '-plocal-chatgpt-only', '-N', 'chatgpt_test'], input=query, capture_output=True).stdout.strip()

try:
    execute(COMPOSE + ['up', '-d', '--wait', '--wait-timeout', '120', 'mysql'])
    sql('CREATE TABLE experiment(id BIGINT PRIMARY KEY); INSERT INTO experiment VALUES(991);')
    command = ['mvn', '-q', '-f', str(ROOT / 'backend/ads-service/pom.xml'),
               '-Dliquibase.changeLogFile=src/main/resources/db/changelog/changesets/2026-10-27-chatgpt-ads-v1.yaml',
               '-Dliquibase.url=' + jdbc, '-Dliquibase.username=root', '-Dliquibase.password=local-chatgpt-only',
               'org.liquibase:liquibase-maven-plugin:4.26.0:update']
    execute(command, cwd=ROOT / 'backend/ads-service')
    execute(command, cwd=ROOT / 'backend/ads-service')
    assert sql('SELECT COUNT(*) FROM DATABASECHANGELOG') == '1'
    assert sql("SELECT COUNT(*) FROM chatgpt_ads_connection WHERE id=1") == '1'
    print('Migração MySQL 5.7 e reaplicação confirmadas.')
    execute(['mvn', '-q', '-f', str(ROOT / 'backend/ads-service/pom.xml'), '-Dtest=ChatgptAdsServiceTest', '-DforkCount=0',
             '-Dchatgpt.test.jdbc=' + jdbc, '-Dchatgpt.test.user=root', '-Dchatgpt.test.password=local-chatgpt-only', 'test'])
finally:
    execute(COMPOSE + ['down', '--volumes', '--remove-orphans'])
