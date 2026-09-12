#!/usr/bin/env python3
"""Reproduz perda e divergência dos recursos obrigatórios antes da publicação."""
import importlib.util
import subprocess
import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile

spec = importlib.util.spec_from_file_location(
    "packaged_resources", Path(__file__).with_name("verify-backend-packaged-resources.py")
)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class PackagedResourcesTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.repo = Path(self.temp.name)
        subprocess.run(["git", "init", "-q", str(self.repo)], check=True)
        pom = self.repo / "backend/ads-service/pom.xml"
        pom.parent.mkdir(parents=True)
        pom.write_text('''<project xmlns="http://maven.apache.org/POM/4.0.0"><build><resources>
        <resource><targetPath>research-library</targetPath><includes>
        <include>pesquisas/**/*.md</include></includes></resource>
        </resources></build></project>''')
        self.source = self.repo / "pesquisas/video/artigo.md"
        self.source.parent.mkdir(parents=True)
        self.source.write_text("Artigo obrigatório")
        (self.repo / "pesquisas/README.md").write_text("Índice")
        subprocess.run(["git", "-C", str(self.repo), "add", "."], check=True)
        self.jar = self.repo / "app-exec.jar"
        self.package()

    def package(self, omit=False, changed=False):
        with ZipFile(self.jar, "w") as archive:
            for source in (self.repo / "pesquisas").rglob("*.md"):
                if omit and source == self.source:
                    continue
                data = b"obsoleto" if changed and source == self.source else source.read_bytes()
                archive.writestr("BOOT-INF/classes/research-library/" + str(source.relative_to(self.repo)), data)

    def test_complete_package(self):
        self.assertEqual(2, module.verify(self.repo, self.jar))

    def test_sparse_checkout_omission(self):
        self.source.unlink()
        with self.assertRaisesRegex(ValueError, "omitido no checkout"):
            module.verify(self.repo, self.jar)

    def test_missing_jar_resource(self):
        self.package(omit=True)
        with self.assertRaisesRegex(ValueError, "ausente no JAR"):
            module.verify(self.repo, self.jar)

    def test_stale_jar_resource(self):
        self.package(changed=True)
        with self.assertRaisesRegex(ValueError, "divergente no JAR"):
            module.verify(self.repo, self.jar)


class CompiledBackendPackageTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.repo = Path(self.temp.name)
        self.classes = self.repo / "backend/ads-service/target/classes/com/marketinghub"
        self.classes.mkdir(parents=True)
        self.jar = self.repo / "app-exec.jar"
        self.current = b"classe da revisao testada"
        (self.classes / "CreativeVisualEvidenceService.class").write_bytes(self.current)

    def package(self, content=None, extra=False):
        with ZipFile(self.jar, "w") as archive:
            if content is not None:
                archive.writestr("BOOT-INF/classes/com/marketinghub/CreativeVisualEvidenceService.class", content)
            if extra:
                archive.writestr("BOOT-INF/classes/com/marketinghub/Removed.class", b"antiga")

    def test_exact_tested_class_is_accepted(self):
        self.package(self.current)
        self.assertEqual(1, module.verify_compiled_backend(self.repo, self.jar))

    def test_old_jar_without_new_class_is_rejected(self):
        self.package()
        with self.assertRaisesRegex(ValueError, "não correspondem"):
            module.verify_compiled_backend(self.repo, self.jar)

    def test_changed_bytecode_is_rejected(self):
        self.package(b"revisao anterior")
        with self.assertRaisesRegex(ValueError, "Classe divergente"):
            module.verify_compiled_backend(self.repo, self.jar)

    def test_removed_class_cannot_remain_in_jar(self):
        self.package(self.current, extra=True)
        with self.assertRaisesRegex(ValueError, "extras="):
            module.verify_compiled_backend(self.repo, self.jar)

    def test_missing_compilation_cannot_authorize_package(self):
        (self.classes / "CreativeVisualEvidenceService.class").unlink()
        self.package(self.current)
        with self.assertRaisesRegex(ValueError, "Classes compiladas ausentes"):
            module.verify_compiled_backend(self.repo, self.jar)


if __name__ == "__main__":
    unittest.main()
