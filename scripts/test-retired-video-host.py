#!/usr/bin/env python3
"""Valida retirada de host e ciclo de vida sem acessar produção ou inferência."""
import json
import importlib.util
import io
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parent.parent
spec = importlib.util.spec_from_file_location("video_host_state", ROOT / "scripts/video-host-state.py")
state_transfer = importlib.util.module_from_spec(spec)
spec.loader.exec_module(state_transfer)


class RetirementContractTest(unittest.TestCase):
    """Impede retorno ao host retirado e disputa de recursos com os produtos."""

    def test_rotation_recreates_only_video_with_same_image_and_refuses_missing_secret(self):
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            calls = directory / "calls.jsonl"
            docker = directory / "docker"
            docker.write_text('''#!/usr/bin/env python3
import json,os,sys
from pathlib import Path
with Path(os.environ["VIDEO_TEST_CALLS"]).open("a") as f: f.write(json.dumps(sys.argv[1:])+"\\n")
''')
            docker.chmod(0o755)
            secret = directory / "secret"
            secret.write_text("credencial-sintetica-local")
            env = {**os.environ, "PATH": str(directory) + os.pathsep + os.environ["PATH"],
                   "DEPLOY_DIR": str(directory), "VIDEO_TAR": str(directory / "absent.tar"),
                   "VIDEO_TEST_CALLS": str(calls), "VIDEO_IMAGE": "video-contract",
                   "IMAGE_TAG": "same-approved-sha", "VIDEO_PULL": "false"}
            for provider in ["OPENAI", "GEMINI", "LUMA", "KLING", "HEYGEN", "RUNWAY"]:
                env[provider + "_API_KEY_HOST_FILE"] = str(secret)
            command = ["bash", str(ROOT / "deploy/bin/apply-video-only.sh")]
            for _ in range(2):
                result = subprocess.run(command, env=env, capture_output=True, text=True)
                self.assertEqual(result.returncode, 0, result.stderr)
            recorded = [json.loads(line) for line in calls.read_text().splitlines()]
            mutations = [call for call in recorded if call[0] == "compose"]
            self.assertEqual(len(mutations), 2)
            self.assertTrue(all(call == ["compose", "-f", "docker-compose.video.yml", "up", "-d",
                                        "--no-deps", "--force-recreate", "video-management"]
                                for call in mutations))
            before = calls.read_bytes()
            secret.unlink()
            result = subprocess.run(command, env=env, capture_output=True, text=True)
            self.assertNotEqual(result.returncode, 0)
            self.assertEqual(calls.read_bytes(), before)

    def test_no_workflow_contacts_retired_host(self):
        for path in (ROOT / ".github/workflows").glob("*.yml"):
            self.assertNotIn("177.153.62.107", path.read_text(), str(path))

    def test_video_observability_and_capacity_follow_destination(self):
        workflow = (ROOT / ".github/workflows/deploy-containers.yml").read_text()
        self.assertIn("VIDEO_VPS_IP: 163.245.200.7", workflow)
        self.assertIn("group: deploy-vps-163-245-200-7", workflow)
        self.assertIn("/root/infra/apollo-secrets/openai_api_key", workflow)
        for file in ["mcp-server/src/main/resources/application.yml",
                     "deploy/docker-compose.mcp.yml", "deploy/docker-compose.yml"]:
            source = (ROOT / file).read_text()
            self.assertIn("http://163.245.200.7:8095/actuator/logfile", source)
            self.assertNotIn("177.153.62.107", source)
        compose = (ROOT / "deploy/docker-compose.video.yml").read_text()
        self.assertIn('cpus: "${VIDEO_CPU_LIMIT:-2.0}"', compose)
        self.assertIn("mem_limit: ${VIDEO_MEMORY_LIMIT:-1536m}", compose)
        self.assertIn("-Xmx768m -XX:ActiveProcessorCount=2", compose)
        apply = (ROOT / "deploy/bin/apply-video-only.sh").read_text()
        self.assertNotIn("prune", apply)
        self.assertNotIn("cleanup_previous_tags", apply)

    def test_collectors_cannot_activate_on_push(self):
        for name in ["mois-hotmart-collector", "mois-meta-ad-library-collector"]:
            workflow = (ROOT / f".github/workflows/{name}-ci.yml").read_text()
            self.assertIn("default: prepare", workflow)
            deploy = workflow.split("\n  deploy:", 1)[1]
            self.assertIn("if: github.event_name == 'workflow_dispatch' && github.ref == 'refs/heads/main'", deploy)
            self.assertNotIn("docker rm -f", deploy)
            self.assertNotIn("prune", deploy)
            compose = (ROOT / name / "docker-compose.deploy.yml").read_text()
            self.assertIn('restart: "no"', compose)
        hotmart = (ROOT / "mois-hotmart-collector/docker-compose.deploy.yml").read_text()
        self.assertIn('18096}:8096', hotmart)
        self.assertNotIn('"8096:8096"', hotmart)

    def test_standalone_collector_descriptors_pass_real_compose_validation(self):
        env = {**os.environ, "RADAR_META_TOKEN": "local-contract-only",
               "MOIS_META_AD_LIBRARY_COLLECTOR_IMAGE": "test-only:local",
               "MOIS_HOTMART_COLLECTOR_IMAGE": "test-only:local"}
        for service in ["mois-meta-ad-library-collector", "mois-hotmart-collector"]:
            result = subprocess.run(
                ["docker", "compose", "-p", os.environ.get("PDE_LOCAL_COMPOSE_PROJECT", "retirement-schema"), "-f",
                 str(ROOT / service / "docker-compose.deploy.yml"), "config", "--format", "json"],
                env=env, capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            config = json.loads(result.stdout)["services"][service]
            self.assertNotIn("build", config)
            self.assertEqual(config["restart"], "no")

    def test_legacy_remains_explicit_and_no_cross_stack_cleanup(self):
        for name in ["mds-ci", "mois-ci", "oprm-ci"]:
            source = (ROOT / f".github/workflows/{name}.yml").read_text()
            self.assertIn("inputs.activate == true", source.split("\n  deploy:", 1)[1])
            self.assertIn("default: false", source)
        source = (ROOT / ".github/workflows/oprm-coletor-mei-ci.yml").read_text()
        self.assertNotIn("LEGACY_DEPLOY_HOST", source)
        self.assertNotIn("docker compose -f docker-compose.yml down", source)


class CollectorLifecycleTest(unittest.TestCase):
    """Executa o comando real contra Docker simulado com estados independentes."""

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.directory = Path(self.tmp.name)
        self.state = self.directory / "state.json"
        self.state.write_text(json.dumps({"exists": False, "running": False, "calls": []}))
        docker = self.directory / "docker"
        docker.write_text('''#!/usr/bin/env python3
import json,os,sys
from pathlib import Path
p=Path(os.environ["TEST_DOCKER_STATE"]); s=json.loads(p.read_text()); a=sys.argv[1:]
if a[:2]==["container","inspect"]:
 if not s["exists"]: sys.exit(1)
 field=a[-1]
 print("true" if s["running"] else "false") if "State.Running" in field else print("no" if "RestartPolicy" in field else "healthy")
elif a[:1]==["compose"]:
 s["calls"].append(a)
 if "create" in a: s.update(exists=True,running=False)
 elif "up" in a: s.update(exists=True,running=True)
 else: sys.exit(3)
 p.write_text(json.dumps(s))
else: sys.exit(4)
''')
        docker.chmod(0o755)
        self.env = {**os.environ, "PATH": str(self.directory) + os.pathsep + os.environ["PATH"],
                    "TEST_DOCKER_STATE": str(self.state)}

    def run_command(self, operation, service="mois-hotmart-collector"):
        return subprocess.run(["bash", str(ROOT / "scripts/apply-on-demand-collector.sh"),
                               operation, service], env=self.env, capture_output=True, text=True)

    def test_prepare_both_collectors_without_start(self):
        for service in ["mois-hotmart-collector", "mois-meta-ad-library-collector"]:
            self.assertEqual(self.run_command("prepare", service).returncode, 0)
            state = json.loads(self.state.read_text())
            self.assertFalse(state["running"])
            self.assertFalse(any("up" in call for call in state["calls"]))

    def test_explicit_activation_and_repeated_prepare_preserve_active_worker(self):
        self.assertEqual(self.run_command("activate").returncode, 0)
        before = self.state.read_bytes()
        self.assertNotEqual(self.run_command("prepare").returncode, 0)
        self.assertEqual(self.state.read_bytes(), before)


    def test_invalid_operation_or_unrelated_service_has_no_side_effect(self):
        before = self.state.read_bytes()
        self.assertEqual(self.run_command("purge").returncode, 2)
        self.assertEqual(self.run_command("prepare", "pde-platform-backend").returncode, 2)
        self.assertEqual(self.state.read_bytes(), before)


class StateTransferTest(unittest.TestCase):
    """Exercita cópia real de bytes, colisões, isolamento e recusa de estado ativo."""

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.source, self.destination = self.root / "source", self.root / "destination"
        self.source.mkdir()
        self.destination.mkdir()
        for index, name in enumerate(state_transfer.SECRET_MAP):
            path = self.source / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(f"segredo-ficticio-{index}")
        self.auth = self.source / "root/infra/apollo-codex-home/auth.json"
        self.auth.parent.mkdir(parents=True)
        self.auth.write_text('{"syntheticSession":"local-only"}')
        self.containers = [{"Name": "/mois-meta-ad-library-collector", "State": {"Running": False},
                            "Config": {"Env": ["META_AD_LIBRARY_ACCESS_TOKEN=ficticio-local"]}}]

    def archive(self):
        output = io.BytesIO()
        state_transfer.export_state(self.source, output, self.containers)
        path = self.root / "state.tar.gz"
        path.write_bytes(output.getvalue())
        return path

    def test_transfer_preserves_session_secrets_and_is_repeatable(self):
        archive = self.archive()
        first = state_transfer.restore_state(self.destination, archive)
        second = state_transfer.restore_state(self.destination, archive)
        self.assertEqual(first, second)
        self.assertEqual((self.destination / self.auth.relative_to(self.source)).read_bytes(), self.auth.read_bytes())
        self.assertFalse((self.destination / "root/infra/openai-token").exists())
        self.assertEqual((self.destination / "root/infra/apollo-secrets/openai_api_key").stat().st_mode & 0o777, 0o600)

    def test_existing_other_secret_is_not_overwritten_or_partially_applied(self):
        target = self.destination / "root/infra/apollo-secrets/luma_api_key"
        target.parent.mkdir(parents=True)
        target.write_text("outro-valor-local")
        with self.assertRaises(ValueError):
            state_transfer.restore_state(self.destination, self.archive())
        self.assertEqual(target.read_text(), "outro-valor-local")
        self.assertFalse((target.parent / "openai_api_key").exists())

    def test_active_executor_and_path_traversal_are_rejected(self):
        self.containers[0]["State"]["Running"] = True
        with self.assertRaises(ValueError):
            self.archive()
        for name in ["../root/infra/key", "/etc/shadow", "root/infra/../../x"]:
            with self.assertRaises(ValueError):
                state_transfer.destination_for(name)

    def test_code_and_other_product_files_are_only_in_backup(self):
        path = self.source / "opt/marketinghub/containers/bin/script.sh"
        path.parent.mkdir(parents=True)
        path.write_text("echo codigo-nao-publicado")
        state_transfer.restore_state(self.destination, self.archive())
        self.assertFalse((self.destination / path.relative_to(self.source)).exists())

    def test_historical_container_logs_remain_in_protected_backup_only(self):
        log = self.source / "var/lib/docker/containers/synthetic/synthetic-json.log"
        log.parent.mkdir(parents=True)
        log.write_text('{"log":"historico-local"}')
        self.containers[0]["LogPath"] = "/" + str(log.relative_to(self.source))
        archive = self.archive()
        with state_transfer.tarfile.open(archive) as contents:
            self.assertIn(str(log.relative_to(self.source)), contents.getnames())
        state_transfer.restore_state(self.destination, archive)
        self.assertFalse((self.destination / "var/lib/docker").exists())



if __name__ == "__main__":
    unittest.main()
