"""Protege cobertura e sanitização das provas emitidas a partir do JUnit real dos controles."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest import mock
from xml.etree import ElementTree

SPEC = importlib.util.spec_from_file_location("controls", Path(__file__).with_name("emit-controls-evidence.py"))
CONTROLS = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(CONTROLS)
REPORT = CONTROLS.ROOT / "backend/ads-service/target/surefire-reports" / ("TEST-" + CONTROLS.SUITE + ".xml")


class ControlsEvidenceTest(unittest.TestCase):
    """Recusa provas incompletas e impede propriedades técnicas de entrarem no relatório público."""

    def setUp(self):
        """Lê a suíte realmente executada e mantém todas as mutações dentro de diretório efêmero."""
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.input = Path(self.directory.name) / "input.xml"
        self.output = Path(self.directory.name) / "output.json"
        self.suite = ElementTree.fromstring(REPORT.read_bytes())

    def emit(self):
        """Serializa somente a cópia sintética, sem tocar no relatório original."""
        self.input.write_bytes(ElementTree.tostring(self.suite))
        CONTROLS.emit(self.input, self.output)

    def test_sanitizes_properties_and_logs(self):
        """Emite sete resultados rastreáveis sem copiar propriedades ou saídas do JVM."""
        marker = "PRIVATE_FIXTURE_MUST_NOT_BE_PUBLISHED"
        ElementTree.SubElement(self.suite, "properties").text = marker
        ElementTree.SubElement(self.suite, "system-out").text = marker
        self.emit()
        raw = self.output.read_text()
        report = json.loads(raw)
        self.assertNotIn(marker, raw)
        self.assertEqual(7, len(report["criteria"]))
        self.assertEqual(0, report["providerCalls"])
        self.assertFalse(report["commercialSideEffects"])

    def test_rejects_missing_criterion(self):
        """A ausência de um critério não pode ser convertida em cobertura completa."""
        self.suite.remove(self.suite.findall("testcase")[0])
        with self.assertRaises(ValueError):
            self.emit()

    def test_rejects_skipped_suite(self):
        """Skip de ambiente continua sendo validação pendente."""
        self.suite.set("skipped", "1")
        with self.assertRaises(ValueError):
            self.emit()

    def test_rejects_failed_case_even_with_successful_summary(self):
        """Confere cada resultado e não confia somente no resumo da suíte."""
        ElementTree.SubElement(self.suite.findall("testcase")[0], "failure")
        with self.assertRaises(ValueError):
            self.emit()

    def test_rejects_foreign_class(self):
        """Uma suíte de outra implementação não satisfaz os controles de Mira."""
        self.suite.findall("testcase")[0].set("classname", "another.Implementation")
        with self.assertRaises(ValueError):
            self.emit()

    def test_rejects_uncompiled_source(self):
        """Não emite prova quando a classe atual não foi compilada e testada."""
        with mock.patch.object(CONTROLS, "ROOT", Path(self.directory.name)):
            with self.assertRaises(ValueError):
                self.emit()


if __name__ == "__main__":
    unittest.main()
