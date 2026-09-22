#!/usr/bin/env python3
"""Valida a migração com Liquibase real e remove a topologia sintética ao encerrar."""

import os
from pathlib import Path
import subprocess

root = Path(__file__).resolve().parents[3]
project = os.environ["PDE_PRINCIPLES_COMPOSE_PROJECT"]
compose = ["docker", "compose", "-p", project, "-f", str(Path(__file__).with_name("compose.yaml"))]

for command in (["docker", "version"], ["docker", "buildx", "version"], ["docker", "compose", "version"]):
    subprocess.run(command, check=True)
try:
    subprocess.run(compose + ["up", "-d", "--wait"], check=True)
    subprocess.run(
        ["mvn", "-B", "-Dtest=PdeCommercialPrinciplesMysql57Test", "test"],
        cwd=root / "backend/ads-service",
        env={**os.environ, "PDE_PRINCIPLES_MYSQL": "true"},
        check=True,
    )
finally:
    subprocess.run(compose + ["down", "--volumes", "--remove-orphans"], check=True)
