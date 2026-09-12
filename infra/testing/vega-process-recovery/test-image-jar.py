#!/usr/bin/env python3
"""Reproduz imagem antiga, arquivo ausente e sobreposição de camadas sem acessar Docker."""
import hashlib
import importlib.util
import io
import json
import tarfile
import tempfile
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location("image_jar", Path(__file__).with_name("verify-image-jar.py"))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


def add(archive, name, content):
    info = tarfile.TarInfo(name)
    info.size = len(content)
    archive.addfile(info, io.BytesIO(content))


class ImageJarTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.archive = Path(self.temp.name) / "image.tar"
        self.expected = hashlib.sha256(b"pacote testado").hexdigest()

    def image(self, layers):
        names = []
        with tarfile.open(self.archive, "w") as archive:
            for i, files in enumerate(layers):
                name = f"layer-{i}.tar"
                names.append(name)
                body = io.BytesIO()
                with tarfile.open(fileobj=body, mode="w") as layer:
                    for path, content in files.items():
                        add(layer, path, content)
                add(archive, name, body.getvalue())
            add(archive, "manifest.json", json.dumps([{"Layers": names}]).encode())

    def test_exact_jar_is_accepted(self):
        self.image([{"app/app.jar": b"pacote testado"}])
        self.assertEqual(self.expected, module.verify_archive(self.archive, self.expected))

    def test_previous_jar_is_rejected(self):
        self.image([{"app/app.jar": b"pacote anterior"}])
        with self.assertRaisesRegex(ValueError, "diverge"):
            module.verify_archive(self.archive, self.expected)

    def test_later_layer_cannot_replace_tested_jar(self):
        self.image([{"app/app.jar": b"pacote testado"}, {"app/app.jar": b"pacote anterior"}])
        with self.assertRaisesRegex(ValueError, "diverge"):
            module.verify_archive(self.archive, self.expected)

    def test_missing_jar_is_rejected(self):
        self.image([{"etc/fixture": b"sem jar"}])
        with self.assertRaisesRegex(ValueError, "recebido=None"):
            module.verify_archive(self.archive, self.expected)

    def test_deleted_jar_is_rejected(self):
        self.image([{"app/app.jar": b"pacote testado"}, {"app/.wh.app.jar": b""}])
        with self.assertRaisesRegex(ValueError, "recebido=None"):
            module.verify_archive(self.archive, self.expected)


if __name__ == "__main__":
    unittest.main()
