"""Valida DDL real no MySQL 5.7 com duas identidades sintéticas e limpeza obrigatória."""
import os
import subprocess
from pathlib import Path
import textwrap

root = Path(__file__).resolve().parents[3]
project = os.environ['PSIQUE_AUDIO_COMPOSE_PROJECT']
assert project.startswith('aihub-'), 'Use o projeto Compose exclusivo da sandbox.'
compose = ['docker', 'compose', '-p', project, '-f', str(Path(__file__).with_name('compose.yaml'))]

def sql(statement):
    return subprocess.run(compose + ['exec', '-T', 'audio-mysql', 'mysql', '-uroot',
                                    '-paudio-local-only', 'psique_audio_test'],
                          input=statement, text=True, capture_output=True, check=True).stdout

try:
    subprocess.run(compose + ['up', '-d', '--wait'], check=True)
    sql('CREATE TABLE agent_task (id BIGINT NOT NULL PRIMARY KEY) ENGINE=InnoDB; INSERT INTO agent_task VALUES (9001),(9002);')
    change = (root/'backend/ads-service/src/main/resources/db/changelog/changesets/2026-10-15-psique-audio-review-v1.yaml').read_text()
    statement = textwrap.dedent(change.split('sql: |', 1)[1].split('      rollback:', 1)[0])
    sql(statement)
    for ident in [9001, 9002]:
        sql(f"INSERT INTO agent_audio_review(task_id,asset_id,product_id,experiment_id,video_sha256,audio_sha256,request_hash,request_json,status,reserved_usd,started_at) VALUES ({ident},9101,{ident},9201,REPEAT('a',64),REPEAT('b',64),REPEAT('c',64),'{{}}','RESERVED',4.116,NOW());")
    assert '2' in sql('SELECT COUNT(*) FROM agent_audio_review;')
    try:
        sql("INSERT INTO agent_audio_review SELECT NULL,task_id,asset_id,product_id,experiment_id,video_sha256,audio_sha256,request_hash,request_json,response_json,result_json,status,error_message,reserved_usd,estimated_usd,started_at,finished_at FROM agent_audio_review WHERE task_id=9001;")
        raise AssertionError('Duplicidade deveria falhar')
    except subprocess.CalledProcessError as ex:
        assert 'Duplicate entry' in ex.stderr
    sql("UPDATE agent_audio_review SET response_json='{\"usage\":{}}', status='COST_PENDING',finished_at=NOW() WHERE task_id=9001;")
    assert 'NULL' in sql('SELECT estimated_usd FROM agent_audio_review WHERE task_id=9001;')
    print('MySQL 5.7: DDL, unicidade, isolamento, custo desconhecido e auditoria aprovados.')
finally:
    subprocess.run(compose + ['down', '--volumes', '--remove-orphans'], check=True)
