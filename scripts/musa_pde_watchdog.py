#!/usr/bin/env python3
"""Resolve e comprova a revisao publica vigente do PDE Metodo MUSA."""

from __future__ import annotations

import argparse
from dataclasses import dataclass
import json
from pathlib import Path
import re
import subprocess
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


@dataclass(frozen=True)
class MusaSurfaceExpectation:
    """Representa uma versão MUSA suportada que o Watchdog precisa comprovar."""

    target: str
    version_id: str
    experience_version: str
    public_url: str
    source_sha256: str | None
    minimum_revision: str | None
    release_contract: str | None


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


def _publications_for_frontend(
    documents: dict[str, dict[str, Any]], frontend_version: str
) -> list[MusaPublication]:
    """Lista manifestos imutáveis de uma única superfície pública."""

    return [
        candidate
        for path, document in documents.items()
        if (candidate := _candidate(path, document)) is not None
        and candidate.frontend_version == frontend_version
    ]


def select_frontend_publication(
    documents: dict[str, dict[str, Any]], frontend_version: str
) -> MusaPublication | None:
    """Seleciona o manifesto mais recente da superfície, quando ela já é moderna."""

    candidates = _publications_for_frontend(documents, frontend_version)
    if not candidates:
        return None
    return max(
        candidates,
        key=lambda publication: (
            _version_numbers(publication.experience_version),
            publication.process_version,
            _version_numbers(publication.contract_version),
            publication.path,
        ),
    )


def select_supported_surfaces(
    documents: dict[str, dict[str, Any]],
) -> list[MusaSurfaceExpectation]:
    """Resolve todas as versões MUSA suportadas sem eleger apenas a mais nova."""

    inventory_path = "pde-platform/contracts/product-runtime-isolation-v1.json"
    inventory = documents.get(inventory_path)
    if not isinstance(inventory, dict):
        raise ValueError("Inventário operacional das superfícies PDE não foi encontrado")
    products = [
        product
        for product in inventory.get("products", [])
        if product.get("productId") == MUSA_PRODUCT_ID
        and product.get("productSlug") == MUSA_PRODUCT_SLUG
    ]
    if len(products) != 1:
        raise ValueError("Inventário deve conter exatamente um produto Método MUSA")

    expectations: list[MusaSurfaceExpectation] = []
    for surface in products[0].get("surfaces", []):
        if (
            surface.get("lifecycleStatus") != "SUPPORTED"
            or surface.get("watchdogRequired") is not True
        ):
            continue
        target = surface.get("deployTarget")
        version_id = surface.get("versionId")
        experience_version = surface.get("experienceVersion")
        public_url = surface.get("publicUrl")
        if not all(
            isinstance(value, str) and value.strip()
            for value in (target, version_id, experience_version, public_url)
        ):
            raise ValueError("Superfície MUSA suportada possui identidade incompleta")

        publication = select_frontend_publication(documents, target)
        source_sha256 = None
        release_contract = None
        minimum_revision = surface.get("legacyMinimumRevision")
        if publication is not None:
            if (
                publication.experience_version != experience_version
                or publication.public_url.rstrip("/") != public_url.rstrip("/")
            ):
                raise ValueError(
                    f"Manifesto e inventário divergem para a superfície {target}"
                )
            source_sha256 = publication.source_sha256
            release_contract = publication.path
            minimum_revision = None
        elif not isinstance(minimum_revision, str) or not SHA40.fullmatch(
            minimum_revision
        ):
            raise ValueError(
                f"Superfície legada {target} não possui revisão mínima verificável"
            )

        expectations.append(
            MusaSurfaceExpectation(
                target=target,
                version_id=version_id,
                experience_version=experience_version,
                public_url=public_url.rstrip("/"),
                source_sha256=source_sha256,
                minimum_revision=minimum_revision,
                release_contract=release_contract,
            )
        )

    if not expectations:
        raise ValueError("Nenhuma superfície suportada do Método MUSA foi encontrada")
    targets = [expectation.target for expectation in expectations]
    if len(targets) != len(set(targets)):
        raise ValueError("Inventário MUSA contém target de Watchdog duplicado")
    return sorted(expectations, key=lambda expectation: _version_numbers(expectation.target))


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


def publication_changed_for_frontend(
    repository_root: Path, base: str, head: str, frontend_version: str
) -> bool:
    """Detecta promoção pendente somente para a versão pública informada."""

    previous = select_frontend_publication(
        documents_at(repository_root, base), frontend_version
    )
    current = select_frontend_publication(
        documents_at(repository_root, head), frontend_version
    )
    if previous is None and current is None:
        return False
    if previous is None or current is None:
        return True
    return previous.identity != current.identity


def supported_surfaces_at(
    repository_root: Path, ref: str
) -> list[MusaSurfaceExpectation]:
    """Resolve o conjunto completo de superfícies MUSA suportadas em uma revisão Git."""

    return select_supported_surfaces(documents_at(repository_root, ref))


