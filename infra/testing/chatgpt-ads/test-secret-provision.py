"""Verifica transporte dos segredos sem rede nem credenciais reais."""
from pathlib import Path
import os
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[3] / 'scripts/provision-chatgpt-ads-secrets.sh'

class ProvisionTest(unittest.TestCase):
    """Simula SSH para conferir stdin, preservação e rejeição de parâmetros."""
    def test_secret_uses_stdin_and_absence_preserves_existing(self):
        with tempfile.TemporaryDirectory(dir=SCRIPT.parents[1] / 'artifacts') as tmp:
            root = Path(tmp)
            (root / 'ssh').write_text('#!/bin/bash\nset -eu\nprintf "%s\\n" "$@" > "$PROVISION_TEST_DIR/args"\ncat > "$PROVISION_TEST_DIR/stdin"\n')
            (root / 'ssh').chmod(0o700)
            env = dict(os.environ, PATH=tmp+':'+os.environ['PATH'], PROVISION_TEST_DIR=tmp,
                       CHATGPT_ADS_WORKER_TOKEN='synthetic-worker-key-longer-than-32-characters')
            result = subprocess.run(['bash', str(SCRIPT), 'backend', 'root@test.invalid', '/safe/test'], env=env, capture_output=True, text=True)
            self.assertEqual(0, result.returncode)
            self.assertEqual(env['CHATGPT_ADS_WORKER_TOKEN'], (root/'stdin').read_text())
            self.assertNotIn(env['CHATGPT_ADS_WORKER_TOKEN'], (root/'args').read_text()+result.stdout+result.stderr)
            (root/'args').unlink()
            env['CHATGPT_ADS_WORKER_TOKEN']=''
            result = subprocess.run(['bash',str(SCRIPT),'backend','root@test.invalid','/safe/test'],env=env,capture_output=True)
            self.assertEqual(0,result.returncode)
            self.assertFalse((root/'args').exists())
            result = subprocess.run(['bash',str(SCRIPT),'backend','root@test.invalid','/unsafe;command'],env=env,capture_output=True)
            self.assertNotEqual(0,result.returncode)
            self.assertFalse((root/'args').exists())

if __name__ == '__main__':
    unittest.main()
