#!/usr/bin/env python3
"""Reproduz downloads incompletos e garante promoção íntegra antes do deploy."""

import contextlib
import copy
import hashlib
import importlib.util
import io
from pathlib import Path
import stat
import subprocess
import tempfile
import unittest
import warnings
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("run_artifact", ROOT / "scripts/download-run-artifact.py")
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


def package(files):
    """Produz ZIP real sem conexão externa para cada cenário de integridade."""
    output = io.BytesIO()
    with zipfile.ZipFile(output, "w") as archive:
        for name, content in files.items():
            archive.writestr(name, content)
    return output.getvalue()


class RunArtifactDownloadTest(unittest.TestCase):
    """Valida o contrato reutilizável com arquivos, ZIP, digest e identidades distintos."""

    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.destination = self.root / "backend"
        self.files = {"backend-image.tar": b"backend fixture", "process-worker-image.tar": b"worker fixture"}
        self.payload = package(self.files)
        self.run_id, self.revision = 101, "a" * 40
        self.artifact = {"id": 501, "name": "backend-image", "expired": False,
                         "digest": "sha256:" + hashlib.sha256(self.payload).hexdigest(),
                         "workflow_run": {"id": self.run_id, "head_sha": self.revision}}
        self.transfers, self.delays = [], []

    def execute(self, payloads=None, required=None, artifact=None, name="backend-image", destination=None):
        """Executa o downloader real contra respostas locais do gh e ZIPs reais."""
        payloads = payloads or [self.payload]
        selected = artifact or self.artifact

        def transfer(endpoint, target):
            self.transfers.append(endpoint)
            data = payloads[min(len(self.transfers) - 1, len(payloads) - 1)]
            if isinstance(data, Exception):
                raise data
            target.write_bytes(data)

        with contextlib.redirect_stderr(io.StringIO()):
            return MODULE.download("fixture/repository", self.run_id, self.revision, name,
                                   destination or self.destination, required or list(self.files),
                                   metadata=lambda _: {"artifacts": [selected]},
                                   transfer=transfer, pause=self.delays.append)

    def test_complete_package_and_another_run_have_same_contract(self):
        for run_id, revision, artifact_id, name in [(101, "a" * 40, 501, "backend-image"),
                                                     (202, "b" * 40, 902, "different-package")]:
            with self.subTest(run_id=run_id):
                self.run_id, self.revision = run_id, revision
                self.artifact.update(id=artifact_id, name=name,
                                     workflow_run={"id": run_id, "head_sha": revision})
                self.transfers.clear()
                target = self.root / str(run_id)
                result = self.execute(name=name, destination=target)
                self.assertEqual(result["runId"], run_id)
                self.assertEqual(result["revision"], revision)
                self.assertEqual(result["attempts"], 1)
                self.assertEqual(len(self.transfers), 1)
                for file, content in self.files.items():
                    self.assertEqual((target / file).read_bytes(), content)

    def test_partial_download_recovers_same_immutable_artifact(self):
        result = self.execute([self.payload[:len(self.payload) // 2], self.payload])
        self.assertEqual(result["attempts"], 2)
        self.assertEqual(self.transfers, ["repos/fixture/repository/actions/artifacts/501/zip"] * 2)
        self.assertEqual(self.delays, [10])
        self.assertEqual((self.destination / "process-worker-image.tar").read_bytes(), self.files["process-worker-image.tar"])

    def test_timeout_recovers_without_promoting_partial_output(self):
        result = self.execute([subprocess.TimeoutExpired("gh", 180), self.payload])
        self.assertEqual(result["attempts"], 2)
        self.assertTrue(self.destination.is_dir())

    def test_missing_or_empty_worker_never_promotes_a_successful_zip(self):
        for worker in [None, b""]:
            with self.subTest(worker=worker):
                files = {"backend-image.tar": b"backend fixture"}
                if worker is not None:
                    files["process-worker-image.tar"] = worker
                data = package(files)
                artifact = copy.deepcopy(self.artifact)
                artifact["digest"] = "sha256:" + hashlib.sha256(data).hexdigest()
                self.transfers.clear()
                with self.assertRaisesRegex(ValueError, "Arquivo obrigatório ausente ou vazio"):
                    self.execute([data], artifact=artifact)
                self.assertEqual(len(self.transfers), 3)
                self.assertFalse(self.destination.exists())
                self.assertEqual(list(self.root.iterdir()), [])

    def test_persistent_digest_mismatch_is_bounded(self):
        with self.assertRaisesRegex(ValueError, "Digest"):
            self.execute([self.payload[:-10]])
        self.assertEqual(len(self.transfers), 3)
        self.assertEqual(self.delays, [10, 20])
        self.assertFalse(self.destination.exists())

    def test_corrupt_crc_is_rejected_even_with_matching_archive_digest(self):
        data = self.payload.replace(b"backend fixture", b"backend invalid", 1)
        artifact = copy.deepcopy(self.artifact)
        artifact["digest"] = "sha256:" + hashlib.sha256(data).hexdigest()
        with self.assertRaises((ValueError, zipfile.BadZipFile)):
            self.execute([data], artifact=artifact)
        self.assertFalse(self.destination.exists())

    def test_invalid_zip_with_matching_digest_still_fails(self):
        data = b"incomplete archive with trusted transport digest"
        artifact = copy.deepcopy(self.artifact)
        artifact["digest"] = "sha256:" + hashlib.sha256(data).hexdigest()
        with self.assertRaises(zipfile.BadZipFile):
            self.execute([data], artifact=artifact)
        self.assertEqual(len(self.transfers), 3)
        self.assertFalse(self.destination.exists())

    def test_existing_verified_destination_is_preserved(self):
        self.destination.mkdir()
        original = self.destination / "verified.tar"
        original.write_bytes(b"previous verified package")
        with self.assertRaisesRegex(ValueError, "preservado"):
            self.execute()
        self.assertEqual(original.read_bytes(), b"previous verified package")
        self.assertEqual(self.transfers, [])

    def test_frontend_and_backend_have_independent_destinations(self):
        self.execute()
        data = package({"frontend-image.tar": b"frontend fixture"})
        artifact = copy.deepcopy(self.artifact)
        artifact.update(name="frontend-image", digest="sha256:" + hashlib.sha256(data).hexdigest())
        self.transfers.clear()
        result = self.execute([data], required=["frontend-image.tar"], artifact=artifact,
                              name="frontend-image", destination=self.root / "frontend")
        self.assertEqual(result["files"], {"frontend-image.tar": 16})
        self.assertTrue((self.destination / "process-worker-image.tar").exists())

    def test_wrong_or_expired_identity_never_downloads(self):
        for change in [{"expired": True}, {"digest": ""}, {"id": True},
                       {"workflow_run": {"id": 102, "head_sha": self.revision}},
                       {"workflow_run": {"id": self.run_id, "head_sha": "b" * 40}}]:
            with self.subTest(change=change):
                artifact = copy.deepcopy(self.artifact)
                artifact.update(change)
                with self.assertRaises(ValueError):
                    self.execute(artifact=artifact)
                self.assertEqual(self.transfers, [])

    def test_missing_or_ambiguous_artifact_never_changes_origin(self):
        for artifacts in [[], [self.artifact, self.artifact]]:
            with self.subTest(count=len(artifacts)):
                with self.assertRaisesRegex(ValueError, "ausente ou ambíguo"):
                    MODULE.download("fixture/repository", self.run_id, self.revision, "backend-image",
                                    self.destination, list(self.files),
                                    metadata=lambda _: {"artifacts": artifacts})

    def test_paths_links_and_duplicate_entries_never_escape_staging(self):
        for name in ["../outside", "/outside", "folder\\outside", "./backend-image.tar"]:
            with self.subTest(name=name):
                data = package({name: b"unsafe"})
                artifact = copy.deepcopy(self.artifact)
                artifact["digest"] = "sha256:" + hashlib.sha256(data).hexdigest()
                with self.assertRaisesRegex(ValueError, "caminho inseguro"):
                    self.execute([data], artifact=artifact)
                self.assertFalse((self.root / "outside").exists())
        output = io.BytesIO()
        with zipfile.ZipFile(output, "w") as archive:
            link = zipfile.ZipInfo("backend-image.tar")
            link.external_attr = (stat.S_IFLNK | 0o777) << 16
            archive.writestr(link, "../outside")
        artifact = copy.deepcopy(self.artifact)
        artifact["digest"] = "sha256:" + hashlib.sha256(output.getvalue()).hexdigest()
        with self.assertRaisesRegex(ValueError, "link"):
            self.execute([output.getvalue()], artifact=artifact)

    def test_duplicate_zip_entry_never_overwrites_a_required_file(self):
        output = io.BytesIO()
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", UserWarning)
            with zipfile.ZipFile(output, "w") as archive:
                archive.writestr("backend-image.tar", "first")
                archive.writestr("backend-image.tar", "overwritten")
        data = output.getvalue()
        artifact = copy.deepcopy(self.artifact)
        artifact["digest"] = "sha256:" + hashlib.sha256(data).hexdigest()
        with self.assertRaisesRegex(ValueError, "duplicado"):
            self.execute([data], artifact=artifact)
        self.assertFalse(self.destination.exists())

    def test_workflow_checks_packages_before_any_ssh(self):
        workflow = (ROOT / ".github/workflows/deploy-containers.yml").read_text()
        deploy = workflow[workflow.index("  deploy-app:"):] if "  deploy-app:" in workflow else workflow[workflow.index("    name: Deploy backend + frontend"):]
        self.assertLess(deploy.index("download-run-artifact.py"), deploy.index("name: Add SSH key"))
        self.assertIn("--required backend-image.tar --required process-worker-image.tar", deploy)
        self.assertIn("--destination artifacts/backend", deploy)
        self.assertIn("--destination artifacts/frontend", deploy)
        self.assertIn("artifacts/backend/process-worker-image.tar", deploy)
        self.assertIn("artifacts/frontend/frontend-image.tar", deploy)
        for file in [".github/workflows/backend-ci.yml", ".github/workflows/deploy-containers.yml"]:
            self.assertIn("python3 scripts/test-download-run-artifact.py", (ROOT / file).read_text())


if __name__ == "__main__":
    unittest.main()
