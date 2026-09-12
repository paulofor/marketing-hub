#!/usr/bin/env python3
"""Mantém o registro operacional de intervenções sob lock exclusivo, fora do diretório de deploy."""

import fcntl
from datetime import datetime
import json
import os
from pathlib import Path
import re
import sys
import tempfile


def validate_state(state):
    """Recusa estado incompleto em vez de interpretar corrupção como ausência de proteção."""
    if not isinstance(state, dict) or state.get("schema") != 1:
        raise ValueError("Registro de intervenção inválido; requer recuperação operacional.")
    if not re.fullmatch(r"[a-f0-9]{32}", state.get("id", "")):
        raise ValueError("Identificador de intervenção inválido.")
    if state.get("phase") not in {"DRAINING", "ACTIVE", "OPERATING", "AWAITING_MERGE", "RESUMING", "RELEASED"}:
        raise ValueError("Estado de intervenção desconhecido.")
    for field in ("repository", "owner", "reason", "authorization", "protected_version", "initial_sha"):
        if not isinstance(state.get(field), str) or not state[field].strip():
            raise ValueError("Metadados obrigatórios da intervenção ausentes.")
    if not state.get("workflows") or not isinstance(state.get("history"), list):
        raise ValueError("Escopo ou histórico da intervenção ausente.")
    automatic = state.get("automatic_resume")
    if automatic is not None:
        if (not isinstance(automatic, dict)
                or not re.fullmatch(r"[a-f0-9]{40}", automatic.get("validated_commit", ""))
                or not isinstance(automatic.get("evidence"), str) or not automatic["evidence"].strip()
                or not isinstance(automatic.get("prepared_at"), str) or not automatic["prepared_at"].strip()):
            raise ValueError("Preparação de retomada automática inválida.")
        if datetime.fromisoformat(automatic["prepared_at"].replace("Z", "+00:00")).tzinfo is None:
            raise ValueError("Preparação de retomada exige data com fuso horário.")
    if state.get("phase") == "AWAITING_MERGE" and automatic is None:
        raise ValueError("Espera de merge sem comprovação de homologação.")


def atomic_write(path, payload):
    """Substitui um registro completo e sincroniza a gravação antes de confirmar ao operador."""
    descriptor, temporary = tempfile.mkstemp(prefix=".state-", dir=path.parent)
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8") as stream:
            json.dump(payload, stream, ensure_ascii=False, indent=2)
            stream.write("\n")
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(temporary, path)
        directory = os.open(path.parent, os.O_RDONLY)
        try:
            os.fsync(directory)
        finally:
            os.close(directory)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def serve(directory, incoming=sys.stdin, outgoing=sys.stdout):
    """Atende uma sessão JSON por linha; a desconexão libera o lock, preservando a pausa."""
    directory = Path(directory)
    directory.mkdir(mode=0o700, parents=True, exist_ok=True)
    if directory.is_symlink():
        raise ValueError("Diretório de controle não pode ser um link simbólico.")
    os.chmod(directory, 0o700)
    state_path = directory / "current.json"
    with (directory / "operator.lock").open("a") as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            outgoing.write(json.dumps({"error": "Outro operador está coordenando os deploys."}) + "\n")
            outgoing.flush()
            return
        for line in incoming:
            try:
                request = json.loads(line)
                current = json.loads(state_path.read_text()) if state_path.exists() else None
                if current is not None:
                    validate_state(current)
                if request["op"] == "save":
                    new = request["state"]
                    validate_state(new)
                    if current and current["id"] != new["id"] and current["phase"] != "RELEASED":
                        raise ValueError("Já existe uma intervenção não encerrada.")
                    # O snapshot histórico nunca é removido ao abrir a próxima intervenção.
                    atomic_write(directory / (new["id"] + ".json"), new)
                    atomic_write(state_path, new)
                    response = {"saved": True}
                elif request["op"] == "load":
                    response = {"state": current}
                else:
                    raise ValueError("Operação de controle desconhecida.")
            except (ValueError, KeyError, OSError) as error:
                response = {"error": str(error)}
            outgoing.write(json.dumps(response, ensure_ascii=False) + "\n")
            outgoing.flush()


if __name__ == "__main__":
    serve(sys.argv[1])
