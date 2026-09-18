#!/usr/bin/env python3
"""Resolve e comprova a revisao publica vigente do PDE Metodo MUSA."""

from __future__ import annotations

import argparse
from dataclasses import dataclass
import json
from pathlib import Path
import re
import subprocess
import sys
from typing import Any, Callable
from urllib.parse import urljoin
from urllib.request import Request, urlopen

MUSA_PRODUCT_ID = 4
MUSA_PRODUCT_SLUG = "metodo-musa-7-dias"
SHA40 = re.compile(r"^[0-9a-f]{40}$")
SHA256 = re.compile(r"^[0-9a-f]{64}$")


@dataclass(frozen=True)
class MusaPublication:
    """Representa a candidata publicada que deve estar visivel para o cliente."""

    path: str
    contract_version: str
    process_version: int
    experience_version: str
    frontend_version: str
    public_url: str
    source_sha256: str

    @property
    def identity(self) -> tuple[str, ...]:
        """Retorna os campos que mudam quando uma nova candidata deve ser publicada."""

        return (
            self.path,
            self.contract_version,
            str(self.process_version),
            self.experience_version,
            self.frontend_version,
            self.public_url.rstrip("/"),
            self.source_sha256,
        )


def _version_numbers(value: str) -> tuple[int, ...]:
    """Extrai versoes numericas para ordenar contratos sem fixar v8, v12 ou revisao futura."""

    return tuple(int(number) for number in re.findall(r"(?:^|[-.])v(\d+)", value))


def _candidate(path: str, document: dict[str, Any]) -> MusaPublication | None:
    """Converte um contrato elegivel do Metodo MUSA em uma publicacao validada."""

    product = document.get("product")
    publication = document.get("publicationContract")
    if not isinstance(product, dict) or not isinstance(publication, dict):
        return None
    if (
        product.get("id") != MUSA_PRODUCT_ID
        or product.get("slug") != MUSA_PRODUCT_SLUG
        or document.get("status") != "READY_FOR_INDEPENDENT_REVIEW"
        or publication.get("automaticDeployOnMerge") is not True
    ):
        return None

    contract_version = document.get("contractVersion")
    process_version = document.get("processVersion")
    experience_version = product.get("experienceVersion")
    frontend_version = publication.get("frontendVersion")
    public_url = publication.get("publicUrl")
    source_sha256 = publication.get("requiredFrontendSourceSha256")
    if not all(
        isinstance(value, str) and value.strip()
        for value in (
            contract_version,
            experience_version,
            frontend_version,
            public_url,
            source_sha256,
        )
    ):
        raise ValueError(f"Contrato de publicacao MUSA incompleto: {path}")
    if not isinstance(process_version, int) or process_version <= 0:
        raise ValueError(f"Versao de processo MUSA invalida: {path}")
    if not re.fullmatch(r"v[1-9][0-9]*", frontend_version):
        raise ValueError(f"Versao de frontend MUSA invalida: {path}")
    if not public_url.startswith("https://"):
        raise ValueError(f"URL publica MUSA deve usar HTTPS: {path}")
    if not SHA256.fullmatch(source_sha256):
        raise ValueError(f"Fingerprint de fonte MUSA invalido: {path}")

    return MusaPublication(
        path=path,
        contract_version=contract_version,
        process_version=process_version,
        experience_version=experience_version,
        frontend_version=frontend_version,
        public_url=public_url.rstrip("/"),
        source_sha256=source_sha256,
    )


def select_current_publication(documents: dict[str, dict[str, Any]]) -> MusaPublication:
    """Seleciona a candidata MUSA mais recente sem depender do nome da versao atual."""

    candidates = [
        candidate
        for path, document in documents.items()
        if (candidate := _candidate(path, document)) is not None
    ]
    if not candidates:
        raise ValueError("Nenhum manifesto publicavel do Metodo MUSA foi encontrado")

    def order(publication: MusaPublication) -> tuple[Any, ...]:
        return (
            _version_numbers(publication.experience_version),
            publication.process_version,
            _version_numbers(publication.contract_version),
            publication.path,
        )

    return max(candidates, key=order)


