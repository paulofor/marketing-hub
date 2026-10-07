#!/usr/bin/env python3
"""Baixa um artefato imutável do run e promove somente sua extração íntegra."""

import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import stat
import subprocess
import sys
import tempfile
import time
import zipfile


def github_json(endpoint):
    """Consulta metadados pela autenticação do gh sem manipular credenciais."""
    return json.loads(subprocess.check_output(["gh", "api", endpoint], text=True))


def github_archive(endpoint, destination):
    """Conclui o download em arquivo temporário antes de qualquer extração."""
    with destination.open("wb") as output:
        subprocess.run(["gh", "api", endpoint], stdout=output, check=True, timeout=180)


def download(repository, run_id, revision, name, destination, required,
             metadata=github_json, transfer=github_archive, pause=time.sleep):
    """Confere identidade, digest, ZIP e arquivos, recuperando no máximo três downloads."""
    destination = Path(destination).resolve()
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repository or ""):
        raise ValueError("Informe o repositório do run.")
    if type(run_id) is not int or run_id <= 0 or not re.fullmatch(r"[a-f0-9]{40}", revision or ""):
        raise ValueError("Informe run e revisão completos.")
    if not name or not required or len(set(required)) != len(required):
        raise ValueError("Informe artefato e arquivos obrigatórios distintos.")
    if any(not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]*", item) for item in required):
        raise ValueError("Arquivos obrigatórios devem ser nomes simples.")
    if destination == destination.parent or (destination.exists() and
            (not destination.is_dir() or any(destination.iterdir()))):
        raise ValueError("Use um destino exclusivo e vazio; pacote existente será preservado.")
    response = metadata(f"repos/{repository}/actions/runs/{run_id}/artifacts?per_page=100")
    candidates = [a for a in response["artifacts"] if a["name"] == name]
    if len(candidates) != 1:
        raise ValueError("Artefato ausente ou ambíguo no run; não escolher outra origem.")
    artifact = candidates[0]
    identity = artifact.get("workflow_run", {})
    digest = artifact.get("digest", "")
    if (artifact.get("expired") is not False or identity.get("id") != run_id
            or identity.get("head_sha") != revision
            or not re.fullmatch(r"sha256:[a-f0-9]{64}", digest)
            or type(artifact.get("id")) is not int or artifact["id"] <= 0):
        raise ValueError("Artefato sem identidade, revisão ou digest válido do run esperado.")
    endpoint = f"repos/{repository}/actions/artifacts/{artifact['id']}/zip"
    destination.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="artifact-download-", dir=destination.parent) as temporary:
        stage = Path(temporary)
        archive, extracted = stage / "artifact.zip", stage / "extracted"
        for attempt in range(1, 4):
            try:
                shutil.rmtree(extracted, ignore_errors=True)
                transfer(endpoint, archive)
                with archive.open("rb") as stream:
                    actual = hashlib.file_digest(stream, "sha256").hexdigest()
                if actual != digest[7:]:
                    raise ValueError("Digest do download diverge do artefato imutável.")
                with zipfile.ZipFile(archive) as package:
                    names = set()
                    for item in package.infolist():
                        path = PurePosixPath(item.filename)
                        if (path.is_absolute() or ".." in path.parts or "\\" in item.filename
                                or ":" in item.filename or item.filename in names
                                or path.as_posix() != item.filename.rstrip("/")
                                or stat.S_ISLNK(item.external_attr >> 16)):
                            raise ValueError("ZIP contém caminho inseguro, duplicado ou link.")
                        names.add(item.filename)
                    if package.testzip() is not None:
                        raise ValueError("CRC inválido no ZIP do artefato.")
                    package.extractall(extracted)
                for filename in required:
                    file = extracted / filename
                    if not file.is_file() or file.stat().st_size == 0:
                        raise ValueError(f"Arquivo obrigatório ausente ou vazio: {filename}.")
                if destination.exists():
                    destination.rmdir()
                extracted.replace(destination)
                return {"runId": run_id, "revision": revision, "artifactId": artifact["id"],
                        "artifactName": name, "digest": digest, "attempts": attempt,
                        "files": {f: (destination / f).stat().st_size for f in required}}
            except (OSError, ValueError, RuntimeError, zipfile.BadZipFile, subprocess.SubprocessError) as error:
                print(json.dumps({"runId": run_id, "artifactId": artifact["id"],
                                  "attempt": attempt, "error": str(error)}, ensure_ascii=False),
                      file=sys.stderr)
                if attempt == 3:
                    raise
                pause(attempt * 10)


def main():
    """Aplica o contrato ao run atual do workflow, sem escolher revisões alternativas."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--run-id", type=int, default=os.environ.get("GITHUB_RUN_ID"))
    parser.add_argument("--repository", default=os.environ.get("GITHUB_REPOSITORY"))
    parser.add_argument("--revision", default=os.environ.get("GITHUB_SHA"))
    parser.add_argument("--name", required=True)
    parser.add_argument("--destination", required=True)
    parser.add_argument("--required", action="append", required=True)
    args = parser.parse_args()
    try:
        result = download(args.repository, args.run_id or 0, args.revision, args.name,
                          args.destination, args.required)
        print(json.dumps(result, ensure_ascii=False))
        return 0
    except (OSError, ValueError, KeyError, RuntimeError, zipfile.BadZipFile, subprocess.SubprocessError) as error:
        print(json.dumps({"runId": args.run_id, "revision": args.revision, "artifactName": args.name,
                          "error": str(error), "packageVerified": False}, ensure_ascii=False),
              file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
