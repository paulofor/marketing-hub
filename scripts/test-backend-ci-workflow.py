#!/usr/bin/env python3
"""Protege a execução integral do backend antes do merge e a evidência das falhas."""

import re
import subprocess
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
            "scripts/download-approved-pr-artifact.sh",
            "scripts/test-backend-ci-workflow.py",
            "infra/testing/vega-integrity-cycle/run-round.sh",
            "infra/testing/runway-clip-plan/run-round.sh",
            "infra/testing/runway-gen45/run-round.sh",
            "infra/testing/pde-version-contract/**",
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

    def test_pr_validates_liquibase_and_preserves_reusable_package(self):
        self.assertIn("image: mysql:5.7", self.workflow)
        self.assertIn("liquibase-maven-plugin:4.26.0:validate", self.workflow)
        self.assertIn("name: backend-approved-package", self.workflow)
        self.assertIn("backend/ads-service/target/app.jar", self.workflow)
        self.assertIn("backend/ads-service/target/approved-tree.sha", self.workflow)
        self.assertIn("git rev-parse 'HEAD^{tree}'", self.workflow)

        deployment = (REPO / ".github/workflows/deploy-containers.yml").read_text()
        self.assertIn("download-approved-pr-artifact.sh", deployment)
        self.assertIn("backend-ci.yml backend-approved-package", deployment)
        self.assertGreaterEqual(
            deployment.count("if: steps.approved-package.outputs.reused != 'true'"), 3
        )

    def test_approved_artifact_helper_has_valid_shell_syntax(self):
        helper = REPO / "scripts/download-approved-pr-artifact.sh"
        result = subprocess.run(["bash", "-n", str(helper)], capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        content = helper.read_text()
        self.assertIn("HEAD^{tree}", content)
        self.assertIn("HEAD)", content)
        self.assertIn("event=pull_request&status=success", content)
        self.assertIn("approved-tree.sha", content)

    def test_local_vega_matrix_covers_shared_agent_catalog(self):
        script = (REPO / "infra/testing/vega-integrity-cycle/run-round.sh").read_text()
        backend = next(line for line in script.splitlines() if line.startswith("run backend "))
        selected_tests = re.search(r"'-Dtest=([^']+)'", backend).group(1).split(",")
        self.assertIn("AgentHarnessCatalogTest", selected_tests)

    def test_local_mira_matrix_checks_ci_contract_before_backend(self):
        script = (REPO / "infra/testing/mira-communication/run-round.sh").read_text()
        contract = script.index("run backend-ci-contract python3 scripts/test-backend-ci-workflow.py")
        backend = script.index("run backend mvn ")
        self.assertLess(contract, backend)

    def test_local_runway_matrices_cover_full_backend_and_packaged_catalog(self):
        script = (REPO / "infra/testing/runway-clip-plan/run-round.sh").read_text()
        steps = [
            "run backend-ci-contract python3 scripts/test-backend-ci-workflow.py",
            "run backend mvn -B -ntp -f backend/ads-service/pom.xml test",
            "run backend-package mvn -B -ntp -f backend/ads-service/pom.xml package -DskipTests",
            "run backend-package-contract python3 scripts/test-backend-packaged-resources.py",
            "run backend-package-integrity python3 scripts/verify-backend-packaged-resources.py",
            "run worker mvn ",
        ]
        positions = [script.index(step) for step in steps]
        self.assertEqual(positions, sorted(positions))
        self.assertNotRegex(script, r"-Dtest=|testFailureIgnore|continue-on-error|\|\| true")
        gen45 = (REPO / "infra/testing/runway-gen45/run-round.sh").read_text()
        shared = gen45.index('bash infra/testing/runway-clip-plan/run-round.sh "$round"')
        image = gen45.index("bash infra/testing/runway-gen45/verify-image.sh")
        self.assertLess(shared, image)

    def test_packages_only_after_full_suite(self):
        tests = self.workflow.index("run: mvn -B test")
        liquibase = self.workflow.index("liquibase-maven-plugin:4.26.0:validate")
        package = self.workflow.index("run: mvn -B package -DskipTests")
        resources = self.workflow.index("python3 scripts/verify-backend-packaged-resources.py")
        artifact = self.workflow.index("name: backend-approved-package")
        self.assertLess(tests, liquibase)
        self.assertLess(liquibase, package)
        self.assertLess(package, resources)
        self.assertLess(resources, artifact)

    def test_local_pde_matrix_covers_full_backend_packaging_and_reviewers(self):
        script = (REPO / "infra/testing/pde-version-contract/run-round.sh").read_text()
        steps = [
            "gate backend-ci-contract python3 scripts/test-backend-ci-workflow.py",
            "gate backend mvn -B -f backend/ads-service/pom.xml test",
            "gate backend-package mvn -B -f backend/ads-service/pom.xml package -DskipTests",
            "gate backend-package-integrity python3 scripts/verify-backend-packaged-resources.py",
            "gate journeys python3 infra/testing/pde-version-contract/run-journies.py",
            "gate commercial-evidence env EVIDENCE_COMPOSE_PROJECT=",
            "bash infra/testing/commercial-evidence/run-local.sh validation",
        ]
        positions = [script.index(step) for step in steps]
        self.assertEqual(positions, sorted(positions))
        self.assertNotRegex(script, r"-Dtest=|testFailureIgnore|continue-on-error|\|\| true")

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
