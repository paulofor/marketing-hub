#!/usr/bin/env python3
"""Homologa a atualização dos nove agentes sem dados reais, inferência ou acesso produtivo."""
import json
import os
import pathlib
import subprocess

ROOT = pathlib.Path(__file__).resolve().parents[3]
PROJECT = os.environ["AGENT_RUNTIME_COMPOSE_PROJECT"]
if not PROJECT.startswith("aihub-"):
    raise ValueError("Use o projeto Compose exclusivo autorizado.")
COMPOSE = ["docker", "compose", "-p", PROJECT, "-f", str(pathlib.Path(__file__).with_name("compose.yaml"))]
CONTRACT = json.loads((ROOT / "config/agents/codex-agent-health-compliance.json").read_text())
AGENTS = CONTRACT["agents"]
LIQUIBASE = [
    "mvn", "-q", "-f", str(ROOT / "backend/ads-service/pom.xml"),
    "-Dliquibase.changeLogFile=src/main/resources/db/changelog/changesets/2026-10-01-agents-codex-gpt61-v1.yaml",
    "-Dliquibase.url=jdbc:mysql://" + os.environ.get("AGENT_RUNTIME_DB_HOST", "sandbox-docker") + ":33308/agent_runtime_test?useSSL=false",
    "-Dliquibase.username=root", "-Dliquibase.password=local-agent-runtime-only",
]
PLUGIN = "org.liquibase:liquibase-maven-plugin:4.26.0:"


def run(args, **kwargs):
    """Executa somente a topologia local e interrompe em falha real."""
    return subprocess.run(args, check=True, text=True, **kwargs)


def sql(query):
    """Lê e grava exclusivamente na base sintética desta homologação."""
    return run(COMPOSE + ["exec", "-T", "mysql", "mysql", "-uroot", "-plocal-agent-runtime-only",
                          "--default-character-set=utf8mb4", "-N", "agent_runtime_test"],
               input=query, capture_output=True).stdout.strip()


def update():
    """Aplica a migração real pelo Liquibase e suas precondições."""
    return subprocess.run(LIQUIBASE + [PLUGIN + "update"], cwd=ROOT / "backend/ads-service",
                          check=False, text=True, capture_output=True)


def require_update():
    """Exige sucesso e preserva o diagnóstico completo de uma falha inesperada."""
    result = update()
    assert result.returncode == 0, result.stdout + result.stderr


def rollback():
    """Retira a ativação sem apagar snapshots que possam ter execuções vinculadas."""
    run(LIQUIBASE + ["-Dliquibase.rollbackCount=1", PLUGIN + "rollback"], cwd=ROOT / "backend/ads-service")


try:
    run(COMPOSE + ["up", "-d", "--wait", "--wait-timeout", "120"])
    sql("""CREATE TABLE agent(id BIGINT PRIMARY KEY,agent_key VARCHAR(80) UNIQUE,
      current_version INT NOT NULL,model_name VARCHAR(255),updated_at DATETIME NOT NULL);
      CREATE TABLE agent_version(id BIGINT AUTO_INCREMENT PRIMARY KEY,agent_id BIGINT NOT NULL,
      version_number INT NOT NULL,contract_snapshot LONGTEXT NOT NULL,created_at DATETIME NOT NULL,
      UNIQUE KEY uq_agent_version(agent_id,version_number),
      FOREIGN KEY(agent_id) REFERENCES agent(id));""")
    for index, agent in enumerate(AGENTS, 1):
        old_version = agent["expectedVersion"] - 1
        key = agent["key"]
        snapshot = json.dumps({"agentKey": key, "model": "gpt-5.6-sol", "version": old_version,
                               "inputs": [{"card": "synthetic-card-preserved"}],
                               "authority": "SYNTHETIC_READ_ONLY", "prompt": "synthetic-prompt"})
        sql(f"INSERT INTO agent VALUES ({index},'{key}',{old_version},'gpt-5.6-sol',UTC_TIMESTAMP());"
            f"INSERT INTO agent_version(agent_id,version_number,contract_snapshot,created_at)"
            f" VALUES ({index},{old_version},'{snapshot}',UTC_TIMESTAMP());")
    sql("INSERT INTO agent VALUES (99,'synthetic-unrelated',1,'other-model',UTC_TIMESTAMP());")
    historical = sql("SELECT agent_id,version_number,SHA2(contract_snapshot,256) FROM agent_version ORDER BY agent_id;")

    # A colisão não pode sobrescrever outra versão nem avançar qualquer agente parcialmente.
    target = AGENTS[0]["expectedVersion"]
    sql(f"INSERT INTO agent_version(agent_id,version_number,contract_snapshot,created_at)"
        f" VALUES (1,{target},'{{\"sourceChangeSet\":\"another-owner\"}}',UTC_TIMESTAMP());")
    refused = update()
    assert refused.returncode != 0 and "precondition" in (refused.stdout + refused.stderr).lower()
    assert sql("SELECT COUNT(*) FROM agent WHERE model_name='gpt-6.1-sol'") == "0"
    sql(f"DELETE FROM agent_version WHERE agent_id=1 AND version_number={target};")

    require_update()
    require_update()
    assert sql("SELECT COUNT(*) FROM agent WHERE model_name='gpt-6.1-sol'") == "9"
    assert sql("SELECT COUNT(*) FROM agent_version") == "18"
    assert sql("SELECT COUNT(*) FROM DATABASECHANGELOG") == "1"
    assert sql("SELECT model_name FROM agent WHERE id=99") == "other-model"
    for index, agent in enumerate(AGENTS, 1):
        expected = agent["expectedVersion"]
        assert sql(f"SELECT current_version FROM agent WHERE id={index}") == str(expected)
        assert sql(f"SELECT JSON_UNQUOTE(JSON_EXTRACT(contract_snapshot,'$.codexVersion'))"
                   f" FROM agent_version WHERE agent_id={index} AND version_number={expected}") == CONTRACT["codexVersion"]
        assert sql(f"SELECT JSON_UNQUOTE(JSON_EXTRACT(contract_snapshot,'$.inputs[0].card'))"
                   f" FROM agent_version WHERE agent_id={index} AND version_number={expected}") == "synthetic-card-preserved"
    assert sql("SELECT agent_id,version_number,SHA2(contract_snapshot,256) FROM agent_version"
               " WHERE JSON_EXTRACT(contract_snapshot,'$.sourceChangeSet') IS NULL ORDER BY agent_id;") == historical
    rollback()
    assert sql("SELECT COUNT(*) FROM agent WHERE model_name='gpt-5.6-sol'") == "9"
    assert sql("SELECT COUNT(*) FROM agent_version") == "18"
    require_update()
    assert sql("SELECT COUNT(*) FROM agent_version") == "18"

    # Uma evolução posterior não pode regredir quando a atualização desta entrega for retirada.
    sql(f"INSERT INTO agent_version(agent_id,version_number,contract_snapshot,created_at)"
        f" VALUES (1,{target+1},'{{\"sourceChangeSet\":\"future-owner\"}}',UTC_TIMESTAMP());"
        f"UPDATE agent SET current_version={target+1},model_name='future-model' WHERE id=1;")
    rollback()
    assert sql("SELECT model_name FROM agent WHERE id=1") == "future-model"
    assert sql("SELECT COUNT(*) FROM agent WHERE model_name='gpt-5.6-sol'") == "8"
    assert update().returncode != 0
    print("PASS: nove agentes, cards/histórico preservados, colisão bloqueada, idempotência, rollback e proteção de evolução posterior.")
finally:
    run(COMPOSE + ["down", "--volumes", "--remove-orphans"])
