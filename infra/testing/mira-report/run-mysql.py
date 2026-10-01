#!/usr/bin/env python3
"""Valida fisicamente consolidação Meta com dados sintéticos, idempotência e rollback MySQL 5.7."""
import os
import pathlib
import subprocess

ROOT = pathlib.Path(__file__).resolve().parents[3]
PROJECT = os.environ['MIRA_COMPOSE_PROJECT']
if not PROJECT.startswith('aihub-'):
    raise ValueError('Use o projeto exclusivo autorizado da sandbox')
COMPOSE = ['docker', 'compose', '-p', PROJECT, '-f', str(pathlib.Path(__file__).with_name('compose.yaml'))]

def run(args, **kwargs):
    """Executa validação e interrompe em falha real."""
    return subprocess.run(args, check=True, text=True, **kwargs)

def sql(query):
    """Consulta somente a base sintética deste Compose pelo container local."""
    return run(COMPOSE + ['exec', '-T', 'mysql', 'mysql', '-uroot', '-plocal-mira-only', '-N', 'mira_test'],
               input=query, capture_output=True).stdout.strip()

LIQUIBASE = ['mvn', '-q', '-f', str(ROOT / 'backend/ads-service/pom.xml'),
             '-Dliquibase.changeLogFile=src/main/resources/db/changelog/changesets/2026-10-01-facebook-metrics-settlement-v1.yaml',
             '-Dliquibase.url=jdbc:mysql://' + os.environ.get('MIRA_TEST_DB_HOST', 'sandbox-docker') + ':33307/mira_test?useSSL=false',
             '-Dliquibase.username=root', '-Dliquibase.password=local-mira-only']
try:
    run(COMPOSE + ['up', '-d', '--wait', '--wait-timeout', '120'])
    sql('''CREATE TABLE experiment(id BIGINT PRIMARY KEY,status VARCHAR(30));
    CREATE TABLE facebook_ads_campaign(id VARCHAR(50) PRIMARY KEY,experiment_id BIGINT,
      metrics_last_synced_at DATETIME(6),metrics_final_synced_at DATETIME(6));
    INSERT INTO experiment VALUES (93,'INVALIDATED'),(91,'USER_STOPPED'),(90,'RUNNING');
    INSERT INTO facebook_ads_campaign VALUES
      ('mira',93,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)),
      ('old',91,UTC_TIMESTAMP(6)-INTERVAL 3 DAY,UTC_TIMESTAMP(6)-INTERVAL 3 DAY),
      ('active',90,UTC_TIMESTAMP(6),NULL);''')
    for _ in range(2):
        run(LIQUIBASE + ['org.liquibase:liquibase-maven-plugin:4.26.0:update'], cwd=ROOT / 'backend/ads-service')
    assert sql("SELECT COUNT(*) FROM facebook_ads_campaign WHERE id='mira' AND metrics_settlement_started_at IS NOT NULL AND metrics_final_synced_at IS NULL") == '1'
    assert sql("SELECT COUNT(*) FROM facebook_ads_campaign WHERE id='old' AND metrics_settlement_started_at IS NULL AND metrics_final_synced_at IS NOT NULL") == '1'
    assert sql("SELECT COUNT(*) FROM facebook_ads_campaign WHERE id='active' AND metrics_settlement_started_at IS NULL") == '1'
    assert sql('SELECT COUNT(*) FROM DATABASECHANGELOG') == '1'
    run(LIQUIBASE + ['-Dliquibase.rollbackCount=1', 'org.liquibase:liquibase-maven-plugin:4.26.0:rollback'], cwd=ROOT / 'backend/ads-service')
    assert sql("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='mira_test' AND table_name='facebook_ads_campaign' AND column_name='metrics_settlement_started_at'") == '0'
    assert sql("SELECT COUNT(*) FROM facebook_ads_campaign WHERE id='mira' AND metrics_final_synced_at=metrics_last_synced_at") == '1'
    run(LIQUIBASE + ['org.liquibase:liquibase-maven-plugin:4.26.0:update'], cwd=ROOT / 'backend/ads-service')
    assert sql("SELECT COUNT(*) FROM facebook_ads_campaign WHERE id='mira' AND metrics_settlement_started_at IS NOT NULL AND metrics_final_synced_at IS NULL") == '1'
    sql('CREATE DATABASE hermes_test')
    analytics_env = dict(os.environ,
        HERMES_TEST_JDBC_URL='jdbc:mysql://' + os.environ.get('MIRA_TEST_DB_HOST', 'sandbox-docker') + ':33307/hermes_test?useSSL=false',
        HERMES_TEST_JDBC_USERNAME='root', HERMES_TEST_JDBC_PASSWORD='local-mira-only')
    run(['mvn', '-q', '-f', str(ROOT / 'backend/ads-service/pom.xml'),
         '-Dtest=PdeExperimentAnalyticsIntegrationTest', 'test'], env=analytics_env)
    print('PASS: MySQL 5.7, aplicação, idempotência, rollback e reaplicação sem alterar campanhas antigas ou ativas.')
finally:
    run(COMPOSE + ['down', '--volumes', '--remove-orphans'])
