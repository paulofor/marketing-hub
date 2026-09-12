#!/usr/bin/env python3
"""Confere o JAR efetivamente incorporado à imagem, sem iniciar container ou serviço."""
import hashlib
import json
import subprocess
import sys
import tarfile
import tempfile
from pathlib import Path


def digest(stream):
    value = hashlib.sha256()
    for chunk in iter(lambda: stream.read(1024 * 1024), b""):
        value.update(chunk)
    return value.hexdigest()


def verify_archive(image_archive, expected_hash):
    found = None
    with tarfile.open(image_archive) as archive:
        manifests = json.load(archive.extractfile("manifest.json"))
        if len(manifests) != 1:
            raise ValueError("A conferência exige exatamente uma imagem.")
        for layer in manifests[0]["Layers"]:
            with tarfile.open(fileobj=archive.extractfile(layer), mode="r|*") as content:
                for member in content:
                    name = member.name.removeprefix("./")
                    if name in ("app/.wh.app.jar", "app/.wh..wh..opq"):
                        found = None
                    if name == "app/app.jar":
                        if not member.isfile():
                            raise ValueError("O JAR da imagem não é um arquivo regular.")
                        found = digest(content.extractfile(member))
    if found != expected_hash:
        raise ValueError(f"JAR da imagem diverge do pacote conferido: esperado={expected_hash}; recebido={found}")
    return found


def main():
    jar = Path("backend/ads-service/target/app-exec.jar")
    with jar.open("rb") as source:
        expected = digest(source)
    output = Path("artifacts/vega-process-recovery")
    output.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="image-jar-", dir=output) as directory:
        archive = Path(directory) / "image.tar"
        subprocess.run(["docker", "image", "save", "-o", str(archive), sys.argv[1]], check=True)
        print("JAR da imagem conferido: " + verify_archive(archive, expected))


if __name__ == "__main__":
    main()
