#!/usr/bin/env python3
"""Valida a revisão congelada da recuperação antes de qualquer teste, build ou publicação."""

import importlib.util
import os
from pathlib import Path
import re
import sys

from deploy_publisher_recovery import POLICY, successful_application


def validate(github, environment):
    """Impede drift de main, base ilegítima e agente sem aplicação aprovada."""
    expected = environment.get("RECOVERY_SHA", "")
    if not expected:
        return
    if (not re.fullmatch(r"[a-f0-9]{40}", expected)
            or environment.get("GITHUB_SHA") != expected
            or environment.get("GITHUB_REF") != "refs/heads/main"
            or environment.get("GITHUB_EVENT_NAME") != "workflow_dispatch"
            or environment.get("GITHUB_REPOSITORY") != "paulofor/marketing-hub"):
        raise ValueError("Recuperação rejeitada: evento, repositório, branch ou revisão divergente.")
    workflow = environment["GITHUB_WORKFLOW_REF"].split("@", 1)[0].rsplit("/", 1)[-1]
    if workflow not in POLICY:
        raise ValueError("Publicador fora do catálogo de recuperação automática.")
    recovery_base_input = POLICY[workflow].get("recovery_base_input")
    if recovery_base_input:
        base = environment.get("RECOVERY_BASE_SHA", "")
        if not re.fullmatch(r"[a-f0-9]{40}", base):
            raise ValueError("Recuperação rejeitada: base protegida ausente ou inválida.")
        comparison = github.api(f"compare/{base}...{expected}")
        if not isinstance(comparison, dict) or comparison.get("status") not in {"ahead", "identical"}:
            raise ValueError("Recuperação rejeitada: base protegida não pertence ao histórico da revisão.")
    if POLICY[workflow].get("requires_app") and not successful_application(github, expected, "1970-01-01T00:00:00+00:00"):
        raise ValueError("Recuperação do agente exige deploy da aplicação aprovado na mesma revisão.")


def main():
    """Reutiliza a API autenticada, preservando secrets fora dos diagnósticos."""
    spec = importlib.util.spec_from_file_location("coordination", Path(__file__).with_name("coordinate-deploy-intervention.py"))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    try:
        validate(module.GitHub(), os.environ)
    except (ValueError, RuntimeError, KeyError) as error:
        print(str(error), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
