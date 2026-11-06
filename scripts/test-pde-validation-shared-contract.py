#!/usr/bin/env python3
"""Confere a mesma fonte de escopo no Maven, no CI e nas imagens de ambos os módulos."""
import pathlib
import unittest
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]
NS = {"m": "http://maven.apache.org/POM/4.0.0"}


class SharedValidationContractTest(unittest.TestCase):
    def test_active_clients_match_backend_replay_handshake(self):
        import re
        source = (ROOT / "backend/ads-service/src/main/java/com/marketinghub/agenttask/AtenaBpmWorkerContract.java").read_text()
        contract = re.search(r'VERSION\s*=\s*"([^"]+)"', source).group(1)
        for name in (
            "experiment-strategist-worker/src/main/java/com/marketinghub/experimentstrategistworker/PdeMarketStrategyBpmTaskConsumer.java",
            "infra/testing/product-financial-plan/native-planning-matrix.mjs",
        ):
            self.assertIn(contract, (ROOT / name).read_text(), name)

    def test_modules_compile_same_policy_and_regressions(self):
        for module in ("backend/ads-service", "experiment-strategist-worker"):
            base = ROOT / module
            pom = ET.parse(base / "pom.xml")
            for execution, suffix in (("pde-validation-scope", "main"),
                                      ("pde-validation-scope-tests", "test")):
                matches = [node for node in pom.findall(".//m:execution", NS)
                           if node.findtext("m:id", namespaces=NS) == execution]
                self.assertEqual(len(matches), 1, module)
                source = matches[0].findtext("m:configuration/m:sources/m:source", namespaces=NS)
                resolved = pathlib.Path(source.replace("${project.basedir}", str(base))).resolve()
                self.assertEqual(resolved, ROOT / "shared/pde-validation-scope/src" / suffix / "java")
                self.assertTrue(resolved.is_dir())

    def test_worker_image_and_local_build_use_root_context(self):
        dockerfile = (ROOT / "experiment-strategist-worker/Dockerfile").read_text()
        self.assertIn("COPY shared/pde-validation-scope /build/shared/pde-validation-scope", dockerfile)
        self.assertIn("WORKDIR /build/experiment-strategist-worker", dockerfile)
        workflow = (ROOT / ".github/workflows/experiment-strategist-worker-ci.yml").read_text()
        self.assertIn("shared/pde-validation-scope/**", workflow)
        self.assertRegex(workflow, r"-f experiment-strategist-worker/Dockerfile .* \.\n")
        local = (ROOT / "experiment-strategist-worker/scripts/validate-bpm-image-local.sh").read_text()
        self.assertIn("-f experiment-strategist-worker/Dockerfile .", local)

    def test_backend_compilation_images_include_shared_source(self):
        for name in ("Dockerfile.liquibase-mysql57", "Dockerfile.argos-meta-supervised-session-test"):
            text = (ROOT / "backend/ads-service" / name).read_text()
            self.assertIn("COPY shared/pde-validation-scope /workspace/shared/pde-validation-scope", text)
            self.assertIn("WORKDIR /workspace/backend/ads-service", text)
        for path in (ROOT / "backend/ads-service").glob("docker-compose*.yml"):
            text = path.read_text()
            for name in ("Dockerfile.liquibase-mysql57", "Dockerfile.argos-meta-supervised-session-test"):
                if name in text:
                    self.assertNotIn("dockerfile: " + name, text, str(path))
                    self.assertIn("context: ../..", text, str(path))
        self.assertIn("shared/pde-validation-scope/**", (ROOT / ".github/workflows/backend-ci.yml").read_text())
        self.assertIn("shared/pde-validation-scope/*) backend=true", (ROOT / "scripts/detect-deployment-changes.sh").read_text())


if __name__ == "__main__":
    unittest.main()
