#!/usr/bin/env python3
"""Valida o gate de imagens com dependências reais e regressões em arquivos temporários."""

import re
import shlex
import shutil
import subprocess
import tempfile
import textwrap
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
VALIDATOR = ROOT / "scripts/validate-canonical-image-model.sh"
MODEL = "gpt-image-2.5-sunburst"
DEFAULTS = (
    ".env.example",
    "docker-compose.yml",
    "ai-worker/docker-compose.yml",
    "ai-worker/src/main/resources/application.properties",
    "deploy/docker-compose.yml",
    "deploy/docker-compose.video.yml",
    "feo/src/main/resources/application.yml",
    "lead-portal-payments-service/docker-compose.deploy.yml",
    "lead-portal-payments-service/src/main/resources/application.yml",
    "meta-ad-approver-worker/docker-compose.yml",
    "meta-ad-approver-worker/src/main/resources/application.yml",
    "video-management-service/src/main/resources/application.yml",
)
VIDEO_CONFIGS = (
    "docker-compose.yml",
    "deploy/docker-compose.yml",
    "deploy/docker-compose.video.yml",
    "video-management-service/src/main/resources/application.yml",
)
ACTIVE_DIRECTORIES = (
    "ai-worker/src/main",
    "backend/ads-service/src/main/java",
    "backend/ads-service/src/main/resources/agent-harness",
    "feo/src/main",
    "frontend/src",
    "landing-generator-agent-worker/src/main/resources/prompts",
    "lead-portal/backend/src/main",
    "lead-portal-payments-service/src/main",
    "meta-ad-approver-worker/src/main",
    "video-management-service/src/main",
)
PRICING = "frontend/src/utils/imagePricing.ts"
MASTER = "backend/ads-service/src/main/resources/db/changelog/db.changelog-master.yaml"
INCLUDE = "changesets/2030-09-19-add-gpt-image-2-5-sunburst.yaml"


