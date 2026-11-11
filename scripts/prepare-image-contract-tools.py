#!/usr/bin/env python3
"""Prepara o scanner oficial versionado no diretório temporário do runner, sem APT."""

import hashlib
import io
import os
import platform
import shutil
import subprocess
import tarfile
import urllib.request
from pathlib import Path

VERSION = "14.1.1"
ARCHIVE = f"ripgrep-{VERSION}-x86_64-unknown-linux-musl"
URL = f"https://github.com/BurntSushi/ripgrep/releases/download/{VERSION}/{ARCHIVE}.tar.gz"
SHA256 = "4cf9f2741e6c465ffdb7c26f38056a59e2a2544b51f7cc128ef28337eeae4d8e"


def install(destination, download=urllib.request.urlopen):
    """Confere integridade antes de extrair somente o executável esperado, sem executar o arquivo recebido."""
    if platform.system() != "Linux" or platform.machine() not in ("x86_64", "amd64"):
        raise RuntimeError("O scanner fixado requer runner Linux x86_64.")
    with download(URL, timeout=60) as response:
        payload = response.read(20 * 1024 * 1024 + 1)
    if hashlib.sha256(payload).hexdigest() != SHA256:
        raise RuntimeError("SHA-256 divergente no scanner oficial; validação interrompida.")
    with tarfile.open(fileobj=io.BytesIO(payload), mode="r:gz") as archive:
        member = archive.getmember(f"{ARCHIVE}/rg")
        if not member.isfile() or member.size > 20 * 1024 * 1024:
            raise RuntimeError("Executável do scanner inválido; validação interrompida.")
        binary = archive.extractfile(member).read()
    destination = Path(destination)
    destination.mkdir(parents=True, exist_ok=True)
    target = destination / "rg"
    temporary = destination / "rg.tmp"
    temporary.write_bytes(binary)
    temporary.chmod(0o755)
    temporary.replace(target)
    return target


def prepare(destination, github_path, which=shutil.which, installer=install):
    """Reutiliza ferramenta existente ou expõe a versão verificada somente no PATH do job."""
    present = which("rg")
    if present:
        return Path(present)
    target = installer(destination)
    subprocess.run([str(target), "--version"], check=True, timeout=10, capture_output=True)
    with Path(github_path).open("a", encoding="utf-8") as output:
        output.write(str(target.parent.resolve()) + "\n")
    return target


if __name__ == "__main__":
    prepare(Path(os.environ["RUNNER_TEMP"]) / "canonical-image-tools", os.environ["GITHUB_PATH"])
