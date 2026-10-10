#!/usr/bin/env python3
"""Publica um executor somente após a execução corrente liberar seu lock durável."""

import argparse
import datetime
import fcntl
import json
import os
import pathlib
import re
import subprocess
import sys
import time
import uuid


def container_state(container, mount, relative):
    """Resolve apenas o volume persistente do consumidor informado pelo Docker."""
    result = subprocess.run(["docker", "inspect", container], check=True, capture_output=True, text=True)
    entries = json.loads(result.stdout)
    volumes = [m for m in entries[0]["Mounts"] if m["Destination"] == mount and m["Type"] == "volume" and m["RW"]]
    if len(volumes) != 1:
        raise RuntimeError("Volume durável exclusivo do consumidor não identificado.")
    root = pathlib.Path(volumes[0]["Source"]).resolve()
    state = (root / relative).resolve()
    if not state.is_relative_to(root) or state == root:
        raise RuntimeError("Diretório do consumidor fora do volume autorizado.")
    return state


def write_pause(state, revision):
    """Preserva a proteção e seu proprietário mesmo se o publicador for interrompido."""
    marker = state / "publisher-pause.json"
    if marker.is_symlink():
        raise RuntimeError("Proteção de publicação não pode ser um link simbólico.")
    previous = json.loads(marker.read_text()) if marker.exists() else None
    value = {"publicationId": str(uuid.uuid4()), "revision": revision,
             "pausedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
             "previousRevision": previous.get("revision") if previous else None}
    temporary = state / ("publisher-pause-" + value["publicationId"] + ".tmp")
    with temporary.open("x") as output:
        json.dump(value, output)
        output.flush()
        os.fsync(output.fileno())
        if os.geteuid() == 0:
            owner = state.stat()
            os.fchown(output.fileno(), owner.st_uid, owner.st_gid)
    os.chmod(temporary, 0o600)
    temporary.replace(marker)
    return marker, value


def publish(state, revision, wait_seconds, command):
    """Aguarda o lock Java, mantém exclusão durante a troca e só retoma após sucesso."""
    if not re.fullmatch(r"[0-9a-f]{40}", revision):
        raise RuntimeError("A publicação exige SHA imutável de 40 caracteres.")
    if not state.is_dir():
        raise RuntimeError("Diretório durável do consumidor não existe; não publicar.")
    marker, identity = write_pause(state, revision)
    lock_path = state / "consumer.lock"
    if lock_path.is_symlink():
        raise RuntimeError("Lock do consumidor não pode ser um link simbólico.")
    with lock_path.open("a+b") as lock:
        if os.geteuid() == 0:
            owner = state.stat()
            os.fchown(lock.fileno(), owner.st_uid, owner.st_gid)
        deadline = time.monotonic() + wait_seconds
        while True:
            try:
                fcntl.lockf(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
                break
            except BlockingIOError:
                if time.monotonic() >= deadline:
                    raise TimeoutError("Execução corrente ainda ativa; imagem preservada e pausa mantida.")
                time.sleep(min(1, max(0.01, deadline - time.monotonic())))
        print("Consumidor ocioso e protegido; iniciando publicação validada.", flush=True)
        result = subprocess.run(command)
        if result.returncode:
            print("Publicação incompleta; proteção persistente mantida.", file=sys.stderr)
            return result.returncode
        if not marker.exists() or json.loads(marker.read_text()) != identity:
            raise RuntimeError("Proteção mudou durante a publicação; não liberar outra pausa.")
        marker.unlink()
        print("Imagem e saúde validadas; novas reservas liberadas.", flush=True)
        return 0


def main():
    """Recebe o volume operacional ou diretório de teste e um comando versionado de publicação."""
    parser = argparse.ArgumentParser(description=__doc__)
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument("--container")
    source.add_argument("--state-directory", type=pathlib.Path)
    parser.add_argument("--mount", default="/var/lib/atena")
    parser.add_argument("--relative-directory", default="bpm-market-strategy")
    parser.add_argument("--revision", required=True)
    parser.add_argument("--wait-seconds", type=float, default=3600)
    parser.add_argument("command", nargs=argparse.REMAINDER)
    args = parser.parse_args()
    command = args.command[1:] if args.command[:1] == ["--"] else args.command
    if not command or args.wait_seconds < 0:
        parser.error("Informe o comando e um prazo não negativo.")
    state = args.state_directory or container_state(args.container, args.mount, args.relative_directory)
    return publish(state, args.revision, args.wait_seconds, command)


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as error:
        print("Publicação bloqueada: " + str(error), file=sys.stderr)
        sys.exit(1)