class CanonicalImageModelTest(unittest.TestCase):
    """Distingue regressão do contrato, histórico permitido e impossibilidade de verificar."""

    def setUp(self):
        self.bash = shutil.which("bash")
        self.rg = shutil.which("rg")
        self.assertIsNotNone(self.bash, "Instale bash antes de executar a suíte.")
        self.assertIsNotNone(self.rg, "Instale ripgrep antes de executar a suíte.")
        temporary = tempfile.TemporaryDirectory(prefix="image-contract-")
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.bin = self.root / "bin"
        self.bin.mkdir()
        (self.bin / "rg").symlink_to(self.rg)
        for path in DEFAULTS:
            self.write(path, f"model: {MODEL}\n")
        for path in ACTIVE_DIRECTORIES:
            (self.root / path).mkdir(parents=True, exist_ok=True)
        for path in (
            ".github/workflows/meta-ad-approver-worker-ci.yml",
            "deploy/local-validation/creative-production-v5/docker-compose.yml",
            "lead-portal-payments-service/scripts/agenda-cheia-photo-library.sh",
        ):
            self.write(path, f"model: {MODEL}\n")
        for path in VIDEO_CONFIGS:
            self.write(path, f"model: {MODEL}\nOPENAI_IMAGE_ORCHESTRATION_MODEL: text-model\n")
        self.write(PRICING, 'const prices = { "GPT IMAGE 2 5 SUNBURST": 1 };\n')
        self.write(MASTER, f"databaseChangeLog:\n  - include:\n      file: {INCLUDE}\n      relativeToChangelogFile: true\n")

    def write(self, path, content):
        destination = self.root / path
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_text(content, encoding="utf-8")

    def run_validator(self, *, root=None, path=None):
        return subprocess.run(
            [self.bash, str(VALIDATOR)],
            cwd=root or self.root,
            env={"PATH": str(path or self.bin), "LC_ALL": "C"},
            capture_output=True,
            text=True,
            timeout=30,
        )

    def assert_failure(self, result, code, message):
        self.assertEqual(result.returncode, code, result.stdout + result.stderr)
        self.assertIn(message, result.stderr)
        self.assertNotIn("validado nos fluxos ativos", result.stdout)

    def fake_rg_error(self, argument):
        (self.bin / "rg").unlink()
        self.write("bin/rg", f"#!{self.bash}\nfor argument in \"$@\"; do\n"
                   f"  if [[ \"$argument\" == {shlex.quote(argument)} ]]; then\n"
                   "    echo 'falha sintética do scanner' >&2\n    exit 2\n  fi\ndone\n"
                   f"exec {shlex.quote(self.rg)} \"$@\"\n")
        (self.bin / "rg").chmod(0o755)

    def test_real_repository_passes(self):
        result = self.run_validator(root=ROOT)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("validado nos fluxos ativos", result.stdout)

    def test_valid_configuration_passes_with_only_declared_scanner_on_path(self):
        result = self.run_validator()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_missing_ripgrep_is_reported_as_dependency_failure(self):
        (self.bin / "rg").unlink()
        result = self.run_validator()
        self.assert_failure(result, 2, "dependência obrigatória ausente: rg (ripgrep)")
        self.assertNotIn("padrão canônico ausente", result.stderr)

    def test_retired_models_are_blocked_in_each_active_module(self):
        for directory in ACTIVE_DIRECTORIES:
            source = f"{directory}/contract-regression.txt"
            for model in ("gpt-image-1", "gpt-image-1.5", "gpt-image-2", "gpt-image-2-2025-01-01"):
                with self.subTest(directory=directory, model=model):
                    self.write(source, f"model: {model}\n")
                    result = self.run_validator()
                    self.assert_failure(result, 1, "modelo aposentado encontrado")
                    self.assertIn(f"{source}:1:", result.stdout)
            (self.root / source).unlink()

    def test_tests_and_historical_changelogs_keep_original_models(self):
        for path in (
            "frontend/src/contract.test.ts",
            "frontend/src/__tests__/contract.ts",
            "backend/ads-service/src/main/resources/db/changelog/changesets/historical.yaml",
        ):
            self.write(path, "gpt-image-1 gpt-image-1.5 gpt-image-2\n")
        result = self.run_validator()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_every_required_default_is_enforced(self):
        for path in DEFAULTS:
            with self.subTest(path=path):
                original = (self.root / path).read_text()
                self.write(path, "model: unknown-model\n")
                self.assert_failure(self.run_validator(), 1, f"padrão canônico ausente em {path}")
                self.write(path, original)

    def test_missing_default_file_is_a_technical_failure(self):
        (self.root / ".env.example").unlink()
        self.assert_failure(self.run_validator(), 2, "falha técnica ao verificar .env.example")

    def test_missing_active_source_never_passes(self):
        (self.root / "lead-portal/backend/src/main").rmdir()
        self.assert_failure(self.run_validator(), 2, "falha técnica ao pesquisar modelos aposentados")

    def test_pcre2_failure_never_passes(self):
        self.fake_rg_error("--pcre2")
        self.assert_failure(self.run_validator(), 2, "falha técnica ao pesquisar modelos aposentados")

    def test_default_read_error_is_not_reported_as_invalid_model(self):
        self.fake_rg_error(".env.example")
        self.assert_failure(self.run_validator(), 2, "falha técnica ao verificar .env.example")

    def test_missing_financial_estimate_is_blocked(self):
        self.write(PRICING, "const prices = {};\n")
        self.assert_failure(self.run_validator(), 1, "estimativa financeira do Sunburst ausente")

    def test_financial_estimate_read_error_is_a_technical_failure(self):
        self.fake_rg_error(PRICING)
        self.assert_failure(self.run_validator(), 2, f"falha técnica ao verificar {PRICING}")

    def test_video_orchestrator_remains_separate_in_each_configuration(self):
        for path in VIDEO_CONFIGS:
            with self.subTest(path=path):
                original = (self.root / path).read_text()
                self.write(path, f"model: {MODEL}\n")
                self.assert_failure(self.run_validator(), 1, f"modelo orquestrador de vídeo não está isolado em {path}")
                self.write(path, original)

    def test_liquibase_include_must_remain_relative(self):
        for suffix in ("", "\n      relativeToChangelogFile: false"):
            with self.subTest(suffix=suffix):
                self.write(MASTER, f"databaseChangeLog:\n  - include:\n      file: {INCLUDE}{suffix}\n")
                self.assert_failure(self.run_validator(), 1, "include Liquibase canônico não é relativo")

    def test_missing_changelog_is_a_technical_failure(self):
        (self.root / MASTER).unlink()
        self.assert_failure(self.run_validator(), 2, f"falha técnica ao verificar {MASTER}")

    def test_workflow_installs_dependencies_before_tests_and_validation(self):
        workflow = (ROOT / ".github/workflows/image-model-contract.yml").read_text()
        installation = workflow.index('sudo apt-get "${apt_network[@]}" install')
        tests = workflow.index("python3 scripts/test-canonical-image-model.py -v")
        validation = workflow.index("bash scripts/validate-canonical-image-model.sh")
        self.assertLess(workflow.index("command -v rg"), installation)
        self.assertLess(installation, workflow.index('sudo apt-get "${apt_network[@]}" update'))
        self.assertLess(installation, tests)
        self.assertLess(tests, validation)
        for event in ("push", "pull_request"):
            section = re.search(rf"^  {event}:\n(.*?)(?=^  \w|\Z)", workflow, re.M | re.S).group(1)
            for path in ("scripts/test-canonical-image-model.py", "scripts/validate-canonical-image-model.sh"):
                self.assertIn(f'- "{path}"', section)
        self.assertIn("contents: read", workflow)
        self.assertNotRegex(workflow, r"continue-on-error|\|\| true|secrets\.|contents: write")