def documents_at(repository_root: Path, ref: str) -> dict[str, dict[str, Any]]:
    """Le os contratos PDE exatamente na revisao Git informada."""

    listing = subprocess.run(
        [
            "git",
            "ls-tree",
            "-r",
            "--name-only",
            ref,
            "--",
            "pde-platform/contracts",
        ],
        cwd=repository_root,
        text=True,
        capture_output=True,
        check=True,
    ).stdout.splitlines()
    documents: dict[str, dict[str, Any]] = {}
    for path in listing:
        if not path.endswith(".json"):
            continue
        content = subprocess.run(
            ["git", "show", f"{ref}:{path}"],
            cwd=repository_root,
            text=True,
            capture_output=True,
            check=True,
        ).stdout
        documents[path] = json.loads(content)
    return documents


def publication_at(repository_root: Path, ref: str) -> MusaPublication:
    """Resolve a publicacao MUSA vigente em uma revisao Git."""

    return select_current_publication(documents_at(repository_root, ref))


def publication_changed(repository_root: Path, base: str, head: str) -> bool:
    """Informa se o intervalo Git exige publicar uma nova experiencia MUSA."""

    try:
        previous = publication_at(repository_root, base)
    except ValueError:
        previous = None
    current = publication_at(repository_root, head)
    return previous is None or previous.identity != current.identity


def validate_public_diagnostics(
    publication: MusaPublication,
    health: dict[str, Any],
    diagnostics: dict[str, Any],
) -> str:
    """Confere saude, produto, versao, fonte e commit observados no dominio publico."""

    if health.get("status") != "UP":
        raise ValueError("Health publico do PDE Metodo MUSA nao esta UP")
    expected = {
        "status": "UP",
        "surface": "pde-platform-frontend",
        "version": publication.frontend_version,
        "imageVersionId": publication.frontend_version,
        "publicUrl": publication.public_url,
        "experienceVersion": publication.experience_version,
        "productSlug": MUSA_PRODUCT_SLUG,
        "frontendSourceSha256": publication.source_sha256,
    }
    for field, expected_value in expected.items():
        observed = diagnostics.get(field)
        if field == "publicUrl" and isinstance(observed, str):
            observed = observed.rstrip("/")
        if observed != expected_value:
            raise ValueError(
                f"Diagnostico publico MUSA diverge em {field}: "
                f"esperado={expected_value!r}, observado={observed!r}"
            )
    revision = diagnostics.get("commitSha")
    if not isinstance(revision, str) or not SHA40.fullmatch(revision):
        raise ValueError("Commit publico do PDE Metodo MUSA esta ausente ou invalido")
    return revision


def _read_json(url: str, timeout: float = 15.0) -> dict[str, Any]:
    """Le um documento JSON publico com timeout e identificacao do Watchdog."""

    request = Request(url, headers={"User-Agent": "marketing-hub-production-watchdog/1"})
    with urlopen(request, timeout=timeout) as response:  # noqa: S310 - URL vem de contrato versionado HTTPS
        document = json.loads(response.read().decode("utf-8"))
    if not isinstance(document, dict):
        raise ValueError(f"Resposta publica nao e um objeto JSON: {url}")
    return document


def probe_publication(
    publication: MusaPublication,
    reader: Callable[[str], dict[str, Any]] = _read_json,
) -> str:
    """Consulta o dominio declarado e retorna a revisao comprovada em producao."""

    health = reader(urljoin(publication.public_url + "/", "healthz"))
    diagnostics = reader(
        urljoin(publication.public_url + "/", "version-diagnostics.json")
    )
    return validate_public_diagnostics(publication, health, diagnostics)


def main() -> int:
    """Executa a sonda publica e grava a revisao para o GitHub Actions."""

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository-root", default=".")
    parser.add_argument("--ref", default="HEAD")
    parser.add_argument("--output")
    args = parser.parse_args()

    revision = "MISSING"
    exit_code = 0
    try:
        publication = publication_at(Path(args.repository_root).resolve(), args.ref)
        revision = probe_publication(publication)
        print(
            json.dumps(
                {
                    "status": "UP",
                    "revision": revision,
                    "manifest": publication.path,
                    "publicUrl": publication.public_url,
                    "frontendVersion": publication.frontend_version,
                    "experienceVersion": publication.experience_version,
                },
                ensure_ascii=False,
            )
        )
    except Exception as error:  # a falha deve chegar ao avaliador como revisao nao comprovada
        print(f"Falha ao comprovar PDE Metodo MUSA: {error}", file=sys.stderr)
        exit_code = 1

    line = f"revision={revision}\n"
    if args.output:
        with Path(args.output).open("a", encoding="utf-8") as stream:
            stream.write(line)
    else:
        print(line, end="")
    return exit_code


if __name__ == "__main__":
    raise SystemExit(main())
