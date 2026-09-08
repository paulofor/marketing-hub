#!/usr/bin/env python3
"""Bloqueia publicação de JAR com recursos externos ausentes, inclusive em sparse checkout."""

import argparse
import re
import subprocess
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path
from zipfile import ZipFile

NS = {"m": "http://maven.apache.org/POM/4.0.0"}


def matches(path, pattern):
    expression = re.escape(pattern).replace(r"\*\*/", "(?:.*/)?")
    expression = expression.replace(r"\*", "[^/]*").replace(r"\?", "[^/]")
    return re.fullmatch(expression, path) is not None


def verify(repo, jar):
    pom = ET.parse(repo / "backend/ads-service/pom.xml")
    tracked = subprocess.check_output(
        ["git", "-C", str(repo), "ls-files", "-z"], text=True
    ).split("\0")
    checked = 0
    with ZipFile(jar) as archive:
        entries = set(archive.namelist())
        for resource in pom.findall("m:build/m:resources/m:resource", NS):
            target = resource.findtext("m:targetPath", namespaces=NS)
            if not target:
                continue
            patterns = [node.text for node in resource.findall("m:includes/m:include", NS)]
            expected = [path for path in tracked if any(matches(path, p) for p in patterns)]
            if not expected:
                raise ValueError(f"Nenhum recurso versionado para {target}")
            for path in expected:
                source = repo / path
                entry = f"BOOT-INF/classes/{target}/{path}"
                if not source.is_file():
                    raise ValueError(f"Recurso omitido no checkout: {path}")
                if entry not in entries:
                    raise ValueError(f"Recurso ausente no JAR: {entry}")
                if archive.read(entry) != source.read_bytes():
                    raise ValueError(f"Recurso divergente no JAR: {entry}")
                checked += 1
    return checked


def smoke(repo, jar):
    with tempfile.TemporaryDirectory(prefix="backend-package-smoke-") as work:
        subprocess.run([
            "javac", "-d", work, str(repo / "scripts/BackendResearchPackageSmoke.java")
        ], check=True)
        subprocess.run([
            "java", f"-Dloader.path={work}", "-Dloader.main=BackendResearchPackageSmoke",
            "-cp", str(jar), "org.springframework.boot.loader.launch.PropertiesLauncher"
        ], check=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--jar", type=Path)
    args = parser.parse_args()
    repo = args.repo.resolve()
    jar = (args.jar or repo / "backend/ads-service/target/app-exec.jar").resolve()
    print(f"Recursos externos íntegros: {verify(repo, jar)}", flush=True)
    smoke(repo, jar)


if __name__ == "__main__":
    main()
