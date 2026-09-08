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


if __name__ == "__main__":
    unittest.main()
