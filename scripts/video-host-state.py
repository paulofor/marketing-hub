#!/usr/bin/env python3
"""Transfere somente estado recuperável do VPS retirado; não publica código nem inicia serviços."""
import argparse
import hashlib
import io
import json
import os
from pathlib import Path, PurePosixPath
import shutil
import subprocess
import sys
import tarfile
import tempfile

CONTAINERS = ["marketinghub-video-management", "mois-meta-ad-library-collector",
              "mois-hotmart-collector", "mois-service", "mds", "oprm-worker"]
ROOTS = ["root/infra", "opt/marketinghub", "root/oprm"]
ARCHIVE_DIRECTORY = "var/backups/marketinghub/retired-177-153-62-107"
SECRET_MAP = {
    f"root/infra/{provider}-token/{provider}_api_key":
        f"root/infra/apollo-secrets/{provider}_api_key"
    for provider in ["openai", "gemini", "luma", "kling", "heygen"]
}
SECRET_MAP["root/infra/runaway-token/runaway_api_key"] = "root/infra/apollo-secrets/runway_api_key"
PRESERVED_PREFIXES = ["root/infra/apollo-codex-home/", "root/infra/marketplace-credentials/hotmart/",
                      "opt/marketinghub/containers/volumes/video-management/logs/",
                      "opt/marketinghub/mois-hotmart-collector/logs/"]
META_ENV = "opt/marketinghub/mois-meta-ad-library-collector/.env"


def add_bytes(archive, name, data):
    """Insere estado sensível em arquivo restrito sem reproduzi-lo em diagnóstico."""
    info = tarfile.TarInfo(name)
    info.size, info.mode = len(data), 0o600
    archive.addfile(info, io.BytesIO(data))


def export_state(root, output, containers):
    """Exige executores parados e exporta arquivos regulares e configuração de recuperação."""
    if any(item["State"]["Running"] for item in containers):
        raise ValueError("Existe executor ativo; drenar e parar antes de copiar estado.")
    with tarfile.open(fileobj=output, mode="w|gz", dereference=True) as archive:
        for prefix in ROOTS:
            directory = root / prefix
            if not directory.exists():
                continue
            for path in sorted(directory.rglob("*")):
                if path.is_file() and not path.is_symlink():
                    name = path.relative_to(root).as_posix()
                    if name != META_ENV:
                        archive.add(path, arcname=name, recursive=False)
        add_bytes(archive, "runtime-containers.json", json.dumps(containers).encode())
        for item in containers:
            log_path = item.get("LogPath", "")
            if log_path.startswith("/var/lib/docker/containers/"):
                path = root / log_path.lstrip("/")
                if path.is_file() and not path.is_symlink():
                    archive.add(path, arcname=path.relative_to(root).as_posix(), recursive=False)
            if item["Name"].lstrip("/") == "mois-meta-ad-library-collector":
                env = dict(entry.split("=", 1) for entry in item["Config"]["Env"] if "=" in entry)
                token = env.get("META_AD_LIBRARY_ACCESS_TOKEN", "")
                if not token or any(ch in token for ch in "\r\n\x00"):
                    raise ValueError("Credencial dedicada do coletor ausente ou inválida para transporte.")
                add_bytes(archive, META_ENV, ("RADAR_META_TOKEN=" + token + "\n").encode())


def destination_for(name):
    """Restringe restauração a dados e segredos próprios, sem copiar código ou arquivos dos PDEs."""
    parts = PurePosixPath(name)
    if parts.is_absolute() or ".." in parts.parts:
        raise ValueError("Caminho inseguro no backup.")
    if name in SECRET_MAP:
        return SECRET_MAP[name]
    if name == META_ENV or any(name.startswith(prefix) for prefix in PRESERVED_PREFIXES):
        return name
    return None


def restore_state(root, archive_path):
    """Valida todos os destinos antes de criar arquivos; divergência preserva estado existente."""
    selected = []
    digest = hashlib.sha256()
    with tarfile.open(archive_path, "r:gz") as archive:
        for member in archive:
            target_name = destination_for(member.name)
            if target_name is None:
                continue
            if not member.isfile():
                raise ValueError("Backup contém link ou tipo não permitido.")
            target = root / target_name
            if any(parent.is_symlink() for parent in [target, *target.parents]):
                raise ValueError("Destino contém link simbólico.")
            data = archive.extractfile(member).read()
            sha = hashlib.sha256(data).hexdigest()
            if target.exists() and (not target.is_file() or hashlib.sha256(target.read_bytes()).hexdigest() != sha):
                raise ValueError("Destino possui conteúdo diferente; nenhuma substituição automática.")
            selected.append((target, data))
            digest.update(target_name.encode() + b"\0" + sha.encode())
    if not all(root / name in [path for path, _ in selected] for name in SECRET_MAP.values()):
        raise ValueError("Backup incompleto: não restaurar sem todos os segredos de Apolo.")
    for path, data in selected:
        path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
        if not path.exists():
            with path.open("xb") as stream:
                stream.write(data)
            path.chmod(0o600)
    return {"restored_files": len(selected), "state_manifest_sha256": digest.hexdigest()}


def receive(root, stream):
    """Guarda backup completo restrito no destino antes de restaurar estado selecionado."""
    directory = root / ARCHIVE_DIRECTORY
    directory.mkdir(parents=True, exist_ok=True, mode=0o700)
    directory.chmod(0o700)
    if shutil.disk_usage(directory).free < 2 * 1024 ** 3:
        raise ValueError("Menos de 2 GiB livres para backup e recuperação.")
    with tempfile.NamedTemporaryFile(dir=directory, prefix="state-", suffix=".tar.gz", delete=False) as output:
        os.fchmod(output.fileno(), 0o600)
        shutil.copyfileobj(stream, output)
        path = Path(output.name)
    checksum = hashlib.sha256(path.read_bytes()).hexdigest()
    result = restore_state(root, path)
    result.update(archive=str(path), archive_sha256=checksum, bytes=path.stat().st_size)
    return result


def main():
    """Opera por stdin/stdout binários, sem credenciais em argumentos ou mensagens."""
    parser = argparse.ArgumentParser()
    parser.add_argument("operation", choices=["export", "receive"])
    args = parser.parse_args()
    os.umask(0o077)
    if args.operation == "export":
        data = json.loads(subprocess.check_output(["docker", "inspect", *CONTAINERS]))
        export_state(Path("/"), sys.stdout.buffer, data)
    else:
        print(json.dumps(receive(Path("/"), sys.stdin.buffer)))


if __name__ == "__main__":
    main()
