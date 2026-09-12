"""Protege integração do novo módulo, identidade e manutenção da credencial sem publicação."""
import os
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]

class DeliveryContractTest(unittest.TestCase):
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
        self.assertIn('docker build -t marketinghub-process-execution-worker:',workflow)
        self.assertIn('process-execution-worker/*) backend=true', (ROOT/'scripts/detect-deployment-changes.sh').read_text())
        self.assertLess(apply.index('wait_http "frontend"'),apply.index('"iniciar conciliador de processos"'))
        self.assertIn('--wait --wait-timeout 120 process-execution-worker',apply)
        self.assertIn('docker compose stop process-execution-worker',apply)
        subprocess.run(['bash','-n',str(ROOT/'deploy/bin/apply.sh')],check=True)

    def test_worker_has_no_database_or_activity_command(self):
        source=(ROOT/'process-execution-worker/src/v1/reconciler.mjs').read_text()
        for forbidden in ('/execution-requests','mysql','sourceReference','nextStage','activityId'):
            self.assertNotIn(forbidden,source)
        self.assertIn('/pending?limit=',source)

if __name__ == '__main__': unittest.main()
