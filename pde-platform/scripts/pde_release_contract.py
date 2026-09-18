#!/usr/bin/env python3
"""Valida o inventário e a identidade imutável de uma publicação PDE."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import re
import sys
from typing import Any

SHA40 = re.compile(r"^[0-9a-f]{40}$")
SHA256 = re.compile(r"^[0-9a-f]{64}$")


def load_object(path: Path) -> dict[str, Any]:
    """Lê um objeto JSON ou interrompe com uma mensagem operacional curta."""

    document = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(document, dict):
        raise ValueError(f"Documento JSON não é objeto: {path}")
    return document


def surfaces(inventory: dict[str, Any]) -> list[dict[str, Any]]:
    """Expande produtos e superfícies preservando a identidade do produto pai."""

    result: list[dict[str, Any]] = []
    for product in inventory.get("products", []):
        for surface in product.get("surfaces", []):
            result.append(
                {
                    **surface,
                    "productId": product.get("productId"),
                    "productSlug": product.get("productSlug"),
                }
            )
    return result


def select_surface(inventory: dict[str, Any], target: str) -> dict[str, Any]:
    """Resolve exatamente uma superfície pelo target oficial de deploy."""

    matches = [item for item in surfaces(inventory) if item.get("deployTarget") == target]
    if len(matches) != 1:
        raise ValueError(
            f"Target PDE deve resolver exatamente uma superfície: {target} ({len(matches)})"
        )
    return matches[0]


def diagnostic_surface(surface: dict[str, Any]) -> str:
    """Informa a identidade pública esperada pelo diagnóstico do container."""

    if surface["deployTarget"] == "mira":
        return "pde-platform-frontend-mira"
    return "pde-platform-frontend"


def validate_inventory(inventory: dict[str, Any]) -> None:
    """Garante que targets e identidades operacionais não se sobreponham."""

    items = surfaces(inventory)
    required = (
        "deployTarget",
        "versionId",
        "experienceVersion",
        "lifecycleStatus",
        "serviceName",
        "containerName",
        "imageName",
        "imageVariable",
        "portVariable",
        "hostPort",
        "publicUrl",
        "backendProbePath",
    )
    for item in items:
        missing = [field for field in required if item.get(field) in (None, "")]
        if missing:
            raise ValueError(
                f"Superfície {item.get('deployTarget', '?')} incompleta: {', '.join(missing)}"
            )
    for field in (
        "deployTarget",
        "serviceName",
        "containerName",
        "imageName",
        "imageVariable",
        "portVariable",
        "hostPort",
        "publicUrl",
    ):
        values = [item[field] for item in items]
        if len(values) != len(set(values)):
            raise ValueError(f"Campo operacional duplicado no inventário: {field}")


def validate_diagnostics(
    surface: dict[str, Any],
    diagnostics: dict[str, Any],
    expected_image: str,
    expected_commit: str,
    expected_source: str,
) -> None:
    """Comprova que o runtime corresponde ao target e ao artefato promovido."""

    expected = {
        "status": "UP",
        "surface": diagnostic_surface(surface),
        "version": surface["versionId"],
        "imageVersionId": surface["versionId"],
        "publicUrl": surface["publicUrl"],
        "experienceVersion": surface["experienceVersion"],
        "productSlug": surface["productSlug"],
        "image": expected_image,
        "commitSha": expected_commit,
        "frontendSourceSha256": expected_source,
    }
    for field, value in expected.items():
        observed = diagnostics.get(field)
        if field == "publicUrl" and isinstance(observed, str):
            observed = observed.rstrip("/")
        if observed != value:
            raise ValueError(
                f"Diagnóstico {surface['deployTarget']} diverge em {field}: "
                f"esperado={value!r}, observado={observed!r}"
            )
    if not SHA40.fullmatch(expected_commit):
        raise ValueError("Commit esperado da publicação PDE deve ser SHA-1 completo")
    if not SHA256.fullmatch(expected_source):
        raise ValueError("Fingerprint esperado da publicação PDE deve ser SHA-256")


def validate_release_contract(
    surface: dict[str, Any], contract: dict[str, Any], expected_source: str
) -> None:
    """Vincula o target ao manifesto comercial ou ao inventário transitório permitido."""

    if not SHA256.fullmatch(expected_source):
        raise ValueError("Fingerprint esperado da publicação PDE deve ser SHA-256")
    if contract.get("contractVersion") == "pde-product-runtime-isolation.v1":
        if surface.get("productId") == 4:
            raise ValueError(
                "Superfície MUSA exige manifesto imutável próprio para publicação"
            )
        declared = select_surface(contract, surface["deployTarget"])
        fields = (
            "versionId",
            "experienceVersion",
            "serviceName",
            "containerName",
            "imageVariable",
            "publicUrl",
        )
        for field in fields:
            if declared.get(field) != surface.get(field):
                raise ValueError(
                    f"Inventário transitório diverge em {field} para "
                    f"{surface['deployTarget']}"
                )
        if declared.get("lifecycleStatus") != "SUPPORTED":
            raise ValueError("Inventário transitório não autoriza superfície descontinuada")
        return

    product = contract.get("product")
    publication = contract.get("publicationContract")
    live_visual = contract.get("liveVisualContract")
    runtime = (
        live_visual.get("runtimeIdentity") if isinstance(live_visual, dict) else None
    )
    if not all(isinstance(value, dict) for value in (product, publication, runtime)):
        raise ValueError("Manifesto de publicação PDE não contém identidade completa")
    expected = {
        "status": "READY_FOR_INDEPENDENT_REVIEW",
        "product.id": surface["productId"],
        "product.slug": surface["productSlug"],
        "product.experienceVersion": surface["experienceVersion"],
        "product.publicUrl": surface["publicUrl"],
        "publication.frontendVersion": surface["deployTarget"],
        "publication.publicUrl": surface["publicUrl"],
        "publication.requiredFrontendSourceSha256": expected_source,
        "runtime.version": surface["versionId"],
        "runtime.experienceVersion": surface["experienceVersion"],
        "runtime.frontendSourceSha256": expected_source,
    }
    observed = {
        "status": contract.get("status"),
        "product.id": product.get("id"),
        "product.slug": product.get("slug"),
        "product.experienceVersion": product.get("experienceVersion"),
        "product.publicUrl": product.get("publicUrl"),
        "publication.frontendVersion": publication.get("frontendVersion"),
        "publication.publicUrl": publication.get("publicUrl"),
        "publication.requiredFrontendSourceSha256": publication.get(
            "requiredFrontendSourceSha256"
        ),
        "runtime.version": runtime.get("version"),
        "runtime.experienceVersion": runtime.get("experienceVersion"),
        "runtime.frontendSourceSha256": runtime.get("frontendSourceSha256"),
    }
    if publication.get("automaticDeployOnMerge") is not True:
        raise ValueError("Manifesto não autoriza publicação automática")
    for field, value in expected.items():
        actual = observed[field]
        if field.endswith("publicUrl") and isinstance(actual, str):
            actual = actual.rstrip("/")
            value = str(value).rstrip("/")
        if actual != value:
            raise ValueError(
                f"Manifesto diverge em {field}: esperado={value!r}, observado={actual!r}"
            )


def validate_rollback(before: dict[str, Any], after: dict[str, Any]) -> None:
    """Comprova que um rollback restaurou a identidade observada antes da troca."""

    fields = (
        "status",
        "surface",
        "version",
        "imageVersionId",
        "publicUrl",
        "experienceVersion",
        "productSlug",
        "image",
        "imageTag",
        "commitSha",
        "frontendSourceSha256",
    )
    for field in fields:
        if before.get(field) != after.get(field):
            raise ValueError(
                f"Rollback não restaurou {field}: antes={before.get(field)!r}, "
                f"depois={after.get(field)!r}"
            )


def emit_surface(surface: dict[str, Any]) -> None:
    """Emite os campos operacionais em TSV para consumo seguro pelo Bash."""

    fields = (
        "serviceName",
        "containerName",
        "imageVariable",
        "portVariable",
        "hostPort",
        "publicUrl",
        "experienceVersion",
        "productSlug",
        "backendProbePath",
    )
    values = [str(surface[field]) for field in fields]
    if any("\t" in value or "\n" in value for value in values):
        raise ValueError("Inventário PDE contém separador inválido")
    print("\t".join(values))


def main() -> int:
    """Executa o subcomando solicitado pelo publicador ou pelos testes."""

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--inventory",
        default=str(
            Path(__file__).resolve().parent.parent
            / "contracts"
            / "product-runtime-isolation-v1.json"
        ),
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    surface_parser = subparsers.add_parser("surface")
    surface_parser.add_argument("--target", required=True)

    validate_parser = subparsers.add_parser("validate-diagnostics")
    validate_parser.add_argument("--target", required=True)
    validate_parser.add_argument("--diagnostics", required=True)
    validate_parser.add_argument("--expected-image", required=True)
    validate_parser.add_argument("--expected-commit", required=True)
    validate_parser.add_argument("--expected-source", required=True)

    release_parser = subparsers.add_parser("validate-release")
    release_parser.add_argument("--target", required=True)
    release_parser.add_argument("--contract", required=True)
    release_parser.add_argument("--expected-source", required=True)

    rollback_parser = subparsers.add_parser("validate-rollback")
    rollback_parser.add_argument("--before", required=True)
    rollback_parser.add_argument("--after", required=True)

    subparsers.add_parser("validate-inventory")
    args = parser.parse_args()
    inventory = load_object(Path(args.inventory))
    validate_inventory(inventory)

    if args.command == "validate-inventory":
        return 0
    if args.command == "surface":
        emit_surface(select_surface(inventory, args.target))
        return 0
    if args.command == "validate-diagnostics":
        validate_diagnostics(
            select_surface(inventory, args.target),
            load_object(Path(args.diagnostics)),
            args.expected_image,
            args.expected_commit,
            args.expected_source,
        )
        return 0
    if args.command == "validate-release":
        validate_release_contract(
            select_surface(inventory, args.target),
            load_object(Path(args.contract)),
            args.expected_source,
        )
        return 0
    if args.command == "validate-rollback":
        validate_rollback(
            load_object(Path(args.before)), load_object(Path(args.after))
        )
        return 0
    raise AssertionError(f"Subcomando não tratado: {args.command}")


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, ValueError, json.JSONDecodeError) as error:
        print(f"[ARQUITETURA] {error}", file=sys.stderr)
        raise SystemExit(1) from error