def validate_surface_diagnostics(
    expectation: MusaSurfaceExpectation,
    health: dict[str, Any],
    diagnostics: dict[str, Any],
) -> str:
    """Confere a identidade pública de uma superfície moderna ou legada suportada."""

    if health.get("status") != "UP":
        raise ValueError(
            f"Health público da superfície MUSA {expectation.target} não está UP"
        )
    expected: dict[str, Any] = {
        "status": "UP",
        "surface": "pde-platform-frontend",
        "version": expectation.version_id,
        "imageVersionId": expectation.version_id,
        "publicUrl": expectation.public_url,
        "experienceVersion": expectation.experience_version,
        "productSlug": MUSA_PRODUCT_SLUG,
    }
    if expectation.source_sha256 is not None:
        expected["frontendSourceSha256"] = expectation.source_sha256
    for field, expected_value in expected.items():
        observed = diagnostics.get(field)
        if field == "publicUrl" and isinstance(observed, str):
            observed = observed.rstrip("/")
        if observed != expected_value:
            raise ValueError(
                f"Diagnóstico MUSA {expectation.target} diverge em {field}: "
                f"esperado={expected_value!r}, observado={observed!r}"
            )
    revision = diagnostics.get("commitSha")
    if not isinstance(revision, str) or not SHA40.fullmatch(revision):
        raise ValueError(
            f"Commit público da superfície MUSA {expectation.target} está ausente ou inválido"
        )
    return revision


def validate_public_diagnostics(
    publication: MusaPublication,
    health: dict[str, Any],
    diagnostics: dict[str, Any],
) -> str:
    """Confere saude, produto, versao, fonte e commit observados no dominio publico."""

    return validate_surface_diagnostics(
        MusaSurfaceExpectation(
            target=publication.frontend_version,
            version_id=publication.frontend_version,
            experience_version=publication.experience_version,
            public_url=publication.public_url,
            source_sha256=publication.source_sha256,
            minimum_revision=None,
            release_contract=publication.path,
        ),
        health,
        diagnostics,
    )


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


def probe_surface(
    expectation: MusaSurfaceExpectation,
    reader: Callable[[str], dict[str, Any]] = _read_json,
) -> str:
    """Consulta uma superfície suportada e devolve a revisão realmente observada."""

    health = reader(urljoin(expectation.public_url + "/", "healthz"))
    diagnostics = reader(
        urljoin(expectation.public_url + "/", "version-diagnostics.json")
    )
    return validate_surface_diagnostics(expectation, health, diagnostics)


def revision_satisfies_minimum(
    repository_root: Path, minimum_revision: str, observed_revision: str
) -> bool:
    """Comprova que uma superfície legada não regrediu abaixo da revisão conhecida."""

    for revision in (minimum_revision, observed_revision):
        exists = subprocess.run(
            ["git", "cat-file", "-e", f"{revision}^{{commit}}"],
            cwd=repository_root,
            capture_output=True,
            check=False,
        )
        if exists.returncode != 0:
            return False
    return (
        subprocess.run(
            ["git", "merge-base", "--is-ancestor", minimum_revision, observed_revision],
            cwd=repository_root,
            capture_output=True,
            check=False,
        ).returncode
        == 0
    )


def probe_supported_surfaces(
    expectations: list[MusaSurfaceExpectation],
    repository_root: Path,
    reader: Callable[[str], dict[str, Any]] = _read_json,
    minimum_validator: Callable[[Path, str, str], bool] = revision_satisfies_minimum,
) -> list[dict[str, Any]]:
    """Sonda todas as versões atendidas e preserva um diagnóstico por superfície."""

    results: list[dict[str, Any]] = []
    for expectation in expectations:
        result: dict[str, Any] = {
            "target": expectation.target,
            "versionId": expectation.version_id,
            "experienceVersion": expectation.experience_version,
            "publicUrl": expectation.public_url,
            "releaseContract": expectation.release_contract,
            "minimumRevision": expectation.minimum_revision,
            "expectedSourceSha256": expectation.source_sha256,
        }
        try:
            revision = probe_surface(expectation, reader)
            if expectation.minimum_revision is not None and not minimum_validator(
                repository_root, expectation.minimum_revision, revision
            ):
                raise ValueError(
                    f"Revisão {revision} de {expectation.target} é anterior ou "
                    f"incompatível com o mínimo {expectation.minimum_revision}"
                )
            result.update(status="UP", revision=revision)
        except Exception as error:  # cada versão deve continuar aparecendo no relatório
            result.update(status="ERROR", revision="MISSING", error=str(error))
        results.append(result)
    return results


def main() -> int:
    """Executa a sonda de todas as versões MUSA atendidas e grava o relatório."""

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository-root", default=".")
    parser.add_argument("--ref", default="HEAD")
    parser.add_argument("--output")
    parser.add_argument("--report")
    args = parser.parse_args()

    repository_root = Path(args.repository_root).resolve()
    try:
        results = probe_supported_surfaces(
            supported_surfaces_at(repository_root, args.ref), repository_root
        )
        status = "UP" if all(item["status"] == "UP" for item in results) else "ERROR"
        document = {"status": status, "surfaces": results}
        exit_code = 0 if status == "UP" else 1
    except Exception as error:  # erro de contrato também precisa produzir evidência auditável
        document = {"status": "ERROR", "surfaces": [], "error": str(error)}
        exit_code = 1

    rendered = json.dumps(document, ensure_ascii=False, indent=2)
    print(rendered)
    if args.report:
        Path(args.report).write_text(rendered + "\n", encoding="utf-8")
    line = f"status={document['status']}\n"
    if args.output:
        with Path(args.output).open("a", encoding="utf-8") as stream:
            stream.write(line)
    else:
        print(line, end="")
    return exit_code


if __name__ == "__main__":
    raise SystemExit(main())