class WorkflowDependenciesTest(unittest.TestCase):
    """Executa a preparação real da CI sem rede e preserva falhas de dependências."""

    def setUp(self):
        self.bash = shutil.which("bash")
        temporary = tempfile.TemporaryDirectory(prefix="image-workflow-dependencies-")
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.bin = self.root / "bin"
        self.bin.mkdir()
        workflow = (ROOT / ".github/workflows/image-model-contract.yml").read_text()
        match = re.search(
            r"      - name: Instalar dependências do verificador e dos testes\n"
            r"        run: \|\n(.*?)(?=      - name:)", workflow, re.S
        )
        self.assertIsNotNone(match, "A preparação precisa permanecer antes dos testes.")
        self.preparation = textwrap.dedent(match.group(1))
        self.log = self.root / "calls"
        self.tool("timeout", '''printf 'timeout %s\\n' "$*" >> "$DEPENDENCY_CALLS"
[[ "$1" == --kill-after=5s && "$2" == 60s ]] || exit 41
shift 2
exec "$@"
''')
        self.tool("sudo", '''[[ "$1" == apt-get ]] || exit 40
shift
operation=
for argument in "$@"; do
  case "$argument" in install|update) operation="$argument" ;; esac
done
printf '%s\\n' "$operation" >> "$DEPENDENCY_CALLS"
case "$operation" in
  update)
    [[ "$DEPENDENCY_UPDATE_FAIL" != 1 ]] || exit 42
    printf 'updated' > "$DEPENDENCY_STATE"
    ;;
  install)
    if [[ "$DEPENDENCY_CACHE_FAIL" == 1 && ! -f "$DEPENDENCY_STATE" ]]; then exit 43; fi
    if [[ "$DEPENDENCY_INSTALL_TOOLS" == 1 ]]; then
      for tool in rg python3; do
        printf '#!/bin/bash\\nexit 0\\n' > "$DEPENDENCY_BIN/$tool"
        /bin/chmod +x "$DEPENDENCY_BIN/$tool"
      done
    fi
    ;;
  *) exit 44 ;;
esac
''')

    def tool(self, name, content="exit 0\n"):
        destination = self.bin / name
        destination.write_text(f"#!{self.bash}\n{content}")
        destination.chmod(0o755)

    def run_preparation(self, *, cache_failure=False, update_failure=False, install_tools=True):
        result = subprocess.run(
            [self.bash, "-e", "-o", "pipefail", "-c", self.preparation],
            env={
                "PATH": str(self.bin), "DEPENDENCY_BIN": str(self.bin),
                "DEPENDENCY_CALLS": str(self.log), "DEPENDENCY_STATE": str(self.root / "updated"),
                "DEPENDENCY_CACHE_FAIL": str(int(cache_failure)),
                "DEPENDENCY_UPDATE_FAIL": str(int(update_failure)),
                "DEPENDENCY_INSTALL_TOOLS": str(int(install_tools)),
            }, capture_output=True, text=True, timeout=10,
        )
        calls = self.log.read_text().splitlines() if self.log.exists() else []
        operations = [line for line in calls if not line.startswith("timeout ")]
        return result, calls, operations

    def test_preinstalled_tools_do_not_contact_package_mirrors(self):
        self.tool("rg")
        self.tool("python3")
        result, calls, _ = self.run_preparation()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(calls, [])

    def test_missing_tool_is_installed_from_cache_with_bounded_network(self):
        for present in ("rg", "python3"):
            with self.subTest(present=present):
                for tool in ("rg", "python3"):
                    (self.bin / tool).unlink(missing_ok=True)
                self.log.unlink(missing_ok=True)
                self.tool(present)
                result, calls, operations = self.run_preparation()
                self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
                self.assertEqual(operations, ["install"])
                self.assertEqual(len(calls), 2)
                self.assertIn("Acquire::Retries=1", calls[0])
                self.assertIn("Acquire::http::Timeout=15", calls[0])
                self.assertIn("Acquire::https::Timeout=15", calls[0])

    def test_stale_cache_is_refreshed_once_then_installed(self):
        result, calls, operations = self.run_preparation(cache_failure=True)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(operations, ["install", "update", "install"])
        self.assertEqual(len(calls), 6)

    def test_permanent_mirror_failure_stops_before_validation(self):
        result, _, operations = self.run_preparation(cache_failure=True, update_failure=True)
        self.assertEqual(result.returncode, 42, result.stdout + result.stderr)
        self.assertEqual(operations, ["install", "update"])
        self.assertFalse((self.bin / "rg").exists())

    def test_successful_installer_without_required_tools_is_still_failure(self):
        result, _, operations = self.run_preparation(install_tools=False)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(operations, ["install"])


if __name__ == "__main__":
    unittest.main()
