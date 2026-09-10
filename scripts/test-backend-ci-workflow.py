#!/usr/bin/env python3
"""Protege a execução integral do backend antes do merge e a evidência das falhas."""

import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]


class BackendCiWorkflowTest(unittest.TestCase):
    """Confere o contrato que faltava no PR responsável pela regressão de testes."""

    def setUp(self):
        self.workflow = (REPO / ".github/workflows/backend-ci.yml").read_text()

    def test_pr_covers_backend_and_packaged_inputs(self):
        self.assertRegex(self.workflow, r"pull_request:\s+branches: \[main\]")
        for path in [
            "backend/**",
            "config/agents/codex-agent-health-compliance.json",
            ".github/workflows/backend-ci.yml",
            ".github/workflows/deploy-containers.yml",
            "scripts/test-backend-ci-workflow.py",
            "**/src/main/resources/prompts/**",
            "product-discovery-worker/prompts/**",
            "pesquisas/**",
        ]:
            self.assertIn(f'- "{path}"', self.workflow)

    def test_whole_suite_matches_application_deploy(self):
        self.assertIn("working-directory: backend/ads-service\n        run: mvn -B test", self.workflow)
        deployment = (REPO / ".github/workflows/deploy-containers.yml").read_text()
        self.assertIn("mvn -B -q test | tee", deployment)
        self.assertNotRegex(self.workflow, r"-Dtest=|testFailureIgnore|continue-on-error|\|\| true")

    def test_packages_only_after_full_suite(self):
        tests = self.workflow.index("run: mvn -B test")
        package = self.workflow.index("run: mvn -B package -DskipTests")
        resources = self.workflow.index("python3 scripts/verify-backend-packaged-resources.py")
        self.assertLess(tests, package)
        self.assertLess(package, resources)

    def test_reports_survive_failure_and_ci_cannot_publish(self):
        self.assertRegex(self.workflow, r"if: always\(\)\s+uses: actions/upload-artifact@v4")
        self.assertIn("path: backend/ads-service/target/surefire-reports/", self.workflow)
        self.assertIn("permissions:\n  contents: read", self.workflow)
        self.assertNotRegex(self.workflow, r"secrets\.|packages: write|contents: write|\bssh\b|\bdocker push\b|mvn[^\n]+deploy")

    def test_spring_database_overrides_do_not_restore_shared_names(self):
        for source in (REPO / "backend/ads-service/src/test/java").rglob("*.java"):
            content = source.read_text()
            for url in re.findall(r"spring\.datasource\.url=(jdbc:h2:mem:[^\"\n]+)", content):
                self.assertIn("${random.uuid}", url, str(source.relative_to(REPO)))
                test_class = re.search(r"^package ([^;]+);", content, re.M).group(1) + "." + source.stem
                self.assertIn(test_class, url, str(source.relative_to(REPO)))
                self.assertIn("DB_CLOSE_DELAY=0", url, str(source.relative_to(REPO)))

    def test_spring_context_cache_remains_bounded(self):
        properties = (REPO / "backend/ads-service/src/test/resources/spring.properties").read_text()
        limit = int(re.search(r"^spring\.test\.context\.cache\.maxSize=(\d+)$", properties, re.M).group(1))
        self.assertGreater(limit, 0)
        self.assertLessEqual(limit, 8)


if __name__ == "__main__":
    unittest.main()
