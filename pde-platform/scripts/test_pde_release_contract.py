#!/usr/bin/env python3
"""Testes unitários do contrato de promoção e rollback das superfícies PDE."""

from __future__ import annotations

import importlib.util
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parent.parent.parent
SPEC = importlib.util.spec_from_file_location(
    "pde_release_contract", ROOT / "pde-platform/scripts/pde_release_contract.py"
)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


class PdeReleaseContractTest(unittest.TestCase):
    def setUp(self):
        self.inventory = MODULE.load_object(
            ROOT / "pde-platform/contracts/product-runtime-isolation-v1.json"
        )

    def test_inventory_resolves_every_supported_surface_once(self):
        MODULE.validate_inventory(self.inventory)
        self.assertEqual(
            {surface["deployTarget"] for surface in MODULE.surfaces(self.inventory)},
            {"v5", "v6", "v7", "v8", "mira", "kit-whatsapp"},
        )

    def test_diagnostics_bind_target_image_commit_and_source(self):
        surface = MODULE.select_surface(self.inventory, "v8")
        diagnostics = {
            "status": "UP",
            "surface": "pde-platform-frontend",
            "version": "v8",
            "imageVersionId": "v8",
            "publicUrl": "https://v8.clubemusa.com.br/",
            "experienceVersion": "musa-pde-entry-v12-primeiro-ajuste-aplicavel",
            "productSlug": "metodo-musa-7-dias",
            "image": "registry/v8:" + "a" * 40,
            "commitSha": "a" * 40,
            "frontendSourceSha256": "b" * 64,
        }
        MODULE.validate_diagnostics(
            surface,
            diagnostics,
            "registry/v8:" + "a" * 40,
            "a" * 40,
            "b" * 64,
        )

    def test_diagnostics_reject_another_build(self):
        surface = MODULE.select_surface(self.inventory, "v8")
        with self.assertRaisesRegex(ValueError, "frontendSourceSha256"):
            MODULE.validate_diagnostics(
                surface,
                {
                    "status": "UP",
                    "surface": "pde-platform-frontend",
                    "version": "v8",
                    "imageVersionId": "v8",
                    "publicUrl": "https://v8.clubemusa.com.br",
                    "experienceVersion": "musa-pde-entry-v12-primeiro-ajuste-aplicavel",
                    "productSlug": "metodo-musa-7-dias",
                    "image": "registry/v8:" + "a" * 40,
                    "commitSha": "a" * 40,
                    "frontendSourceSha256": "c" * 64,
                },
                "registry/v8:" + "a" * 40,
                "a" * 40,
                "b" * 64,
            )

    def test_release_contract_binds_product_target_and_source(self):
        surface = MODULE.select_surface(self.inventory, "v8")
        source = "b" * 64
        contract = {
            "contractVersion": "musa-v12-commercial-homologation.v6",
            "status": "READY_FOR_INDEPENDENT_REVIEW",
            "product": {
                "id": 4,
                "slug": "metodo-musa-7-dias",
                "experienceVersion": "musa-pde-entry-v12-primeiro-ajuste-aplicavel",
                "publicUrl": "https://v8.clubemusa.com.br/",
            },
            "publicationContract": {
                "automaticDeployOnMerge": True,
                "frontendVersion": "v8",
                "publicUrl": "https://v8.clubemusa.com.br/",
                "requiredFrontendSourceSha256": source,
            },
            "liveVisualContract": {
                "runtimeIdentity": {
                    "version": "v8",
                    "experienceVersion": "musa-pde-entry-v12-primeiro-ajuste-aplicavel",
                    "frontendSourceSha256": source,
                }
            },
        }
        MODULE.validate_release_contract(surface, contract, source)
        contract["publicationContract"]["frontendVersion"] = "v7"
        with self.assertRaisesRegex(ValueError, "publication.frontendVersion"):
            MODULE.validate_release_contract(surface, contract, source)

    def test_musa_rejects_inventory_as_release_contract(self):
        with self.assertRaisesRegex(ValueError, "manifesto imutável"):
            MODULE.validate_release_contract(
                MODULE.select_surface(self.inventory, "v8"), self.inventory, "b" * 64
            )

    def test_non_musa_temporarily_accepts_supported_inventory_contract(self):
        MODULE.validate_release_contract(
            MODULE.select_surface(self.inventory, "mira"), self.inventory, "b" * 64
        )

    def test_rollback_requires_the_exact_previous_identity(self):
        before = {
            "status": "UP",
            "surface": "pde-platform-frontend",
            "version": "v8",
            "imageVersionId": "v8",
            "publicUrl": "https://v8.clubemusa.com.br",
            "experienceVersion": "musa-pde-entry-v12-primeiro-ajuste-aplicavel",
            "productSlug": "metodo-musa-7-dias",
            "image": "registry/v8:old",
            "imageTag": "d" * 40,
            "commitSha": "d" * 40,
            "frontendSourceSha256": "e" * 64,
        }
        MODULE.validate_rollback(before, dict(before))
        changed = dict(before, imageTag="f" * 40)
        with self.assertRaisesRegex(ValueError, "imageTag"):
            MODULE.validate_rollback(before, changed)


if __name__ == "__main__":
    unittest.main(verbosity=2)
