"""Protege integração do novo módulo, identidade e manutenção da credencial sem publicação."""
import os
import re
import shlex
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]

class DeliveryContractTest(unittest.TestCase):
    def assert_worker_image_contract(self, workflow, image='marketinghub-process-execution-worker:${GITHUB_SHA}'):
        """Confere comandos reais do produtor, normalizando aspas e a revisão do GitHub."""
        job = re.search(r'^  backend-image:\n(.*?)(?=^  [a-z][a-z0-9-]*:|\Z)', workflow, re.M | re.S)
        self.assertIsNotNone(job, 'Produtor backend-image ausente.')
        source = job.group(1).replace('${{ github.sha }}', '${GITHUB_SHA}')
        commands = [shlex.split(line.strip()) for line in source.splitlines()
                    if line.strip().startswith('docker ')]
        expected = [
            ['docker', 'build', '-t', image, 'process-execution-worker'],
            ['docker', 'run', '--rm', '--entrypoint', 'node', image, '--check', 'src/main.mjs'],
            ['docker', 'save', image, '-o', 'process-worker-image.tar'],
        ]
        for command in expected:
            self.assertIn(command, commands, 'Construção, verificação e exportação devem usar a mesma imagem.')
        positions = [commands.index(command) for command in expected]
        self.assertEqual(positions, sorted(positions), 'Verificar a imagem antes de exportar.')

    def test_credential_is_generated_once_and_never_printed(self):
        with tempfile.TemporaryDirectory() as tmp:
            cmd = ['bash', str(ROOT/'deploy/bin/prepare-process-worker-secret.sh'), tmp]
            first = subprocess.run(cmd, check=True, capture_output=True)
            value = (Path(tmp)/'token').read_bytes()
            self.assertGreaterEqual(len(value), 32)
            second = subprocess.run(cmd, check=True, capture_output=True)
            self.assertEqual((Path(tmp)/'token').read_bytes(), value)
            self.assertEqual(first.stdout + first.stderr + second.stdout + second.stderr, b'')
            self.assertEqual(os.stat(tmp).st_mode & 0o777, 0o700)
            self.assertEqual(os.stat(Path(tmp)/'token').st_mode & 0o777, 0o444)

    def test_backend_publication_contains_worker_and_waits_for_health(self):
        workflow=(ROOT/'.github/workflows/deploy-containers.yml').read_text()
        apply=(ROOT/'deploy/bin/apply.sh').read_text()
        self.assertIn('process-worker-image.tar',workflow)
        self.assert_worker_image_contract(workflow)
        self.assertIn('process-execution-worker/*) backend=true', (ROOT/'scripts/detect-deployment-changes.sh').read_text())
        self.assertLess(apply.index('wait_http "frontend"'),apply.index('"iniciar conciliador de processos"'))
        self.assertIn('--wait --wait-timeout 120 process-execution-worker',apply)
        self.assertIn('docker compose stop process-execution-worker',apply)
        subprocess.run(['bash','-n',str(ROOT/'deploy/bin/apply.sh')],check=True)

    def test_quoted_current_and_unquoted_legacy_commands_are_equivalent(self):
        workflow=(ROOT/'.github/workflows/deploy-containers.yml').read_text()
        self.assert_worker_image_contract(workflow)
        legacy=workflow.replace('"marketinghub-process-execution-worker:${GITHUB_SHA}"',
                                'marketinghub-process-execution-worker:${{ github.sha }}')
        self.assert_worker_image_contract(legacy)
        other_image='marketinghub-process-execution-worker:another-tested-revision'
        self.assert_worker_image_contract(
            workflow.replace('marketinghub-process-execution-worker:${GITHUB_SHA}', other_image), other_image)

    def test_missing_mismatched_or_unverified_image_is_rejected(self):
        workflow=(ROOT/'.github/workflows/deploy-containers.yml').read_text()
        changes=[
            ('docker build -t "marketinghub-process-execution-worker:${GITHUB_SHA}" process-execution-worker',
             'docker build -t "marketinghub-process-execution-worker:${GITHUB_SHA}" other-module'),
            ('docker run --rm --entrypoint node "marketinghub-process-execution-worker:${GITHUB_SHA}"',
             'docker run --rm --entrypoint node "marketinghub-process-execution-worker:other-revision"'),
            ('docker run --rm --entrypoint node "marketinghub-process-execution-worker:${GITHUB_SHA}" --check src/main.mjs',
             '# verificação removida'),
            ('docker save "marketinghub-process-execution-worker:${GITHUB_SHA}" -o process-worker-image.tar',
             'docker save "marketinghub-process-execution-worker:other-revision" -o process-worker-image.tar'),
            ('-o process-worker-image.tar', '-o other-image.tar'),
            ('  backend-image:', '  unrelated-image:'),
        ]
        for before,after in changes:
            with self.subTest(change=after):
                self.assertIn(before,workflow)
                with self.assertRaises(AssertionError):
                    self.assert_worker_image_contract(workflow.replace(before,after))

    def test_worker_has_no_database_or_activity_command(self):
        source=(ROOT/'process-execution-worker/src/v1/reconciler.mjs').read_text()
        for forbidden in ('/execution-requests','mysql','sourceReference','nextStage','activityId'):
            self.assertNotIn(forbidden,source)
        self.assertIn('/pending?limit=',source)

if __name__ == '__main__': unittest.main()
