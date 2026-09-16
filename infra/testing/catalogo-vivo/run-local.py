#!/usr/bin/env python3
"""Homologa o catálogo no banco segregado e limpa somente a topologia Compose desta sessão."""
import argparse
import os
from pathlib import Path
import subprocess
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / 'artifacts/catalogo-vivo-opala'
PROJECT = os.environ.get('CATALOGO_VIVO_COMPOSE_PROJECT', '')
HOST = os.environ.get('CATALOGO_VIVO_DB_HOST', 'sandbox-docker')


def command(args, name, env=None, cwd=ROOT):
    """Executa um gate e preserva a saída completa, mostrando o trecho final quando falha."""
    with (OUT / name).open('w') as log:
        result = subprocess.run(args, cwd=cwd, env=env, stdout=log, stderr=subprocess.STDOUT)
    if result.returncode:
        print((OUT / name).read_text()[-6000:])
        raise RuntimeError(f'Falha em {name}: código {result.returncode}')


def wait_url(url, process):
    """Aguarda a API local por prazo limitado e interrompe se o processo encerrar."""
    for _ in range(90):
        if process.poll() is not None:
            raise RuntimeError('A aplicação local encerrou antes da verificação')
        try:
            with urllib.request.urlopen(url, timeout=2) as response:
                if response.status == 200:
                    return
        except (OSError, ValueError):
            pass
        time.sleep(1)
    raise RuntimeError(f'API local não respondeu: {url}')


def main():
    """Executa matriz completa ou somente persistência, preservando segregação e limpeza."""
    parser = argparse.ArgumentParser()
    parser.add_argument('--persistence-only', action='store_true')
    args = parser.parse_args()
    if not PROJECT.startswith('aihub-') or HOST not in ('127.0.0.1', 'sandbox-docker'):
        raise SystemExit('Informe CATALOGO_VIVO_COMPOSE_PROJECT exclusivo e host local permitido')
    OUT.mkdir(parents=True, exist_ok=True)
    compose = ['docker', 'compose', '-p', PROJECT, '-f', 'backend/ads-service/docker-compose.learning-cycles-local.yml']
    processes, logs = [], []
    env = dict(os.environ, CATALOGO_VIVO_MYSQL_URL=f'jdbc:mysql://{HOST}:18307/catalogo_vivo_local?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC')
    try:
        command(['docker', 'version'], 'docker-version.log')
        command(['docker', 'buildx', 'version'], 'buildx-version.log')
        command(['docker', 'compose', 'version'], 'compose-version.log')
        command(compose + ['up', '-d', '--wait', 'learning-cycles-mysql'], 'mysql-start.log')
        command(compose + ['exec', '-T', 'learning-cycles-mysql', 'mysql', '-uroot', '-pcycles-root-local-only', '-e', 'DROP DATABASE IF EXISTS catalogo_vivo_local; CREATE DATABASE catalogo_vivo_local CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;'], 'mysql-reset.log')
        if not args.persistence_only:
            command(['mvn', '-B', '-f', 'backend/ads-service/pom.xml', 'clean', 'test'], 'backend-full-tests.log')
            for module in ['landing-generator-agent-worker', 'financial-agent-worker', 'customer-agent-worker', 'meta-ad-approver-worker']:
                command(['mvn', '-B', '-f', f'{module}/pom.xml', 'clean', 'test'], f'{module}-tests.log')
        command(['mvn', '-B', '-f', 'backend/ads-service/pom.xml', '-Dtest=CatalogoVivo*Test,OpalaAdoptionRoutingTest', 'test'], 'catalog-integration.log', env)
        command(['bash', 'scripts/validate-liquibase-mysql57.sh'], 'liquibase-static.log')
        if args.persistence_only:
            return
        command(['npm', 'ci', '--no-audit', '--no-fund'], 'frontend-install.log', cwd=ROOT/'frontend')
        command(['npm', 'run', 'typecheck'], 'frontend-typecheck.log', cwd=ROOT/'frontend')
        command(['npm', 'test', '--', '--run', 'src/pages/catalogoVivo', 'src/pages/learningCycle', 'src/pages/agent/AgentListPage.test.tsx', 'src/pages/agent/AgentDetailPage.test.tsx'], 'frontend-tests.log', cwd=ROOT/'frontend')
        command(['mvn', '-B', '-f', 'backend/ads-service/pom.xml', '-DskipTests', 'package', 'dependency:build-classpath', '-Dmdep.includeScope=test', '-Dmdep.outputFile=target/catalogo-vivo.classpath'], 'backend-package.log')
        for module in ['landing-generator-agent-worker', 'financial-agent-worker', 'customer-agent-worker', 'meta-ad-approver-worker']:
            command(['mvn', '-B', '-f', f'{module}/pom.xml', '-DskipTests', 'package'], f'{module}-package.log')
        command(['python3', 'infra/testing/catalogo-vivo/verify-packaging.py'], 'packaging-verification.log')
        command(['python3', 'scripts/test-spotless-changed-java.py'], 'formatter-contract.log')
        classpath = ':'.join(str(ROOT/p) for p in ['backend/ads-service/target/test-classes','backend/ads-service/target/classes']) + ':' + (ROOT/'backend/ads-service/target/catalogo-vivo.classpath').read_text().strip()
        for name, cmd, cwd in [
            ('fixture-api.log', ['java', '-Xmx768m', '-cp', classpath, 'com.marketinghub.catalogovivo.v1.service.CatalogoVivoLocalApplication'], ROOT),
            ('vite.log', ['./node_modules/.bin/vite', '--config', 'vite.learning-cycles-local.config.ts'], ROOT/'frontend')]:
            log = (OUT/name).open('w'); logs.append(log)
            processes.append(subprocess.Popen(cmd, cwd=cwd, env=env, stdout=log, stderr=subprocess.STDOUT))
        wait_url('http://127.0.0.1:18091/fixture/health', processes[0])
        wait_url('http://127.0.0.1:15173/catalogo-vivo/opala', processes[1])
        # A integração anterior comprovou idempotência. Reabre somente a adesão da fixture
        # para provar o envio pelo navegador; nenhuma definição, tarefa ou orçamento é apagado.
        command(compose + ['exec', '-T', 'learning-cycles-mysql', 'mysql', '-uroot', '-pcycles-root-local-only', 'catalogo_vivo_local', '-e', 'DELETE FROM catalogo_vivo_opala_adoption_v1 WHERE cycle_id=900002 AND product_id=900004;'], 'adoption-fixture-reset.log')
        command(['node', 'infra/testing/catalogo-vivo/browser.mjs'], 'browser.log')
    finally:
        for process in reversed(processes):
            process.terminate()
            try:
                process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                process.kill(); process.wait()
        for log in logs:
            log.close()
        command(compose + ['down', '--volumes', '--remove-orphans'], 'cleanup.log')


if __name__ == '__main__':
    main()
